/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.di.annotations.ApplicationContext
import kotlinx.coroutines.withContext

data class HomeDeviceContact(
    val id: String,
    val displayName: String,
    val phoneNumbers: List<String>,
) {
    val primaryPhoneNumber: String = phoneNumbers.firstOrNull().orEmpty()
}

interface HomeDeviceContactsDataSource {
    suspend fun getContacts(): Result<List<HomeDeviceContact>>
}

@ContributesBinding(SessionScope::class)
@Inject
class DefaultHomeDeviceContactsDataSource(
    @ApplicationContext private val context: Context,
    private val dispatchers: CoroutineDispatchers,
) : HomeDeviceContactsDataSource {
    override suspend fun getContacts(): Result<List<HomeDeviceContact>> = withContext(dispatchers.io) {
        runCatching {
            if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                return@runCatching emptyList()
            }

            val contacts = linkedMapOf<Long, MutableHomeDeviceContact>()
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            )
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            )?.use { cursor ->
                val contactIdIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val displayNameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val phoneNumberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(contactIdIndex)
                    val displayName = cursor.getString(displayNameIndex).orEmpty().trim()
                    val phoneNumber = cursor.getString(phoneNumberIndex).orEmpty().trim()
                    if (displayName.isNotBlank() && phoneNumber.isNotBlank()) {
                        contacts.getOrPut(contactId) { MutableHomeDeviceContact(displayName) }
                            .addPhoneNumber(phoneNumber)
                    }
                }
            }

            contacts.map { (contactId, contact) ->
                contact.toHomeDeviceContact(contactId)
            }.filter { it.phoneNumbers.isNotEmpty() }
        }
    }

    private data class MutableHomeDeviceContact(
        val displayName: String,
        val phoneNumbers: MutableList<String> = mutableListOf(),
    ) {
        fun addPhoneNumber(phoneNumber: String) {
            if (phoneNumbers.none { it.normalizedPhoneKey() == phoneNumber.normalizedPhoneKey() }) {
                phoneNumbers += phoneNumber
            }
        }

        fun toHomeDeviceContact(contactId: Long): HomeDeviceContact {
            return HomeDeviceContact(
                id = contactId.toString(),
                displayName = displayName,
                phoneNumbers = phoneNumbers,
            )
        }
    }
}

private fun String.normalizedPhoneKey(): String {
    return filter(Char::isDigit)
}
