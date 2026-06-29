/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.general

import io.element.android.features.preferences.impl.advanced.ThemeOption

sealed interface MomentGeneralSettingsEvent {
    data class SetTheme(val theme: ThemeOption) : MomentGeneralSettingsEvent
}
