/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.appnav

import com.bumble.appyx.core.navigation.NavKey
import com.bumble.appyx.core.navigation.Operation
import com.bumble.appyx.navmodel.backstack.BackStack
import com.bumble.appyx.navmodel.backstack.BackStack.State.ACTIVE
import com.bumble.appyx.navmodel.backstack.BackStack.State.STASHED
import com.bumble.appyx.navmodel.backstack.BackStackElement
import com.google.common.truth.Truth.assertThat
import io.element.android.features.preferences.api.PreferencesEntryPoint
import io.element.android.libraries.matrix.api.core.toRoomIdOrAlias
import io.element.android.libraries.matrix.test.A_ROOM_ID
import org.junit.Test

class LoggedInRootTabNavigationTest {
    @Test
    fun `switching to profile keeps chats stashed before active profile`() {
        val result = SwitchRootTabOperation(LoggedInRootTab.Profile).invoke(
            listOf(element(LoggedInFlowNode.NavTarget.Home, ACTIVE))
        )

        assertThat(result.map { it.key.navTarget }).containsExactly(
            LoggedInFlowNode.NavTarget.Home,
            LoggedInFlowNode.NavTarget.Settings(),
        ).inOrder()
        assertThat(result.map { it.targetState }).containsExactly(STASHED, ACTIVE).inOrder()
    }

    @Test
    fun `switching back to chats keeps profile stashed before active chats`() {
        val profileElements = SwitchRootTabOperation(LoggedInRootTab.Profile).invoke(
            listOf(element(LoggedInFlowNode.NavTarget.Home, ACTIVE))
        )

        val result = SwitchRootTabOperation(LoggedInRootTab.Chats).invoke(profileElements)

        assertThat(result.map { it.key.navTarget }).containsExactly(
            LoggedInFlowNode.NavTarget.Settings(),
            LoggedInFlowNode.NavTarget.Home,
        ).inOrder()
        assertThat(result.map { it.targetState }).containsExactly(STASHED, ACTIVE).inOrder()
    }

    @Test
    fun `back handler ignores active root tabs`() {
        val activeChats = listOf(
            element(LoggedInFlowNode.NavTarget.Settings(), STASHED),
            element(LoggedInFlowNode.NavTarget.Home, ACTIVE),
        )
        val activeProfile = listOf(
            element(LoggedInFlowNode.NavTarget.Home, STASHED),
            element(LoggedInFlowNode.NavTarget.Settings(), ACTIVE),
        )

        assertThat(LoggedInBackPressHandler.canHandleBackPress(activeChats)).isFalse()
        assertThat(LoggedInBackPressHandler.canHandleBackPress(activeProfile)).isFalse()
    }

    @Test
    fun `back handler still handles non root screens over root tabs`() {
        val activeRoom = listOf(
            element(LoggedInFlowNode.NavTarget.Settings(), STASHED),
            element(LoggedInFlowNode.NavTarget.Home, STASHED),
            element(LoggedInFlowNode.NavTarget.Room(A_ROOM_ID.toRoomIdOrAlias()), ACTIVE),
        )

        assertThat(LoggedInBackPressHandler.canHandleBackPress(activeRoom)).isTrue()
    }

    @Test
    fun `only the root settings target is treated as the profile tab`() {
        assertThat(LoggedInFlowNode.NavTarget.Settings().isRootTabTarget()).isTrue()
        assertThat(
            LoggedInFlowNode.NavTarget.Settings(
                initialElement = PreferencesEntryPoint.InitialTarget.NotificationSettings
            ).isRootTabTarget()
        ).isFalse()
    }

    private fun element(
        navTarget: LoggedInFlowNode.NavTarget,
        state: BackStack.State,
    ) = BackStackElement(
        key = NavKey(navTarget),
        fromState = state,
        targetState = state,
        operation = Operation.Noop()
    )
}
