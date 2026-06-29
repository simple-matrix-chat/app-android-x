/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.media

data class MatrixStickerInfo(
    val height: Long?,
    val width: Long?,
    val mimetype: String?,
    val size: Long?,
    val thumbnailInfo: ThumbnailInfo?,
    val thumbnailUrl: String?,
    val blurhash: String?,
)
