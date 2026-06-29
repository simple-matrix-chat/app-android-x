/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.sessions

import io.element.android.libraries.designsystem.utils.snackbar.SnackbarMessage
import io.element.android.libraries.matrix.api.core.DeviceId
import io.element.android.libraries.matrix.api.session.MatrixSessionDevice

data class MomentSessionsState(
    val currentSession: MatrixSessionDevice?,
    val otherSessions: List<MatrixSessionDevice>,
    val isLoading: Boolean,
    val hasLoadError: Boolean,
    val terminatingDeviceId: DeviceId?,
    val pendingAccountManagementUrl: String?,
    val snackbarMessage: SnackbarMessage?,
    val eventSink: (MomentSessionsEvent) -> Unit,
) {
    val isInitialLoading: Boolean = isLoading && currentSession == null && otherSessions.isEmpty()
    val rowsEnabled: Boolean = !isLoading && terminatingDeviceId == null
}
