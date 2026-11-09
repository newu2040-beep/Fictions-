package com.editingcells.fictions.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.editingcells.fictions.domain.model.SortOption
import com.editingcells.fictions.domain.model.StorageSummary
import com.editingcells.fictions.domain.model.VaultFile
import com.editingcells.fictions.domain.model.VaultFileType
import com.editingcells.fictions.domain.model.VaultFolder
import com.editingcells.fictions.domain.model.ViewMode
import com.editingcells.fictions.ui.components.CircleIconButton
import com.editingcells.fictions.ui.components.EmptyVaultState
import com.editingcells.fictions.ui.components.ErrorStateBanner
import com.editingcells.fictions.ui.components.FileGridCard
import com.editingcells.fictions.ui.components.FileListCard
import com.editingcells.fictions.ui.components.FolderGridCard
import com.editingcells.fictions.ui.components.FooterCredit
import com.editingcells.fictions.ui.components.GlassSurface
import com.editingcells.fictions.ui.components.LoadingSkeletonList
import com.editingcells.fictions.ui.components.PillSearchBar
import com.editingcells.fictions.ui.components.StorageSummarySection
import com.editingcells.fictions.ui.components.TypeFilterChipsRow
import com.editingcells.fictions.ui.theme.LocalResponsiveLayout
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    var startAnim by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.82f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "splash_scale"
    )

    LaunchedEffect(Unit) {
        startAnim = true
        delay(1050)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.scale(scale)
        ) {
            GlassSurface(
                shape = RoundedCornerShape(32.dp),
                refractionHeight = 20.dp,
                refractionAmount = 32.dp,
                modifier = Modifier.size(108.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "F",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp),
                        color = Color.White
                    )
                }
            }

            AnimatedVisibility(visible = startAnim, enter = fadeIn(tween(600))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "FICTIONS",
                        style = MaterialTheme.typography.displayMedium.copy(
                            letterSpacing = 3.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Liquid Glass Document Sanctuary",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    storageSummary: StorageSummary,
    recentFiles: List<VaultFile>,
    favoriteFiles: List<VaultFile>,
    folders: List<VaultFolder>,
    selectedTypeFilter: VaultFileType?,
    selectedFileIds: Set<Long>,
    isLoading: Boolean,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onSelectTypeFilter: (VaultFileType?) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLibraryWithType: (VaultFileType?) -> Unit,
    onOpenFolder: (VaultFolder) -> Unit,
    onOpenAllFolders: () -> Unit,
    onNewFolder: () -> Unit,
    onAddClick: () -> Unit,
    onFileClick: (VaultFile) -> Unit,
    onFileLongClick: (VaultFile) -> Unit,
    onToggleFavorite: (VaultFile) -> Unit,
    onOpenFileMenu: (VaultFile) -> Unit,
    onScrollDirectionChanged: (Boolean) -> Unit,
    onScrollPhaseChanged: (Float) -> Unit = {}
) {
    val responsive = LocalResponsiveLayout.current
    val gridState = rememberLazyGridState()
    var prevIndex by remember { mutableIntStateOf(0) }
    var prevOffset by remember { mutableIntStateOf(0) }

    val isScrollingUp by remember {
        derivedStateOf {
            if (gridState.firstVisibleItemIndex != prevIndex) {
                val up = gridState.firstVisibleItemIndex < prevIndex
                prevIndex = gridState.firstVisibleItemIndex
                prevOffset = gridState.firstVisibleItemScrollOffset
                up
            } else {
                val up = gridState.firstVisibleItemScrollOffset <= prevOffset
                prevOffset = gridState.firstVisibleItemScrollOffset
                up
            }
        }
    }

    val scrollPhase by remember {
        derivedStateOf {
            ((gridState.firstVisibleItemIndex * 240 + gridState.firstVisibleItemScrollOffset) % 1000) / 1000f
        }
    }

    LaunchedEffect(isScrollingUp) {
        onScrollDirectionChanged(isScrollingUp)
    }

    LaunchedEffect(scrollPhase) {
        onScrollPhaseChanged(scrollPhase)
    }

    val selectionMode = selectedFileIds.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout)),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = responsive.folderMinGridWidth),
            modifier = Modifier
                .widthIn(max = responsive.contentMaxWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = responsive.screenMargin,
                end = responsive.screenMargin,
                top = responsive.space16,
                bottom = responsive.bottomBarClearance
            ),
            horizontalArrangement = Arrangement.spacedBy(responsive.space12),
            verticalArrangement = Arrangement.spacedBy(responsive.space16)
        ) {
            // Top Header: Big bold greeting headline + circular Favorites & Settings buttons
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = "FICTIONS VAULT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.6.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Curated Archives",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircleIconButton(
                            icon = Icons.Default.Star,
                            contentDescription = "Favorites",
                            contentColor = Color(0xFFF59E0B),
                            onClick = onOpenFavorites
                        )
                        CircleIconButton(
                            icon = Icons.Default.Settings,
                            contentDescription = "Settings",
                            onClick = onOpenSettings
                        )
                    }
                }
            }

            // Pill Search Field
            item(span = { GridItemSpan(maxLineSpan) }) {
                PillSearchBar(
                    query = "",
                    onQueryChange = {},
                    readOnly = true,
                    onClick = onOpenSearch
                )
            }

            // Pill Filter Chips (All, PDF, CSV, TXT) with counts
            item(span = { GridItemSpan(maxLineSpan) }) {
                TypeFilterChipsRow(
                    selectedType = selectedTypeFilter,
                    totalCount = storageSummary.totalCount,
                    pdfCount = storageSummary.pdfCount,
                    csvCount = storageSummary.csvCount,
                    txtCount = storageSummary.txtCount,
                    onSelectType = onSelectTypeFilter
                )
            }

            if (errorMessage != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ErrorStateBanner(message = errorMessage, onDismiss = onDismissError)
                }
            }

            // Storage Summary Card with circular progress ring & 3 stat cards
            item(span = { GridItemSpan(maxLineSpan) }) {
                StorageSummarySection(
                    summary = storageSummary,
                    onStatClick = { type -> onOpenLibraryWithType(type) }
                )
            }

            // Recent Files Horizontal Carousel
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(responsive.space12)) {
                    SectionHeader(
                        title = "Recent Files",
                        actionText = "See All",
                        onActionClick = { onOpenLibraryWithType(selectedTypeFilter) }
                    )
                    if (isLoading) {
                        LoadingSkeletonList(count = 2)
                    } else if (recentFiles.isEmpty()) {
                        EmptyVaultState(
                            title = "Your Vault is Ready",
                            subtitle = "Import PDFs, CSV tables, or TXT notes using the + button below.",
                            actionLabel = "Add Files",
                            onActionClick = onAddClick
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(responsive.space12),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(recentFiles, key = { "recent_${it.id}" }) { file ->
                                FileGridCard(
                                    file = file,
                                    isSelected = file.id in selectedFileIds,
                                    selectionMode = selectionMode,
                                    onClick = {
                                        if (selectionMode) onFileLongClick(file) else onFileClick(file)
                                    },
                                    onLongClick = { onFileLongClick(file) },
                                    onFavoriteToggle = { onToggleFavorite(file) },
                                    onMenuClick = { onOpenFileMenu(file) },
                                    modifier = Modifier.width(responsive.cardMinGridWidth + 36.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Favorites Section
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(responsive.space12)) {
                    SectionHeader(
                        title = "Starred Favorites",
                        actionText = if (favoriteFiles.isNotEmpty()) "View All (${favoriteFiles.size})" else null,
                        onActionClick = onOpenFavorites
                    )
                    if (favoriteFiles.isEmpty()) {
                        Surface(
                            onClick = onOpenFavorites,
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B)
                                )
                                Text(
                                    text = "Tap the star on any PDF, CSV, or TXT file to pin it here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            favoriteFiles.take(3).forEach { file ->
                                FileListCard(
                                    file = file,
                                    isSelected = file.id in selectedFileIds,
                                    selectionMode = selectionMode,
                                    onClick = {
                                        if (selectionMode) onFileLongClick(file) else onFileClick(file)
                                    },
                                    onLongClick = { onFileLongClick(file) },
                                    onFavoriteToggle = { onToggleFavorite(file) },
                                    onMenuClick = { onOpenFileMenu(file) }
                                )
                            }
                        }
                    }
                }
            }

            // Folders Adaptive Grid Header
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = "Collections & Folders",
                    actionText = "Manage",
                    onActionClick = onOpenAllFolders
                )
            }

            if (folders.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyVaultState(
                        title = "No Folders Yet",
                        subtitle = "Create color-coded folders to organize your manuscripts and tables.",
                        actionLabel = "Create Folder",
                        onActionClick = onNewFolder
                    )
                }
            } else {
                items(folders, key = { "home_folder_${it.id}" }) { folder ->
                    FolderGridCard(
                        folder = folder,
                        onClick = { onOpenFolder(folder) }
                    )
                }
            }

            // Footer Credit
            item(span = { GridItemSpan(maxLineSpan) }) {
                FooterCredit()
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (actionText != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun LibraryScreen(
    files: List<VaultFile>,
    storageSummary: StorageSummary,
    searchQuery: String,
    selectedTypeFilter: VaultFileType?,
    viewMode: ViewMode,
    sortOption: SortOption,
    sortAscending: Boolean,
    selectedFileIds: Set<Long>,
    isLoading: Boolean,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectTypeFilter: (VaultFileType?) -> Unit,
    onToggleViewMode: () -> Unit,
    onSelectSortOption: (SortOption) -> Unit,
    onSelectAll: () -> Unit,
    onAddClick: () -> Unit,
    onFileClick: (VaultFile) -> Unit,
    onFileLongClick: (VaultFile) -> Unit,
    onToggleFavorite: (VaultFile) -> Unit,
    onOpenFileMenu: (VaultFile) -> Unit,
    onScrollDirectionChanged: (Boolean) -> Unit,
    onScrollPhaseChanged: (Float) -> Unit = {}
) {
    val responsive = LocalResponsiveLayout.current
    val gridState = rememberLazyGridState()
    var prevIndex by remember { mutableIntStateOf(0) }
    var prevOffset by remember { mutableIntStateOf(0) }

    val isScrollingUp by remember {
        derivedStateOf {
            if (gridState.firstVisibleItemIndex != prevIndex) {
                val up = gridState.firstVisibleItemIndex < prevIndex
                prevIndex = gridState.firstVisibleItemIndex
                prevOffset = gridState.firstVisibleItemScrollOffset
                up
            } else {
                val up = gridState.firstVisibleItemScrollOffset <= prevOffset
                prevOffset = gridState.firstVisibleItemScrollOffset
                up
            }
        }
    }

    val scrollPhase by remember {
        derivedStateOf {
            ((gridState.firstVisibleItemIndex * 240 + gridState.firstVisibleItemScrollOffset) % 1000) / 1000f
        }
    }

    LaunchedEffect(isScrollingUp) {
        onScrollDirectionChanged(isScrollingUp)
    }

    LaunchedEffect(scrollPhase) {
        onScrollPhaseChanged(scrollPhase)
    }

    val selectionMode = selectedFileIds.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout)),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = responsive.cardMinGridWidth),
            modifier = Modifier
                .widthIn(max = responsive.contentMaxWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = responsive.screenMargin,
                end = responsive.screenMargin,
                top = responsive.space16,
                bottom = responsive.bottomBarClearance
            ),
            horizontalArrangement = Arrangement.spacedBy(responsive.space12),
            verticalArrangement = Arrangement.spacedBy(responsive.space16)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = "ALL DOCUMENTS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Vault Library",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircleIconButton(
                            icon = Icons.Default.Checklist,
                            contentDescription = "Select all files",
                            onClick = onSelectAll
                        )
                        CircleIconButton(
                            icon = if (viewMode == ViewMode.GRID) {
                                Icons.AutoMirrored.Filled.ViewList
                            } else {
                                Icons.Default.GridView
                            },
                            contentDescription = "Toggle Grid or List view",
                            onClick = onToggleViewMode
                        )
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                PillSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                TypeFilterChipsRow(
                    selectedType = selectedTypeFilter,
                    totalCount = storageSummary.totalCount,
                    pdfCount = storageSummary.pdfCount,
                    csvCount = storageSummary.csvCount,
                    txtCount = storageSummary.txtCount,
                    onSelectType = onSelectTypeFilter
                )
            }

            // Sort Pill Row
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort by",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    SortOption.entries.forEach { option ->
                        val selected = sortOption == option
                        Surface(
                            onClick = { onSelectSortOption(option) },
                            shape = CircleShape,
                            color = if (selected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .minimumInteractiveComponentSize()
                                .border(
                                    width = 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                    shape = CircleShape
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1
                                )
                                if (selected) {
                                    Icon(
                                        imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = if (sortAscending) "Ascending" else "Descending",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ErrorStateBanner(message = errorMessage, onDismiss = onDismissError)
                }
            }

            if (isLoading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LoadingSkeletonList(count = 4)
                }
            } else if (files.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyVaultState(
                        title = if (searchQuery.isNotBlank()) "No Matching Documents" else "No Files in This View",
                        subtitle = if (searchQuery.isNotBlank()) {
                            "Try searching for a different title, folder, or tag."
                        } else {
                            "Tap the + button below to import PDF, CSV, or TXT files."
                        },
                        actionLabel = "Add to Vault",
                        onActionClick = onAddClick
                    )
                }
            } else if (viewMode == ViewMode.LIST) {
                items(
                    items = files,
                    key = { "lib_list_${it.id}" },
                    span = { GridItemSpan(maxLineSpan) }
                ) { file ->
                    FileListCard(
                        file = file,
                        isSelected = file.id in selectedFileIds,
                        selectionMode = selectionMode,
                        onClick = {
                            if (selectionMode) onFileLongClick(file) else onFileClick(file)
                        },
                        onLongClick = { onFileLongClick(file) },
                        onFavoriteToggle = { onToggleFavorite(file) },
                        onMenuClick = { onOpenFileMenu(file) }
                    )
                }
            } else {
                items(
                    items = files,
                    key = { "lib_grid_${it.id}" }
                ) { file ->
                    FileGridCard(
                        file = file,
                        isSelected = file.id in selectedFileIds,
                        selectionMode = selectionMode,
                        onClick = {
                            if (selectionMode) onFileLongClick(file) else onFileClick(file)
                        },
                        onLongClick = { onFileLongClick(file) },
                        onFavoriteToggle = { onToggleFavorite(file) },
                        onMenuClick = { onOpenFileMenu(file) }
                    )
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(
    favoriteFiles: List<VaultFile>,
    viewMode: ViewMode,
    selectedFileIds: Set<Long>,
    onBack: () -> Unit,
    onToggleViewMode: () -> Unit,
    onFileClick: (VaultFile) -> Unit,
    onFileLongClick: (VaultFile) -> Unit,
    onToggleFavorite: (VaultFile) -> Unit,
    onOpenFileMenu: (VaultFile) -> Unit
) {
    BackHandler(onBack = onBack)
    val responsive = LocalResponsiveLayout.current
    val selectionMode = selectedFileIds.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout)),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = responsive.cardMinGridWidth),
            modifier = Modifier
                .widthIn(max = responsive.contentMaxWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = responsive.screenMargin,
                end = responsive.screenMargin,
                top = responsive.space16,
                bottom = 140.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(responsive.space12),
            verticalArrangement = Arrangement.spacedBy(responsive.space16)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        CircleIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = onBack
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "STARRED COLLECTION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.4.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFFF59E0B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Favorites",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    CircleIconButton(
                        icon = if (viewMode == ViewMode.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle view",
                        onClick = onToggleViewMode
                    )
                }
            }

            if (favoriteFiles.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyVaultState(
                        title = "No Starred Files Yet",
                        subtitle = "Tap the star icon on any PDF, CSV, or TXT file to keep it in your Favorites.",
                        icon = Icons.Default.Star
                    )
                }
            } else if (viewMode == ViewMode.LIST) {
                items(
                    items = favoriteFiles,
                    key = { "fav_list_${it.id}" },
                    span = { GridItemSpan(maxLineSpan) }
                ) { file ->
                    FileListCard(
                        file = file,
                        isSelected = file.id in selectedFileIds,
                        selectionMode = selectionMode,
                        onClick = { if (selectionMode) onFileLongClick(file) else onFileClick(file) },
                        onLongClick = { onFileLongClick(file) },
                        onFavoriteToggle = { onToggleFavorite(file) },
                        onMenuClick = { onOpenFileMenu(file) }
                    )
                }
            } else {
                items(
                    items = favoriteFiles,
                    key = { "fav_grid_${it.id}" }
                ) { file ->
                    FileGridCard(
                        file = file,
                        isSelected = file.id in selectedFileIds,
                        selectionMode = selectionMode,
                        onClick = { if (selectionMode) onFileLongClick(file) else onFileClick(file) },
                        onLongClick = { onFileLongClick(file) },
                        onFavoriteToggle = { onToggleFavorite(file) },
                        onMenuClick = { onOpenFileMenu(file) }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchScreen(
    allFiles: List<VaultFile>,
    searchQuery: String,
    selectedTypeFilter: VaultFileType?,
    onSearchQueryChange: (String) -> Unit,
    onSelectTypeFilter: (VaultFileType?) -> Unit,
    onBack: () -> Unit,
    onFileClick: (VaultFile) -> Unit,
    onToggleFavorite: (VaultFile) -> Unit,
    onOpenFileMenu: (VaultFile) -> Unit
) {
    BackHandler(onBack = onBack)
    val responsive = LocalResponsiveLayout.current

    val allTags = remember(allFiles) {
        allFiles.flatMap { it.tags }.distinct().take(10)
    }

    val results = remember(allFiles, searchQuery, selectedTypeFilter) {
        allFiles.filter { file ->
            val typeOk = selectedTypeFilter == null || file.type == selectedTypeFilter
            val q = searchQuery.trim()
            val queryOk = q.isEmpty() ||
                file.name.contains(q, ignoreCase = true) ||
                file.folderName?.contains(q, ignoreCase = true) == true ||
                file.tags.any { it.contains(q, ignoreCase = true) }
            typeOk && queryOk
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = responsive.contentMaxWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = responsive.screenMargin,
                end = responsive.screenMargin,
                top = responsive.space16,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(responsive.space16)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircleIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack
                    )
                    PillSearchBar(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                TypeFilterChipsRow(
                    selectedType = selectedTypeFilter,
                    totalCount = allFiles.size,
                    pdfCount = allFiles.count { it.type == VaultFileType.PDF },
                    csvCount = allFiles.count { it.type == VaultFileType.CSV },
                    txtCount = allFiles.count { it.type == VaultFileType.TXT },
                    onSelectType = onSelectTypeFilter
                )
            }

            if (allTags.isNotEmpty() && searchQuery.isEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Quick Tag Filters",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allTags.forEach { tag ->
                                Surface(
                                    onClick = { onSearchQueryChange(tag) },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "${results.size} matching document${if (results.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (results.isEmpty()) {
                item {
                    EmptyVaultState(
                        title = "No Results Found",
                        subtitle = "No PDF, CSV, or TXT files matched \"$searchQuery\"."
                    )
                }
            } else {
                items(results, key = { "search_${it.id}" }) { file ->
                    FileListCard(
                        file = file,
                        isSelected = false,
                        selectionMode = false,
                        onClick = { onFileClick(file) },
                        onLongClick = {},
                        onFavoriteToggle = { onToggleFavorite(file) },
                        onMenuClick = { onOpenFileMenu(file) }
                    )
                }
            }
        }
    }
}
