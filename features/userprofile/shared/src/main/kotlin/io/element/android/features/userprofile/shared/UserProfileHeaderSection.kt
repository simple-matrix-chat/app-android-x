/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.userprofile.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.modifiers.niceClickable
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.preview.USER_NAME_ALICE
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
    openAvatarPreview: (url: String) -> Unit,
    onUserIdClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
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
        Spacer(modifier = Modifier.height(24.dp))
        if (userName != null) {
            Text(
                modifier = Modifier
                    .clipToBounds()
                    .semantics {
                        heading()
                    },
                text = userName,
                style = ElementTheme.typography.fontHeadingLgBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
        Text(
            modifier = Modifier.niceClickable { onUserIdClick() },
            text = userId.value,
            style = ElementTheme.typography.fontBodyLgRegular,
            color = ElementTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
    }
}

@PreviewsDayNight
@Composable
internal fun UserProfileHeaderSectionPreview() = ElementPreview {
    UserProfileHeaderSection(
        avatarUrl = null,
        userId = UserId("@alice:example.com"),
        userName = USER_NAME_ALICE,
        openAvatarPreview = {},
        onUserIdClick = {},
    )
}
