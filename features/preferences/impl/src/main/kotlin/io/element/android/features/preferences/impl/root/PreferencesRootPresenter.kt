/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Inject
import io.element.android.features.enterprise.api.SessionEnterpriseService
import io.element.android.features.logout.api.direct.DirectLogoutState
import io.element.android.features.preferences.impl.notifications.SystemNotificationsEnabledProvider
import io.element.android.features.preferences.impl.privacy.MomentPrivacyAccess
import io.element.android.features.preferences.impl.privacy.MomentPrivacySettings
import io.element.android.features.preferences.impl.privacy.MomentVisibilityAccess
import io.element.android.features.preferences.impl.utils.ShowDeveloperSettingsProvider
import io.element.android.features.rageshake.api.RageshakeFeatureAvailability
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarDispatcher
import io.element.android.libraries.designsystem.utils.snackbar.collectSnackbarMessageAsState
import io.element.android.libraries.featureflag.api.FeatureFlagService
import io.element.android.libraries.featureflag.api.FeatureFlags
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.pushstore.api.UserPushStoreFactory
import io.element.android.libraries.sessionstorage.api.SessionStore
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val MOMENT_PRIVACY_ACCOUNT_DATA_TYPE = "io.moment.privacy"

@Inject
class PreferencesRootPresenter(
    private val matrixClient: MatrixClient,
    private val versionFormatter: VersionFormatter,
    private val snackbarDispatcher: SnackbarDispatcher,
    private val directLogoutPresenter: Presenter<DirectLogoutState>,
    private val showDeveloperSettingsProvider: ShowDeveloperSettingsProvider,
    private val rageshakeFeatureAvailability: RageshakeFeatureAvailability,
    private val featureFlagService: FeatureFlagService,
    private val sessionStore: SessionStore,
    private val sessionEnterpriseService: SessionEnterpriseService,
    private val userPushStoreFactory: UserPushStoreFactory,
    private val systemNotificationsEnabledProvider: SystemNotificationsEnabledProvider,
) : Presenter<PreferencesRootState> {
    @Composable
    override fun present(): PreferencesRootState {
        val coroutineScope = rememberCoroutineScope()
        val matrixUser = matrixClient.userProfile.collectAsState()
        LaunchedEffect(Unit) {
            // Force a refresh of the profile
            matrixClient.getUserProfile()
        }
        var profileStatus by remember { mutableStateOf("") }
        LaunchedEffect(matrixUser.value.userId) {
            profileStatus = matrixClient.getProfileStatus(matrixUser.value.userId)
                .getOrDefault("")
                .trim()
        }

        val isMultiAccountEnabled by remember {
            featureFlagService.isFeatureEnabledFlow(FeatureFlags.MultiAccount)
        }.collectAsState(initial = false)

        val otherSessions by remember {
            sessionStore.sessionsFlow().map { list ->
                list
                    .filter { it.userId != matrixClient.sessionId.value }
                    .map {
                        MatrixUser(
                            userId = UserId(it.userId),
                            displayName = it.userDisplayName,
                            avatarUrl = it.userAvatarUrl,
                        )
                    }
                    .toImmutableList()
            }
        }.collectAsState(initial = persistentListOf())

        val snackbarMessage by snackbarDispatcher.collectSnackbarMessageAsState()
        val accountManagementUrl: MutableState<String?> = remember {
            mutableStateOf(null)
        }
        var canDeactivateAccount by remember {
            mutableStateOf(false)
        }
        val canReportBug by remember { rageshakeFeatureAvailability.isAvailable() }.collectAsState(false)
        LaunchedEffect(Unit) {
            canDeactivateAccount = matrixClient.canDeactivateAccount()
        }

        val nbOfBlockedUsers by produceState(initialValue = 0) {
            matrixClient.ignoredUsersFlow
                .onEach { value = it.size }
                .launchIn(this)
        }

        val showLabsItem = remember { featureFlagService.getAvailableFeatures(isInLabs = true).isNotEmpty() }

        var momentPrivacySummary by remember { mutableStateOf(MomentPrivacySummary.Everyone) }
        LaunchedEffect(Unit) {
            momentPrivacySummary = matrixClient.getAccountData(MOMENT_PRIVACY_ACCOUNT_DATA_TYPE)
                .map { content -> MomentPrivacySettings.fromJson(content).rootSummary() }
                .getOrDefault(MomentPrivacySummary.Everyone)
        }

        val userPushStore = remember { userPushStoreFactory.getOrCreate(matrixClient.sessionId) }
        val appNotificationsEnabled by remember {
            userPushStore.getNotificationEnabledForDevice()
        }.collectAsState(initial = false)
        val systemNotificationsEnabled = remember { systemNotificationsEnabledProvider.notificationsEnabled() }
        val momentNotificationsSummary = remember(appNotificationsEnabled, systemNotificationsEnabled) {
            if (appNotificationsEnabled && systemNotificationsEnabled) {
                MomentNotificationsSummary.Enabled
            } else {
                MomentNotificationsSummary.Disabled
            }
        }

        val directLogoutState = directLogoutPresenter.present()

        LaunchedEffect(Unit) {
            initAccountManagementUrl(accountManagementUrl)
        }

        val showDeveloperSettings by showDeveloperSettingsProvider.showDeveloperSettings.collectAsState()

        fun handleEvent(event: PreferencesRootEvent) {
            when (event) {
                is PreferencesRootEvent.OnVersionInfoClick -> {
                    showDeveloperSettingsProvider.unlockDeveloperSettings(coroutineScope)
                }
                is PreferencesRootEvent.SwitchToSession -> coroutineScope.launch {
                    sessionStore.setLatestSession(event.sessionId.value)
                }
            }
        }

        return PreferencesRootState(
            myUser = matrixUser.value,
            profileStatus = profileStatus,
            version = remember { versionFormatter.get() },
            deviceId = matrixClient.deviceId,
            isMultiAccountEnabled = isMultiAccountEnabled,
            otherSessions = otherSessions,
            accountManagementUrl = accountManagementUrl.value,
            canReportBug = canReportBug,
            showDeveloperSettings = showDeveloperSettings,
            canDeactivateAccount = canDeactivateAccount,
            nbOfBlockedUsers = nbOfBlockedUsers,
            showLabsItem = showLabsItem,
            momentPrivacySummary = momentPrivacySummary,
            momentNotificationsSummary = momentNotificationsSummary,
            directLogoutState = directLogoutState,
            snackbarMessage = snackbarMessage,
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

private fun MomentPrivacySettings.rootSummary(): MomentPrivacySummary {
    return when {
        directMessages == MomentPrivacyAccess.Everyone &&
            groupInvites == MomentPrivacyAccess.Everyone &&
            avatarVisibility == MomentVisibilityAccess.Everyone &&
            phoneVisibility == MomentPrivacyAccess.Everyone &&
            presenceVisibility == MomentVisibilityAccess.Everyone -> MomentPrivacySummary.Everyone
        directMessages == MomentPrivacyAccess.ContactsOnly &&
            groupInvites == MomentPrivacyAccess.ContactsOnly &&
            avatarVisibility == MomentVisibilityAccess.ContactsOnly &&
            phoneVisibility == MomentPrivacyAccess.ContactsOnly &&
            presenceVisibility == MomentVisibilityAccess.ContactsOnly -> MomentPrivacySummary.ContactsOnly
        else -> MomentPrivacySummary.Custom
    }
}
