/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.contacts

import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.permissions.api.PermissionsState
import kotlinx.collections.immutable.ImmutableList

data class HomeContactsState(
    val contactsPermissionState: PermissionsState,
    val contacts: AsyncData<HomeContactsData>,
    val unavailableContactDialog: HomeDeviceContact?,
    val eventSink: (HomeContactsEvent) -> Unit,
)

data class HomeContactsData(
    val momentContacts: ImmutableList<HomeMomentContact>,
    val unavailableContacts: ImmutableList<HomeDeviceContact>,
)

data class HomeMomentContact(
    val matrixUser: MatrixUser,
    val subtitle: String?,
)
