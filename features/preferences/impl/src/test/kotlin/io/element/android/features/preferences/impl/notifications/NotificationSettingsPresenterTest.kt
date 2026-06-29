/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.notifications

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.fullscreenintent.api.FullScreenIntentPermissionsState
import io.element.android.libraries.fullscreenintent.api.aFullScreenIntentPermissionsState
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.room.RoomNotificationMode
import io.element.android.libraries.matrix.test.AN_EXCEPTION
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.matrix.test.notificationsettings.FakeNotificationSettingsService
import io.element.android.libraries.push.api.PushService
import io.element.android.libraries.push.test.FakePushService
import io.element.android.libraries.pushproviders.api.Distributor
import io.element.android.libraries.pushproviders.api.PushProvider
import io.element.android.libraries.pushproviders.test.FakePushProvider
import io.element.android.libraries.pushstore.test.userpushstore.FakeUserPushStoreFactory
import io.element.android.tests.testutils.awaitLastSequentialItem
import io.element.android.tests.testutils.consumeItemsUntilPredicate
import io.element.android.tests.testutils.lambda.lambdaRecorder
import io.element.android.tests.testutils.test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NotificationSettingsPresenterTest {
    @Test
    fun `present - ensures initial state is correct`() = runTest {
        val presenter = createNotificationSettingsPresenter()
        presenter.test {
            val initialState = awaitItem()
            assertThat(initialState.appSettings.appNotificationsEnabled).isFalse()
            assertThat(initialState.appSettings.systemNotificationsEnabled).isTrue()
            assertThat(initialState.matrixSettings).isEqualTo(NotificationSettingsState.MatrixSettings.Uninitialized)
            val loadedState = consumeItemsUntilPredicate {
                it.matrixSettings is NotificationSettingsState.MatrixSettings.Valid
            }.last()
            assertThat(loadedState.appSettings.appNotificationsEnabled).isTrue()
            assertThat(loadedState.appSettings.systemNotificationsEnabled).isTrue()
            val valid = loadedState.matrixSettings as? NotificationSettingsState.MatrixSettings.Valid
            assertThat(valid?.atRoomNotificationsEnabled).isFalse()
            assertThat(valid?.callNotificationsEnabled).isFalse()
            assertThat(valid?.inviteForMeNotificationsEnabled).isFalse()
            assertThat(valid?.defaultGroupNotificationMode).isEqualTo(RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY)
            assertThat(valid?.defaultOneToOneNotificationMode).isEqualTo(RoomNotificationMode.ALL_MESSAGES)
            assertThat(valid?.inconsistentSettings).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - inconsistent encrypted and unencrypted defaults fallback to all messages`() = runTest {
        val notificationSettingsService = FakeNotificationSettingsService(
            initialGroupDefaultMode = RoomNotificationMode.ALL_MESSAGES,
            initialEncryptedGroupDefaultMode = RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY,
        )
        val presenter = createNotificationSettingsPresenter(notificationSettingsService)
        presenter.test {
            val loadedState = consumeItemsUntilPredicate {
                (it.matrixSettings as? NotificationSettingsState.MatrixSettings.Valid)?.inconsistentSettings?.isNotEmpty() == true
            }.last()
            val valid = loadedState.matrixSettings as? NotificationSettingsState.MatrixSettings.Valid
            assertThat(valid?.defaultGroupNotificationMode).isEqualTo(RoomNotificationMode.ALL_MESSAGES)
            assertThat(valid?.inconsistentSettings).containsExactly(
                NotificationSettingsState.InconsistentSetting(
                    isOneToOne = false,
                    isEncrypted = true,
                )
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - fix configuration mismatch sets inconsistent default modes to all messages`() = runTest {
        val notificationSettingsService = FakeNotificationSettingsService(
            initialGroupDefaultMode = RoomNotificationMode.ALL_MESSAGES,
            initialEncryptedGroupDefaultMode = RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY,
        )
        val presenter = createNotificationSettingsPresenter(notificationSettingsService)
        presenter.test {
            val loadedState = consumeItemsUntilPredicate {
                (it.matrixSettings as? NotificationSettingsState.MatrixSettings.Valid)?.inconsistentSettings?.isNotEmpty() == true
            }.last()
            loadedState.eventSink(NotificationSettingsEvents.FixConfigurationMismatch)
            val fixedState = consumeItemsUntilPredicate {
                val valid = it.matrixSettings as? NotificationSettingsState.MatrixSettings.Valid
                valid != null && valid.inconsistentSettings.isEmpty() && valid.defaultGroupNotificationMode == RoomNotificationMode.ALL_MESSAGES
            }.last()
            val valid = fixedState.matrixSettings as? NotificationSettingsState.MatrixSettings.Valid
            assertThat(valid?.inconsistentSettings).isEmpty()
            assertThat(notificationSettingsService.setDefaultRoomNotificationModeRequests).contains(
                FakeNotificationSettingsService.SetDefaultRoomNotificationModeRequest(
                    isEncrypted = true,
                    mode = RoomNotificationMode.ALL_MESSAGES,
                    isDm = false,
                )
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - default group notification mode changed`() = runTest {
        val notificationSettingsService = FakeNotificationSettingsService()
        val presenter = createNotificationSettingsPresenter(notificationSettingsService)
        presenter.test {
            notificationSettingsService.setDefaultRoomNotificationMode(isEncrypted = false, isDM = false, mode = RoomNotificationMode.ALL_MESSAGES)
            val updatedState = consumeItemsUntilPredicate {
                (it.matrixSettings as? NotificationSettingsState.MatrixSettings.Valid)
                    ?.defaultGroupNotificationMode == RoomNotificationMode.ALL_MESSAGES
            }.last()
            val valid = updatedState.matrixSettings as? NotificationSettingsState.MatrixSettings.Valid
            assertThat(valid?.defaultGroupNotificationMode).isEqualTo(RoomNotificationMode.ALL_MESSAGES)
        }
    }

    @Test
    fun `present - set notifications enabled`() = runTest {
        val unregisterWithResult = lambdaRecorder<MatrixClient, Result<Unit>> { Result.success(Unit) }
        val ensurePusherIsRegisteredResult = lambdaRecorder<Result<Unit>> { Result.success(Unit) }
        val presenter = createNotificationSettingsPresenter(
            pushService = FakePushService(
                currentPushProvider = {
                    FakePushProvider(
                        unregisterWithResult = unregisterWithResult,
                    )
                },
                ensurePusherIsRegisteredResult = ensurePusherIsRegisteredResult,
            )
        )
        presenter.test {
            val loadedState = consumeItemsUntilPredicate {
                it.matrixSettings is NotificationSettingsState.MatrixSettings.Valid
            }.last()
            assertThat(loadedState.appSettings.appNotificationsEnabled).isTrue()
            loadedState.eventSink(NotificationSettingsEvents.SetNotificationsEnabled(false))
            val updatedState = consumeItemsUntilPredicate {
                !it.appSettings.appNotificationsEnabled
            }.last()
            assertThat(updatedState.appSettings.appNotificationsEnabled).isFalse()
            unregisterWithResult.assertions().isCalledOnce()
            // Enable notification again
            loadedState.eventSink(NotificationSettingsEvents.SetNotificationsEnabled(true))
            val updatedState2 = consumeItemsUntilPredicate {
                it.appSettings.appNotificationsEnabled
            }.last()
            assertThat(updatedState2.appSettings.appNotificationsEnabled).isTrue()
            ensurePusherIsRegisteredResult.assertions().isCalledOnce()
        }
    }

    @Test
    fun `present - set call notifications enabled`() = runTest {
        val presenter = createNotificationSettingsPresenter()
        presenter.test {
            val loadedState = consumeItemsUntilPredicate {
                (it.matrixSettings as? NotificationSettingsState.MatrixSettings.Valid)?.callNotificationsEnabled == false
            }.last()
            val validMatrixState = loadedState.matrixSettings as? NotificationSettingsState.MatrixSettings.Valid
            assertThat(validMatrixState?.callNotificationsEnabled).isFalse()
            loadedState.eventSink(NotificationSettingsEvents.SetCallNotificationsEnabled(true))
            val updatedState = consumeItemsUntilPredicate {
                (it.matrixSettings as? NotificationSettingsState.MatrixSettings.Valid)?.callNotificationsEnabled == true
            }.last()
            val updatedMatrixState = updatedState.matrixSettings as? NotificationSettingsState.MatrixSettings.Valid
            assertThat(updatedMatrixState?.callNotificationsEnabled).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - set invite for me notifications enabled`() = runTest {
        val presenter = createNotificationSettingsPresenter()
        presenter.test {
            val loadedState = consumeItemsUntilPredicate {
                (it.matrixSettings as? NotificationSettingsState.MatrixSettings.Valid)?.inviteForMeNotificationsEnabled == false
            }.last()
            val validMatrixState = loadedState.matrixSettings as? NotificationSettingsState.MatrixSettings.Valid
            assertThat(validMatrixState?.inviteForMeNotificationsEnabled).isFalse()
            loadedState.eventSink(NotificationSettingsEvents.SetInviteForMeNotificationsEnabled(true))
            val updatedState = consumeItemsUntilPredicate {
                (it.matrixSettings as? NotificationSettingsState.MatrixSettings.Valid)?.inviteForMeNotificationsEnabled == true
            }.last()
            val updatedMatrixState = updatedState.matrixSettings as? NotificationSettingsState.MatrixSettings.Valid
            assertThat(updatedMatrixState?.inviteForMeNotificationsEnabled).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - set atRoom notifications enabled`() = runTest {
        val presenter = createNotificationSettingsPresenter()
        presenter.test {
            val loadedState = consumeItemsUntilPredicate {
                (it.matrixSettings as? NotificationSettingsState.MatrixSettings.Valid)?.atRoomNotificationsEnabled == false
            }.last()
            val validMatrixState = loadedState.matrixSettings as? NotificationSettingsState.MatrixSettings.Valid
            assertThat(validMatrixState?.atRoomNotificationsEnabled).isFalse()
            loadedState.eventSink(NotificationSettingsEvents.SetAtRoomNotificationsEnabled(true))
            val updatedState = consumeItemsUntilPredicate {
                (it.matrixSettings as? NotificationSettingsState.MatrixSettings.Valid)?.atRoomNotificationsEnabled == true
            }.last()
            val updatedMatrixState = updatedState.matrixSettings as? NotificationSettingsState.MatrixSettings.Valid
            assertThat(updatedMatrixState?.atRoomNotificationsEnabled).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - clear notification settings change error`() = runTest {
        val notificationSettingsService = FakeNotificationSettingsService()
        val presenter = createNotificationSettingsPresenter(notificationSettingsService)
        notificationSettingsService.givenSetAtRoomError(AN_EXCEPTION)
        presenter.test {
            val loadedState = consumeItemsUntilPredicate {
                (it.matrixSettings as? NotificationSettingsState.MatrixSettings.Valid)?.atRoomNotificationsEnabled == false
            }.last()
            val validMatrixState = loadedState.matrixSettings as? NotificationSettingsState.MatrixSettings.Valid
            assertThat(validMatrixState?.atRoomNotificationsEnabled).isFalse()
            loadedState.eventSink(NotificationSettingsEvents.SetAtRoomNotificationsEnabled(true))
            val errorState = consumeItemsUntilPredicate {
                it.changeNotificationSettingAction.isFailure()
            }.last()
            assertThat(errorState.changeNotificationSettingAction.isFailure()).isTrue()
            errorState.eventSink(NotificationSettingsEvents.ClearNotificationChangeError)
            val clearErrorState = consumeItemsUntilPredicate {
                it.changeNotificationSettingAction.isUninitialized()
            }.last()
            assertThat(clearErrorState.changeNotificationSettingAction.isUninitialized()).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - change push provider`() = runTest {
        val presenter = createNotificationSettingsPresenter(
            pushService = createFakePushService(),
        )
        presenter.test {
            val initialState = awaitLastSequentialItem()
            assertThat(initialState.currentPushDistributor).isEqualTo(AsyncData.Success(Distributor(value = "aDistributorValue0", name = "aDistributorName0")))
            assertThat(initialState.availablePushDistributors).containsExactly(
                Distributor(value = "aDistributorValue0", name = "aDistributorName0"),
                Distributor(value = "aDistributorValue1", name = "aDistributorName1"),
            )
            initialState.eventSink.invoke(NotificationSettingsEvents.ChangePushProvider)
            val withDialog = awaitItem()
            assertThat(withDialog.showChangePushProviderDialog).isTrue()
            // Cancel
            withDialog.eventSink(NotificationSettingsEvents.CancelChangePushProvider)
            val withoutDialog = awaitItem()
            assertThat(withoutDialog.showChangePushProviderDialog).isFalse()
            withDialog.eventSink.invoke(NotificationSettingsEvents.ChangePushProvider)
            assertThat(awaitItem().showChangePushProviderDialog).isTrue()
            withDialog.eventSink(NotificationSettingsEvents.SetPushProvider(1))
            val withNewProvider = awaitItem()
            assertThat(withNewProvider.showChangePushProviderDialog).isFalse()
            assertThat(withNewProvider.currentPushDistributor).isInstanceOf(AsyncData.Loading::class.java)
            skipItems(1)
            val lastItem = awaitItem()
            assertThat(lastItem.currentPushDistributor).isEqualTo(AsyncData.Success(Distributor(value = "aDistributorValue1", name = "aDistributorName1")))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - change push provider to the same value is no op`() = runTest {
        val presenter = createNotificationSettingsPresenter(
            pushService = createFakePushService(),
        )
        presenter.test {
            val initialState = awaitLastSequentialItem()
            assertThat(initialState.currentPushDistributor).isEqualTo(AsyncData.Success(Distributor(value = "aDistributorValue0", name = "aDistributorName0")))
            assertThat(initialState.availablePushDistributors).containsExactly(
                Distributor(value = "aDistributorValue0", name = "aDistributorName0"),
                Distributor(value = "aDistributorValue1", name = "aDistributorName1"),
            )
            initialState.eventSink.invoke(NotificationSettingsEvents.ChangePushProvider)
            assertThat(awaitItem().showChangePushProviderDialog).isTrue()
            // Choose the same value (index 0)
            initialState.eventSink(NotificationSettingsEvents.SetPushProvider(0))
            val withNewProvider = awaitItem()
            assertThat(withNewProvider.showChangePushProviderDialog).isFalse()
            expectNoEvents()
        }
    }

    @Test
    fun `present - RefreshSystemNotificationsEnabled also refreshes fullScreenIntentState`() = runTest {
        var lambdaResult = aFullScreenIntentPermissionsState(permissionGranted = false)
        val fullScreenIntentPermissionsStateLambda = { lambdaResult }
        val presenter = createNotificationSettingsPresenter(
            pushService = createFakePushService(),
            fullScreenIntentPermissionsStateLambda = fullScreenIntentPermissionsStateLambda,
        )
        presenter.test {
            val initialState = awaitLastSequentialItem()
            assertThat(initialState.fullScreenIntentPermissionsState.permissionGranted).isFalse()

            // Change the notification settings
            lambdaResult = lambdaResult.copy(permissionGranted = true)
            // Check it's not changed unless we refresh
            expectNoEvents()

            // Refresh
            initialState.eventSink.invoke(NotificationSettingsEvents.RefreshSystemNotificationsEnabled)
            assertThat(awaitItem().fullScreenIntentPermissionsState.permissionGranted).isTrue()
        }
    }

    @Test
    fun `present - change push provider error`() = runTest {
        val presenter = createNotificationSettingsPresenter(
            pushService = createFakePushService(
                registerWithLambda = { _, _, _ ->
                    Result.failure(Exception("An error"))
                },
            ),
        )
        presenter.test {
            val initialState = awaitLastSequentialItem()
            initialState.eventSink.invoke(NotificationSettingsEvents.ChangePushProvider)
            val withDialog = awaitItem()
            assertThat(withDialog.showChangePushProviderDialog).isTrue()
            withDialog.eventSink(NotificationSettingsEvents.SetPushProvider(1))
            val withNewProvider = awaitItem()
            assertThat(withNewProvider.showChangePushProviderDialog).isFalse()
            assertThat(withNewProvider.currentPushDistributor).isInstanceOf(AsyncData.Loading::class.java)
            val lastItem = awaitItem()
            assertThat(lastItem.currentPushDistributor).isInstanceOf(AsyncData.Failure::class.java)
        }
    }

    private fun createFakePushService(
        registerWithLambda: (MatrixClient, PushProvider, Distributor) -> Result<Unit> = { _, _, _ ->
            Result.success(Unit)
        }
    ): PushService {
        val pushProvider1 = FakePushProvider(
            index = 0,
            name = "aFakePushProvider0",
            distributors = listOf(Distributor("aDistributorValue0", "aDistributorName0")),
        )
        val pushProvider2 = FakePushProvider(
            index = 1,
            name = "aFakePushProvider1",
            distributors = listOf(Distributor("aDistributorValue1", "aDistributorName1")),
        )
        return FakePushService(
            availablePushProviders = listOf(pushProvider1, pushProvider2),
            registerWithLambda = registerWithLambda,
        )
    }

    private fun TestScope.createNotificationSettingsPresenter(
        notificationSettingsService: FakeNotificationSettingsService = FakeNotificationSettingsService(),
        pushService: PushService = FakePushService(),
        fullScreenIntentPermissionsStateLambda: () -> FullScreenIntentPermissionsState = { aFullScreenIntentPermissionsState() },
    ): NotificationSettingsPresenter {
        val matrixClient = FakeMatrixClient(notificationSettingsService = notificationSettingsService)
        return NotificationSettingsPresenter(
            notificationSettingsService = notificationSettingsService,
            userPushStoreFactory = FakeUserPushStoreFactory(),
            matrixClient = matrixClient,
            pushService = pushService,
            systemNotificationsEnabledProvider = FakeSystemNotificationsEnabledProvider(),
            fullScreenIntentPermissionsPresenter = { fullScreenIntentPermissionsStateLambda() },
            sessionCoroutineScope = backgroundScope,
        )
    }
}
