/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.user.username

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.user.MatrixProfileUsernameException
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.test.A_USER_ID
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.matrix.ui.components.aMatrixUser
import io.element.android.tests.testutils.WarmUpRule
import io.element.android.tests.testutils.consumeItemsUntilPredicate
import io.element.android.tests.testutils.lambda.lambdaRecorder
import io.element.android.tests.testutils.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MomentUsernamePresenterTest {
    @get:Rule
    val warmUpRule = WarmUpRule()

    private fun createPresenter(
        matrixClient: MatrixClient = FakeMatrixClient(),
        matrixUser: MatrixUser = aMatrixUser(),
        navigator: MomentUsernameNavigator = FakeMomentUsernameNavigator(),
    ): MomentUsernamePresenter {
        return MomentUsernamePresenter(
            matrixClient = matrixClient,
            matrixUser = matrixUser,
            navigator = navigator,
        )
    }

    @Test
    fun `present - loads profile username`() = runTest {
        val matrixClient = FakeMatrixClient().apply {
            givenProfileUsername("alice")
        }
        val presenter = createPresenter(matrixClient = matrixClient)

        presenter.test {
            awaitItem()
            val loadedState = consumeItemsUntilPredicate { it.username == "alice" }.last()
            assertThat(loadedState.currentUsername).isEqualTo("alice")
            assertThat(loadedState.canSave).isFalse()
        }
    }

    @Test
    fun `present - normalizes and saves changed profile username`() = runTest {
        val matrixClient = FakeMatrixClient().apply {
            givenProfileUsername("alice")
        }
        val closeLambda = lambdaRecorder<Unit> {}
        val presenter = createPresenter(
            matrixClient = matrixClient,
            matrixUser = aMatrixUser(id = A_USER_ID.value, displayName = "Alice"),
            navigator = FakeMomentUsernameNavigator(closeLambda),
        )

        presenter.test {
            awaitItem()
            val loadedState = consumeItemsUntilPredicate { it.username == "alice" }.last()
            loadedState.eventSink(MomentUsernameEvent.UpdateUsername("@Bob_User"))
            val updatedState = consumeItemsUntilPredicate { it.username == "bob_user" }.last()
            assertThat(updatedState.canSave).isTrue()

            updatedState.eventSink(MomentUsernameEvent.Save)
            consumeItemsUntilPredicate { matrixClient.setProfileUsernameCalled }
            assertThat(matrixClient.getProfileUsername(A_USER_ID).getOrThrow()).isEqualTo("bob_user")
            closeLambda.assertions().isCalledOnce()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - shows validation error from server`() = runTest {
        val matrixClient = FakeMatrixClient().apply {
            givenProfileUsername("alice")
            givenSetProfileUsernameResult(Result.failure(MatrixProfileUsernameException.Taken))
        }
        val closeLambda = lambdaRecorder<Unit> {}
        val presenter = createPresenter(
            matrixClient = matrixClient,
            navigator = FakeMomentUsernameNavigator(closeLambda),
        )

        presenter.test {
            awaitItem()
            val loadedState = consumeItemsUntilPredicate { it.username == "alice" }.last()
            loadedState.eventSink(MomentUsernameEvent.UpdateUsername("bob"))
            val updatedState = consumeItemsUntilPredicate { it.canSave }.last()
            updatedState.eventSink(MomentUsernameEvent.Save)
            val errorState = consumeItemsUntilPredicate { it.usernameError == MomentUsernameError.Taken }.last()
            assertThat(errorState.canSave).isFalse()
            closeLambda.assertions().isNeverCalled()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - closes on close event`() = runTest {
        val closeLambda = lambdaRecorder<Unit> {}
        val presenter = createPresenter(navigator = FakeMomentUsernameNavigator(closeLambda))

        presenter.test {
            val initialState = awaitItem()
            initialState.eventSink(MomentUsernameEvent.Close)
            closeLambda.assertions().isCalledOnce()
        }
    }
}

private class FakeMomentUsernameNavigator(
    private val closeLambda: () -> Unit = {},
) : MomentUsernameNavigator {
    override fun close() = closeLambda()
}
