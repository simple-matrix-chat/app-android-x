/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.sessions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Inject
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarDispatcher
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarMessage
import io.element.android.libraries.designsystem.utils.snackbar.collectSnackbarMessageAsState
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.DeviceId
import io.element.android.libraries.matrix.api.session.DeleteSessionDeviceResult
import io.element.android.libraries.matrix.api.session.MatrixSessionDevice
import kotlinx.coroutines.launch

@Inject
class MomentSessionsPresenter(
    private val matrixClient: MatrixClient,
    private val snackbarDispatcher: SnackbarDispatcher,
) : Presenter<MomentSessionsState> {
    @Composable
    override fun present(): MomentSessionsState {
        val coroutineScope = rememberCoroutineScope()
        val snackbarMessage by snackbarDispatcher.collectSnackbarMessageAsState()
        var currentSession by remember { mutableStateOf<MatrixSessionDevice?>(null) }
        var otherSessions by remember { mutableStateOf(emptyList<MatrixSessionDevice>()) }
        var isLoading by remember { mutableStateOf(true) }
        var hasLoadError by remember { mutableStateOf(false) }
        var terminatingDeviceId by remember { mutableStateOf<DeviceId?>(null) }
        var pendingAccountManagementUrl by remember { mutableStateOf<String?>(null) }

        suspend fun loadDevices() {
            isLoading = true
            hasLoadError = false
            matrixClient.getSessionDevices()
                .onSuccess { devices ->
                    val current = devices.firstOrNull { it.isCurrent }
                    currentSession = current
                    otherSessions = devices.filter { it.deviceId != current?.deviceId }
                }
                .onFailure {
                    hasLoadError = true
                    snackbarDispatcher.post(SnackbarMessage(R.string.screen_moment_sessions_load_failed))
                }
            isLoading = false
        }

        LaunchedEffect(Unit) {
            loadDevices()
        }

        fun terminateSession(deviceId: DeviceId) {
            if (terminatingDeviceId != null || currentSession?.deviceId == deviceId) return
            terminatingDeviceId = deviceId
            coroutineScope.launch {
                matrixClient.deleteSessionDevice(deviceId)
                    .onSuccess { result ->
                        when (result) {
                            DeleteSessionDeviceResult.Deleted -> {
                                otherSessions = otherSessions.filterNot { it.deviceId == deviceId }
                            }
                            is DeleteSessionDeviceResult.RequiresAccountManagement -> {
                                result.url?.let {
                                    pendingAccountManagementUrl = it
                                } ?: snackbarDispatcher.post(SnackbarMessage(R.string.screen_moment_sessions_terminate_failed))
                            }
                        }
                    }
                    .onFailure {
                        snackbarDispatcher.post(SnackbarMessage(R.string.screen_moment_sessions_terminate_failed))
                    }
                terminatingDeviceId = null
            }
        }

        fun handleEvent(event: MomentSessionsEvent) {
            when (event) {
                MomentSessionsEvent.Refresh -> {
                    if (!isLoading) {
                        coroutineScope.launch { loadDevices() }
                    }
                }
                is MomentSessionsEvent.TerminateSession -> terminateSession(event.deviceId)
                MomentSessionsEvent.AccountManagementOpened -> pendingAccountManagementUrl = null
            }
        }

        return MomentSessionsState(
            currentSession = currentSession,
            otherSessions = otherSessions,
            isLoading = isLoading,
            hasLoadError = hasLoadError,
            terminatingDeviceId = terminatingDeviceId,
            pendingAccountManagementUrl = pendingAccountManagementUrl,
            snackbarMessage = snackbarMessage,
            eventSink = ::handleEvent,
        )
    }
}
