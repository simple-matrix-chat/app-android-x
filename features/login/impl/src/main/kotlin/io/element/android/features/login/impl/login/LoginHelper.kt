/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import dev.zacsweers.metro.Inject
import io.element.android.appconfig.AuthenticationConfig
import io.element.android.features.login.impl.error.ChangeServerError
import io.element.android.features.login.impl.moment.MomentAuthenticationService
import io.element.android.features.login.impl.screens.chooseaccountprovider.ChooseAccountProviderPresenter
import io.element.android.features.login.impl.screens.confirmaccountprovider.ConfirmAccountProviderPresenter
import io.element.android.features.login.impl.screens.createaccount.AccountCreationNotSupported
import io.element.android.features.login.impl.screens.onboarding.OnBoardingPresenter
import io.element.android.features.login.impl.web.WebClientUrlForAuthenticationRetriever
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.architecture.runCatchingUpdatingState
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.matrix.api.auth.MatrixAuthenticationService
import io.element.android.libraries.matrix.api.auth.OAuthPrompt
import io.element.android.libraries.oauth.api.OAuthAction
import io.element.android.libraries.oauth.api.OAuthActionFlow

/**
 * This class is responsible for managing the login flow, including handling OIDC actions and
 * submitting login requests.
 * It's a helper to avoid code duplication. It is used by [OnBoardingPresenter], [ConfirmAccountProviderPresenter]
 * and [ChooseAccountProviderPresenter].
 */
@Inject
class LoginHelper(
    private val oAuthActionFlow: OAuthActionFlow,
    private val authenticationService: MatrixAuthenticationService,
    private val momentAuthenticationService: MomentAuthenticationService,
    private val webClientUrlForAuthenticationRetriever: WebClientUrlForAuthenticationRetriever,
) {
    private val loginModeState: MutableState<AsyncData<LoginMode>> = mutableStateOf(AsyncData.Uninitialized)

    @Composable
    fun collectLoginMode(): State<AsyncData<LoginMode>> {
        LaunchedEffect(Unit) {
            oAuthActionFlow.collect { oAuthAction ->
                if (oAuthAction != null) {
                    onOAuthAction(oAuthAction)
                }
            }
        }
        return loginModeState
    }

    fun clearError() {
        loginModeState.value = AsyncData.Uninitialized
    }

    suspend fun submit(
        isAccountCreation: Boolean,
        homeserverUrl: String,
        resolvedHomeserverUrl: String?,
        loginHint: String?,
    ) {
        suspend {
            val matrixHomeServerDetails = setHomeserver(homeserverUrl, resolvedHomeserverUrl)
            if (momentAuthenticationService.isMomentHomeserver(homeserverUrl)) {
                LoginMode.OAuth(
                    momentAuthenticationService.getOAuthDetails().getOrThrow()
                )
            } else {
                if (matrixHomeServerDetails.supportsOAuthLogin) {
                    // Retrieve the details right now
                    val oAuthPrompt = if (isAccountCreation) OAuthPrompt.Create else OAuthPrompt.Login
                    LoginMode.OAuth(
                        authenticationService.getOAuthUrl(prompt = oAuthPrompt, loginHint = loginHint).getOrThrow()
                    )
                } else if (isAccountCreation) {
                    val url = webClientUrlForAuthenticationRetriever.retrieve(homeserverUrl)
                    LoginMode.AccountCreation(url)
                } else if (matrixHomeServerDetails.supportsPasswordLogin) {
                    LoginMode.PasswordLogin
                } else {
                    error("Unsupported login flow")
                }
            }
        }.runCatchingUpdatingState(
            state = loginModeState,
            errorTransform = {
                when (it) {
                    is AccountCreationNotSupported -> it
                    else -> ChangeServerError.from(it)
                }
            }
        )
    }

    private suspend fun onOAuthAction(oAuthAction: OAuthAction) {
        if (oAuthAction is OAuthAction.GoBack && oAuthAction.toUnblock && loginModeState.value !is AsyncData.Loading) {
            // Ignore GoBack action if the current state is not Loading. This GoBack action is coming from LoginFlowNode.
            // This can happen if there is an error, for instance attempt to login again on the same account.
            return
        }
        loginModeState.value = AsyncData.Loading()
        when (oAuthAction) {
            is OAuthAction.GoBack -> {
                authenticationService.cancelOAuthLogin()
                    .onSuccess {
                        loginModeState.value = AsyncData.Uninitialized
                    }
                    .onFailure { failure ->
                        loginModeState.value = AsyncData.Failure(failure)
                    }
            }
            is OAuthAction.Success -> {
                if (momentAuthenticationService.isMomentCallbackUrl(oAuthAction.url)) {
                    loginWithMomentBff(oAuthAction.url)
                } else {
                    authenticationService.loginWithOAuth(oAuthAction.url)
                }
                    .onFailure { failure ->
                        loginModeState.value = AsyncData.Failure(failure)
                    }
            }
        }
        oAuthActionFlow.reset()
    }

    private suspend fun setHomeserver(
        homeserverUrl: String,
        resolvedHomeserverUrl: String?,
    ) = authenticationService.setHomeserver(homeserverUrl).recoverCatching {
        // No .well-known file?
        // If the homeserver is not reachable, try using resolvedHomeserverUrl.
        if (resolvedHomeserverUrl != null && resolvedHomeserverUrl != homeserverUrl) {
            authenticationService.setHomeserver(resolvedHomeserverUrl).getOrThrow()
        } else {
            throw it
        }
    }.getOrThrow()

    private suspend fun loginWithMomentBff(callbackUrl: String) = runCatchingExceptions {
        authenticationService.setHomeserver(AuthenticationConfig.DEFAULT_HOMESERVER_URL).getOrThrow()
        val externalSession = momentAuthenticationService.getExternalSession(callbackUrl).getOrThrow()
        authenticationService.importCreatedSession(externalSession).getOrThrow()
    }
}
