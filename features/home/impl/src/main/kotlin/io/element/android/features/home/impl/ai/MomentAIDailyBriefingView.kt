/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.ai.api.MomentAIDailyBriefingResult
import io.element.android.features.ai.api.MomentAIDailyDigest
import io.element.android.features.ai.api.MomentAIDailyDigestRoom
import io.element.android.features.ai.api.MomentAIDigestSkipped
import io.element.android.features.ai.api.MomentAIDigestWindow
import io.element.android.features.home.impl.R
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.ButtonSize
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MomentAIDailyBriefingSheet(
    state: MomentAIDailyBriefingState,
    modifier: Modifier = Modifier,
) {
    if (!state.isVisible) return

    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    ModalBottomSheet(
        modifier = modifier,
        sheetState = sheetState,
        onDismissRequest = {
            coroutineScope.launch {
                sheetState.hide()
                state.eventSink(MomentAIDailyBriefingEvent.Dismiss)
            }
        },
        scrollable = true,
    ) {
        MomentAIDailyBriefingContent(
            state = state,
            onDismiss = {
                coroutineScope.launch {
                    sheetState.hide()
                    state.eventSink(MomentAIDailyBriefingEvent.Dismiss)
                }
            },
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun MomentAIDailyBriefingContent(
    state: MomentAIDailyBriefingState,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ElementTheme.colors.bgCanvasDefault)
            .padding(horizontal = 20.dp)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        MomentAIDailyBriefingHeader(onDismiss = onDismiss)
        when (val action = state.action) {
            AsyncData.Uninitialized -> MomentAIDailyBriefingIdle(state)
            is AsyncData.Loading -> MomentAIDailyBriefingLoading()
            is AsyncData.Failure -> MomentAIDailyBriefingError(state)
            is AsyncData.Success -> MomentAIDailyBriefingSuccess(
                result = action.data,
                onGenerateClick = {
                    state.eventSink(MomentAIDailyBriefingEvent.Generate(force = true))
                },
            )
        }
    }
}

@Composable
private fun MomentAIDailyBriefingHeader(
    onDismiss: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            resourceId = R.drawable.ic_moment_sparkles,
            contentDescription = null,
            tint = ElementTheme.colors.iconPrimary,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.screen_home_ai_daily_briefing_title),
            style = ElementTheme.typography.fontBodyLgMedium.copy(fontWeight = FontWeight.Bold),
            color = ElementTheme.colors.textPrimary,
        )
        IconButton(
            modifier = Modifier.size(36.dp),
            onClick = onDismiss,
        ) {
            Icon(
                imageVector = CompoundIcons.Close(),
                contentDescription = stringResource(CommonStrings.action_close),
                tint = ElementTheme.colors.iconSecondary,
            )
        }
    }
}

@Composable
private fun MomentAIDailyBriefingIdle(
    state: MomentAIDailyBriefingState,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.screen_home_ai_daily_briefing_description),
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
        )
        Button(
            text = stringResource(R.string.screen_home_ai_daily_briefing_generate),
            size = ButtonSize.Large,
            onClick = {
                state.eventSink(MomentAIDailyBriefingEvent.Generate(force = true))
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MomentAIDailyBriefingLoading() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
        )
        Text(
            text = stringResource(R.string.screen_home_ai_daily_briefing_generating),
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun MomentAIDailyBriefingError(
    state: MomentAIDailyBriefingState,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.screen_home_ai_daily_briefing_error),
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textCriticalPrimary,
        )
        Button(
            text = stringResource(CommonStrings.action_retry),
            size = ButtonSize.Large,
            onClick = {
                state.eventSink(MomentAIDailyBriefingEvent.Generate(force = true))
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MomentAIDailyBriefingSuccess(
    result: MomentAIDailyBriefingResult,
    onGenerateClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = if (result.digest.rooms.isEmpty()) {
                stringResource(R.string.screen_home_ai_daily_briefing_empty)
            } else {
                stringResource(R.string.screen_home_ai_daily_briefing_posted)
            },
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textPrimary,
        )
        Text(
            text = stringResource(R.string.screen_home_ai_daily_briefing_rooms_count, result.digest.rooms.size),
            style = ElementTheme.typography.fontBodySmRegular,
            color = ElementTheme.colors.textSecondary,
        )
        Button(
            text = stringResource(R.string.screen_home_ai_daily_briefing_generate),
            size = ButtonSize.Large,
            onClick = onGenerateClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@PreviewsDayNight
@Composable
internal fun MomentAIDailyBriefingSheetPreview() = ElementPreview {
    MomentAIDailyBriefingContent(
        state = aMomentAIDailyBriefingState(
            isVisible = true,
            action = AsyncData.Success(
                MomentAIDailyBriefingResult(
                    digest = aMomentAIDailyDigest(),
                    roomId = "!briefing:server",
                    eventId = "\$event",
                    posted = true,
                )
            ),
        ),
        onDismiss = {},
    )
}

private fun aMomentAIDailyDigest(): MomentAIDailyDigest {
    return MomentAIDailyDigest(
        generatedAt = "2026-07-01T10:00:00Z",
        window = MomentAIDigestWindow(
            from = "2026-06-30T22:00:00+03:00",
            to = "2026-07-01T10:00:00+03:00",
        ),
        metaSummary = "Three chats had updates since last night.",
        rooms = listOf(
            MomentAIDailyDigestRoom(
                roomId = "!room:server",
                title = "Design",
                kind = "group",
                messageCount = 12,
                summary = "The team aligned on the daily briefing entry point.",
                highlights = emptyList(),
                youMentioned = false,
                alert = null,
            )
        ),
        skipped = MomentAIDigestSkipped(
            encrypted = 0,
            noActivity = 0,
            filteredOut = 0,
        ),
        model = "preview",
        partial = false,
    )
}
