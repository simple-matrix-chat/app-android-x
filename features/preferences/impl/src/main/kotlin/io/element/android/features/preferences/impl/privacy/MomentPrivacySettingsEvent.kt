/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.privacy

sealed interface MomentPrivacySettingsEvent {
    data class SelectDirectMessages(val access: MomentPrivacyAccess) : MomentPrivacySettingsEvent
    data class SelectGroupInvites(val access: MomentPrivacyAccess) : MomentPrivacySettingsEvent
    data class SelectAvatarVisibility(val access: MomentVisibilityAccess) : MomentPrivacySettingsEvent
    data class SelectPhoneVisibility(val access: MomentPrivacyAccess) : MomentPrivacySettingsEvent
    data class SelectPresenceVisibility(val access: MomentVisibilityAccess) : MomentPrivacySettingsEvent
}
