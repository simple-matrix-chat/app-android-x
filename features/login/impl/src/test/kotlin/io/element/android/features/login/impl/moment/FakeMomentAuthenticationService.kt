/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.moment

import io.element.android.libraries.matrix.api.auth.OAuthDetails
import io.element.android.libraries.matrix.api.auth.external.ExternalSession

class FakeMomentAuthenticationService(
    var isMomentHomeserverResult: (String) -> Boolean = { false },
    var isMomentCallbackUrlResult: (String) -> Boolean = { false },
    var isMomentAuthorizationUrlResult: (String) -> Boolean = { false },
    var getOAuthDetailsResult: () -> Result<OAuthDetails> = { Result.success(OAuthDetails(url = "moment-auth-url")) },
    var getExternalSessionResult: (String) -> Result<ExternalSession> = {
        Result.success(
            ExternalSession(
                userId = "@alice:unmoment.app",
                deviceId = "DEVICE",
                accessToken = "access-token",
                refreshToken = null,
                homeserverUrl = "https://unmoment.app",
            )
        )
    },
) : MomentAuthenticationService {
    override fun isMomentHomeserver(url: String): Boolean {
        return isMomentHomeserverResult(url)
    }

    override fun isMomentCallbackUrl(url: String): Boolean {
        return isMomentCallbackUrlResult(url)
    }

    override fun isMomentAuthorizationUrl(url: String): Boolean {
        return isMomentAuthorizationUrlResult(url)
    }

    override suspend fun getOAuthDetails(): Result<OAuthDetails> {
        return getOAuthDetailsResult()
    }

    override suspend fun getExternalSession(callbackUrl: String): Result<ExternalSession> {
        return getExternalSessionResult(callbackUrl)
    }
}
