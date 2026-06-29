/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.blockedusers

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.components.async.AsyncIndicator
import io.element.android.libraries.designsystem.components.async.AsyncIndicatorHost
import io.element.android.libraries.designsystem.components.async.rememberAsyncIndicatorState
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.components.dialogs.ConfirmationDialog
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.matrix.ui.model.getBestName
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun BlockedUsersView(
    state: BlockedUsersState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackClick)

    val rowsEnabled = state.unblockUserAction !is AsyncAction.Loading

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ElementTheme.colors.bgSubtleSecondary,
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp),
            ) {
                MomentBlockedUsersTopBar(onBackClick = onBackClick)
                if (state.blockedUsers.isEmpty()) {
                    MomentBlockedUsersEmptyState(
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(
                            items = state.blockedUsers,
                            key = { it.userId.value },
                        ) { matrixUser ->
                            BlockedUserItem(
                                matrixUser = matrixUser,
                                enabled = rowsEnabled,
                                onClick = { state.eventSink(BlockedUsersEvents.Unblock(it)) },
                            )
                        }
                    }
                }
            }
        }

        val asyncIndicatorState = rememberAsyncIndicatorState()
        AsyncIndicatorHost(modifier = Modifier.statusBarsPadding(), state = asyncIndicatorState)

        when (state.unblockUserAction) {
            is AsyncAction.Loading -> {
                LaunchedEffect(state.unblockUserAction) {
                    asyncIndicatorState.enqueue {
                        AsyncIndicator.Loading(text = stringResource(R.string.screen_blocked_users_unblocking))
                    }
                }
            }
            is AsyncAction.Failure -> {
                LaunchedEffect(state.unblockUserAction) {
                    asyncIndicatorState.enqueue(durationMs = AsyncIndicator.DURATION_SHORT) {
                        AsyncIndicator.Failure(text = stringResource(CommonStrings.common_failed))
                    }
                }
            }
            is AsyncAction.Success -> {
                LaunchedEffect(state.unblockUserAction) {
                    asyncIndicatorState.clear()
                }
            }
            is AsyncAction.Confirming -> {
                ConfirmationDialog(
                    title = stringResource(R.string.screen_blocked_users_unblock_alert_title),
                    content = stringResource(R.string.screen_blocked_users_unblock_alert_description),
                    submitText = stringResource(R.string.screen_blocked_users_unblock_alert_action),
                    onSubmitClick = { state.eventSink(BlockedUsersEvents.ConfirmUnblock) },
                    onDismiss = { state.eventSink(BlockedUsersEvents.Cancel) }
                )
            }
            else -> Unit
        }
    }
}

@Composable
private fun MomentBlockedUsersTopBar(
    onBackClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = CompoundIcons.ChevronLeft(),
                contentDescription = stringResource(CommonStrings.action_back),
                tint = ElementTheme.colors.iconPrimary,
            )
        }
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp),
            text = stringResource(CommonStrings.common_blocked_users),
            style = ElementTheme.typography.fontHeadingSmMedium,
            color = ElementTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MomentBlockedUsersEmptyState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.screen_blocked_users_empty),
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BlockedUserItem(
    matrixUser: MatrixUser,
    enabled: Boolean,
    onClick: (UserId) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.62f),
        shape = RoundedCornerShape(24.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .semantics(mergeDescendants = true) {}
                .clickable(enabled = enabled) { onClick(matrixUser.userId) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                avatarData = matrixUser.getAvatarData(AvatarSize.UserListItem),
                avatarType = AvatarType.User,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = matrixUser.getBestName(),
                    style = ElementTheme.typography.fontBodyLgMedium,
                    color = ElementTheme.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!matrixUser.displayName.isNullOrEmpty()) {
                    Text(
                        text = matrixUser.userId.value,
                        style = ElementTheme.typography.fontBodySmRegular,
                        color = ElementTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (enabled) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = CompoundIcons.Block(),
                    contentDescription = null,
                    tint = ElementTheme.colors.iconSecondary,
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}

@PreviewsDayNight
@Composable
internal fun BlockedUsersViewPreview(@PreviewParameter(BlockedUsersStateProvider::class) state: BlockedUsersState) {
    ElementPreview {
        BlockedUsersView(
            state = state,
            onBackClick = {},
        )
    }
}
