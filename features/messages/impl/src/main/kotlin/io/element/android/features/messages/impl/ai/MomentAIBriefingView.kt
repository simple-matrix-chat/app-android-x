/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.messages.impl.MomentAIBriefingState
import io.element.android.features.messages.impl.R
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.ui.strings.CommonStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MomentAIBriefingBottomSheet(
    state: MomentAIBriefingState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isVisible) {
        ModalBottomSheet(
            modifier = modifier
                .systemBarsPadding()
                .navigationBarsPadding(),
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onDismissRequest = onDismiss,
            scrollable = true,
        ) {
            MomentAIBriefingContent(
                state = state,
                onDismiss = onDismiss,
                onRetry = onRetry,
                modifier = Modifier
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun MomentAIBriefingContent(
    state: MomentAIBriefingState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MomentAIBriefingIcon()
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.screen_room_ai_briefing_title),
                style = ElementTheme.typography.fontHeadingMdBold,
                color = ElementTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = CompoundIcons.Close(),
                    contentDescription = stringResource(CommonStrings.action_close),
                    tint = ElementTheme.colors.iconSecondary,
                )
            }
        }

        when {
            state.isLoading -> MomentAIBriefingLoading()
            state.errorMessageResId != null -> MomentAIBriefingError(
                message = stringResource(state.errorMessageResId),
                onRetry = onRetry,
            )
            state.briefing != null -> MomentAIBriefingCard(briefing = state.briefing)
        }
    }
}

@Composable
private fun MomentAIBriefingIcon() {
    Icon(
        resourceId = R.drawable.ic_moment_sparkles,
        contentDescription = null,
        tint = ElementTheme.colors.iconAccentPrimary,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(ElementTheme.colors.bgAccentSelected)
            .padding(8.dp),
    )
}

@Composable
private fun MomentAIBriefingLoading() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ElementTheme.colors.bgSubtleSecondary)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
        )
        Text(
            text = stringResource(R.string.screen_room_ai_briefing_loading),
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun MomentAIBriefingError(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ElementTheme.colors.bgCriticalSubtle)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = CompoundIcons.Warning(),
                contentDescription = null,
                tint = ElementTheme.colors.iconCriticalPrimary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = message,
                style = ElementTheme.typography.fontBodyMdMedium,
                color = ElementTheme.colors.textCriticalPrimary,
            )
        }
        TextButton(
            text = stringResource(R.string.screen_room_ai_briefing_retry),
            onClick = onRetry,
        )
    }
}

@Composable
private fun MomentAIBriefingCard(
    briefing: MomentAIRoomBriefing,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ElementTheme.colors.bgSubtleSecondary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MomentAIBriefingSection(
            title = stringResource(R.string.screen_room_ai_briefing_summary),
            body = briefing.summary,
        )
        if (briefing.decisions.isNotEmpty()) {
            MomentAIBriefingSection(
                title = stringResource(R.string.screen_room_ai_briefing_decisions),
                items = briefing.decisions,
            )
        }
        if (briefing.actionItems.isNotEmpty()) {
            MomentAIBriefingSection(
                title = stringResource(R.string.screen_room_ai_briefing_tasks),
                items = briefing.actionItems,
            )
        }
        briefing.alert?.let { alert ->
            MomentAIBriefingSection(
                title = stringResource(R.string.screen_room_ai_briefing_alert),
                body = alert,
            )
        }
        HorizontalDivider()
        Text(
            text = stringResource(R.string.screen_room_ai_disclaimer),
            style = ElementTheme.typography.fontBodyXsRegular,
            color = ElementTheme.colors.textDisabled,
        )
    }
}

@Composable
private fun MomentAIBriefingSection(
    title: String,
    body: String? = null,
    items: List<String> = emptyList(),
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = ElementTheme.typography.fontBodyMdMedium,
            color = ElementTheme.colors.textPrimary,
        )
        body?.let {
            Text(
                text = it,
                style = ElementTheme.typography.fontBodyMdRegular,
                color = ElementTheme.colors.textSecondary,
            )
        }
        items.forEachIndexed { index, item ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${index + 1}.",
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                )
                Text(
                    text = item,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@PreviewsDayNight
@Composable
internal fun MomentAIBriefingContentPreview(
    @PreviewParameter(MomentAIBriefingStateProvider::class) state: MomentAIBriefingState,
) = ElementPreview {
    MomentAIBriefingContent(
        state = state,
        onDismiss = {},
        onRetry = {},
        modifier = Modifier.padding(20.dp),
    )
}

private class MomentAIBriefingStateProvider : PreviewParameterProvider<MomentAIBriefingState> {
    override val values = sequenceOf(
        MomentAIBriefingState.Default.copy(
            isVisible = true,
            isLoading = true,
        ),
        MomentAIBriefingState.Default.copy(
            isVisible = true,
            errorMessageResId = R.string.screen_room_ai_briefing_error,
        ),
        MomentAIBriefingState.Default.copy(
            isVisible = true,
            briefing = MomentAIRoomBriefing(
                summary = "The room discussed the next Android release and agreed to keep call support visible.",
                decisions = listOf("Keep calls in the room header.", "Match the iOS AI briefing entry point."),
                actionItems = listOf("Check the Android header on small screens.", "Verify the briefing API response."),
                alert = "There are open backend errors to monitor.",
            ),
        ),
    )
}
