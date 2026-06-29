/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.logout.impl

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.deactivation.impl.R
import io.element.android.features.logout.impl.ui.AccountDeactivationActionDialog
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.components.form.textFieldState
import io.element.android.libraries.designsystem.modifiers.onTabOrEnterKeyFocusNext
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.text.buildAnnotatedStringWithStyledPart
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Switch
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextField
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.testtags.testTag
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun AccountDeactivationView(
    state: AccountDeactivationState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val eventSink = state.eventSink

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ElementTheme.colors.bgSubtleSecondary,
        bottomBar = {
            MomentDeactivationBottomBar(
                state = state,
                onSubmitClick = {
                    eventSink(AccountDeactivationEvents.DeactivateAccount(isRetry = false))
                },
            )
        },
    ) { padding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .imePadding()
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(state = scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            MomentDeactivationTopBar(onBackClick = onBackClick)
            Content(
                state = state,
                onSubmitClick = {
                    eventSink(AccountDeactivationEvents.DeactivateAccount(isRetry = false))
                }
            )
        }
    }
    AccountDeactivationActionDialog(
        state.accountDeactivationAction,
        onConfirmClick = {
            eventSink(AccountDeactivationEvents.DeactivateAccount(isRetry = false))
        },
        onRetryClick = {
            eventSink(AccountDeactivationEvents.DeactivateAccount(isRetry = true))
        },
        onDismissDialog = {
            eventSink(AccountDeactivationEvents.CloseDialogs)
        },
    )
}

@Composable
private fun MomentDeactivationTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBackClick,
                ),
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
            text = stringResource(R.string.screen_deactivate_account_title),
            style = ElementTheme.typography.fontHeadingSmMedium,
            color = ElementTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MomentDeactivationBottomBar(
    state: AccountDeactivationState,
    onSubmitClick: () -> Unit,
) {
    Surface(
        color = ElementTheme.colors.bgSubtleSecondary,
    ) {
        Button(
            text = stringResource(CommonStrings.action_deactivate_account),
            showProgress = state.accountDeactivationAction is AsyncAction.Loading,
            destructive = true,
            enabled = state.submitEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            onClick = onSubmitClick,
        )
    }
}

@Composable
private fun Content(
    state: AccountDeactivationState,
    onSubmitClick: () -> Unit,
) {
    val isLoading = state.accountDeactivationAction is AsyncAction.Loading
    val eraseData = state.deactivateFormState.eraseData
    var passwordFieldState by textFieldState(stateValue = state.deactivateFormState.password)

    val focusManager = LocalFocusManager.current
    val eventSink = state.eventSink

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        MomentDeactivationInfoCard()
        MomentDeactivationEraseDataSection(
            eraseData = eraseData,
            enabled = !isLoading,
            onEraseDataChange = {
                eventSink(AccountDeactivationEvents.SetEraseData(it))
            },
        )
        MomentDeactivationPasswordSection(
            passwordFieldState = passwordFieldState,
            isLoading = isLoading,
            onSubmitClick = onSubmitClick,
            onPasswordChange = {
                passwordFieldState = it
                eventSink(AccountDeactivationEvents.SetPassword(it))
            },
            focusManagerModifier = Modifier.onTabOrEnterKeyFocusNext(focusManager),
        )
    }
}

@Composable
private fun MomentDeactivationInfoCard(
    modifier: Modifier = Modifier,
) {
    MomentDeactivationCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = buildAnnotatedStringWithStyledPart(
                    R.string.screen_deactivate_account_description,
                    R.string.screen_deactivate_account_description_bold_part,
                    color = ElementTheme.colors.textSecondary,
                    bold = true,
                    underline = false,
                ),
                style = ElementTheme.typography.fontBodyMdRegular,
                color = ElementTheme.colors.textSecondary,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MomentDeactivationInfoItem(
                    message = buildAnnotatedStringWithStyledPart(
                        R.string.screen_deactivate_account_list_item_1,
                        R.string.screen_deactivate_account_list_item_1_bold_part,
                        color = ElementTheme.colors.textSecondary,
                        bold = true,
                        underline = false,
                    ),
                    imageVector = CompoundIcons.Close(),
                    iconTint = ElementTheme.colors.iconCriticalPrimary,
                )
                MomentDeactivationInfoItem(
                    message = AnnotatedString(stringResource(R.string.screen_deactivate_account_list_item_2)),
                    imageVector = CompoundIcons.Close(),
                    iconTint = ElementTheme.colors.iconCriticalPrimary,
                )
                MomentDeactivationInfoItem(
                    message = AnnotatedString(stringResource(R.string.screen_deactivate_account_list_item_3)),
                    imageVector = CompoundIcons.Close(),
                    iconTint = ElementTheme.colors.iconCriticalPrimary,
                )
                MomentDeactivationInfoItem(
                    message = AnnotatedString(stringResource(R.string.screen_deactivate_account_list_item_4)),
                    imageVector = CompoundIcons.Check(),
                    iconTint = ElementTheme.colors.iconSuccessPrimary,
                )
            }
        }
    }
}

@Composable
private fun MomentDeactivationInfoItem(
    message: AnnotatedString,
    imageVector: ImageVector,
    iconTint: Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(20.dp),
            imageVector = imageVector,
            contentDescription = null,
            tint = iconTint,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = message,
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun MomentDeactivationEraseDataSection(
    eraseData: Boolean,
    enabled: Boolean,
    onEraseDataChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    MomentDeactivationCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .semantics(mergeDescendants = true) {}
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onEraseDataChange(!eraseData) },
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.screen_deactivate_account_delete_all_messages),
                style = ElementTheme.typography.fontBodyLgMedium,
                color = if (enabled) ElementTheme.colors.textPrimary else ElementTheme.colors.textDisabled,
            )
            Switch(
                checked = eraseData,
                enabled = enabled,
                onCheckedChange = onEraseDataChange,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 20.dp),
            color = ElementTheme.colors.borderDisabled,
        )
        Text(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            text = stringResource(R.string.screen_deactivate_account_delete_all_messages_notice),
            style = ElementTheme.typography.fontBodySmRegular,
            color = ElementTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun MomentDeactivationPasswordSection(
    passwordFieldState: String,
    isLoading: Boolean,
    onSubmitClick: () -> Unit,
    onPasswordChange: (String) -> Unit,
    focusManagerModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    if (isLoading) {
        // Ensure password is hidden when user submits the form.
        passwordVisible = false
    }

    MomentDeactivationSection(
        title = stringResource(CommonStrings.action_confirm_password),
        modifier = modifier,
    ) {
        MomentDeactivationCard {
            Box(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                TextField(
                    value = passwordFieldState,
                    label = stringResource(CommonStrings.common_password),
                    readOnly = isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(focusManagerModifier)
                        .testTag(TestTags.loginPassword)
                        .semantics {
                            contentType = ContentType.Password
                        },
                    onValueChange = {
                        onPasswordChange(it.sanitize())
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image =
                            if (passwordVisible) CompoundIcons.VisibilityOn() else CompoundIcons.VisibilityOff()
                        val description =
                            if (passwordVisible) stringResource(CommonStrings.a11y_hide_password) else stringResource(CommonStrings.a11y_show_password)

                        Box(modifier = Modifier.clickable { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, description)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onSubmitClick() }
                    ),
                    singleLine = true,
                )
            }
        }
    }
}

@Composable
private fun MomentDeactivationSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = title,
            style = ElementTheme.typography.fontBodySmMedium,
            color = ElementTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        content()
    }
}

@Composable
private fun MomentDeactivationCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.55f)),
    ) {
        Column(content = content)
    }
}

/**
 * Ensure that the string does not contain any new line characters, which can happen when pasting values.
 */
private fun String.sanitize(): String {
    return replace("\n", "")
}

@PreviewsDayNight
@Composable
internal fun AccountDeactivationViewPreview(
    @PreviewParameter(AccountDeactivationStateProvider::class) state: AccountDeactivationState,
) = ElementPreview {
    AccountDeactivationView(
        state,
        onBackClick = {},
    )
}
