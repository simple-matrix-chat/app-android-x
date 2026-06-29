/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.licenses.impl.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.licenses.impl.list.aDependencyLicenseItem
import io.element.android.features.licenses.impl.model.DependencyLicenseItem
import io.element.android.features.licenses.impl.model.License
import io.element.android.libraries.designsystem.components.ClickableLinkText
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun DependenciesDetailsView(
    licenseItem: DependencyLicenseItem,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ElementTheme.colors.bgSubtleSecondary,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp),
        ) {
            MomentLicenseDetailsTopBar(
                title = licenseItem.safeName,
                onBackClick = onBack,
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    DependencySummaryCard(licenseItem = licenseItem)
                }

                val licenses = licenseItem.licenses.orEmpty() +
                    licenseItem.unknownLicenses.orEmpty()
                if (licenses.isEmpty()) {
                    item {
                        MomentLicenseDetailsMessageCard(
                            text = stringResource(CommonStrings.common_no_results),
                            imageVector = CompoundIcons.Info(),
                        )
                    }
                } else {
                    items(licenses) { license ->
                        LicenseTextCard(license = license)
                    }
                }
            }
        }
    }
}

@Composable
private fun MomentLicenseDetailsTopBar(
    title: String,
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
            text = title,
            style = ElementTheme.typography.fontHeadingSmMedium,
            color = ElementTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DependencySummaryCard(
    licenseItem: DependencyLicenseItem,
) {
    MomentLicenseDetailsCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 84.dp)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MomentLicenseDetailsIconTile(imageVector = CompoundIcons.Document())
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = licenseItem.safeName,
                    style = ElementTheme.typography.fontBodyLgMedium,
                    color = ElementTheme.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = licenseItem.coordinates(),
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LicenseTextCard(
    license: License,
) {
    val text = buildString {
        if (license.name != null) {
            append(license.name)
            append("\n")
            append("\n")
        }
        if (license.url != null) {
            append(license.url)
        }
    }
    MomentLicenseDetailsCard {
        ClickableLinkText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            text = text,
            interactionSource = remember { MutableInteractionSource() },
            style = ElementTheme.typography.fontBodyMdRegular.copy(color = ElementTheme.colors.textPrimary),
        )
    }
}

@Composable
private fun MomentLicenseDetailsMessageCard(
    text: String,
    imageVector: ImageVector,
) {
    MomentLicenseDetailsCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MomentLicenseDetailsIconTile(imageVector = imageVector)
            Text(
                modifier = Modifier.weight(1f),
                text = text,
                style = ElementTheme.typography.fontBodyLgMedium,
                color = ElementTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun MomentLicenseDetailsCard(
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.5f)),
    ) {
        content()
    }
}

@Composable
private fun MomentLicenseDetailsIconTile(
    imageVector: ImageVector,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(
                color = ElementTheme.colors.bgSubtleSecondary,
                shape = RoundedCornerShape(10.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = imageVector,
            contentDescription = null,
            tint = ElementTheme.colors.iconPrimary,
        )
    }
}

private fun DependencyLicenseItem.coordinates(): String {
    return "$groupId:$artifactId:$version"
}

@PreviewsDayNight
@Composable
internal fun DependenciesDetailsViewPreview() = ElementPreview {
    DependenciesDetailsView(
        licenseItem = aDependencyLicenseItem(),
        onBack = {},
    )
}
