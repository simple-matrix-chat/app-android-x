/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.ai.api.MomentAIDailyBriefingResult
import io.element.android.features.ai.api.MomentAIDailyDigest
import io.element.android.features.ai.api.MomentAIDailyDigestRoom
import io.element.android.features.ai.api.MomentAIDigestSkipped
import io.element.android.features.ai.api.MomentAIDigestWindow
import io.element.android.features.messages.impl.R
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text

@Composable
internal fun MomentAIDailyBriefingComposerHint(
    state: MomentAIDailyBriefingComposerState,
    onGenerateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLoading = state.action.isLoading()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ElementTheme.colors.bgCanvasDefault)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF7871FF),
                            Color(0xFFB579FF),
                        )
                    )
                )
                .clickable(enabled = !isLoading, onClick = onGenerateClick)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(18.dp),
                    resourceId = R.drawable.ic_moment_sparkles,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
            Text(
                text = stringResource(
                    if (isLoading) {
                        R.string.screen_room_ai_daily_briefing_generating
                    } else {
                        R.string.screen_room_ai_daily_briefing_generate
                    }
                ),
                style = ElementTheme.typography.fontBodyMdMedium,
                color = Color.White,
            )
        }

        val status = when (state.action) {
            is AsyncData.Failure -> stringResource(R.string.screen_room_ai_daily_briefing_error)
            is AsyncData.Success -> stringResource(R.string.screen_room_ai_daily_briefing_posted)
            else -> null
        }
        status?.let {
            Text(
                text = it,
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@PreviewsDayNight
@Composable
internal fun MomentAIDailyBriefingComposerHintPreview(
    @PreviewParameter(MomentAIDailyBriefingComposerStateProvider::class) state: MomentAIDailyBriefingComposerState
) = ElementPreview {
    MomentAIDailyBriefingComposerHint(
        state = state,
        onGenerateClick = {},
    )
}

private class MomentAIDailyBriefingComposerStateProvider : PreviewParameterProvider<MomentAIDailyBriefingComposerState> {
    override val values: Sequence<MomentAIDailyBriefingComposerState>
        get() = sequenceOf(
            MomentAIDailyBriefingComposerState(
                isVisible = true,
                action = AsyncData.Uninitialized,
            ),
            MomentAIDailyBriefingComposerState(
                isVisible = true,
                action = AsyncData.Loading(),
            ),
            MomentAIDailyBriefingComposerState(
                isVisible = true,
                action = AsyncData.Success(aMomentAIDailyBriefingResult()),
            ),
            MomentAIDailyBriefingComposerState(
                isVisible = true,
                action = AsyncData.Failure(IllegalStateException()),
            ),
        )
}

private fun aMomentAIDailyBriefingResult(): MomentAIDailyBriefingResult {
    return MomentAIDailyBriefingResult(
        digest = MomentAIDailyDigest(
            generatedAt = "2026-07-01T10:00:00Z",
            window = MomentAIDigestWindow(
                from = "2026-06-30T22:00:00+03:00",
                to = "2026-07-01T10:00:00+03:00",
            ),
            metaSummary = "Three chats had updates since last night.",
            rooms = listOf(
                MomentAIDailyDigestRoom(
                    roomId = "!room:server",
                    title = "Project room",
                    kind = "group",
                    messageCount = 12,
                    summary = "The team agreed on the next milestone.",
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
            model = "moment-ai",
            partial = false,
        ),
        roomId = "!briefing:server",
        eventId = "\$event",
        posted = true,
    )
}
