/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.privacy

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarDispatcher
import io.element.android.libraries.matrix.api.privacy.MatrixMomentPrivacyAccess
import io.element.android.libraries.matrix.api.privacy.MatrixMomentPrivacySettings
import io.element.android.libraries.matrix.api.privacy.MatrixMomentVisibilityAccess
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.tests.testutils.consumeItemsUntilPredicate
import io.element.android.tests.testutils.test
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val MOMENT_PRIVACY_ACCOUNT_DATA_TYPE = "io.moment.privacy"

class MomentPrivacySettingsPresenterTest {
    @Test
    fun `present - uses default settings when account data is missing`() = runTest {
        val presenter = createPresenter()

        presenter.test {
            val loadedState = consumeItemsUntilPredicate { !it.isLoading }.last()

            assertThat(loadedState.settings).isEqualTo(MomentPrivacySettings.Default)
            assertThat(loadedState.rowsEnabled).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - loads settings from account data`() = runTest {
        val matrixClient = FakeMatrixClient().apply {
            givenAccountData(
                eventType = MOMENT_PRIVACY_ACCOUNT_DATA_TYPE,
                content = """
                    {
                      "directMessages": "contacts",
                      "groupInvites": "nobody",
                      "avatarVisibility": "contacts",
                      "phoneVisibility": "all",
                      "presenceVisibility": "contacts"
                    }
                """.trimIndent(),
            )
        }
        val presenter = createPresenter(matrixClient)

        presenter.test {
            val loadedState = consumeItemsUntilPredicate { !it.isLoading }.last()

            assertThat(loadedState.settings).isEqualTo(
                MomentPrivacySettings(
                    directMessages = MomentPrivacyAccess.ContactsOnly,
                    groupInvites = MomentPrivacyAccess.Nobody,
                    avatarVisibility = MomentVisibilityAccess.ContactsOnly,
                    phoneVisibility = MomentPrivacyAccess.Everyone,
                    presenceVisibility = MomentVisibilityAccess.ContactsOnly,
                )
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - saves selected settings to account data`() = runTest {
        val matrixClient = FakeMatrixClient()
        val presenter = createPresenter(matrixClient)

        presenter.test {
            val loadedState = consumeItemsUntilPredicate { !it.isLoading }.last()
            loadedState.eventSink(MomentPrivacySettingsEvent.SelectPhoneVisibility(MomentPrivacyAccess.Nobody))

            val savedState = consumeItemsUntilPredicate {
                matrixClient.latestSyncedMomentPrivacySettings != null &&
                    it.savingSection == null &&
                    it.settings.phoneVisibility == MomentPrivacyAccess.Nobody
            }.last()
            val savedContent = matrixClient.getAccountData(MOMENT_PRIVACY_ACCOUNT_DATA_TYPE).getOrThrow()

            assertThat(savedState.settings.phoneVisibility).isEqualTo(MomentPrivacyAccess.Nobody)
            assertThat(MomentPrivacySettings.fromJson(savedContent).phoneVisibility).isEqualTo(MomentPrivacyAccess.Nobody)
            assertThat(matrixClient.latestSyncedMomentPrivacySettings).isEqualTo(
                MatrixMomentPrivacySettings(
                    directMessages = MatrixMomentPrivacyAccess.Everyone,
                    groupInvites = MatrixMomentPrivacyAccess.Everyone,
                    avatarVisibility = MatrixMomentVisibilityAccess.Everyone,
                    phoneVisibility = MatrixMomentPrivacyAccess.Nobody,
                    presenceVisibility = MatrixMomentVisibilityAccess.Everyone,
                )
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - keeps saved settings when best-effort sync fails`() = runTest {
        val matrixClient = FakeMatrixClient().apply {
            givenSyncMomentPrivacySettingsResult(Result.failure(RuntimeException("Sync failed")))
        }
        val presenter = createPresenter(matrixClient)

        presenter.test {
            val loadedState = consumeItemsUntilPredicate { !it.isLoading }.last()
            loadedState.eventSink(MomentPrivacySettingsEvent.SelectDirectMessages(MomentPrivacyAccess.ContactsOnly))

            val savedState = consumeItemsUntilPredicate {
                matrixClient.latestSyncedMomentPrivacySettings != null &&
                    it.savingSection == null &&
                    it.settings.directMessages == MomentPrivacyAccess.ContactsOnly
            }.last()
            val savedContent = matrixClient.getAccountData(MOMENT_PRIVACY_ACCOUNT_DATA_TYPE).getOrThrow()

            assertThat(savedState.settings.directMessages).isEqualTo(MomentPrivacyAccess.ContactsOnly)
            assertThat(MomentPrivacySettings.fromJson(savedContent).directMessages).isEqualTo(MomentPrivacyAccess.ContactsOnly)
            assertThat(matrixClient.latestSyncedMomentPrivacySettings?.directMessages).isEqualTo(MatrixMomentPrivacyAccess.ContactsOnly)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createPresenter(
        matrixClient: FakeMatrixClient = FakeMatrixClient(),
    ): MomentPrivacySettingsPresenter {
        return MomentPrivacySettingsPresenter(
            matrixClient = matrixClient,
            snackbarDispatcher = SnackbarDispatcher(),
        )
    }
}
