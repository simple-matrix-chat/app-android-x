/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.knockrequests.impl.list

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.knockrequests.impl.R
import io.element.android.features.knockrequests.impl.data.KnockRequestPresentable
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.components.BigIcon
import io.element.android.libraries.designsystem.components.ProgressDialog
import io.element.android.libraries.designsystem.components.async.AsyncActionView
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.components.dialogs.ConfirmationDialog
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.text.toDp
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.ButtonSize
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.OutlinedButton
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList

@Composable
fun KnockRequestsListView(
    state: KnockRequestsListState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = ElementTheme.colors.bgCanvasDefault,
        topBar = {
            KnockRequestsListTopBar(onBackClick = onBackClick)
        },
        content = { padding ->
            KnockRequestsListContent(
                state = state,
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
            )
        }
    )
}

@Composable
private fun KnockRequestsListContent(
    state: KnockRequestsListState,
    modifier: Modifier = Modifier,
) {
    fun onAcceptClick(knockRequest: KnockRequestPresentable) {
        state.eventSink(KnockRequestsListEvents.Accept(knockRequest))
    }

    fun onDeclineClick(knockRequest: KnockRequestPresentable) {
        state.eventSink(KnockRequestsListEvents.Decline(knockRequest))
    }

    fun onBanClick(knockRequest: KnockRequestPresentable) {
        state.eventSink(KnockRequestsListEvents.DeclineAndBan(knockRequest))
    }

    var bottomPaddingInPixels by remember { mutableIntStateOf(0) }

    Box(modifier.fillMaxSize()) {
        when (state.knockRequests) {
            is AsyncData.Success -> {
                val knockRequests = state.knockRequests.data
                if (knockRequests.isEmpty()) {
                    KnockRequestsEmptyList()
                } else {
                    KnockRequestsList(
                        knockRequests = knockRequests,
                        canAccept = state.permissions.canAccept,
                        canDecline = state.permissions.canDecline,
                        canBan = state.permissions.canBan,
                        onAcceptClick = ::onAcceptClick,
                        onDeclineClick = ::onDeclineClick,
                        onBanClick = ::onBanClick,
                        bottomContentPadding = bottomPaddingInPixels.toDp(),
                    )
                }
            }
            is AsyncData.Loading -> Unit
            else -> Unit
        }
        KnockRequestsActionsView(
            currentAction = state.currentAction,
            asyncAction = state.asyncAction,
            onConfirm = {
                state.eventSink(KnockRequestsListEvents.ConfirmCurrentAction)
            },
            onRetry = {
                state.eventSink(KnockRequestsListEvents.RetryCurrentAction)
            },
            onDismiss = {
                state.eventSink(KnockRequestsListEvents.ResetCurrentAction)
            },
        )
        if (state.canAcceptAll) {
            KnockRequestsAcceptAll(
                onClick = {
                    state.eventSink(KnockRequestsListEvents.AcceptAll)
                },
                onHeightChange = { height ->
                    bottomPaddingInPixels = height
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun KnockRequestsActionsView(
    currentAction: KnockRequestsAction,
    asyncAction: AsyncAction<Unit>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        AsyncActionView(
            async = asyncAction,
            onSuccess = { onDismiss() },
            onErrorDismiss = onDismiss,
            confirmationDialog = {
                KnockRequestActionConfirmation(
                    currentAction = currentAction,
                    onSubmit = onConfirm,
                    onDismiss = onDismiss,
                )
            },
            progressDialog = {
                KnockRequestActionProgress(target = currentAction)
            },
            errorMessage = {
                when (currentAction) {
                    is KnockRequestsAction.Accept -> stringResource(R.string.screen_knock_requests_list_accept_failed_alert_description)
                    is KnockRequestsAction.Decline -> stringResource(R.string.screen_knock_requests_list_decline_failed_alert_description)
                    is KnockRequestsAction.DeclineAndBan -> stringResource(R.string.screen_knock_requests_list_decline_failed_alert_description)
                    KnockRequestsAction.AcceptAll -> stringResource(R.string.screen_knock_requests_list_accept_all_failed_alert_description)
                    else -> ""
                }
            },
            onRetry = onRetry,
        )
    }
}

@Composable
private fun KnockRequestActionConfirmation(
    currentAction: KnockRequestsAction,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (title, content, submitText) = when (currentAction) {
        KnockRequestsAction.AcceptAll -> Triple(
            stringResource(R.string.screen_knock_requests_list_accept_all_alert_title),
            stringResource(R.string.screen_knock_requests_list_accept_all_alert_description),
            stringResource(R.string.screen_knock_requests_list_accept_all_alert_confirm_button_title),
        )
        is KnockRequestsAction.Decline -> Triple(
            stringResource(R.string.screen_knock_requests_list_decline_alert_title),
            stringResource(R.string.screen_knock_requests_list_decline_alert_description, currentAction.knockRequest.getBestName()),
            stringResource(R.string.screen_knock_requests_list_decline_alert_confirm_button_title),
        )
        is KnockRequestsAction.DeclineAndBan -> Triple(
            stringResource(R.string.screen_knock_requests_list_ban_alert_title),
            stringResource(R.string.screen_knock_requests_list_ban_alert_description, currentAction.knockRequest.getBestName()),
            stringResource(R.string.screen_knock_requests_list_ban_alert_confirm_button_title),
        )
        else -> return
    }
    ConfirmationDialog(
        title = title,
        content = content,
        submitText = submitText,
        onSubmitClick = onSubmit,
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

@Composable
private fun KnockRequestActionProgress(
    target: KnockRequestsAction,
    modifier: Modifier = Modifier,
) {
    val progressText = when (target) {
        is KnockRequestsAction.Accept -> stringResource(R.string.screen_knock_requests_list_accept_loading_title)
        is KnockRequestsAction.Decline -> stringResource(R.string.screen_knock_requests_list_decline_loading_title)
        is KnockRequestsAction.DeclineAndBan -> stringResource(R.string.screen_knock_requests_list_ban_loading_title)
        KnockRequestsAction.AcceptAll -> stringResource(R.string.screen_knock_requests_list_accept_all_loading_title)
        else -> return
    }
    ProgressDialog(
        text = progressText,
        modifier = modifier,
    )
}

@Composable
private fun KnockRequestsList(
    knockRequests: ImmutableList<KnockRequestPresentable>,
    canAccept: Boolean,
    canDecline: Boolean,
    canBan: Boolean,
    onAcceptClick: (KnockRequestPresentable) -> Unit,
    onDeclineClick: (KnockRequestPresentable) -> Unit,
    onBanClick: (KnockRequestPresentable) -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 40.dp, bottom = bottomContentPadding),
    ) {
        itemsIndexed(knockRequests) { index, knockRequest ->
            KnockRequestItem(
                knockRequest = knockRequest,
                onAcceptClick = onAcceptClick,
                canBan = canBan,
                canDecline = canDecline,
                canAccept = canAccept,
                onDeclineClick = onDeclineClick,
                onBanClick = onBanClick,
                showDivider = index != knockRequests.size - 1,
            )
        }
    }
}

@Composable
private fun KnockRequestItem(
    knockRequest: KnockRequestPresentable,
    canAccept: Boolean,
    canDecline: Boolean,
    canBan: Boolean,
    onAcceptClick: (KnockRequestPresentable) -> Unit,
    onDeclineClick: (KnockRequestPresentable) -> Unit,
    onBanClick: (KnockRequestPresentable) -> Unit,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ElementTheme.colors.bgCanvasDefault)
            .padding(top = 16.dp)
            .padding(start = 16.dp)
    ) {
        Avatar(
            avatarData = knockRequest.getAvatarData(AvatarSize.KnockRequestItem),
            avatarType = AvatarType.User,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            // Name and date
            Row {
                Text(
                    modifier = Modifier
                        .clipToBounds()
                        .weight(1f),
                    text = knockRequest.getBestName(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = ElementTheme.colors.textPrimary,
                    style = ElementTheme.typography.fontBodyLgMedium,
                )
                val formattedDate = knockRequest.formattedDate
                if (!formattedDate.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedDate,
                        color = ElementTheme.colors.textSecondary,
                        style = ElementTheme.typography.fontBodySmRegular,
                    )
                }
            }
            // UserId
            if (!knockRequest.displayName.isNullOrEmpty()) {
                Text(
                    text = knockRequest.userId.value,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = ElementTheme.typography.fontBodyMdRegular,
                )
            }
            // Reason
            val reason = knockRequest.reason
            if (!reason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                var isExpanded by rememberSaveable(knockRequest.userId) { mutableStateOf(false) }
                var isExpandable by rememberSaveable(knockRequest.userId) { mutableStateOf(false) }
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .animateContentSize()
                        .clickable(enabled = isExpandable) { isExpanded = !isExpanded }
                ) {
                    Text(
                        text = reason,
                        style = ElementTheme.typography.fontBodyMdRegular,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                        onTextLayout = { result ->
                            if (!isExpanded && result.hasVisualOverflow) {
                                isExpandable = true
                            }
                        },
                        overflow = TextOverflow.Ellipsis,
                        color = ElementTheme.colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Box(modifier = Modifier.size(24.dp)) {
                        if (isExpandable) {
                            Icon(
                                imageVector = if (isExpanded) CompoundIcons.ChevronUp() else CompoundIcons.ChevronDown(),
                                contentDescription = null,
                                tint = ElementTheme.colors.iconTertiary,
                            )
                        }
                    }
                }
            }
            // Actions
            if (canDecline || canAccept) {
                Spacer(modifier = Modifier.height(16.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (canDecline) {
                    OutlinedButton(
                        text = stringResource(CommonStrings.action_decline),
                        onClick = {
                            onDeclineClick(knockRequest)
                        },
                        size = ButtonSize.MediumLowPadding,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (canAccept) {
                    Button(
                        text = stringResource(CommonStrings.action_accept),
                        onClick = {
                            onAcceptClick(knockRequest)
                        },
                        size = ButtonSize.MediumLowPadding,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (canBan) {
                Spacer(modifier = Modifier.height(if (canDecline || canAccept) 16.dp else 4.dp))
                TextButton(
                    text = stringResource(R.string.screen_knock_requests_list_decline_and_ban_action_title),
                    onClick = {
                        onBanClick(knockRequest)
                    },
                    destructive = true,
                    size = ButtonSize.Small,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (showDivider) {
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun KnockRequestsAcceptAll(
    onClick: () -> Unit,
    onHeightChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(elevation = 24.dp, spotColor = Color.Transparent)
            .background(color = ElementTheme.colors.bgCanvasDefault)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 4.dp)
            .onSizeChanged { onHeightChange(it.height) }
    ) {
        OutlinedButton(
            text = stringResource(R.string.screen_knock_requests_list_accept_all_button_title),
            onClick = onClick,
            size = ButtonSize.Medium,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun KnockRequestsEmptyList(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp)
            .padding(top = 53.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BigIcon(style = BigIcon.Style.Default(CompoundIcons.AskToJoin()))
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = stringResource(R.string.screen_knock_requests_list_empty_state_title),
            style = ElementTheme.typography.fontHeadingMdBold,
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = stringResource(R.string.screen_knock_requests_list_empty_state_description),
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun KnockRequestsListTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 20.dp),
    ) {
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            BackButton(onClick = onBackClick)
        }
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 72.dp),
            text = stringResource(R.string.screen_knock_requests_list_title),
            style = ElementTheme.typography.fontBodyLgMedium,
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@PreviewsDayNight
@Composable
internal fun KnockRequestsListViewPreview(
    @PreviewParameter(KnockRequestsListStateProvider::class) state: KnockRequestsListState
) = ElementPreview {
    KnockRequestsListView(
        state = state,
        onBackClick = {},
    )
}
