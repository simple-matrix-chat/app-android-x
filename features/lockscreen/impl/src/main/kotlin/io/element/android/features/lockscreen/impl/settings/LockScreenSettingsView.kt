/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.lockscreen.impl.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.lockscreen.impl.R
import io.element.android.features.lockscreen.impl.components.MomentLockScreenCard
import io.element.android.features.lockscreen.impl.components.MomentLockScreenRow
import io.element.android.features.lockscreen.impl.components.MomentLockScreenSection
import io.element.android.features.lockscreen.impl.components.MomentLockScreenSwitchRow
import io.element.android.features.lockscreen.impl.components.MomentLockScreenTopBar
import io.element.android.libraries.designsystem.components.dialogs.ConfirmationDialog
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Scaffold

@Composable
fun LockScreenSettingsView(
    state: LockScreenSettingsState,
    onChangePinClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ElementTheme.colors.bgSubtleSecondary,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 32.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            MomentLockScreenTopBar(
                title = stringResource(id = io.element.android.libraries.ui.strings.R.string.common_screen_lock),
                onBackClick = onBackClick,
            )
            MomentLockScreenCard {
                MomentLockScreenRow(
                    title = stringResource(id = R.string.screen_app_lock_settings_change_pin),
                    imageVector = CompoundIcons.KeySolid(),
                    onClick = onChangePinClick,
                    showDivider = state.showRemovePinOption,
                )
                if (state.showRemovePinOption) {
                    MomentLockScreenRow(
                        title = stringResource(id = R.string.screen_app_lock_settings_remove_pin),
                        imageVector = CompoundIcons.Delete(),
                        onClick = {
                            state.eventSink(LockScreenSettingsEvent.OnRemovePin)
                        },
                        destructive = true,
                        showDivider = false,
                    )
                }
            }
            if (state.showToggleBiometric) {
                MomentLockScreenSection(
                    title = stringResource(id = R.string.screen_app_lock_biometric_authentication),
                ) {
                    MomentLockScreenCard {
                        MomentLockScreenSwitchRow(
                            title = stringResource(id = R.string.screen_app_lock_settings_enable_biometric_unlock),
                            imageVector = CompoundIcons.LockSolid(),
                            checked = state.isBiometricEnabled,
                            onCheckedChange = {
                                state.eventSink(LockScreenSettingsEvent.ToggleBiometricAllowed)
                            },
                            showDivider = false,
                        )
                    }
                }
            }
        }
    }
    if (state.showRemovePinConfirmation) {
        ConfirmationDialog(
            title = stringResource(id = R.string.screen_app_lock_settings_remove_pin_alert_title),
            content = stringResource(id = R.string.screen_app_lock_settings_remove_pin_alert_message),
            onSubmitClick = {
                state.eventSink(LockScreenSettingsEvent.ConfirmRemovePin)
            },
            onDismiss = {
                state.eventSink(LockScreenSettingsEvent.CancelRemovePin)
            }
        )
    }
}

@PreviewsDayNight
@Composable
internal fun LockScreenSettingsViewPreview(
    @PreviewParameter(LockScreenSettingsStateProvider::class) state: LockScreenSettingsState,
) {
    ElementPreview {
        LockScreenSettingsView(
            state = state,
            onChangePinClick = {},
            onBackClick = {},
        )
    }
}
