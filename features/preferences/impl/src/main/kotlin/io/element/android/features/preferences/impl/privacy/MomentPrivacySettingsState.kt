/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.privacy

import io.element.android.libraries.designsystem.utils.snackbar.SnackbarMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class MomentPrivacySettingsState(
    val settings: MomentPrivacySettings,
    val isLoading: Boolean,
    val savingSection: MomentPrivacySettingsSection?,
    val snackbarMessage: SnackbarMessage?,
    val eventSink: (MomentPrivacySettingsEvent) -> Unit,
) {
    val rowsEnabled: Boolean = !isLoading && savingSection == null
}

data class MomentPrivacySettings(
    val directMessages: MomentPrivacyAccess,
    val groupInvites: MomentPrivacyAccess,
    val avatarVisibility: MomentVisibilityAccess,
    val phoneVisibility: MomentPrivacyAccess,
    val presenceVisibility: MomentVisibilityAccess,
) {
    fun toJson(): String {
        return buildJsonObject {
            put(KEY_DIRECT_MESSAGES, directMessages.serializedValue)
            put(KEY_GROUP_INVITES, groupInvites.serializedValue)
            put(KEY_AVATAR_VISIBILITY, avatarVisibility.serializedValue)
            put(KEY_PHONE_VISIBILITY, phoneVisibility.serializedValue)
            put(KEY_PRESENCE_VISIBILITY, presenceVisibility.serializedValue)
        }.toString()
    }

    companion object {
        val Default = MomentPrivacySettings(
            directMessages = MomentPrivacyAccess.Everyone,
            groupInvites = MomentPrivacyAccess.Everyone,
            avatarVisibility = MomentVisibilityAccess.Everyone,
            phoneVisibility = MomentPrivacyAccess.ContactsOnly,
            presenceVisibility = MomentVisibilityAccess.Everyone,
        )

        fun fromJson(content: String?): MomentPrivacySettings {
            if (content.isNullOrBlank()) return Default
            return runCatching {
                val json = Json.parseToJsonElement(content).jsonObject
                MomentPrivacySettings(
                    directMessages = MomentPrivacyAccess.fromSerializedValue(json.stringOrNull(KEY_DIRECT_MESSAGES)) ?: Default.directMessages,
                    groupInvites = MomentPrivacyAccess.fromSerializedValue(json.stringOrNull(KEY_GROUP_INVITES)) ?: Default.groupInvites,
                    avatarVisibility = MomentVisibilityAccess.fromSerializedValue(json.stringOrNull(KEY_AVATAR_VISIBILITY)) ?: Default.avatarVisibility,
                    phoneVisibility = MomentPrivacyAccess.fromSerializedValue(json.stringOrNull(KEY_PHONE_VISIBILITY)) ?: Default.phoneVisibility,
                    presenceVisibility = MomentVisibilityAccess.fromSerializedValue(json.stringOrNull(KEY_PRESENCE_VISIBILITY)) ?: Default.presenceVisibility,
                )
            }.getOrDefault(Default)
        }
    }
}

private fun kotlinx.serialization.json.JsonObject.stringOrNull(key: String): String? {
    return get(key)?.jsonPrimitive?.contentOrNull
}

enum class MomentPrivacyAccess(val serializedValue: String) {
    Everyone("all"),
    ContactsOnly("contacts"),
    Nobody("nobody"),
    ;

    companion object {
        fun fromSerializedValue(value: String?): MomentPrivacyAccess? {
            return entries.firstOrNull { it.serializedValue == value }
        }
    }
}

enum class MomentVisibilityAccess(val serializedValue: String) {
    Everyone("all"),
    ContactsOnly("contacts"),
    ;

    companion object {
        fun fromSerializedValue(value: String?): MomentVisibilityAccess? {
            return entries.firstOrNull { it.serializedValue == value }
        }
    }
}

enum class MomentPrivacySettingsSection {
    DirectMessages,
    GroupInvites,
    AvatarVisibility,
    PhoneVisibility,
    PresenceVisibility,
}

private const val KEY_DIRECT_MESSAGES = "directMessages"
private const val KEY_GROUP_INVITES = "groupInvites"
private const val KEY_AVATAR_VISIBILITY = "avatarVisibility"
private const val KEY_PHONE_VISIBILITY = "phoneVisibility"
private const val KEY_PRESENCE_VISIBILITY = "presenceVisibility"
