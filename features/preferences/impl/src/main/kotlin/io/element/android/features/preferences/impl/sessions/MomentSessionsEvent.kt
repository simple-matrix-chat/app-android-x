/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.sessions

import io.element.android.libraries.matrix.api.core.DeviceId

sealed interface MomentSessionsEvent {
    data object Refresh : MomentSessionsEvent
    data class TerminateSession(val deviceId: DeviceId) : MomentSessionsEvent
    data object AccountManagementOpened : MomentSessionsEvent
}
