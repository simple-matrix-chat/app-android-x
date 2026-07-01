/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.userprofile.shared.blockuser

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.userprofile.api.UserProfileEvents
import io.element.android.features.userprofile.api.UserProfileState
import io.element.android.features.userprofile.shared.R
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.core.bool.orFalse
import io.element.android.libraries.designsystem.components.dialogs.RetryDialog
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun BlockUserSection(
    state: UserProfileState,
    modifier: Modifier = Modifier,
) {
    val isBlocked = state.isBlocked
    when (isBlocked) {
        is AsyncData.Failure -> MomentBlockUserCard(modifier = modifier, isBlocked = isBlocked.prevData, isLoading = false, eventSink = state.eventSink)
        is AsyncData.Loading -> MomentBlockUserCard(modifier = modifier, isBlocked = isBlocked.prevData, isLoading = true, eventSink = state.eventSink)
        is AsyncData.Success -> MomentBlockUserCard(modifier = modifier, isBlocked = isBlocked.data, isLoading = false, eventSink = state.eventSink)
        AsyncData.Uninitialized -> MomentBlockUserCard(modifier = modifier, isBlocked = null, isLoading = true, eventSink = state.eventSink)
    }
    if (isBlocked is AsyncData.Failure) {
        RetryDialog(
            content = stringResource(CommonStrings.error_unknown),
            onDismiss = { state.eventSink(UserProfileEvents.ClearBlockUserError) },
            onRetry = {
                val event = when (isBlocked.prevData) {
                    true -> UserProfileEvents.UnblockUser(needsConfirmation = false)
                    false -> UserProfileEvents.BlockUser(needsConfirmation = false)
                    // null case Should not happen
                    null -> UserProfileEvents.ClearBlockUserError
                }
                state.eventSink(event)
            },
        )
    }
}

@Composable
private fun MomentBlockUserCard(
    modifier: Modifier = Modifier,
    isBlocked: Boolean?,
    isLoading: Boolean,
    eventSink: (UserProfileEvents) -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, ElementTheme.colors.borderDisabled),
    ) {
        MomentBlockUserRow(
            title = stringResource(
                if (isBlocked.orFalse()) R.string.screen_room_member_details_unblock_user else R.string.screen_room_member_details_block_user
            ),
            destructive = !isBlocked.orFalse(),
            isLoading = isLoading,
            onClick = {
                if (!isLoading) {
                    if (isBlocked.orFalse()) {
                        eventSink(UserProfileEvents.UnblockUser(needsConfirmation = true))
                    } else {
                        eventSink(UserProfileEvents.BlockUser(needsConfirmation = true))
                    }
                }
            },
        )
    }
}

@Composable
private fun MomentBlockUserRow(
    title: String,
    destructive: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val foregroundColor = if (destructive) ElementTheme.colors.textCriticalPrimary else ElementTheme.colors.textPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .semantics(mergeDescendants = true) {}
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = if (destructive) ElementTheme.colors.bgCriticalSubtle else ElementTheme.colors.bgSubtleSecondary,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = CompoundIcons.Block(),
                contentDescription = null,
                tint = foregroundColor,
            )
        }
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            text = title,
            color = foregroundColor,
            style = ElementTheme.typography.fontBodyLgMedium,
        )
        if (isLoading) {
            MomentBlockUserLoadingIndicator()
        }
    }
}

@Composable
private fun MomentBlockUserLoadingIndicator() {
    CircularProgressIndicator(
        modifier = Modifier
            .progressSemantics()
            .size(20.dp),
        strokeWidth = 2.dp,
    )
}
