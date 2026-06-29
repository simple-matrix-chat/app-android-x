/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.general

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import io.element.android.annotations.ContributesNode
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.logout.api.direct.DirectLogoutEvents
import io.element.android.features.logout.api.direct.DirectLogoutView
import io.element.android.libraries.androidutils.browser.openUrlInChromeCustomTab
import io.element.android.libraries.architecture.callback
import io.element.android.libraries.di.SessionScope

@ContributesNode(SessionScope::class)
@AssistedInject
class MomentGeneralSettingsNode(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    private val presenter: MomentGeneralSettingsPresenter,
    private val directLogoutView: DirectLogoutView,
) : Node(buildContext, plugins = plugins) {
    interface Callback : Plugin {
        fun onDone()
        fun navigateToLinkNewDevice()
        fun navigateToBlockedUsers()
        fun navigateToSessions()
        fun navigateToLockScreenSettings()
        fun navigateToAdvancedSettings()
        fun startSignOutFlow()
        fun startAccountDeactivationFlow()
    }

    private val callback: Callback = callback()

    private fun onManageAccountClick(
        activity: Activity,
        url: String?,
        isDark: Boolean,
    ) {
        url?.let {
            activity.openUrlInChromeCustomTab(
                null,
                darkTheme = isDark,
                url = it
            )
        }
    }

    @Composable
    override fun View(modifier: Modifier) {
        val state = presenter.present()
        val activity = requireNotNull(LocalActivity.current)
        val isDark = ElementTheme.isLightTheme.not()
        MomentGeneralSettingsView(
            state = state,
            onBackClick = callback::onDone,
            onManageAccountClick = { onManageAccountClick(activity, it, isDark) },
            onOpenLinkNewDevice = callback::navigateToLinkNewDevice,
            onOpenBlockedUsers = callback::navigateToBlockedUsers,
            onOpenSessions = callback::navigateToSessions,
            onOpenLockScreenSettings = callback::navigateToLockScreenSettings,
            onOpenAdvancedSettings = callback::navigateToAdvancedSettings,
            onSignOutClick = {
                if (state.directLogoutState.canDoDirectSignOut) {
                    state.directLogoutState.eventSink(DirectLogoutEvents.Logout(ignoreSdkError = false))
                } else {
                    callback.startSignOutFlow()
                }
            },
            onDeactivateClick = callback::startAccountDeactivationFlow,
            modifier = modifier,
        )

        directLogoutView.Render(state = state.directLogoutState)
    }
}
