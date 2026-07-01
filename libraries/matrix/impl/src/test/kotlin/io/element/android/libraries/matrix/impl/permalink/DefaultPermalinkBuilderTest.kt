/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.permalink

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.RoomAlias
import io.element.android.libraries.matrix.api.core.UserId
import org.junit.Test

class DefaultPermalinkBuilderTest {
    private val builder = DefaultPermalinkBuilder()

    @Test
    fun `permalinkForUser uses Moment web permalink base`() {
        assertThat(builder.permalinkForUser(UserId("@alice:unmoment.app")).getOrThrow())
            .isEqualTo("https://unmoment.app/#/@alice:unmoment.app")
    }

    @Test
    fun `permalinkForRoomAlias uses Moment web permalink base`() {
        assertThat(builder.permalinkForRoomAlias(RoomAlias("#general:unmoment.app")).getOrThrow())
            .isEqualTo("https://unmoment.app/#/#general:unmoment.app")
    }
}
