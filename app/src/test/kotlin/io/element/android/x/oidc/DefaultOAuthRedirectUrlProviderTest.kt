/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.oidc

import com.google.common.truth.Truth.assertThat
import io.element.android.appconfig.AuthenticationConfig
import org.junit.Test

class DefaultOAuthRedirectUrlProviderTest {
    @Test
    fun `test provide`() {
        val sut = DefaultOAuthRedirectUrlProvider()
        val result = sut.provide()
        assertThat(result).isEqualTo(AuthenticationConfig.WBID_REDIRECT_URI)
    }
}
