/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.messagecomposer

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.emojibasebindings.Emoji
import io.element.android.emojibasebindings.EmojibaseCategory
import io.element.android.emojibasebindings.EmojibaseStore
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.textcomposer.mentions.ResolvedSuggestion
import io.element.android.libraries.textcomposer.model.MessageComposerMode
import io.element.android.libraries.textcomposer.model.TextEditorState
import io.element.android.libraries.textcomposer.model.aTextEditorStateRich
import io.element.android.wysiwyg.display.TextDisplay
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

open class MessageComposerStateProvider : PreviewParameterProvider<MessageComposerState> {
    override val values: Sequence<MessageComposerState>
        get() = sequenceOf(
            aMessageComposerState(),
        )
}

fun aMessageComposerState(
    textEditorState: TextEditorState = aTextEditorStateRich(),
    isFullScreen: Boolean = false,
    mode: MessageComposerMode = MessageComposerMode.Normal,
    showTextFormatting: Boolean = false,
    showAttachmentSourcePicker: Boolean = false,
    composerEmojiPickerState: ComposerEmojiPickerState = aComposerEmojiPickerState(),
    contactAttachmentPickerState: ContactAttachmentPickerState = aContactAttachmentPickerState(),
    canShareLocation: Boolean = true,
    suggestions: ImmutableList<ResolvedSuggestion> = persistentListOf(),
    slashCommandAction: AsyncAction<Unit> = AsyncAction.Uninitialized,
    eventSink: (MessageComposerEvent) -> Unit = {},
) = MessageComposerState(
    textEditorState = textEditorState,
    isFullScreen = isFullScreen,
    mode = mode,
    showTextFormatting = showTextFormatting,
    showAttachmentSourcePicker = showAttachmentSourcePicker,
    composerEmojiPickerState = composerEmojiPickerState,
    contactAttachmentPickerState = contactAttachmentPickerState,
    canShareLocation = canShareLocation,
    suggestions = suggestions,
    resolveMentionDisplay = { _, _ -> TextDisplay.Plain },
    resolveAtRoomMentionDisplay = { TextDisplay.Plain },
    slashCommandAction = slashCommandAction,
    eventSink = eventSink,
)

fun aComposerEmojiPickerState(
    isVisible: Boolean = false,
) = ComposerEmojiPickerState(
    isVisible = isVisible,
    emojibaseStore = EmojibaseStore(
        persistentMapOf(
            EmojibaseCategory.People to persistentListOf(
                Emoji(
                    "1F600",
                    "grinning face",
                    persistentListOf("grinning"),
                    persistentListOf("smile"),
                    "😀",
                    null,
                )
            )
        )
    ),
    recentEmojis = persistentListOf("😀"),
)

fun aContactAttachmentPickerState(
    isVisible: Boolean = false,
    permissionState: ContactAttachmentPermissionState = ContactAttachmentPermissionState.Granted,
    contacts: ImmutableList<ContactAttachment> = persistentListOf(aContactAttachment()),
    isLoading: Boolean = false,
    hasError: Boolean = false,
) = ContactAttachmentPickerState(
    isVisible = isVisible,
    permissionState = permissionState,
    contacts = contacts,
    isLoading = isLoading,
    hasError = hasError,
)

fun aContactAttachment(
    id: String = "1",
    displayName: String = "Alice Smith",
    details: String? = "+44 7700 900000",
    formattedContact: String = "Alice Smith\n+44 7700 900000\nalice@example.org",
) = ContactAttachment(
    id = id,
    displayName = displayName,
    details = details,
    formattedContact = formattedContact,
)
