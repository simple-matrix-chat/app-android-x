/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.ai

class FakeMomentAIService(
    private val transformTextResult: (text: String, mode: String) -> Result<String> = { text, _ -> Result.success(text) },
    private val quickRewriteResult: (text: String) -> Result<String> = { text -> Result.success(text) },
) : MomentAIService {
    override suspend fun transformText(text: String, mode: String): Result<String> {
        return transformTextResult(text, mode)
    }

    override suspend fun quickRewrite(text: String): Result<String> {
        return quickRewriteResult(text)
    }
}
