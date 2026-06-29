/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.general

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import dev.zacsweers.metro.Inject
import io.element.android.compound.theme.Theme
import io.element.android.compound.theme.mapToTheme
import io.element.android.features.enterprise.api.SessionEnterpriseService
import io.element.android.features.logout.api.direct.DirectLogoutState
import io.element.android.features.preferences.impl.advanced.ThemeOption
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.di.annotations.SessionCoroutineScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.preferences.api.store.AppPreferencesStore
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Inject
class MomentGeneralSettingsPresenter(
    private val matrixClient: MatrixClient,
    private val appPreferencesStore: AppPreferencesStore,
    private val sessionEnterpriseService: SessionEnterpriseService,
    private val directLogoutPresenter: Presenter<DirectLogoutState>,
    @SessionCoroutineScope
    private val sessionCoroutineScope: CoroutineScope,
) : Presenter<MomentGeneralSettingsState> {
    @Composable
    override fun present(): MomentGeneralSettingsState {
        val localCoroutineScope = rememberCoroutineScope()
        val theme by remember {
            appPreferencesStore.getThemeFlow().mapToTheme(allowBlackTheme = false)
        }.collectAsState(initial = Theme.System)
        val accountManagementUrl: MutableState<String?> = remember {
            mutableStateOf(null)
        }
        val canDeactivateAccount by produceState(initialValue = false) {
            value = matrixClient.canDeactivateAccount()
        }
        val showLinkNewDevice by produceState(initialValue = false) {
            value = matrixClient.canLinkNewDevice().getOrDefault(false)
        }
        val showBlockedUsers by produceState(initialValue = false) {
            matrixClient.ignoredUsersFlow
                .onEach { value = it.isNotEmpty() }
                .launchIn(this)
        }
        val directLogoutState = directLogoutPresenter.present()

        remember {
            localCoroutineScope.initAccountManagementUrl(accountManagementUrl)
            true
        }

        fun handleEvent(event: MomentGeneralSettingsEvent) {
            when (event) {
                is MomentGeneralSettingsEvent.SetTheme -> sessionCoroutineScope.launch {
                    val theme = when (event.theme) {
                        ThemeOption.System -> Theme.System
                        ThemeOption.Light -> Theme.Light
                        ThemeOption.Dark -> Theme.Dark
                        ThemeOption.Black -> Theme.Dark
                    }
                    appPreferencesStore.setTheme(theme.name)
                }
            }
        }

        return MomentGeneralSettingsState(
            theme = theme.toThemeOption(),
            availableThemeOptions = remember {
                listOf(ThemeOption.System, ThemeOption.Light, ThemeOption.Dark).toImmutableList()
            },
            accountManagementUrl = accountManagementUrl.value,
            showLinkNewDevice = showLinkNewDevice,
            showBlockedUsers = showBlockedUsers,
            canDeactivateAccount = canDeactivateAccount,
            directLogoutState = directLogoutState,
            eventSink = ::handleEvent,
        )
    }

    private fun CoroutineScope.initAccountManagementUrl(
        accountManagementUrl: MutableState<String?>,
    ) = launch {
        accountManagementUrl.value = matrixClient.getAccountManagementUrl(null)
            .getOrNull()
            ?.let {
                sessionEnterpriseService.tweakMasUrl(it)
            }
    }
}

private fun Theme.toThemeOption(): ThemeOption = when (this) {
    Theme.System -> ThemeOption.System
    Theme.Dark, Theme.Black -> ThemeOption.Dark
    Theme.Light -> ThemeOption.Light
}
