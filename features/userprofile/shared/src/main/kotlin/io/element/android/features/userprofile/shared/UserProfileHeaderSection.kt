/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.userprofile.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.preview.USER_NAME_ALICE
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.testtags.testTag
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun UserProfileHeaderSection(
    avatarUrl: String?,
    userId: UserId,
    userName: String?,
    username: String?,
    phoneNumber: String?,
    status: String?,
    openAvatarPreview: (url: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, ElementTheme.colors.borderDisabled),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Avatar(
                avatarData = AvatarData(userId.value, userName, avatarUrl, AvatarSize.UserHeader),
                avatarType = AvatarType.User,
                contentDescription = stringResource(CommonStrings.a11y_user_avatar),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        enabled = avatarUrl != null,
                        onClickLabel = stringResource(CommonStrings.action_view),
                    ) {
                        openAvatarPreview(avatarUrl!!)
                    }
                    .testTag(TestTags.memberDetailAvatar)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                modifier = Modifier
                    .clipToBounds()
                    .semantics {
                        heading()
                    },
                text = userName ?: userId.value,
                style = ElementTheme.typography.fontHeadingLgBold,
                color = ElementTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            MomentUserProfileIdentityDetails(
                username = username,
                phoneNumber = phoneNumber,
                status = status,
            )
        }
    }
}

@Composable
private fun MomentUserProfileIdentityDetails(
    username: String?,
    phoneNumber: String?,
    status: String?,
) {
    val detailRows = listOfNotNull(
        username?.trim()?.removePrefix("@")?.takeIf { it.isNotEmpty() }?.let {
            MomentUserProfileIdentityDetail(text = "@$it", isPrimary = true, isStatus = false)
        },
        phoneNumber?.trim()?.takeIf { it.isNotEmpty() }?.let {
            MomentUserProfileIdentityDetail(text = it, isPrimary = false, isStatus = false)
        },
        status?.trim()?.takeIf { it.isNotEmpty() }?.let {
            MomentUserProfileIdentityDetail(text = it, isPrimary = false, isStatus = true)
        },
    )
    if (detailRows.isEmpty()) return

    Spacer(modifier = Modifier.height(8.dp))
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        detailRows.forEach { detail ->
            Text(
                text = detail.text,
                style = if (detail.isPrimary) ElementTheme.typography.fontBodyLgMedium else ElementTheme.typography.fontBodyMdMedium,
                color = if (detail.isPrimary) ElementTheme.colors.textPrimary else ElementTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = if (detail.isStatus) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class MomentUserProfileIdentityDetail(
    val text: String,
    val isPrimary: Boolean,
    val isStatus: Boolean,
)

@PreviewsDayNight
@Composable
internal fun UserProfileHeaderSectionPreview() = ElementPreview {
    UserProfileHeaderSection(
        avatarUrl = null,
        userId = UserId("@alice:example.com"),
        userName = USER_NAME_ALICE,
        username = "alice",
        phoneNumber = "+44 7123 456789",
        status = "Available",
        openAvatarPreview = {},
    )
}
