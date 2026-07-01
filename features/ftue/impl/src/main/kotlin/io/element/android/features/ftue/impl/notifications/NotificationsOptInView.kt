/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.ftue.impl.notifications

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.ftue.impl.R
import io.element.android.libraries.designsystem.atomic.molecules.ButtonColumnMolecule
import io.element.android.libraries.designsystem.components.BigIcon
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun NotificationsOptInView(
    state: NotificationsOptInState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ElementTheme.colors.bgCanvasDefault)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(0.24f))
        NotificationsOptInContent(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
        )
        Spacer(modifier = Modifier.weight(0.16f))
        NotificationsOptInFooter(state)
    }
}

@Composable
private fun NotificationsOptInFooter(state: NotificationsOptInState) {
    ButtonColumnMolecule(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp),
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(CommonStrings.action_ok),
            onClick = {
                state.eventSink(NotificationsOptInEvents.ContinueClicked)
            },
        )
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(CommonStrings.action_not_now),
            onClick = {
                state.eventSink(NotificationsOptInEvents.NotNowClicked)
            },
        )
    }
}

@Composable
private fun NotificationsOptInContent(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.widthIn(max = 560.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BigIcon(
            style = BigIcon.Style.Default(CompoundIcons.NotificationsSolid()),
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Text(
            text = stringResource(R.string.screen_notification_optin_title),
            style = ElementTheme.typography.fontHeadingMdBold,
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.screen_notification_optin_subtitle),
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        NotificationsPromptGraphic(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        )
    }
}

@Composable
private fun NotificationsPromptGraphic(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.widthIn(max = 393.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        NotificationRow(
            avatarLetter = "M",
            avatarColorsId = "5",
            firstRowPercent = 1f,
            secondRowPercent = 0.4f,
        )

        NotificationRow(
            avatarLetter = "A",
            avatarColorsId = "1",
            firstRowPercent = 1f,
            secondRowPercent = 1f,
        )

        NotificationRow(
            avatarLetter = "T",
            avatarColorsId = "4",
            firstRowPercent = 0.65f,
            secondRowPercent = 0f,
        )
    }
}

@Composable
private fun NotificationRow(
    avatarLetter: String,
    avatarColorsId: String,
    firstRowPercent: Float,
    secondRowPercent: Float,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ElementTheme.colors.bgCanvasDefault,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                avatarData = AvatarData(id = avatarColorsId, name = avatarLetter, size = AvatarSize.NotificationsOptIn),
                avatarType = AvatarType.User,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .fillMaxWidth(firstRowPercent)
                        .height(10.dp)
                        .background(NotificationPlaceholderColor),
                )
                if (secondRowPercent > 0f) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .fillMaxWidth(secondRowPercent)
                            .height(10.dp)
                            .background(NotificationPlaceholderColor),
                    )
                }
            }
        }
    }
}

private val NotificationPlaceholderColor = Color(0x1F052E61)

@PreviewsDayNight
@Composable
internal fun NotificationsOptInViewPreview(
    @PreviewParameter(NotificationsOptInStateProvider::class) state: NotificationsOptInState,
) {
    ElementPreview {
        NotificationsOptInView(
            onBack = {},
            state = state,
        )
    }
}
