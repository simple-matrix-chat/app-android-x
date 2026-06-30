/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.roomdetailsedit.api

import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import io.element.android.libraries.architecture.FeatureEntryPoint

interface RoomDetailsEditEntryPoint : FeatureEntryPoint {
    interface Callback : Plugin {
        fun navigateToSecurityAndPrivacy()
        fun navigateToRolesAndPermissions()

        data object Noop : Callback {
            override fun navigateToSecurityAndPrivacy() = Unit
            override fun navigateToRolesAndPermissions() = Unit
        }
    }

    fun createNode(parentNode: Node, buildContext: BuildContext, callback: Callback): Node

    fun createNode(parentNode: Node, buildContext: BuildContext): Node {
        return createNode(parentNode, buildContext, Callback.Noop)
    }
}
