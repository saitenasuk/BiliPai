package com.android.purebilibili.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Wifi
import com.android.purebilibili.core.theme.UiPreset
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Tv
import io.github.alexzhirkevich.cupertino.icons.outlined.Bookmark
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronBackward
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronDown
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronForward
import io.github.alexzhirkevich.cupertino.icons.outlined.Clock
import io.github.alexzhirkevich.cupertino.icons.outlined.Envelope
import io.github.alexzhirkevich.cupertino.icons.outlined.Lock
import io.github.alexzhirkevich.cupertino.icons.outlined.MagnifyingGlass
import io.github.alexzhirkevich.cupertino.icons.outlined.PersonCropCircleBadgePlus
import io.github.alexzhirkevich.cupertino.icons.outlined.RectanglePortraitAndArrowForward
import io.github.alexzhirkevich.cupertino.icons.outlined.XmarkCircle
import org.junit.Assert.assertEquals
import org.junit.Test

class AppIconsPresetPolicyTest {

    @Test
    fun `md3 preset should map key chrome icons to material vectors`() {
        assertEquals(Icons.AutoMirrored.Filled.ArrowBack, resolveAppBackIcon(UiPreset.MD3))
        assertEquals(Icons.Filled.Search, resolveAppSearchIcon(UiPreset.MD3))
        assertEquals(Icons.Filled.Clear, resolveAppClearIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.KeyboardArrowRight, resolveAppChevronForwardIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.KeyboardArrowDown, resolveAppChevronDownIcon(UiPreset.MD3))
    }

    @Test
    fun `md3 preset should map service and panel icons to material vectors`() {
        assertEquals(Icons.Outlined.History, resolveAppHistoryIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.BookmarkBorder, resolveAppBookmarkIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.MailOutline, resolveAppInboxIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.LiveTv, resolveAppTvIcon(UiPreset.MD3))
        assertEquals(Icons.AutoMirrored.Outlined.ExitToApp, resolveAppLogoutIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.Timer, resolveAppTimerIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.MusicNote, resolveAppMusicIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.SwapHoriz, resolveAppFlipHorizontalIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.SwapVert, resolveAppFlipVerticalIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.Headphones, resolveAppHeadphonesIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.PlayCircleOutline, resolveAppQualityIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.Memory, resolveAppCodecIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.Speed, resolveAppSpeedIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.TouchApp, resolveAppGestureTapIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.Wifi, resolveAppWifiIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.PersonAddAlt1, resolveAppProfileAddIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.Lock, resolveAppLockIcon(UiPreset.MD3))
    }

    @Test
    fun `ios preset should preserve cupertino mappings`() {
        assertEquals(CupertinoIcons.Outlined.ChevronBackward, resolveAppBackIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.MagnifyingGlass, resolveAppSearchIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.XmarkCircle, resolveAppClearIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.Clock, resolveAppHistoryIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.Bookmark, resolveAppBookmarkIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.Envelope, resolveAppInboxIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Filled.Tv, resolveAppTvIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.RectanglePortraitAndArrowForward, resolveAppLogoutIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.PersonCropCircleBadgePlus, resolveAppProfileAddIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.Lock, resolveAppLockIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.ChevronForward, resolveAppChevronForwardIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.ChevronDown, resolveAppChevronDownIcon(UiPreset.IOS))
    }
}
