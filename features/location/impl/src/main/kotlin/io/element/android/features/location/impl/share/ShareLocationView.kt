/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package io.element.android.features.location.impl.share

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.location.api.Location
import io.element.android.features.location.api.internal.centerBottomEdge
import io.element.android.features.location.impl.R
import io.element.android.features.location.impl.common.MapDefaults
import io.element.android.features.location.impl.common.ui.LocationConstraintsDialog
import io.element.android.features.location.impl.common.ui.LocationFloatingActionButton
import io.element.android.features.location.impl.common.ui.MapBottomSheetScaffold
import io.element.android.features.location.impl.common.ui.UserLocationPuck
import io.element.android.features.location.impl.common.ui.rememberUserLocationState
import io.element.android.features.location.impl.share.ShareLocationEvent.StartLiveLocationShare
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.components.LocationPin
import io.element.android.libraries.designsystem.components.PinVariant
import io.element.android.libraries.designsystem.components.async.AsyncIndicator
import io.element.android.libraries.designsystem.components.async.AsyncIndicatorHost
import io.element.android.libraries.designsystem.components.async.rememberAsyncIndicatorState
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.components.dialogs.ConfirmationDialog
import io.element.android.libraries.designsystem.components.dialogs.ListDialog
import io.element.android.libraries.designsystem.components.list.RadioButtonListItem
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList
import org.maplibre.compose.camera.CameraMoveReason
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.location.UserLocationState
import kotlin.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareLocationView(
    state: ShareLocationState,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val dialogState = state.dialogState) {
        ShareLocationState.Dialog.None -> Unit
        is ShareLocationState.Dialog.Constraints -> LocationConstraintsDialog(
            state = dialogState.state,
            appName = state.appName,
            onRequestPermissions = { state.eventSink(ShareLocationEvent.RequestPermissions) },
            onOpenAppSettings = { state.eventSink(ShareLocationEvent.OpenAppSettings) },
            onOpenLocationSettings = { state.eventSink(ShareLocationEvent.OpenLocationSettings) },
            onDismiss = { state.eventSink(ShareLocationEvent.DismissDialog) },
        )
        ShareLocationState.Dialog.LiveLocationDisclaimer -> ConfirmationDialog(
            content = stringResource(R.string.screen_share_location_live_location_disclaimer_title),
            submitText = stringResource(CommonStrings.action_accept),
            cancelText = stringResource(CommonStrings.action_decline),
            onSubmitClick = { state.eventSink(ShareLocationEvent.AcceptLiveLocationDisclaimer) },
            onDismiss = { state.eventSink(ShareLocationEvent.DismissDialog) },
        )
        is ShareLocationState.Dialog.LiveLocationDurations -> LiveLocationDurationDialog(
            durations = dialogState.durations,
            onSelectDuration = { duration ->
                state.eventSink(StartLiveLocationShare(duration))
            },
            onDismiss = { state.eventSink(ShareLocationEvent.DismissDialog) },
        )
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.Expanded)
    )
    val cameraState = rememberCameraState(firstPosition = MapDefaults.defaultCameraPosition)
    val userLocationState = rememberUserLocationState(state.hasLocationPermission)

    LaunchedEffect(cameraState.isCameraMoving) {
        if (cameraState.moveReason == CameraMoveReason.GESTURE) {
            state.eventSink(ShareLocationEvent.StopTrackingUserLocation)
        }
    }

    MapBottomSheetScaffold(
        cameraState = cameraState,
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetDragHandle = null,
        sheetSwipeEnabled = false,
        topBar = {
            TopAppBar(
                titleStr = stringResource(CommonStrings.screen_share_location_title),
                navigationIcon = {
                    BackButton(onClick = navigateUp)
                },
            )
        },
        sheetContent = {
            BottomSheetContent(
                cameraState = cameraState,
                state = state,
                userLocationState = userLocationState,
                navigateUp = navigateUp
            )
        },
        mapContent = {
            UserLocationPuck(
                cameraState = cameraState,
                locationState = userLocationState,
                trackUserLocation = state.trackUserLocation
            )
        },
        overlayContent = { sheetPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(sheetPadding)
            ) {
                val variant = if (state.trackUserLocation) {
                    PinVariant.UserLocation(isLive = false, avatarData = state.currentUser.getAvatarData(AvatarSize.LocationPin))
                } else {
                    PinVariant.PinnedLocation
                }
                LocationPin(
                    variant = variant,
                    modifier = Modifier.centerBottomEdge(this),
                )
            }
            LocationFloatingActionButton(
                isMapCenteredOnUser = state.trackUserLocation,
                onClick = { state.eventSink(ShareLocationEvent.StartTrackingUserLocation) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(all = 16.dp),
            )
            StartLiveLocationActionView(state.startLiveLocationAction, navigateUp)
        }
    )
}

@Composable
private fun StartLiveLocationActionView(
    action: AsyncAction<Unit>,
    onActionSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        val asyncIndicatorState = rememberAsyncIndicatorState()
        AsyncIndicatorHost(state = asyncIndicatorState)

        when (action) {
            is AsyncAction.Loading -> {
                LaunchedEffect(action) {
                    asyncIndicatorState.enqueue {
                        AsyncIndicator.Loading(text = stringResource(CommonStrings.common_waiting_live_location))
                    }
                }
            }
            is AsyncAction.Failure -> {
                LaunchedEffect(action) {
                    asyncIndicatorState.enqueue(AsyncIndicator.DURATION_SHORT) {
                        AsyncIndicator.Failure(
                            text = stringResource(CommonStrings.common_something_went_wrong),
                        )
                    }
                }
            }
            is AsyncAction.Success -> {
                LaunchedEffect(action) { onActionSuccess() }
            }
            else -> Unit
        }
    }
}

@Composable
private fun BottomSheetContent(
    cameraState: CameraState,
    state: ShareLocationState,
    userLocationState: UserLocationState,
    navigateUp: () -> Unit,
) {
    Text(
        text = stringResource(CommonStrings.screen_sharing_location_option_sheet_title),
        style = ElementTheme.typography.fontBodyLgMedium,
        color = ElementTheme.colors.textPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 29.dp, bottom = 13.dp),
        textAlign = TextAlign.Center,
    )
    val userLocation = userLocationState.location
    if (state.trackUserLocation && userLocation != null) {
        ShareCurrentLocationItem(
            showDivider = state.canShareLiveLocation,
            onClick = {
                state.eventSink(
                    ShareLocationEvent.ShareStaticLocation(
                        location = Location(
                            lat = userLocation.position.latitude,
                            lon = userLocation.position.longitude
                        ),
                        isPinned = false
                    )
                )
                navigateUp()
            },
        )
    } else {
        SharePinLocationItem(
            showDivider = state.canShareLiveLocation,
            onClick = {
                val positionTarget = cameraState.position.target
                state.eventSink(
                    ShareLocationEvent.ShareStaticLocation(
                        location = Location(lat = positionTarget.latitude, lon = positionTarget.longitude),
                        isPinned = true
                    )
                )
                navigateUp()
            }
        )
    }
    if (state.canShareLiveLocation) {
        ShareLiveLocationItem {
            state.eventSink(ShareLocationEvent.InitiateLiveLocationShare)
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ShareCurrentLocationItem(
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    ShareLocationActionRow(
        text = stringResource(CommonStrings.screen_share_my_location_action),
        icon = CompoundIcons.LocationNavigatorCentred(),
        iconTint = ElementTheme.colors.iconSecondary,
        onClick = onClick,
        showDivider = showDivider,
    )
}

@Composable
private fun SharePinLocationItem(
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    ShareLocationActionRow(
        text = stringResource(CommonStrings.screen_share_this_location_action),
        icon = CompoundIcons.LocationNavigator(),
        iconTint = ElementTheme.colors.iconSecondary,
        onClick = onClick,
        showDivider = showDivider,
    )
}

@Composable
private fun ShareLiveLocationItem(
    onClick: () -> Unit,
) {
    ShareLocationActionRow(
        text = stringResource(CommonStrings.action_share_live_location),
        icon = CompoundIcons.LocationPinSolid(),
        iconTint = ElementTheme.colors.iconAccentPrimary,
        onClick = onClick,
        showDivider = false,
    )
}

@Composable
private fun ShareLocationActionRow(
    text: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    showDivider: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .semantics(mergeDescendants = true) {}
                .clickable(onClick = onClick)
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = text,
                style = ElementTheme.typography.fontBodyLgRegular,
                color = ElementTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 50.dp),
                color = ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.45f),
            )
        }
    }
}

@Composable
private fun LiveLocationDurationDialog(
    durations: ImmutableList<LiveLocationDuration>,
    onSelectDuration: (Duration) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    ListDialog(
        title = stringResource(R.string.screen_share_location_live_location_duration_picker_title),
        submitText = stringResource(CommonStrings.action_continue),
        onSubmit = { onSelectDuration(durations[selectedIndex].duration) },
        onDismissRequest = onDismiss,
        applyPaddingToContents = false,
        verticalArrangement = Arrangement.Top
    ) {
        itemsIndexed(durations) { index, duration ->
            RadioButtonListItem(
                headline = duration.formatted,
                selected = index == selectedIndex,
                onSelect = { selectedIndex = index },
                compactLayout = true,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@PreviewsDayNight
@Composable
internal fun ShareLocationViewPreview(
    @PreviewParameter(ShareLocationStateProvider::class) state: ShareLocationState
) = ElementPreview {
    ShareLocationView(
        state = state,
        navigateUp = {},
    )
}
