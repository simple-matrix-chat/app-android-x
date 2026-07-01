/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.ai

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import io.element.android.features.messages.impl.R

@Immutable
data class MomentAIMessageActionState(
    val factCheck: MomentAIFactCheckUiState = MomentAIFactCheckUiState.Hidden,
    val summary: MomentAISummaryUiState = MomentAISummaryUiState.Hidden,
) {
    val isVisible: Boolean = factCheck !is MomentAIFactCheckUiState.Hidden || summary !is MomentAISummaryUiState.Hidden

    companion object {
        val Empty = MomentAIMessageActionState()
    }
}

@Immutable
sealed interface MomentAIFactCheckUiState {
    data object Hidden : MomentAIFactCheckUiState
    data object Loading : MomentAIFactCheckUiState
    data class Success(val result: MomentAIFactCheckResult) : MomentAIFactCheckUiState
    data class Error(@StringRes val messageResId: Int = R.string.screen_room_ai_fact_check_error_title) : MomentAIFactCheckUiState
}

@Immutable
sealed interface MomentAISummaryUiState {
    data object Hidden : MomentAISummaryUiState
    data object Loading : MomentAISummaryUiState
    data class Success(val summary: String) : MomentAISummaryUiState
    data class Error(@StringRes val messageResId: Int = R.string.screen_room_ai_summary_error_title) : MomentAISummaryUiState
}
