/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.privacy

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarHost
import io.element.android.libraries.designsystem.utils.snackbar.rememberSnackbarHostState
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun MomentPrivacySettingsView(
    state: MomentPrivacySettingsState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = rememberSnackbarHostState(snackbarMessage = state.snackbarMessage)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ElementTheme.colors.bgSubtleSecondary,
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.navigationBarsPadding(),
            )
        },
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
            MomentPrivacyTopBar(onBackClick = onBackClick)
            PrivacyAccessSection(
                title = stringResource(R.string.screen_moment_privacy_direct_messages_title),
                description = stringResource(R.string.screen_moment_privacy_direct_messages_description),
                selected = state.settings.directMessages,
                loading = state.isLoading || state.savingSection == MomentPrivacySettingsSection.DirectMessages,
                enabled = state.rowsEnabled,
                onSelect = { state.eventSink(MomentPrivacySettingsEvent.SelectDirectMessages(it)) },
            )
            PrivacyAccessSection(
                title = stringResource(R.string.screen_moment_privacy_group_invites_title),
                description = stringResource(R.string.screen_moment_privacy_group_invites_description),
                selected = state.settings.groupInvites,
                loading = state.isLoading || state.savingSection == MomentPrivacySettingsSection.GroupInvites,
                enabled = state.rowsEnabled,
                onSelect = { state.eventSink(MomentPrivacySettingsEvent.SelectGroupInvites(it)) },
            )
            VisibilityAccessSection(
                title = stringResource(R.string.screen_moment_privacy_avatar_visibility_title),
                description = stringResource(R.string.screen_moment_privacy_avatar_visibility_description),
                selected = state.settings.avatarVisibility,
                loading = state.isLoading || state.savingSection == MomentPrivacySettingsSection.AvatarVisibility,
                enabled = state.rowsEnabled,
                onSelect = { state.eventSink(MomentPrivacySettingsEvent.SelectAvatarVisibility(it)) },
            )
            PrivacyAccessSection(
                title = stringResource(R.string.screen_moment_privacy_phone_visibility_title),
                description = stringResource(R.string.screen_moment_privacy_phone_visibility_description),
                selected = state.settings.phoneVisibility,
                loading = state.isLoading || state.savingSection == MomentPrivacySettingsSection.PhoneVisibility,
                enabled = state.rowsEnabled,
                onSelect = { state.eventSink(MomentPrivacySettingsEvent.SelectPhoneVisibility(it)) },
            )
            VisibilityAccessSection(
                title = stringResource(R.string.screen_moment_privacy_presence_visibility_title),
                description = stringResource(R.string.screen_moment_privacy_presence_visibility_description),
                selected = state.settings.presenceVisibility,
                loading = state.isLoading || state.savingSection == MomentPrivacySettingsSection.PresenceVisibility,
                enabled = state.rowsEnabled,
                onSelect = { state.eventSink(MomentPrivacySettingsEvent.SelectPresenceVisibility(it)) },
            )
        }
    }
}

@Composable
private fun MomentPrivacyTopBar(
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
            text = stringResource(R.string.screen_moment_privacy_title),
            style = ElementTheme.typography.fontBodyLgMedium.copy(fontWeight = FontWeight.Bold),
            color = ElementTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PrivacyAccessSection(
    title: String,
    description: String,
    selected: MomentPrivacyAccess,
    loading: Boolean,
    enabled: Boolean,
    onSelect: (MomentPrivacyAccess) -> Unit,
) {
    MomentPrivacySection(
        title = title,
        description = description,
        loading = loading,
    ) {
        MomentPrivacyCard {
            MomentPrivacyAccess.entries.forEachIndexed { index, access ->
                MomentPrivacyOptionRow(
                    title = access.label(),
                    selected = selected == access,
                    enabled = enabled,
                    onClick = { onSelect(access) },
                    showDivider = index != MomentPrivacyAccess.entries.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun VisibilityAccessSection(
    title: String,
    description: String,
    selected: MomentVisibilityAccess,
    loading: Boolean,
    enabled: Boolean,
    onSelect: (MomentVisibilityAccess) -> Unit,
) {
    MomentPrivacySection(
        title = title,
        description = description,
        loading = loading,
    ) {
        MomentPrivacyCard {
            MomentVisibilityAccess.entries.forEachIndexed { index, access ->
                MomentPrivacyOptionRow(
                    title = access.label(),
                    selected = selected == access,
                    enabled = enabled,
                    onClick = { onSelect(access) },
                    showDivider = index != MomentVisibilityAccess.entries.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun MomentPrivacySection(
    title: String,
    description: String,
    loading: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = ElementTheme.typography.fontBodySmMedium.copy(fontWeight = FontWeight.Bold),
                color = ElementTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
        Text(
            text = description,
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
        )
        content()
    }
}

@Composable
private fun MomentPrivacyCard(
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
private fun MomentPrivacyOptionRow(
    title: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics(mergeDescendants = true) {}
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = ElementTheme.typography.fontBodyLgMedium,
                color = ElementTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (selected) {
                Icon(
                    modifier = Modifier.size(14.dp),
                    imageVector = CompoundIcons.Check(),
                    contentDescription = null,
                    tint = ElementTheme.colors.iconPrimary,
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                color = ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.45f),
            )
        }
    }
}

@Composable
private fun MomentPrivacyAccess.label(): String {
    return when (this) {
        MomentPrivacyAccess.Everyone -> stringResource(R.string.screen_moment_privacy_option_everyone)
        MomentPrivacyAccess.ContactsOnly -> stringResource(R.string.screen_moment_privacy_option_contacts_only)
        MomentPrivacyAccess.Nobody -> stringResource(R.string.screen_moment_privacy_option_nobody)
    }
}

@Composable
private fun MomentVisibilityAccess.label(): String {
    return when (this) {
        MomentVisibilityAccess.Everyone -> stringResource(R.string.screen_moment_privacy_option_everyone)
        MomentVisibilityAccess.ContactsOnly -> stringResource(R.string.screen_moment_privacy_option_contacts_only)
    }
}

@PreviewsDayNight
@Composable
internal fun MomentPrivacySettingsViewPreview(@PreviewParameter(MomentPrivacySettingsStateProvider::class) state: MomentPrivacySettingsState) =
    ElementPreview {
        ContentToPreview(state)
    }

@ExcludeFromCoverage
@Composable
private fun ContentToPreview(state: MomentPrivacySettingsState) {
    MomentPrivacySettingsView(
        state = state,
        onBackClick = {},
    )
}
