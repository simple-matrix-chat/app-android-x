/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.features.preferences.impl.general

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.element.android.features.preferences.impl.R
import io.element.android.features.preferences.impl.advanced.ThemeOption
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.tests.testutils.EnsureNeverCalled
import io.element.android.tests.testutils.EnsureNeverCalledWithParam
import io.element.android.tests.testutils.EventsRecorder
import io.element.android.tests.testutils.assertNoNodeWithText
import io.element.android.tests.testutils.clickOn
import io.element.android.tests.testutils.ensureCalledOnce
import io.element.android.tests.testutils.ensureCalledOnceWithParam
import io.element.android.tests.testutils.pressBackKey
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MomentGeneralSettingsViewTest {
    @Test
    fun `pressing back key invokes back callback`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<MomentGeneralSettingsEvent>(expectEvents = false)
        ensureCalledOnce { callback ->
            setView(
                state = aMomentGeneralSettingsState(eventSink = eventsRecorder),
                onBackClick = callback,
            )
            pressBackKey()
        }
    }

    @Test
    fun `clicking on other theme emits the expected event`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<MomentGeneralSettingsEvent>()
        setView(
            state = aMomentGeneralSettingsState(eventSink = eventsRecorder),
        )
        clickOn(R.string.theme_dark)
        eventsRecorder.assertSingle(MomentGeneralSettingsEvent.SetTheme(ThemeOption.Dark))
    }

    @Test
    fun `click on Manage account invokes the expected callback`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<MomentGeneralSettingsEvent>(expectEvents = false)
        ensureCalledOnceWithParam("aUrl") { callback ->
            setView(
                state = aMomentGeneralSettingsState(
                    accountManagementUrl = "aUrl",
                    eventSink = eventsRecorder,
                ),
                onManageAccountClick = callback,
            )
            clickOn(CommonStrings.action_manage_account_and_devices)
        }
    }

    @Test
    fun `click on Link new device invokes the expected callback`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<MomentGeneralSettingsEvent>(expectEvents = false)
        ensureCalledOnce { callback ->
            setView(
                state = aMomentGeneralSettingsState(
                    showLinkNewDevice = true,
                    eventSink = eventsRecorder,
                ),
                onOpenLinkNewDevice = callback,
            )
            clickOn(CommonStrings.common_link_new_device)
        }
    }

    @Test
    fun `click on Blocked users invokes the expected callback`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<MomentGeneralSettingsEvent>(expectEvents = false)
        ensureCalledOnce { callback ->
            setView(
                state = aMomentGeneralSettingsState(
                    showBlockedUsers = true,
                    eventSink = eventsRecorder,
                ),
                onOpenBlockedUsers = callback,
            )
            clickOn(CommonStrings.common_blocked_users)
        }
    }

    @Test
    fun `account section is hidden when no account actions are available`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<MomentGeneralSettingsEvent>(expectEvents = false)
        setView(
            state = aMomentGeneralSettingsState(eventSink = eventsRecorder),
        )
        onNodeWithText(activity!!.getString(CommonStrings.action_manage_account_and_devices)).assertDoesNotExist()
        assertNoNodeWithText(io.element.android.libraries.ui.strings.R.string.common_link_new_device)
        assertNoNodeWithText(io.element.android.libraries.ui.strings.R.string.common_blocked_users)
    }

    @Test
    fun `click on Sessions invokes the expected callback`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<MomentGeneralSettingsEvent>(expectEvents = false)
        ensureCalledOnce { callback ->
            setView(
                state = aMomentGeneralSettingsState(eventSink = eventsRecorder),
                onOpenSessions = callback,
            )
            scrollToAndClickOn(R.string.screen_moment_sessions_title)
        }
    }

    @Test
    fun `click on Screen lock invokes the expected callback`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<MomentGeneralSettingsEvent>(expectEvents = false)
        ensureCalledOnce { callback ->
            setView(
                state = aMomentGeneralSettingsState(eventSink = eventsRecorder),
                onOpenLockScreenSettings = callback,
            )
            scrollToAndClickOn(CommonStrings.common_screen_lock)
        }
    }

    @Test
    fun `click on Advanced settings invokes the expected callback`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<MomentGeneralSettingsEvent>(expectEvents = false)
        ensureCalledOnce { callback ->
            setView(
                state = aMomentGeneralSettingsState(eventSink = eventsRecorder),
                onOpenAdvancedSettings = callback,
            )
            scrollToAndClickOn(CommonStrings.common_advanced_settings)
        }
    }

    @Test
    fun `labs item is not shown`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<MomentGeneralSettingsEvent>(expectEvents = false)
        setView(
            state = aMomentGeneralSettingsState(eventSink = eventsRecorder),
        )
        onNodeWithText(activity!!.getString(R.string.screen_labs_title)).assertDoesNotExist()
    }

    @Test
    fun `click on Sign out invokes the expected callback`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<MomentGeneralSettingsEvent>(expectEvents = false)
        ensureCalledOnce { callback ->
            setView(
                state = aMomentGeneralSettingsState(eventSink = eventsRecorder),
                onSignOutClick = callback,
            )
            scrollToAndClickOn(CommonStrings.action_signout)
        }
    }

    @Test
    fun `click on Deactivate account invokes the expected callback`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<MomentGeneralSettingsEvent>(expectEvents = false)
        ensureCalledOnce { callback ->
            setView(
                state = aMomentGeneralSettingsState(
                    canDeactivateAccount = true,
                    eventSink = eventsRecorder,
                ),
                onDeactivateClick = callback,
            )
            scrollToAndClickOn(CommonStrings.action_deactivate_account)
        }
    }

    @Test
    fun `deactivate account item is hidden when unavailable`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<MomentGeneralSettingsEvent>(expectEvents = false)
        setView(
            state = aMomentGeneralSettingsState(
                canDeactivateAccount = false,
                eventSink = eventsRecorder,
            ),
        )
        onNodeWithText(activity!!.getString(CommonStrings.action_deactivate_account)).assertDoesNotExist()
    }
}

private fun AndroidComposeUiTest<ComponentActivity>.scrollToAndClickOn(@StringRes res: Int) {
    val text = activity!!.getString(res)
    onNode(hasText(text) and hasClickAction())
        .performScrollTo()
        .performClick()
}

private fun AndroidComposeUiTest<ComponentActivity>.setView(
    state: MomentGeneralSettingsState,
    onBackClick: () -> Unit = EnsureNeverCalled(),
    onManageAccountClick: (url: String) -> Unit = EnsureNeverCalledWithParam(),
    onOpenLinkNewDevice: () -> Unit = EnsureNeverCalled(),
    onOpenBlockedUsers: () -> Unit = EnsureNeverCalled(),
    onOpenSessions: () -> Unit = EnsureNeverCalled(),
    onOpenLockScreenSettings: () -> Unit = EnsureNeverCalled(),
    onOpenAdvancedSettings: () -> Unit = EnsureNeverCalled(),
    onSignOutClick: () -> Unit = EnsureNeverCalled(),
    onDeactivateClick: () -> Unit = EnsureNeverCalled(),
) {
    setContent {
        MomentGeneralSettingsView(
            state = state,
            onBackClick = onBackClick,
            onManageAccountClick = onManageAccountClick,
            onOpenLinkNewDevice = onOpenLinkNewDevice,
            onOpenBlockedUsers = onOpenBlockedUsers,
            onOpenSessions = onOpenSessions,
            onOpenLockScreenSettings = onOpenLockScreenSettings,
            onOpenAdvancedSettings = onOpenAdvancedSettings,
            onSignOutClick = onSignOutClick,
            onDeactivateClick = onDeactivateClick,
        )
    }
}
