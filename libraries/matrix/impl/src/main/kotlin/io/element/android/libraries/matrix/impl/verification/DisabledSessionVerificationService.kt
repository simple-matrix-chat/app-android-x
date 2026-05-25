/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.verification

import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.verification.SessionVerificationService
import io.element.android.libraries.matrix.api.verification.SessionVerificationServiceListener
import io.element.android.libraries.matrix.api.verification.SessionVerifiedStatus
import io.element.android.libraries.matrix.api.verification.VerificationFlowState
import io.element.android.libraries.matrix.api.verification.VerificationRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

object DisabledSessionVerificationService : SessionVerificationService {
    override val verificationFlowState = MutableStateFlow<VerificationFlowState>(VerificationFlowState.Initial)
    override val sessionVerifiedStatus = MutableStateFlow<SessionVerifiedStatus>(SessionVerifiedStatus.Verified)
    override val needsSessionVerification = flowOf(false)

    override suspend fun requestDeviceVerification() = Unit

    override suspend fun requestUserVerification(userId: UserId) = Unit

    override suspend fun cancelVerification() = Unit

    override suspend fun approveVerification() = Unit

    override suspend fun declineVerification() = Unit

    override suspend fun startSasVerification() = Unit

    override suspend fun reset(cancelAnyPendingVerificationAttempt: Boolean) = Unit

    override fun setListener(listener: SessionVerificationServiceListener?) = Unit

    override suspend fun acknowledgeVerificationRequest(verificationRequest: VerificationRequest.Incoming) = Unit

    override suspend fun acceptVerificationRequest() = Unit
}
