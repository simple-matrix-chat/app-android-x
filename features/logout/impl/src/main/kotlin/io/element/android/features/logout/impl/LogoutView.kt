/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.logout.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.logout.impl.ui.LogoutActionDialog
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.atomic.pages.FlowStepPage
import io.element.android.libraries.designsystem.components.BigIcon
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.testtags.testTag
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun LogoutView(
    state: LogoutState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val eventSink = state.eventSink

    FlowStepPage(
        onBackClick = onBackClick,
        title = stringResource(CommonStrings.action_signout),
        subTitle = null,
        iconStyle = BigIcon.Style.Default(CompoundIcons.KeySolid()),
        modifier = modifier,
        buttons = {
            Buttons(
                state = state,
                onLogoutClick = {
                    eventSink(LogoutEvents.Logout(ignoreSdkError = false))
                }
            )
        },
    ) {
        Content(state)
    }

    LogoutActionDialog(
        state.logoutAction,
        onConfirmClick = {
            eventSink(LogoutEvents.Logout(ignoreSdkError = false))
        },
        onForceLogoutClick = {
            eventSink(LogoutEvents.Logout(ignoreSdkError = true))
        },
        onDismissDialog = {
            eventSink(LogoutEvents.CloseDialogs)
        },
    )
}

@Composable
private fun ColumnScope.Buttons(
    state: LogoutState,
    onLogoutClick: () -> Unit,
) {
    val logoutAction = state.logoutAction
    val signOutSubmitRes = when {
        logoutAction is AsyncAction.Loading -> R.string.screen_signout_in_progress_dialog_content
        else -> CommonStrings.action_signout
    }
    Button(
        text = stringResource(id = signOutSubmitRes),
        showProgress = logoutAction is AsyncAction.Loading,
        destructive = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.signOut),
        onClick = onLogoutClick,
    )
}

@Composable
private fun Content(
    state: LogoutState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 60.dp, start = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Unit
    }
}

@PreviewsDayNight
@Composable
internal fun LogoutViewPreview(
    @PreviewParameter(LogoutStateProvider::class) state: LogoutState,
) = ElementPreview {
    LogoutView(
        state,
        onBackClick = {},
    )
}
