/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.contacts

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.MatrixMomentUserSearchMatch
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.permissions.test.FakePermissionsPresenter
import io.element.android.libraries.permissions.test.FakePermissionsPresenterFactory
import io.element.android.tests.testutils.WarmUpRule
import io.element.android.tests.testutils.test
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class HomeContactsPresenterTest {
    @get:Rule
    val warmUpRule = WarmUpRule()

    @Test
    fun `present - loads device contacts when permission is granted`() = runTest {
        val permissionsPresenter = FakePermissionsPresenter().apply { setPermissionGranted() }
        val deviceContact = HomeDeviceContact(
            id = "1",
            displayName = "Alice Phonebook",
            phoneNumbers = listOf("+44 7700 900123"),
        )
        val contactsDataSource = FakeHomeDeviceContactsDataSource(Result.success(listOf(deviceContact)))
        val presenter = HomeContactsPresenter(
            permissionsPresenterFactory = FakePermissionsPresenterFactory(permissionsPresenter),
            matrixClient = FakeMatrixClient(),
            deviceContactsDataSource = contactsDataSource,
        )

        presenter.test {
            assertThat(awaitItem().contactsPermissionState.permissionGranted).isTrue()
            val loadedState = awaitItem()
            assertThat(loadedState.contacts.dataOrNull()?.unavailableContacts).containsExactly(deviceContact)
            assertThat(contactsDataSource.calls).isEqualTo(1)
        }
    }

    @Test
    fun `present - resolves phonebook contacts through Moment user search`() = runTest {
        val permissionsPresenter = FakePermissionsPresenter().apply { setPermissionGranted() }
        val matchedContact = HomeDeviceContact(
            id = "1",
            displayName = "Alice Phonebook",
            phoneNumbers = listOf("+44 7700 900123"),
        )
        val unavailableContact = HomeDeviceContact(
            id = "2",
            displayName = "Bob Phonebook",
            phoneNumbers = listOf("+44 7700 900456"),
        )
        val calls = mutableListOf<String>()
        val matrixClient = FakeMatrixClient(
            searchMomentUsersResult = { query, limit, _ ->
                calls += query
                assertThat(limit).isEqualTo(1)
                if (query == "+44 7700 900123") {
                    Result.success(
                        listOf(
                            MatrixMomentUserSearchMatch(
                                userId = UserId("@alice:example.org"),
                                displayName = "Alice Moment",
                                avatarUrl = null,
                                phoneNumber = "+44 7700 900123",
                            )
                        )
                    )
                } else {
                    Result.success(emptyList())
                }
            }
        )
        val contactsDataSource = FakeHomeDeviceContactsDataSource(Result.success(listOf(matchedContact, unavailableContact)))
        val presenter = HomeContactsPresenter(
            permissionsPresenterFactory = FakePermissionsPresenterFactory(permissionsPresenter),
            matrixClient = matrixClient,
            deviceContactsDataSource = contactsDataSource,
        )

        presenter.test {
            skipItems(1)
            val loadedState = awaitItem()
            val contacts = requireNotNull(loadedState.contacts.dataOrNull())
            assertThat(contacts.momentContacts).containsExactly(
                HomeMomentContact(
                    matrixUser = MatrixMomentUserSearchMatch(
                        userId = UserId("@alice:example.org"),
                        displayName = "Alice Moment",
                        avatarUrl = null,
                        phoneNumber = "+44 7700 900123",
                    ).matrixUser.copy(displayName = "Alice Phonebook"),
                    subtitle = "+44 7700 900123",
                )
            )
            assertThat(contacts.unavailableContacts).containsExactly(unavailableContact)
            assertThat(calls).containsExactly("+44 7700 900123", "+44 7700 900456").inOrder()
        }
    }

    @Test
    fun `present - does not read device contacts without permission`() = runTest {
        val permissionsPresenter = FakePermissionsPresenter().apply { setPermissionDenied() }
        val contactsDataSource = FakeHomeDeviceContactsDataSource(Result.success(emptyList()))
        val presenter = HomeContactsPresenter(
            permissionsPresenterFactory = FakePermissionsPresenterFactory(permissionsPresenter),
            matrixClient = FakeMatrixClient(),
            deviceContactsDataSource = contactsDataSource,
        )

        presenter.test {
            val initialState = awaitItem()
            assertThat(initialState.contactsPermissionState.permissionGranted).isFalse()
            assertThat(initialState.contacts).isEqualTo(AsyncData.Uninitialized)
            assertThat(contactsDataSource.calls).isEqualTo(0)
        }
    }
}

private class FakeHomeDeviceContactsDataSource(
    private val result: Result<List<HomeDeviceContact>>,
) : HomeDeviceContactsDataSource {
    var calls = 0
        private set

    override suspend fun getContacts(): Result<List<HomeDeviceContact>> {
        calls++
        return result
    }
}
