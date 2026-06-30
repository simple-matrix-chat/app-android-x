/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.appnav

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.theme.components.Icon

@Composable
internal fun LoggedInRootBottomBar(
    activeRootTab: LoggedInRootTab,
    onChatsClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.12f),
            )
            .clip(CircleShape)
            .background(ElementTheme.colors.bgCanvasDefaultLevel1)
            .border(
                width = 1.dp,
                color = ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.55f),
                shape = CircleShape,
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LoggedInRootBottomBarButton(
            item = LoggedInRootBottomBarItem.Chats,
            isSelected = activeRootTab == LoggedInRootTab.Chats,
            onClick = onChatsClick,
        )
        LoggedInRootBottomBarButton(
            item = LoggedInRootBottomBarItem.Profile,
            isSelected = activeRootTab == LoggedInRootTab.Profile,
            onClick = onProfileClick,
        )
    }
}

@Composable
private fun LoggedInRootBottomBarButton(
    item: LoggedInRootBottomBarItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(item.labelRes)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .width(64.dp)
            .height(44.dp)
            .clip(CircleShape)
            .background(if (isSelected) ElementTheme.colors.bgSubtleSecondary else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics {
                contentDescription = label
                selected = isSelected
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = item.icon(isSelected),
            contentDescription = null,
            tint = if (isSelected) ElementTheme.colors.iconPrimary else ElementTheme.colors.iconSecondary,
        )
    }
}

private enum class LoggedInRootBottomBarItem(
    @StringRes val labelRes: Int,
) {
    Chats(R.string.screen_logged_in_tab_chats),
    Profile(R.string.screen_logged_in_tab_profile);

    @Composable
    fun icon(isSelected: Boolean): ImageVector {
        return when (this) {
            Chats -> if (isSelected) CompoundIcons.ChatSolid() else CompoundIcons.Chat()
            Profile -> if (isSelected) CompoundIcons.UserProfileSolid() else CompoundIcons.UserProfile()
        }
    }
}
