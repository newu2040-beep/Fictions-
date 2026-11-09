package com.editingcells.fictions.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.editingcells.fictions.domain.model.PastelTheme
import com.editingcells.fictions.domain.model.SortOption
import com.editingcells.fictions.domain.model.StorageSummary
import com.editingcells.fictions.domain.model.ThemeMode
import com.editingcells.fictions.domain.model.UserPreferences
import com.editingcells.fictions.domain.model.VaultFile
import com.editingcells.fictions.domain.model.VaultFolder
import com.editingcells.fictions.domain.model.ViewMode
import com.editingcells.fictions.domain.model.formatBytes
import com.editingcells.fictions.ui.components.CircleIconButton
import com.editingcells.fictions.ui.components.EmptyVaultState
import com.editingcells.fictions.ui.components.FileGridCard
import com.editingcells.fictions.ui.components.FileListCard
import com.editingcells.fictions.ui.components.FolderGridCard
import com.editingcells.fictions.ui.components.FooterCredit
import com.editingcells.fictions.ui.theme.JetBrainsMonoFamily
import com.editingcells.fictions.ui.theme.LocalFileTypePalette
import com.editingcells.fictions.ui.theme.LocalResponsiveLayout
import com.editingcells.fictions.ui.theme.PlusJakartaSansFamily

@Composable
fun FoldersScreen(
    folders: List<VaultFolder>,
    unfiledCount: Int,
    onOpenFolder: (VaultFolder) -> Unit,
    onEditFolder: (VaultFolder) -> Unit,
    onCreateFolder: () -> Unit,
    onScrollDirectionChanged: (Boolean) -> Unit = {},
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
                            text = "COLOR-CODED VAULT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Folders",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    CircleIconButton(
                        icon = Icons.Default.CreateNewFolder,
                        contentDescription = "Create new folder",
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = onCreateFolder
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(26.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = "${folders.size} Active Collections",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$unfiledCount unfiled documents in root vault",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = onCreateFolder,
                            shape = CircleShape,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "New Folder",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (folders.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyVaultState(
                        title = "No Folders Created Yet",
                        subtitle = "Group your PDFs, CSV tables, and TXT notes into color-coded collections.",
                        actionLabel = "Create Folder",
                        onActionClick = onCreateFolder
                    )
                }
            } else {
                items(folders, key = { "folder_grid_${it.id}" }) { folder ->
                    FolderGridCard(
                        folder = folder,
                        onClick = { onOpenFolder(folder) },
                        onMenuClick = { onEditFolder(folder) }
                    )
                }
            }
        }
    }
}

@Composable
fun FolderDetailScreen(
    folder: VaultFolder,
    files: List<VaultFile>,
    viewMode: ViewMode,
    selectedFileIds: Set<Long>,
    onBack: () -> Unit,
    onEditFolder: () -> Unit,
    onToggleViewMode: () -> Unit,
    onImportToFolder: () -> Unit,
    onNewNoteInFolder: () -> Unit,
    onFileClick: (VaultFile) -> Unit,
    onFileLongClick: (VaultFile) -> Unit,
    onToggleFavorite: (VaultFile) -> Unit,
    onOpenFileMenu: (VaultFile) -> Unit
) {
    BackHandler(onBack = onBack)
    val responsive = LocalResponsiveLayout.current
    val folderColor = Color(folder.colorHex)
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
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(folderColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = folderColor
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${files.size} files • ${formatBytes(files.sumOf { it.sizeBytes })}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircleIconButton(
                            icon = Icons.Default.Edit,
                            contentDescription = "Edit folder",
                            onClick = onEditFolder
                        )
                        CircleIconButton(
                            icon = if (viewMode == ViewMode.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle view",
                            onClick = onToggleViewMode
                        )
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onImportToFolder,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Import & Rename",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = onNewNoteInFolder,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = "New TXT Note",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (files.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyVaultState(
                        title = "Folder is Empty",
                        subtitle = "Import PDF, CSV, or TXT files directly into ${folder.name} with custom names.",
                        actionLabel = "Import Files",
                        onActionClick = onImportToFolder
                    )
                }
            } else if (viewMode == ViewMode.LIST) {
                items(
                    items = files,
                    key = { "fdetail_list_${it.id}" },
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
                    items = files,
                    key = { "fdetail_grid_${it.id}" }
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

/**
 * In-app TXT Note Editor Screen (create new TXT note or edit existing TXT file).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    existingFile: VaultFile? = null,
    initialBody: String = "",
    folders: List<VaultFolder>,
    preselectedFolderId: Long? = null,
    onBack: () -> Unit,
    onSaveNote: (existingId: Long?, title: String, body: String, folderId: Long?, tags: List<String>) -> Unit
) {
    BackHandler(onBack = onBack)
    val responsive = LocalResponsiveLayout.current

    var title by remember(existingFile) {
        mutableStateOf(existingFile?.name?.removeSuffix(".txt")?.removeSuffix(".TXT") ?: "")
    }
    var body by remember(existingFile, initialBody) {
        mutableStateOf(initialBody)
    }
    var selectedFolderId by remember(existingFile, preselectedFolderId) {
        mutableStateOf(existingFile?.folderId ?: preselectedFolderId)
    }
    val tags = remember(existingFile) {
        mutableStateListOf(*(existingFile?.tags?.toTypedArray() ?: arrayOf("note")))
    }
    var tagInput by remember { mutableStateOf("") }
    var useMonospace by remember { mutableStateOf(false) }

    val wordCount = remember(body) {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) 0 else trimmed.split(Regex("\\s+")).size
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
                                text = if (existingFile == null) "NEW TXT DOCUMENT" else "EDIT TXT DOCUMENT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.4.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (existingFile == null) "Compose Note" else "Edit Note",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onSaveNote(
                                existingFile?.id,
                                title.ifBlank { "Untitled Note" },
                                body,
                                selectedFolderId,
                                tags.toList()
                            )
                        },
                        shape = CircleShape,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save .TXT", maxLines = 1)
                    }
                }
            }

            // Title input
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title (.txt)") },
                    placeholder = { Text("e.g. Manuscript Chapter 1") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Folder & Font style row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { useMonospace = !useMonospace },
                        shape = CircleShape,
                        color = if (useMonospace) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .minimumInteractiveComponentSize()
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (useMonospace) "Font: Monospace" else "Font: Sans-Serif",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        onClick = { selectedFolderId = null },
                        shape = CircleShape,
                        color = if (selectedFolderId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .minimumInteractiveComponentSize()
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Unfiled",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selectedFolderId == null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }

                    folders.forEach { folder ->
                        val selected = selectedFolderId == folder.id
                        Surface(
                            onClick = { selectedFolderId = folder.id },
                            shape = CircleShape,
                            color = if (selected) Color(folder.colorHex) else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .minimumInteractiveComponentSize()
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = folder.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Live stats bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$wordCount words • ${body.length} characters",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "UTF-8 Plain Text",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }

            // Body Editor
            item {
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = { Text("Begin writing your note, essay, or literary fragment…") },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = if (useMonospace) JetBrainsMonoFamily else PlusJakartaSansFamily,
                        lineHeight = 26.sp
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 280.dp)
                )
            }

            // Tags chip editor
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Tags",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tags.forEach { tag ->
                            Surface(
                                onClick = { tags.remove(tag) },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("#$tag", style = MaterialTheme.typography.labelMedium)
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove tag",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tagInput,
                            onValueChange = { tagInput = it },
                            placeholder = { Text("Add tag…") },
                            singleLine = true,
                            shape = CircleShape,
                            modifier = Modifier.weight(1f)
                        )
                        CircleIconButton(
                            icon = Icons.Default.Add,
                            contentDescription = "Add tag",
                            onClick = {
                                val clean = tagInput.trim().removePrefix("#")
                                if (clean.isNotEmpty() && !tags.contains(clean)) {
                                    tags.add(clean)
                                    tagInput = ""
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Settings Screen:
 * - Permissions & Full Device Access (Notification, Gallery, All Files Access + Import All Files)
 * - 6 named pastel themes (Lavender, Peach, Mint, Sky, Rose, Sand)
 * - Dark / Light / System mode
 * - Default Library View (Grid / List)
 * - Default Sort Order (Date, Name, Size, Type)
 * - Storage Used breakdown + Clear All Vault Data
 * - Footer credit: "Made with ❤️ by Rahul Shah"
 */
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    storageSummary: StorageSummary,
    hasNotificationPermission: Boolean,
    hasGalleryPermission: Boolean,
    hasAllFilesAccess: Boolean,
    onRequestPermissions: () -> Unit,
    onRequestAllFilesAccess: () -> Unit,
    onImportAllFiles: () -> Unit,
    onScanDeviceStorage: () -> Unit,
    onSelectTheme: (PastelTheme) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectDefaultView: (ViewMode) -> Unit,
    onSelectDefaultSort: (SortOption) -> Unit,
    onClearAllVaultData: () -> Unit,
    onScrollDirectionChanged: (Boolean) -> Unit = {},
    onScrollPhaseChanged: (Float) -> Unit = {}
) {
    val palette = LocalFileTypePalette.current
    val responsive = LocalResponsiveLayout.current
    val listState = rememberLazyListState()
    var prevIndex by remember { mutableIntStateOf(0) }
    var prevOffset by remember { mutableIntStateOf(0) }

    val isScrollingUp by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex != prevIndex) {
                val up = listState.firstVisibleItemIndex < prevIndex
                prevIndex = listState.firstVisibleItemIndex
                prevOffset = listState.firstVisibleItemScrollOffset
                up
            } else {
                val up = listState.firstVisibleItemScrollOffset <= prevOffset
                prevOffset = listState.firstVisibleItemScrollOffset
                up
            }
        }
    }

    val scrollPhase by remember {
        derivedStateOf {
            ((listState.firstVisibleItemIndex * 240 + listState.firstVisibleItemScrollOffset) % 1000) / 1000f
        }
    }

    LaunchedEffect(isScrollingUp) {
        onScrollDirectionChanged(isScrollingUp)
    }

    LaunchedEffect(scrollPhase) {
        onScrollPhaseChanged(scrollPhase)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout)),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .widthIn(max = responsive.contentMaxWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = responsive.screenMargin,
                end = responsive.screenMargin,
                top = responsive.space16,
                bottom = responsive.bottomBarClearance
            ),
            verticalArrangement = Arrangement.spacedBy(responsive.space20)
        ) {
            item {
                Column {
                    Text(
                        text = "PERMISSIONS, CUSTOMIZATION & STORAGE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 0. Permissions & Full Device Access Card
            item {
                SettingsCard(
                    title = "Permissions & Full File Access",
                    subtitle = "Grant Notification, Gallery, and All Files Access for unrestricted vault imports"
                ) {
                    PermissionStatusRow(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        subtitle = if (hasNotificationPermission) "Granted • Vault import & export alerts enabled" else "Tap below to allow vault notifications",
                        granted = hasNotificationPermission,
                        onClick = onRequestPermissions
                    )

                    PermissionStatusRow(
                        icon = Icons.Default.PhotoLibrary,
                        title = "Gallery & Media Access",
                        subtitle = if (hasGalleryPermission) "Granted • Full gallery & media access enabled" else "Tap below to allow gallery & media access",
                        granted = hasGalleryPermission,
                        onClick = onRequestPermissions
                    )

                    PermissionStatusRow(
                        icon = Icons.Default.FolderOpen,
                        title = "Files Full Access",
                        subtitle = if (hasAllFilesAccess) "Granted • All device files & folders accessible" else "Tap to grant All Files Access in system settings",
                        granted = hasAllFilesAccess,
                        onClick = onRequestAllFilesAccess
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onRequestPermissions,
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("request_all_permissions_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Allow Permissions",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = onRequestAllFilesAccess,
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("grant_all_files_access_button")
                        ) {
                            Text(
                                text = "All Files Access",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onImportAllFiles,
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Import All Files",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = onScanDeviceStorage,
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = "Scan Storage",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 1. 6 Pastel Themes Card
            item {
                SettingsCard(
                    title = "Pastel Theme Palette",
                    subtitle = "Choose from 6 signature pastel palettes with live theme-tuned liquid glass"
                ) {
                    val rows = PastelTheme.entries.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        rows.forEach { rowThemes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowThemes.forEach { theme ->
                                    val selected = preferences.theme == theme
                                    Surface(
                                        onClick = { onSelectTheme(theme) },
                                        shape = RoundedCornerShape(22.dp),
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 60.dp)
                                            .border(
                                                width = if (selected) 2.dp else 1.dp,
                                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(22.dp)
                                            )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(
                                                                Color(theme.previewPrimaryHex),
                                                                Color(theme.previewSecondaryHex)
                                                            )
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (selected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = theme.displayName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = theme.subtitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Appearance Mode (System / Light / Dark)
            item {
                SettingsCard(
                    title = "Appearance Mode",
                    subtitle = "Switch between System, Light, or Dark liquid-glass surfaces"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val selected = preferences.themeMode == mode
                            Surface(
                                onClick = { onSelectThemeMode(mode) },
                                shape = CircleShape,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Default View & Default Sort Order
            item {
                SettingsCard(
                    title = "Library Defaults",
                    subtitle = "Configure default layout and file sorting"
                ) {
                    Text(
                        text = "Default View Layout",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ViewMode.entries.forEach { mode ->
                            val selected = preferences.defaultViewMode == mode
                            Surface(
                                onClick = { onSelectDefaultView(mode) },
                                shape = CircleShape,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 11.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Default Sort Order",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SortOption.entries.forEach { sort ->
                            val selected = preferences.defaultSortOption == sort
                            Surface(
                                onClick = { onSelectDefaultSort(sort) },
                                shape = CircleShape,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .minimumInteractiveComponentSize()
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = sort.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Storage Used Breakdown
            item {
                SettingsCard(
                    title = "Vault Storage Used",
                    subtitle = "App-private storage for your real imported files & notes"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = formatBytes(storageSummary.totalBytes),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${storageSummary.totalCount} total files",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    StorageBreakdownRow("PDF Documents (${storageSummary.pdfCount})", formatBytes(storageSummary.pdfBytes), palette.pdfBadgeFg)
                    StorageBreakdownRow("CSV Data Tables (${storageSummary.csvCount})", formatBytes(storageSummary.csvBytes), palette.csvBadgeFg)
                    StorageBreakdownRow("TXT Plain Notes (${storageSummary.txtCount})", formatBytes(storageSummary.txtBytes), palette.txtBadgeFg)

                    if (storageSummary.totalCount > 0) {
                        OutlinedButton(
                            onClick = onClearAllVaultData,
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Clear All Vault Data",
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 5. Footer Credit
            item {
                FooterCredit()
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (granted) Color(0xFF10B981).copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (granted) Color(0xFF059669) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = CircleShape,
                color = if (granted) Color(0xFF10B981).copy(alpha = 0.18f) else MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (granted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Granted",
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = if (granted) "Allowed" else "Allow",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (granted) Color(0xFF059669) else MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(28.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            content()
        }
    }
}

@Composable
private fun StorageBreakdownRow(
    label: String,
    sizeText: String,
    dotColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = sizeText,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
