/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.event

import android.text.SpannableString
import android.text.Spanned
import android.text.SpannedString
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.core.text.getSpans
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.messages.impl.timeline.components.layout.ContentAvoidingLayout
import io.element.android.features.messages.impl.timeline.components.layout.ContentAvoidingLayoutData
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemTextBasedContent
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemTextBasedContentProvider
import io.element.android.features.messages.impl.timeline.model.event.aTimelineItemTextContent
import io.element.android.features.messages.impl.utils.containsOnlyEmojis
import io.element.android.libraries.androidutils.text.LinkifyHelper
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.textcomposer.ElementRichTextEditorStyle
import io.element.android.libraries.textcomposer.mentions.LocalMentionSpanUpdater
import io.element.android.wysiwyg.compose.EditorStyledText
import io.element.android.wysiwyg.link.Link
import io.element.android.wysiwyg.view.spans.QuoteSpan

@Composable
fun TimelineItemTextView(
    content: TimelineItemTextBasedContent,
    onLinkClick: (Link) -> Unit,
    onLinkLongClick: (Link) -> Unit,
    modifier: Modifier = Modifier,
    onContentLayoutChange: (ContentAvoidingLayoutData) -> Unit = {},
) {
    val emojiOnly = content.formattedBody.toString() == content.body &&
        content.body.replace(" ", "").containsOnlyEmojis()
    val textStyle = when {
        emojiOnly -> ElementTheme.typography.fontHeadingXlRegular
        else -> ElementTheme.typography.fontBodyLgRegular
    }
    CompositionLocalProvider(
        LocalContentColor provides ElementTheme.colors.textPrimary,
        LocalTextStyle provides textStyle
    ) {
        val text = getTextWithResolvedMentions(content)
        Box(
            modifier
                .padding(horizontal = 2.dp)
                .semantics { contentDescription = content.plainText }
        ) {
            EditorStyledText(
                text = text,
                onLinkClickedListener = onLinkClick,
                onLinkLongClickedListener = onLinkLongClick,
                style = ElementRichTextEditorStyle.textStyle(usesMomentTimelineStyle = true),
                onTextLayout = ContentAvoidingLayout.measureLegacyLastTextLine(
                    onContentLayoutChange = onContentLayoutChange,
                    extraWidth = 4.dp,
                ),
                releaseOnDetach = false,
            )
        }
    }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun getTextWithResolvedMentions(content: TimelineItemTextBasedContent): CharSequence {
    val mentionSpanUpdater = LocalMentionSpanUpdater.current
    val bodyWithResolvedMentions = mentionSpanUpdater.rememberMentionSpans(content.formattedBody)
    val quoteColor = ElementTheme.colors.textSecondary.toArgb()
    val density = LocalDensity.current
    val indicatorWidth = with(density) { 1.5.dp.roundToPx().coerceAtLeast(1) }
    val indicatorPadding = with(density) { 4.dp.roundToPx() }
    val margin = with(density) { 4.dp.roundToPx() }
    return remember(bodyWithResolvedMentions, quoteColor, indicatorWidth, indicatorPadding, margin) {
        SpannedString.valueOf(
            bodyWithResolvedMentions.withMomentQuoteSpans(
                indicatorColor = quoteColor,
                indicatorWidth = indicatorWidth,
                indicatorPadding = indicatorPadding,
                margin = margin,
            )
        )
    }
}

private fun CharSequence.withMomentQuoteSpans(
    indicatorColor: Int,
    indicatorWidth: Int,
    indicatorPadding: Int,
    margin: Int,
): CharSequence {
    if (this !is Spanned) return this
    val quoteSpans = getSpans<QuoteSpan>(0, length)
    if (quoteSpans.isEmpty()) return this

    val spannable = SpannableString(this)
    quoteSpans.forEach { quoteSpan ->
        val start = spannable.getSpanStart(quoteSpan)
        val end = spannable.getSpanEnd(quoteSpan)
        val flags = spannable.getSpanFlags(quoteSpan)
        spannable.removeSpan(quoteSpan)
        spannable.setSpan(
            QuoteSpan(
                indicatorColor = indicatorColor,
                indicatorWidth = indicatorWidth,
                indicatorPadding = indicatorPadding,
                margin = margin,
            ),
            start,
            end,
            flags,
        )
    }
    return spannable
}

@PreviewsDayNight
@Composable
internal fun TimelineItemTextViewPreview(
    @PreviewParameter(TimelineItemTextBasedContentProvider::class) content: TimelineItemTextBasedContent
) = ElementPreview {
    TimelineItemTextView(
        content = content,
        onLinkClick = {},
        onLinkLongClick = {},
    )
}

@Preview
@Composable
internal fun TimelineItemTextViewWithLinkifiedUrlPreview() = ElementPreview {
    val content = aTimelineItemTextContent(
        formattedBody = LinkifyHelper.linkify("The link should end after the first '?' (url: github.com/element-hq/element-x-android/README?)?.")
    )
    TimelineItemTextView(
        content = content,
        onLinkClick = {},
        onLinkLongClick = {},
    )
}

@Preview
@Composable
internal fun TimelineItemTextViewWithLinkifiedUrlAndNestedParenthesisPreview() = ElementPreview {
    val content = aTimelineItemTextContent(
        formattedBody = LinkifyHelper.linkify("The link should end after the '(ME)' ((url: github.com/element-hq/element-x-android/READ(ME)))!")
    )
    TimelineItemTextView(
        content = content,
        onLinkClick = {},
        onLinkLongClick = {},
    )
}
