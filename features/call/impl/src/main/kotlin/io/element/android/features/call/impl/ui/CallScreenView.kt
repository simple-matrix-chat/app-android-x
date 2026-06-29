/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.call.impl.R
import io.element.android.features.call.impl.pip.PictureInPictureEvent
import io.element.android.features.call.impl.pip.PictureInPictureState
import io.element.android.features.call.impl.pip.aPictureInPictureState
import io.element.android.features.call.impl.utils.InvalidAudioDeviceReason
import io.element.android.features.call.impl.utils.WebViewAudioManager
import io.element.android.features.call.impl.utils.WebViewPipController
import io.element.android.features.call.impl.utils.WebViewWidgetMessageInterceptor
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.components.dialogs.ErrorDialog
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.ui.strings.CommonStrings
import timber.log.Timber

typealias RequestPermissionCallback = (Array<String>) -> Unit

interface CallScreenNavigator {
    fun close()
}

@Composable
internal fun CallScreenView(
    state: CallScreenState,
    pipState: PictureInPictureState,
    onConsoleMessage: (ConsoleMessage) -> Unit,
    requestPermissions: (Array<String>, RequestPermissionCallback) -> Unit,
    modifier: Modifier = Modifier,
) {
    var callWebView by remember { mutableStateOf<WebView?>(null) }

    fun handleBack(fromNative: Boolean = false) {
        when (CallScreenBackPressPolicy.resolve(supportPip = pipState.supportPip, hasWebView = callWebView != null, fromNative)) {
            CallScreenBackPressAction.EnterPictureInPicture ->
                pipState.eventSink(PictureInPictureEvent.EnterPictureInPicture)
            CallScreenBackPressAction.DispatchEscapeToWebView ->
                callWebView?.dispatchEscKeyEvent()
            null -> Timber.d("Back press with unsupported pip is a no-op")
        }
    }

    BackHandler {
        handleBack(fromNative = true)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ElementTheme.colors.bgCanvasDefault),
    ) {
        if (state.webViewError != null) {
            MomentCallErrorView(
                message = buildString {
                    append(stringResource(CommonStrings.error_unknown))
                    state.webViewError.takeIf { it.isNotEmpty() }?.let { append("\n\n").append(it) }
                },
                onClose = { state.eventSink(CallScreenEvent.Hangup) },
            )
            return@Box
        }

        var webViewAudioManager by remember { mutableStateOf<WebViewAudioManager?>(null) }
        val coroutineScope = rememberCoroutineScope()

        var invalidAudioDeviceReason by remember { mutableStateOf<InvalidAudioDeviceReason?>(null) }
        invalidAudioDeviceReason?.let {
            InvalidAudioDeviceDialog(invalidAudioDeviceReason = it) {
                invalidAudioDeviceReason = null
            }
        }

        when (state.urlState) {
            AsyncData.Uninitialized,
            is AsyncData.Loading ->
                MomentCallLoadingView()
            is AsyncData.Failure -> {
                Timber.e(state.urlState.error, "WebView failed to load call URL")
                MomentCallErrorView(
                    message = state.urlState.error.message.takeUnless { it.isNullOrBlank() } ?: stringResource(CommonStrings.error_unknown),
                    onClose = { state.eventSink(CallScreenEvent.Hangup) },
                )
            }
            is AsyncData.Success -> {
                CallWebView(
                    modifier = Modifier
                        .consumeWindowInsets(WindowInsets.systemBars)
                        .fillMaxSize(),
                    url = state.urlState,
                    userAgent = state.userAgent,
                    onPermissionsRequest = { request ->
                        val androidPermissions = mapWebkitPermissions(request.resources)
                        val callback: RequestPermissionCallback = { request.grant(it) }
                        requestPermissions(androidPermissions.toTypedArray(), callback)
                    },
                    onConsoleMessage = onConsoleMessage,
                    onCreateWebView = { webView ->
                        callWebView = webView
                        webView.addBackHandler(onBackPressed = ::handleBack)
                        val interceptor = WebViewWidgetMessageInterceptor(
                            webView = webView,
                            onUrlLoaded = {
                                webView.evaluateJavascript("controls.onBackButtonPressed = () => { backHandler.onBackPressed() }", null)
                                if (webViewAudioManager?.isInCallMode?.get() == false) {
                                    Timber.d("Call URL is loaded, starting in-call audio mode")
                                    webViewAudioManager?.onCallStarted()
                                } else {
                                    Timber.d("Can't start in-call audio mode since the app is already in it.")
                                }
                            },
                            onError = { state.eventSink(CallScreenEvent.OnWebViewError(it)) },
                        )
                        webViewAudioManager = WebViewAudioManager(
                            webView = webView,
                            coroutineScope = coroutineScope,
                            onInvalidAudioDeviceAdded = { invalidAudioDeviceReason = it },
                        )
                        state.eventSink(CallScreenEvent.SetupMessageChannels(interceptor))
                        val pipController = WebViewPipController(webView)
                        pipState.eventSink(PictureInPictureEvent.SetPipController(pipController))
                    },
                    onDestroyWebView = {
                        callWebView = null
                        // Reset audio mode
                        webViewAudioManager?.onCallStopped()
                    }
                )
            }
        }
    }
}

@Composable
private fun MomentCallLoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = ElementTheme.colors.iconPrimary,
        )
    }
}

@Composable
private fun MomentCallErrorView(
    message: String,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            imageVector = CompoundIcons.ErrorSolid(),
            tint = ElementTheme.colors.iconCriticalPrimary,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(top = 20.dp),
            text = stringResource(CommonStrings.common_error),
            style = ElementTheme.typography.fontHeadingMdBold,
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
            text = message,
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Button(
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(),
            text = stringResource(CommonStrings.action_close),
            onClick = onClose,
        )
    }
}

@Composable
private fun InvalidAudioDeviceDialog(
    invalidAudioDeviceReason: InvalidAudioDeviceReason,
    onDismiss: () -> Unit,
) {
    ErrorDialog(
        content = when (invalidAudioDeviceReason) {
            InvalidAudioDeviceReason.BT_AUDIO_DEVICE_DISABLED -> {
                stringResource(R.string.call_invalid_audio_device_bluetooth_devices_disabled)
            }
        },
        onSubmit = onDismiss,
    )
}

@Composable
private fun CallWebView(
    url: AsyncData<String>,
    userAgent: String,
    onPermissionsRequest: (PermissionRequest) -> Unit,
    onConsoleMessage: (ConsoleMessage) -> Unit,
    onCreateWebView: (WebView) -> Unit,
    onDestroyWebView: (WebView) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("WebView - can't be previewed")
        }
    } else {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                WebView(context).apply {
                    onCreateWebView(this)
                    setup(
                        userAgent = userAgent,
                        onPermissionsRequested = onPermissionsRequest,
                        onConsoleMessage = onConsoleMessage,
                    )
                }
            },
            update = { webView ->
                if (url is AsyncData.Success && webView.url != url.data) {
                    webView.loadUrl(url.data)
                }
            },
            onRelease = { webView ->
                onDestroyWebView(webView)
                webView.destroy()
            }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.setup(
    userAgent: String,
    onPermissionsRequested: (PermissionRequest) -> Unit,
    onConsoleMessage: (ConsoleMessage) -> Unit,
) {
    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )

    with(settings) {
        javaScriptEnabled = true
        allowContentAccess = true
        allowFileAccess = true
        domStorageEnabled = true
        mediaPlaybackRequiresUserGesture = false
        @Suppress("DEPRECATION")
        databaseEnabled = true
        loadsImagesAutomatically = true
        userAgentString = userAgent
    }

    webChromeClient = object : WebChromeClient() {
        override fun onPermissionRequest(request: PermissionRequest) {
            onPermissionsRequested(request)
        }

        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            onConsoleMessage(consoleMessage)
            return true
        }
    }
}

private fun WebView.addBackHandler(onBackPressed: () -> Unit) {
    addJavascriptInterface(
        JavascriptBackHandlerBridge(callback = onBackPressed),
        "backHandler"
    )
}

private fun WebView.dispatchEscKeyEvent() {
    dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ESCAPE))
    dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ESCAPE))
}

@PreviewsDayNight
@Composable
internal fun CallScreenViewPreview(
    @PreviewParameter(CallScreenStateProvider::class) state: CallScreenState,
) = ElementPreview {
    CallScreenView(
        state = state,
        pipState = aPictureInPictureState(),
        requestPermissions = { _, _ -> },
        onConsoleMessage = {},
    )
}

@PreviewsDayNight
@Composable
internal fun InvalidAudioDeviceDialogPreview() = ElementPreview {
    InvalidAudioDeviceDialog(invalidAudioDeviceReason = InvalidAudioDeviceReason.BT_AUDIO_DEVICE_DISABLED) {}
}

internal class JavascriptBackHandlerBridge(
    private val callback: () -> Unit,
) {
    @JavascriptInterface
    fun onBackPressed() {
        callback()
    }
}
