/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.test

import io.element.android.libraries.matrix.api.HomeserverCapabilitiesProvider
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.analytics.SdkStoreSizes
import io.element.android.libraries.matrix.api.core.DeviceId
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomAlias
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.RoomIdOrAlias
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.ThreadId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.createroom.CreateRoomParameters
import io.element.android.libraries.matrix.api.encryption.EncryptionService
import io.element.android.libraries.matrix.api.linknewdevice.LinkDesktopHandler
import io.element.android.libraries.matrix.api.linknewdevice.LinkMobileHandler
import io.element.android.libraries.matrix.api.media.MatrixMediaLoader
import io.element.android.libraries.matrix.api.media.MatrixStickerInfo
import io.element.android.libraries.matrix.api.media.MediaPreviewService
import io.element.android.libraries.matrix.api.notification.NotificationService
import io.element.android.libraries.matrix.api.notificationsettings.NotificationSettingsService
import io.element.android.libraries.matrix.api.oauth.AccountManagementAction
import io.element.android.libraries.matrix.api.privacy.MatrixMomentPrivacySettings
import io.element.android.libraries.matrix.api.pusher.PushersService
import io.element.android.libraries.matrix.api.room.BaseRoom
import io.element.android.libraries.matrix.api.room.JoinedRoom
import io.element.android.libraries.matrix.api.room.NotJoinedRoom
import io.element.android.libraries.matrix.api.room.RoomInfo
import io.element.android.libraries.matrix.api.room.RoomMembershipObserver
import io.element.android.libraries.matrix.api.room.alias.ResolvedRoomAlias
import io.element.android.libraries.matrix.api.room.location.BeaconInfoUpdate
import io.element.android.libraries.matrix.api.roomdirectory.RoomDirectoryService
import io.element.android.libraries.matrix.api.roomlist.RoomListService
import io.element.android.libraries.matrix.api.search.MatrixMessageSearchPage
import io.element.android.libraries.matrix.api.session.DeleteSessionDeviceResult
import io.element.android.libraries.matrix.api.session.MatrixSessionDevice
import io.element.android.libraries.matrix.api.spaces.SpaceService
import io.element.android.libraries.matrix.api.sync.SlidingSyncVersion
import io.element.android.libraries.matrix.api.sync.SyncService
import io.element.android.libraries.matrix.api.user.MatrixMomentUserSearchMatch
import io.element.android.libraries.matrix.api.user.MatrixProfileLink
import io.element.android.libraries.matrix.api.user.MatrixProfileUsername
import io.element.android.libraries.matrix.api.user.MatrixPublicProfile
import io.element.android.libraries.matrix.api.user.MatrixSearchUserResults
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.api.verification.SessionVerificationService
import io.element.android.libraries.matrix.test.encryption.FakeEncryptionService
import io.element.android.libraries.matrix.test.media.FakeMatrixMediaLoader
import io.element.android.libraries.matrix.test.media.FakeMediaPreviewService
import io.element.android.libraries.matrix.test.notification.FakeNotificationService
import io.element.android.libraries.matrix.test.notificationsettings.FakeNotificationSettingsService
import io.element.android.libraries.matrix.test.pushers.FakePushersService
import io.element.android.libraries.matrix.test.roomdirectory.FakeRoomDirectoryService
import io.element.android.libraries.matrix.test.roomlist.FakeRoomListService
import io.element.android.libraries.matrix.test.spaces.FakeSpaceService
import io.element.android.libraries.matrix.test.sync.FakeSyncService
import io.element.android.libraries.matrix.test.verification.FakeSessionVerificationService
import io.element.android.tests.testutils.lambda.lambdaError
import io.element.android.tests.testutils.lambda.lambdaRecorder
import io.element.android.tests.testutils.simulateLongTask
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import java.util.Optional

class FakeMatrixClient(
    override val sessionId: SessionId = A_SESSION_ID,
    override val deviceId: DeviceId = A_DEVICE_ID,
    override val sessionCoroutineScope: CoroutineScope = TestScope(),
    private val userDisplayName: String? = A_USER_NAME,
    private val userAvatarUrl: String? = AN_AVATAR_URL,
    override val roomListService: RoomListService = FakeRoomListService(),
    override val spaceService: SpaceService = FakeSpaceService(),
    override val matrixMediaLoader: MatrixMediaLoader = FakeMatrixMediaLoader(),
    override val sessionVerificationService: SessionVerificationService = FakeSessionVerificationService(),
    override val pushersService: PushersService = FakePushersService(),
    override val notificationService: NotificationService = FakeNotificationService(),
    override val notificationSettingsService: NotificationSettingsService = FakeNotificationSettingsService(),
    override val syncService: SyncService = FakeSyncService(),
    override val encryptionService: EncryptionService = FakeEncryptionService(),
    override val roomDirectoryService: RoomDirectoryService = FakeRoomDirectoryService(),
    override val mediaPreviewService: MediaPreviewService = FakeMediaPreviewService(),
    override val roomMembershipObserver: RoomMembershipObserver = RoomMembershipObserver(),
    private val homeserverCapabilitiesProvider: FakeHomeserverCapabilitiesProvider = FakeHomeserverCapabilitiesProvider(),
    private val accountManagementUrlResult: (AccountManagementAction?) -> Result<String?> = { lambdaError() },
    private val getAccountDataResult: (String) -> Result<String?> = { Result.success(null) },
    private val setAccountDataResult: (String, String) -> Result<Unit> = { _, _ -> Result.success(Unit) },
    private var syncMomentPrivacySettingsResult: (MatrixMomentPrivacySettings) -> Result<Unit> = { Result.success(Unit) },
    private val searchMessagesResult: (String, String?, RoomId?) -> Result<MatrixMessageSearchPage> = { _, _, _ ->
        Result.success(MatrixMessageSearchPage(emptyList(), null))
    },
    private val searchMomentUsersResult: (String, Int, String?) -> Result<List<MatrixMomentUserSearchMatch>> = { _, _, _ ->
        Result.success(emptyList())
    },
    private val getProfileStatusResult: (UserId) -> Result<String> = { Result.success("") },
    private var setProfileStatusResult: (String) -> Result<Unit> = { Result.success(Unit) },
    private val getProfileUsernameResult: (UserId) -> Result<String> = { Result.success("") },
    private var setProfileUsernameResult: (String, String) -> Result<String> = { username, _ -> Result.success(MatrixProfileUsername.normalize(username)) },
    private val getPublicProfileResult: (UserId) -> Result<MatrixPublicProfile?> = { Result.success(null) },
    private val createUserProfileLinkResult: (UserId) -> Result<String?> = { Result.success(MatrixProfileLink.fallbackUserLink(it)) },
    private val ensureSavedMessagesRoomResult: () -> Result<RoomId> = { Result.success(A_ROOM_ID) },
    private val getSessionDevicesResult: () -> Result<List<MatrixSessionDevice>> = {
        Result.success(
            listOf(
                MatrixSessionDevice(
                    deviceId = deviceId,
                    displayName = deviceId.value,
                    lastSeenIp = null,
                    lastSeenTimestamp = null,
                    isCurrent = true,
                )
            )
        )
    },
    private val deleteSessionDeviceResult: (DeviceId) -> Result<DeleteSessionDeviceResult> = { Result.success(DeleteSessionDeviceResult.Deleted) },
    private val resolveRoomAliasResult: (RoomAlias) -> Result<Optional<ResolvedRoomAlias>> = {
        Result.success(
            Optional.of(ResolvedRoomAlias(A_ROOM_ID, emptyList()))
        )
    },
    private val getNotJoinedRoomResult: (RoomIdOrAlias, List<String>) -> Result<NotJoinedRoom> = { _, _ -> lambdaError() },
    private val clearCacheLambda: () -> Unit = { lambdaError() },
    private val userIdServerNameLambda: () -> String = { lambdaError() },
    private val getUrlLambda: (String) -> Result<ByteArray> = { lambdaError() },
    private val getRoomStateEventContentLambda: (RoomId, String) -> Result<String> = { _, _ -> lambdaError() },
    private val canDeactivateAccountResult: () -> Boolean = { lambdaError() },
    private val deactivateAccountResult: (String, Boolean) -> Result<Unit> = { _, _ -> lambdaError() },
    private val currentSlidingSyncVersionLambda: () -> Result<SlidingSyncVersion> = { lambdaError() },
    private val ignoreUserResult: (UserId) -> Result<Unit> = { lambdaError() },
    private val canLinkNewDeviceResult: () -> Result<Boolean> = { lambdaError() },
    private val createLinkMobileHandlerResult: () -> Result<LinkMobileHandler> = { lambdaError() },
    private val createLinkDesktopHandlerResult: () -> Result<LinkDesktopHandler> = { lambdaError() },
    private var unIgnoreUserResult: (UserId) -> Result<Unit> = { Result.success(Unit) },
    private val canReportRoomLambda: () -> Boolean = { false },
    private val isLivekitRtcSupportedLambda: () -> Boolean = { false },
    override val ignoredUsersFlow: StateFlow<ImmutableList<UserId>> = MutableStateFlow(persistentListOf()),
    override val ownBeaconInfoUpdates: Flow<BeaconInfoUpdate> = emptyFlow(),
    private val getMaxUploadSizeResult: () -> Result<Long> = { lambdaError() },
    private val getJoinedRoomIdsResult: () -> Result<Set<RoomId>> = { Result.success(emptySet()) },
    private val getRecentEmojisLambda: () -> Result<List<String>> = { Result.success(emptyList()) },
    private val addRecentEmojiLambda: (String) -> Result<Unit> = { Result.success(Unit) },
    private val markRoomAsFullyReadResult: (RoomId, EventId) -> Result<Unit> = { _, _ -> lambdaError() },
    private val performDatabaseVacuumLambda: () -> Result<Unit> = { lambdaError() },
    private val getDatabaseSizesLambda: () -> Result<SdkStoreSizes> = { lambdaError() },
    private val resetWellKnownConfigLambda: () -> Result<Unit> = { lambdaError() },
) : MatrixClient {
    data class SentSticker(
        val roomId: RoomId,
        val body: String,
        val url: String,
        val info: MatrixStickerInfo,
        val threadRootId: ThreadId?,
    )

    var setDisplayNameCalled: Boolean = false
        private set
    var uploadAvatarCalled: Boolean = false
        private set
    var removeAvatarCalled: Boolean = false
        private set
    var setProfileStatusCalled: Boolean = false
        private set
    var setProfileUsernameCalled: Boolean = false
        private set
    var latestSyncedMomentPrivacySettings: MatrixMomentPrivacySettings? = null
        private set

    private val _userProfile: MutableStateFlow<MatrixUser> = MutableStateFlow(MatrixUser(sessionId, userDisplayName, userAvatarUrl))
    override val userProfile: StateFlow<MatrixUser> = _userProfile

    private var createRoomResult: Result<RoomId> = Result.success(A_ROOM_ID)
    private var createDmResult: Result<RoomId> = Result.success(A_ROOM_ID)
    private var findDmResult: Result<RoomId?> = Result.success(A_ROOM_ID)
    private val getRoomResults = mutableMapOf<RoomId, BaseRoom>()
    private val searchUserResults = mutableMapOf<String, Result<MatrixSearchUserResults>>()
    private val getProfileResults = mutableMapOf<UserId, Result<MatrixUser>>()
    private var uploadMediaResult: Result<String> = Result.success(AN_AVATAR_URL)
    private var sendStickerResult: Result<Unit> = Result.success(Unit)
    val sentStickers = mutableListOf<SentSticker>()
    private var setDisplayNameResult: Result<Unit> = Result.success(Unit)
    private var uploadAvatarResult: Result<Unit> = Result.success(Unit)
    private var removeAvatarResult: Result<Unit> = Result.success(Unit)
    private var profileStatus: String? = null

    var latestCreateRoomParameters: CreateRoomParameters? = null
        private set
    private var profileUsername: String? = null
    private var publicProfile: MatrixPublicProfile? = null
    private var userProfileLink: String? = null
    private val accountData = mutableMapOf<String, String>()
    private var sessionDevices: List<MatrixSessionDevice>? = null
    var joinRoomLambda: (RoomId) -> Result<RoomInfo?> = {
        Result.success(null)
    }
    var joinRoomByIdOrAliasLambda: (RoomIdOrAlias, List<String>) -> Result<RoomInfo?> = { _, _ ->
        Result.success(null)
    }
    var knockRoomLambda: (RoomIdOrAlias, String, List<String>) -> Result<RoomInfo?> = { _, _, _ ->
        Result.success(null)
    }
    var getRoomInfoFlowLambda = { _: RoomId ->
        flowOf<Optional<RoomInfo>>(Optional.empty())
    }
    var logoutLambda: (Boolean, Boolean) -> Unit = { _, _ -> }

    override suspend fun getRoom(roomId: RoomId): BaseRoom? {
        return getRoomResults[roomId]
    }

    override suspend fun getJoinedRoom(roomId: RoomId): JoinedRoom? {
        return getRoomResults[roomId] as? JoinedRoom
    }

    override suspend fun findDM(userId: UserId): Result<RoomId?> {
        return findDmResult
    }

    override suspend fun getJoinedRoomIds(): Result<Set<RoomId>> {
        return getJoinedRoomIdsResult()
    }

    override suspend fun ignoreUser(userId: UserId): Result<Unit> = simulateLongTask {
        return ignoreUserResult(userId)
    }

    override suspend fun unignoreUser(userId: UserId): Result<Unit> = simulateLongTask {
        return unIgnoreUserResult(userId)
    }

    override suspend fun createRoom(createRoomParams: CreateRoomParameters): Result<RoomId> = simulateLongTask {
        latestCreateRoomParameters = createRoomParams
        return createRoomResult
    }

    override suspend fun createDM(userId: UserId): Result<RoomId> = simulateLongTask {
        return createDmResult
    }

    override suspend fun ensureSavedMessagesRoom(): Result<RoomId> {
        return ensureSavedMessagesRoomResult()
    }

    override suspend fun getProfile(userId: UserId): Result<MatrixUser> {
        return getProfileResults[userId] ?: Result.failure(IllegalStateException("No profile found for $userId"))
    }

    override suspend fun searchUsers(searchTerm: String, limit: Long): Result<MatrixSearchUserResults> {
        return searchUserResults[searchTerm] ?: Result.failure(IllegalStateException("No response defined for $searchTerm"))
    }

    override suspend fun searchMomentUsers(query: String, limit: Int, defaultCountry: String?): Result<List<MatrixMomentUserSearchMatch>> {
        return searchMomentUsersResult(query, limit, defaultCountry)
    }

    override suspend fun searchMessages(searchTerm: String, nextBatch: String?, roomId: RoomId?): Result<MatrixMessageSearchPage> {
        return searchMessagesResult(searchTerm, nextBatch, roomId)
    }

    override suspend fun getCacheSize(): Long {
        return 0
    }

    override suspend fun getDatabaseSizes(): Result<SdkStoreSizes> {
        return getDatabaseSizesLambda()
    }

    override suspend fun clearCache() = simulateLongTask {
        clearCacheLambda()
    }

    override suspend fun logout(userInitiated: Boolean, ignoreSdkError: Boolean) = simulateLongTask {
        logoutLambda(ignoreSdkError, userInitiated)
    }

    override fun canDeactivateAccount() = canDeactivateAccountResult()

    override suspend fun deactivateAccount(password: String, eraseData: Boolean): Result<Unit> = simulateLongTask {
        deactivateAccountResult(password, eraseData)
    }

    override suspend fun getUserProfile(): Result<MatrixUser> = simulateLongTask {
        val result = getProfileResults[sessionId]?.getOrNull() ?: MatrixUser(sessionId, userDisplayName, userAvatarUrl)
        _userProfile.tryEmit(result)
        return Result.success(result)
    }

    override suspend fun getProfileStatus(): Result<String> = getProfileStatus(sessionId)

    override suspend fun getProfileStatus(userId: UserId): Result<String> = simulateLongTask {
        return profileStatus?.let { Result.success(it) } ?: getProfileStatusResult(userId)
    }

    override suspend fun setProfileStatus(status: String): Result<Unit> = simulateLongTask {
        setProfileStatusCalled = true
        val result = setProfileStatusResult(status)
        if (result.isSuccess) {
            profileStatus = status.trim()
        }
        return result
    }

    override suspend fun getProfileUsername(userId: UserId): Result<String> = simulateLongTask {
        return profileUsername?.let { Result.success(it) } ?: getProfileUsernameResult(userId)
    }

    override suspend fun setProfileUsername(username: String, displayName: String): Result<String> = simulateLongTask {
        setProfileUsernameCalled = true
        val result = setProfileUsernameResult(username, displayName)
        result.onSuccess {
            profileUsername = it
        }
        return result
    }

    override suspend fun getPublicProfile(userId: UserId): Result<MatrixPublicProfile?> = simulateLongTask {
        return publicProfile?.let { Result.success(it) } ?: getPublicProfileResult(userId)
    }

    override suspend fun createUserProfileLink(userId: UserId): Result<String?> = simulateLongTask {
        return userProfileLink?.let { Result.success(it) } ?: createUserProfileLinkResult(userId)
    }

    override suspend fun getAccountManagementUrl(action: AccountManagementAction?): Result<String?> = simulateLongTask {
        accountManagementUrlResult(action)
    }

    override suspend fun getAccountData(eventType: String): Result<String?> = simulateLongTask {
        accountData[eventType]?.let { return@simulateLongTask Result.success(it) }
        return@simulateLongTask getAccountDataResult(eventType)
    }

    override suspend fun setAccountData(eventType: String, content: String): Result<Unit> = simulateLongTask {
        val result = setAccountDataResult(eventType, content)
        if (result.isSuccess) {
            accountData[eventType] = content
        }
        return@simulateLongTask result
    }

    override suspend fun syncMomentPrivacySettings(settings: MatrixMomentPrivacySettings): Result<Unit> = simulateLongTask {
        latestSyncedMomentPrivacySettings = settings
        return@simulateLongTask syncMomentPrivacySettingsResult(settings)
    }

    override suspend fun getSessionDevices(): Result<List<MatrixSessionDevice>> = simulateLongTask {
        return sessionDevices?.let { Result.success(it) } ?: getSessionDevicesResult()
    }

    override suspend fun deleteSessionDevice(deviceId: DeviceId): Result<DeleteSessionDeviceResult> = simulateLongTask {
        val result = deleteSessionDeviceResult(deviceId)
        if (result.getOrNull() == DeleteSessionDeviceResult.Deleted) {
            sessionDevices = (sessionDevices ?: getSessionDevicesResult().getOrNull()).orEmpty()
                .filterNot { it.deviceId == deviceId }
        }
        return@simulateLongTask result
    }

    override suspend fun uploadMedia(
        mimeType: String,
        data: ByteArray,
    ): Result<String> {
        return uploadMediaResult
    }

    override suspend fun sendSticker(
        roomId: RoomId,
        body: String,
        url: String,
        info: MatrixStickerInfo,
        threadRootId: ThreadId?,
    ): Result<Unit> {
        sentStickers += SentSticker(
            roomId = roomId,
            body = body,
            url = url,
            info = info,
            threadRootId = threadRootId,
        )
        return sendStickerResult
    }

    override suspend fun setDisplayName(displayName: String): Result<Unit> = simulateLongTask {
        setDisplayNameCalled = true
        return setDisplayNameResult
    }

    override suspend fun uploadAvatar(mimeType: String, data: ByteArray): Result<Unit> = simulateLongTask {
        uploadAvatarCalled = true
        return uploadAvatarResult
    }

    override suspend fun removeAvatar(): Result<Unit> = simulateLongTask {
        removeAvatarCalled = true
        return removeAvatarResult
    }

    override suspend fun joinRoom(roomId: RoomId): Result<RoomInfo?> = joinRoomLambda(roomId)

    override suspend fun joinRoomByIdOrAlias(roomIdOrAlias: RoomIdOrAlias, serverNames: List<String>): Result<RoomInfo?> {
        return joinRoomByIdOrAliasLambda(roomIdOrAlias, serverNames)
    }

    override suspend fun knockRoom(roomIdOrAlias: RoomIdOrAlias, message: String, serverNames: List<String>): Result<RoomInfo?> {
        return knockRoomLambda(roomIdOrAlias, message, serverNames)
    }

    // Mocks

    fun givenCreateRoomResult(result: Result<RoomId>) {
        createRoomResult = result
    }

    fun givenCreateDmResult(result: Result<RoomId>) {
        createDmResult = result
    }

    fun givenFindDmResult(result: Result<RoomId?>) {
        findDmResult = result
    }

    fun givenGetRoomResult(roomId: RoomId, result: BaseRoom?) {
        if (result == null) {
            getRoomResults.remove(roomId)
        } else {
            getRoomResults[roomId] = result
        }
    }

    fun givenSearchUsersResult(searchTerm: String, result: Result<MatrixSearchUserResults>) {
        searchUserResults[searchTerm] = result
    }

    fun givenGetProfileResult(userId: UserId, result: Result<MatrixUser>) {
        getProfileResults[userId] = result
    }

    fun givenUploadMediaResult(result: Result<String>) {
        uploadMediaResult = result
    }

    fun givenSendStickerResult(result: Result<Unit>) {
        sendStickerResult = result
    }

    fun givenSetDisplayNameResult(result: Result<Unit>) {
        setDisplayNameResult = result
    }

    fun givenUploadAvatarResult(result: Result<Unit>) {
        uploadAvatarResult = result
    }

    fun givenRemoveAvatarResult(result: Result<Unit>) {
        removeAvatarResult = result
    }

    fun givenProfileStatus(status: String?) {
        profileStatus = status
    }

    fun givenSetProfileStatusResult(result: Result<Unit>) {
        setProfileStatusResult = { result }
    }

    fun givenProfileUsername(username: String?) {
        profileUsername = username
    }

    fun givenSetProfileUsernameResult(result: Result<String>) {
        setProfileUsernameResult = { _, _ -> result }
    }

    fun givenPublicProfile(profile: MatrixPublicProfile?) {
        publicProfile = profile
    }

    fun givenUserProfileLink(link: String?) {
        userProfileLink = link
    }

    fun givenAccountData(eventType: String, content: String?) {
        if (content == null) {
            accountData.remove(eventType)
        } else {
            accountData[eventType] = content
        }
    }

    fun givenSyncMomentPrivacySettingsResult(result: Result<Unit>) {
        syncMomentPrivacySettingsResult = { result }
    }

    fun givenSessionDevices(devices: List<MatrixSessionDevice>) {
        sessionDevices = devices
    }

    private val visitedRoomsId: MutableList<RoomId> = mutableListOf()

    override suspend fun trackRecentlyVisitedRoom(roomId: RoomId): Result<Unit> {
        visitedRoomsId.removeAll { it == roomId }
        visitedRoomsId.add(0, roomId)
        return Result.success(Unit)
    }

    override suspend fun resolveRoomAlias(roomAlias: RoomAlias): Result<Optional<ResolvedRoomAlias>> = simulateLongTask {
        resolveRoomAliasResult(roomAlias)
    }

    override suspend fun getRoomPreview(roomIdOrAlias: RoomIdOrAlias, serverNames: List<String>): Result<NotJoinedRoom> = simulateLongTask {
        getNotJoinedRoomResult(roomIdOrAlias, serverNames)
    }

    override suspend fun getRecentlyVisitedRooms(): Result<List<RoomId>> {
        return Result.success(visitedRoomsId)
    }

    override fun getRoomInfoFlow(roomId: RoomId) = getRoomInfoFlowLambda(roomId)

    var setAllSendQueuesEnabledLambda = lambdaRecorder(ensureNeverCalled = true) { _: Boolean ->
        // no-op
    }

    override suspend fun setAllSendQueuesEnabled(enabled: Boolean) = setAllSendQueuesEnabledLambda(enabled)

    var sendQueueDisabledFlow = emptyFlow<RoomId>()
    override fun sendQueueDisabledFlow(): Flow<RoomId> = sendQueueDisabledFlow

    override fun userIdServerName(): String {
        return userIdServerNameLambda()
    }

    override suspend fun getUrl(url: String): Result<ByteArray> {
        return getUrlLambda(url)
    }

    override suspend fun getRoomStateEventContent(roomId: RoomId, eventType: String): Result<String> {
        return getRoomStateEventContentLambda(roomId, eventType)
    }

    override suspend fun currentSlidingSyncVersion(): Result<SlidingSyncVersion> {
        return currentSlidingSyncVersionLambda()
    }

    override suspend fun canReportRoom(): Boolean {
        return canReportRoomLambda()
    }

    override suspend fun isLivekitRtcSupported(): Boolean {
        return isLivekitRtcSupportedLambda()
    }

    override suspend fun getMaxFileUploadSize(): Result<Long> {
        return getMaxUploadSizeResult()
    }

    override suspend fun addRecentEmoji(emoji: String): Result<Unit> {
        return addRecentEmojiLambda(emoji)
    }

    override suspend fun getRecentEmojis(): Result<List<String>> {
        return getRecentEmojisLambda()
    }

    override suspend fun markRoomAsFullyRead(roomId: RoomId, eventId: EventId): Result<Unit> {
        return markRoomAsFullyReadResult(roomId, eventId)
    }

    override suspend fun performDatabaseVacuum(): Result<Unit> {
        return performDatabaseVacuumLambda()
    }

    override suspend fun canLinkNewDevice(): Result<Boolean> = simulateLongTask {
        return canLinkNewDeviceResult()
    }

    override fun createLinkDesktopHandler(): Result<LinkDesktopHandler> {
        return createLinkDesktopHandlerResult()
    }

    override fun createLinkMobileHandler(): Result<LinkMobileHandler> {
        return createLinkMobileHandlerResult()
    }

    override suspend fun resetWellKnownConfig(): Result<Unit> {
        return resetWellKnownConfigLambda()
    }

    override fun homeserverCapabilities(): HomeserverCapabilitiesProvider {
        return homeserverCapabilitiesProvider
    }
}
