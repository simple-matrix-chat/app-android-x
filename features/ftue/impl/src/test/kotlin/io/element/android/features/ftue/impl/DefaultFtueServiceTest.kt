/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.ftue.impl

import android.os.Build
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.element.android.features.ftue.api.state.FtueState
import io.element.android.features.ftue.impl.state.DefaultFtueService
import io.element.android.features.ftue.impl.state.FtueStep
import io.element.android.features.ftue.impl.state.InternalFtueState
import io.element.android.features.lockscreen.api.LockScreenService
import io.element.android.features.lockscreen.test.FakeLockScreenService
import io.element.android.libraries.permissions.api.PermissionStateProvider
import io.element.android.libraries.permissions.test.FakePermissionStateProvider
import io.element.android.services.toolbox.test.sdk.FakeBuildVersionSdkIntProvider
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultFtueServiceTest {
    @Test
    fun `given any check being false, FtueState is Incomplete`() = runTest {
        val service = createDefaultFtueService()

        service.state.test {
            assertThat(awaitItem()).isEqualTo(FtueState.Unknown)
            assertThat(awaitItem()).isEqualTo(FtueState.Incomplete)
        }
    }

    @Test
    fun `given all checks being true, FtueState is Complete`() = runTest {
        val permissionStateProvider = FakePermissionStateProvider(permissionGranted = true)
        val lockScreenService = FakeLockScreenService().apply {
            setIsPinSetup(true)
        }
        val service = createDefaultFtueService(
            permissionStateProvider = permissionStateProvider,
            lockScreenService = lockScreenService,
        )

        service.state.test {
            assertThat(awaitItem()).isEqualTo(FtueState.Unknown)
            assertThat(awaitItem()).isEqualTo(FtueState.Complete)
        }
    }

    @Test
    fun `traverse flow`() = runTest {
        val permissionStateProvider = FakePermissionStateProvider(permissionGranted = false)
        val lockScreenService = FakeLockScreenService()
        val service = createDefaultFtueService(
            permissionStateProvider = permissionStateProvider,
            lockScreenService = lockScreenService,
        )

        service.ftueStepStateFlow.test {
            assertThat(awaitItem()).isEqualTo(InternalFtueState.Unknown)
            assertThat(awaitItem()).isEqualTo(InternalFtueState.Incomplete(FtueStep.NotificationsOptIn))

            permissionStateProvider.setPermissionGranted()
            service.updateFtueStep()
            assertThat(awaitItem()).isEqualTo(InternalFtueState.Incomplete(FtueStep.LockscreenSetup))

            lockScreenService.setIsPinSetup(true)
            service.updateFtueStep()
            assertThat(awaitItem()).isEqualTo(InternalFtueState.Complete)
        }
    }

    @Test
    fun `if a check for a step is true, start from the next one`() = runTest {
        val permissionStateProvider = FakePermissionStateProvider(permissionGranted = true)
        val lockScreenService = FakeLockScreenService()
        val service = createDefaultFtueService(
            permissionStateProvider = permissionStateProvider,
            lockScreenService = lockScreenService,
        )

        service.ftueStepStateFlow.test {
            assertThat(awaitItem()).isEqualTo(InternalFtueState.Unknown)
            assertThat(awaitItem()).isEqualTo(InternalFtueState.Incomplete(FtueStep.LockscreenSetup))

            lockScreenService.setIsPinSetup(true)
            service.updateFtueStep()
            assertThat(awaitItem()).isEqualTo(InternalFtueState.Complete)
        }
    }

    @Test
    fun `if version is older than 13 we don't display the notification opt in screen`() = runTest {
        val lockScreenService = FakeLockScreenService()
        val service = createDefaultFtueService(
            sdkIntVersion = Build.VERSION_CODES.M,
            lockScreenService = lockScreenService,
        )

        service.ftueStepStateFlow.test {
            assertThat(awaitItem()).isEqualTo(InternalFtueState.Unknown)
            assertThat(awaitItem()).isEqualTo(InternalFtueState.Incomplete(FtueStep.LockscreenSetup))

            lockScreenService.setIsPinSetup(true)
            service.updateFtueStep()
            assertThat(awaitItem()).isEqualTo(InternalFtueState.Complete)
        }
    }
}

internal fun TestScope.createDefaultFtueService(
    permissionStateProvider: PermissionStateProvider = FakePermissionStateProvider(permissionGranted = false),
    lockScreenService: LockScreenService = FakeLockScreenService(),
    // First version where notification permission is required
    sdkIntVersion: Int = Build.VERSION_CODES.TIRAMISU,
) = DefaultFtueService(
    sessionCoroutineScope = backgroundScope,
    sdkVersionProvider = FakeBuildVersionSdkIntProvider(sdkIntVersion),
    permissionStateProvider = permissionStateProvider,
    lockScreenService = lockScreenService,
)
