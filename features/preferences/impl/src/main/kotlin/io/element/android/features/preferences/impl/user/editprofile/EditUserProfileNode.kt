/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.user.editprofile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import io.element.android.annotations.ContributesNode
import io.element.android.libraries.androidutils.system.startSharePlainTextIntent
import io.element.android.libraries.architecture.NodeInputs
import io.element.android.libraries.architecture.callback
import io.element.android.libraries.architecture.inputs
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.libraries.androidutils.R as AndroidUtilsR

@ContributesNode(SessionScope::class)
@AssistedInject
class EditUserProfileNode(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    presenterFactory: EditUserProfilePresenter.Factory,
) : Node(buildContext, plugins = plugins),
    EditUserProfileNavigator {
    data class Inputs(
        val matrixUser: MatrixUser
    ) : NodeInputs

    interface Callback : Plugin {
        fun onDone()
        fun navigateToUsername(matrixUser: MatrixUser)
    }

    val matrixUser = inputs<Inputs>().matrixUser
    val callback: Callback = callback()
    val presenter = presenterFactory.create(
        matrixUser = matrixUser,
        navigator = this,
    )

    @Composable
    override fun View(modifier: Modifier) {
        val context = LocalContext.current
        val state = presenter.present()
        EditUserProfileView(
            state = state,
            onEditProfileSuccess = ::close,
            onEditUsername = { callback.navigateToUsername(matrixUser) },
            onShareProfile = { profileShareText ->
                context.startSharePlainTextIntent(
                    activityResultLauncher = null,
                    chooserTitle = context.getString(CommonStrings.action_share),
                    text = profileShareText,
                    noActivityFoundMessage = context.getString(AndroidUtilsR.string.error_no_compatible_app_found),
                )
            },
            modifier = modifier
        )
    }

    override fun close() = callback.onDone()
}
