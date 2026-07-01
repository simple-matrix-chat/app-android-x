/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.ai

import io.element.android.features.ai.api.MomentAIDailyBriefingResult
import io.element.android.libraries.architecture.AsyncData

data class MomentAIDailyBriefingComposerState(
    val isVisible: Boolean,
    val action: AsyncData<MomentAIDailyBriefingResult>,
)
