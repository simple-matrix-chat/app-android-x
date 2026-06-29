/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.rageshake.impl.bugreport

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.rageshake.impl.R
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.components.async.AsyncActionView
import io.element.android.libraries.designsystem.components.form.textFieldState
import io.element.android.libraries.designsystem.modifiers.onTabOrEnterKeyFocusNext
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextField
import io.element.android.libraries.designsystem.theme.components.TextFieldValidity
import io.element.android.libraries.ui.strings.CommonStrings

private const val MIN_BUG_REPORT_DESCRIPTION_LENGTH = 10

@Composable
fun BugReportView(
    state: BugReportState,
    onViewLogs: () -> Unit,
    onSuccess: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackClick)

    val eventSink = state.eventSink
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isFormEnabled = state.sending !is AsyncAction.Loading
    var descriptionFieldState by textFieldState(
        stateValue = state.formState.description,
    )
    val canSend = state.submitEnabled && descriptionFieldState.length >= MIN_BUG_REPORT_DESCRIPTION_LENGTH

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ElementTheme.colors.bgSubtleSecondary,
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding()
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MomentBugReportTopBar(
                    title = stringResource(id = CommonStrings.common_report_a_problem),
                    actionText = stringResource(id = CommonStrings.action_send),
                    actionEnabled = canSend,
                    onBackClick = onBackClick,
                    onActionClick = {
                        if (canSend) {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            eventSink(BugReportEvents.SendBugReport)
                        }
                    },
                )

                MomentBugReportCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TextField(
                            value = descriptionFieldState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 132.dp)
                                .onTabOrEnterKeyFocusNext(focusManager),
                            enabled = isFormEnabled,
                            placeholder = stringResource(id = R.string.screen_bug_report_editor_placeholder),
                            onValueChange = {
                                descriptionFieldState = it
                                eventSink(BugReportEvents.SetDescription(it))
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(onNext = {
                                keyboardController?.hide()
                            }),
                            minLines = 4,
                            validity = if (state.isDescriptionInError) TextFieldValidity.Invalid else TextFieldValidity.None,
                        )
                        Text(
                            modifier = Modifier.padding(top = 10.dp),
                            text = stringResource(id = R.string.screen_bug_report_editor_description),
                            style = ElementTheme.typography.fontBodySmRegular,
                            color = ElementTheme.colors.textSecondary,
                        )
                    }
                }

                MomentBugReportCard {
                    MomentBugReportActionRow(
                        title = stringResource(id = R.string.screen_bug_report_view_logs),
                        enabled = isFormEnabled,
                        onClick = onViewLogs,
                    )
                    MomentBugReportDivider()
                    MomentBugReportToggleRow(
                        title = stringResource(id = R.string.screen_bug_report_include_logs),
                        subtitle = stringResource(id = R.string.screen_bug_report_logs_description),
                        checked = state.formState.sendLogs,
                        enabled = isFormEnabled,
                        onCheckedChange = { eventSink(BugReportEvents.SetSendLog(it)) },
                    )
                }

                if (state.screenshotUri != null) {
                    MomentBugReportCard {
                        MomentBugReportToggleRow(
                            title = stringResource(id = R.string.screen_bug_report_include_screenshot),
                            checked = state.formState.sendScreenshot,
                            enabled = isFormEnabled,
                            onCheckedChange = { eventSink(BugReportEvents.SetSendScreenshot(it)) },
                        )
                        if (state.formState.sendScreenshot) {
                            MomentBugReportScreenshotPreview(
                                screenshotUri = state.screenshotUri,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            )
                        }
                    }
                }

                MomentBugReportCard {
                    MomentBugReportToggleRow(
                        title = stringResource(id = R.string.screen_bug_report_contact_me_title),
                        subtitle = stringResource(id = R.string.screen_bug_report_contact_me),
                        checked = state.formState.canContact,
                        enabled = isFormEnabled,
                        onCheckedChange = { eventSink(BugReportEvents.SetCanContact(it)) },
                    )
                }

                MomentBugReportCard {
                    MomentBugReportToggleRow(
                        title = stringResource(R.string.screen_bug_report_send_notification_settings_title),
                        subtitle = stringResource(R.string.screen_bug_report_send_notification_settings_description),
                        checked = state.formState.sendPushRules,
                        enabled = isFormEnabled,
                        onCheckedChange = { eventSink(BugReportEvents.SetSendPushRules(it)) },
                    )
                }
            }
        }

        AsyncActionView(
            async = state.sending,
            progressDialog = { },
            onSuccess = {
                eventSink(BugReportEvents.ResetAll)
                onSuccess()
            },
            errorMessage = { error ->
                when (error) {
                    BugReportFormError.DescriptionTooShort -> stringResource(id = R.string.screen_bug_report_error_description_too_short)
                    else -> error.message ?: error.toString()
                }
            },
            onErrorDismiss = { eventSink(BugReportEvents.ClearError) },
        )
    }
}

@Composable
private fun MomentBugReportTopBar(
    title: String,
    actionText: String,
    actionEnabled: Boolean,
    onBackClick: () -> Unit,
    onActionClick: () -> Unit,
) {
    val backContentDescription = stringResource(CommonStrings.action_back)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .height(44.dp)
                .clickable(onClick = onBackClick)
                .semantics { contentDescription = backContentDescription }
                .padding(end = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = stringResource(CommonStrings.action_cancel),
                style = ElementTheme.typography.fontBodyLgMedium,
                color = ElementTheme.colors.textActionAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 84.dp),
            text = title,
            style = ElementTheme.typography.fontHeadingSmMedium,
            color = ElementTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(44.dp)
                .clickable(onClick = onActionClick)
                .padding(start = 12.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = actionText,
                style = ElementTheme.typography.fontBodyLgMedium,
                color = if (actionEnabled) ElementTheme.colors.textActionAccent else ElementTheme.colors.textDisabled,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MomentBugReportCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.55f)),
    ) {
        Column(content = content)
    }
}

@Composable
private fun MomentBugReportActionRow(
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .semantics(mergeDescendants = true) {}
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = ElementTheme.typography.fontBodyLgMedium,
            color = if (enabled) ElementTheme.colors.textPrimary else ElementTheme.colors.textDisabled,
        )
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = CompoundIcons.ChevronRight(),
            contentDescription = null,
            tint = if (enabled) ElementTheme.colors.iconSecondary else ElementTheme.colors.iconDisabled,
        )
    }
}

@Composable
private fun MomentBugReportToggleRow(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (subtitle == null) 64.dp else 78.dp)
            .semantics(mergeDescendants = true) {}
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = ElementTheme.typography.fontBodyLgMedium,
                color = if (enabled) ElementTheme.colors.textPrimary else ElementTheme.colors.textDisabled,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary,
                )
            }
        }
        Switch(
            enabled = enabled,
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun MomentBugReportDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.45f),
    )
}

@Composable
private fun MomentBugReportScreenshotPreview(
    screenshotUri: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val model = ImageRequest.Builder(context)
        .data(screenshotUri)
        // The URI stays stable while the underlying screenshot can change.
        .memoryCachePolicy(CachePolicy.DISABLED)
        .build()

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .clip(RoundedCornerShape(18.dp)),
            model = model,
            contentDescription = null,
        )
    }
}

@Preview(heightDp = 1000)
@Composable
internal fun BugReportViewDayPreview(@PreviewParameter(BugReportStateProvider::class) state: BugReportState) = ElementPreview {
    BugReportView(
        state = state,
        onSuccess = {},
        onBackClick = {},
        onViewLogs = {},
    )
}

@Preview(heightDp = 1000, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun BugReportViewNightPreview(@PreviewParameter(BugReportStateProvider::class) state: BugReportState) = ElementPreview {
    BugReportView(
        state = state,
        onSuccess = {},
        onBackClick = {},
        onViewLogs = {},
    )
}
