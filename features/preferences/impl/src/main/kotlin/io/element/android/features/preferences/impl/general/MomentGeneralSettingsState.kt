/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.general

import io.element.android.features.logout.api.direct.DirectLogoutState
import io.element.android.features.preferences.impl.advanced.ThemeOption
import kotlinx.collections.immutable.ImmutableList

data class MomentGeneralSettingsState(
    val theme: ThemeOption,
    val availableThemeOptions: ImmutableList<ThemeOption>,
    val accountManagementUrl: String?,
    val showLinkNewDevice: Boolean,
    val showBlockedUsers: Boolean,
    val canDeactivateAccount: Boolean,
    val directLogoutState: DirectLogoutState,
    val eventSink: (MomentGeneralSettingsEvent) -> Unit,
)
