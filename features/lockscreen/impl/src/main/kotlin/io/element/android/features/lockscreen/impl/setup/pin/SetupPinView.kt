/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.lockscreen.impl.setup.pin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.lockscreen.impl.R
import io.element.android.features.lockscreen.impl.components.MomentLockScreenHeader
import io.element.android.features.lockscreen.impl.components.MomentLockScreenTopBar
import io.element.android.features.lockscreen.impl.components.PinEntryTextField
import io.element.android.features.lockscreen.impl.setup.pin.validation.SetupPinFailure
import io.element.android.libraries.designsystem.components.dialogs.ErrorDialog
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Scaffold

@Composable
fun SetupPinView(
    state: SetupPinState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ElementTheme.colors.bgCanvasDefault,
        content = { padding ->
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .imePadding()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .verticalScroll(state = scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 32.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(36.dp),
            ) {
                MomentLockScreenTopBar(onBackClick = onBackClick)
                SetupPinHeader(state.isConfirmationStep, state.appName)
                SetupPinContent(state)
            }
        }
    )
}

@Composable
private fun SetupPinHeader(
    isValidationStep: Boolean,
    appName: String,
) {
    MomentLockScreenHeader(
        title = if (isValidationStep) {
            stringResource(id = R.string.screen_app_lock_setup_confirm_pin)
        } else {
            stringResource(id = R.string.screen_app_lock_setup_choose_pin)
        },
        subtitle = stringResource(id = R.string.screen_app_lock_setup_pin_context, appName),
        imageVector = CompoundIcons.LockSolid(),
        iconSize = 32.dp,
        iconTileSize = 72.dp,
    )
}

@Composable
private fun SetupPinContent(
    state: SetupPinState,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    PinEntryTextField(
        pinEntry = state.activePinEntry,
        isSecured = true,
        onValueChange = { entry ->
            state.eventSink(SetupPinEvent.OnPinEntryChanged(entry, state.isConfirmationStep))
        },
        modifier = Modifier
            .focusRequester(focusRequester)
            .padding(top = 4.dp)
            .fillMaxWidth()
    )
    if (state.setupPinFailure != null) {
        ErrorDialog(
            title = state.setupPinFailure.title(),
            content = state.setupPinFailure.content(),
            onSubmit = {
                state.eventSink(SetupPinEvent.ClearFailure)
            }
        )
    }
}

@Composable
@ReadOnlyComposable
private fun SetupPinFailure.content(): String {
    return when (this) {
        SetupPinFailure.ForbiddenPin -> stringResource(id = R.string.screen_app_lock_setup_pin_forbidden_dialog_content)
        SetupPinFailure.PinsDoNotMatch -> stringResource(id = R.string.screen_app_lock_setup_pin_mismatch_dialog_content)
    }
}

@Composable
@ReadOnlyComposable
private fun SetupPinFailure.title(): String {
    return when (this) {
        SetupPinFailure.ForbiddenPin -> stringResource(id = R.string.screen_app_lock_setup_pin_forbidden_dialog_title)
        SetupPinFailure.PinsDoNotMatch -> stringResource(id = R.string.screen_app_lock_setup_pin_mismatch_dialog_title)
    }
}

@Composable
@PreviewsDayNight
internal fun SetupPinViewPreview(@PreviewParameter(SetupPinStateProvider::class) state: SetupPinState) {
    ElementPreview {
        SetupPinView(
            state = state,
            onBackClick = {},
        )
    }
}
