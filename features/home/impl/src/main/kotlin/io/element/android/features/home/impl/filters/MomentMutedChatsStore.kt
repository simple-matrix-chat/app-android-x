/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.filters

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.di.annotations.SessionCoroutineScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.room.RoomNotificationMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import timber.log.Timber

enum class MomentHomeMuteDuration(val expiresAfterMillis: Long?) {
    Hours8(8 * 60 * 60 * 1_000L),
    OneWeek(7 * 24 * 60 * 60 * 1_000L),
    Forever(null),
}

interface MomentMutedChatsStore {
    val finiteMutedRoomIds: StateFlow<Set<RoomId>>

    suspend fun syncExpiredMutedChats(nowMillis: Long = System.currentTimeMillis())
    suspend fun muteRoom(roomId: RoomId, duration: MomentHomeMuteDuration, isEncrypted: Boolean, isOneToOne: Boolean): Result<Unit>
    suspend fun unmuteRoom(roomId: RoomId, isEncrypted: Boolean, isOneToOne: Boolean): Result<Unit>
}

@ContributesBinding(SessionScope::class)
@SingleIn(SessionScope::class)
class DefaultMomentMutedChatsStore @Inject constructor(
    private val matrixClient: MatrixClient,
    @SessionCoroutineScope
    private val sessionCoroutineScope: CoroutineScope,
) : MomentMutedChatsStore {
    private companion object {
        const val ACCOUNT_DATA_EVENT_TYPE = "io.moment.muted_chats"
    }

    private val finiteMutedChatExpiries = MutableStateFlow<Map<RoomId, Long>>(emptyMap())
    private var nextExpiryJob: Job? = null

    override val finiteMutedRoomIds = MutableStateFlow<Set<RoomId>>(emptySet())

    init {
        sessionCoroutineScope.launch {
            reload()
            syncExpiredMutedChats()
        }
    }

    override suspend fun syncExpiredMutedChats(nowMillis: Long) {
        val currentExpiries = finiteMutedChatExpiries.value
        val expiredRoomIds = currentExpiries
            .filterValues { expiresAtMillis -> expiresAtMillis <= nowMillis }
            .keys

        if (expiredRoomIds.isEmpty()) {
            scheduleNextExpirySync()
            return
        }

        val updatedExpiries = currentExpiries.toMutableMap()
        var shouldPersist = false
        expiredRoomIds.forEach { roomId ->
            val room = matrixClient.getRoom(roomId)
            if (room == null) {
                updatedExpiries.remove(roomId)
                shouldPersist = true
                return@forEach
            }

            room.use {
                val roomInfo = it.roomInfoFlow.value
                val result = matrixClient.notificationSettingsService.unmuteRoom(
                    roomId = roomId,
                    isEncrypted = roomInfo.isEncrypted == true,
                    isOneToOne = roomInfo.isDm,
                )
                if (result.isSuccess) {
                    updatedExpiries.remove(roomId)
                    shouldPersist = true
                } else {
                    Timber.e(result.exceptionOrNull(), "Failed unmuting expired Moment mute for room $roomId")
                }
            }
        }

        if (!shouldPersist) {
            scheduleNextExpirySync()
            return
        }

        persistFiniteMutedChatExpiries(updatedExpiries)
            .onSuccess {
                updateFiniteMutedChatExpiries(updatedExpiries)
            }
            .onFailure { error ->
                Timber.e(error, "Failed persisting Moment muted chats after expiry sync.")
            }
    }

    override suspend fun muteRoom(
        roomId: RoomId,
        duration: MomentHomeMuteDuration,
        isEncrypted: Boolean,
        isOneToOne: Boolean,
    ): Result<Unit> {
        val muteResult = matrixClient.notificationSettingsService.setRoomNotificationMode(roomId, RoomNotificationMode.MUTE)
        if (muteResult.isFailure) {
            return muteResult
        }

        val updatedExpiries = finiteMutedChatExpiries.value.toMutableMap()
        val expiresAfterMillis = duration.expiresAfterMillis
        if (expiresAfterMillis == null) {
            updatedExpiries.remove(roomId)
        } else {
            updatedExpiries[roomId] = System.currentTimeMillis() + expiresAfterMillis
        }

        val persistResult = persistFiniteMutedChatExpiries(updatedExpiries)
        if (persistResult.isFailure) {
            Timber.e(persistResult.exceptionOrNull(), "Failed persisting Moment mute duration for room $roomId")
            return persistResult
        }

        updateFiniteMutedChatExpiries(updatedExpiries)
        return Result.success(Unit)
    }

    override suspend fun unmuteRoom(roomId: RoomId, isEncrypted: Boolean, isOneToOne: Boolean): Result<Unit> {
        val unmuteResult = matrixClient.notificationSettingsService.unmuteRoom(roomId, isEncrypted, isOneToOne)
        if (unmuteResult.isFailure) {
            return unmuteResult
        }

        val updatedExpiries = finiteMutedChatExpiries.value.toMutableMap()
        updatedExpiries.remove(roomId)

        val persistResult = persistFiniteMutedChatExpiries(updatedExpiries)
        if (persistResult.isFailure) {
            Timber.e(persistResult.exceptionOrNull(), "Failed removing Moment mute duration for room $roomId")
            return persistResult
        }

        updateFiniteMutedChatExpiries(updatedExpiries)
        return Result.success(Unit)
    }

    private suspend fun reload() {
        matrixClient.getAccountData(ACCOUNT_DATA_EVENT_TYPE)
            .onSuccess { content ->
                updateFiniteMutedChatExpiries(parseFiniteMutedChatExpiries(content))
            }
            .onFailure { error ->
                Timber.e(error, "Failed loading Moment muted chats from account data.")
            }
    }

    private fun updateFiniteMutedChatExpiries(expiries: Map<RoomId, Long>) {
        finiteMutedChatExpiries.value = expiries
        finiteMutedRoomIds.value = expiries.keys
        scheduleNextExpirySync()
    }

    private suspend fun persistFiniteMutedChatExpiries(expiries: Map<RoomId, Long>): Result<Unit> {
        return matrixClient.setAccountData(ACCOUNT_DATA_EVENT_TYPE, serializedFiniteMutedChatExpiries(expiries))
    }

    private fun scheduleNextExpirySync() {
        nextExpiryJob?.cancel()
        val nextExpiryMillis = finiteMutedChatExpiries.value.values.minOrNull() ?: return
        nextExpiryJob = sessionCoroutineScope.launch {
            delay((nextExpiryMillis - System.currentTimeMillis()).coerceAtLeast(0L))
            syncExpiredMutedChats()
        }
    }

    private fun parseFiniteMutedChatExpiries(content: String?): Map<RoomId, Long> {
        if (content == null) return emptyMap()
        val jsonObject = runCatching {
            Json.parseToJsonElement(content).jsonObject
        }.getOrElse {
            return emptyMap()
        }

        return jsonObject.mapNotNull { (roomId, expiresAtElement) ->
            val expiresAtMillis = expiresAtElement.jsonPrimitive.longOrNull ?: return@mapNotNull null
            if (expiresAtMillis > 0L) RoomId(roomId) to expiresAtMillis else null
        }.toMap()
    }

    private fun serializedFiniteMutedChatExpiries(expiries: Map<RoomId, Long>): String {
        return buildJsonObject {
            expiries.forEach { (roomId, expiresAtMillis) ->
                put(roomId.value, JsonPrimitive(expiresAtMillis))
            }
        }.toString()
    }
}
