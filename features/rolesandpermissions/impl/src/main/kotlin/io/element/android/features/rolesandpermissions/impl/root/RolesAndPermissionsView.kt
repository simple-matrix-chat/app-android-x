/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.rolesandpermissions.impl.root

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.rolesandpermissions.impl.R
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.components.ProgressDialog
import io.element.android.libraries.designsystem.components.async.AsyncActionView
import io.element.android.libraries.designsystem.components.dialogs.ConfirmationDialog
import io.element.android.libraries.designsystem.components.dialogs.ErrorDialog
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.ListItem
import io.element.android.libraries.designsystem.theme.components.ListItemStyle
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.hide
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList

@Composable
fun RolesAndPermissionsView(
    state: RolesAndPermissionsState,
    rolesAndPermissionsNavigator: RolesAndPermissionsNavigator,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding(),
        containerColor = ElementTheme.colors.bgSubtleSecondary,
        topBar = {
            RolesAndPermissionsTopBar(
                title = stringResource(R.string.screen_room_roles_and_permissions_title),
                onBackClick = rolesAndPermissionsNavigator::onBackClick,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val adminsTitle = if (state.roomSupportsOwnerRole) {
                stringResource(R.string.screen_room_roles_and_permissions_admins_and_owners)
            } else {
                stringResource(R.string.screen_room_roles_and_permissions_admins)
            }
            MomentRolesSection(title = stringResource(R.string.screen_room_roles_and_permissions_roles_header)) {
                MomentRolesRow(
                    title = adminsTitle,
                    icon = CompoundIcons.Admin(),
                    count = state.adminCount,
                    showCount = true,
                    showNavigation = true,
                    onClick = rolesAndPermissionsNavigator::openAdminList,
                )
                MomentRolesDivider()
                MomentRolesRow(
                    title = stringResource(R.string.screen_room_roles_and_permissions_moderators),
                    icon = CompoundIcons.ChatProblem(),
                    count = state.moderatorCount,
                    showCount = true,
                    showNavigation = true,
                    onClick = rolesAndPermissionsNavigator::openModeratorList,
                )
                if (state.canSelfDemote) {
                    MomentRolesDivider()
                    MomentRolesRow(
                        title = stringResource(R.string.screen_room_roles_and_permissions_change_my_role),
                        icon = CompoundIcons.Edit(),
                        onClick = { state.eventSink(RolesAndPermissionsEvents.ChangeOwnRole) },
                    )
                }
            }
            MomentRolesSection {
                MomentRolesRow(
                    title = stringResource(R.string.screen_room_roles_and_permissions_permissions_header),
                    icon = CompoundIcons.Settings(),
                    showNavigation = true,
                    onClick = rolesAndPermissionsNavigator::openEditPermissions,
                )
            }
            MomentRolesSection {
                MomentRolesRow(
                    title = stringResource(R.string.screen_room_roles_and_permissions_reset),
                    icon = CompoundIcons.Delete(),
                    destructive = true,
                    onClick = { state.eventSink(RolesAndPermissionsEvents.ResetPermissions) },
                )
            }
        }
    }

    AsyncActionView(
        async = state.resetPermissionsAction,
        confirmationDialog = {
            ConfirmationDialog(
                title = stringResource(R.string.screen_room_roles_and_permissions_reset_confirm_title),
                content = stringResource(R.string.screen_room_roles_and_permissions_reset_confirm_description),
                submitText = stringResource(CommonStrings.action_reset),
                destructiveSubmit = true,
                onSubmitClick = { state.eventSink(RolesAndPermissionsEvents.ResetPermissions) },
                onDismiss = { state.eventSink(RolesAndPermissionsEvents.CancelPendingAction) },
            )
        },
        onSuccess = { state.eventSink(RolesAndPermissionsEvents.CancelPendingAction) },
        onErrorDismiss = { state.eventSink(RolesAndPermissionsEvents.CancelPendingAction) }
    )

    when (state.changeOwnRoleAction) {
        is AsyncAction.Confirming -> {
            ChangeOwnRoleBottomSheet(
                availableDemoteActions = state.availableSelfDemoteActions,
                eventSink = state.eventSink,
            )
        }
        is AsyncAction.Loading -> {
            ProgressDialog()
        }
        is AsyncAction.Failure -> {
            ErrorDialog(
                content = stringResource(CommonStrings.error_unknown),
                onSubmit = { state.eventSink(RolesAndPermissionsEvents.CancelPendingAction) }
            )
        }
        else -> Unit
    }
}

@Composable
private fun RolesAndPermissionsTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp),
            onClick = onBackClick,
        ) {
            Icon(
                imageVector = CompoundIcons.ChevronLeft(),
                contentDescription = stringResource(CommonStrings.action_back),
                tint = ElementTheme.colors.iconPrimary,
            )
        }
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 64.dp)
                .semantics { heading() },
            text = title,
            style = ElementTheme.typography.fontHeadingSmMedium,
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MomentRolesSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (title != null) {
            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = title,
                style = ElementTheme.typography.fontBodySmMedium,
                color = ElementTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        MomentRolesCard(content = content)
    }
}

@Composable
private fun MomentRolesCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        border = BorderStroke(1.dp, ElementTheme.colors.borderDisabled),
        shadowElevation = 3.dp,
    ) {
        Column(content = content)
    }
}

@Composable
private fun MomentRolesRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    showCount: Boolean = false,
    showNavigation: Boolean = false,
    destructive: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (destructive) ElementTheme.colors.bgCriticalSubtle else ElementTheme.colors.bgSubtleSecondary
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = icon,
                contentDescription = null,
                tint = if (destructive) {
                    ElementTheme.colors.iconCriticalPrimary
                } else {
                    ElementTheme.colors.iconPrimary
                },
            )
        }
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = ElementTheme.typography.fontBodyLgRegular,
            color = if (destructive) ElementTheme.colors.textCriticalPrimary else ElementTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (showCount) {
            if (count != null) {
                Text(
                    text = count.toString(),
                    style = ElementTheme.typography.fontBodyMdMedium,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = ElementTheme.colors.iconSecondary,
                )
            }
        }
        if (showNavigation) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = CompoundIcons.ChevronRight(),
                contentDescription = null,
                tint = ElementTheme.colors.iconTertiary,
            )
        }
    }
}

@Composable
private fun MomentRolesDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp),
        color = ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.22f),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangeOwnRoleBottomSheet(
    availableDemoteActions: ImmutableList<SelfDemoteAction>,
    eventSink: (RolesAndPermissionsEvents) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    fun dismiss() {
        sheetState.hide(coroutineScope) {
            eventSink(RolesAndPermissionsEvents.CancelPendingAction)
        }
    }
    ModalBottomSheet(
        modifier = Modifier
            .systemBarsPadding()
            .navigationBarsPadding(),
        sheetState = sheetState,
        onDismissRequest = ::dismiss,
        scrollable = true,
    ) {
        Text(
            modifier = Modifier.padding(14.dp),
            text = stringResource(R.string.screen_room_roles_and_permissions_change_my_role),
            style = ElementTheme.typography.fontBodyLgMedium,
            color = ElementTheme.colors.textPrimary,
        )
        Text(
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 16.dp),
            text = stringResource(R.string.screen_room_change_role_confirm_demote_self_description),
            style = ElementTheme.typography.fontBodyLgRegular,
            color = ElementTheme.colors.textPrimary,
        )
        for (demoteAction in availableDemoteActions) {
            ListItem(
                headlineContent = { Text(stringResource(demoteAction.titleRes)) },
                onClick = {
                    sheetState.hide(coroutineScope) {
                        eventSink(RolesAndPermissionsEvents.DemoteSelfTo(demoteAction.role))
                    }
                },
                style = ListItemStyle.Destructive,
            )
        }
        ListItem(
            headlineContent = { Text(stringResource(CommonStrings.action_cancel)) },
            onClick = ::dismiss,
            style = ListItemStyle.Primary,
        )
    }
}

@PreviewsDayNight
@Composable
internal fun RolesAndPermissionsViewPreview(@PreviewParameter(RolesAndPermissionsStateProvider::class) state: RolesAndPermissionsState) {
    ElementPreview {
        RolesAndPermissionsView(
            state = state,
            rolesAndPermissionsNavigator = object : RolesAndPermissionsNavigator {},
        )
    }
}
