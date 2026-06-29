/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.mediapickers.impl

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.FileProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.libraries.core.mimetype.MimeTypes
import io.element.android.libraries.di.annotations.ApplicationContext
import io.element.android.libraries.mediapickers.api.ComposePickerLauncher
import io.element.android.libraries.mediapickers.api.NoOpPickerLauncher
import io.element.android.libraries.mediapickers.api.PickerLauncher
import io.element.android.libraries.mediapickers.api.PickerProvider
import io.element.android.libraries.mediapickers.api.PickerType
import java.io.File

@ContributesBinding(AppScope::class)
class DefaultPickerProvider(
    @ApplicationContext private val context: Context,
) : PickerProvider {
    /**
     * Remembers and returns a [PickerLauncher] for a certain media/file [type].
     */
    @Composable
    internal fun <Input, Output> rememberPickerLauncher(
        type: PickerType<Input, Output>,
        onResult: (Output) -> Unit,
    ): PickerLauncher<Input, Output> {
        return if (LocalInspectionMode.current) {
            NoOpPickerLauncher { }
        } else {
            val contract = type.getContract()
            val managedLauncher = rememberLauncherForActivityResult(contract = contract, onResult = onResult)
            remember(type) { ComposePickerLauncher(managedLauncher, type.getDefaultRequest()) }
        }
    }

    /**
     * Remembers and returns a [PickerLauncher] for a gallery picture.
     * [onResult] will be called with either the selected file's [Uri] or `null` if nothing was selected.
     */
    @Composable
    override fun registerGalleryImagePicker(onResult: (Uri?) -> Unit): PickerLauncher<PickVisualMediaRequest, Uri?> {
        // Tests and UI preview can't handle Contexts, so we might as well disable the whole picker
        return if (LocalInspectionMode.current) {
            NoOpPickerLauncher { onResult(null) }
        } else {
            rememberPickerLauncher(type = PickerType.Image) { uri -> onResult(uri) }
        }
    }

    /**
     * Remembers and returns a [PickerLauncher] for a gallery video.
     * [onResult] will be called with either the selected file's [Uri] or `null` if nothing was selected.
     */
    @Composable
    override fun registerGalleryVideoPicker(onResult: (Uri?) -> Unit): PickerLauncher<PickVisualMediaRequest, Uri?> {
        // Tests and UI preview can't handle Contexts, so we might as well disable the whole picker
        return if (LocalInspectionMode.current) {
            NoOpPickerLauncher { onResult(null) }
        } else {
            rememberPickerLauncher(type = PickerType.Video) { uri -> onResult(uri) }
        }
    }

    /**
     * Remembers and returns a [PickerLauncher] for a gallery item, either a picture or a video.
     * [onResult] will be called with either the selected file's [Uri] or `null` if nothing was selected.
     */
    @Composable
    override fun registerGalleryPicker(
        onResult: (uri: Uri?, mimeType: String?) -> Unit
    ): PickerLauncher<PickVisualMediaRequest, Uri?> {
        // Tests and UI preview can't handle Contexts, so we might as well disable the whole picker
        return if (LocalInspectionMode.current) {
            NoOpPickerLauncher { onResult(null, null) }
        } else {
            rememberPickerLauncher(type = PickerType.ImageAndVideo) { uri ->
                val mimeType = uri?.let { context.contentResolver.getType(it) }
                onResult(uri, mimeType)
            }
        }
    }

    /**
     * Remembers and returns a [PickerLauncher] for a file of a certain [mimeType] (any type of file, by default).
     * [onResult] will be called with either the selected file's [Uri] or `null` if nothing was selected.
     */
    @Composable
    override fun registerFilePicker(
        mimeType: String,
        onResult: (uri: Uri?, mimeType: String?) -> Unit,
    ): PickerLauncher<String, Uri?> {
        // Tests and UI preview can't handle Context or FileProviders, so we might as well disable the whole picker
        return if (LocalInspectionMode.current) {
            NoOpPickerLauncher { onResult(null, null) }
        } else {
            rememberPickerLauncher(type = PickerType.File(mimeType)) { uri ->
                val pickedMimeType = uri?.let { context.contentResolver.getType(it) }
                onResult(uri, pickedMimeType)
            }
        }
    }

    @Composable
    override fun registerContactPicker(onResult: (Uri?) -> Unit): PickerLauncher<Void?, Uri?> {
        return if (LocalInspectionMode.current) {
            NoOpPickerLauncher { onResult(null) }
        } else {
            rememberPickerLauncher(type = PickerType.Contact) { uri -> onResult(uri) }
        }
    }

    @Composable
    override fun registerCameraPicker(onResult: (uri: Uri?, mimeType: String?) -> Unit): PickerLauncher<Intent, ActivityResult> {
        return if (LocalInspectionMode.current) {
            NoOpPickerLauncher { onResult(null, null) }
        } else {
            val currentRequest = remember { mutableStateOf<CameraMediaRequest?>(null) }
            val managedLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val request = currentRequest.value
                if (result.resultCode != Activity.RESULT_OK) {
                    onResult(null, null)
                    return@rememberLauncherForActivityResult
                }

                val returnedUri = result.data?.data
                when {
                    returnedUri != null -> onResult(returnedUri, context.contentResolver.getType(returnedUri))
                    request?.videoFile?.hasCaptureResult() == true -> onResult(request.videoUri, MimeTypes.Mp4)
                    request?.photoFile?.hasCaptureResult() == true -> onResult(request.photoUri, MimeTypes.Jpeg)
                    else -> onResult(null, null)
                }
            }
            remember {
                CameraMediaPickerLauncher(managedLauncher) {
                    createCameraMediaRequest().also { currentRequest.value = it }.intent
                }
            }
        }
    }

    /**
     * Remembers and returns a [PickerLauncher] for taking a photo with a camera app.
     * @param [onResult] will be called with either the photo's [Uri] or `null` if nothing was selected.
     */
    @Composable
    override fun registerCameraPhotoPicker(onResult: (Uri?) -> Unit): PickerLauncher<Uri, Boolean> {
        // Tests and UI preview can't handle Context or FileProviders, so we might as well disable the whole picker
        return if (LocalInspectionMode.current) {
            NoOpPickerLauncher { onResult(null) }
        } else {
            val tmpFile = remember { getTemporaryFile("photo.jpg") }
            val tmpFileUri = remember(tmpFile) { getTemporaryUri(tmpFile) }
            rememberPickerLauncher(type = PickerType.Camera.Photo(tmpFileUri)) { success ->
                // Execute callback
                onResult(if (success) tmpFileUri else null)
            }
        }
    }

    /**
     * Remembers and returns a [PickerLauncher] for recording a video with a camera app.
     * @param [onResult] will be called with either the video's [Uri] or `null` if nothing was selected.
     */
    @Composable
    override fun registerCameraVideoPicker(onResult: (Uri?) -> Unit): PickerLauncher<Uri, Boolean> {
        // Tests and UI preview can't handle Context or FileProviders, so we might as well disable the whole picker
        return if (LocalInspectionMode.current) {
            NoOpPickerLauncher { onResult(null) }
        } else {
            val tmpFile = remember { getTemporaryFile("video.mp4") }
            val tmpFileUri = remember(tmpFile) { getTemporaryUri(tmpFile) }
            rememberPickerLauncher(type = PickerType.Camera.Video(tmpFileUri)) { success ->
                // Execute callback
                onResult(if (success) tmpFileUri else null)
            }
        }
    }

    private fun getTemporaryFile(
        filename: String,
    ): File {
        return File(context.cacheDir, filename)
    }

    private fun getTemporaryUri(
        file: File,
    ): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    private fun createCameraMediaRequest(): CameraMediaRequest {
        val timestamp = System.currentTimeMillis()
        val photoFile = getTemporaryFile("photo-$timestamp.jpg")
        val videoFile = getTemporaryFile("video-$timestamp.mp4")
        val photoUri = getTemporaryUri(photoFile)
        val videoUri = getTemporaryUri(videoFile)
        val photoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            .withCameraWritePermission()
        val videoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, videoUri)
            .withCameraWritePermission()
        val chooserIntent = Intent.createChooser(photoIntent, null)
            .putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(videoIntent))
        return CameraMediaRequest(
            intent = chooserIntent,
            photoFile = photoFile,
            photoUri = photoUri,
            videoFile = videoFile,
            videoUri = videoUri,
        )
    }

    private fun Intent.withCameraWritePermission(): Intent {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        return this
    }
}

private class CameraMediaPickerLauncher(
    private val managedLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    private val requestFactory: () -> Intent,
) : PickerLauncher<Intent, ActivityResult> {
    override fun launch() = managedLauncher.launch(requestFactory())
    override fun launch(customInput: Intent) = managedLauncher.launch(customInput)
}

private data class CameraMediaRequest(
    val intent: Intent,
    val photoFile: File,
    val photoUri: Uri,
    val videoFile: File,
    val videoUri: Uri,
)

private fun File.hasCaptureResult(): Boolean {
    return exists() && length() > 0
}
