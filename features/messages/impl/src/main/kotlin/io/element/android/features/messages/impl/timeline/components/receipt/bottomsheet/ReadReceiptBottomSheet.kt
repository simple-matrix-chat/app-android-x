/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.receipt.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.messages.impl.timeline.model.ReadReceiptData
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.components.avatar.getBestName
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReadReceiptBottomSheet(
    state: ReadReceiptBottomSheetState,
    onUserDataClick: (UserId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isVisible = state.selectedEvent != null

    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    if (isVisible) {
        ModalBottomSheet(
            modifier = modifier,
//            modifier = modifier.navigationBarsPadding() - FIXME after https://issuetracker.google.com/issues/275849044
//                    .imePadding()
            sheetState = sheetState,
            onDismissRequest = {
                coroutineScope.launch {
                    sheetState.hide()
                    state.eventSink(ReadReceiptBottomSheetEvent.Dismiss)
                }
            },
            scrollable = false,
        ) {
            ReadReceiptBottomSheetContent(
                state = state,
                onUserDataClick = {
                    coroutineScope.launch {
                        sheetState.hide()
                        state.eventSink(ReadReceiptBottomSheetEvent.Dismiss)
                        onUserDataClick.invoke(it)
                    }
                },
            )
            // FIXME remove after https://issuetracker.google.com/issues/275849044
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ReadReceiptBottomSheetContent(
    state: ReadReceiptBottomSheetState,
    onUserDataClick: (UserId) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(ElementTheme.colors.bgCanvasDefault),
        contentPadding = PaddingValues(top = 24.dp),
    ) {
        item {
            Text(
                text = stringResource(id = CommonStrings.common_seen_by),
                style = ElementTheme.typography.fontBodyLgMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ElementTheme.colors.textPrimary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(
            items = state.selectedEvent?.readReceiptState?.receipts.orEmpty()
        ) {
            ReadReceiptRow(
                readReceiptData = it,
                onUserDataClick = onUserDataClick,
            )
        }
    }
}

@Composable
private fun ReadReceiptRow(
    readReceiptData: ReadReceiptData,
    onUserDataClick: (UserId) -> Unit,
) {
    val avatarData = readReceiptData.avatarData
    val userId = UserId(avatarData.id)
    val title = avatarData.getBestName()
    val subtitle = avatarData.name?.takeUnless { it.isBlank() }?.let { avatarData.id }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserDataClick(userId) }
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            avatarData = avatarData.copy(size = AvatarSize.ReadReceiptList),
            avatarType = AvatarType.User,
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = ElementTheme.typography.fontBodyMdMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ElementTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (readReceiptData.formattedDate.isNotBlank()) {
                    Text(
                        text = readReceiptData.formattedDate,
                        style = ElementTheme.typography.fontBodyXsRegular,
                        color = ElementTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            subtitle?.let {
                Text(
                    text = it,
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@PreviewsDayNight
@Composable
internal fun ReadReceiptBottomSheetPreview(@PreviewParameter(ReadReceiptBottomSheetStateProvider::class) state: ReadReceiptBottomSheetState) = ElementPreview {
    Column {
        ReadReceiptBottomSheetContent(
            state = state,
            onUserDataClick = {},
        )
    }
}
