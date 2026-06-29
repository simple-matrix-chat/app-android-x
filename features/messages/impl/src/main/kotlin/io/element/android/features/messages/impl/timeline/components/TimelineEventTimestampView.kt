/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.messages.impl.timeline.TimelineEvent
import io.element.android.features.messages.impl.timeline.model.ReadReceiptData
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.features.messages.impl.timeline.model.event.isEdited
import io.element.android.libraries.designsystem.components.avatar.getBestName
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.timeline.item.event.LocalEventSendState
import io.element.android.libraries.ui.strings.CommonPlurals
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.coroutines.delay

private const val SEND_FAILURE_DISPLAY_DELAY_IN_MILLIS = 700L

@Composable
fun TimelineEventTimestampView(
    event: TimelineItem.Event,
    eventSink: (TimelineEvent.TimelineItemEvent) -> Unit,
    onReadReceiptClick: (TimelineItem.Event) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val formattedTime = event.sentTime
    val adjustedLocalSendState = rememberAdjustedLocalSendState(event)
    val hasError = adjustedLocalSendState is LocalEventSendState.Failed
    val isMessageEdited = event.content.isEdited()
    val tint = if (hasError) ElementTheme.colors.textCriticalPrimary else ElementTheme.colors.textSecondary
    val deliveryState = when {
        !event.isMine || hasError -> null
        event.readReceiptState.receipts.isNotEmpty() -> TimestampDeliveryState.Read
        adjustedLocalSendState is LocalEventSendState.Sending -> TimestampDeliveryState.Sending
        else -> TimestampDeliveryState.Sent
    }

    val isVerifiedUserSendFailure = adjustedLocalSendState is LocalEventSendState.Failed.VerifiedUser
    val readReceiptDescription = if (deliveryState == TimestampDeliveryState.Read) {
        computeReceiptDescription(event.readReceiptState.receipts)
    } else {
        null
    }
    val onClickLabel = when {
        hasError && isVerifiedUserSendFailure -> stringResource(CommonStrings.action_open_context_menu)
        readReceiptDescription != null -> stringResource(CommonStrings.a11y_read_receipts_tap_to_show_all)
        else -> null
    }
    val clickableModifier = remember(hasError, isVerifiedUserSendFailure, readReceiptDescription, onClickLabel, event, eventSink, onReadReceiptClick) {
        when {
            hasError -> Modifier
                .clickable(
                    enabled = isVerifiedUserSendFailure,
                    onClickLabel = onClickLabel,
                ) {
                    eventSink(TimelineEvent.ComputeVerifiedUserSendFailure(event))
                }
            readReceiptDescription != null -> Modifier
                .clickable(
                    onClickLabel = onClickLabel,
                ) {
                    onReadReceiptClick(event)
                }
            else -> Modifier
        }
    }
    Row(
        modifier = Modifier
            .padding(PaddingValues(start = TimelineEventTimestampViewDefaults.spacing))
            // For a better click target, make the corners rounded
            .clip(RoundedCornerShape(8.dp))
            .then(clickableModifier)
            .then(
                if (readReceiptDescription != null) {
                    Modifier.semantics {
                        contentDescription = readReceiptDescription
                    }
                } else {
                    Modifier
                }
            )
            .then(modifier),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isMessageEdited) {
            Text(
                stringResource(CommonStrings.common_edited_suffix),
                style = ElementTheme.typography.fontBodyXsRegular,
                color = tint,
            )
        }
        Text(
            formattedTime,
            style = ElementTheme.typography.fontBodyXsRegular,
            color = tint,
        )
        if (hasError) {
            Icon(
                imageVector = CompoundIcons.ErrorSolid(),
                contentDescription = stringResource(id = CommonStrings.common_sending_failed),
                tint = tint,
                modifier = Modifier.size(15.dp, 18.dp),
            )
        }
        deliveryState?.let {
            TimestampDeliveryIcon(
                state = it,
                receipts = event.readReceiptState.receipts,
                readReceiptDescription = readReceiptDescription,
            )
        }
    }
}

@Composable
private fun rememberAdjustedLocalSendState(event: TimelineItem.Event): LocalEventSendState? {
    val localSendState = event.localSendState
    var adjustedLocalSendState by remember(event.eventOrTransactionId) {
        mutableStateOf(localSendState)
    }

    LaunchedEffect(event.eventOrTransactionId, localSendState) {
        if (localSendState is LocalEventSendState.Failed && adjustedLocalSendState is LocalEventSendState.Sending) {
            delay(SEND_FAILURE_DISPLAY_DELAY_IN_MILLIS)
        }
        adjustedLocalSendState = localSendState
    }

    return adjustedLocalSendState
}

@Composable
private fun TimestampDeliveryIcon(
    state: TimestampDeliveryState,
    receipts: List<ReadReceiptData>,
    readReceiptDescription: String?,
) {
    when (state) {
        TimestampDeliveryState.Sending -> Icon(
            imageVector = CompoundIcons.Circle(),
            contentDescription = stringResource(id = CommonStrings.common_sending),
            tint = ElementTheme.colors.iconSecondary,
            modifier = Modifier.size(10.dp),
        )
        TimestampDeliveryState.Sent -> Icon(
            imageVector = CompoundIcons.Check(),
            contentDescription = stringResource(id = CommonStrings.common_sent),
            tint = ElementTheme.colors.iconSecondary,
            modifier = Modifier.size(12.dp),
        )
        TimestampDeliveryState.Read -> {
            val effectiveReadReceiptDescription = readReceiptDescription ?: computeReceiptDescription(receipts)
            Row(
                modifier = Modifier
                    .width(18.dp)
                    .clearAndSetSemantics { contentDescription = effectiveReadReceiptDescription },
                horizontalArrangement = Arrangement.spacedBy((-6).dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = CompoundIcons.Check(),
                    contentDescription = null,
                    tint = ElementTheme.colors.iconAccentPrimary,
                    modifier = Modifier.size(12.dp),
                )
                Icon(
                    imageVector = CompoundIcons.Check(),
                    contentDescription = null,
                    tint = ElementTheme.colors.iconAccentPrimary,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

@Composable
private fun computeReceiptDescription(receipts: List<ReadReceiptData>): String {
    return when (receipts.size) {
        0 -> stringResource(CommonStrings.common_sent)
        1 -> stringResource(
            id = CommonStrings.a11y_read_receipts_single,
            receipts[0].avatarData.getBestName()
        )
        2 -> stringResource(
            id = CommonStrings.a11y_read_receipts_multiple,
            receipts[0].avatarData.getBestName(),
            receipts[1].avatarData.getBestName()
        )
        else -> pluralStringResource(
            id = CommonPlurals.a11y_read_receipts_multiple_with_others,
            count = receipts.size - 1,
            receipts[0].avatarData.getBestName(),
            receipts.size - 1
        )
    }
}

@PreviewsDayNight
@Composable
internal fun TimelineEventTimestampViewPreview(@PreviewParameter(TimelineItemEventForTimestampViewProvider::class) event: TimelineItem.Event) = ElementPreview {
    TimelineEventTimestampView(
        event = event,
        eventSink = {},
    )
}

object TimelineEventTimestampViewDefaults {
    val spacing = 8.dp
}

private enum class TimestampDeliveryState {
    Sending,
    Sent,
    Read
}
