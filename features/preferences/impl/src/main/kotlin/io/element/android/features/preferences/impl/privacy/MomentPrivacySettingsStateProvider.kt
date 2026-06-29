/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.privacy

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

open class MomentPrivacySettingsStateProvider : PreviewParameterProvider<MomentPrivacySettingsState> {
    override val values: Sequence<MomentPrivacySettingsState>
        get() = sequenceOf(
            aMomentPrivacySettingsState(),
            aMomentPrivacySettingsState(isLoading = true),
            aMomentPrivacySettingsState(savingSection = MomentPrivacySettingsSection.PhoneVisibility),
            aMomentPrivacySettingsState(
                settings = MomentPrivacySettings.Default.copy(
                    directMessages = MomentPrivacyAccess.ContactsOnly,
                    groupInvites = MomentPrivacyAccess.Nobody,
                    avatarVisibility = MomentVisibilityAccess.ContactsOnly,
                    presenceVisibility = MomentVisibilityAccess.ContactsOnly,
                ),
            ),
        )
}

fun aMomentPrivacySettingsState(
    settings: MomentPrivacySettings = MomentPrivacySettings.Default,
    isLoading: Boolean = false,
    savingSection: MomentPrivacySettingsSection? = null,
    eventSink: (MomentPrivacySettingsEvent) -> Unit = {},
) = MomentPrivacySettingsState(
    settings = settings,
    isLoading = isLoading,
    savingSection = savingSection,
    snackbarMessage = null,
    eventSink = eventSink,
)
