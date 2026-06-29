/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.libraries.roomselect.impl

import android.icu.text.ListFormatter
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.element.android.libraries.designsystem.theme.components.SearchBarResultState
import io.element.android.libraries.matrix.api.core.RoomAlias
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.ui.components.aSelectRoomInfo
import io.element.android.libraries.roomselect.api.RoomSelectMode
import io.element.android.libraries.ui.strings.CommonPlurals
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class RoomSelectViewTest {
    @Test
    fun `forward room rows show iOS-like alias and hero descriptions`() = runAndroidComposeUiTest {
        val alias = RoomAlias("#briefing:example.org")
        val aliasRoom = aSelectRoomInfo(
            roomId = RoomId("!alias:example.org"),
            name = "Briefing",
            canonicalAlias = alias,
        )
        val heroRoom = aSelectRoomInfo(
            roomId = RoomId("!heroes:example.org"),
            name = "Product",
            heroes = persistentListOf(
                MatrixUser(UserId("@alice:example.org"), displayName = "Alice"),
                MatrixUser(UserId("@bob:example.org"), displayName = "Bob"),
            ),
            activeMembersCount = 5,
        )

        setRoomSelectView(
            aRoomSelectState(
                mode = RoomSelectMode.Forward,
                resultState = SearchBarResultState.Results(persistentListOf(aliasRoom, heroRoom)),
            )
        )

        onNodeWithText(alias.value).assertExists()

        val others = activity!!.resources.getQuantityString(CommonPlurals.common_many_members, 3, 3)
        val heroDescription = ListFormatter.getInstance(Locale.getDefault()).format(listOf("Alice", "Bob", others))
        onNodeWithText(heroDescription).assertExists()
    }
}

private fun AndroidComposeUiTest<ComponentActivity>.setRoomSelectView(
    state: RoomSelectState,
) {
    setContent {
        RoomSelectView(
            state = state,
            onDismiss = {},
            onSubmit = { _, _ -> },
        )
    }
}
