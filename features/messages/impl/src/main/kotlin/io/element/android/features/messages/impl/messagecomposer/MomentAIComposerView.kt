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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    var activeDrillDown by rememberSaveable { mutableStateOf<MomentAIDrillDown?>(null) }

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
                    activeDrillDown = activeDrillDown,
                    onSelectMode = onSelectMode,
                    onOpenDrillDown = { activeDrillDown = it },
                    onCloseDrillDown = { activeDrillDown = null },
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
    activeDrillDown: MomentAIDrillDown?,
    onSelectMode: (String) -> Unit,
    onOpenDrillDown: (MomentAIDrillDown) -> Unit,
    onCloseDrillDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mainModes = listOf(
        MomentAIMode("fix", stringResource(R.string.screen_room_ai_fix), mode = "fix_grammar"),
        MomentAIMode("shorter", stringResource(R.string.screen_room_ai_shorter), mode = "shorter"),
        MomentAIMode("longer", stringResource(R.string.screen_room_ai_longer), mode = "longer"),
        MomentAIMode("style", stringResource(R.string.screen_room_ai_style), drillDown = MomentAIDrillDown.Style),
        MomentAIMode("translate", stringResource(R.string.screen_room_ai_translate), drillDown = MomentAIDrillDown.Translate),
    )
    val styleModes = listOf(
        MomentAIMode("formal", stringResource(R.string.screen_room_ai_formal), mode = "formal"),
        MomentAIMode("corporate", stringResource(R.string.screen_room_ai_corporate), mode = "corporate"),
        MomentAIMode("friendly", stringResource(R.string.screen_room_ai_friendly), mode = "friendly"),
        MomentAIMode("old_russian", stringResource(R.string.screen_room_ai_old_russian), mode = "old_russian"),
    )
    val translateModes = listOf(
        MomentAIMode("translate_en", stringResource(R.string.screen_room_ai_translate_english), mode = "translate_en"),
        MomentAIMode("translate_ru", stringResource(R.string.screen_room_ai_translate_russian), mode = "translate_ru"),
        MomentAIMode("translate_kk", stringResource(R.string.screen_room_ai_translate_kazakh), mode = "translate_kk"),
        MomentAIMode("translate_uz", stringResource(R.string.screen_room_ai_translate_uzbek), mode = "translate_uz"),
        MomentAIMode("translate_be", stringResource(R.string.screen_room_ai_translate_belarusian), mode = "translate_be"),
        MomentAIMode("translate_ky", stringResource(R.string.screen_room_ai_translate_kyrgyz), mode = "translate_ky"),
        MomentAIMode("translate_tg", stringResource(R.string.screen_room_ai_translate_tajik), mode = "translate_tg"),
        MomentAIMode("translate_hy", stringResource(R.string.screen_room_ai_translate_armenian), mode = "translate_hy"),
        MomentAIMode("translate_ka", stringResource(R.string.screen_room_ai_translate_georgian), mode = "translate_ka"),
        MomentAIMode("translate_tr", stringResource(R.string.screen_room_ai_translate_turkish), mode = "translate_tr"),
    )

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (activeDrillDown != null) {
            IconButton(onClick = onCloseDrillDown) {
                Icon(
                    imageVector = CompoundIcons.ChevronLeft(),
                    contentDescription = stringResource(CommonStrings.action_back),
                    tint = ElementTheme.colors.iconSecondary,
                )
            }
        }
        val visibleModes = when (activeDrillDown) {
            MomentAIDrillDown.Style -> styleModes
            MomentAIDrillDown.Translate -> translateModes
            null -> mainModes
        }
        visibleModes.forEach { mode ->
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ElementTheme.colors.bgSubtleSecondary)
                    .clickable {
                        when {
                            mode.drillDown != null -> onOpenDrillDown(mode.drillDown)
                            mode.mode != null -> onSelectMode(mode.mode)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = mode.label,
                    style = ElementTheme.typography.fontBodySmMedium,
                    color = ElementTheme.colors.textPrimary,
                )
                if (mode.drillDown != null) {
                    Icon(
                        imageVector = CompoundIcons.ChevronRight(),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = ElementTheme.colors.iconSecondary,
                    )
                }
            }
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
    val mode: String? = null,
    val drillDown: MomentAIDrillDown? = null,
)

private enum class MomentAIDrillDown {
    Style,
    Translate,
}
