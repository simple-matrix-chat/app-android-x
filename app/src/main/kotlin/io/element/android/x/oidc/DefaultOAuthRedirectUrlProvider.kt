/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.oidc

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.appconfig.AuthenticationConfig
import io.element.android.libraries.matrix.api.auth.OAuthRedirectUrlProvider

@ContributesBinding(AppScope::class)
class DefaultOAuthRedirectUrlProvider : OAuthRedirectUrlProvider {
    override fun provide() = AuthenticationConfig.WBID_REDIRECT_URI
}
