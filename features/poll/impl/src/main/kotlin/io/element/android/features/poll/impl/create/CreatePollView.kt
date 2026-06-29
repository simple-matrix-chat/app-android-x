/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.poll.impl.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.poll.impl.R
import io.element.android.libraries.designsystem.components.dialogs.ConfirmationDialog
import io.element.android.libraries.designsystem.components.dialogs.SaveChangesDialog
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Switch
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.designsystem.theme.components.TextField
import io.element.android.libraries.matrix.api.poll.PollKind
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun CreatePollView(
    state: CreatePollState,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    val navBack = { state.eventSink(CreatePollEvent.ConfirmNavBack) }
    BackHandler(onBack = navBack)
    if (state.showBackConfirmation) {
        SaveChangesDialog(
            onSaveClick = { state.eventSink(CreatePollEvent.Save) },
            onDiscardClick = { state.eventSink(CreatePollEvent.NavBack) },
            onDismiss = { state.eventSink(CreatePollEvent.HideConfirmation) },
        )
    }
    if (state.showDeleteConfirmation) {
        ConfirmationDialog(
            title = stringResource(id = R.string.screen_edit_poll_delete_confirmation_title),
            content = stringResource(id = R.string.screen_edit_poll_delete_confirmation),
            onSubmitClick = { state.eventSink(CreatePollEvent.Delete(confirmed = true)) },
            onDismiss = { state.eventSink(CreatePollEvent.HideConfirmation) }
        )
    }
    val questionFocusRequester = remember { FocusRequester() }
    val answerFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        questionFocusRequester.requestFocus()
    }
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding(),
        containerColor = ElementTheme.colors.bgSubtleSecondary,
        topBar = {
            CreatePollTopBar(
                mode = state.mode,
                saveEnabled = state.canSave,
                onBackClick = navBack,
                onSaveClick = { state.eventSink(CreatePollEvent.Save) }
            )
        },
    ) { paddingValues ->
        val lazyListState = rememberLazyListState()
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding()
                .fillMaxSize(),
            state = lazyListState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                PollSection(title = stringResource(id = R.string.screen_create_poll_question_desc)) {
                    TextField(
                        value = state.question,
                        onValueChange = {
                            state.eventSink(CreatePollEvent.SetQuestion(it))
                        },
                        modifier = Modifier
                            .focusRequester(questionFocusRequester)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        placeholder = stringResource(id = R.string.screen_create_poll_question_hint),
                        keyboardOptions = keyboardOptions,
                    )
                }
            }
            item {
                PollSection(title = stringResource(id = R.string.screen_create_poll_options_section_title)) {
                    state.answers.forEachIndexed { index, answer ->
                        val isLastItem = index == state.answers.size - 1
                        PollAnswerRow(
                            value = answer.text,
                            placeholder = stringResource(id = R.string.screen_create_poll_answer_hint, index + 1),
                            canDelete = answer.canDelete,
                            modifier = if (isLastItem) {
                                Modifier.focusRequester(answerFocusRequester)
                            } else {
                                Modifier
                            },
                            onValueChange = { state.eventSink(CreatePollEvent.SetAnswer(index, it)) },
                            onDeleteClick = { state.eventSink(CreatePollEvent.RemoveAnswer(index)) },
                        )
                        PollDivider()
                    }
                    if (state.canAddAnswer) {
                        AddOptionRow(
                            onClick = {
                                state.eventSink(CreatePollEvent.AddAnswer)
                                coroutineScope.launch(Dispatchers.Main) {
                                    lazyListState.animateScrollToItem(1)
                                    answerFocusRequester.requestFocus()
                                }
                            }
                        )
                    }
                }
            }
            item {
                PollSection(title = stringResource(id = R.string.screen_create_poll_settings_section_title)) {
                    PollSwitchRow(
                        title = stringResource(id = R.string.screen_create_poll_anonymous_headline),
                        description = stringResource(id = R.string.screen_create_poll_anonymous_desc),
                        checked = state.pollKind == PollKind.Undisclosed,
                        onClick = {
                            state.eventSink(
                                CreatePollEvent.SetPollKind(
                                    if (state.pollKind == PollKind.Disclosed) PollKind.Undisclosed else PollKind.Disclosed
                                )
                            )
                        },
                    )
                }
            }
            if (state.canDelete) {
                item {
                    PollSection {
                        DeletePollRow(
                            onClick = { state.eventSink(CreatePollEvent.Delete(confirmed = false)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatePollTopBar(
    mode: CreatePollState.Mode,
    saveEnabled: Boolean,
    onBackClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        TextButton(
            modifier = Modifier.align(Alignment.CenterStart),
            text = stringResource(id = CommonStrings.action_cancel),
            onClick = onBackClick,
        )
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 84.dp)
                .semantics { heading() },
            text = when (mode) {
                CreatePollState.Mode.New -> stringResource(id = R.string.screen_create_poll_title)
                CreatePollState.Mode.Edit -> stringResource(id = R.string.screen_edit_poll_title)
            },
            style = ElementTheme.typography.fontHeadingSmMedium,
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            text = when (mode) {
                CreatePollState.Mode.New -> stringResource(id = CommonStrings.action_create)
                CreatePollState.Mode.Edit -> stringResource(id = CommonStrings.action_done)
            },
            onClick = onSaveClick,
            enabled = saveEnabled,
        )
    }
}

@Composable
private fun PollSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (title != null) {
            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = title,
                style = ElementTheme.typography.fontBodySmMedium,
                color = ElementTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = ElementTheme.colors.bgCanvasDefault,
            border = BorderStroke(1.dp, ElementTheme.colors.borderDisabled),
            shadowElevation = 3.dp,
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun PollAnswerRow(
    value: String,
    placeholder: String,
    canDelete: Boolean,
    onValueChange: (String) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            modifier = Modifier.size(44.dp),
            enabled = canDelete,
            onClick = onDeleteClick,
        ) {
            Icon(
                imageVector = CompoundIcons.Delete(),
                contentDescription = stringResource(R.string.screen_create_poll_delete_option_a11y, value),
                tint = if (canDelete) ElementTheme.colors.iconCriticalPrimary else ElementTheme.colors.iconDisabled,
            )
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .weight(1f)
                .fillMaxWidth(),
            placeholder = placeholder,
            keyboardOptions = keyboardOptions,
        )
    }
}

@Composable
private fun AddOptionRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = CompoundIcons.Plus(),
            contentDescription = null,
            tint = ElementTheme.colors.iconAccentPrimary,
        )
        Text(
            text = stringResource(id = R.string.screen_create_poll_add_option_btn),
            style = ElementTheme.typography.fontBodyLgRegular,
            color = ElementTheme.colors.textActionAccent,
        )
    }
}

@Composable
private fun PollSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = ElementTheme.typography.fontBodyLgRegular,
                color = ElementTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = ElementTheme.typography.fontBodyMdRegular,
                color = ElementTheme.colors.textSecondary,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
        )
    }
}

@Composable
private fun DeletePollRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = CommonStrings.action_delete_poll),
            style = ElementTheme.typography.fontBodyLgRegular,
            color = ElementTheme.colors.textCriticalPrimary,
        )
    }
}

@Composable
private fun PollDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.22f),
    )
}

@PreviewsDayNight
@Composable
internal fun CreatePollViewPreview(
    @PreviewParameter(CreatePollStateProvider::class) state: CreatePollState
) = ElementPreview {
    CreatePollView(
        state = state,
    )
}

private val keyboardOptions = KeyboardOptions.Default.copy(
    capitalization = KeyboardCapitalization.Sentences,
    imeAction = ImeAction.Next,
)
