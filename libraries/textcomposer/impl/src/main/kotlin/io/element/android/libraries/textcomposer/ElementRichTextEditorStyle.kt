/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.textcomposer

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.theme.bgSubtleTertiary
import io.element.android.wysiwyg.compose.RichTextEditorDefaults
import io.element.android.wysiwyg.compose.RichTextEditorStyle

object ElementRichTextEditorStyle {
    @Composable
    fun composerStyle(
        hasFocus: Boolean,
    ): RichTextEditorStyle {
        val baseStyle = common()
        return baseStyle.copy(
            text = baseStyle.text.copy(
                color = if (hasFocus) {
                    ElementTheme.colors.textPrimary
                } else {
                    ElementTheme.colors.textSecondary
                },
                placeholderColor = ElementTheme.colors.textSecondary,
                lineHeight = TextUnit.Unspecified,
                includeFontPadding = true,
            )
        )
    }

    @Composable
    fun textStyle(usesMomentTimelineStyle: Boolean = false): RichTextEditorStyle {
        return common(usesMomentTimelineStyle = usesMomentTimelineStyle)
    }

    @Composable
    private fun common(usesMomentTimelineStyle: Boolean = false): RichTextEditorStyle {
        val colors = ElementTheme.colors
        val codeCornerRadius = if (usesMomentTimelineStyle) 2.dp else 4.dp
        val codeBorderWidth = if (usesMomentTimelineStyle) 0.dp else 1.dp
        val codeBackgroundColor = colors.bgSubtleTertiary
        val codeBorderColor = if (usesMomentTimelineStyle) codeBackgroundColor else colors.borderInteractiveSecondary
        val codeRelativeTextSize = if (usesMomentTimelineStyle) 0.9f else 0.85f
        return RichTextEditorDefaults.style(
            bulletList = RichTextEditorDefaults.bulletListStyle(
                bulletGapWidth = if (usesMomentTimelineStyle) 6.dp else 8.dp,
            ),
            text = RichTextEditorDefaults.textStyle(
                color = LocalTextStyle.current.color.takeIf { it.isSpecified } ?: LocalContentColor.current,
                fontStyle = LocalTextStyle.current.fontStyle,
                lineHeight = LocalTextStyle.current.lineHeight,
                includeFontPadding = false,
            ),
            cursor = RichTextEditorDefaults.cursorStyle(
                color = colors.iconAccentTertiary,
            ),
            link = RichTextEditorDefaults.linkStyle(
                color = colors.textLinkExternal,
            ),
            codeBlock = RichTextEditorDefaults.codeBlockStyle(
                leadingMargin = if (usesMomentTimelineStyle) 6.dp else 8.dp,
                verticalPadding = if (usesMomentTimelineStyle) 4.dp else 8.dp,
                relativeTextSize = codeRelativeTextSize,
                background = RichTextEditorDefaults.codeBlockBackgroundStyle(
                    color = codeBackgroundColor,
                    borderColor = codeBorderColor,
                    cornerRadius = codeCornerRadius,
                    borderWidth = codeBorderWidth,
                )
            ),
            inlineCode = RichTextEditorDefaults.inlineCodeStyle(
                horizontalPadding = if (usesMomentTimelineStyle) 3.dp else 4.dp,
                verticalPadding = 0.dp,
                relativeTextSize = codeRelativeTextSize,
                background = RichTextEditorDefaults.inlineCodeBackgroundStyle(
                    color = codeBackgroundColor,
                    borderColor = codeBorderColor,
                    cornerRadius = codeCornerRadius,
                    borderWidth = codeBorderWidth,
                )
            ),
        )
    }
}
