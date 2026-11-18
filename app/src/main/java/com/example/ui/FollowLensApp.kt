package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.screens.ConnectionsScreen
import com.example.ui.screens.GuideScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.viewmodel.FollowLensViewModel
import kotlinx.coroutines.launch

private sealed class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    data object Overview : TopLevelDestination(
        route = "overview",
        labelRes = R.string.nav_overview,
        selectedIcon = Icons.Rounded.Analytics,
        unselectedIcon = Icons.Outlined.Analytics,
    )

    data object Connections : TopLevelDestination(
        route = "connections",
        labelRes = R.string.nav_connections,
        selectedIcon = Icons.Rounded.Group,
        unselectedIcon = Icons.Outlined.Group,
    )

    data object History : TopLevelDestination(
        route = "history",
        labelRes = R.string.nav_history,
        selectedIcon = Icons.Rounded.History,
        unselectedIcon = Icons.Outlined.History,
    )

    data object Guide : TopLevelDestination(
        route = "guide",
        labelRes = R.string.nav_guide,
        selectedIcon = Icons.Rounded.HelpOutline,
        unselectedIcon = Icons.Outlined.HelpOutline,
    )
}

private val TopLevelDestinations = listOf(
    TopLevelDestination.Overview,
    TopLevelDestination.Connections,
    TopLevelDestination.History,
    TopLevelDestination.Guide,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowLensApp(
    viewModel: FollowLensViewModel,
    isMusicPlaying: Boolean,
    onToggleMusic: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var pendingHandleInput by rememberSaveable { mutableStateOf("") }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                viewModel.importFiles(uris, pendingHandleInput)
            }
        },
    )

    val launchFilePicker: () -> Unit = {
        runCatching {
            documentPickerLauncher.launch(arrayOf("*/*"))
        }.onFailure {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("No file picker found on this device.")
            }
        }
    }

    LaunchedEffect(uiState.bannerMessage, uiState.errorMessage) {
        val message = uiState.errorMessage ?: uiState.bannerMessage
        if (message != null) {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
            viewModel.dismissMessages()
        }
    }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = onToggleMusic,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("music_toggle_button"),
                        ) {
                            Icon(
                                imageVector = if (isMusicPlaying) {
                                    Icons.Rounded.MusicNote
                                } else {
                                    Icons.Rounded.MusicOff
                                },
                                contentDescription = if (isMusicPlaying) {
                                    "Pause background music"
                                } else {
                                    "Play background music"
                                },
                                tint = if (isMusicPlaying) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        TopLevelDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes),
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag("nav_tab_${dest.route}"),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        TopLevelDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes),
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag("nav_rail_${dest.route}"),
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = TopLevelDestination.Overview.route,
                    modifier = Modifier.weight(1f),
                ) {
                    composable(TopLevelDestination.Overview.route) {
                        OverviewScreen(
                            uiState = uiState,
                            pendingHandleInput = pendingHandleInput,
                            onPendingHandleChange = { pendingHandleInput = it },
                            onImportClick = launchFilePicker,
                            onOpenInstagramExportPage = {
                                val opened = openExternalUrl(
                                    context,
                                    "https://accountscenter.instagram.com/info_and_permissions/",
                                )
                                if (!opened) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("No browser app found to open Instagram.")
                                    }
                                }
                            },
                            onOpenInstagramProfile = { username ->
                                val clean = extractInstagramHandle(username)
                                val opened = openExternalUrl(context, "https://www.instagram.com/$clean/")
                                if (!opened) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("No browser app found to open @$clean.")
                                    }
                                }
                            },
                            onOpenGuide = { navigateToTab(TopLevelDestination.Guide.route) },
                            onSelectSnapshot = viewModel::selectSnapshot,
                            onUpdateAccountHandle = { id, raw ->
                                viewModel.updateAccountHandle(id, extractInstagramHandle(raw))
                            },
                            onOpenFilter = { filter ->
                                viewModel.setFilter(filter)
                                navigateToTab(TopLevelDestination.Connections.route)
                            },
                            onToggleIgnore = viewModel::toggleIgnoreAccount,
                            onCopiedUsername = { username ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Copied @$username")
                                }
                            },
                        )
                    }

                    composable(TopLevelDestination.Connections.route) {
                        ConnectionsScreen(
                            uiState = uiState,
                            onSelectFilter = viewModel::setFilter,
                            onSearchQueryChange = viewModel::setSearchQuery,
                            onSelectSortOrder = viewModel::setSortOrder,
                            onToggleHideIgnored = viewModel::toggleHideIgnored,
                            onToggleIgnoreAccount = viewModel::toggleIgnoreAccount,
                            onCopiedUsername = { username ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Copied @$username")
                                }
                            },
                            onImportClick = launchFilePicker,
                        )
                    }

                    composable(TopLevelDestination.History.route) {
                        HistoryScreen(
                            uiState = uiState,
                            onSelectSnapshot = { id ->
                                viewModel.selectSnapshot(id)
                                navigateToTab(TopLevelDestination.Overview.route)
                            },
                            onDeleteSnapshot = viewModel::deleteSnapshot,
                            onClearAll = viewModel::clearAllData,
                            onImportClick = launchFilePicker,
                        )
                    }

                    composable(TopLevelDestination.Guide.route) {
                        GuideScreen(
                            onImportClick = launchFilePicker,
                            onOpenInstagramExportPage = {
                                val opened = openExternalUrl(
                                    context,
                                    "https://accountscenter.instagram.com/info_and_permissions/",
                                )
                                if (!opened) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("No browser app found to open Instagram.")
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun extractInstagramHandle(rawInput: String): String {
    val trimmed = rawInput.trim()
    val urlMatch = Regex("""instagram\.com/(?:_u/)?([A-Za-z0-9._]+)""", RegexOption.IGNORE_CASE)
        .find(trimmed)
    return (urlMatch?.groupValues?.getOrNull(1) ?: trimmed).removePrefix("@").trim()
}

private fun openExternalUrl(context: Context, url: String): Boolean {
    return runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
