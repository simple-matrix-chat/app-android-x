/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.root

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.architecture.coverage.ExcludeFromCoverage
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.preview.ElementPreviewDark
import io.element.android.libraries.designsystem.preview.ElementPreviewLight
import io.element.android.libraries.designsystem.preview.PreviewWithLargeHeight
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.utils.CommonDrawables
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarHost
import io.element.android.libraries.designsystem.utils.snackbar.rememberSnackbarHostState
import io.element.android.libraries.matrix.api.core.DeviceId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.matrix.ui.model.getBestName
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun PreferencesRootView(
    state: PreferencesRootState,
    onBackClick: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenDeveloperSettings: () -> Unit,
    onOpenGeneralSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenPrivacySettings: () -> Unit,
    onOpenUserProfile: (MatrixUser) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = rememberSnackbarHostState(snackbarMessage = state.snackbarMessage)
    BackHandler(onBack = onBackClick)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ElementTheme.colors.bgSubtleSecondary,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            MomentSettingsTitle()
            MomentSettingsUserCard(
                matrixUser = state.myUser,
                profileStatus = state.profileStatus,
                onClick = { onOpenUserProfile(state.myUser) },
            )
            AppSettingsCard(
                version = state.version,
                momentPrivacySummary = state.momentPrivacySummary,
                momentNotificationsSummary = state.momentNotificationsSummary,
                onOpenGeneralSettings = onOpenGeneralSettings,
                onOpenNotificationSettings = onOpenNotificationSettings,
                onOpenPrivacySettings = onOpenPrivacySettings,
                onOpenAbout = onOpenAbout,
            )
            if (state.showDeveloperSettings) {
                DeveloperCard(onOpenDeveloperSettings = onOpenDeveloperSettings)
            }
            Footer(
                version = state.version,
                deviceId = state.deviceId,
                onClick = if (!state.showDeveloperSettings) {
                    { state.eventSink(PreferencesRootEvent.OnVersionInfoClick) }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun MomentSettingsTitle() {
    Text(
        text = stringResource(R.string.screen_preferences_root_tab_profile),
        style = ElementTheme.typography.fontHeadingXlBold,
        color = ElementTheme.colors.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun MomentSettingsUserCard(
    matrixUser: MatrixUser,
    profileStatus: String,
    onClick: () -> Unit,
) {
    val subtitle = profileStatus.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.screen_preferences_root_profile_no_status)
    MomentSettingsCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                avatarData = matrixUser.getAvatarData(AvatarSize.UserHeader),
                avatarType = AvatarType.User,
                forcedAvatarSize = 72.dp,
                modifier = Modifier.clip(CircleShape),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = matrixUser.getBestName(),
                    style = ElementTheme.typography.fontHeadingMdBold,
                    color = ElementTheme.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = ElementTheme.typography.fontBodyMdMedium,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                modifier = Modifier.size(14.dp),
                imageVector = CompoundIcons.ChevronRight(),
                contentDescription = null,
                tint = ElementTheme.colors.iconSecondary,
            )
        }
    }
}

@Composable
private fun AppSettingsCard(
    version: String,
    momentPrivacySummary: MomentPrivacySummary,
    momentNotificationsSummary: MomentNotificationsSummary?,
    onOpenGeneralSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenPrivacySettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val aboutSummary = remember(version) { version.lineSequence().firstOrNull().orEmpty() }
    MomentSettingsCard {
        MomentSettingsRow(
            title = stringResource(R.string.screen_moment_general_title),
            subtitle = stringResource(R.string.screen_moment_general_subtitle),
            imageVector = CompoundIcons.Settings(),
            onClick = onOpenGeneralSettings,
        )
        MomentSettingsRow(
            title = stringResource(R.string.screen_moment_privacy_title),
            subtitle = stringResource(momentPrivacySummary.stringRes),
            imageVector = CompoundIcons.Lock(),
            onClick = onOpenPrivacySettings,
        )
        MomentSettingsRow(
            title = stringResource(R.string.screen_notification_settings_title),
            subtitle = momentNotificationsSummary?.let { stringResource(it.stringRes) },
            imageVector = CompoundIcons.Notifications(),
            onClick = onOpenNotificationSettings,
        )
        MomentSettingsRow(
            title = stringResource(R.string.screen_moment_about_app_title),
            subtitle = aboutSummary,
            imageVector = CompoundIcons.Info(),
            onClick = onOpenAbout,
            showDivider = false,
        )
    }
}

private val MomentPrivacySummary.stringRes: Int
    get() = when (this) {
        MomentPrivacySummary.Everyone -> R.string.screen_moment_privacy_summary_everyone
        MomentPrivacySummary.ContactsOnly -> R.string.screen_moment_privacy_summary_contacts_only
        MomentPrivacySummary.Custom -> R.string.screen_moment_privacy_summary_custom
    }

private val MomentNotificationsSummary.stringRes: Int
    get() = when (this) {
        MomentNotificationsSummary.Enabled -> R.string.screen_moment_notifications_summary_enabled
        MomentNotificationsSummary.Disabled -> R.string.screen_moment_notifications_summary_disabled
    }

@Composable
private fun DeveloperCard(
    onOpenDeveloperSettings: () -> Unit,
) {
    MomentSettingsCard {
        MomentSettingsRow(
            title = stringResource(CommonStrings.common_developer_options),
            imageVector = CompoundIcons.Code(),
            onClick = onOpenDeveloperSettings,
            showDivider = false,
        )
    }
}

@Composable
private fun MomentSettingsCard(
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
private fun MomentSettingsRow(
    title: String,
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingText: String? = null,
    isDestructive: Boolean = false,
    showDivider: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (subtitle == null) 60.dp else 74.dp)
                .semantics(mergeDescendants = true) {}
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MomentSettingsIconTile(
                imageVector = imageVector,
                isDestructive = isDestructive,
            )
            MomentSettingsRowText(
                modifier = Modifier.weight(1f),
                title = title,
                subtitle = subtitle,
                isDestructive = isDestructive,
            )
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = ElementTheme.typography.fontBodyLgRegular,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                modifier = Modifier.size(14.dp),
                imageVector = CompoundIcons.ChevronRight(),
                contentDescription = null,
                tint = ElementTheme.colors.iconSecondary,
            )
        }
        if (showDivider) {
            MomentSettingsDivider()
        }
    }
}

@Composable
private fun MomentSettingsIconTile(
    imageVector: ImageVector,
    isDestructive: Boolean,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(
                color = if (isDestructive) {
                    ElementTheme.colors.bgCriticalSubtle
                } else {
                    ElementTheme.colors.bgSubtleSecondary
                },
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = imageVector,
            contentDescription = null,
            tint = if (isDestructive) {
                ElementTheme.colors.iconCriticalPrimary
            } else {
                ElementTheme.colors.iconPrimary
            },
        )
    }
}

@Composable
private fun MomentSettingsRowText(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isDestructive: Boolean = false,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            style = ElementTheme.typography.fontBodyLgMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (isDestructive) ElementTheme.colors.textCriticalPrimary else ElementTheme.colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MomentSettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 60.dp),
        color = ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.45f),
    )
}

@Composable
private fun Footer(
    version: String,
    deviceId: DeviceId?,
    onClick: (() -> Unit)?,
) {
    val text = remember(version, deviceId) {
        buildString {
            append(version)
            if (deviceId != null) {
                append("\n")
                append(deviceId)
            }
        }
    }
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = onClick ?: {})
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
        textAlign = TextAlign.Center,
        text = text,
        style = ElementTheme.typography.fontBodySmRegular,
        color = ElementTheme.colors.textSecondary,
    )
}

@PreviewWithLargeHeight
@Composable
internal fun PreferencesRootViewLightPreview(@PreviewParameter(PreferencesRootStateProvider::class) state: PreferencesRootState) =
    ElementPreviewLight(
        drawableFallbackForImages = CommonDrawables.sample_avatar,
    ) { ContentToPreview(state) }

@PreviewWithLargeHeight
@Composable
internal fun PreferencesRootViewDarkPreview(@PreviewParameter(PreferencesRootStateProvider::class) state: PreferencesRootState) =
    ElementPreviewDark(
        drawableFallbackForImages = CommonDrawables.sample_avatar,
    ) { ContentToPreview(state) }

@ExcludeFromCoverage
@Composable
private fun ContentToPreview(state: PreferencesRootState) {
    PreferencesRootView(
        state = state,
        onBackClick = {},
        onOpenDeveloperSettings = {},
        onOpenGeneralSettings = {},
        onOpenAbout = {},
        onOpenNotificationSettings = {},
        onOpenPrivacySettings = {},
        onOpenUserProfile = {},
    )
}
