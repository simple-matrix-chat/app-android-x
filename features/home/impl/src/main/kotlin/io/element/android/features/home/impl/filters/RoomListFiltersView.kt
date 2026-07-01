/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.filters

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.home.impl.R
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.testtags.testTag

@Composable
fun RoomListFiltersView(
    state: RoomListFiltersState,
    modifier: Modifier = Modifier
) {
    fun onClearFiltersClick() {
        state.eventSink(RoomListFiltersEvent.ClearSelectedFilters)
    }

    fun onToggleFilter(filter: RoomListFilter) {
        state.eventSink(RoomListFiltersEvent.ToggleFilter(filter))
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item("all_filter") {
            RoomListFilterTab(
                label = stringResource(id = R.string.screen_roomlist_filter_all),
                selected = state.hasAnyFilterSelected.not(),
                onClick = {
                    if (state.hasAnyFilterSelected) {
                        onClearFiltersClick()
                    }
                },
                modifier = if (state.hasAnyFilterSelected) {
                    Modifier.testTag(TestTags.homeScreenClearFilters)
                } else {
                    Modifier
                }
            )
        }
        state.filterSelectionStates.forEach { filterWithSelection ->
            item(filterWithSelection.filter) {
                RoomListFilterTab(
                    label = stringResource(id = filterWithSelection.filter.stringResource),
                    selected = filterWithSelection.isSelected,
                    onClick = {
                        onToggleFilter(filterWithSelection.filter)
                    },
                )
            }
        }
    }
}

@Composable
private fun RoomListFilterTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val textColour = animateColorAsState(
        targetValue = if (selected) ElementTheme.colors.textPrimary else ElementTheme.colors.textSecondary,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chip text colour",
    )
    val indicatorColour = animateColorAsState(
        targetValue = if (selected) ElementTheme.colors.textPrimary else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "filter indicator colour",
    )

    Column(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .semantics {
                role = Role.Tab
                this.selected = selected
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = label,
            style = if (selected) {
                ElementTheme.typography.fontBodyLgMedium.copy(fontWeight = FontWeight.SemiBold)
            } else {
                ElementTheme.typography.fontBodyLgRegular
            },
            color = textColour.value,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(CircleShape)
                .background(indicatorColour.value)
        )
    }
}

@PreviewsDayNight
@Composable
internal fun RoomListFiltersViewPreview(@PreviewParameter(RoomListFiltersStateProvider::class) state: RoomListFiltersState) = ElementPreview {
    RoomListFiltersView(
        modifier = Modifier.padding(vertical = 4.dp),
        state = state,
    )
}
