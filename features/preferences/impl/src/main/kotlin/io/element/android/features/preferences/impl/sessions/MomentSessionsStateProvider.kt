/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.sessions

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.libraries.matrix.api.core.DeviceId
import io.element.android.libraries.matrix.api.session.MatrixSessionDevice

open class MomentSessionsStateProvider : PreviewParameterProvider<MomentSessionsState> {
    override val values: Sequence<MomentSessionsState>
        get() = sequenceOf(
            aMomentSessionsState(),
            aMomentSessionsState(isLoading = true, currentSession = null, otherSessions = emptyList()),
            aMomentSessionsState(hasLoadError = true),
            aMomentSessionsState(terminatingDeviceId = A_SECOND_DEVICE_ID),
            aMomentSessionsState(otherSessions = emptyList()),
        )
}

fun aMomentSessionsState(
    currentSession: MatrixSessionDevice? = aSessionDevice(
        deviceId = A_CURRENT_DEVICE_ID,
        displayName = "Pixel 9 Pro XL",
        isCurrent = true,
    ),
    otherSessions: List<MatrixSessionDevice> = listOf(
        aSessionDevice(
            deviceId = A_SECOND_DEVICE_ID,
            displayName = "MacBook Pro",
            lastSeenIp = "fd37:ee0e:f59e::6",
            lastSeenTimestamp = 1_788_000_000_000,
        ),
        aSessionDevice(
            deviceId = DeviceId("tablet-session"),
            displayName = "iPad",
            lastSeenTimestamp = 1_787_996_400_000,
        ),
    ),
    isLoading: Boolean = false,
    hasLoadError: Boolean = false,
    terminatingDeviceId: DeviceId? = null,
    pendingAccountManagementUrl: String? = null,
    eventSink: (MomentSessionsEvent) -> Unit = {},
) = MomentSessionsState(
    currentSession = currentSession,
    otherSessions = otherSessions,
    isLoading = isLoading,
    hasLoadError = hasLoadError,
    terminatingDeviceId = terminatingDeviceId,
    pendingAccountManagementUrl = pendingAccountManagementUrl,
    snackbarMessage = null,
    eventSink = eventSink,
)

fun aSessionDevice(
    deviceId: DeviceId,
    displayName: String,
    lastSeenIp: String? = null,
    lastSeenTimestamp: Long? = 1_788_000_000_000,
    isCurrent: Boolean = false,
) = MatrixSessionDevice(
    deviceId = deviceId,
    displayName = displayName,
    lastSeenIp = lastSeenIp,
    lastSeenTimestamp = lastSeenTimestamp,
    isCurrent = isCurrent,
)

private val A_CURRENT_DEVICE_ID = DeviceId("current-session")
private val A_SECOND_DEVICE_ID = DeviceId("desktop-session")
