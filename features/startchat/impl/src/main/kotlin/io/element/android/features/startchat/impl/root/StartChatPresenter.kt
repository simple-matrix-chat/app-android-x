/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.startchat.impl.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Inject
import im.vector.app.features.analytics.plan.CreatedRoom
import io.element.android.features.startchat.api.StartDMAction
import io.element.android.features.startchat.impl.userlist.SelectionMode
import io.element.android.features.startchat.impl.userlist.UserListDataStore
import io.element.android.features.startchat.impl.userlist.UserListPresenter
import io.element.android.features.startchat.impl.userlist.UserListPresenterArgs
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.architecture.runCatchingUpdatingState
import io.element.android.libraries.core.meta.BuildMeta
import io.element.android.libraries.designsystem.theme.components.SearchBarResultState
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.createroom.CreateRoomParameters
import io.element.android.libraries.matrix.api.createroom.MomentRoomKind
import io.element.android.libraries.matrix.api.createroom.RoomPreset
import io.element.android.libraries.matrix.api.room.history.RoomHistoryVisibility
import io.element.android.libraries.matrix.api.roomdirectory.RoomVisibility
import io.element.android.libraries.matrix.api.user.MatrixMomentUserSearchMatch
import io.element.android.libraries.usersearch.api.UserRepository
import io.element.android.libraries.usersearch.api.UserSearchResult
import io.element.android.services.analytics.api.AnalyticsService
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale

private const val MAX_MOMENT_USER_SEARCH_RESULTS = 20
private const val MAX_PHONEBOOK_CONTACT_LOOKUPS = 25

@Inject
class StartChatPresenter(
    presenterFactory: UserListPresenter.Factory,
    userRepository: UserRepository,
    userListDataStore: UserListDataStore,
    private val startDMAction: StartDMAction,
    private val matrixClient: MatrixClient,
    private val analyticsService: AnalyticsService,
    private val buildMeta: BuildMeta,
    private val deviceContactsDataSource: DeviceContactsDataSource,
) : Presenter<StartChatState> {
    private val presenter = presenterFactory.create(
        UserListPresenterArgs(
            selectionMode = SelectionMode.Single,
        ),
        userRepository,
        userListDataStore,
    )

    @Composable
    override fun present(): StartChatState {
        val userListState = presenter.present()

        val localCoroutineScope = rememberCoroutineScope()
        val startDmActionState: MutableState<AsyncAction<RoomId>> = remember { mutableStateOf(AsyncAction.Uninitialized) }
        val createMomentRoomActionState: MutableState<AsyncAction<RoomId>> = remember { mutableStateOf(AsyncAction.Uninitialized) }
        var momentUserSearchResults: ImmutableList<UserSearchResult> by remember { mutableStateOf(persistentListOf()) }
        var isSearchingMomentUsers by remember { mutableStateOf(false) }
        var phonebookContacts: ImmutableList<UserSearchResult> by remember { mutableStateOf(persistentListOf()) }
        val searchQuery = userListState.searchQuery.text.toString()

        LaunchedEffect(userListState.isSearchActive, searchQuery) {
            val trimmedQuery = searchQuery.trim()
            if (!userListState.isSearchActive || trimmedQuery.isEmpty()) {
                momentUserSearchResults = persistentListOf<UserSearchResult>()
                isSearchingMomentUsers = false
                return@LaunchedEffect
            }

            momentUserSearchResults = persistentListOf<UserSearchResult>()
            isSearchingMomentUsers = true
            val result = matrixClient.searchMomentUsers(
                query = trimmedQuery,
                limit = MAX_MOMENT_USER_SEARCH_RESULTS,
                defaultCountry = Locale.getDefault().country,
            )
            isSearchingMomentUsers = false
            result
                .onSuccess { matches ->
                    momentUserSearchResults = matches
                        .filterNot { match -> matrixClient.isMe(match.userId) }
                        .distinctBy { match -> match.userId }
                        .map { match -> match.toUserSearchResult() }
                        .toImmutableList()
                }
                .onFailure {
                    momentUserSearchResults = persistentListOf<UserSearchResult>()
                }
        }

        LaunchedEffect(Unit) {
            phonebookContacts = loadPhonebookContacts()
        }

        val enrichedUserListState = userListState.copy(
            searchResults = userListState.searchResults.withMomentUserResults(momentUserSearchResults),
            showSearchLoader = userListState.showSearchLoader || isSearchingMomentUsers,
        )

        fun handleEvent(event: StartChatEvents) {
            when (event) {
                is StartChatEvents.StartDM -> localCoroutineScope.launch {
                    startDMAction.execute(
                        matrixUser = event.matrixUser,
                        createIfDmDoesNotExist = startDmActionState.value is AsyncAction.Confirming,
                        actionState = startDmActionState,
                    )
                }
                StartChatEvents.CancelStartDM -> startDmActionState.value = AsyncAction.Uninitialized
                is StartChatEvents.CreateMomentRoom -> localCoroutineScope.createMomentRoom(
                    name = event.name,
                    momentRoomKind = event.momentRoomKind,
                    isPublic = event.isPublic,
                    createRoomActionState = createMomentRoomActionState,
                )
                StartChatEvents.CancelCreateMomentRoom -> createMomentRoomActionState.value = AsyncAction.Uninitialized
            }
        }

        return StartChatState(
            applicationName = buildMeta.applicationName,
            userListState = enrichedUserListState,
            phonebookContacts = phonebookContacts,
            startDmAction = startDmActionState.value,
            createMomentRoomAction = createMomentRoomActionState.value,
            eventSink = ::handleEvent,
        )
    }

    private fun CoroutineScope.createMomentRoom(
        name: String,
        momentRoomKind: MomentRoomKind,
        isPublic: Boolean,
        createRoomActionState: MutableState<AsyncAction<RoomId>>,
    ) = launch {
        suspend {
            val roomName = name.trim()
            require(roomName.isNotEmpty())
            val params = if (isPublic) {
                CreateRoomParameters(
                    name = roomName,
                    topic = null,
                    isEncrypted = false,
                    isDirect = false,
                    visibility = RoomVisibility.Public,
                    preset = RoomPreset.PUBLIC_CHAT,
                    momentRoomKind = momentRoomKind,
                )
            } else {
                CreateRoomParameters(
                    name = roomName,
                    topic = null,
                    isEncrypted = false,
                    isDirect = false,
                    visibility = RoomVisibility.Private,
                    historyVisibilityOverride = RoomHistoryVisibility.Invited,
                    preset = RoomPreset.PRIVATE_CHAT,
                    momentRoomKind = momentRoomKind,
                )
            }
            matrixClient.createRoom(params)
                .onFailure { failure ->
                    Timber.e(failure, "Failed to create Moment room")
                }
                .onSuccess {
                    analyticsService.capture(CreatedRoom(isDM = false))
                }
                .getOrThrow()
        }.runCatchingUpdatingState(createRoomActionState)
    }

    private suspend fun loadPhonebookContacts(): ImmutableList<UserSearchResult> {
        val deviceContacts = deviceContactsDataSource.getContacts().getOrElse {
            return persistentListOf()
        }
        val phoneEntries = deviceContacts
            .flatMap { contact ->
                contact.phoneNumbers.map { phoneNumber ->
                    PhonebookLookupEntry(
                        displayName = contact.displayName,
                        phoneNumber = phoneNumber,
                    )
                }
            }
            .distinctBy { it.phoneNumber.normalizedPhoneKey() }
            .take(MAX_PHONEBOOK_CONTACT_LOOKUPS)

        val results = phoneEntries.mapNotNull { entry ->
            matrixClient.searchMomentUsers(
                query = entry.phoneNumber,
                limit = 1,
                defaultCountry = Locale.getDefault().country,
            ).getOrNull()
                ?.firstOrNull { match -> !matrixClient.isMe(match.userId) }
                ?.let { match ->
                    UserSearchResult(
                        matrixUser = match.matrixUser.copy(
                            displayName = entry.displayName.takeIf { it.isNotBlank() } ?: match.displayName,
                        ),
                        subtitle = match.phoneNumber?.takeIf { it.isNotBlank() } ?: entry.phoneNumber,
                    )
                }
        }

        return results
            .distinctBy { it.matrixUser.userId }
            .toImmutableList()
    }

    private fun MatrixMomentUserSearchMatch.toUserSearchResult(): UserSearchResult {
        return UserSearchResult(
            matrixUser = matrixUser,
            subtitle = phoneNumber?.takeIf { it.isNotBlank() } ?: userId.value,
        )
    }

    private fun SearchBarResultState<*>.withMomentUserResults(
        momentUserSearchResults: List<UserSearchResult>,
    ): SearchBarResultState<ImmutableList<UserSearchResult>> {
        if (momentUserSearchResults.isEmpty()) {
            @Suppress("UNCHECKED_CAST")
            return this as SearchBarResultState<ImmutableList<UserSearchResult>>
        }

        val existingResults = when (this) {
            is SearchBarResultState.Results<*> -> results as? List<*> ?: emptyList<Any>()
            else -> emptyList()
        }.filterIsInstance<UserSearchResult>()

        val existingUserIds = existingResults.map { result -> result.matrixUser.userId }.toSet()
        return SearchBarResultState.Results(
            (existingResults + momentUserSearchResults.filterNot { result -> result.matrixUser.userId in existingUserIds }).toImmutableList()
        )
    }

    private data class PhonebookLookupEntry(
        val displayName: String,
        val phoneNumber: String,
    )
}

private fun String.normalizedPhoneKey(): String {
    return filter(Char::isDigit)
}
