/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.messages.impl.R
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
internal fun MomentAIMessageActionCards(
    state: MomentAIMessageActionState?,
    onDismissFactCheck: () -> Unit,
    onRetryFactCheck: () -> Unit,
    onDismissSummary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state == null || !state.isVisible) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (val factCheckState = state.factCheck) {
            MomentAIFactCheckUiState.Hidden -> Unit
            MomentAIFactCheckUiState.Loading -> MomentAILoadingCard(
                icon = { FactCheckIcon(tint = ElementTheme.colors.iconInfoPrimary) },
                text = stringResource(R.string.screen_room_ai_fact_check_loading),
            )
            is MomentAIFactCheckUiState.Error -> MomentAIErrorCard(
                message = stringResource(factCheckState.messageResId),
                onRetry = onRetryFactCheck,
            )
            is MomentAIFactCheckUiState.Success -> MomentAIFactCheckCard(
                result = factCheckState.result,
                onDismiss = onDismissFactCheck,
            )
        }

        when (val summaryState = state.summary) {
            MomentAISummaryUiState.Hidden -> Unit
            MomentAISummaryUiState.Loading -> MomentAILoadingCard(
                icon = {
                    Icon(
                        imageVector = CompoundIcons.Quote(),
                        contentDescription = null,
                        tint = ElementTheme.colors.iconInfoPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                },
                text = stringResource(R.string.screen_room_ai_summary_loading),
            )
            is MomentAISummaryUiState.Error -> MomentAIErrorCard(
                message = stringResource(summaryState.messageResId),
                onRetry = null,
            )
            is MomentAISummaryUiState.Success -> MomentAISummaryCard(
                summary = summaryState.summary,
                onDismiss = onDismissSummary,
            )
        }
    }
}

@Composable
private fun MomentAILoadingCard(
    icon: @Composable () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
) {
    MomentAICard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = ElementTheme.colors.iconSecondary,
                strokeWidth = 2.dp,
            )
            icon()
            Text(
                text = text,
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textSecondary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MomentAIErrorCard(
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    MomentAICard(
        modifier = modifier,
        borderColor = ElementTheme.colors.textCriticalPrimary.copy(alpha = 0.28f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = CompoundIcons.Warning(),
                contentDescription = null,
                tint = ElementTheme.colors.iconCriticalPrimary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = message,
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            onRetry?.let {
                TextButton(
                    text = stringResource(R.string.screen_room_ai_retry),
                    onClick = it,
                )
            }
        }
    }
}

@Composable
private fun MomentAIFactCheckCard(
    result: MomentAIFactCheckResult,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var claimsExpanded by remember { mutableStateOf(false) }
    MomentAICard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FactCheckIcon(tint = verdictColor(result.verdict))
                Text(
                    text = stringResource(R.string.screen_room_ai_fact_check_title),
                    style = ElementTheme.typography.fontBodySmMedium,
                    color = ElementTheme.colors.textPrimary,
                )
                Spacer(modifier = Modifier.weight(1f))
                VerdictBadge(verdict = result.verdict)
                CloseButton(onClick = onDismiss)
            }

            Text(
                text = result.rationale,
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textSecondary,
            )

            if (result.claims.isNotEmpty()) {
                TextButton(
                    text = stringResource(
                        if (claimsExpanded) R.string.screen_room_ai_claims_hide else R.string.screen_room_ai_claims_show,
                        result.claims.size,
                    ),
                    onClick = { claimsExpanded = !claimsExpanded },
                )
                if (claimsExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        result.claims.forEach { claim ->
                            ClaimRow(claim)
                        }
                    }
                }
            }

            if (result.sources.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.screen_room_ai_sources_title),
                    style = ElementTheme.typography.fontBodySmMedium,
                    color = ElementTheme.colors.textPrimary,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    result.sources.take(3).forEach { source ->
                        SourceRow(source)
                    }
                }
            }

            Text(
                text = stringResource(R.string.screen_room_ai_disclaimer_full),
                style = ElementTheme.typography.fontBodyXsRegular,
                color = ElementTheme.colors.textDisabled,
            )
        }
    }
}

@Composable
private fun MomentAISummaryCard(
    summary: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MomentAICard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = CompoundIcons.Quote(),
                    contentDescription = null,
                    tint = ElementTheme.colors.iconInfoPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.screen_room_ai_summary_title),
                    style = ElementTheme.typography.fontBodySmMedium,
                    color = ElementTheme.colors.textPrimary,
                )
                Spacer(modifier = Modifier.weight(1f))
                CloseButton(onClick = onDismiss)
            }
            Text(
                text = summary,
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.screen_room_ai_disclaimer),
                style = ElementTheme.typography.fontBodyXsRegular,
                color = ElementTheme.colors.textDisabled,
            )
        }
    }
}

@Composable
private fun MomentAICard(
    modifier: Modifier = Modifier,
    borderColor: Color = ElementTheme.colors.borderInteractiveSecondary,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MomentAICardShape)
            .background(ElementTheme.colors.bgCanvasDefault)
            .border(1.dp, borderColor.copy(alpha = 0.6f), MomentAICardShape)
            .padding(14.dp)
    ) {
        content()
    }
}

@Composable
private fun FactCheckIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = CompoundIcons.Shield(),
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(18.dp),
    )
}

@Composable
private fun VerdictBadge(
    verdict: String,
    modifier: Modifier = Modifier,
) {
    val color = verdictColor(verdict)
    Text(
        text = verdictLabel(verdict),
        style = ElementTheme.typography.fontBodyXsMedium,
        color = color,
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun ClaimRow(
    claim: MomentAIFactCheckClaim,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ElementTheme.colors.bgSubtleSecondary)
            .padding(8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = when (claim.verdict) {
                "true" -> CompoundIcons.CheckCircleSolid()
                "false" -> CompoundIcons.Close()
                "partially_true" -> CompoundIcons.Minus()
                else -> CompoundIcons.InfoSolid()
            },
            contentDescription = null,
            tint = verdictColor(claim.verdict),
            modifier = Modifier.size(16.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(
                text = claim.text,
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textPrimary,
            )
            Text(
                text = claimVerdictLabel(claim.verdict),
                style = ElementTheme.typography.fontBodyXsRegular,
                color = verdictColor(claim.verdict),
            )
        }
    }
}

@Composable
private fun SourceRow(
    source: MomentAIFactCheckSource,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                imageVector = if (source.type == "web") CompoundIcons.Public() else CompoundIcons.InfoSolid(),
                contentDescription = null,
                tint = ElementTheme.colors.iconSecondary,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = source.title.ifBlank { source.url },
                style = ElementTheme.typography.fontBodyXsMedium,
                color = ElementTheme.colors.textInfoPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (source.snippet.isNotBlank()) {
            Text(
                text = source.snippet,
                style = ElementTheme.typography.fontBodyXsRegular,
                color = ElementTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CloseButton(
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
        Icon(
            imageVector = CompoundIcons.Close(),
            contentDescription = stringResource(CommonStrings.action_close),
            tint = ElementTheme.colors.iconSecondary,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun verdictLabel(verdict: String): String {
    return when (verdict) {
        "true" -> stringResource(R.string.screen_room_ai_verdict_true)
        "false" -> stringResource(R.string.screen_room_ai_verdict_false)
        "partially_true" -> stringResource(R.string.screen_room_ai_verdict_partial)
        "unverifiable" -> stringResource(R.string.screen_room_ai_verdict_unverifiable)
        else -> verdict
    }
}

@Composable
private fun claimVerdictLabel(verdict: String): String {
    return when (verdict) {
        "true" -> stringResource(R.string.screen_room_ai_claim_confirmed)
        "false" -> stringResource(R.string.screen_room_ai_claim_denied)
        "partially_true" -> stringResource(R.string.screen_room_ai_claim_partial)
        else -> stringResource(R.string.screen_room_ai_claim_undetermined)
    }
}

@Composable
private fun verdictColor(verdict: String): Color {
    return when (verdict) {
        "true" -> ElementTheme.colors.textSuccessPrimary
        "false" -> ElementTheme.colors.textCriticalPrimary
        "partially_true" -> Color(0xFFE67E22)
        else -> ElementTheme.colors.textSecondary
    }
}

private val MomentAICardShape = RoundedCornerShape(12.dp)

@PreviewsDayNight
@Composable
internal fun MomentAIMessageActionCardsPreview(
    @PreviewParameter(MomentAIMessageActionStateProvider::class) state: MomentAIMessageActionState,
) = ElementPreview {
    MomentAIMessageActionCards(
        state = state,
        onDismissFactCheck = {},
        onRetryFactCheck = {},
        onDismissSummary = {},
        modifier = Modifier.padding(16.dp),
    )
}

private class MomentAIMessageActionStateProvider : PreviewParameterProvider<MomentAIMessageActionState> {
    override val values = sequenceOf(
        MomentAIMessageActionState(factCheck = MomentAIFactCheckUiState.Loading),
        MomentAIMessageActionState(summary = MomentAISummaryUiState.Success("The message asks the team to prepare release notes and verify Android parity.")),
        MomentAIMessageActionState(
            factCheck = MomentAIFactCheckUiState.Success(
                MomentAIFactCheckResult(
                    verdict = "partially_true",
                    confidence = 0.74,
                    rationale = "The statement is mostly right, but one detail needs local verification.",
                    claims = listOf(MomentAIFactCheckClaim("The release is planned for Friday.", "partially_true", 0.74)),
                    sources = listOf(MomentAIFactCheckSource("web", "Release calendar", "https://example.com", "The current plan lists Friday as tentative.")),
                    knowledgeCutoffWarning = false,
                    model = "preview",
                )
            ),
            summary = MomentAISummaryUiState.Loading,
        ),
        MomentAIMessageActionState(factCheck = MomentAIFactCheckUiState.Error(), summary = MomentAISummaryUiState.Error()),
    )
}
