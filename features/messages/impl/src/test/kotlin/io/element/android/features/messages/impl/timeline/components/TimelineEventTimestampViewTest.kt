/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.features.messages.impl.timeline.components

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.element.android.features.messages.impl.timeline.aTimelineItemEvent
import io.element.android.features.messages.impl.timeline.aTimelineItemReadReceipts
import io.element.android.features.messages.impl.timeline.components.receipt.aReadReceiptData
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.libraries.designsystem.components.avatar.getBestName
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.matrix.api.timeline.item.event.LocalEventSendState
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.tests.testutils.ensureCalledOnceWithParam
import io.element.android.tests.testutils.setSafeContent
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimelineEventTimestampViewTest {
    @Test
    fun `failed send state after sending is delayed`() = runAndroidComposeUiTest<ComponentActivity> {
        mainClock.autoAdvance = false
        lateinit var setSendState: (LocalEventSendState?) -> Unit
        val sendingDescription = activity!!.getString(CommonStrings.common_sending)
        val failedDescription = activity!!.getString(CommonStrings.common_sending_failed)

        setTimestampViewWithMutableSendState { updateSendState ->
            setSendState = updateSendState
        }

        onNodeWithContentDescription(sendingDescription).assertExists()

        setSendState(LocalEventSendState.Failed.Unknown("AN_ERROR"))
        mainClock.advanceTimeBy(699)
        onNodeWithContentDescription(sendingDescription).assertExists()
        onNodeWithContentDescription(failedDescription).assertDoesNotExist()

        mainClock.advanceTimeBy(1)
        onNodeWithContentDescription(failedDescription).assertExists()
        onNodeWithContentDescription(sendingDescription).assertDoesNotExist()
    }

    @Test
    fun `initial failed send state is shown immediately`() = runAndroidComposeUiTest<ComponentActivity> {
        val failedDescription = activity!!.getString(CommonStrings.common_sending_failed)

        setTimestampView(
            event = aTimelineItemEvent(
                isMine = true,
                sendState = LocalEventSendState.Failed.Unknown("AN_ERROR"),
            )
        )

        onNodeWithContentDescription(failedDescription).assertExists()
    }

    @Test
    fun `read receipt send info click emits read receipt event`() = runAndroidComposeUiTest<ComponentActivity> {
        val readReceiptData = aReadReceiptData(0)
        val readReceiptDescription = activity!!.getString(
            CommonStrings.a11y_read_receipts_single,
            readReceiptData.avatarData.getBestName(),
        )
        val event = aTimelineItemEvent(
            isMine = true,
            readReceiptState = aTimelineItemReadReceipts(
                receipts = listOf(readReceiptData),
            ),
        )

        ensureCalledOnceWithParam(event) { onReadReceiptClick ->
            setTimestampView(
                event = event,
                onReadReceiptClick = onReadReceiptClick,
            )

            onNode(hasContentDescription(readReceiptDescription) and hasClickAction()).performClick()
        }
    }
}

private fun AndroidComposeUiTest<ComponentActivity>.setTimestampViewWithMutableSendState(
    onSendStateUpdaterReady: ((LocalEventSendState?) -> Unit) -> Unit,
) {
    setSafeContent {
        ElementPreview {
            var sendState by remember { mutableStateOf<LocalEventSendState?>(LocalEventSendState.Sending.Event) }
            onSendStateUpdaterReady { sendState = it }
            TimelineEventTimestampView(
                event = aTimelineItemEvent(
                    isMine = true,
                    sendState = sendState,
                ),
                eventSink = {},
            )
        }
    }
}

private fun AndroidComposeUiTest<ComponentActivity>.setTimestampView(
    event: TimelineItem.Event,
    onReadReceiptClick: (TimelineItem.Event) -> Unit = {},
) {
    setSafeContent {
        ElementPreview {
            TimelineEventTimestampView(
                event = event,
                eventSink = {},
                onReadReceiptClick = onReadReceiptClick,
            )
        }
    }
}
