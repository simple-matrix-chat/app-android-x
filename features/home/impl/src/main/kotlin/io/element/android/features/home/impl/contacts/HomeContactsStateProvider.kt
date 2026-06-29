/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.contacts

import android.Manifest
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.permissions.api.aPermissionsState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

open class HomeContactsStateProvider : PreviewParameterProvider<HomeContactsState> {
    override val values: Sequence<HomeContactsState>
        get() = sequenceOf(
            aHomeContactsState(),
            aHomeContactsState(contactsPermissionGranted = false),
            aHomeContactsState(contacts = AsyncData.Loading()),
            aHomeContactsState(contacts = AsyncData.Failure(RuntimeException("Unable to sync contacts"))),
        )
}

internal fun aHomeContactsState(
    contactsPermissionGranted: Boolean = true,
    contacts: AsyncData<HomeContactsData> = AsyncData.Success(
        aHomeContactsData(
            momentContacts = persistentListOf(
                HomeMomentContact(
                    matrixUser = MatrixUser(
                        userId = UserId("@alice:example.org"),
                        displayName = "Alice Phonebook",
                    ),
                    subtitle = "+44 7700 900123",
                ),
            ),
            unavailableContacts = persistentListOf(
                HomeDeviceContact(
                    id = "2",
                    displayName = "Bob Phonebook",
                    phoneNumbers = listOf("+44 7700 900456"),
                ),
            ),
        )
    ),
    unavailableContactDialog: HomeDeviceContact? = null,
    eventSink: (HomeContactsEvent) -> Unit = {},
) = HomeContactsState(
    contactsPermissionState = aPermissionsState(
        showDialog = false,
        permission = Manifest.permission.READ_CONTACTS,
        permissionGranted = contactsPermissionGranted,
    ),
    contacts = contacts,
    unavailableContactDialog = unavailableContactDialog,
    eventSink = eventSink,
)

internal fun aHomeContactsData(
    momentContacts: ImmutableList<HomeMomentContact> = persistentListOf(),
    unavailableContacts: ImmutableList<HomeDeviceContact> = persistentListOf(),
) = HomeContactsData(
    momentContacts = momentContacts,
    unavailableContacts = unavailableContacts,
)
