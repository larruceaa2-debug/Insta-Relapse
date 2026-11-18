package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.parser.ConnectionFilter
import com.example.domain.parser.SortOrder
import com.example.ui.components.AccountRowCard
import com.example.ui.theme.LocalFollowLensColors
import com.example.ui.viewmodel.FollowLensUiState

@Composable
fun ConnectionsScreen(
    uiState: FollowLensUiState,
    onSelectFilter: (ConnectionFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectSortOrder: (SortOrder) -> Unit,
    onToggleHideIgnored: () -> Unit,
    onToggleIgnoreAccount: (String) -> Unit,
    onCopiedUsername: (String) -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantics = LocalFollowLensColors.current
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (uiState.activeSnapshot == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 520.dp)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Rounded.SearchOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(56.dp),
                )
                Spacer(Modifier.size(14.dp))
                Text(
                    text = "No Snapshot Loaded",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Import your Instagram ZIP or JSON export to explore followers, followings, and non-followers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.size(20.dp))
                Button(
                    onClick = onImportClick,
                    modifier = Modifier.testTag("connections_import_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.import_export_button))
                }
            }
        } else {
            val availableFilters = remember(uiState.pendingRequests.size, uiState.recentlyUnfollowed.size) {
                buildList {
                    add(ConnectionFilter.NOT_FOLLOWING_BACK)
                    add(ConnectionFilter.MUTUALS)
                    add(ConnectionFilter.FANS)
                    add(ConnectionFilter.FOLLOWERS)
                    add(ConnectionFilter.FOLLOWING)
                    if (uiState.pendingRequests.isNotEmpty()) add(ConnectionFilter.PENDING)
                    if (uiState.recentlyUnfollowed.isNotEmpty()) add(ConnectionFilter.RECENTLY_UNFOLLOWED)
                }
            }

            val accentColor = when (uiState.selectedFilter) {
                ConnectionFilter.NOT_FOLLOWING_BACK -> semantics.notFollowingBack
                ConnectionFilter.MUTUALS -> semantics.mutual
                ConnectionFilter.FANS -> semantics.fan
                ConnectionFilter.FOLLOWERS -> semantics.followersAccent
                ConnectionFilter.FOLLOWING -> MaterialTheme.colorScheme.secondary
                ConnectionFilter.PENDING -> MaterialTheme.colorScheme.secondary
                ConnectionFilter.RECENTLY_UNFOLLOWED -> MaterialTheme.colorScheme.error
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp),
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(availableFilters, key = { it.name }) { filter ->
                        val count = when (filter) {
                            ConnectionFilter.NOT_FOLLOWING_BACK -> uiState.notFollowingBack.size
                            ConnectionFilter.MUTUALS -> uiState.mutuals.size
                            ConnectionFilter.FANS -> uiState.fans.size
                            ConnectionFilter.FOLLOWERS -> uiState.followers.size
                            ConnectionFilter.FOLLOWING -> uiState.following.size
                            ConnectionFilter.PENDING -> uiState.pendingRequests.size
                            ConnectionFilter.RECENTLY_UNFOLLOWED -> uiState.recentlyUnfollowed.size
                        }
                        val selected = uiState.selectedFilter == filter
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectFilter(filter) },
                            label = { Text("${filter.label} ($count)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}"),
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.search_accounts_hint)) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                )
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = semantics.subtleBorder,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("search_accounts_input"),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${uiState.visibleAccounts.size} accounts",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (uiState.selectedFilter == ConnectionFilter.NOT_FOLLOWING_BACK &&
                            uiState.activeIgnoredInNotFollowingCount > 0
                        ) {
                            FilterChip(
                                selected = uiState.hideIgnored,
                                onClick = onToggleHideIgnored,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.VisibilityOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                label = {
                                    Text(if (uiState.hideIgnored) "Ignored hidden" else "Hide ignored")
                                },
                                modifier = Modifier.testTag("toggle_hide_ignored"),
                            )
                        }

                        Box {
                            OutlinedButton(
                                onClick = { sortMenuExpanded = true },
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, semantics.subtleBorder),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("sort_order_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Sort,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    text = uiState.sortOrder.label,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }

                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false },
                            ) {
                                SortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.label) },
                                        onClick = {
                                            onSelectSortOrder(order)
                                            sortMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.visibleAccounts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(44.dp),
                            )
                            Text(
                                text = "No matching accounts",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (uiState.searchQuery.isNotBlank()) {
                                    "Try clearing or changing your search query."
                                } else {
                                    "No accounts found in this category."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(
                            items = uiState.visibleAccounts,
                            key = { "${uiState.selectedFilter.name}_${it.username}" },
                        ) { item ->
                            AccountRowCard(
                                item = item,
                                accentColor = accentColor,
                                showIgnoreAction = uiState.selectedFilter == ConnectionFilter.NOT_FOLLOWING_BACK,
                                onToggleIgnore = { onToggleIgnoreAccount(item.username) },
                                onCopied = onCopiedUsername,
                            )
                        }
                    }
                }
            }
        }
    }
}
