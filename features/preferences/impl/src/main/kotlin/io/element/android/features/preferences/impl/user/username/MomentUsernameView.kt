/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.user.username

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun MomentUsernameView(
    state: MomentUsernameState,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ElementTheme.colors.bgSubtleSecondary,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            MomentUsernameTopBar(
                onBackClick = {
                    focusManager.clearFocus()
                    state.eventSink(MomentUsernameEvent.Close)
                },
            )
            MomentUsernameTitleBlock()
            MomentUsernameInputCard(
                username = state.username,
                usernameError = state.usernameError,
                focusRequester = focusRequester,
                onUsernameChange = { state.eventSink(MomentUsernameEvent.UpdateUsername(it)) },
                onDone = {
                    focusManager.clearFocus()
                    state.eventSink(MomentUsernameEvent.Save)
                },
            )
            MomentUsernameSaveButton(
                enabled = state.canSave,
                isSaving = state.isSaving,
                onClick = {
                    focusManager.clearFocus()
                    state.eventSink(MomentUsernameEvent.Save)
                },
            )
        }
    }
}

@Composable
private fun MomentUsernameTopBar(
    onBackClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = CompoundIcons.ChevronLeft(),
                contentDescription = stringResource(CommonStrings.action_back),
                tint = ElementTheme.colors.iconPrimary,
            )
        }
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp),
            text = stringResource(R.string.screen_moment_edit_profile_username),
            style = ElementTheme.typography.fontHeadingSmMedium,
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MomentUsernameTitleBlock() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.screen_moment_edit_profile_username),
            style = ElementTheme.typography.fontHeadingXlBold,
            color = ElementTheme.colors.textPrimary,
        )
        Text(
            text = stringResource(R.string.screen_moment_username_description),
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun MomentUsernameInputCard(
    username: String,
    usernameError: MomentUsernameError?,
    focusRequester: FocusRequester,
    onUsernameChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.screen_moment_username_field_label).uppercase(),
                style = ElementTheme.typography.fontBodyXsMedium,
                color = ElementTheme.colors.textSecondary,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = ElementTheme.colors.bgSubtleSecondary,
                        shape = RoundedCornerShape(18.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "@",
                    style = ElementTheme.typography.fontHeadingMdBold,
                    color = ElementTheme.colors.textPrimary,
                )
                BasicTextField(
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    value = username,
                    onValueChange = onUsernameChange,
                    singleLine = true,
                    textStyle = ElementTheme.typography.fontHeadingMdBold.copy(
                        color = ElementTheme.colors.textPrimary,
                    ),
                    cursorBrush = SolidColor(ElementTheme.colors.textPrimary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onDone() }),
                    decorationBox = { innerTextField ->
                        Box {
                            if (username.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.screen_moment_edit_profile_username_placeholder),
                                    style = ElementTheme.typography.fontHeadingMdBold,
                                    color = ElementTheme.colors.textSecondary,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
            Text(
                text = usernameValidationText(usernameError),
                style = ElementTheme.typography.fontBodySmRegular,
                color = if (usernameError == null) ElementTheme.colors.textSecondary else ElementTheme.colors.textCriticalPrimary,
            )
        }
    }
}

@Composable
private fun MomentUsernameSaveButton(
    enabled: Boolean,
    isSaving: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isSaving) stringResource(CommonStrings.common_saving) else stringResource(CommonStrings.action_save),
            style = ElementTheme.typography.fontBodyLgMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun usernameValidationText(error: MomentUsernameError?): String {
    return when (error) {
        null -> stringResource(R.string.screen_moment_edit_profile_username_rules)
        MomentUsernameError.Required -> stringResource(R.string.screen_moment_edit_profile_username_required)
        MomentUsernameError.TooShort -> stringResource(R.string.screen_moment_edit_profile_username_too_short)
        MomentUsernameError.TooLong -> stringResource(R.string.screen_moment_edit_profile_username_too_long)
        MomentUsernameError.Invalid -> stringResource(R.string.screen_moment_edit_profile_username_invalid)
        MomentUsernameError.Taken -> stringResource(R.string.screen_moment_edit_profile_username_taken)
        MomentUsernameError.Unsupported -> stringResource(R.string.screen_moment_edit_profile_username_unsupported)
        MomentUsernameError.SaveFailed -> stringResource(R.string.screen_moment_edit_profile_username_save_failed)
    }
}

@PreviewsDayNight
@Composable
internal fun MomentUsernameViewPreview(@PreviewParameter(MomentUsernameStateProvider::class) state: MomentUsernameState) =
    ElementPreview {
        MomentUsernameView(state)
    }
