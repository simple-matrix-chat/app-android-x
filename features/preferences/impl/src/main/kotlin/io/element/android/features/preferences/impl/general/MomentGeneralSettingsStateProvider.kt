/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.general

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.features.logout.api.direct.DirectLogoutState
import io.element.android.features.logout.api.direct.aDirectLogoutState
import io.element.android.features.preferences.impl.advanced.ThemeOption
import kotlinx.collections.immutable.toImmutableList

open class MomentGeneralSettingsStateProvider : PreviewParameterProvider<MomentGeneralSettingsState> {
    override val values: Sequence<MomentGeneralSettingsState>
        get() = sequenceOf(
            aMomentGeneralSettingsState(
                accountManagementUrl = "aUrl",
                showLinkNewDevice = true,
                showBlockedUsers = true,
                canDeactivateAccount = true,
            ),
            aMomentGeneralSettingsState(theme = ThemeOption.Dark),
            aMomentGeneralSettingsState(),
        )
}

fun aMomentGeneralSettingsState(
    theme: ThemeOption = ThemeOption.System,
    availableThemeOptions: List<ThemeOption> = listOf(ThemeOption.System, ThemeOption.Light, ThemeOption.Dark),
    accountManagementUrl: String? = null,
    showLinkNewDevice: Boolean = false,
    showBlockedUsers: Boolean = false,
    canDeactivateAccount: Boolean = false,
    directLogoutState: DirectLogoutState = aDirectLogoutState(),
    eventSink: (MomentGeneralSettingsEvent) -> Unit = {},
) = MomentGeneralSettingsState(
    theme = theme,
    availableThemeOptions = availableThemeOptions.toImmutableList(),
    accountManagementUrl = accountManagementUrl,
    showLinkNewDevice = showLinkNewDevice,
    showBlockedUsers = showBlockedUsers,
    canDeactivateAccount = canDeactivateAccount,
    directLogoutState = directLogoutState,
    eventSink = eventSink,
)
