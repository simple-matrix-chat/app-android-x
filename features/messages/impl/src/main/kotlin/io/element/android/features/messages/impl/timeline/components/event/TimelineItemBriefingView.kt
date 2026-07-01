/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.event

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.ai.api.MomentAIBriefingPayload
import io.element.android.features.ai.api.MomentAIDailyDigestRoom
import io.element.android.features.ai.api.MomentAIDigestSkipped
import io.element.android.features.ai.api.MomentAIDigestWindow
import io.element.android.features.messages.impl.R
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemBriefingContent
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text

@Composable
fun TimelineItemBriefingView(
    content: TimelineItemBriefingContent,
    modifier: Modifier = Modifier,
) {
    val payload = content.payload
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(BriefingCardShape)
            .background(ElementTheme.colors.bgCanvasDefault)
            .border(1.dp, ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.6f), BriefingCardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BriefingHeader(totalMessages = payload.rooms.sumOf { room -> room.messageCount })
        if (payload.metaSummary.isNotBlank()) {
            BriefingMetaSummary(summary = payload.metaSummary)
        }
        payload.rooms.forEach { room ->
            BriefingRoomRow(room = room)
        }
        BriefingSkippedInfo(skipped = payload.skipped)
        Text(
            text = stringResource(R.string.screen_room_ai_disclaimer_full),
            style = ElementTheme.typography.fontBodyXsRegular,
            color = ElementTheme.colors.textDisabled,
        )
    }
}

@Composable
private fun BriefingHeader(
    totalMessages: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF786DFF), Color(0xFFB878FF)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                resourceId = R.drawable.ic_moment_sparkles,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.screen_room_ai_daily_briefing_title),
                style = ElementTheme.typography.fontBodyMdMedium,
                color = ElementTheme.colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.screen_room_ai_message_count, totalMessages),
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun BriefingMetaSummary(
    summary: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ElementTheme.colors.bgSubtleSecondary)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LabelRow(
            icon = CompoundIcons.Document(),
            title = stringResource(R.string.screen_room_ai_meta_summary),
            tint = ElementTheme.colors.iconSecondary,
        )
        Text(
            text = summary,
            style = ElementTheme.typography.fontBodySmRegular,
            color = ElementTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun BriefingRoomRow(
    room: MomentAIDailyDigestRoom,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = room.title.takeUnless { title -> title.isBlank() || title.startsWith("!") }
                    ?: stringResource(R.string.screen_room_ai_room_fallback),
                style = ElementTheme.typography.fontBodySmMedium,
                color = ElementTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (room.youMentioned) {
                MentionBadge()
            }
            KindBadge(kind = room.kind)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.screen_room_ai_message_count, room.messageCount),
                style = ElementTheme.typography.fontBodyXsRegular,
                color = ElementTheme.colors.textDisabled,
            )
        }
        Text(
            text = room.summary,
            style = ElementTheme.typography.fontBodySmRegular,
            color = ElementTheme.colors.textPrimary,
        )
        room.highlights.take(3).forEach { highlight ->
            HighlightRow(text = highlight)
        }
        room.alert?.takeIf { alert -> alert.isNotBlank() }?.let { alert ->
            AlertRow(text = alert)
        }
    }
}

@Composable
private fun HighlightRow(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(4.dp)
                .clip(CircleShape)
                .background(Color(0xFF786DFF))
        )
        Text(
            text = text,
            style = ElementTheme.typography.fontBodySmRegular,
            color = ElementTheme.colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AlertRow(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE67E22).copy(alpha = 0.1f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = CompoundIcons.Warning(),
            contentDescription = null,
            tint = Color(0xFFE67E22),
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            style = ElementTheme.typography.fontBodySmRegular,
            color = ElementTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BriefingSkippedInfo(
    skipped: MomentAIDigestSkipped,
    modifier: Modifier = Modifier,
) {
    if (skipped.encrypted <= 0 && skipped.noActivity <= 0) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ElementTheme.colors.bgSubtleSecondary)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = CompoundIcons.Lock(),
            contentDescription = null,
            tint = ElementTheme.colors.iconSecondary,
            modifier = Modifier.size(14.dp),
        )
        val encrypted = skipped.encrypted.takeIf { count -> count > 0 }
            ?.let { count -> stringResource(R.string.screen_room_ai_skipped_encrypted, count) }
        val noActivity = skipped.noActivity.takeIf { count -> count > 0 }
            ?.let { count -> stringResource(R.string.screen_room_ai_skipped_no_activity, count) }
        Text(
            text = listOfNotNull(encrypted, noActivity).joinToString(" / "),
            style = ElementTheme.typography.fontBodyXsRegular,
            color = ElementTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun LabelRow(
    icon: ImageVector,
    title: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = title,
            style = ElementTheme.typography.fontBodyXsMedium,
            color = ElementTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun MentionBadge(
    modifier: Modifier = Modifier,
) {
    Text(
        text = "@",
        style = ElementTheme.typography.fontBodyXsMedium,
        color = Color.White,
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF2F80ED))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun KindBadge(
    kind: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(
            if (kind == "dm") R.string.screen_room_ai_room_kind_dm else R.string.screen_room_ai_room_kind_group
        ),
        style = ElementTheme.typography.fontBodyXsRegular,
        color = ElementTheme.colors.textSecondary,
        modifier = modifier
            .clip(CircleShape)
            .background(ElementTheme.colors.bgSubtleSecondary)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

private val BriefingCardShape = RoundedCornerShape(12.dp)

@PreviewsDayNight
@Composable
internal fun TimelineItemBriefingViewPreview(
    @PreviewParameter(TimelineItemBriefingContentProvider::class) content: TimelineItemBriefingContent,
) = ElementPreview {
    TimelineItemBriefingView(
        content = content,
        modifier = Modifier.padding(16.dp),
    )
}

private class TimelineItemBriefingContentProvider : PreviewParameterProvider<TimelineItemBriefingContent> {
    override val values = sequenceOf(
        TimelineItemBriefingContent(
            body = "Daily briefing",
            formattedBody = "Daily briefing",
            isEdited = false,
            payload = MomentAIBriefingPayload(
                version = 1,
                generatedAt = "2026-07-01T09:00:00Z",
                window = MomentAIDigestWindow(from = "2026-06-30T22:00:00Z", to = "2026-07-01T09:00:00Z"),
                metaSummary = "The team aligned on release readiness and there are two follow-up items.",
                rooms = listOf(
                    MomentAIDailyDigestRoom(
                        roomId = "!room:example.org",
                        title = "Android",
                        kind = "group",
                        messageCount = 18,
                        summary = "Release blockers were reviewed and the new AI actions need final visual QA.",
                        highlights = listOf("Push the latest APK.", "Check the briefing room rendering."),
                        youMentioned = true,
                        alert = "One backend endpoint still returns an intermittent error.",
                    ),
                ),
                skipped = MomentAIDigestSkipped(encrypted = 1, noActivity = 2, filteredOut = 0),
                partial = false,
                model = "preview",
            ),
        )
    )
}
