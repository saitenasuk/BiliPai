package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.data.model.response.SeasonArchiveItem
import com.android.purebilibili.data.model.response.SeasonItem
import com.android.purebilibili.data.model.response.SeriesArchiveItem
import com.android.purebilibili.data.model.response.SeriesItem

internal fun shouldApplySpaceLoadResult(
    requestMid: Long,
    activeMid: Long,
    requestGeneration: Long,
    activeGeneration: Long
): Boolean {
    return requestMid > 0L &&
        requestMid == activeMid &&
        requestGeneration == activeGeneration
}

internal fun applySpaceSupplementalData(
    state: SpaceUiState.Success,
    seasons: List<SeasonItem>,
    series: List<SeriesItem>,
    createdFavoriteFolders: List<FavFolder>,
    collectedFavoriteFolders: List<FavFolder>,
    seasonArchives: Map<Long, List<SeasonArchiveItem>>,
    seriesArchives: Map<Long, List<SeriesArchiveItem>>
): SpaceUiState.Success {
    val nextState = state.copy(
        seasons = seasons,
        series = series,
        createdFavoriteFolders = createdFavoriteFolders,
        collectedFavoriteFolders = collectedFavoriteFolders,
        seasonArchives = seasonArchives,
        seriesArchives = seriesArchives,
        headerState = state.headerState.copy(
            createdFavorites = createdFavoriteFolders,
            collectedFavorites = collectedFavoriteFolders
        )
    )

    val hasCollectionsLoaded = seasons.isNotEmpty() ||
        series.isNotEmpty() ||
        createdFavoriteFolders.isNotEmpty() ||
        collectedFavoriteFolders.isNotEmpty()

    return nextState.copy(
        tabShellState = nextState.tabShellState.withUpdatedTab(SpaceMainTab.COLLECTIONS) {
            it.copy(hasLoaded = hasCollectionsLoaded)
        }
    )
}
