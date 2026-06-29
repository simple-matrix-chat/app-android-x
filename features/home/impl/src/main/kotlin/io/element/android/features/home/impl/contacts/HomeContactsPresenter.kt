/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.contacts

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Inject
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.user.MatrixMomentUserSearchMatch
import io.element.android.libraries.permissions.api.PermissionsEvent
import io.element.android.libraries.permissions.api.PermissionsPresenter
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import java.util.Locale

private const val MAX_PHONEBOOK_CONTACT_LOOKUPS = 25

@Inject
class HomeContactsPresenter(
    permissionsPresenterFactory: PermissionsPresenter.Factory,
    private val matrixClient: MatrixClient,
    private val deviceContactsDataSource: HomeDeviceContactsDataSource,
) : Presenter<HomeContactsState> {
    private val contactsPermissionPresenter = permissionsPresenterFactory.create(Manifest.permission.READ_CONTACTS)

    @Composable
    override fun present(): HomeContactsState {
        val contactsPermissionState = contactsPermissionPresenter.present()
        val coroutineScope = rememberCoroutineScope()
        val contactsState = remember { mutableStateOf<AsyncData<HomeContactsData>>(AsyncData.Uninitialized) }
        var unavailableContactDialog by remember { mutableStateOf<HomeDeviceContact?>(null) }

        suspend fun syncContacts() {
            val previousContacts = contactsState.value.dataOrNull()
            contactsState.value = AsyncData.Loading(previousContacts)
            deviceContactsDataSource.getContacts().fold(
                onSuccess = { contacts ->
                    contactsState.value = AsyncData.Success(contacts.resolveMomentContacts())
                },
                onFailure = { throwable ->
                    contactsState.value = AsyncData.Failure(
                        error = throwable,
                        prevData = previousContacts,
                    )
                }
            )
        }

        LaunchedEffect(contactsPermissionState.permissionGranted) {
            if (contactsPermissionState.permissionGranted) {
                syncContacts()
            } else {
                contactsState.value = AsyncData.Uninitialized
            }
        }

        fun handleEvents(event: HomeContactsEvent) {
            when (event) {
                HomeContactsEvent.RequestContactsPermission -> {
                    contactsPermissionState.eventSink(PermissionsEvent.RequestPermissions)
                }
                HomeContactsEvent.RetryContactsSync -> {
                    coroutineScope.launch {
                        syncContacts()
                    }
                }
                is HomeContactsEvent.SelectUnavailableContact -> {
                    unavailableContactDialog = event.contact
                }
                HomeContactsEvent.DismissUnavailableContactDialog -> {
                    unavailableContactDialog = null
                }
            }
        }

        return HomeContactsState(
            contactsPermissionState = contactsPermissionState,
            contacts = contactsState.value,
            unavailableContactDialog = unavailableContactDialog,
            eventSink = ::handleEvents,
        )
    }

    private suspend fun List<HomeDeviceContact>.resolveMomentContacts(): HomeContactsData {
        val phoneEntries = flatMap { contact ->
            contact.phoneNumbers.map { phoneNumber ->
                HomePhonebookLookupEntry(
                    contact = contact,
                    phoneNumber = phoneNumber,
                )
            }
        }.distinctBy { entry ->
            entry.phoneNumber.normalizedPhoneKey()
        }.take(MAX_PHONEBOOK_CONTACT_LOOKUPS)

        val momentContacts = mutableListOf<HomeMomentContact>()
        val matchedPhoneKeys = mutableSetOf<String>()
        val seenUserIds = mutableSetOf<String>()

        phoneEntries.forEach { entry ->
            val match = matrixClient.searchMomentUsers(
                query = entry.phoneNumber,
                limit = 1,
                defaultCountry = Locale.getDefault().country,
            ).getOrNull()
                ?.firstOrNull { match ->
                    !matrixClient.isMe(match.userId) && seenUserIds.add(match.userId.value)
                }

            if (match != null) {
                matchedPhoneKeys += entry.phoneNumber.normalizedPhoneKey()
                momentContacts += match.toHomeMomentContact(entry)
            }
        }

        val unavailableContacts = filterNot { contact ->
            contact.phoneNumbers.any { phoneNumber -> phoneNumber.normalizedPhoneKey() in matchedPhoneKeys }
        }

        return HomeContactsData(
            momentContacts = momentContacts.toImmutableList(),
            unavailableContacts = unavailableContacts.toImmutableList(),
        )
    }

    private fun MatrixMomentUserSearchMatch.toHomeMomentContact(
        lookupEntry: HomePhonebookLookupEntry,
    ): HomeMomentContact {
        return HomeMomentContact(
            matrixUser = matrixUser.copy(
                displayName = lookupEntry.contact.displayName.takeIf { it.isNotBlank() } ?: displayName,
            ),
            subtitle = phoneNumber?.takeIf { it.isNotBlank() } ?: lookupEntry.phoneNumber,
        )
    }

    private data class HomePhonebookLookupEntry(
        val contact: HomeDeviceContact,
        val phoneNumber: String,
    )
}

private fun String.normalizedPhoneKey(): String {
    return filter(Char::isDigit)
}
