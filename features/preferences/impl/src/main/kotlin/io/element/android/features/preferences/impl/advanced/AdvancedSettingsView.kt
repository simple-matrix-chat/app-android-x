/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.advanced

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import im.vector.app.features.analytics.plan.Interaction
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.architecture.coverage.ExcludeFromCoverage
import io.element.android.libraries.designsystem.components.dialogs.ListDialog
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.ElementPreviewBlack
import io.element.android.libraries.designsystem.preview.ElementPreviewDark
import io.element.android.libraries.designsystem.preview.ElementPreviewLight
import io.element.android.libraries.designsystem.preview.PreviewWithLargeHeight
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.text.stringWithLink
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.ListSupportingText
import io.element.android.libraries.designsystem.theme.components.ListSupportingTextDefaults
import io.element.android.libraries.designsystem.theme.components.RadioButton
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Slider
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Switch
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.utils.snackbar.LocalSnackbarDispatcher
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarHost
import io.element.android.libraries.designsystem.utils.snackbar.collectSnackbarMessageAsState
import io.element.android.libraries.designsystem.utils.snackbar.rememberSnackbarHostState
import io.element.android.libraries.matrix.api.media.MediaPreviewValue
import io.element.android.libraries.preferences.api.store.VideoCompressionPreset
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.services.analytics.compose.LocalAnalyticsService
import io.element.android.services.analyticsproviders.api.trackers.captureInteraction
import kotlin.math.roundToInt

@Composable
fun AdvancedSettingsView(
    state: AdvancedSettingsState,
    onBackClick: () -> Unit,
    onOpenAppSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val analyticsService = LocalAnalyticsService.current
    val snackbarDispatcher = LocalSnackbarDispatcher.current
    val snackbarMessage by snackbarDispatcher.collectSnackbarMessageAsState()
    val snackbarHostState = rememberSnackbarHostState(snackbarMessage = snackbarMessage)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ElementTheme.colors.bgSubtleSecondary,
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            MomentAdvancedTopBar(onBackClick = onBackClick)
            ThemeSection(state = state)
            PreferencesSection(state = state)
            UploadsSection(
                state = state,
                captureMediaCompressionInteraction = { enabled ->
                    analyticsService.captureInteraction(
                        if (enabled) {
                            Interaction.Name.MobileSettingsOptimizeMediaUploadsEnabled
                        } else {
                            Interaction.Name.MobileSettingsOptimizeMediaUploadsDisabled
                        }
                    )
                }
            )
            ModerationAndSafetySection(state = state)
            if (state.liveLocationMinimumDistanceUpdate != null) {
                LiveLocationUpdatesSection(
                    value = state.liveLocationMinimumDistanceUpdate,
                    onValueSaved = { value ->
                        state.eventSink(AdvancedSettingsEvents.SetLiveLocationMinimumDistanceUpdate(value))
                    },
                    onOpenAppPermissionsClick = onOpenAppSettingsClick,
                )
            }
        }
    }
}

@Composable
private fun MomentAdvancedTopBar(
    onBackClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = CompoundIcons.ChevronLeft(),
                contentDescription = stringResource(CommonStrings.action_back),
                tint = ElementTheme.colors.iconPrimary,
            )
        }
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp),
            text = stringResource(CommonStrings.common_advanced_settings),
            style = ElementTheme.typography.fontBodyLgMedium.copy(fontWeight = FontWeight.Bold),
            color = ElementTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ThemeSection(
    state: AdvancedSettingsState,
) {
    MomentAdvancedSection(
        title = stringResource(CommonStrings.common_appearance),
        description = stringResource(R.string.screen_advanced_settings_moment_appearance_description),
    ) {
        MomentAdvancedCard {
            state.availableThemeOptions.forEachIndexed { index, themeOption ->
                MomentThemeRow(
                    title = themeOption.getText(),
                    isSelected = state.theme == themeOption,
                    onClick = { state.eventSink(AdvancedSettingsEvents.SetTheme(themeOption)) },
                    showDivider = index != state.availableThemeOptions.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun PreferencesSection(
    state: AdvancedSettingsState,
) {
    MomentAdvancedSection(
        title = stringResource(R.string.screen_advanced_settings_moment_preferences_section_title),
    ) {
        MomentAdvancedCard {
            MomentSwitchRow(
                title = stringResource(CommonStrings.action_view_source),
                description = stringResource(R.string.screen_advanced_settings_view_source_description),
                imageVector = CompoundIcons.Code(),
                checked = state.isDeveloperModeEnabled,
                onCheckedChange = {
                    state.eventSink(AdvancedSettingsEvents.SetDeveloperModeEnabled(!state.isDeveloperModeEnabled))
                },
            )
            MomentSwitchRow(
                title = stringResource(R.string.screen_advanced_settings_share_presence),
                description = stringResource(R.string.screen_advanced_settings_share_presence_description),
                imageVector = CompoundIcons.PresenceSolid8X8(),
                checked = state.isSharePresenceEnabled,
                onCheckedChange = {
                    state.eventSink(AdvancedSettingsEvents.SetSharePresenceEnabled(!state.isSharePresenceEnabled))
                },
                showDivider = false,
            )
        }
    }
}

@Composable
private fun UploadsSection(
    state: AdvancedSettingsState,
    captureMediaCompressionInteraction: (Boolean) -> Unit,
) {
    val mediaOptimizationState = state.mediaOptimizationState ?: return
    MomentAdvancedSection(
        title = stringResource(R.string.screen_advanced_settings_moment_uploads_section_title),
    ) {
        MomentAdvancedCard {
            when (mediaOptimizationState) {
                is MediaOptimizationState.AllMedia -> {
                    val compressImages = mediaOptimizationState.shouldCompressImages
                    MomentSwitchRow(
                        title = stringResource(R.string.screen_advanced_settings_media_compression_title),
                        description = stringResource(R.string.screen_advanced_settings_media_compression_description),
                        imageVector = CompoundIcons.Image(),
                        checked = compressImages,
                        onCheckedChange = {
                            val newValue = !compressImages
                            captureMediaCompressionInteraction(newValue)
                            state.eventSink(AdvancedSettingsEvents.SetCompressMedia(newValue))
                        },
                        showDivider = false,
                    )
                }
                is MediaOptimizationState.Split -> {
                    val compressImages = mediaOptimizationState.shouldCompressImages
                    MomentSwitchRow(
                        title = stringResource(R.string.screen_advanced_settings_optimise_image_upload_quality_title),
                        description = stringResource(R.string.screen_advanced_settings_optimise_image_upload_quality_description),
                        imageVector = CompoundIcons.Image(),
                        checked = compressImages,
                        onCheckedChange = {
                            val newValue = !compressImages
                            captureMediaCompressionInteraction(newValue)
                            state.eventSink(AdvancedSettingsEvents.SetCompressMedia(newValue))
                        },
                    )

                    var displaySelectorDialog by remember { mutableStateOf(false) }
                    val videoQuality = videoQualityTitle(mediaOptimizationState.videoPreset)
                    val videoDescription = stringResource(
                        id = R.string.screen_advanced_settings_optimise_video_upload_quality_description,
                        videoQuality
                    )

                    MomentAdvancedRow(
                        title = stringResource(R.string.screen_advanced_settings_optimise_video_upload_quality_title),
                        description = videoDescription,
                        imageVector = CompoundIcons.VideoCall(),
                        onClick = { displaySelectorDialog = true },
                        trailingText = videoQuality,
                        showDivider = false,
                    )

                    if (displaySelectorDialog) {
                        VideoQualitySelectorDialog(
                            selectedPreset = mediaOptimizationState.videoPreset,
                            onSubmit = { preset ->
                                state.eventSink(AdvancedSettingsEvents.SetVideoUploadQuality(preset))
                                displaySelectorDialog = false
                            },
                            onDismiss = { displaySelectorDialog = false },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun videoQualityTitle(videoPreset: VideoCompressionPreset): String {
    return when (videoPreset) {
        VideoCompressionPreset.LOW -> stringResource(id = R.string.screen_advanced_settings_optimise_video_upload_quality_low)
        VideoCompressionPreset.STANDARD -> stringResource(id = R.string.screen_advanced_settings_optimise_video_upload_quality_standard)
        VideoCompressionPreset.HIGH -> stringResource(id = R.string.screen_advanced_settings_optimise_video_upload_quality_high)
    }
}

@Composable
private fun ModerationAndSafetySection(
    state: AdvancedSettingsState,
) {
    MomentAdvancedSection(
        title = stringResource(R.string.screen_advanced_settings_moderation_and_safety_section_title),
    ) {
        MomentAdvancedCard {
            MomentSwitchRow(
                title = stringResource(R.string.screen_advanced_settings_hide_invite_avatars_toggle_title),
                imageVector = CompoundIcons.VisibilityOff(),
                checked = state.mediaPreviewConfigState.hideInviteAvatars,
                enabled = !state.mediaPreviewConfigState.setHideInviteAvatarsAction.isLoading(),
                onCheckedChange = {
                    state.eventSink(AdvancedSettingsEvents.SetHideInviteAvatars(!state.mediaPreviewConfigState.hideInviteAvatars))
                },
            )
            MomentAdvancedRow(
                title = stringResource(R.string.screen_advanced_settings_show_media_timeline_title),
                description = stringResource(R.string.screen_advanced_settings_show_media_timeline_subtitle),
                imageVector = CompoundIcons.Image(),
                onClick = null,
            )
            TimelineMediaPreviewRow(
                title = stringResource(R.string.screen_advanced_settings_show_media_timeline_always_hide),
                selected = state.mediaPreviewConfigState.timelineMediaPreviewValue == MediaPreviewValue.Off,
                enabled = !state.mediaPreviewConfigState.setTimelineMediaPreviewAction.isLoading(),
                onClick = {
                    state.eventSink(AdvancedSettingsEvents.SetTimelineMediaPreviewValue(MediaPreviewValue.Off))
                },
            )
            TimelineMediaPreviewRow(
                title = stringResource(R.string.screen_advanced_settings_show_media_timeline_private_rooms),
                selected = state.mediaPreviewConfigState.timelineMediaPreviewValue == MediaPreviewValue.Private,
                enabled = !state.mediaPreviewConfigState.setTimelineMediaPreviewAction.isLoading(),
                onClick = {
                    state.eventSink(AdvancedSettingsEvents.SetTimelineMediaPreviewValue(MediaPreviewValue.Private))
                },
            )
            TimelineMediaPreviewRow(
                title = stringResource(R.string.screen_advanced_settings_show_media_timeline_always_show),
                selected = state.mediaPreviewConfigState.timelineMediaPreviewValue == MediaPreviewValue.On,
                enabled = !state.mediaPreviewConfigState.setTimelineMediaPreviewAction.isLoading(),
                onClick = {
                    state.eventSink(AdvancedSettingsEvents.SetTimelineMediaPreviewValue(MediaPreviewValue.On))
                },
                showDivider = false,
            )
        }
    }
}

@Composable
private fun TimelineMediaPreviewRow(
    title: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean = true,
) {
    val rowClick: () -> Unit = if (enabled) {
        onClick
    } else {
        {}
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .semantics(mergeDescendants = true) {}
                .clickable(onClick = rowClick)
                .padding(start = 60.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = ElementTheme.typography.fontBodyLgRegular,
                color = if (enabled) ElementTheme.colors.textPrimary else ElementTheme.colors.textDisabled,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            RadioButton(
                selected = selected,
                onClick = if (enabled) onClick else null,
                enabled = enabled,
            )
        }
        if (showDivider) {
            MomentAdvancedDivider(start = 60.dp)
        }
    }
}

@Composable
private fun LiveLocationUpdatesSection(
    value: Int,
    onValueSaved: (Int) -> Unit,
    onOpenAppPermissionsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MomentAdvancedSection(
        modifier = modifier,
        title = stringResource(R.string.screen_advanced_settings_live_location_section_title),
        description = stringResource(R.string.screen_advanced_settings_live_location_section_description),
    ) {
        MomentAdvancedCard {
            var sliderValue by remember(value) { mutableIntStateOf(value) }
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = pluralStringResource(
                        R.plurals.screen_advanced_settings_live_location_update_distance,
                        sliderValue,
                        sliderValue,
                    ),
                    style = ElementTheme.typography.fontBodyLgRegular,
                    color = ElementTheme.colors.textPrimary,
                )
                val valueRange = 1f..100f
                val start = valueRange.start.toInt()
                val end = valueRange.endInclusive.toInt()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${start}m", color = ElementTheme.colors.textSecondary, style = ElementTheme.typography.fontBodyMdRegular)
                    Slider(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        value = sliderValue.toFloat(),
                        onValueChange = { sliderValue = it.roundToInt() },
                        onValueChangeFinish = {
                            onValueSaved(sliderValue)
                        },
                        valueRange = valueRange,
                        colors = SliderDefaults.colors(
                            thumbColor = ElementTheme.colors.iconAccentPrimary,
                            activeTrackColor = ElementTheme.colors.iconAccentPrimary,
                            inactiveTrackColor = ElementTheme.colors.bgBadgeAccent,
                            inactiveTickColor = ElementTheme.colors.iconAccentPrimary,
                        )
                    )
                    Text("${end}m", color = ElementTheme.colors.textSecondary, style = ElementTheme.typography.fontBodyMdRegular)
                }
            }
        }
        val footerText = stringWithLink(
            textRes = R.string.screen_advanced_settings_live_location_section_footer,
            url = "",
            linkTextRes = R.string.screen_advanced_settings_live_location_section_footer_link,
            onLinkClick = { onOpenAppPermissionsClick() },
        )
        ListSupportingText(
            annotatedString = footerText,
            contentPadding = ListSupportingTextDefaults.Padding.None,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun MomentAdvancedSection(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                style = ElementTheme.typography.fontBodySmMedium.copy(fontWeight = FontWeight.Bold),
                color = ElementTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                )
            }
        }
        content()
    }
}

@Composable
private fun MomentAdvancedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.55f)),
    ) {
        Column(content = content)
    }
}

@Composable
private fun MomentAdvancedRow(
    title: String,
    imageVector: ImageVector,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    description: String? = null,
    trailingText: String? = null,
    showDivider: Boolean = true,
) {
    val rowClick = onClick
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (description == null) 60.dp else 76.dp)
                .semantics(mergeDescendants = true) {}
                .then(if (rowClick != null) Modifier.clickable(onClick = rowClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MomentAdvancedIconTile(imageVector = imageVector)
            MomentAdvancedRowText(
                modifier = Modifier.weight(1f),
                title = title,
                description = description,
            )
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (rowClick != null) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = CompoundIcons.ChevronRight(),
                    contentDescription = null,
                    tint = ElementTheme.colors.iconSecondary,
                )
            }
        }
        if (showDivider) {
            MomentAdvancedDivider(start = 60.dp)
        }
    }
}

@Composable
private fun MomentSwitchRow(
    title: String,
    imageVector: ImageVector,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    showDivider: Boolean = true,
) {
    val rowClick: () -> Unit = if (enabled) {
        onCheckedChange
    } else {
        {}
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (description == null) 64.dp else 82.dp)
                .semantics(mergeDescendants = true) {}
                .clickable(onClick = rowClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MomentAdvancedIconTile(
                imageVector = imageVector,
                enabled = enabled,
            )
            MomentAdvancedRowText(
                modifier = Modifier.weight(1f),
                title = title,
                description = description,
                enabled = enabled,
            )
            Switch(
                checked = checked,
                onCheckedChange = if (enabled) {
                    { onCheckedChange() }
                } else {
                    null
                },
                enabled = enabled,
            )
        }
        if (showDivider) {
            MomentAdvancedDivider(start = 60.dp)
        }
    }
}

@Composable
private fun MomentThemeRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .semantics(mergeDescendants = true) {}
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = ElementTheme.typography.fontBodyLgMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ElementTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isSelected) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = CompoundIcons.Check(),
                    contentDescription = null,
                    tint = ElementTheme.colors.iconPrimary,
                )
            }
        }
        if (showDivider) {
            MomentAdvancedDivider(start = 16.dp)
        }
    }
}

@Composable
private fun MomentAdvancedIconTile(
    imageVector: ImageVector,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(
                color = if (enabled) {
                    ElementTheme.colors.bgSubtleSecondary
                } else {
                    ElementTheme.colors.bgSubtleSecondary.copy(alpha = 0.55f)
                },
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = imageVector,
            contentDescription = null,
            tint = if (enabled) ElementTheme.colors.iconPrimary else ElementTheme.colors.iconDisabled,
        )
    }
}

@Composable
private fun MomentAdvancedRowText(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = title,
            style = ElementTheme.typography.fontBodyLgMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (enabled) ElementTheme.colors.textPrimary else ElementTheme.colors.textDisabled,
        )
        if (description != null) {
            Text(
                text = description,
                style = ElementTheme.typography.fontBodySmRegular,
                color = if (enabled) ElementTheme.colors.textSecondary else ElementTheme.colors.textDisabled,
            )
        }
    }
}

@Composable
private fun MomentAdvancedDivider(start: androidx.compose.ui.unit.Dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = start),
        color = ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.45f),
    )
}

@Composable
private fun VideoQualitySelectorDialog(
    selectedPreset: VideoCompressionPreset,
    onSubmit: (VideoCompressionPreset) -> Unit,
    onDismiss: () -> Unit
) {
    val videoPresets = VideoCompressionPreset.entries
    var localSelectedPreset by remember { mutableStateOf(selectedPreset) }
    ListDialog(
        title = stringResource(CommonStrings.dialog_video_quality_selector_title),
        subtitle = stringResource(CommonStrings.dialog_default_video_quality_selector_subtitle),
        onSubmit = { onSubmit(localSelectedPreset) },
        onDismissRequest = onDismiss,
        applyPaddingToContents = false,
    ) {
        for (preset in videoPresets) {
            val isSelected = preset == localSelectedPreset
            item(
                key = preset,
                contentType = preset,
            ) {
                val title = videoQualityTitle(preset)
                val subtitle = when (preset) {
                    VideoCompressionPreset.LOW -> stringResource(CommonStrings.common_video_quality_low_description)
                    VideoCompressionPreset.STANDARD -> stringResource(CommonStrings.common_video_quality_standard_description)
                    VideoCompressionPreset.HIGH -> stringResource(CommonStrings.common_video_quality_high_description)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 74.dp)
                        .semantics(mergeDescendants = true) {}
                        .clickable {
                            localSelectedPreset = preset
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { localSelectedPreset = preset },
                    )
                    MomentAdvancedRowText(
                        modifier = Modifier.weight(1f),
                        title = title,
                        description = subtitle,
                    )
                }
            }
        }
    }
}

@PreviewWithLargeHeight
@Composable
internal fun AdvancedSettingsViewLightPreview(@PreviewParameter(AdvancedSettingsStateProvider::class) state: AdvancedSettingsState) =
    ElementPreviewLight { ContentToPreview(state) }

@PreviewWithLargeHeight
@Composable
internal fun AdvancedSettingsViewDarkPreview(@PreviewParameter(AdvancedSettingsStateProvider::class) state: AdvancedSettingsState) =
    ElementPreviewDark { ContentToPreview(state) }

@PreviewWithLargeHeight
@Composable
internal fun AdvancedSettingsViewBlackPreview(@PreviewParameter(AdvancedSettingsStateProvider::class) state: AdvancedSettingsState) =
    ElementPreviewBlack { ContentToPreview(state) }

@ExcludeFromCoverage
@Composable
private fun ContentToPreview(state: AdvancedSettingsState) {
    AdvancedSettingsView(
        state = state,
        onBackClick = { },
        onOpenAppSettingsClick = {}
    )
}

@Composable
@PreviewsDayNight
internal fun VideoQualitySelectorDialogPreview() {
    ElementPreview {
        VideoQualitySelectorDialog(
            selectedPreset = VideoCompressionPreset.STANDARD,
            onSubmit = { /* no-op */ },
            onDismiss = { /* no-op */ }
        )
    }
}
