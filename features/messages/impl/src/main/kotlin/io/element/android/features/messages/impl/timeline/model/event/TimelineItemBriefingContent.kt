/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.model.event

import io.element.android.features.ai.api.MomentAIBriefingPayload
import org.jsoup.nodes.Document

data class TimelineItemBriefingContent(
    override val body: String,
    override val formattedBody: CharSequence,
    override val isEdited: Boolean,
    val payload: MomentAIBriefingPayload,
) : TimelineItemTextBasedContent {
    override val type: String = "TimelineItemBriefingContent"
    override val htmlDocument: Document? = null
    override val plainText: String = body
}
