/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.user.username

sealed interface MomentUsernameEvent {
    data class UpdateUsername(val username: String) : MomentUsernameEvent
    data object Save : MomentUsernameEvent
    data object Close : MomentUsernameEvent
}
