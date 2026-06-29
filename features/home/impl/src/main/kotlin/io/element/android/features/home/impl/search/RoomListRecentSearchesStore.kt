/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.search

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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

interface RoomListRecentSearchesStore {
    val roomIds: StateFlow<List<RoomId>>

    suspend fun track(roomIds: List<RoomId>)
}

@ContributesBinding(SessionScope::class)
@SingleIn(SessionScope::class)
class DefaultRoomListRecentSearchesStore @Inject constructor(
    matrixClient: MatrixClient,
    preferenceDataStoreFactory: PreferenceDataStoreFactory,
    @SessionCoroutineScope
    sessionCoroutineScope: CoroutineScope,
) : RoomListRecentSearchesStore {
    private companion object {
        const val MAX_SEARCHES = 5
        const val ROOM_ID_SEPARATOR = "\n"
        val ROOM_IDS_KEY = stringPreferencesKey("room_ids")
    }

    private val store = preferenceDataStoreFactory.create("moment_global_search_recent_searches_${matrixClient.sessionId.value.hashCode().toUIntString()}")

    override val roomIds: StateFlow<List<RoomId>> = store.data
        .map { preferences ->
            preferences[ROOM_IDS_KEY]
                .orEmpty()
                .split(ROOM_ID_SEPARATOR)
                .filter(String::isNotBlank)
                .map(::RoomId)
        }
        .stateIn(sessionCoroutineScope, SharingStarted.Eagerly, emptyList())

    override suspend fun track(roomIds: List<RoomId>) {
        val uniqueNewRoomIds = roomIds.distinct()
        if (uniqueNewRoomIds.isEmpty()) return

        store.edit { preferences ->
            val currentRoomIds = preferences[ROOM_IDS_KEY]
                .orEmpty()
                .split(ROOM_ID_SEPARATOR)
                .filter(String::isNotBlank)
                .map(::RoomId)
            val updatedRoomIds = (uniqueNewRoomIds + currentRoomIds.filterNot(uniqueNewRoomIds::contains))
                .take(MAX_SEARCHES)

            if (updatedRoomIds.isEmpty()) {
                preferences.remove(ROOM_IDS_KEY)
            } else {
                preferences[ROOM_IDS_KEY] = updatedRoomIds.joinToString(ROOM_ID_SEPARATOR) { it.value }
            }
        }
    }
}

private fun Int.toUIntString(): String {
    return Integer.toUnsignedString(this, 16)
}
