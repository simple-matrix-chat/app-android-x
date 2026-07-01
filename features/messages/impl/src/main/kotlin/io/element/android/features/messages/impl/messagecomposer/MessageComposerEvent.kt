/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.messagecomposer

import android.net.Uri
import io.element.android.libraries.textcomposer.mentions.ResolvedSuggestion
import io.element.android.libraries.textcomposer.model.MessageComposerMode
import io.element.android.libraries.textcomposer.model.Suggestion

sealed interface MessageComposerEvent {
    data object ToggleFullScreenState : MessageComposerEvent
    data object SendMessage : MessageComposerEvent
    data class SendUri(val uri: Uri) : MessageComposerEvent
    data object CloseSpecialMode : MessageComposerEvent
    data class SetMode(val composerMode: MessageComposerMode) : MessageComposerEvent
    data object AddAttachment : MessageComposerEvent
    data object DismissAttachmentMenu : MessageComposerEvent
    data object DismissComposerEmojiPicker : MessageComposerEvent
    data object DismissContactAttachmentPicker : MessageComposerEvent
    data object RequestContactAttachmentPermission : MessageComposerEvent
    data object RetryLoadContactAttachments : MessageComposerEvent
    data class SelectContactAttachment(val formattedContact: String) : MessageComposerEvent
    data class InsertPlainText(val text: String) : MessageComposerEvent
    sealed interface PickAttachmentSource : MessageComposerEvent {
        data object Emoji : PickAttachmentSource
        data object Sticker : PickAttachmentSource
        data object FromGallery : PickAttachmentSource
        data object FromVideoGallery : PickAttachmentSource
        data object FromFiles : PickAttachmentSource
        data object FromCamera : PickAttachmentSource
        data object PhotoFromCamera : PickAttachmentSource
        data object VideoFromCamera : PickAttachmentSource
        data object Location : PickAttachmentSource
        data object Poll : PickAttachmentSource
        data object Contact : PickAttachmentSource
        data object VoiceMessage : PickAttachmentSource
    }

    data class ToggleTextFormatting(val enabled: Boolean) : MessageComposerEvent
    data class Error(val error: Throwable) : MessageComposerEvent
    data class TypingNotice(val isTyping: Boolean) : MessageComposerEvent
    data class SuggestionReceived(val suggestion: Suggestion?) : MessageComposerEvent
    data class InsertSuggestion(val resolvedSuggestion: ResolvedSuggestion) : MessageComposerEvent
    data object ToggleAIComposer : MessageComposerEvent
    data object QuickAIRewrite : MessageComposerEvent
    data class SelectAITransformMode(val mode: String) : MessageComposerEvent
    data class ReplaceWithAIResult(val result: String) : MessageComposerEvent
    data class SendAIResult(val result: String) : MessageComposerEvent
    data object DismissAIComposer : MessageComposerEvent
    data object SaveDraft : MessageComposerEvent
    data object ClearSlashError : MessageComposerEvent
}
