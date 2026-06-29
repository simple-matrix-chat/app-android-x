/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.report

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
import io.element.android.features.messages.impl.R
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
fun ReportMessageView(
    state: ReportMessageState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackClick)

    val focusManager = LocalFocusManager.current
    val isSending = state.result is AsyncAction.Loading
    val canSend = state.reason.isNotBlank() && !isSending
    AsyncActionView(
        async = state.result,
        progressDialog = {},
        onSuccess = { onBackClick() },
        errorMessage = { stringResource(CommonStrings.error_unknown) },
        onErrorDismiss = { state.eventSink(ReportMessageEvent.ClearError) }
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
            MomentReportMessageTopBar(
                title = stringResource(CommonStrings.action_report_content),
                actionText = stringResource(CommonStrings.action_send),
                actionEnabled = canSend,
                onBackClick = onBackClick,
                onActionClick = {
                    if (canSend) {
                        focusManager.clearFocus(force = true)
                        state.eventSink(ReportMessageEvent.Report)
                    }
                },
            )

            MomentReportMessageCard {
                TextField(
                    value = state.reason,
                    onValueChange = { state.eventSink(ReportMessageEvent.UpdateReason(it)) },
                    placeholder = stringResource(R.string.screen_report_content_hint),
                    minLines = 4,
                    enabled = !isSending,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                )
                Text(
                    modifier = Modifier.padding(top = 10.dp),
                    text = stringResource(R.string.screen_report_content_explanation),
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary,
                )
            }

            MomentReportMessageToggleCard(
                title = stringResource(R.string.screen_report_content_block_user),
                subtitle = stringResource(R.string.screen_report_content_block_user_hint),
                checked = state.blockUser,
                enabled = !isSending,
                onToggle = { state.eventSink(ReportMessageEvent.ToggleBlockUser) },
            )
        }
    }
}

@Composable
private fun MomentReportMessageTopBar(
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
private fun MomentReportMessageCard(
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
private fun MomentReportMessageToggleCard(
    title: String,
    subtitle: String,
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
                .heightIn(min = 76.dp)
                .semantics(mergeDescendants = true) {}
                .clickable(enabled = enabled, onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = ElementTheme.typography.fontBodyLgMedium,
                    color = ElementTheme.colors.textPrimary,
                )
                Text(
                    text = subtitle,
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary,
                )
            }
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
internal fun ReportMessageViewPreview(@PreviewParameter(ReportMessageStateProvider::class) state: ReportMessageState) = ElementPreview {
    ReportMessageView(
        onBackClick = {},
        state = state,
    )
}
