/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.logout.impl

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.libraries.architecture.AsyncAction

open class LogoutStateProvider : PreviewParameterProvider<LogoutState> {
    override val values: Sequence<LogoutState>
        get() = sequenceOf(
            aLogoutState(),
            aLogoutState(logoutAction = AsyncAction.ConfirmingNoParams),
            aLogoutState(logoutAction = AsyncAction.Loading),
            aLogoutState(logoutAction = AsyncAction.Failure(Exception("Failed to logout"))),
        )
}

fun aLogoutState(
    logoutAction: AsyncAction<Unit> = AsyncAction.Uninitialized,
    eventSink: (LogoutEvents) -> Unit = {},
) = LogoutState(
    logoutAction = logoutAction,
    eventSink = eventSink,
)
