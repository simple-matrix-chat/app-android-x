/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.user.editprofile

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.element.android.libraries.androidutils.file.TemporaryUriDeleter
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.architecture.runCatchingUpdatingState
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.core.mimetype.MimeTypes
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.user.MatrixProfileLink
import io.element.android.libraries.matrix.api.user.MatrixProfileUsername
import io.element.android.libraries.matrix.api.user.MatrixProfileUsernameException
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.ui.media.AvatarAction
import io.element.android.libraries.mediapickers.api.PickerProvider
import io.element.android.libraries.mediaupload.api.MediaOptimizationConfigProvider
import io.element.android.libraries.mediaupload.api.MediaPreProcessor
import io.element.android.libraries.permissions.api.PermissionsEvent
import io.element.android.libraries.permissions.api.PermissionsPresenter
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@AssistedInject
class EditUserProfilePresenter(
    @Assisted private val matrixUser: MatrixUser,
    @Assisted private val navigator: EditUserProfileNavigator,
    private val matrixClient: MatrixClient,
    private val mediaPickerProvider: PickerProvider,
    private val mediaPreProcessor: MediaPreProcessor,
    private val temporaryUriDeleter: TemporaryUriDeleter,
    private val mediaOptimizationConfigProvider: MediaOptimizationConfigProvider,
    permissionsPresenterFactory: PermissionsPresenter.Factory,
) : Presenter<EditUserProfileState> {
    private val cameraPermissionPresenter: PermissionsPresenter = permissionsPresenterFactory.create(android.Manifest.permission.CAMERA)
    private var pendingPermissionRequest = false

    @AssistedFactory
    interface Factory {
        fun create(
            matrixUser: MatrixUser,
            navigator: EditUserProfileNavigator,
        ): EditUserProfilePresenter
    }

    @Composable
    override fun present(): EditUserProfileState {
        val cameraPermissionState = cameraPermissionPresenter.present()
        var userAvatarUri by rememberSaveable { mutableStateOf(matrixUser.avatarUrl) }
        var userDisplayName by rememberSaveable { mutableStateOf(matrixUser.displayName) }
        var currentProfileUsername by rememberSaveable { mutableStateOf("") }
        var profileUsername by rememberSaveable { mutableStateOf("") }
        var phoneNumber by rememberSaveable { mutableStateOf("") }
        var profileLink by rememberSaveable { mutableStateOf<String?>(null) }
        var showProfileUsername by rememberSaveable { mutableStateOf(false) }
        var usernameError by rememberSaveable { mutableStateOf<EditUserProfileUsernameError?>(null) }
        var currentProfileStatus by rememberSaveable { mutableStateOf("") }
        var profileStatus by rememberSaveable { mutableStateOf("") }
        var isLoadingProfileStatus by remember { mutableStateOf(false) }
        var isLoadingProfileUsername by remember { mutableStateOf(false) }
        val cameraPhotoPicker = mediaPickerProvider.registerCameraPhotoPicker(
            onResult = { uri ->
                if (uri != null) {
                    temporaryUriDeleter.delete(userAvatarUri?.toUri())
                    userAvatarUri = uri.toString()
                }
            }
        )
        val galleryImagePicker = mediaPickerProvider.registerGalleryImagePicker(
            onResult = { uri ->
                if (uri != null) {
                    temporaryUriDeleter.delete(userAvatarUri?.toUri())
                    userAvatarUri = uri.toString()
                }
            }
        )

        val avatarActions by remember(userAvatarUri) {
            derivedStateOf {
                listOfNotNull(
                    AvatarAction.TakePhoto,
                    AvatarAction.ChoosePhoto,
                    AvatarAction.Remove.takeIf { userAvatarUri != null },
                ).toImmutableList()
            }
        }

        LaunchedEffect(cameraPermissionState.permissionGranted) {
            if (cameraPermissionState.permissionGranted && pendingPermissionRequest) {
                pendingPermissionRequest = false
                cameraPhotoPicker.launch()
            }
        }
        LaunchedEffect(Unit) {
            matrixClient.getProfileStatus()
                .onSuccess { status ->
                    currentProfileStatus = status
                    profileStatus = status
                }
                .onFailure { Timber.e(it, "Failed to load profile status") }
            isLoadingProfileStatus = false

            matrixClient.getProfileUsername(matrixUser.userId)
                .onSuccess { username ->
                    currentProfileUsername = username
                    profileUsername = username
                    usernameError = null
                    showProfileUsername = true
                }
                .onFailure { failure ->
                    if (failure is MatrixProfileUsernameException.Unsupported) {
                        showProfileUsername = false
                    } else {
                        Timber.e(failure, "Failed to load profile username")
                        showProfileUsername = true
                    }
                    currentProfileUsername = ""
                    profileUsername = ""
                }
            isLoadingProfileUsername = false

            matrixClient.getPublicProfile(matrixUser.userId)
                .onSuccess { publicProfile ->
                    phoneNumber = publicProfile?.phoneNumber.orEmpty()
                }
                .onFailure { Timber.e(it, "Failed to load public profile identity") }

            matrixClient.createUserProfileLink(matrixUser.userId)
                .onSuccess { link ->
                    profileLink = link
                }
                .onFailure { Timber.e(it, "Failed to load public profile link") }
        }

        val homeserverCapabilities = matrixClient.homeserverCapabilities()
        val canChangeDisplayName = produceState(true) {
            value = homeserverCapabilities.canChangeDisplayName().getOrDefault(true)
        }
        val canChangeAvatar = produceState(true) {
            value = homeserverCapabilities.canChangeAvatarUrl().getOrDefault(true)
        }

        val saveAction: MutableState<AsyncAction<Unit>> = remember { mutableStateOf(AsyncAction.Uninitialized) }
        val localCoroutineScope = rememberCoroutineScope()

        val canSave = remember(
            userDisplayName,
            userAvatarUri,
            profileUsername,
            currentProfileUsername,
            usernameError,
            showProfileUsername,
            profileStatus,
            currentProfileStatus,
        ) {
            val hasProfileChanged = hasDisplayNameChanged(userDisplayName, matrixUser) ||
                hasAvatarUrlChanged(userAvatarUri, matrixUser)
            val hasUsernameChanged = showProfileUsername && usernameError == null && hasUsernameChanged(profileUsername, currentProfileUsername)
            val hasStatusChanged = hasStatusChanged(profileStatus, currentProfileStatus)
            !userDisplayName.isNullOrBlank() && (hasProfileChanged || hasUsernameChanged || hasStatusChanged) && usernameError == null
        }

        fun handleEvent(event: EditUserProfileEvent) {
            when (event) {
                is EditUserProfileEvent.Save -> localCoroutineScope.saveChanges(
                    name = userDisplayName,
                    avatarUri = userAvatarUri?.toUri(),
                    currentUser = matrixUser,
                    username = profileUsername,
                    currentUsername = currentProfileUsername,
                    updateUsername = showProfileUsername,
                    status = profileStatus,
                    currentStatus = currentProfileStatus,
                    action = saveAction,
                    onUsernameError = { usernameError = it },
                )
                is EditUserProfileEvent.HandleAvatarAction -> {
                    when (event.action) {
                        AvatarAction.ChoosePhoto -> galleryImagePicker.launch()
                        AvatarAction.TakePhoto -> if (cameraPermissionState.permissionGranted) {
                            cameraPhotoPicker.launch()
                        } else {
                            pendingPermissionRequest = true
                            cameraPermissionState.eventSink(PermissionsEvent.RequestPermissions)
                        }
                        AvatarAction.Remove -> {
                            temporaryUriDeleter.delete(userAvatarUri?.toUri())
                            userAvatarUri = null
                        }
                    }
                }
                is EditUserProfileEvent.UpdateDisplayName -> userDisplayName = event.name
                is EditUserProfileEvent.UpdateUsername -> {
                    profileUsername = MatrixProfileUsername.normalize(event.username)
                    usernameError = usernameValidationError(profileUsername, currentProfileUsername)
                }
                is EditUserProfileEvent.UpdateStatus -> profileStatus = event.status
                is EditUserProfileEvent.SelectStatusPreset -> profileStatus = event.status
                EditUserProfileEvent.Exit -> {
                    when (saveAction.value) {
                        is AsyncAction.Confirming -> {
                            // Close the dialog right now
                            saveAction.value = AsyncAction.Uninitialized
                            navigator.close()
                        }
                        AsyncAction.Loading -> Unit
                        is AsyncAction.Failure,
                        is AsyncAction.Success -> {
                            // Should not happen
                        }
                        AsyncAction.Uninitialized -> {
                            if (canSave) {
                                saveAction.value = AsyncAction.ConfirmingCancellation
                            } else {
                                navigator.close()
                            }
                        }
                    }
                }
                EditUserProfileEvent.CloseDialog -> saveAction.value = AsyncAction.Uninitialized
            }
        }

        return EditUserProfileState(
            userId = matrixUser.userId,
            displayName = userDisplayName.orEmpty(),
            username = profileUsername,
            phoneNumber = phoneNumber,
            status = profileStatus,
            profileShareText = buildProfileShareText(
                profileLink = profileLink,
                displayName = userDisplayName,
                currentUser = matrixUser,
            ),
            userAvatarUrl = userAvatarUri,
            avatarActions = avatarActions,
            saveButtonEnabled = canSave && saveAction.value !is AsyncAction.Loading,
            saveAction = saveAction.value,
            isLoadingProfileStatus = isLoadingProfileStatus,
            isLoadingProfileUsername = isLoadingProfileUsername,
            isLoadingMomentProfile = false,
            showProfileUsername = showProfileUsername,
            usernameError = usernameError,
            cameraPermissionState = cameraPermissionState,
            canChangeDisplayName = canChangeDisplayName.value,
            canChangeAvatarUrl = canChangeAvatar.value,
            eventSink = ::handleEvent,
        )
    }

    private fun hasDisplayNameChanged(name: String?, currentUser: MatrixUser) =
        name?.trim() != currentUser.displayName?.trim()

    private fun hasAvatarUrlChanged(avatarUri: String?, currentUser: MatrixUser) =
        avatarUri?.trim() != currentUser.avatarUrl?.trim()

    private fun hasUsernameChanged(username: String, currentUsername: String) =
        MatrixProfileUsername.normalize(username) != MatrixProfileUsername.normalize(currentUsername)

    private fun hasStatusChanged(status: String, currentStatus: String) =
        status.trim() != currentStatus.trim()

    private fun buildProfileShareText(
        profileLink: String?,
        displayName: String?,
        currentUser: MatrixUser,
    ): String? {
        val link = profileLink?.trim()?.takeIf { it.isNotEmpty() }
            ?: MatrixProfileLink.fallbackUserLink(currentUser.userId)
            ?: return null
        val shareLabel = displayName?.trim()?.takeIf { it.isNotEmpty() } ?: currentUser.userId.value
        return "$shareLabel\n$link"
    }

    private fun usernameValidationError(
        username: String,
        currentUsername: String,
    ): EditUserProfileUsernameError? {
        if (username.isBlank() && currentUsername.isBlank()) {
            return null
        }
        return MatrixProfileUsername.validationError(username)?.toEditUserProfileUsernameError()
    }

    private fun CoroutineScope.saveChanges(
        name: String?,
        avatarUri: Uri?,
        currentUser: MatrixUser,
        username: String,
        currentUsername: String,
        updateUsername: Boolean,
        status: String,
        currentStatus: String,
        action: MutableState<AsyncAction<Unit>>,
        onUsernameError: (EditUserProfileUsernameError) -> Unit,
    ) = launch {
        val results = mutableListOf<Result<Unit>>()
        suspend {
            if (!name.isNullOrEmpty() && name.trim() != currentUser.displayName.orEmpty().trim()) {
                results.add(matrixClient.setDisplayName(name).onFailure {
                    Timber.e(it, "Failed to set user's display name")
                })
            }
            if (avatarUri?.toString()?.trim() != currentUser.avatarUrl?.trim()) {
                results.add(updateAvatar(avatarUri).onFailure {
                    Timber.e(it, "Failed to update user's avatar")
                })
            }
            if (updateUsername && hasUsernameChanged(username, currentUsername)) {
                val displayName = name?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: currentUser.displayName?.trim()
                    ?: currentUser.userId.value
                results.add(
                    matrixClient.setProfileUsername(username, displayName)
                        .map { Unit }
                        .onFailure { failure ->
                            onUsernameError(failure.toEditUserProfileUsernameError())
                            Timber.e(failure, "Failed to update user's profile username")
                        }
                )
            }
            if (hasStatusChanged(status, currentStatus)) {
                results.add(matrixClient.setProfileStatus(status).onFailure {
                    Timber.e(it, "Failed to update user's profile status")
                })
            }
            if (results.all { it.isSuccess }) Unit else results.first { it.isFailure }.getOrThrow()
        }.runCatchingUpdatingState(action)
    }

    private suspend fun updateAvatar(avatarUri: Uri?): Result<Unit> {
        return runCatchingExceptions {
            if (avatarUri != null) {
                val preprocessed = mediaPreProcessor.process(
                    uri = avatarUri,
                    mimeType = MimeTypes.Jpeg,
                    deleteOriginal = false,
                    mediaOptimizationConfig = mediaOptimizationConfigProvider.get(),
                ).getOrThrow()
                matrixClient.uploadAvatar(MimeTypes.Jpeg, preprocessed.file.readBytes()).getOrThrow()
            } else {
                matrixClient.removeAvatar().getOrThrow()
            }
        }.onFailure { Timber.e(it, "Unable to update avatar") }
    }

    private fun Throwable.toEditUserProfileUsernameError(): EditUserProfileUsernameError {
        return when (this) {
            MatrixProfileUsernameException.Required -> EditUserProfileUsernameError.Required
            MatrixProfileUsernameException.TooShort -> EditUserProfileUsernameError.TooShort
            MatrixProfileUsernameException.TooLong -> EditUserProfileUsernameError.TooLong
            MatrixProfileUsernameException.Invalid -> EditUserProfileUsernameError.Invalid
            MatrixProfileUsernameException.Taken -> EditUserProfileUsernameError.Taken
            MatrixProfileUsernameException.Unsupported -> EditUserProfileUsernameError.Unsupported
            else -> EditUserProfileUsernameError.SaveFailed
        }
    }
}
