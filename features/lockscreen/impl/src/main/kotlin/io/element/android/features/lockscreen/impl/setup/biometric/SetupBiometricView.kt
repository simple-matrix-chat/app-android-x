/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.lockscreen.impl.setup.biometric

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.lockscreen.impl.R
import io.element.android.features.lockscreen.impl.components.MomentLockScreenHeader
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.TextButton

@Composable
fun SetupBiometricView(
    state: SetupBiometricState,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        state.eventSink(SetupBiometricEvent.UsePin)
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ElementTheme.colors.bgCanvasDefault,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .navigationBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
            ) {
                SetupBiometricHeader()
            }
            SetupBiometricFooter(
                onAllowClick = { state.eventSink(SetupBiometricEvent.AllowBiometric) },
                onSkipClick = { state.eventSink(SetupBiometricEvent.UsePin) }
            )
        }
    }
}

@Composable
private fun SetupBiometricHeader() {
    val biometricAuth = stringResource(id = R.string.screen_app_lock_biometric_authentication)
    MomentLockScreenHeader(
        imageVector = Icons.Default.Fingerprint,
        title = stringResource(id = R.string.screen_app_lock_settings_enable_biometric_unlock),
        subtitle = stringResource(id = R.string.screen_app_lock_setup_biometric_unlock_subtitle, biometricAuth),
        iconSize = 72.dp,
        iconTileSize = 120.dp,
    )
}

@Composable
private fun SetupBiometricFooter(
    onAllowClick: () -> Unit,
    onSkipClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val biometricAuth = stringResource(id = R.string.screen_app_lock_biometric_authentication)
        Button(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.screen_app_lock_setup_biometric_unlock_allow_title, biometricAuth),
            onClick = onAllowClick
        )
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.screen_app_lock_setup_biometric_unlock_skip),
            onClick = onSkipClick
        )
    }
}

@Composable
@PreviewsDayNight
internal fun SetupBiometricViewPreview(@PreviewParameter(SetupBiometricStateProvider::class) state: SetupBiometricState) {
    ElementPreview {
        SetupBiometricView(
            state = state,
        )
    }
}
