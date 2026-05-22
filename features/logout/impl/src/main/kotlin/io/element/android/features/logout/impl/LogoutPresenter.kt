/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.logout.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import dev.zacsweers.metro.Inject
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.architecture.runCatchingUpdatingState
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.workmanager.api.WorkManagerScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Inject
class LogoutPresenter(
    private val matrixClient: MatrixClient,
    private val workManagerScheduler: WorkManagerScheduler,
) : Presenter<LogoutState> {
    @Composable
    override fun present(): LogoutState {
        val localCoroutineScope = rememberCoroutineScope()
        val logoutAction: MutableState<AsyncAction<Unit>> = remember {
            mutableStateOf(AsyncAction.Uninitialized)
        }

        fun handleEvent(event: LogoutEvents) {
            when (event) {
                is LogoutEvents.Logout -> {
                    if (logoutAction.value.isConfirming() || event.ignoreSdkError) {
                        localCoroutineScope.logout(logoutAction, event.ignoreSdkError)
                    } else {
                        logoutAction.value = AsyncAction.ConfirmingNoParams
                    }
                }
                LogoutEvents.CloseDialogs -> {
                    logoutAction.value = AsyncAction.Uninitialized
                }
            }
        }

        return LogoutState(
            logoutAction = logoutAction.value,
            eventSink = ::handleEvent,
        )
    }

    private fun CoroutineScope.logout(
        logoutAction: MutableState<AsyncAction<Unit>>,
        ignoreSdkError: Boolean,
    ) = launch {
        suspend {
            // Cancel any pending work (e.g. notification sync)
            workManagerScheduler.cancel(matrixClient.sessionId)

            matrixClient.logout(userInitiated = true, ignoreSdkError)
        }.runCatchingUpdatingState(logoutAction)
    }
}
