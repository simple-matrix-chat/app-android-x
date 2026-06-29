/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.test.notificationsettings

import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.notificationsettings.NotificationSettingsService
import io.element.android.libraries.matrix.api.room.RoomNotificationMode
import io.element.android.libraries.matrix.api.room.RoomNotificationSettings
import io.element.android.libraries.matrix.test.A_ROOM_NOTIFICATION_MODE
import io.element.android.tests.testutils.lambda.lambdaError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow

class FakeNotificationSettingsService(
    initialRoomMode: RoomNotificationMode = A_ROOM_NOTIFICATION_MODE,
    initialRoomModeIsDefault: Boolean = true,
    initialGroupDefaultMode: RoomNotificationMode = RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY,
    initialEncryptedGroupDefaultMode: RoomNotificationMode = RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY,
    initialOneToOneDefaultMode: RoomNotificationMode = RoomNotificationMode.ALL_MESSAGES,
    initialEncryptedOneToOneDefaultMode: RoomNotificationMode = RoomNotificationMode.ALL_MESSAGES,
    private val getRawPushRulesResult: () -> Result<String> = { lambdaError() },
    private val getRoomsWithUserDefinedRulesResult: () -> Result<List<RoomId>> = { lambdaError() },
) : NotificationSettingsService {
    data class RoomNotificationSettingsRequest(
        val roomId: RoomId,
        val isEncrypted: Boolean,
        val isOneToOne: Boolean,
    )

    data class DefaultRoomNotificationModeRequest(
        val isEncrypted: Boolean,
        val isOneToOne: Boolean,
    )

    data class SetDefaultRoomNotificationModeRequest(
        val isEncrypted: Boolean,
        val mode: RoomNotificationMode,
        val isDm: Boolean,
    )

    private val notificationSettingsStateFlow = MutableStateFlow(Unit)
    private var defaultGroupRoomNotificationMode: RoomNotificationMode = initialGroupDefaultMode
    private var defaultEncryptedGroupRoomNotificationMode: RoomNotificationMode = initialEncryptedGroupDefaultMode
    private var defaultOneToOneRoomNotificationMode: RoomNotificationMode = initialOneToOneDefaultMode
    private var defaultEncryptedOneToOneRoomNotificationMode: RoomNotificationMode = initialEncryptedOneToOneDefaultMode
    private var roomNotificationMode: RoomNotificationMode = initialRoomMode
    private var roomNotificationModeIsDefault: Boolean = initialRoomModeIsDefault
    private var callNotificationsEnabled = false
    private var inviteNotificationsEnabled = false
    private var atRoomNotificationsEnabled = false
    private var setNotificationModeError: Throwable? = null
    private var restoreDefaultNotificationModeError: Throwable? = null
    private var setDefaultNotificationModeError: Throwable? = null
    private var setAtRoomError: Throwable? = null
    private var canHomeServerPushEncryptedEventsToDeviceResult = Result.success(true)
    var lastGetRoomNotificationSettingsRequest: RoomNotificationSettingsRequest? = null
        private set
    var lastGetDefaultRoomNotificationModeRequest: DefaultRoomNotificationModeRequest? = null
        private set
    var lastUnmuteRoomRequest: RoomNotificationSettingsRequest? = null
        private set
    val getDefaultRoomNotificationModeRequests = mutableListOf<DefaultRoomNotificationModeRequest>()
    val setDefaultRoomNotificationModeRequests = mutableListOf<SetDefaultRoomNotificationModeRequest>()

    override val notificationSettingsChangeFlow: SharedFlow<Unit>
        get() = notificationSettingsStateFlow

    override suspend fun getRoomNotificationSettings(roomId: RoomId, isEncrypted: Boolean, isOneToOne: Boolean): Result<RoomNotificationSettings> {
        lastGetRoomNotificationSettingsRequest = RoomNotificationSettingsRequest(roomId, isEncrypted, isOneToOne)
        return Result.success(
            RoomNotificationSettings(
                mode = if (roomNotificationModeIsDefault) defaultMode(isEncrypted, isOneToOne) else roomNotificationMode,
                isDefault = roomNotificationModeIsDefault
            )
        )
    }

    override suspend fun getDefaultRoomNotificationMode(isEncrypted: Boolean, isOneToOne: Boolean): Result<RoomNotificationMode> {
        val request = DefaultRoomNotificationModeRequest(isEncrypted, isOneToOne)
        lastGetDefaultRoomNotificationModeRequest = request
        getDefaultRoomNotificationModeRequests += request
        return Result.success(defaultMode(isEncrypted, isOneToOne))
    }

    override suspend fun setDefaultRoomNotificationMode(isEncrypted: Boolean, mode: RoomNotificationMode, isDM: Boolean): Result<Unit> {
        setDefaultRoomNotificationModeRequests += SetDefaultRoomNotificationModeRequest(isEncrypted, mode, isDM)
        val error = setDefaultNotificationModeError
        if (error != null) {
            return Result.failure(error)
        }
        if (isDM) {
            if (isEncrypted) {
                defaultEncryptedOneToOneRoomNotificationMode = mode
            } else {
                defaultOneToOneRoomNotificationMode = mode
            }
        } else {
            if (isEncrypted) {
                defaultEncryptedGroupRoomNotificationMode = mode
            } else {
                defaultGroupRoomNotificationMode = mode
            }
        }
        notificationSettingsStateFlow.emit(Unit)
        return Result.success(Unit)
    }

    override suspend fun setRoomNotificationMode(roomId: RoomId, mode: RoomNotificationMode): Result<Unit> {
        val error = setNotificationModeError
        return if (error != null) {
            Result.failure(error)
        } else {
            roomNotificationModeIsDefault = false
            roomNotificationMode = mode
            notificationSettingsStateFlow.emit(Unit)
            Result.success(Unit)
        }
    }

    override suspend fun restoreDefaultRoomNotificationMode(roomId: RoomId): Result<Unit> {
        val error = restoreDefaultNotificationModeError
        if (error != null) {
            return Result.failure(error)
        }
        roomNotificationModeIsDefault = true
        roomNotificationMode = defaultEncryptedGroupRoomNotificationMode
        notificationSettingsStateFlow.emit(Unit)
        return Result.success(Unit)
    }

    override suspend fun muteRoom(roomId: RoomId): Result<Unit> {
        return setRoomNotificationMode(roomId, RoomNotificationMode.MUTE)
    }

    override suspend fun unmuteRoom(roomId: RoomId, isEncrypted: Boolean, isOneToOne: Boolean): Result<Unit> {
        lastUnmuteRoomRequest = RoomNotificationSettingsRequest(roomId, isEncrypted, isOneToOne)
        val error = restoreDefaultNotificationModeError
        if (error != null) {
            return Result.failure(error)
        }
        roomNotificationModeIsDefault = true
        roomNotificationMode = defaultMode(isEncrypted, isOneToOne)
        notificationSettingsStateFlow.emit(Unit)
        return Result.success(Unit)
    }

    override suspend fun isRoomMentionEnabled(): Result<Boolean> {
        return Result.success(atRoomNotificationsEnabled)
    }

    override suspend fun setRoomMentionEnabled(enabled: Boolean): Result<Unit> {
        val error = setAtRoomError
        if (error != null) {
            return Result.failure(error)
        }
        atRoomNotificationsEnabled = enabled
        return Result.success(Unit)
    }

    override suspend fun isCallEnabled(): Result<Boolean> {
        return Result.success(callNotificationsEnabled)
    }

    override suspend fun setCallEnabled(enabled: Boolean): Result<Unit> {
        callNotificationsEnabled = enabled
        return Result.success(Unit)
    }

    override suspend fun isInviteForMeEnabled(): Result<Boolean> {
        return Result.success(inviteNotificationsEnabled)
    }

    override suspend fun setInviteForMeEnabled(enabled: Boolean): Result<Unit> {
        inviteNotificationsEnabled = enabled
        return Result.success(Unit)
    }

    override suspend fun getRoomsWithUserDefinedRules(): Result<List<RoomId>> {
        return getRoomsWithUserDefinedRulesResult()
    }

    override suspend fun canHomeServerPushEncryptedEventsToDevice(): Result<Boolean> {
        return canHomeServerPushEncryptedEventsToDeviceResult
    }

    fun givenSetNotificationModeError(throwable: Throwable?) {
        setNotificationModeError = throwable
    }

    fun givenRestoreDefaultNotificationModeError(throwable: Throwable?) {
        restoreDefaultNotificationModeError = throwable
    }

    fun givenSetAtRoomError(throwable: Throwable?) {
        setAtRoomError = throwable
    }

    fun givenSetDefaultNotificationModeError(throwable: Throwable?) {
        setDefaultNotificationModeError = throwable
    }

    fun givenCanHomeServerPushEncryptedEventsToDeviceResult(result: Result<Boolean>) {
        canHomeServerPushEncryptedEventsToDeviceResult = result
    }

    override suspend fun getRawPushRules(): Result<String?> {
        return getRawPushRulesResult()
    }

    private fun defaultMode(isEncrypted: Boolean, isOneToOne: Boolean): RoomNotificationMode {
        return if (isOneToOne) {
            if (isEncrypted) {
                defaultEncryptedOneToOneRoomNotificationMode
            } else {
                defaultOneToOneRoomNotificationMode
            }
        } else {
            if (isEncrypted) {
                defaultEncryptedGroupRoomNotificationMode
            } else {
                defaultGroupRoomNotificationMode
            }
        }
    }
}
