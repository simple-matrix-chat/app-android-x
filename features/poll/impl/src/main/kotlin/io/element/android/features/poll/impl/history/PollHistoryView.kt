/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.poll.impl.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.poll.api.pollcontent.PollContentView
import io.element.android.features.poll.impl.R
import io.element.android.features.poll.impl.history.model.PollHistoryFilter
import io.element.android.features.poll.impl.history.model.PollHistoryItem
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.OutlinedButton
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.SegmentedButton
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollHistoryView(
    state: PollHistoryState,
    onEditPoll: (EventId) -> Unit,
    goBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    fun onLoadMore() {
        state.eventSink(PollHistoryEvents.LoadMore)
    }

    fun onSelectAnswer(pollStartId: EventId, answerId: String) {
        state.eventSink(PollHistoryEvents.SelectPollAnswer(pollStartId, answerId))
    }

    fun onEndPoll(pollStartId: EventId) {
        state.eventSink(PollHistoryEvents.EndPoll(pollStartId))
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding(),
        containerColor = ElementTheme.colors.bgSubtleSecondary,
        topBar = {
            PollHistoryTopBar(
                title = stringResource(R.string.screen_polls_history_title),
                onBackClick = goBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .fillMaxSize()
        ) {
            val pagerState = rememberPagerState(state.activeFilter.ordinal, 0f) {
                PollHistoryFilter.entries.size
            }
            LaunchedEffect(state.activeFilter) {
                pagerState.scrollToPage(state.activeFilter.ordinal)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                PollHistoryFilterButtons(
                    activeFilter = state.activeFilter,
                    onSelectFilter = { state.eventSink(PollHistoryEvents.SelectFilter(it)) },
                    modifier = Modifier
                        .widthIn(max = PollHistoryMaxContentWidth)
                        .fillMaxWidth(),
                )
            }
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val filter = PollHistoryFilter.entries[page]
                val pollHistoryItems = state.pollHistoryForFilter(filter)
                PollHistoryList(
                    filter = filter,
                    pollHistoryItems = pollHistoryItems,
                    hasMoreToLoad = state.hasMoreToLoad,
                    isLoading = state.isLoading,
                    onSelectAnswer = ::onSelectAnswer,
                    onEditPoll = onEditPoll,
                    onEndPoll = ::onEndPoll,
                    onLoadMore = ::onLoadMore,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun PollHistoryTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp),
            onClick = onBackClick,
        ) {
            Icon(
                imageVector = CompoundIcons.ChevronLeft(),
                contentDescription = stringResource(CommonStrings.action_back),
                tint = ElementTheme.colors.iconPrimary,
            )
        }
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 64.dp)
                .semantics { heading() },
            text = title,
            style = ElementTheme.typography.fontHeadingSmMedium,
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PollHistoryFilterButtons(
    activeFilter: PollHistoryFilter,
    onSelectFilter: (PollHistoryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        PollHistoryFilter.entries.forEach { filter ->
            SegmentedButton(
                index = filter.ordinal,
                count = PollHistoryFilter.entries.size,
                selected = activeFilter == filter,
                onClick = { onSelectFilter(filter) },
                text = stringResource(filter.stringResource),
            )
        }
    }
}

@Composable
private fun PollHistoryList(
    filter: PollHistoryFilter,
    pollHistoryItems: ImmutableList<PollHistoryItem>,
    hasMoreToLoad: Boolean,
    isLoading: Boolean,
    onSelectAnswer: (pollStartId: EventId, answerId: String) -> Unit,
    onEditPoll: (pollStartId: EventId) -> Unit,
    onEndPoll: (pollStartId: EventId) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    LazyColumn(
        state = lazyListState,
        modifier = modifier.navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(pollHistoryItems) { pollHistoryItem ->
            PollHistoryItemRow(
                pollHistoryItem = pollHistoryItem,
                onSelectAnswer = onSelectAnswer,
                onEditPoll = onEditPoll,
                onEndPoll = onEndPoll,
                modifier = Modifier
                    .widthIn(max = PollHistoryMaxContentWidth)
                    .fillMaxWidth()
            )
        }
        if (pollHistoryItems.isEmpty()) {
            item {
                EmptyPollHistoryMessage(
                    filter = filter,
                    modifier = Modifier
                        .widthIn(max = PollHistoryMaxContentWidth)
                        .fillMaxWidth()
                )
            }
        }
        if (hasMoreToLoad) {
            item {
                LoadMoreButton(
                    isLoading = isLoading,
                    onClick = onLoadMore,
                    modifier = Modifier.padding(top = if (pollHistoryItems.isEmpty()) 0.dp else 16.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyPollHistoryMessage(
    filter: PollHistoryFilter,
    modifier: Modifier = Modifier,
) {
    val emptyStringResource = if (filter == PollHistoryFilter.PAST) {
        stringResource(R.string.screen_polls_history_empty_past)
    } else {
        stringResource(R.string.screen_polls_history_empty_ongoing)
    }
    Text(
        text = emptyStringResource,
        style = ElementTheme.typography.fontBodyLgRegular,
        color = ElementTheme.colors.textSecondary,
        modifier = modifier.padding(top = 48.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun LoadMoreButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        text = stringResource(CommonStrings.action_load_more),
        showProgress = isLoading,
        enabled = !isLoading,
        onClick = onClick,
        modifier = modifier.padding(bottom = 24.dp),
    )
}

@Composable
private fun PollHistoryItemRow(
    pollHistoryItem: PollHistoryItem,
    onSelectAnswer: (pollStartId: EventId, answerId: String) -> Unit,
    onEditPoll: (pollStartId: EventId) -> Unit,
    onEndPoll: (pollStartId: EventId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {
            // Allow the answers to be traversed by Talkback
            isTraversalGroup = true
        },
        color = ElementTheme.colors.bgCanvasDefaultLevel1,
        shape = RoundedCornerShape(size = 12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = pollHistoryItem.formattedDate,
                color = ElementTheme.colors.textSecondary,
                style = ElementTheme.typography.fontBodySmRegular,
            )
            Spacer(modifier = Modifier.height(8.dp))
            PollContentView(
                state = pollHistoryItem.state,
                onSelectAnswer = onSelectAnswer,
                onEditPoll = onEditPoll,
                onEndPoll = onEndPoll,
            )
        }
    }
}

private val PollHistoryMaxContentWidth = 475.dp

@PreviewsDayNight
@Composable
internal fun PollHistoryViewPreview(
    @PreviewParameter(PollHistoryStateProvider::class) state: PollHistoryState
) = ElementPreview {
    PollHistoryView(
        state = state,
        onEditPoll = {},
        goBack = {},
    )
}
