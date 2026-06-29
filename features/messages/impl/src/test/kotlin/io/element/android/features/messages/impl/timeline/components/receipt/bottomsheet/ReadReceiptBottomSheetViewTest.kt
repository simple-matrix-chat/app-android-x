/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.features.messages.impl.timeline.components.receipt.bottomsheet

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.element.android.features.messages.impl.timeline.aTimelineItemEvent
import io.element.android.features.messages.impl.timeline.aTimelineItemReadReceipts
import io.element.android.features.messages.impl.timeline.components.receipt.aReadReceiptData
import io.element.android.features.messages.impl.timeline.model.ReadReceiptData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.anAvatarData
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.tests.testutils.ensureCalledOnceWithParam
import io.element.android.tests.testutils.setSafeContent
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadReceiptBottomSheetViewTest {
    @Test
    fun `read receipt bottom sheet displays Moment rows and opens user profile`() = runAndroidComposeUiTest<ComponentActivity> {
        val readReceiptData = aReadReceiptData(
            index = 0,
            avatarData = anAvatarData(
                id = "@alice:matrix.org",
                name = "Alice",
                size = AvatarSize.TimelineReadReceipt,
            ),
            formattedDate = "10:00",
        )
        val selectedUserId = UserId("@alice:matrix.org")

        ensureCalledOnceWithParam(selectedUserId) { onUserDataClick ->
            setReadReceiptBottomSheet(
                receipts = listOf(readReceiptData),
                onUserDataClick = onUserDataClick,
            )

            onNodeWithText(activity!!.getString(CommonStrings.common_seen_by)).assertExists()
            onNodeWithText("Alice").assertExists()
            onNodeWithText("@alice:matrix.org").assertExists()
            onNodeWithText("10:00").assertExists()
            onNode(hasText("Alice") and hasClickAction()).performClick()
            mainClock.advanceTimeBy(milliseconds = 1_000)
        }
    }
}

private fun AndroidComposeUiTest<ComponentActivity>.setReadReceiptBottomSheet(
    receipts: List<ReadReceiptData>,
    onUserDataClick: (UserId) -> Unit,
) {
    setSafeContent {
        ElementPreview {
            ReadReceiptBottomSheet(
                state = ReadReceiptBottomSheetState(
                    selectedEvent = aTimelineItemEvent(
                        readReceiptState = aTimelineItemReadReceipts(receipts),
                    ),
                    eventSink = {},
                ),
                onUserDataClick = onUserDataClick,
            )
        }
    }
}
