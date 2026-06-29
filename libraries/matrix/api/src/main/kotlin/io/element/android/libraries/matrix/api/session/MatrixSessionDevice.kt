/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.session

import io.element.android.libraries.matrix.api.core.DeviceId

data class MatrixSessionDevice(
    val deviceId: DeviceId,
    val displayName: String,
    val lastSeenIp: String?,
    val lastSeenTimestamp: Long?,
    val isCurrent: Boolean,
)

sealed interface DeleteSessionDeviceResult {
    data object Deleted : DeleteSessionDeviceResult
    data class RequiresAccountManagement(val url: String?) : DeleteSessionDeviceResult
}
