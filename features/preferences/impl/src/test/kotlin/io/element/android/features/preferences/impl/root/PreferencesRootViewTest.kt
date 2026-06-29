/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.features.preferences.impl.root

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.ui.components.aMatrixUser
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.tests.testutils.EnsureNeverCalled
import io.element.android.tests.testutils.EnsureNeverCalledWithParam
import io.element.android.tests.testutils.EventsRecorder
import io.element.android.tests.testutils.clickOn
import io.element.android.tests.testutils.ensureCalledOnce
import io.element.android.tests.testutils.ensureCalledOnceWithParam
import io.element.android.tests.testutils.pressBackKey
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesRootViewTest {
    @Test
    fun `pressing back key invokes back callback`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        ensureCalledOnce { callback ->
            setView(
                aPreferencesRootState(
                    eventSink = eventsRecorder
                ),
                onBackClick = callback,
            )
            pressBackKey()
        }
    }

    @Test
    fun `root profile tab chrome is shown`() = runAndroidComposeUiTest<ComponentActivity> {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        setView(
            aPreferencesRootState(
                eventSink = eventsRecorder,
            ),
        )

        onNodeWithContentDescription(activity!!.getString(CommonStrings.action_back)).assertDoesNotExist()
        onNodeWithText(activity!!.getString(R.string.screen_preferences_root_tab_profile)).assertExists()
        onNodeWithText(activity!!.getString(CommonStrings.common_settings)).assertDoesNotExist()
        onNodeWithContentDescription("Profile").assertIsSelected()
    }

    @Test
    fun `root user card shows profile status when available`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        setView(
            aPreferencesRootState(
                profileStatus = "Available",
                eventSink = eventsRecorder,
            ),
        )

        onNodeWithText("Available").assertExists()
    }

    @Test
    fun `root moment section summaries match iOS root shape`() = runAndroidComposeUiTest<ComponentActivity> {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        setView(
            aPreferencesRootState(
                version = "Version: 1.2.3 (123)\nbranch (revision)",
                showDeveloperSettings = true,
                momentPrivacySummary = MomentPrivacySummary.ContactsOnly,
                momentNotificationsSummary = MomentNotificationsSummary.Disabled,
                eventSink = eventsRecorder,
            ),
        )

        onNodeWithText(activity!!.getString(R.string.screen_moment_general_subtitle)).assertExists()
        onNodeWithText(activity!!.getString(R.string.screen_moment_privacy_summary_contacts_only)).assertExists()
        onNodeWithText(activity!!.getString(R.string.screen_moment_notifications_summary_disabled)).assertExists()
        onNodeWithText(activity!!.getString(R.string.screen_moment_about_app_title)).assertExists()
        onNodeWithText("Version: 1.2.3 (123)").assertExists()
        onNodeWithText(activity!!.getString(R.string.screen_preferences_root_developer_summary)).assertDoesNotExist()
        onNodeWithText(activity!!.getString(R.string.screen_preferences_root_privacy_summary)).assertDoesNotExist()
        onNodeWithText(activity!!.getString(R.string.screen_preferences_root_notifications_summary)).assertDoesNotExist()
    }

    @Test
    fun `clicking on chats tab invokes back callback`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        ensureCalledOnce { callback ->
            setView(
                aPreferencesRootState(
                    eventSink = eventsRecorder,
                ),
                onBackClick = callback,
            )

            onNodeWithContentDescription("Chats").performClick()
        }
    }

    @Test
    fun `click on User profile invokes the expected callback`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        val user = aMatrixUser()
        ensureCalledOnceWithParam(user) { callback ->
            setView(
                aPreferencesRootState(
                    myUser = user,
                    eventSink = eventsRecorder,
                ),
                onOpenUserProfile = callback,
            )
            onNodeWithText("Alice").performClick()
        }
    }

    @Test
    fun `multi account items are not shown on root`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        setView(
            aPreferencesRootState(
                isMultiAccountEnabled = true,
                otherSessions = listOf(
                    aMatrixUser(
                        displayName = "Bob",
                    )
                ),
                eventSink = eventsRecorder,
            ),
        )
        onNodeWithText("Bob").assertDoesNotExist()
        onNodeWithText(activity!!.getString(CommonStrings.common_add_another_account)).assertDoesNotExist()
    }

    @Test
    fun `when multi account is not enabled, add account item is not shown`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        setView(
            aPreferencesRootState(
                isMultiAccountEnabled = false,
                eventSink = eventsRecorder,
            ),
        )
        onNodeWithText(activity!!.getString(CommonStrings.common_add_another_account)).assertDoesNotExist()
    }

    @Test
    fun `manage account item is not shown on root`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        setView(
            aPreferencesRootState(
                accountManagementUrl = "aUrl",
                eventSink = eventsRecorder,
            ),
        )
        onNodeWithText(activity!!.getString(CommonStrings.action_manage_account_and_devices)).assertDoesNotExist()
    }

    @Test
    fun `when accountManagementUrl is null, item is not shown`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        setView(
            aPreferencesRootState(
                accountManagementUrl = null,
                eventSink = eventsRecorder,
            ),
        )
        onNodeWithText(activity!!.getString(CommonStrings.action_manage_account_and_devices)).assertDoesNotExist()
    }

    @Test
    fun `link new device item is not shown`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        setView(
            aPreferencesRootState(
                eventSink = eventsRecorder,
            ),
        )
        onNodeWithText(activity!!.getString(CommonStrings.common_link_new_device)).assertDoesNotExist()
    }

    @Test
    fun `analytics item is not shown`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        setView(
            aPreferencesRootState(
                eventSink = eventsRecorder,
            ),
        )
        onNodeWithText(activity!!.getString(CommonStrings.common_analytics)).assertDoesNotExist()
    }

    @Test
    fun `report a problem item is not shown on root`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        setView(
            aPreferencesRootState(
                canReportBug = true,
                eventSink = eventsRecorder,
            ),
        )
        onNodeWithText(activity!!.getString(CommonStrings.common_report_a_problem)).assertDoesNotExist()
    }

    @Test
    fun `when canReportBug is false, item is not shown`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        setView(
            aPreferencesRootState(
                canReportBug = false,
                eventSink = eventsRecorder,
            ),
        )
        onNodeWithText(activity!!.getString(CommonStrings.common_report_a_problem)).assertDoesNotExist()
    }

    @Test
    fun `screen lock item is not shown on root`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        setView(
            aPreferencesRootState(
                eventSink = eventsRecorder,
            ),
        )
        onNodeWithText(activity!!.getString(CommonStrings.common_screen_lock)).assertDoesNotExist()
    }

    @Test
    fun `click on About invokes the expected callback`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        ensureCalledOnce { callback ->
            setView(
                aPreferencesRootState(
                    eventSink = eventsRecorder,
                ),
                onOpenAbout = callback,
            )
            scrollToAndClickOn(R.string.screen_moment_about_app_title)
        }
    }

    @Test
    fun `click on Developer settings invokes the expected callback`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        ensureCalledOnce { callback ->
            setView(
                aPreferencesRootState(
                    showDeveloperSettings = true,
                    eventSink = eventsRecorder,
                ),
                onOpenDeveloperSettings = callback,
            )
            scrollToAndClickOn(CommonStrings.common_developer_options)
        }
    }

    @Test
    fun `when showDeveloperSettings is false, item is not shown`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        setView(
            aPreferencesRootState(
                showDeveloperSettings = false,
                eventSink = eventsRecorder,
            ),
        )
        onNodeWithText(activity!!.getString(CommonStrings.common_developer_options)).assertDoesNotExist()
    }

    @Test
    fun `advanced settings item is not shown on root`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        setView(
            aPreferencesRootState(
                eventSink = eventsRecorder,
            ),
        )
        onNodeWithText(activity!!.getString(CommonStrings.common_advanced_settings)).assertDoesNotExist()
    }

    @Test
    fun `when showLabsItem is true, item is still not shown`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        setView(
            aPreferencesRootState(
                showLabsItem = true,
                eventSink = eventsRecorder,
            ),
        )
        onNodeWithText(activity!!.getString(R.string.screen_labs_title)).assertDoesNotExist()
    }

    @Test
    fun `when showLabsItem is false, item is not shown`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        setView(
            aPreferencesRootState(
                showLabsItem = false,
                eventSink = eventsRecorder,
            ),
        )
        onNodeWithText(activity!!.getString(R.string.screen_labs_title)).assertDoesNotExist()
    }

    @Test
    fun `click on Notification invokes the expected callback`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        ensureCalledOnce { callback ->
            setView(
                aPreferencesRootState(
                    eventSink = eventsRecorder,
                ),
                onOpenNotificationSettings = callback,
            )
            clickOn(R.string.screen_notification_settings_title)
        }
    }

    @Test
    fun `click on General settings invokes the expected callback`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        ensureCalledOnce { callback ->
            setView(
                aPreferencesRootState(
                    eventSink = eventsRecorder,
                ),
                onOpenGeneralSettings = callback,
            )
            clickOn(R.string.screen_moment_general_title)
        }
    }

    @Test
    fun `click on Privacy invokes the expected callback`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        ensureCalledOnce { callback ->
            setView(
                aPreferencesRootState(
                    eventSink = eventsRecorder,
                ),
                onOpenPrivacySettings = callback,
            )
            clickOn(R.string.screen_moment_privacy_title)
        }
    }

    @Test
    fun `sessions item is not shown on root`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        setView(
            aPreferencesRootState(
                eventSink = eventsRecorder,
            ),
        )
        onNodeWithText(activity!!.getString(R.string.screen_moment_sessions_title)).assertDoesNotExist()
    }

    @Test
    fun `blocked users item is not shown on root`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        setView(
            aPreferencesRootState(
                nbOfBlockedUsers = 1,
                eventSink = eventsRecorder,
            ),
        )
        onNodeWithText(activity!!.getString(CommonStrings.common_blocked_users)).assertDoesNotExist()
    }

    @Test
    fun `when nbOfBlockedUsers is 0, item is not shown`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        setView(
            aPreferencesRootState(
                nbOfBlockedUsers = 0,
                eventSink = eventsRecorder,
            ),
        )
        onNodeWithText(activity!!.getString(CommonStrings.common_blocked_users)).assertDoesNotExist()
    }

    @Test
    fun `sign out item is not shown on root`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        setView(
            aPreferencesRootState(
                eventSink = eventsRecorder,
            ),
        )
        onNodeWithText(activity!!.getString(CommonStrings.action_signout)).assertDoesNotExist()
    }

    @Test
    fun `delete account item is not shown on root`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        setView(
            aPreferencesRootState(
                canDeactivateAccount = true,
                eventSink = eventsRecorder,
            ),
        )
        onNodeWithText(activity!!.getString(CommonStrings.action_delete_account)).assertDoesNotExist()
    }

    @Test
    fun `when canDeactivateAccount is false, item is not shown`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>(expectEvents = false)
        setView(
            aPreferencesRootState(
                canDeactivateAccount = false,
                eventSink = eventsRecorder,
            ),
        )
        onNodeWithText(activity!!.getString(CommonStrings.action_delete_account)).assertDoesNotExist()
    }

    @Test
    fun `clicking on version sends a PreferencesRootEvents`() = runAndroidComposeUiTest {
        val version = "VERSION\nBRANCH"
        val eventsRecorder = EventsRecorder<PreferencesRootEvent>()
        setView(
            aPreferencesRootState(
                version = version,
                eventSink = eventsRecorder,
            ),
        )
        onNodeWithText(version).performScrollTo().performClick()
        eventsRecorder.assertSingle(PreferencesRootEvent.OnVersionInfoClick)
    }
}

private fun AndroidComposeUiTest<ComponentActivity>.scrollToAndClickOn(@StringRes res: Int) {
    val text = activity!!.getString(res)
    onNode(hasText(text) and hasClickAction())
        .performScrollTo()
        .performClick()
}

private fun AndroidComposeUiTest<ComponentActivity>.setView(
    state: PreferencesRootState,
    onBackClick: () -> Unit = EnsureNeverCalled(),
    onOpenAbout: () -> Unit = EnsureNeverCalled(),
    onOpenDeveloperSettings: () -> Unit = EnsureNeverCalled(),
    onOpenGeneralSettings: () -> Unit = EnsureNeverCalled(),
    onOpenNotificationSettings: () -> Unit = EnsureNeverCalled(),
    onOpenPrivacySettings: () -> Unit = EnsureNeverCalled(),
    onOpenUserProfile: (MatrixUser) -> Unit = EnsureNeverCalledWithParam(),
) {
    setContent {
        PreferencesRootView(
            state = state,
            onBackClick = onBackClick,
            onOpenAbout = onOpenAbout,
            onOpenDeveloperSettings = onOpenDeveloperSettings,
            onOpenGeneralSettings = onOpenGeneralSettings,
            onOpenNotificationSettings = onOpenNotificationSettings,
            onOpenPrivacySettings = onOpenPrivacySettings,
            onOpenUserProfile = onOpenUserProfile,
        )
    }
}
