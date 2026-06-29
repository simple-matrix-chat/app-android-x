/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.messagecomposer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.element.android.emojibasebindings.Emoji
import io.element.android.features.messages.impl.timeline.components.customreaction.picker.EmojiPicker
import io.element.android.features.messages.impl.timeline.components.customreaction.picker.EmojiPickerPresenter
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.hide
import kotlinx.collections.immutable.persistentSetOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComposerEmojiPickerBottomSheet(
    state: MessageComposerState,
    modifier: Modifier = Modifier,
) {
    val emojiPickerState = state.composerEmojiPickerState
    if (!emojiPickerState.isVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    fun onDismiss() {
        state.eventSink(MessageComposerEvent.DismissComposerEmojiPicker)
    }

    fun onEmojiSelected(emoji: Emoji) {
        sheetState.hide(coroutineScope) {
            state.eventSink(MessageComposerEvent.InsertPlainText(emoji.unicode))
        }
    }

    ModalBottomSheet(
        onDismissRequest = ::onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        scrollable = false,
    ) {
        val presenter = remember(emojiPickerState.emojibaseStore, emojiPickerState.recentEmojis) {
            EmojiPickerPresenter(
                emojibaseStore = emojiPickerState.emojibaseStore,
                recentEmojis = emojiPickerState.recentEmojis,
                coroutineDispatchers = CoroutineDispatchers.Default,
            )
        }
        EmojiPicker(
            onSelectEmoji = ::onEmojiSelected,
            state = presenter.present(),
            selectedEmojis = persistentSetOf(),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@PreviewsDayNight
@Composable
internal fun ComposerEmojiPickerBottomSheetPreview() = ElementPreview {
    ComposerEmojiPickerBottomSheet(
        state = aMessageComposerState(
            composerEmojiPickerState = aComposerEmojiPickerState(isVisible = true),
        ),
    )
}
