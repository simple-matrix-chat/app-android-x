/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.licenses.impl.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.licenses.impl.model.DependencyLicenseItem
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.FilledTextField
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun DependencyLicensesListView(
    state: DependencyLicensesListState,
    onBackClick: () -> Unit,
    onOpenLicense: (DependencyLicenseItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackClick)

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
                .padding(top = 8.dp)
        ) {
            MomentLicensesTopBar(
                title = stringResource(CommonStrings.common_open_source_licenses),
                onBackClick = onBackClick,
            )
            if (state.licenses.isSuccess()) {
                MomentLicensesSearchField(
                    filter = state.filter,
                    onFilterChange = { state.eventSink(DependencyLicensesListEvent.SetFilter(it)) },
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (state.licenses) {
                    is AsyncData.Failure -> item {
                        MomentLicensesMessageCard(
                            text = stringResource(CommonStrings.common_error),
                            imageVector = CompoundIcons.Info(),
                        )
                    }
                    AsyncData.Uninitialized,
                    is AsyncData.Loading -> item {
                        MomentLicensesLoadingCard()
                    }
                    is AsyncData.Success -> {
                        if (state.licenses.data.isEmpty()) {
                            item {
                                MomentLicensesMessageCard(
                                    text = stringResource(CommonStrings.common_no_results),
                                    imageVector = CompoundIcons.Search(),
                                )
                            }
                        } else {
                            items(
                                items = state.licenses.data,
                                key = { "${it.groupId}:${it.artifactId}:${it.version}" },
                            ) { license ->
                                MomentLicenseRow(
                                    license = license,
                                    onClick = { onOpenLicense(license) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MomentLicensesTopBar(
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
private fun MomentLicensesSearchField(
    filter: String,
    onFilterChange: (String) -> Unit,
) {
    FilledTextField(
        value = filter,
        onValueChange = onFilterChange,
        singleLine = true,
        textStyle = ElementTheme.typography.fontBodyLgRegular.copy(color = ElementTheme.colors.textPrimary),
        placeholder = {
            Text(
                text = stringResource(CommonStrings.action_search),
                style = ElementTheme.typography.fontBodyLgRegular,
                color = ElementTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = {
            Icon(
                modifier = Modifier.size(22.dp),
                imageVector = CompoundIcons.Search(),
                contentDescription = null,
                tint = ElementTheme.colors.iconSecondary,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = ElementTheme.colors.bgCanvasDefault,
            unfocusedContainerColor = ElementTheme.colors.bgCanvasDefault,
            disabledContainerColor = ElementTheme.colors.bgCanvasDefault,
            errorContainerColor = ElementTheme.colors.bgCanvasDefault,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            focusedLeadingIconColor = ElementTheme.colors.iconSecondary,
            unfocusedLeadingIconColor = ElementTheme.colors.iconSecondary,
            focusedPlaceholderColor = ElementTheme.colors.textSecondary,
            unfocusedPlaceholderColor = ElementTheme.colors.textSecondary,
            cursorColor = ElementTheme.colors.textPrimary,
        ),
    )
}

@Composable
private fun MomentLicenseRow(
    license: DependencyLicenseItem,
    onClick: () -> Unit,
) {
    MomentLicensesCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .semantics(mergeDescendants = true) {}
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MomentLicensesIconTile(imageVector = CompoundIcons.Document())
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = license.safeName,
                    style = ElementTheme.typography.fontBodyLgMedium,
                    color = ElementTheme.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = license.coordinates(),
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = CompoundIcons.ChevronRight(),
                contentDescription = null,
                tint = ElementTheme.colors.iconSecondary,
            )
        }
    }
}

@Composable
private fun MomentLicensesMessageCard(
    text: String,
    imageVector: ImageVector,
) {
    MomentLicensesCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MomentLicensesIconTile(imageVector = imageVector)
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
private fun MomentLicensesLoadingCard() {
    MomentLicensesCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun MomentLicensesCard(
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
private fun MomentLicensesIconTile(
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
internal fun DependencyLicensesListViewPreview(
    @PreviewParameter(DependencyLicensesListStateProvider::class) state: DependencyLicensesListState
) = ElementPreview {
    DependencyLicensesListView(
        state = state,
        onBackClick = {},
        onOpenLicense = {},
    )
}
