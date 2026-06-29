/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.privacy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Inject
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarDispatcher
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarMessage
import io.element.android.libraries.designsystem.utils.snackbar.collectSnackbarMessageAsState
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.privacy.MatrixMomentPrivacyAccess
import io.element.android.libraries.matrix.api.privacy.MatrixMomentPrivacySettings
import io.element.android.libraries.matrix.api.privacy.MatrixMomentVisibilityAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val MOMENT_PRIVACY_ACCOUNT_DATA_TYPE = "io.moment.privacy"

@Inject
class MomentPrivacySettingsPresenter(
    private val matrixClient: MatrixClient,
    private val snackbarDispatcher: SnackbarDispatcher,
) : Presenter<MomentPrivacySettingsState> {
    @Composable
    override fun present(): MomentPrivacySettingsState {
        val coroutineScope = rememberCoroutineScope()
        val snackbarMessage by snackbarDispatcher.collectSnackbarMessageAsState()
        var settings by remember { mutableStateOf(MomentPrivacySettings.Default) }
        var isLoading by remember { mutableStateOf(true) }
        var savingSection by remember { mutableStateOf<MomentPrivacySettingsSection?>(null) }

        LaunchedEffect(Unit) {
            matrixClient.getAccountData(MOMENT_PRIVACY_ACCOUNT_DATA_TYPE)
                .onSuccess { content ->
                    settings = MomentPrivacySettings.fromJson(content)
                }
                .onFailure {
                    snackbarDispatcher.post(SnackbarMessage(R.string.screen_moment_privacy_load_failed))
                }
            isLoading = false
        }

        fun updateSettings(section: MomentPrivacySettingsSection, updatedSettings: MomentPrivacySettings) {
            coroutineScope.updateSettings(
                currentSettings = settings,
                updatedSettings = updatedSettings,
                isLoading = isLoading,
                savingSection = savingSection,
                onOptimisticUpdate = {
                    savingSection = section
                    settings = it
                },
                onFailure = { previousSettings ->
                    settings = previousSettings
                    snackbarDispatcher.post(SnackbarMessage(R.string.screen_moment_privacy_update_failed))
                },
                onDone = {
                    savingSection = null
                },
            )
        }

        fun handleEvent(event: MomentPrivacySettingsEvent) {
            when (event) {
                is MomentPrivacySettingsEvent.SelectDirectMessages -> {
                    updateSettings(
                        MomentPrivacySettingsSection.DirectMessages,
                        settings.copy(directMessages = event.access),
                    )
                }
                is MomentPrivacySettingsEvent.SelectGroupInvites -> {
                    updateSettings(
                        MomentPrivacySettingsSection.GroupInvites,
                        settings.copy(groupInvites = event.access),
                    )
                }
                is MomentPrivacySettingsEvent.SelectAvatarVisibility -> {
                    updateSettings(
                        MomentPrivacySettingsSection.AvatarVisibility,
                        settings.copy(avatarVisibility = event.access),
                    )
                }
                is MomentPrivacySettingsEvent.SelectPhoneVisibility -> {
                    updateSettings(
                        MomentPrivacySettingsSection.PhoneVisibility,
                        settings.copy(phoneVisibility = event.access),
                    )
                }
                is MomentPrivacySettingsEvent.SelectPresenceVisibility -> {
                    updateSettings(
                        MomentPrivacySettingsSection.PresenceVisibility,
                        settings.copy(presenceVisibility = event.access),
                    )
                }
            }
        }

        return MomentPrivacySettingsState(
            settings = settings,
            isLoading = isLoading,
            savingSection = savingSection,
            snackbarMessage = snackbarMessage,
            eventSink = ::handleEvent,
        )
    }

    private fun CoroutineScope.updateSettings(
        currentSettings: MomentPrivacySettings,
        updatedSettings: MomentPrivacySettings,
        isLoading: Boolean,
        savingSection: MomentPrivacySettingsSection?,
        onOptimisticUpdate: (MomentPrivacySettings) -> Unit,
        onFailure: (MomentPrivacySettings) -> Unit,
        onDone: () -> Unit,
    ) {
        if (isLoading || savingSection != null || updatedSettings == currentSettings) return
        onOptimisticUpdate(updatedSettings)
        launch {
            val saveResult = matrixClient.setAccountData(
                eventType = MOMENT_PRIVACY_ACCOUNT_DATA_TYPE,
                content = updatedSettings.toJson(),
            )
            if (saveResult.isSuccess) {
                matrixClient.syncMomentPrivacySettings(updatedSettings.toMatrixMomentPrivacySettings())
            } else {
                onFailure(currentSettings)
            }
            onDone()
        }
    }
}

private fun MomentPrivacySettings.toMatrixMomentPrivacySettings(): MatrixMomentPrivacySettings {
    return MatrixMomentPrivacySettings(
        directMessages = directMessages.toMatrixMomentPrivacyAccess(),
        groupInvites = groupInvites.toMatrixMomentPrivacyAccess(),
        avatarVisibility = avatarVisibility.toMatrixMomentVisibilityAccess(),
        phoneVisibility = phoneVisibility.toMatrixMomentPrivacyAccess(),
        presenceVisibility = presenceVisibility.toMatrixMomentVisibilityAccess(),
    )
}

private fun MomentPrivacyAccess.toMatrixMomentPrivacyAccess(): MatrixMomentPrivacyAccess {
    return when (this) {
        MomentPrivacyAccess.Everyone -> MatrixMomentPrivacyAccess.Everyone
        MomentPrivacyAccess.ContactsOnly -> MatrixMomentPrivacyAccess.ContactsOnly
        MomentPrivacyAccess.Nobody -> MatrixMomentPrivacyAccess.Nobody
    }
}

private fun MomentVisibilityAccess.toMatrixMomentVisibilityAccess(): MatrixMomentVisibilityAccess {
    return when (this) {
        MomentVisibilityAccess.Everyone -> MatrixMomentVisibilityAccess.Everyone
        MomentVisibilityAccess.ContactsOnly -> MatrixMomentVisibilityAccess.ContactsOnly
    }
}
