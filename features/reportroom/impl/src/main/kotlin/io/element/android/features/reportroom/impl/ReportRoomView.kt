/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.reportroom.impl

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.components.async.AsyncActionView
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextField
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun ReportRoomView(
    state: ReportRoomState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackClick)

    val focusManager = LocalFocusManager.current

    val isReporting = state.reportAction is AsyncAction.Loading
    val canReport = state.canReport && !isReporting
    AsyncActionView(
        async = state.reportAction,
        onSuccess = { onBackClick() },
        errorTitle = { failure ->
            when (failure) {
                is ReportRoom.Exception.LeftRoomFailed -> stringResource(R.string.screen_report_room_leave_failed_alert_title)
                else -> stringResource(CommonStrings.dialog_title_error)
            }
        },
        errorMessage = { failure ->
            when (failure) {
                is ReportRoom.Exception.LeftRoomFailed -> stringResource(R.string.screen_report_room_leave_failed_alert_message)
                else -> stringResource(CommonStrings.error_unknown)
            }
        },
        onRetry = {
            state.eventSink(ReportRoomEvents.Report)
        },
        onErrorDismiss = { state.eventSink(ReportRoomEvents.ClearReportAction) }
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ElementTheme.colors.bgSubtleSecondary,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MomentReportRoomTopBar(
                title = stringResource(R.string.screen_report_room_title),
                actionText = stringResource(CommonStrings.action_report),
                actionEnabled = canReport,
                onBackClick = onBackClick,
                onActionClick = {
                    if (canReport) {
                        focusManager.clearFocus(force = true)
                        state.eventSink(ReportRoomEvents.Report)
                    }
                },
            )

            MomentReportRoomCard {
                TextField(
                    value = state.reason,
                    onValueChange = { state.eventSink(ReportRoomEvents.UpdateReason(it)) },
                    placeholder = stringResource(R.string.screen_report_room_reason_placeholder),
                    minLines = 4,
                    enabled = !isReporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                )
                Text(
                    modifier = Modifier.padding(top = 10.dp),
                    text = stringResource(R.string.screen_report_room_reason_footer),
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary,
                )
            }

            MomentReportRoomToggleCard(
                title = stringResource(CommonStrings.action_leave_room),
                checked = state.leaveRoom,
                enabled = !isReporting,
                onToggle = { state.eventSink(ReportRoomEvents.ToggleLeaveRoom) },
            )
        }
    }
}

@Composable
private fun MomentReportRoomTopBar(
    title: String,
    actionText: String,
    actionEnabled: Boolean,
    onBackClick: () -> Unit,
    onActionClick: () -> Unit,
) {
    val backContentDescription = stringResource(CommonStrings.action_back)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .height(44.dp)
                .clickable(onClick = onBackClick)
                .semantics { contentDescription = backContentDescription }
                .padding(end = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = stringResource(CommonStrings.action_cancel),
                style = ElementTheme.typography.fontBodyLgMedium,
                color = ElementTheme.colors.textActionAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 84.dp),
            text = title,
            style = ElementTheme.typography.fontHeadingSmMedium,
            color = ElementTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(44.dp)
                .clickable(onClick = onActionClick)
                .padding(start = 12.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = actionText,
                style = ElementTheme.typography.fontBodyLgMedium,
                color = if (actionEnabled) ElementTheme.colors.textActionAccent else ElementTheme.colors.textDisabled,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MomentReportRoomCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content,
        )
    }
}

@Composable
private fun MomentReportRoomToggleCard(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .semantics(mergeDescendants = true) {}
                .clickable(enabled = enabled, onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = ElementTheme.typography.fontBodyLgMedium,
                color = ElementTheme.colors.textPrimary,
            )
            Switch(
                enabled = enabled,
                checked = checked,
                onCheckedChange = { onToggle() },
            )
        }
    }
}

@PreviewsDayNight
@Composable
internal fun ReportRoomViewPreview(
    @PreviewParameter(ReportRoomStateProvider::class) state: ReportRoomState
) = ElementPreview {
    ReportRoomView(
        state = state,
        onBackClick = {},
    )
}
