/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.messagecomposer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.messages.impl.R
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.ButtonSize
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.OutlinedButton
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
internal fun MomentAIComposerPanel(
    state: MomentAIComposerState,
    onSelectMode: (String) -> Unit,
    onReplace: (String) -> Unit,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .background(ElementTheme.colors.bgCanvasDefault)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.quickMode) {
                Text(
                    text = stringResource(R.string.screen_room_ai_disclaimer),
                    style = ElementTheme.typography.fontBodyXsRegular,
                    color = ElementTheme.colors.textDisabled,
                    modifier = Modifier.weight(1f),
                )
            } else {
                MomentAIModesRow(
                    onSelectMode = onSelectMode,
                    modifier = Modifier.weight(1f),
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = CompoundIcons.Close(),
                    contentDescription = stringResource(CommonStrings.action_close),
                    tint = ElementTheme.colors.iconSecondary,
                )
            }
        }

        state.errorMessageResId?.let { messageResId ->
            Text(
                text = stringResource(messageResId),
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textCriticalPrimary,
            )
        }

        if (state.isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(
                    text = stringResource(R.string.screen_room_ai_generating),
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary,
                )
            }
        }

        state.result?.let { result ->
            MomentAIResultPreview(
                result = result,
                onReplace = onReplace,
                onSend = onSend,
            )
        }

        if (!state.quickMode) {
            Text(
                text = stringResource(R.string.screen_room_ai_disclaimer),
                style = ElementTheme.typography.fontBodyXsRegular,
                color = ElementTheme.colors.textDisabled,
            )
        }
    }
}

@Composable
private fun MomentAIModesRow(
    onSelectMode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = listOf(
        MomentAIMode("fix_grammar", stringResource(R.string.screen_room_ai_fix)),
        MomentAIMode("shorter", stringResource(R.string.screen_room_ai_shorter)),
        MomentAIMode("longer", stringResource(R.string.screen_room_ai_longer)),
        MomentAIMode("formal", stringResource(R.string.screen_room_ai_formal)),
        MomentAIMode("friendly", stringResource(R.string.screen_room_ai_friendly)),
        MomentAIMode("translate_en", stringResource(R.string.screen_room_ai_translate_english)),
        MomentAIMode("translate_ru", stringResource(R.string.screen_room_ai_translate_russian)),
    )
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        modes.forEach { mode ->
            Text(
                text = mode.label,
                style = ElementTheme.typography.fontBodySmMedium,
                color = ElementTheme.colors.textPrimary,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ElementTheme.colors.bgSubtleSecondary)
                    .clickable { onSelectMode(mode.id) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun MomentAIResultPreview(
    result: String,
    onReplace: (String) -> Unit,
    onSend: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = result,
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ElementTheme.colors.bgSubtleSecondary)
                .padding(12.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                text = stringResource(R.string.screen_room_ai_replace),
                onClick = { onReplace(result) },
                size = ButtonSize.Small,
            )
            OutlinedButton(
                text = stringResource(R.string.screen_room_ai_send),
                onClick = { onSend(result) },
                size = ButtonSize.Small,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MomentAIWandButton(
    isActive: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (isActive) Color(0xFF7C3AED) else Color(0xFF8B5CF6)
    Row(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = "✦",
                style = ElementTheme.typography.fontBodyLgMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private data class MomentAIMode(
    val id: String,
    val label: String,
)
