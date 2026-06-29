/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.general

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FabPosition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun MomentGeneralSettingsView(
    state: MomentGeneralSettingsState,
    onBackClick: () -> Unit,
    onManageAccountClick: (url: String) -> Unit,
    onOpenLinkNewDevice: () -> Unit,
    onOpenBlockedUsers: () -> Unit,
    onOpenSessions: () -> Unit,
    onOpenLockScreenSettings: () -> Unit,
    onOpenAdvancedSettings: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeactivateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackClick)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ElementTheme.colors.bgSubtleSecondary,
        floatingActionButtonPosition = FabPosition.Center,
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
            MomentGeneralHeader(onBackClick = onBackClick)
            ThemeSection(state = state)
            if (state.accountManagementUrl != null || state.showLinkNewDevice || state.showBlockedUsers) {
                AccountSection(
                    state = state,
                    onManageAccountClick = onManageAccountClick,
                    onOpenLinkNewDevice = onOpenLinkNewDevice,
                    onOpenBlockedUsers = onOpenBlockedUsers,
                )
            }
            SessionsSection(onOpenSessions = onOpenSessions)
            AppSection(
                onOpenLockScreenSettings = onOpenLockScreenSettings,
                onOpenAdvancedSettings = onOpenAdvancedSettings,
            )
            LogoutSection(
                canDeactivateAccount = state.canDeactivateAccount,
                onSignOutClick = onSignOutClick,
                onDeactivateClick = onDeactivateClick,
            )
        }
    }
}

@Composable
private fun MomentGeneralHeader(
    onBackClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
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
        }

        Text(
            text = stringResource(R.string.screen_moment_general_title),
            style = ElementTheme.typography.fontBodyLgMedium.copy(fontWeight = FontWeight.Bold),
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ThemeSection(
    state: MomentGeneralSettingsState,
) {
    MomentGeneralSection(
        title = R.string.screen_moment_general_theme_section_title,
        description = R.string.screen_moment_general_appearance_description,
    ) {
        MomentGeneralCard {
            state.availableThemeOptions.forEachIndexed { index, themeOption ->
                MomentGeneralSelectionRow(
                    title = themeOption.getText(),
                    isSelected = state.theme == themeOption,
                    onClick = { state.eventSink(MomentGeneralSettingsEvent.SetTheme(themeOption)) },
                    showDivider = index != state.availableThemeOptions.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun AccountSection(
    state: MomentGeneralSettingsState,
    onManageAccountClick: (url: String) -> Unit,
    onOpenLinkNewDevice: () -> Unit,
    onOpenBlockedUsers: () -> Unit,
) {
    MomentGeneralSection(title = R.string.screen_moment_general_account_section_title) {
        MomentGeneralCard {
            val accountManagementUrl = state.accountManagementUrl
            if (accountManagementUrl != null) {
                MomentGeneralRow(
                    title = stringResource(CommonStrings.action_manage_account_and_devices),
                    imageVector = CompoundIcons.UserProfile(),
                    onClick = { onManageAccountClick(accountManagementUrl) },
                    showDivider = state.showLinkNewDevice || state.showBlockedUsers,
                )
            }
            if (state.showLinkNewDevice) {
                MomentGeneralRow(
                    title = stringResource(CommonStrings.common_link_new_device),
                    imageVector = CompoundIcons.Devices(),
                    onClick = onOpenLinkNewDevice,
                    showDivider = state.showBlockedUsers,
                )
            }
            if (state.showBlockedUsers) {
                MomentGeneralRow(
                    title = stringResource(CommonStrings.common_blocked_users),
                    imageVector = CompoundIcons.Block(),
                    onClick = onOpenBlockedUsers,
                    showDivider = false,
                )
            }
        }
    }
}

@Composable
private fun SessionsSection(
    onOpenSessions: () -> Unit,
) {
    MomentGeneralSection(
        title = R.string.screen_moment_general_sessions_section_title,
        description = R.string.screen_moment_general_sessions_section_description,
    ) {
        MomentGeneralCard {
            MomentGeneralRow(
                title = stringResource(R.string.screen_moment_sessions_title),
                subtitle = stringResource(R.string.screen_moment_sessions_description),
                imageVector = CompoundIcons.Devices(),
                onClick = onOpenSessions,
                showDivider = false,
            )
        }
    }
}

@Composable
private fun AppSection(
    onOpenLockScreenSettings: () -> Unit,
    onOpenAdvancedSettings: () -> Unit,
) {
    MomentGeneralSection(title = R.string.screen_moment_general_app_section_title) {
        MomentGeneralCard {
            MomentGeneralRow(
                title = stringResource(CommonStrings.common_screen_lock),
                imageVector = CompoundIcons.Lock(),
                onClick = onOpenLockScreenSettings,
            )
            MomentGeneralRow(
                title = stringResource(CommonStrings.common_advanced_settings),
                imageVector = CompoundIcons.Settings(),
                onClick = onOpenAdvancedSettings,
                showDivider = false,
            )
        }
    }
}

@Composable
private fun LogoutSection(
    canDeactivateAccount: Boolean,
    onSignOutClick: () -> Unit,
    onDeactivateClick: () -> Unit,
) {
    MomentGeneralCard {
        MomentGeneralRow(
            title = stringResource(CommonStrings.action_signout),
            imageVector = CompoundIcons.SignOut(),
            isDestructive = true,
            showChevron = false,
            onClick = onSignOutClick,
            showDivider = canDeactivateAccount,
        )
        if (canDeactivateAccount) {
            MomentGeneralRow(
                title = stringResource(CommonStrings.action_delete_account),
                imageVector = CompoundIcons.Warning(),
                isDestructive = true,
                showChevron = false,
                onClick = onDeactivateClick,
                showDivider = false,
            )
        }
    }
}

@Composable
private fun MomentGeneralSection(
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    @StringRes description: Int? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(title),
            style = ElementTheme.typography.fontBodySmMedium.copy(fontWeight = FontWeight.Bold),
            color = ElementTheme.colors.textSecondary,
        )
        if (description != null) {
            Text(
                text = stringResource(description),
                style = ElementTheme.typography.fontBodyMdRegular,
                color = ElementTheme.colors.textSecondary,
            )
        }
        content()
    }
}

@Composable
private fun MomentGeneralCard(
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
private fun MomentGeneralRow(
    title: String,
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isDestructive: Boolean = false,
    showChevron: Boolean = true,
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
            MomentGeneralIconTile(
                imageVector = imageVector,
                isDestructive = isDestructive,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = ElementTheme.typography.fontBodyLgMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isDestructive) ElementTheme.colors.textCriticalPrimary else ElementTheme.colors.textPrimary,
                    maxLines = 1,
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
            if (showChevron) {
                Icon(
                    modifier = Modifier.size(14.dp),
                    imageVector = CompoundIcons.ChevronRight(),
                    contentDescription = null,
                    tint = ElementTheme.colors.iconSecondary,
                )
            }
        }
        if (showDivider) {
            MomentGeneralDivider(start = 16.dp)
        }
    }
}

@Composable
private fun MomentGeneralSelectionRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .semantics(mergeDescendants = true) {}
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
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
                    modifier = Modifier.size(14.dp),
                    imageVector = CompoundIcons.Check(),
                    contentDescription = null,
                    tint = ElementTheme.colors.iconPrimary,
                )
            }
        }
        if (showDivider) {
            MomentGeneralDivider(start = 16.dp)
        }
    }
}

@Composable
private fun MomentGeneralIconTile(
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
                shape = RoundedCornerShape(16.dp)
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
private fun MomentGeneralDivider(start: androidx.compose.ui.unit.Dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = start),
        color = ElementTheme.colors.borderDisabled.copy(alpha = 0.55f),
    )
}

@ExcludeFromCoverage
@PreviewsDayNight
@Composable
internal fun MomentGeneralSettingsViewPreview(
    @PreviewParameter(MomentGeneralSettingsStateProvider::class) state: MomentGeneralSettingsState,
) = ElementPreview {
    MomentGeneralSettingsView(
        state = state,
        onBackClick = {},
        onManageAccountClick = {},
        onOpenLinkNewDevice = {},
        onOpenBlockedUsers = {},
        onOpenSessions = {},
        onOpenLockScreenSettings = {},
        onOpenAdvancedSettings = {},
        onSignOutClick = {},
        onDeactivateClick = {},
    )
}
