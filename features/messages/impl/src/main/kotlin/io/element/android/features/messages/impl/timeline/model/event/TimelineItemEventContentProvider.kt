/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.model.event

import android.graphics.Typeface
import android.text.style.StyleSpan
import android.text.style.URLSpan
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import io.element.android.wysiwyg.view.spans.CodeBlockSpan
import io.element.android.wysiwyg.view.spans.InlineCodeSpan
import io.element.android.wysiwyg.view.spans.OrderedListSpan
import io.element.android.wysiwyg.view.spans.QuoteSpan
import io.element.android.wysiwyg.view.spans.UnorderedListSpan
import org.jsoup.nodes.Document

class TimelineItemEventContentProvider : PreviewParameterProvider<TimelineItemEventContent> {
    override val values = sequenceOf(
        aTimelineItemEmoteContent(),
        aTimelineItemImageContent(),
        aTimelineItemVideoContent(),
        aTimelineItemFileContent(),
        aTimelineItemFileContent("A bigger file name which doesn't fit.pdf"),
        aTimelineItemAudioContent(),
        aTimelineItemAudioContent("An even bigger bigger bigger bigger bigger bigger bigger sound name which doesn't fit .mp3"),
        aTimelineItemVoiceContent(),
        aTimelineItemLocationContent(mode = aStaticLocationMode()),
        aTimelineItemPollContent(),
        aTimelineItemNoticeContent(),
        aTimelineItemRedactedContent(),
        aTimelineItemTextContent(),
        aTimelineItemUnknownContent(),
        aTimelineItemTextContent().copy(isEdited = true),
        aTimelineItemTextContent(body = AN_EMOJI_ONLY_TEXT),
        aTimelineItemLocationContent(
            mode = aLiveLocationMode(isActive = true, endsAt = "Ends at 12:34", endTimestamp = 0L, lastKnownLocation = null)
        ),
    )
}

const val AN_EMOJI_ONLY_TEXT = "😁"

class TimelineItemTextBasedContentProvider : PreviewParameterProvider<TimelineItemTextBasedContent> {
    private fun buildSpanned(text: String) = buildSpannedString {
        inSpans(StyleSpan(Typeface.BOLD)) {
            append("Rich Text")
        }
        append(" ")
        append(text)
    }

    private fun buildInlineRichText() = buildSpannedString {
        append("Moment ")
        inSpans(StyleSpan(Typeface.BOLD)) {
            append("bold")
        }
        append(", ")
        inSpans(InlineCodeSpan(relativeSizeProportion = 0.9f)) {
            append("inline_code")
        }
        append(", and ")
        inSpans(URLSpan("https://matrix.org")) {
            append("a link")
        }
    }

    private fun buildBlockRichText() = buildSpannedString {
        append("Text before quote\n")
        inSpans(
            QuoteSpan(
                indicatorColor = 0xFF8E8E93.toInt(),
                indicatorWidth = 2,
                indicatorPadding = 4,
                margin = 4,
            )
        ) {
            append("A compact quote that wraps onto another visual line.")
        }
        append("\n")
        inSpans(
            CodeBlockSpan(
                leadingMargin = 6,
                verticalPadding = 4,
                relativeSizeProportion = 0.9f,
            )
        ) {
            append("fun momentStyle() = true\nprintln(momentStyle())")
        }
        append("\nText after code")
    }

    private fun buildListRichText() = buildSpannedString {
        append("List preview\n")
        inSpans(UnorderedListSpan(gapWidth = 6, bulletRadius = 2)) {
            append("First bullet")
        }
        append("\n")
        inSpans(UnorderedListSpan(gapWidth = 6, bulletRadius = 2)) {
            append("Second bullet with enough text to wrap naturally")
        }
        append("\n")
        inSpans(
            OrderedListSpan(
                typeface = Typeface.defaultFromStyle(Typeface.NORMAL),
                textSize = 16f,
                order = 1,
                gapWidth = 6,
            )
        ) {
            append("First ordered item")
        }
        append("\n")
        inSpans(
            OrderedListSpan(
                typeface = Typeface.defaultFromStyle(Typeface.NORMAL),
                textSize = 16f,
                order = 2,
                gapWidth = 6,
            )
        ) {
            append("Second ordered item")
        }
    }

    override val values = sequenceOf(
        aTimelineItemEmoteContent(),
        aTimelineItemEmoteContent().copy(formattedBody = buildSpanned("Emote")),
        aTimelineItemNoticeContent(),
        aTimelineItemNoticeContent().copy(formattedBody = buildSpanned("Notice")),
        aTimelineItemTextContent(),
        aTimelineItemTextContent().copy(formattedBody = buildSpanned("Text")),
        aTimelineItemTextContent(
            body = "Moment rich text",
            formattedBody = buildInlineRichText(),
        ),
        aTimelineItemTextContent(
            body = "Moment block text",
            formattedBody = buildBlockRichText(),
        ),
        aTimelineItemTextContent(
            body = "Moment list text",
            formattedBody = buildListRichText(),
        ),
    )
}

fun aTimelineItemEmoteContent(
    body: String = "Emote",
    htmlDocument: Document? = null,
    formattedBody: CharSequence = body,
    isEdited: Boolean = false,
) = TimelineItemEmoteContent(
    body = body,
    htmlDocument = htmlDocument,
    formattedBody = formattedBody,
    isEdited = isEdited,
)

fun aTimelineItemNoticeContent(
    body: String = "Notice",
    htmlDocument: Document? = null,
    formattedBody: CharSequence = body,
    isEdited: Boolean = false,
) = TimelineItemNoticeContent(
    body = body,
    htmlDocument = htmlDocument,
    formattedBody = formattedBody,
    isEdited = isEdited,
)

fun aTimelineItemRedactedContent() = TimelineItemRedactedContent

fun aTimelineItemTextContent(
    body: String = "Text",
    htmlDocument: Document? = null,
    formattedBody: CharSequence = body,
    isEdited: Boolean = false,
) = TimelineItemTextContent(
    body = body,
    htmlDocument = htmlDocument,
    formattedBody = formattedBody,
    isEdited = isEdited,
)

fun aTimelineItemUnknownContent() = TimelineItemUnknownContent

fun aTimelineItemStateEventContent(
    body: String = "A state event",
) = TimelineItemStateEventContent(
    body = body,
)
