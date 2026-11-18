package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.SnapshotEntity
import com.example.domain.parser.ConnectionFilter
import com.example.ui.components.AccountRowCard
import com.example.ui.components.EmptyArchiveIllustration
import com.example.ui.components.FollowRatioGauge
import com.example.ui.components.formatEpochMillis
import com.example.ui.theme.LocalFollowLensColors
import com.example.ui.viewmodel.FollowLensUiState

@Composable
fun OverviewScreen(
    uiState: FollowLensUiState,
    pendingHandleInput: String,
    onPendingHandleChange: (String) -> Unit,
    onImportClick: () -> Unit,
    onOpenInstagramExportPage: () -> Unit,
    onOpenInstagramProfile: (String) -> Unit,
    onOpenGuide: () -> Unit,
    onSelectSnapshot: (Long) -> Unit,
    onUpdateAccountHandle: (Long, String) -> Unit,
    onOpenFilter: (ConnectionFilter) -> Unit,
    onToggleIgnore: (String) -> Unit,
    onCopiedUsername: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snapshot = uiState.activeSnapshot

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (snapshot == null) {
            EmptyOverviewContent(
                isImporting = uiState.isImporting,
                pendingHandleInput = pendingHandleInput,
                onPendingHandleChange = onPendingHandleChange,
                onImportClick = onImportClick,
                onOpenInstagramExportPage = onOpenInstagramExportPage,
                onOpenGuide = onOpenGuide,
                modifier = Modifier.widthIn(max = 600.dp),
            )
        } else {
            PopulatedOverviewContent(
                uiState = uiState,
                snapshot = snapshot,
                onImportClick = onImportClick,
                onOpenInstagramProfile = onOpenInstagramProfile,
                onSelectSnapshot = onSelectSnapshot,
                onUpdateAccountHandle = onUpdateAccountHandle,
                onOpenFilter = onOpenFilter,
                onToggleIgnore = onToggleIgnore,
                onCopiedUsername = onCopiedUsername,
                modifier = Modifier.widthIn(max = 720.dp),
            )
        }
    }
}

@Composable
private fun EmptyOverviewContent(
    isImporting: Boolean,
    pendingHandleInput: String,
    onPendingHandleChange: (String) -> Unit,
    onImportClick: () -> Unit,
    onOpenInstagramExportPage: () -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantics = LocalFollowLensColors.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Surface(
                shape = CircleShape,
                color = semantics.mutualContainer,
                border = BorderStroke(1.dp, semantics.mutual.copy(alpha = 0.3f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = semantics.mutual,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.privacy_badge),
                        style = MaterialTheme.typography.labelLarge,
                        color = semantics.mutual,
                    )
                }
            }
        }

        item {
            EmptyArchiveIllustration(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp),
            )
        }

        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.empty_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Two quick steps—no Instagram password inside the app. Once downloaded, you can even tap the ZIP in Files to open it straight in Insta-Relapse.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        item {
            OutlinedTextField(
                value = pendingHandleInput,
                onValueChange = onPendingHandleChange,
                label = { Text("Your @username label (optional)") },
                placeholder = { Text("e.g. juan_delacruz") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.AlternateEmail,
                        contentDescription = null,
                    )
                },
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = semantics.subtleBorder,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("empty_account_handle_input"),
            )
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onOpenInstagramExportPage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("open_ig_export_shortcut_button"),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "1. Open Instagram Export Page",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }

                Button(
                    onClick = onImportClick,
                    enabled = !isImporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("import_export_button"),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = if (isImporting) "Parsing Archive…" else "2. ${stringResource(R.string.import_export_button)}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                TextButton(
                    onClick = onOpenGuide,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag("open_guide_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text("See 4-Step Export Guide")
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, semantics.subtleBorder),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Fast Ways to Load Your Account",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    QuickBullet("Tap 'Open Instagram Export Page' → select Followers & following → JSON")
                    QuickBullet("Tap the downloaded .zip in Files → Open with Insta-Relapse")
                    QuickBullet("Or select followers_1.json & following.json right here")
                }
            }
        }
    }
}

@Composable
private fun QuickBullet(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = LocalFollowLensColors.current.mutual,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PopulatedOverviewContent(
    uiState: FollowLensUiState,
    snapshot: SnapshotEntity,
    onImportClick: () -> Unit,
    onOpenInstagramProfile: (String) -> Unit,
    onSelectSnapshot: (Long) -> Unit,
    onUpdateAccountHandle: (Long, String) -> Unit,
    onOpenFilter: (ConnectionFilter) -> Unit,
    onToggleIgnore: (String) -> Unit,
    onCopiedUsername: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantics = LocalFollowLensColors.current
    val spotlightList = uiState.notFollowingBack.filter { !it.isIgnored }.take(5)
    var showEditHandleDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (uiState.snapshots.size > 1) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.snapshots, key = { "switcher_${it.id}" }) { snap ->
                        val chipTitle = if (snap.accountHandle.isNotBlank()) {
                            "@${snap.accountHandle}"
                        } else {
                            snap.label
                        }
                        FilterChip(
                            selected = snap.id == snapshot.id,
                            onClick = { onSelectSnapshot(snap.id) },
                            label = {
                                Text(
                                    text = chipTitle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val displayTitle = if (snapshot.accountHandle.isNotBlank()) {
                            "@${snapshot.accountHandle}"
                        } else {
                            snapshot.label
                        }
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        IconButton(
                            onClick = { showEditHandleDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("edit_account_handle_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Tag @username",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        if (snapshot.accountHandle.isNotBlank()) {
                            IconButton(
                                onClick = { onOpenInstagramProfile(snapshot.accountHandle) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("open_own_profile_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = "Open your Instagram profile",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = if (snapshot.accountHandle.isNotBlank()) {
                            "${snapshot.label} • ${formatEpochMillis(snapshot.importedAt)}"
                        } else {
                            formatEpochMillis(snapshot.importedAt)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Button(
                    onClick = onImportClick,
                    enabled = !uiState.isImporting,
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("import_new_snapshot_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = if (uiState.isImporting) "Importing…" else "Import",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        // Hero Follow-Back Ratio Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, semantics.subtleBorder),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.07f),
                                )
                            )
                        )
                        .padding(20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        FollowRatioGauge(
                            mutualCount = snapshot.mutualCount,
                            notFollowingBackCount = snapshot.notFollowingBackCount,
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "Follow-Back Health",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            RatioLegendRow(
                                color = semantics.mutual,
                                label = "Mutuals",
                                value = snapshot.mutualCount,
                            )
                            RatioLegendRow(
                                color = semantics.notFollowingBack,
                                label = "Not Following Back",
                                value = snapshot.notFollowingBackCount,
                            )
                            RatioLegendRow(
                                color = semantics.fan,
                                label = "Fans (You don't follow)",
                                value = snapshot.fansCount,
                            )
                        }
                    }
                }
            }
        }

        // 2x2 Bento Metric Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MetricBentoCard(
                        title = "Not Following Back",
                        count = snapshot.notFollowingBackCount,
                        subtitle = if (uiState.activeIgnoredInNotFollowingCount > 0) {
                            "${uiState.activeIgnoredInNotFollowingCount} ignored"
                        } else {
                            "Review list"
                        },
                        accentColor = semantics.notFollowingBack,
                        icon = Icons.Rounded.PersonRemove,
                        onClick = { onOpenFilter(ConnectionFilter.NOT_FOLLOWING_BACK) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_not_following_back"),
                    )
                    MetricBentoCard(
                        title = "Mutuals",
                        count = snapshot.mutualCount,
                        subtitle = "Follow each other",
                        accentColor = semantics.mutual,
                        icon = Icons.Rounded.Group,
                        onClick = { onOpenFilter(ConnectionFilter.MUTUALS) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_mutuals"),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MetricBentoCard(
                        title = "Fans",
                        count = snapshot.fansCount,
                        subtitle = "Follow you back-only",
                        accentColor = semantics.fan,
                        icon = Icons.Rounded.Star,
                        onClick = { onOpenFilter(ConnectionFilter.FANS) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_fans"),
                    )
                    MetricBentoCard(
                        title = "Followers",
                        count = snapshot.followersCount,
                        subtitle = "${snapshot.followingCount} following",
                        accentColor = semantics.followersAccent,
                        icon = Icons.Rounded.CheckCircle,
                        onClick = { onOpenFilter(ConnectionFilter.FOLLOWERS) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_followers"),
                    )
                }
            }
        }

        // Spotlight Preview Section
        if (spotlightList.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Not Following Back Preview",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    TextButton(
                        onClick = { onOpenFilter(ConnectionFilter.NOT_FOLLOWING_BACK) },
                        modifier = Modifier.testTag("spotlight_view_all"),
                    ) {
                        Text("View All")
                        Spacer(Modifier.size(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            items(spotlightList, key = { "spot_${it.username}" }) { item ->
                AccountRowCard(
                    item = item,
                    accentColor = semantics.notFollowingBack,
                    showIgnoreAction = true,
                    onToggleIgnore = { onToggleIgnore(item.username) },
                    onCopied = onCopiedUsername,
                )
            }
        }
    }

    if (showEditHandleDialog) {
        var handleDraft by remember(snapshot.id) { mutableStateOf(snapshot.accountHandle) }
        AlertDialog(
            onDismissRequest = { showEditHandleDialog = false },
            title = { Text("Tag Account @username") },
            text = {
                OutlinedTextField(
                    value = handleDraft,
                    onValueChange = { handleDraft = it },
                    label = { Text("Instagram @username") },
                    placeholder = { Text("e.g. juan_delacruz") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.AlternateEmail,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateAccountHandle(snapshot.id, handleDraft)
                        showEditHandleDialog = false
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditHandleDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun RatioLegendRow(
    color: Color,
    label: String,
    value: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun MetricBentoCard(
    title: String,
    count: Int,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantics = LocalFollowLensColors.current
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, semantics.subtleBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
