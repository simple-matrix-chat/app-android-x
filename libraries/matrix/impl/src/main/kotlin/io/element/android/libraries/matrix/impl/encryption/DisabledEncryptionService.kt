/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.encryption

import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.encryption.BackupState
import io.element.android.libraries.matrix.api.encryption.BackupUploadState
import io.element.android.libraries.matrix.api.encryption.EnableRecoveryProgress
import io.element.android.libraries.matrix.api.encryption.EncryptionService
import io.element.android.libraries.matrix.api.encryption.IdentityResetHandle
import io.element.android.libraries.matrix.api.encryption.RecoveryState
import io.element.android.libraries.matrix.api.encryption.identity.IdentityState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

object DisabledEncryptionService : EncryptionService {
    override val backupStateStateFlow = MutableStateFlow(BackupState.ENABLED)
    override val recoveryStateStateFlow = MutableStateFlow(RecoveryState.ENABLED)
    override val enableRecoveryProgressStateFlow = MutableStateFlow<EnableRecoveryProgress>(EnableRecoveryProgress.Starting)
    override val isLastDevice = MutableStateFlow(false)
    override val hasDevicesToVerifyAgainst = MutableStateFlow<AsyncData<Boolean>>(AsyncData.Success(false))

    override suspend fun enableBackups(): Result<Unit> = Result.success(Unit)

    override suspend fun enableRecovery(waitForBackupsToUpload: Boolean): Result<Unit> = Result.success(Unit)

    override suspend fun resetRecoveryKey(): Result<String> = Result.failure(UnsupportedOperationException(MATRIX_E2EE_DISABLED))

    override suspend fun disableRecovery(): Result<Unit> = Result.success(Unit)

    override suspend fun doesBackupExistOnServer(): Result<Boolean> = Result.success(false)

    override suspend fun recover(recoveryKey: String): Result<Unit> = Result.success(Unit)

    override fun waitForBackupUploadSteadyState(): Flow<BackupUploadState> = flowOf(BackupUploadState.Done)

    override suspend fun deviceCurve25519(): String? = null

    override suspend fun deviceEd25519(): String? = null

    override suspend fun startIdentityReset(): Result<IdentityResetHandle?> = Result.success(null)

    override suspend fun pinUserIdentity(userId: UserId): Result<Unit> = Result.success(Unit)

    override suspend fun withdrawVerification(userId: UserId): Result<Unit> = Result.success(Unit)

    override suspend fun getUserIdentity(userId: UserId, fallbackToServer: Boolean): Result<IdentityState?> = Result.success(null)

    private const val MATRIX_E2EE_DISABLED = "Matrix E2EE is disabled"
}
