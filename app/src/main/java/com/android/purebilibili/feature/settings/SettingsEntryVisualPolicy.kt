package com.android.purebilibili.feature.settings

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Feed
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.purebilibili.R
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSOrange
import com.android.purebilibili.core.theme.iOSPink
import com.android.purebilibili.core.theme.iOSPurple
import com.android.purebilibili.core.theme.iOSRed
import com.android.purebilibili.core.theme.iOSTeal
import com.android.purebilibili.core.ui.AppIcons
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowCounterclockwise
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowTriangle2Circlepath
import io.github.alexzhirkevich.cupertino.icons.outlined.DocOnDoc
import io.github.alexzhirkevich.cupertino.icons.outlined.DocText
import io.github.alexzhirkevich.cupertino.icons.outlined.ExclamationmarkTriangle
import io.github.alexzhirkevich.cupertino.icons.outlined.Folder
import io.github.alexzhirkevich.cupertino.icons.outlined.Gift
import io.github.alexzhirkevich.cupertino.icons.outlined.Lightbulb
import io.github.alexzhirkevich.cupertino.icons.outlined.Link
import io.github.alexzhirkevich.cupertino.icons.outlined.ListBullet
import io.github.alexzhirkevich.cupertino.icons.outlined.Lock
import io.github.alexzhirkevich.cupertino.icons.outlined.Newspaper
import io.github.alexzhirkevich.cupertino.icons.outlined.PaintbrushPointed
import io.github.alexzhirkevich.cupertino.icons.outlined.Person
import io.github.alexzhirkevich.cupertino.icons.outlined.PlayCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.PuzzlepieceExtension
import io.github.alexzhirkevich.cupertino.icons.outlined.SquareAndArrowUp
import io.github.alexzhirkevich.cupertino.icons.outlined.SquareStack3dUp
import io.github.alexzhirkevich.cupertino.icons.outlined.Terminal
import io.github.alexzhirkevich.cupertino.icons.outlined.Trash
import io.github.alexzhirkevich.cupertino.icons.outlined.XmarkCircle

internal data class SettingsEntryVisual(
    val icon: ImageVector? = null,
    @DrawableRes val iconResId: Int? = null,
    val iconTint: Color
)

internal fun resolveSettingsEntryVisual(
    target: SettingsSearchTarget,
    uiPreset: UiPreset = UiPreset.IOS
): SettingsEntryVisual {
    if (uiPreset == UiPreset.MD3) {
        return when (target) {
            SettingsSearchTarget.APPEARANCE -> SettingsEntryVisual(
                icon = Icons.Outlined.Palette,
                iconTint = iOSPink
            )
            SettingsSearchTarget.PLAYBACK -> SettingsEntryVisual(
                icon = Icons.Outlined.PlayCircle,
                iconTint = iOSGreen
            )
            SettingsSearchTarget.BOTTOM_BAR -> SettingsEntryVisual(
                icon = Icons.Outlined.Widgets,
                iconTint = iOSBlue
            )
            SettingsSearchTarget.PERMISSION -> SettingsEntryVisual(
                icon = Icons.Outlined.Security,
                iconTint = iOSTeal
            )
            SettingsSearchTarget.BLOCKED_LIST -> SettingsEntryVisual(
                icon = Icons.Outlined.Block,
                iconTint = iOSRed
            )
            SettingsSearchTarget.SETTINGS_SHARE -> SettingsEntryVisual(
                icon = Icons.Outlined.Share,
                iconTint = iOSGreen
            )
            SettingsSearchTarget.WEBDAV_BACKUP -> SettingsEntryVisual(
                icon = Icons.Outlined.Backup,
                iconTint = iOSBlue
            )
            SettingsSearchTarget.DOWNLOAD_PATH -> SettingsEntryVisual(
                icon = Icons.Outlined.Folder,
                iconTint = iOSBlue
            )
            SettingsSearchTarget.CLEAR_CACHE -> SettingsEntryVisual(
                icon = Icons.Outlined.DeleteOutline,
                iconTint = iOSPink
            )
            SettingsSearchTarget.PLUGINS -> SettingsEntryVisual(
                icon = Icons.Outlined.Extension,
                iconTint = iOSPurple
            )
            SettingsSearchTarget.EXPORT_LOGS -> SettingsEntryVisual(
                icon = Icons.Outlined.Article,
                iconTint = iOSTeal
            )
            SettingsSearchTarget.OPEN_SOURCE_LICENSES -> SettingsEntryVisual(
                icon = Icons.Outlined.Description,
                iconTint = iOSOrange
            )
            SettingsSearchTarget.OPEN_SOURCE_HOME -> SettingsEntryVisual(
                icon = Icons.Outlined.OpenInNew,
                iconTint = iOSPurple
            )
            SettingsSearchTarget.CHECK_UPDATE -> SettingsEntryVisual(
                icon = Icons.Outlined.Update,
                iconTint = iOSBlue
            )
            SettingsSearchTarget.VIEW_RELEASE_NOTES -> SettingsEntryVisual(
                icon = Icons.Outlined.Feed,
                iconTint = iOSTeal
            )
            SettingsSearchTarget.REPLAY_ONBOARDING -> SettingsEntryVisual(
                icon = Icons.Outlined.Replay,
                iconTint = iOSPink
            )
            SettingsSearchTarget.TIPS -> SettingsEntryVisual(
                icon = Icons.Outlined.Lightbulb,
                iconTint = iOSOrange
            )
            SettingsSearchTarget.OPEN_LINKS -> SettingsEntryVisual(
                icon = Icons.Outlined.Link,
                iconTint = iOSTeal
            )
            SettingsSearchTarget.DONATE -> SettingsEntryVisual(
                icon = Icons.Outlined.CardGiftcard,
                iconTint = Color(0xFFFF3B30)
            )
            SettingsSearchTarget.TELEGRAM -> SettingsEntryVisual(
                iconResId = R.drawable.ic_telegram_mono,
                iconTint = Color(0xFF0088CC)
            )
            SettingsSearchTarget.TWITTER -> SettingsEntryVisual(
                icon = AppIcons.Twitter,
                iconTint = Color(0xFF1DA1F2)
            )
            SettingsSearchTarget.DISCLAIMER -> SettingsEntryVisual(
                icon = Icons.Outlined.WarningAmber,
                iconTint = iOSBlue
            )
        }
    }

    return when (target) {
        SettingsSearchTarget.APPEARANCE -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.PaintbrushPointed,
            iconTint = iOSPink
        )
        SettingsSearchTarget.PLAYBACK -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.PlayCircle,
            iconTint = iOSGreen
        )
        SettingsSearchTarget.BOTTOM_BAR -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.SquareStack3dUp,
            iconTint = iOSBlue
        )
        SettingsSearchTarget.PERMISSION -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.Lock,
            iconTint = iOSTeal
        )
        SettingsSearchTarget.BLOCKED_LIST -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.XmarkCircle,
            iconTint = iOSRed
        )
        SettingsSearchTarget.SETTINGS_SHARE -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.ListBullet,
            iconTint = iOSGreen
        )
        SettingsSearchTarget.WEBDAV_BACKUP -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.DocOnDoc,
            iconTint = iOSBlue
        )
        SettingsSearchTarget.DOWNLOAD_PATH -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.Folder,
            iconTint = iOSBlue
        )
        SettingsSearchTarget.CLEAR_CACHE -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.Trash,
            iconTint = iOSPink
        )
        SettingsSearchTarget.PLUGINS -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.PuzzlepieceExtension,
            iconTint = iOSPurple
        )
        SettingsSearchTarget.EXPORT_LOGS -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.Terminal,
            iconTint = iOSTeal
        )
        SettingsSearchTarget.OPEN_SOURCE_LICENSES -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.DocText,
            iconTint = iOSOrange
        )
        SettingsSearchTarget.OPEN_SOURCE_HOME -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.SquareAndArrowUp,
            iconTint = iOSPurple
        )
        SettingsSearchTarget.CHECK_UPDATE -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.ArrowTriangle2Circlepath,
            iconTint = iOSBlue
        )
        SettingsSearchTarget.VIEW_RELEASE_NOTES -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.Newspaper,
            iconTint = iOSTeal
        )
        SettingsSearchTarget.REPLAY_ONBOARDING -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.ArrowCounterclockwise,
            iconTint = iOSPink
        )
        SettingsSearchTarget.TIPS -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.Lightbulb,
            iconTint = iOSOrange
        )
        SettingsSearchTarget.OPEN_LINKS -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.Link,
            iconTint = iOSTeal
        )
        SettingsSearchTarget.DONATE -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.Gift,
            iconTint = Color(0xFFFF3B30)
        )
        SettingsSearchTarget.TELEGRAM -> SettingsEntryVisual(
            iconResId = R.drawable.ic_telegram_mono,
            iconTint = Color(0xFF0088CC)
        )
        SettingsSearchTarget.TWITTER -> SettingsEntryVisual(
            icon = AppIcons.Twitter,
            iconTint = Color(0xFF1DA1F2)
        )
        SettingsSearchTarget.DISCLAIMER -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.ExclamationmarkTriangle,
            iconTint = iOSBlue
        )
    }
}
