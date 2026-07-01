/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.ai

import io.element.android.features.ai.api.MomentAIDailyBriefingResult
import io.element.android.libraries.architecture.AsyncData

data class MomentAIDailyBriefingState(
    val isVisible: Boolean,
    val action: AsyncData<MomentAIDailyBriefingResult>,
    val eventSink: (MomentAIDailyBriefingEvent) -> Unit,
)

sealed interface MomentAIDailyBriefingEvent {
    data object Show : MomentAIDailyBriefingEvent
    data object Dismiss : MomentAIDailyBriefingEvent
    data class Generate(val force: Boolean = true) : MomentAIDailyBriefingEvent
}

internal fun aMomentAIDailyBriefingState(
    isVisible: Boolean = false,
    action: AsyncData<MomentAIDailyBriefingResult> = AsyncData.Uninitialized,
    eventSink: (MomentAIDailyBriefingEvent) -> Unit = {},
) = MomentAIDailyBriefingState(
    isVisible = isVisible,
    action = action,
    eventSink = eventSink,
)
