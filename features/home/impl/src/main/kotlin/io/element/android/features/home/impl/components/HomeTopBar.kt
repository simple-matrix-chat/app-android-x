/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.home.impl.HomeNavigationBarItem
import io.element.android.features.home.impl.R
import io.element.android.features.home.impl.filters.RoomListFiltersState
import io.element.android.features.home.impl.filters.RoomListFiltersView
import io.element.android.features.home.impl.filters.aRoomListFiltersState
import io.element.android.features.home.impl.spacefilters.SpaceFiltersEvent
import io.element.android.features.home.impl.spacefilters.SpaceFiltersState
import io.element.android.features.home.impl.spacefilters.aSelectedSpaceFiltersState
import io.element.android.features.home.impl.spacefilters.anUnselectedSpaceFiltersState
import io.element.android.libraries.designsystem.atomic.atoms.RedIndicatorAtom
import io.element.android.libraries.designsystem.components.TopAppBarScrollBehaviorLayout
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.modifiers.backgroundVerticalGradient
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.preview.USER_NAME_ALICE
import io.element.android.libraries.designsystem.theme.aliasScreenTitle
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.ui.components.aMatrixUser
import io.element.android.libraries.matrix.ui.components.aMatrixUserList
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.testtags.testTag
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private val momentHomeControlShape = RoundedCornerShape(18.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    selectedNavigationItem: HomeNavigationBarItem,
    currentUserAndNeighbors: ImmutableList<MatrixUser>,
    showAvatarIndicator: Boolean,
    areSearchResultsDisplayed: Boolean,
    onToggleSearch: () -> Unit,
    onStartChatClick: () -> Unit,
    onOpenContactsClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onAccountSwitch: (SessionId) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    displayFilters: Boolean,
    filtersState: RoomListFiltersState,
    spaceFiltersState: SpaceFiltersState,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        if (selectedNavigationItem == HomeNavigationBarItem.Chats) {
            MomentHomeTopBar(
                onToggleSearch = onToggleSearch,
                onStartChatClick = onStartChatClick,
                onOpenContactsClick = onOpenContactsClick,
                spaceFiltersState = spaceFiltersState,
            )
        } else {
            TopAppBar(
                modifier = Modifier
                    .backgroundVerticalGradient(
                        isVisible = !areSearchResultsDisplayed,
                    )
                    .statusBarsPadding(),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
                title = {
                    val displayTitle = stringResource(selectedNavigationItem.labelRes)
                    Text(
                        modifier = Modifier.semantics {
                            heading()
                        },
                        style = ElementTheme.typography.aliasScreenTitle,
                        text = displayTitle,
                    )
                },
                navigationIcon = {
                    NavigationIcon(
                        currentUserAndNeighbors = currentUserAndNeighbors,
                        showAvatarIndicator = showAvatarIndicator,
                        onAccountSwitch = onAccountSwitch,
                        onClick = onOpenSettings,
                    )
                },
                // We want a 16dp left padding for the navigationIcon :
                // 4dp from default TopAppBarHorizontalPadding
                // 8dp from AccountIcon default padding (because of IconButton)
                // 4dp extra padding using left insets
                windowInsets = WindowInsets(left = 4.dp),
            )
        }
        if (displayFilters) {
            TopAppBarScrollBehaviorLayout(scrollBehavior = scrollBehavior) {
                RoomListFiltersView(
                    state = filtersState,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun MomentHomeTopBar(
    onToggleSearch: () -> Unit,
    onStartChatClick: () -> Unit,
    onOpenContactsClick: () -> Unit,
    spaceFiltersState: SpaceFiltersState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MomentSearchPill(
            onClick = onToggleSearch,
            modifier = Modifier.weight(1f),
        )
        MomentHomeActionButton(
            imageVector = CompoundIcons.Compose(),
            contentDescription = stringResource(CommonStrings.action_start_chat),
            onClick = onStartChatClick,
        )
        MomentHomeActionButton(
            imageVector = CompoundIcons.Chat(),
            contentDescription = stringResource(R.string.action_open_contacts),
            onClick = onOpenContactsClick,
        )
        MomentSpaceFilterButton(spaceFiltersState = spaceFiltersState)
    }
}

@Composable
private fun MomentSearchPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(momentHomeControlShape)
            .background(ElementTheme.colors.bgSubtleSecondary)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = CompoundIcons.Search(),
            contentDescription = null,
            tint = ElementTheme.colors.iconSecondary,
        )
        Text(
            text = stringResource(CommonStrings.action_search),
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun MomentHomeActionButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    IconButton(
        modifier = modifier
            .size(48.dp)
            .clip(momentHomeControlShape)
            .background(
                color = if (isSelected) {
                    ElementTheme.colors.bgActionPrimaryRest
                } else {
                    ElementTheme.colors.bgSubtleSecondary
                },
            ),
        onClick = onClick,
        colors = if (isSelected) {
            IconButtonDefaults.iconButtonColors(
                contentColor = ElementTheme.colors.iconOnSolidPrimary,
            )
        } else {
            IconButtonDefaults.iconButtonColors(
                contentColor = ElementTheme.colors.iconPrimary,
            )
        },
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
        )
    }
}

@Composable
private fun MomentSpaceFilterButton(
    spaceFiltersState: SpaceFiltersState,
) {
    if (spaceFiltersState == SpaceFiltersState.Disabled) return

    fun onClick() {
        when (spaceFiltersState) {
            is SpaceFiltersState.Unselected -> spaceFiltersState.eventSink(SpaceFiltersEvent.Unselected.ShowFilters)
            is SpaceFiltersState.Selected -> spaceFiltersState.eventSink(SpaceFiltersEvent.Selected.ClearSelection)
            else -> Unit
        }
    }

    MomentHomeActionButton(
        imageVector = CompoundIcons.Filter(),
        contentDescription = stringResource(R.string.screen_roomlist_your_spaces),
        onClick = ::onClick,
        isSelected = spaceFiltersState is SpaceFiltersState.Selected,
    )
}

@Composable
private fun NavigationIcon(
    currentUserAndNeighbors: ImmutableList<MatrixUser>,
    showAvatarIndicator: Boolean,
    onAccountSwitch: (SessionId) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (currentUserAndNeighbors.size == 1) {
        AccountIcon(
            matrixUser = currentUserAndNeighbors.single(),
            isCurrentAccount = true,
            showAvatarIndicator = showAvatarIndicator,
            onClick = onClick,
            modifier = modifier,
        )
    } else {
        // Render a vertical pager
        val pagerState = rememberPagerState(initialPage = 1) { currentUserAndNeighbors.size }
        // Listen to page changes and switch account if needed
        val latestOnAccountSwitch by rememberUpdatedState(onAccountSwitch)
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.collect { page ->
                latestOnAccountSwitch(SessionId(currentUserAndNeighbors[page].userId.value))
            }
        }
        VerticalPager(
            state = pagerState,
            modifier = modifier.height(48.dp),
        ) { page ->
            AccountIcon(
                matrixUser = currentUserAndNeighbors[page],
                isCurrentAccount = page == 1,
                showAvatarIndicator = page == 1 && showAvatarIndicator,
                onClick = if (page == 1) {
                    onClick
                } else {
                    {}
                },
            )
        }
    }
}

@Composable
private fun AccountIcon(
    matrixUser: MatrixUser,
    isCurrentAccount: Boolean,
    showAvatarIndicator: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val testTag = if (isCurrentAccount) Modifier.testTag(TestTags.homeScreenSettings) else Modifier
    IconButton(
        modifier = modifier.then(testTag),
        onClick = onClick,
    ) {
        Box {
            val avatarData by remember(matrixUser) {
                derivedStateOf {
                    matrixUser.getAvatarData(size = AvatarSize.CurrentUserTopBar)
                }
            }
            Avatar(
                avatarData = avatarData,
                avatarType = AvatarType.User,
                contentDescription = if (isCurrentAccount) {
                    if (showAvatarIndicator) {
                        stringResource(CommonStrings.a11y_settings_with_required_action)
                    } else {
                        stringResource(CommonStrings.common_settings)
                    }
                } else {
                    null
                },
            )
            if (showAvatarIndicator) {
                RedIndicatorAtom(
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewsDayNight
@Composable
internal fun HomeTopBarPreview() = ElementPreview {
    HomeTopBar(
        selectedNavigationItem = HomeNavigationBarItem.Chats,
        currentUserAndNeighbors = persistentListOf(aMatrixUser(id = "@id:domain", displayName = USER_NAME_ALICE)),
        showAvatarIndicator = false,
        areSearchResultsDisplayed = false,
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
        onOpenSettings = {},
        onAccountSwitch = {},
        onToggleSearch = {},
        onStartChatClick = {},
        onOpenContactsClick = {},
        displayFilters = true,
        filtersState = aRoomListFiltersState(),
        spaceFiltersState = anUnselectedSpaceFiltersState(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewsDayNight
@Composable
internal fun HomeTopBarSpaceFiltersSelectedPreview() = ElementPreview {
    HomeTopBar(
        selectedNavigationItem = HomeNavigationBarItem.Chats,
        currentUserAndNeighbors = persistentListOf(aMatrixUser(id = "@id:domain", displayName = USER_NAME_ALICE)),
        showAvatarIndicator = false,
        areSearchResultsDisplayed = false,
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
        onOpenSettings = {},
        onAccountSwitch = {},
        onToggleSearch = {},
        onStartChatClick = {},
        onOpenContactsClick = {},
        displayFilters = true,
        filtersState = aRoomListFiltersState(),
        spaceFiltersState = aSelectedSpaceFiltersState(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewsDayNight
@Composable
internal fun HomeTopBarSpacesPreview() = ElementPreview {
    HomeTopBar(
        selectedNavigationItem = HomeNavigationBarItem.Spaces,
        currentUserAndNeighbors = persistentListOf(aMatrixUser(id = "@id:domain", displayName = USER_NAME_ALICE)),
        showAvatarIndicator = false,
        areSearchResultsDisplayed = false,
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
        onOpenSettings = {},
        onAccountSwitch = {},
        onToggleSearch = {},
        onStartChatClick = {},
        onOpenContactsClick = {},
        displayFilters = false,
        filtersState = aRoomListFiltersState(),
        spaceFiltersState = anUnselectedSpaceFiltersState(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewsDayNight
@Composable
internal fun HomeTopBarWithIndicatorPreview() = ElementPreview {
    HomeTopBar(
        selectedNavigationItem = HomeNavigationBarItem.Chats,
        currentUserAndNeighbors = persistentListOf(aMatrixUser(id = "@id:domain", displayName = USER_NAME_ALICE)),
        showAvatarIndicator = true,
        areSearchResultsDisplayed = false,
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
        onOpenSettings = {},
        onAccountSwitch = {},
        onToggleSearch = {},
        onStartChatClick = {},
        onOpenContactsClick = {},
        displayFilters = true,
        filtersState = aRoomListFiltersState(),
        spaceFiltersState = anUnselectedSpaceFiltersState(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewsDayNight
@Composable
internal fun HomeTopBarMultiAccountPreview() = ElementPreview {
    HomeTopBar(
        selectedNavigationItem = HomeNavigationBarItem.Chats,
        currentUserAndNeighbors = aMatrixUserList().take(3).toImmutableList(),
        showAvatarIndicator = false,
        areSearchResultsDisplayed = false,
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
        onOpenSettings = {},
        onAccountSwitch = {},
        onToggleSearch = {},
        onStartChatClick = {},
        onOpenContactsClick = {},
        displayFilters = true,
        filtersState = aRoomListFiltersState(),
        spaceFiltersState = anUnselectedSpaceFiltersState(),
    )
}
