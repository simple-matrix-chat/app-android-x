/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.sessions

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarDispatcher
import io.element.android.libraries.matrix.api.core.DeviceId
import io.element.android.libraries.matrix.api.session.DeleteSessionDeviceResult
import io.element.android.libraries.matrix.api.session.MatrixSessionDevice
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.tests.testutils.consumeItemsUntilPredicate
import io.element.android.tests.testutils.test
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MomentSessionsPresenterTest {
    @Test
    fun `present - loads current and other sessions`() = runTest {
        val presenter = createPresenter(
            matrixClient = FakeMatrixClient().apply {
                givenSessionDevices(aMatrixSessionDevices())
            }
        )

        presenter.test {
            val loadedState = consumeItemsUntilPredicate { !it.isLoading }.last()

            assertThat(loadedState.currentSession?.deviceId).isEqualTo(A_CURRENT_DEVICE_ID)
            assertThat(loadedState.otherSessions.map { it.deviceId }).containsExactly(A_OTHER_DEVICE_ID)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - removes terminated session`() = runTest {
        val matrixClient = FakeMatrixClient().apply {
            givenSessionDevices(aMatrixSessionDevices())
        }
        val presenter = createPresenter(matrixClient)

        presenter.test {
            val loadedState = consumeItemsUntilPredicate { !it.isLoading }.last()
            loadedState.eventSink(MomentSessionsEvent.TerminateSession(A_OTHER_DEVICE_ID))

            val updatedState = consumeItemsUntilPredicate {
                it.terminatingDeviceId == null && it.otherSessions.none { session -> session.deviceId == A_OTHER_DEVICE_ID }
            }.last()

            assertThat(updatedState.otherSessions).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - exposes account management URL when deletion requires it`() = runTest {
        val accountManagementUrl = "https://example.org/account/device"
        val matrixClient = FakeMatrixClient(
            deleteSessionDeviceResult = { Result.success(DeleteSessionDeviceResult.RequiresAccountManagement(accountManagementUrl)) }
        ).apply {
            givenSessionDevices(aMatrixSessionDevices())
        }
        val presenter = createPresenter(matrixClient)

        presenter.test {
            val loadedState = consumeItemsUntilPredicate { !it.isLoading }.last()
            loadedState.eventSink(MomentSessionsEvent.TerminateSession(A_OTHER_DEVICE_ID))

            val accountManagementState = consumeItemsUntilPredicate {
                it.pendingAccountManagementUrl == accountManagementUrl
            }.last()
            assertThat(accountManagementState.otherSessions.map { it.deviceId }).containsExactly(A_OTHER_DEVICE_ID)

            accountManagementState.eventSink(MomentSessionsEvent.AccountManagementOpened)
            val clearedState = consumeItemsUntilPredicate { it.pendingAccountManagementUrl == null }.last()
            assertThat(clearedState.pendingAccountManagementUrl).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createPresenter(
        matrixClient: FakeMatrixClient = FakeMatrixClient(),
    ): MomentSessionsPresenter {
        return MomentSessionsPresenter(
            matrixClient = matrixClient,
            snackbarDispatcher = SnackbarDispatcher(),
        )
    }
}

private fun aMatrixSessionDevices(): List<MatrixSessionDevice> {
    return listOf(
        MatrixSessionDevice(
            deviceId = A_CURRENT_DEVICE_ID,
            displayName = "Android",
            lastSeenIp = null,
            lastSeenTimestamp = 1_788_000_000_000,
            isCurrent = true,
        ),
        MatrixSessionDevice(
            deviceId = A_OTHER_DEVICE_ID,
            displayName = "Desktop",
            lastSeenIp = "fd37:ee0e:f59e::6",
            lastSeenTimestamp = 1_787_996_400_000,
            isCurrent = false,
        ),
    )
}

private val A_CURRENT_DEVICE_ID = DeviceId("current-session")
private val A_OTHER_DEVICE_ID = DeviceId("desktop-session")
