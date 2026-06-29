/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.sessions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.architecture.coverage.ExcludeFromCoverage
import io.element.android.libraries.designsystem.components.dialogs.ConfirmationDialog
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarHost
import io.element.android.libraries.designsystem.utils.snackbar.rememberSnackbarHostState
import io.element.android.libraries.matrix.api.session.MatrixSessionDevice
import io.element.android.libraries.ui.strings.CommonStrings
import java.text.DateFormat
import java.util.Date

@Composable
fun MomentSessionsView(
    state: MomentSessionsState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = rememberSnackbarHostState(snackbarMessage = state.snackbarMessage)
    var sessionPendingTermination by remember { mutableStateOf<MatrixSessionDevice?>(null) }

    sessionPendingTermination?.let { session ->
        ConfirmationDialog(
            title = stringResource(R.string.screen_moment_sessions_terminate_confirm_title),
            content = stringResource(R.string.screen_moment_sessions_terminate_confirm_message),
            submitText = stringResource(R.string.screen_moment_sessions_terminate_action),
            destructiveSubmit = true,
            onSubmitClick = {
                state.eventSink(MomentSessionsEvent.TerminateSession(session.deviceId))
                sessionPendingTermination = null
            },
            onDismiss = { sessionPendingTermination = null },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ElementTheme.colors.bgSubtleSecondary,
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.navigationBarsPadding(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            MomentSessionsTopBar(
                isRefreshing = state.isLoading,
                onBackClick = onBackClick,
                onRefreshClick = { state.eventSink(MomentSessionsEvent.Refresh) },
            )
            Text(
                text = stringResource(R.string.screen_moment_sessions_description),
                style = ElementTheme.typography.fontBodyMdRegular,
                color = ElementTheme.colors.textSecondary,
            )

            if (state.isInitialLoading) {
                MomentSessionsLoadingCard()
            } else {
                if (state.hasLoadError) {
                    MomentSessionsErrorCard(
                        message = stringResource(R.string.screen_moment_sessions_load_failed),
                        onRetryClick = { state.eventSink(MomentSessionsEvent.Refresh) },
                    )
                }

                state.currentSession?.let { currentSession ->
                    MomentSessionsSection(
                        title = stringResource(R.string.screen_moment_sessions_current_title),
                        sessions = listOf(currentSession),
                        terminatingDeviceId = state.terminatingDeviceId?.value,
                        rowsEnabled = state.rowsEnabled,
                    )
                }

                MomentSessionsSection(
                    title = stringResource(R.string.screen_moment_sessions_other_title),
                    sessions = state.otherSessions,
                    emptyText = stringResource(R.string.screen_moment_sessions_no_other_sessions),
                    terminatingDeviceId = state.terminatingDeviceId?.value,
                    rowsEnabled = state.rowsEnabled,
                    onTerminate = { sessionPendingTermination = it },
                )
            }
        }
    }
}

@Composable
private fun MomentSessionsTopBar(
    isRefreshing: Boolean,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        MomentSessionsIconButton(
            modifier = Modifier.align(Alignment.CenterStart),
            enabled = true,
            imageVector = CompoundIcons.ChevronLeft(),
            contentDescription = stringResource(CommonStrings.action_back),
            onClick = onBackClick,
        )
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp),
            text = stringResource(R.string.screen_moment_sessions_title),
            style = ElementTheme.typography.fontBodyLgMedium.copy(fontWeight = FontWeight.Bold),
            color = ElementTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                MomentSessionsIconButton(
                    enabled = true,
                    imageVector = CompoundIcons.Restart(),
                    contentDescription = stringResource(CommonStrings.action_retry),
                    onClick = onRefreshClick,
                )
            }
        }
    }
}

@Composable
private fun MomentSessionsLoadingCard() {
    MomentSessionsCard {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(CommonStrings.common_loading),
                style = ElementTheme.typography.fontBodyMdRegular,
                color = ElementTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun MomentSessionsErrorCard(
    message: String,
    onRetryClick: () -> Unit,
) {
    MomentSessionsCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = CompoundIcons.ErrorSolid(),
                    contentDescription = null,
                    tint = ElementTheme.colors.iconCriticalPrimary,
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = message,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textPrimary,
                )
            }
            TextButton(
                text = stringResource(CommonStrings.action_retry),
                onClick = onRetryClick,
            )
        }
    }
}

@Composable
private fun MomentSessionsSection(
    title: String,
    sessions: List<MatrixSessionDevice>,
    terminatingDeviceId: String?,
    rowsEnabled: Boolean,
    emptyText: String? = null,
    onTerminate: ((MatrixSessionDevice) -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = ElementTheme.typography.fontBodySmMedium.copy(fontWeight = FontWeight.Bold),
            color = ElementTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        MomentSessionsCard {
            if (sessions.isEmpty() && emptyText != null) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    text = emptyText,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                )
            } else {
                sessions.forEachIndexed { index, session ->
                    MomentSessionRow(
                        session = session,
                        isTerminating = terminatingDeviceId == session.deviceId.value,
                        enabled = rowsEnabled,
                        onTerminate = onTerminate?.let { terminate -> { terminate(session) } },
                    )
                    if (index < sessions.lastIndex) {
                        MomentSessionsRowDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun MomentSessionRow(
    session: MatrixSessionDevice,
    isTerminating: Boolean,
    enabled: Boolean,
    onTerminate: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(ElementTheme.colors.bgSubtleSecondary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = if (session.isCurrent) CompoundIcons.CheckCircleSolid() else CompoundIcons.Computer(),
                contentDescription = null,
                tint = if (session.isCurrent) ElementTheme.colors.iconSuccessPrimary else ElementTheme.colors.iconPrimary,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = session.displayName,
                style = ElementTheme.typography.fontBodyLgMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ElementTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = session.detailText(),
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onTerminate != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(enabled = enabled && !isTerminating, onClick = onTerminate),
                contentAlignment = Alignment.Center,
            ) {
                if (isTerminating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = ElementTheme.colors.iconCriticalPrimary,
                    )
                } else {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = CompoundIcons.SignOut(),
                        contentDescription = stringResource(R.string.screen_moment_sessions_terminate_action),
                        tint = ElementTheme.colors.iconCriticalPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun MatrixSessionDevice.detailText(): String {
    val details = mutableListOf<String>()
    if (isCurrent) {
        details += stringResource(R.string.screen_moment_sessions_current_badge)
    }
    lastSeenTimestamp?.let {
        details += stringResource(R.string.screen_moment_sessions_last_activity_format, formatSessionLastSeen(it))
    }
    lastSeenIp?.let(details::add)
    if (displayName != deviceId.value) {
        details += deviceId.value
    }
    return details.ifEmpty { listOf(deviceId.value) }.joinToString(" · ")
}

private fun formatSessionLastSeen(timestamp: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
}

@Composable
private fun MomentSessionsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.55f)),
    ) {
        Column(content = content)
    }
}

@Composable
private fun MomentSessionsRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp),
        color = ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.22f),
    )
}

@Composable
private fun MomentSessionsIconButton(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = if (enabled) ElementTheme.colors.iconPrimary else ElementTheme.colors.iconDisabled,
        )
    }
}

@PreviewsDayNight
@Composable
internal fun MomentSessionsViewPreview(@PreviewParameter(MomentSessionsStateProvider::class) state: MomentSessionsState) =
    ElementPreview {
        ContentToPreview(state)
    }

@ExcludeFromCoverage
@Composable
private fun ContentToPreview(
    state: MomentSessionsState,
) {
    MomentSessionsView(
        state = state,
        onBackClick = {},
    )
}
