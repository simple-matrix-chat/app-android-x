/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.space.impl.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.space.impl.R
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun SpaceSettingsView(
    state: SpaceSettingsState,
    onBackClick: () -> Unit,
    onSpaceInfoClick: () -> Unit,
    onMembersClick: () -> Unit,
    onRolesAndPermissionsClick: () -> Unit,
    onLeaveSpaceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ElementTheme.colors.bgSubtleSecondary,
        topBar = {
            SpaceSettingsTopBar(onBackClick = onBackClick)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            MomentSpaceSettingsCard {
                SpaceInfoSection(
                    roomId = state.roomId,
                    name = state.name,
                    avatarUrl = state.avatarUrl,
                    canonicalAlias = state.canonicalAlias?.value,
                    canEditDetails = state.canEditDetails,
                    onSpaceInfoClick = onSpaceInfoClick,
                )
            }
            MomentSpaceSettingsCard {
                MembersItem(state.memberCount, onClick = onMembersClick)
                if (state.showRolesAndPermissions) {
                    RolesAndPermissionsItem(
                        onClick = onRolesAndPermissionsClick,
                        showDivider = false,
                    )
                }
            }
            MomentSpaceSettingsCard {
                LeaveSpaceItem(
                    onClick = onLeaveSpaceClick
                )
            }
        }
    }
}

@Composable
private fun SpaceInfoSection(
    roomId: RoomId,
    name: String,
    avatarUrl: String?,
    canonicalAlias: String?,
    canEditDetails: Boolean,
    onSpaceInfoClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canEditDetails, onClick = onSpaceInfoClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Avatar(
            avatarData = AvatarData(roomId.value, name, avatarUrl, AvatarSize.SpaceListItem),
            avatarType = AvatarType.Space(),
            contentDescription = stringResource(CommonStrings.a11y_avatar),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = name,
                style = ElementTheme.typography.fontHeadingMdRegular,
                color = ElementTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (canonicalAlias != null) {
                Text(
                    text = canonicalAlias,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (canEditDetails) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = CompoundIcons.ChevronRight(),
                contentDescription = null,
                tint = ElementTheme.colors.iconSecondary,
            )
        }
    }
}

@Composable
private fun MomentSpaceSettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.55f)),
    ) {
        Column(content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpaceSettingsTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        titleStr = stringResource(CommonStrings.common_settings),
        navigationIcon = { BackButton(onClick = onBackClick) },
        modifier = modifier,
    )
}

@Composable
private fun MembersItem(
    memberCount: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MomentSpaceSettingsRow(
        title = stringResource(CommonStrings.common_people),
        imageVector = CompoundIcons.User(),
        trailingText = memberCount.toString(),
        showDivider = true,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun RolesAndPermissionsItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    MomentSpaceSettingsRow(
        title = stringResource(R.string.screen_space_settings_roles_and_permissions),
        imageVector = CompoundIcons.Admin(),
        showDivider = showDivider,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun LeaveSpaceItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MomentSpaceSettingsRow(
        title = stringResource(CommonStrings.action_leave_space),
        imageVector = CompoundIcons.Leave(),
        isDestructive = true,
        showChevron = false,
        showDivider = false,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun MomentSpaceSettingsRow(
    title: String,
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    isDestructive: Boolean = false,
    showChevron: Boolean = true,
    showDivider: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MomentSpaceSettingsIconTile(
                imageVector = imageVector,
                isDestructive = isDestructive,
            )
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = ElementTheme.typography.fontBodyLgMedium,
                color = if (isDestructive) ElementTheme.colors.textCriticalPrimary else ElementTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = ElementTheme.typography.fontBodyLgRegular,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showChevron) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = CompoundIcons.ChevronRight(),
                    contentDescription = null,
                    tint = ElementTheme.colors.iconSecondary,
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(start = 60.dp))
        }
    }
}

@Composable
private fun MomentSpaceSettingsIconTile(
    imageVector: ImageVector,
    isDestructive: Boolean,
) {
    Box(
        modifier = Modifier
            .size(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(10.dp),
            color = if (isDestructive) ElementTheme.colors.bgCriticalSubtle else ElementTheme.colors.bgSubtleSecondary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = imageVector,
                    contentDescription = null,
                    tint = if (isDestructive) ElementTheme.colors.iconCriticalPrimary else ElementTheme.colors.iconPrimary,
                )
            }
        }
    }
}

@PreviewsDayNight
@Composable
internal fun SpaceSettingsViewPreview(
    @PreviewParameter(SpaceSettingsStateProvider::class) state: SpaceSettingsState
) = ElementPreview {
    SpaceSettingsView(
        state = state,
        onBackClick = {},
        onSpaceInfoClick = {},
        onMembersClick = {},
        onRolesAndPermissionsClick = {},
        onLeaveSpaceClick = {},
        modifier = Modifier,
    )
}
