package com.android.purebilibili.feature.following

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.FollowingUser
import com.android.purebilibili.data.model.response.RelationTagItem
import com.android.purebilibili.data.repository.ActionRepository
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.android.purebilibili.core.util.PinyinUtils

// UI 状态
sealed class FollowingListUiState {
    object Loading : FollowingListUiState()
    data class Success(
        val users: List<FollowingUser>,
        val total: Int,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = true
    ) : FollowingListUiState()
    data class Error(val message: String) : FollowingListUiState()
}

data class BatchUnfollowResult(
    val successCount: Int,
    val failedCount: Int,
    val succeededMids: Set<Long> = emptySet()
)

internal fun toggleFollowingSelection(current: Set<Long>, mid: Long): Set<Long> {
    return if (current.contains(mid)) current - mid else current + mid
}

internal fun resolveFollowingSelectAll(
    visibleMids: List<Long>,
    currentSelected: Set<Long>
): Set<Long> {
    val visibleSet = visibleMids.toSet()
    if (visibleSet.isEmpty()) return currentSelected
    val allSelected = visibleSet.all { currentSelected.contains(it) }
    return if (allSelected) currentSelected - visibleSet else currentSelected + visibleSet
}

internal fun buildBatchUnfollowResultMessage(successCount: Int, failedCount: Int): String {
    return when {
        failedCount == 0 -> "已取消关注 $successCount 位 UP 主"
        successCount == 0 -> "批量取关失败，请稍后重试"
        else -> "已取消关注 $successCount 位，$failedCount 位失败"
    }
}

data class BatchFollowGroupDialogData(
    val tags: List<RelationTagItem>,
    val initialSelection: Set<Long>,
    val hasMixedSelection: Boolean
)

private const val SPECIAL_FOLLOW_TAG_ID = -10L
private const val DEFAULT_FOLLOW_TAG_ID = 0L

internal fun resolveFollowGroupInitialSelection(groupSets: List<Set<Long>>): Set<Long> {
    if (groupSets.isEmpty()) return emptySet()
    val normalized = groupSets.map { it.filterNot { id -> id == 0L }.toSet() }
    val first = normalized.first()
    val allSame = normalized.all { it == first }
    return if (allSame) first else emptySet()
}

internal fun hasMixedFollowGroupSelection(groupSets: List<Set<Long>>): Boolean {
    if (groupSets.isEmpty()) return false
    val normalized = groupSets.map { it.filterNot { id -> id == 0L }.toSet() }
    return normalized.distinct().size > 1
}

class FollowingListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<FollowingListUiState>(FollowingListUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _isBatchUnfollowing = MutableStateFlow(false)
    val isBatchUnfollowing = _isBatchUnfollowing.asStateFlow()

    private val _followGroupTags = MutableStateFlow<List<RelationTagItem>>(emptyList())
    val followGroupTags = _followGroupTags.asStateFlow()

    private val _userFollowGroupIds = MutableStateFlow<Map<Long, Set<Long>>>(emptyMap())
    val userFollowGroupIds = _userFollowGroupIds.asStateFlow()

    private val _isFollowGroupMetaLoading = MutableStateFlow(false)
    val isFollowGroupMetaLoading = _isFollowGroupMetaLoading.asStateFlow()

    private var currentMid: Long = 0
    private val removedUserMids = mutableSetOf<Long>()
    
    fun loadFollowingList(mid: Long) {
        if (mid <= 0) return
        currentMid = mid
        removedUserMids.clear()
        
        viewModelScope.launch {
            _uiState.value = FollowingListUiState.Loading
            
            try {
                // 1. 加载第一页
                val response = NetworkModule.api.getFollowings(mid, pn = 1, ps = 50)
                if (response.code == 0 && response.data != null) {
                    val initialUsers = response.data.list.orEmpty()
                        .filterNot { removedUserMids.contains(it.mid) }
                    val total = response.data.total
                    
                    _uiState.value = FollowingListUiState.Success(
                        users = initialUsers,
                        total = total,
                        hasMore = initialUsers.size < total // 还有更多数据需要加载
                    )
                    refreshFollowGroupMetadata(initialUsers)
                    
                    // 2. 如果还有更多数据，自动在后台加载剩余所有页面 (为了支持全量搜索)
                    if (initialUsers.size < total) {
                        loadAllRemainingPages(mid, total, initialUsers)
                    }
                } else {
                    _uiState.value = FollowingListUiState.Error("加载失败: ${response.message}")
                }
            } catch (e: Exception) {
                _uiState.value = FollowingListUiState.Error(e.message ?: "网络错误")
            }
        }
    }
    
    // 自动加载剩余所有页面
    private fun loadAllRemainingPages(mid: Long, total: Int, initialUsers: List<FollowingUser>) {
        viewModelScope.launch {
            try {
                var currentUsers = initialUsers.toMutableList()
                val pageSize = 50
                // 计算需要加载的总页数
                val totalPages = (total + pageSize - 1) / pageSize
                
                // 从第2页开始循环加载
                for (page in 2..totalPages) {
                    if (mid != currentMid) break // 如果用户切换了查看的 UP 主，停止加载
                    
                    // 延迟一点时间，避免请求过于频繁触发风控
                    delay(300)
                    
                    val response = NetworkModule.api.getFollowings(mid, pn = page, ps = pageSize)
                    if (response.code == 0 && response.data != null) {
                        val newUsers = response.data.list.orEmpty()
                            .filterNot { removedUserMids.contains(it.mid) }
                        if (newUsers.isNotEmpty()) {
                            currentUsers.addAll(newUsers)
                            currentUsers = currentUsers
                                .distinctBy { it.mid }
                                .filterNot { removedUserMids.contains(it.mid) }
                                .toMutableList()
                            
                            // 更新 UI 状态
                            _uiState.value = FollowingListUiState.Success(
                                users = currentUsers.toList(), // Create new list to trigger recomposition
                                total = total,
                                hasMore = page < totalPages,
                                isLoadingMore = true // 显示正在后台加载
                            )
                            refreshFollowGroupMetadata(currentUsers)
                        }
                    } else {
                        break // 出错停止加载
                    }
                }
                
                // 加载完成
                val current = _uiState.value
                if (current is FollowingListUiState.Success) {
                    _uiState.value = current.copy(isLoadingMore = false, hasMore = false)
                }
            } catch (e: Exception) {
                // 后台加载失败暂不干扰主流程
                val current = _uiState.value
                if (current is FollowingListUiState.Success) {
                    _uiState.value = current.copy(isLoadingMore = false)
                }
            }
        }
    }
    
    // 手动加载更多 (已废弃，保留空实现兼容接口或删除)
    fun loadMore() { }

    suspend fun batchUnfollow(targetUsers: List<FollowingUser>): BatchUnfollowResult {
        if (targetUsers.isEmpty()) {
            return BatchUnfollowResult(successCount = 0, failedCount = 0)
        }
        if (_isBatchUnfollowing.value) {
            return BatchUnfollowResult(successCount = 0, failedCount = targetUsers.size)
        }

        _isBatchUnfollowing.value = true
        val successMids = mutableSetOf<Long>()
        var failedCount = 0
        try {
            targetUsers.forEachIndexed { index, user ->
                val result = ActionRepository.followUser(user.mid, follow = false)
                if (result.isSuccess) {
                    successMids.add(user.mid)
                } else {
                    failedCount += 1
                }
                if (index < targetUsers.lastIndex) {
                    delay(150)
                }
            }
            if (successMids.isNotEmpty()) {
                removedUserMids.addAll(successMids)
                applyRemovedUsers(successMids)
            }
            return BatchUnfollowResult(
                successCount = successMids.size,
                failedCount = failedCount,
                succeededMids = successMids
            )
        } finally {
            _isBatchUnfollowing.value = false
        }
    }

    private fun applyRemovedUsers(removedMids: Set<Long>) {
        val current = _uiState.value as? FollowingListUiState.Success ?: return
        val remainingUsers = current.users.filterNot { removedMids.contains(it.mid) }
        val reducedTotal = (current.total - removedMids.size).coerceAtLeast(remainingUsers.size)
        _userFollowGroupIds.update { currentMap ->
            currentMap - removedMids
        }
        _uiState.value = current.copy(
            users = remainingUsers,
            total = reducedTotal
        )
    }

    private fun refreshFollowGroupMetadata(users: List<FollowingUser>) {
        val mids = users.map { it.mid }.toSet()
        if (mids.isEmpty()) return

        viewModelScope.launch {
            if (_followGroupTags.value.isEmpty()) {
                ActionRepository.getFollowGroupTags().onSuccess { tags ->
                    _followGroupTags.value = tags
                        .filter { it.tagid != DEFAULT_FOLLOW_TAG_ID }
                        .sortedBy { it.tagid != SPECIAL_FOLLOW_TAG_ID }
                }
            }

            val existing = _userFollowGroupIds.value
            val missingMids = mids.filterNot { existing.containsKey(it) }
            if (missingMids.isEmpty()) return@launch

            _isFollowGroupMetaLoading.value = true
            try {
                val fetched = coroutineScope {
                    missingMids.map { mid ->
                        async {
                            val groupIds = ActionRepository.getUserFollowGroupIds(mid)
                                .getOrElse { emptySet() }
                            mid to groupIds
                        }
                    }.awaitAll()
                }.toMap()

                _userFollowGroupIds.update { currentMap ->
                    currentMap + fetched
                }
            } finally {
                _isFollowGroupMetaLoading.value = false
            }
        }
    }

    suspend fun prepareBatchGroupDialogData(targetMids: List<Long>): Result<BatchFollowGroupDialogData> {
        return runCatching {
            val mids = targetMids.toSet().toList()
            if (mids.isEmpty()) {
                return@runCatching BatchFollowGroupDialogData(
                    tags = emptyList(),
                    initialSelection = emptySet(),
                    hasMixedSelection = false
                )
            }

            val tags = ActionRepository.getFollowGroupTags().getOrThrow()
                .filter { it.tagid != 0L }
                .sortedBy { it.tagid != -10L }

            val groupSets = coroutineScope {
                mids.map { mid ->
                    async {
                        ActionRepository.getUserFollowGroupIds(mid).getOrElse { emptySet() }
                    }
                }.awaitAll()
            }

            BatchFollowGroupDialogData(
                tags = tags,
                initialSelection = resolveFollowGroupInitialSelection(groupSets),
                hasMixedSelection = hasMixedFollowGroupSelection(groupSets)
            )
        }
    }

    suspend fun saveBatchGroupSelection(targetMids: List<Long>, selectedTagIds: Set<Long>): Result<Boolean> {
        return ActionRepository.overwriteFollowGroupIds(targetMids.toSet(), selectedTagIds)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingListScreen(
    mid: Long,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,  // 点击跳转到 UP 主空间
    viewModel: FollowingListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isBatchUnfollowing by viewModel.isBatchUnfollowing.collectAsState()
    val followGroupTags by viewModel.followGroupTags.collectAsState()
    val userFollowGroupIds by viewModel.userFollowGroupIds.collectAsState()
    val isFollowGroupMetaLoading by viewModel.isFollowGroupMetaLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(mid) {
        viewModel.loadFollowingList(mid)
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedGroupFilter by remember { mutableStateOf<Long?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    var selectedMids by remember { mutableStateOf(setOf<Long>()) }
    var showBatchUnfollowConfirm by remember { mutableStateOf(false) }
    var showBatchGroupDialog by remember { mutableStateOf(false) }
    var groupDialogLoading by remember { mutableStateOf(false) }
    var groupDialogSaving by remember { mutableStateOf(false) }
    var groupDialogTags by remember { mutableStateOf<List<RelationTagItem>>(emptyList()) }
    var groupDialogSelection by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var groupDialogMixed by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("我的关注") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(CupertinoIcons.Default.ChevronBackward, contentDescription = "返回")
                    }
                },
                actions = {
                    if (uiState is FollowingListUiState.Success) {
                        TextButton(
                            onClick = {
                                isEditMode = !isEditMode
                                if (!isEditMode) {
                                    selectedMids = emptySet()
                                }
                            },
                            enabled = !isBatchUnfollowing
                        ) {
                            Text(if (isEditMode) "完成" else "管理")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 🔍 搜索栏
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                com.android.purebilibili.core.ui.components.IOSSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "搜索 UP 主"
                )
            }

            Box(
                modifier = Modifier.weight(1f)
            ) {
                when (val state = uiState) {
                    is FollowingListUiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CupertinoActivityIndicator()
                        }
                    }
                    
                    is FollowingListUiState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("😢", fontSize = 48.sp)
                                Spacer(Modifier.height(16.dp))
                                Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { viewModel.loadFollowingList(mid) }) {
                                    Text("重试")
                                }
                            }
                        }
                    }
                    
                    is FollowingListUiState.Success -> {
                        LaunchedEffect(state.users) {
                            val available = state.users.asSequence().map { it.mid }.toSet()
                            selectedMids = selectedMids.intersect(available)
                        }

                        val groupFilterChips = remember(state.users, followGroupTags, userFollowGroupIds) {
                            val users = state.users
                            val defaultCount = users.count { user ->
                                userFollowGroupIds[user.mid].isNullOrEmpty()
                            }
                            val dynamicTags = followGroupTags.ifEmpty {
                                listOf(
                                    RelationTagItem(tagid = SPECIAL_FOLLOW_TAG_ID, name = "特别关注", count = 0)
                                )
                            }
                            buildList {
                                add(RelationTagItem(tagid = Long.MIN_VALUE, name = "全部", count = users.size))
                                add(RelationTagItem(tagid = DEFAULT_FOLLOW_TAG_ID, name = "默认分组", count = defaultCount))
                                dynamicTags.forEach { tag ->
                                    val count = users.count { user ->
                                        userFollowGroupIds[user.mid]?.contains(tag.tagid) == true
                                    }
                                    add(tag.copy(count = count))
                                }
                            }
                        }

                        val usersByGroup = remember(state.users, selectedGroupFilter, userFollowGroupIds) {
                            when (selectedGroupFilter) {
                                null, Long.MIN_VALUE -> state.users
                                DEFAULT_FOLLOW_TAG_ID -> state.users.filter { user ->
                                    userFollowGroupIds[user.mid].isNullOrEmpty()
                                }
                                else -> state.users.filter { user ->
                                    userFollowGroupIds[user.mid]?.contains(selectedGroupFilter) == true
                                }
                            }
                        }

                        // 🔍 过滤列表
                        val filteredUsers = remember(usersByGroup, searchQuery) {
                            if (searchQuery.isBlank()) usersByGroup
                            else {
                                usersByGroup.filter {
                                    PinyinUtils.matches(it.uname, searchQuery) ||
                                    PinyinUtils.matches(it.sign, searchQuery)
                                }
                            }
                        }
                        val visibleMids = remember(filteredUsers) { filteredUsers.map { it.mid } }
                        val selectedCount = selectedMids.size
                        val hasSelection = selectedCount > 0

                        if (filteredUsers.isEmpty() && searchQuery.isNotEmpty()) {
                             Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("没有找到相关 UP 主", color = MaterialTheme.colorScheme.onSurfaceVariant)
                             }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                // 统计信息
                                item {
                                    Text(
                                        text = when {
                                            isEditMode -> "已选 $selectedCount 人"
                                            searchQuery.isEmpty() && (selectedGroupFilter == null || selectedGroupFilter == Long.MIN_VALUE) ->
                                                "共 ${state.total} 个关注"
                                            searchQuery.isEmpty() -> "当前分组 ${filteredUsers.size} 人"
                                            else -> "找到 ${filteredUsers.size} 个结果"
                                        },
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }

                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(horizontal = 16.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        groupFilterChips.forEach { chip ->
                                            val chipFilterId = if (chip.tagid == Long.MIN_VALUE) null else chip.tagid
                                            FilterChip(
                                                selected = selectedGroupFilter == chipFilterId ||
                                                    (selectedGroupFilter == null && chip.tagid == Long.MIN_VALUE),
                                                onClick = { selectedGroupFilter = chipFilterId },
                                                label = {
                                                    Text("${chip.name} ${chip.count}")
                                                }
                                            )
                                        }
                                    }
                                }

                                if (isFollowGroupMetaLoading) {
                                    item {
                                        Text(
                                            text = "分组信息加载中...",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                
                                items(filteredUsers, key = { it.mid }) { user ->
                                    FollowingUserItem(
                                        user = user,
                                        isEditMode = isEditMode,
                                        isSelected = selectedMids.contains(user.mid),
                                        onClick = {
                                            if (isEditMode) {
                                                selectedMids = toggleFollowingSelection(selectedMids, user.mid)
                                            } else {
                                                onUserClick(user.mid)
                                            }
                                        }
                                    )
                                }
                                
                                // 加载更多 (仅在未搜索时显示，因为搜索是本地过滤)
                                if (searchQuery.isEmpty()) {
                                    if (state.isLoadingMore) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CupertinoActivityIndicator()
                                            }
                                        }
                                    } else if (state.hasMore) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.loadMore() }
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "加载更多",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (isEditMode) {
                            Surface(
                                tonalElevation = 3.dp,
                                shadowElevation = 3.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            selectedMids = resolveFollowingSelectAll(
                                                visibleMids = visibleMids,
                                                currentSelected = selectedMids
                                            )
                                        },
                                        enabled = !isBatchUnfollowing
                                    ) {
                                        val allVisibleSelected = visibleMids.isNotEmpty() &&
                                            visibleMids.all { selectedMids.contains(it) }
                                        Text(if (allVisibleSelected) "取消全选" else "全选当前")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            showBatchGroupDialog = true
                                            groupDialogLoading = true
                                            scope.launch {
                                                val result = viewModel.prepareBatchGroupDialogData(selectedMids.toList())
                                                result.onSuccess { dialogData ->
                                                    groupDialogTags = dialogData.tags
                                                    groupDialogSelection = dialogData.initialSelection
                                                    groupDialogMixed = dialogData.hasMixedSelection
                                                }.onFailure {
                                                    showBatchGroupDialog = false
                                                    snackbarHostState.showSnackbar("加载分组失败: ${it.message}")
                                                }
                                                groupDialogLoading = false
                                            }
                                        },
                                        enabled = hasSelection && !isBatchUnfollowing
                                    ) {
                                        Text("设置分组")
                                    }

                                    Button(
                                        onClick = { showBatchUnfollowConfirm = true },
                                        enabled = hasSelection && !isBatchUnfollowing,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (isBatchUnfollowing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        } else {
                                            Text("取消关注 ($selectedCount)")
                                        }
                                    }
                                }
                            }
                        }

                        if (showBatchUnfollowConfirm) {
                            AlertDialog(
                                onDismissRequest = {
                                    if (!isBatchUnfollowing) showBatchUnfollowConfirm = false
                                },
                                title = { Text("批量取消关注") },
                                text = { Text("确认取消关注已选择的 $selectedCount 位 UP 主吗？") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            val targets = state.users.filter { selectedMids.contains(it.mid) }
                                            scope.launch {
                                                val result = viewModel.batchUnfollow(targets)
                                                snackbarHostState.showSnackbar(
                                                    buildBatchUnfollowResultMessage(
                                                        successCount = result.successCount,
                                                        failedCount = result.failedCount
                                                    )
                                                )
                                                selectedMids = selectedMids - result.succeededMids
                                                if (selectedMids.isEmpty()) {
                                                    isEditMode = false
                                                }
                                                showBatchUnfollowConfirm = false
                                            }
                                        },
                                        enabled = !isBatchUnfollowing
                                    ) {
                                        Text("确认")
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { showBatchUnfollowConfirm = false },
                                        enabled = !isBatchUnfollowing
                                    ) {
                                        Text("取消")
                                    }
                                }
                            )
                        }

                        if (showBatchGroupDialog) {
                            AlertDialog(
                                onDismissRequest = {
                                    if (!groupDialogSaving) showBatchGroupDialog = false
                                },
                                title = { Text("批量设置分组") },
                                text = {
                                    if (groupDialogLoading) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CupertinoActivityIndicator()
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 320.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            if (groupDialogMixed) {
                                                Text(
                                                    text = "检测到已选 UP 主原分组不一致，已默认全部不选。",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                )
                                            }
                                            if (groupDialogTags.isEmpty()) {
                                                Text(
                                                    text = "暂无可用分组（不勾选即回到默认分组）",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 13.sp
                                                )
                                            } else {
                                                groupDialogTags.forEach { tag ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                groupDialogSelection = if (groupDialogSelection.contains(tag.tagid)) {
                                                                    groupDialogSelection - tag.tagid
                                                                } else {
                                                                    groupDialogSelection + tag.tagid
                                                                }
                                                            }
                                                            .padding(vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Checkbox(
                                                            checked = groupDialogSelection.contains(tag.tagid),
                                                            onCheckedChange = { checked ->
                                                                groupDialogSelection = if (checked == true) {
                                                                    groupDialogSelection + tag.tagid
                                                                } else {
                                                                    groupDialogSelection - tag.tagid
                                                                }
                                                            }
                                                        )
                                                        Text(
                                                            text = "${tag.name} (${tag.count})",
                                                            fontSize = 14.sp,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "确定后会完全覆盖原分组设置。",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(top = 8.dp)
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            groupDialogSaving = true
                                            scope.launch {
                                                val result = viewModel.saveBatchGroupSelection(
                                                    targetMids = selectedMids.toList(),
                                                    selectedTagIds = groupDialogSelection
                                                )
                                                result.onSuccess {
                                                    showBatchGroupDialog = false
                                                    snackbarHostState.showSnackbar("分组设置已保存")
                                                }.onFailure {
                                                    snackbarHostState.showSnackbar("分组设置失败: ${it.message}")
                                                }
                                                groupDialogSaving = false
                                            }
                                        },
                                        enabled = !groupDialogLoading && !groupDialogSaving
                                    ) {
                                        if (groupDialogSaving) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        } else {
                                            Text("确定")
                                        }
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { showBatchGroupDialog = false },
                                        enabled = !groupDialogSaving
                                    ) {
                                        Text("取消")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowingUserItem(
    user: FollowingUser,
    isEditMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(FormatUtils.fixImageUrl(user.face))
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        
        Spacer(Modifier.width(12.dp))
        
        // 用户信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.uname,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (user.sign.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = user.sign,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isEditMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() }
            )
        }
    }
}
