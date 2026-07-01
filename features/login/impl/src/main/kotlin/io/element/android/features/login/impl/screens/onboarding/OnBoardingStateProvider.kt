/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.features.login.impl.login.LoginMode
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.R

open class OnBoardingStateProvider : PreviewParameterProvider<OnBoardingState> {
    override val values: Sequence<OnBoardingState>
        get() = sequenceOf(
            anOnBoardingState(),
            anOnBoardingState(canCreateAccount = true),
            anOnBoardingState(canCreateAccount = true, canReportBug = true),
            anOnBoardingState(defaultAccountProvider = "element.io", canCreateAccount = false, canReportBug = true),
            anOnBoardingState(customLogoResId = R.drawable.sample_background),
            anOnBoardingState(
                isAddingAccount = true,
                canCreateAccount = true,
            ),
            anOnBoardingState(
                showBackButton = true,
                showDeveloperSettings = true,
            ),
            anOnBoardingState(
                productionApplicationName = "Moment",
                defaultAccountProvider = "https://unmoment.app",
                usesMomentWbIdAuthentication = true,
            ),
        )
}

fun anOnBoardingState(
    isAddingAccount: Boolean = false,
    showBackButton: Boolean = false,
    showDeveloperSettings: Boolean = false,
    productionApplicationName: String = "Element",
    defaultAccountProvider: String? = null,
    mustChooseAccountProvider: Boolean = false,
    canCreateAccount: Boolean = false,
    canReportBug: Boolean = false,
    usesMomentWbIdAuthentication: Boolean = false,
    version: String = "1.0.0",
    @DrawableRes
    customLogoResId: Int? = null,
    loginMode: AsyncData<LoginMode> = AsyncData.Uninitialized,
    eventSink: (OnBoardingEvents) -> Unit = {},
) = OnBoardingState(
    isAddingAccount = isAddingAccount,
    showBackButton = showBackButton,
    showDeveloperSettings = showDeveloperSettings,
    productionApplicationName = productionApplicationName,
    defaultAccountProvider = defaultAccountProvider,
    mustChooseAccountProvider = mustChooseAccountProvider,
    canCreateAccount = canCreateAccount,
    canReportBug = canReportBug,
    usesMomentWbIdAuthentication = usesMomentWbIdAuthentication,
    version = version,
    loginMode = loginMode,
    onBoardingLogoResId = customLogoResId,
    eventSink = eventSink,
)
