/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.user.username

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.user.MatrixProfileUsername
import io.element.android.libraries.matrix.api.user.MatrixProfileUsernameException
import io.element.android.libraries.matrix.api.user.MatrixUser
import kotlinx.coroutines.launch
import timber.log.Timber

@AssistedInject
class MomentUsernamePresenter(
    @Assisted private val matrixUser: MatrixUser,
    @Assisted private val navigator: MomentUsernameNavigator,
    private val matrixClient: MatrixClient,
) : Presenter<MomentUsernameState> {
    @AssistedFactory
    interface Factory {
        fun create(
            matrixUser: MatrixUser,
            navigator: MomentUsernameNavigator,
        ): MomentUsernamePresenter
    }

    @Composable
    override fun present(): MomentUsernameState {
        var currentUsername by rememberSaveable { mutableStateOf("") }
        var username by rememberSaveable { mutableStateOf("") }
        var usernameError by rememberSaveable { mutableStateOf<MomentUsernameError?>(null) }
        var isSaving by remember { mutableStateOf(false) }
        val localCoroutineScope = rememberCoroutineScope()

        fun updateUsername(value: String) {
            username = MatrixProfileUsername.normalize(value)
            usernameError = validationError(username, currentUsername)
        }

        LaunchedEffect(Unit) {
            matrixClient.getProfileUsername(matrixUser.userId)
                .onSuccess {
                    currentUsername = it
                    username = it
                    usernameError = validationError(username, currentUsername)
                }
                .onFailure { failure ->
                    if (failure is MatrixProfileUsernameException.Unsupported) {
                        usernameError = MomentUsernameError.Unsupported
                    } else {
                        Timber.e(failure, "Failed to load profile username")
                    }
                }
        }

        val canSave = !isSaving &&
            username.isNotBlank() &&
            username != currentUsername &&
            usernameError == null

        fun saveUsername() {
            if (!canSave) return
            localCoroutineScope.launch {
                isSaving = true
                matrixClient.setProfileUsername(username, displayNameForSave())
                    .onSuccess {
                        currentUsername = it
                        username = it
                        usernameError = validationError(username, currentUsername)
                        navigator.close()
                    }
                    .onFailure { failure ->
                        usernameError = failure.toMomentUsernameError()
                        Timber.e(failure, "Failed to update user's profile username")
                    }
                isSaving = false
            }
        }

        fun handleEvents(event: MomentUsernameEvent) {
            when (event) {
                MomentUsernameEvent.Close -> navigator.close()
                MomentUsernameEvent.Save -> saveUsername()
                is MomentUsernameEvent.UpdateUsername -> updateUsername(event.username)
            }
        }

        return MomentUsernameState(
            userId = matrixUser.userId,
            displayName = matrixUser.displayName.orEmpty(),
            username = username,
            currentUsername = currentUsername,
            isSaving = isSaving,
            canSave = canSave,
            usernameError = usernameError,
            eventSink = ::handleEvents,
        )
    }

    private fun displayNameForSave(): String {
        return matrixUser.displayName
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: matrixUser.userId.value
    }

    private fun validationError(
        username: String,
        currentUsername: String,
    ): MomentUsernameError? {
        if (username.isBlank() && currentUsername.isBlank()) {
            return null
        }
        return MatrixProfileUsername.validationError(username)?.toMomentUsernameError()
    }

    private fun Throwable.toMomentUsernameError(): MomentUsernameError {
        return when (this) {
            MatrixProfileUsernameException.Required -> MomentUsernameError.Required
            MatrixProfileUsernameException.TooShort -> MomentUsernameError.TooShort
            MatrixProfileUsernameException.TooLong -> MomentUsernameError.TooLong
            MatrixProfileUsernameException.Invalid -> MomentUsernameError.Invalid
            MatrixProfileUsernameException.Taken -> MomentUsernameError.Taken
            MatrixProfileUsernameException.Unsupported -> MomentUsernameError.Unsupported
            else -> MomentUsernameError.SaveFailed
        }
    }
}
