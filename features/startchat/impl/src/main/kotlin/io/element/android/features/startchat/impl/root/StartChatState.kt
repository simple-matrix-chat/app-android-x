/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.startchat.impl.root

import io.element.android.features.startchat.impl.userlist.UserListState
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.usersearch.api.UserSearchResult
import kotlinx.collections.immutable.ImmutableList

data class StartChatState(
    val applicationName: String,
    val userListState: UserListState,
    val phonebookContacts: ImmutableList<UserSearchResult>,
    val startDmAction: AsyncAction<RoomId>,
    val createMomentRoomAction: AsyncAction<RoomId>,
    val eventSink: (StartChatEvents) -> Unit,
)
