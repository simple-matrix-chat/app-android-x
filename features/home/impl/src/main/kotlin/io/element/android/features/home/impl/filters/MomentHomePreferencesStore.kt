/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.filters

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.di.annotations.SessionCoroutineScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.preferences.api.store.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

interface MomentHomePreferencesStore {
    val archivedRoomIds: StateFlow<Set<RoomId>>

    suspend fun setRoomArchived(roomId: RoomId, archived: Boolean)
}

@ContributesBinding(SessionScope::class)
@SingleIn(SessionScope::class)
class DefaultMomentHomePreferencesStore @Inject constructor(
    matrixClient: MatrixClient,
    preferenceDataStoreFactory: PreferenceDataStoreFactory,
    @SessionCoroutineScope
    sessionCoroutineScope: CoroutineScope,
) : MomentHomePreferencesStore {
    private companion object {
        val ARCHIVED_ROOM_IDS_KEY = stringSetPreferencesKey("archived_room_ids")
    }

    private val store = preferenceDataStoreFactory.create("moment_home_preferences_${matrixClient.sessionId.value.hashCode().toUIntString()}")

    override val archivedRoomIds: StateFlow<Set<RoomId>> = store.data
        .map { preferences ->
            preferences[ARCHIVED_ROOM_IDS_KEY]
                .orEmpty()
                .map(::RoomId)
                .toSet()
        }
        .stateIn(sessionCoroutineScope, SharingStarted.Eagerly, emptySet())

    override suspend fun setRoomArchived(roomId: RoomId, archived: Boolean) {
        store.edit { preferences ->
            val archivedRoomIds = preferences[ARCHIVED_ROOM_IDS_KEY].orEmpty().toMutableSet()
            if (archived) {
                archivedRoomIds.add(roomId.value)
            } else {
                archivedRoomIds.remove(roomId.value)
            }
            if (archivedRoomIds.isEmpty()) {
                preferences.remove(ARCHIVED_ROOM_IDS_KEY)
            } else {
                preferences[ARCHIVED_ROOM_IDS_KEY] = archivedRoomIds
            }
        }
    }
}

private fun Int.toUIntString(): String {
    return Integer.toUnsignedString(this, 16)
}
