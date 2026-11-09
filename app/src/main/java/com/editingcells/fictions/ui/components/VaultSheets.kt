package com.editingcells.fictions.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.editingcells.fictions.domain.model.FolderColor
import com.editingcells.fictions.domain.model.PendingImportItem
import com.editingcells.fictions.domain.model.VaultFile
import com.editingcells.fictions.domain.model.VaultFileType
import com.editingcells.fictions.domain.model.VaultFolder
import com.editingcells.fictions.domain.model.formatBytes
import com.editingcells.fictions.domain.model.formatDetailedTimestamp
import com.editingcells.fictions.ui.theme.LocalFileTypePalette
import com.editingcells.fictions.ui.theme.LocalResponsiveLayout

/**
 * In-window Liquid Glass Bottom Sheet host (32dp top corners).
 * Renders inside the same Compose window hierarchy as [LocalVaultBackdrop] so
 * Kyant0/AndroidLiquidGlass (`drawBackdrop`) refracts the underlying screen content
 * smoothly at 60fps without cross-window RenderNode exceptions, and springs up smoothly.
 */
@Composable
fun GlassBottomSheetContainer(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    BackHandler(onBack = onDismiss)
    val responsive = LocalResponsiveLayout.current
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val slideFraction by animateFloatAsState(
        targetValue = if (entered) 0f else 0.18f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "sheet_spring_up"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.38f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        GlassSurface(
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            refractionHeight = 16.dp,
            refractionAmount = 26.dp,
            elevation = 20.dp,
            modifier = Modifier
                .widthIn(max = responsive.sheetMaxWidth)
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding()
                .graphicsLayer {
                    translationY = size.height * slideFraction
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Consume clicks inside sheet
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = responsive.screenMargin, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content
            )
        }
    }
}

/**
 * Floating dark liquid-glass pill action bar for Multi-Select Mode.
 * Centered with max width 480dp on wide screens and 48dp minimum touch targets.
 */
@Composable
fun SelectionFloatingActionPill(
    selectedCount: Int,
    visible: Boolean,
    onSelectAll: () -> Unit,
    onMoveSelected: () -> Unit,
    onShareSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val responsive = LocalResponsiveLayout.current

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            initialOffsetY = { it }
        ) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = responsive.screenMargin, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassSurface(
                shape = CircleShape,
                refractionHeight = 16.dp,
                refractionAmount = 24.dp,
                surfaceTint = Color(0xFF14111F).copy(alpha = 0.86f),
                elevation = 16.dp,
                modifier = Modifier
                    .widthIn(max = responsive.floatingBarMaxWidth)
                    .fillMaxWidth()
                    .heightIn(min = 68.dp)
                    .testTag("selection_action_pill")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            onClick = onClearSelection,
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.14f),
                            modifier = Modifier
                                .size(48.dp)
                                .minimumInteractiveComponentSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Exit selection",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "$selectedCount selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SelectionCircleAction(
                            icon = Icons.Default.SelectAll,
                            contentDescription = "Select all",
                            onClick = onSelectAll
                        )
                        SelectionCircleAction(
                            icon = Icons.AutoMirrored.Filled.DriveFileMove,
                            contentDescription = "Move selected",
                            onClick = onMoveSelected
                        )
                        SelectionCircleAction(
                            icon = Icons.Default.Share,
                            contentDescription = "Share selected",
                            onClick = onShareSelected
                        )
                        SelectionCircleAction(
                            icon = Icons.Default.DeleteOutline,
                            contentDescription = "Delete selected",
                            containerColor = Color(0xFFE11D48).copy(alpha = 0.88f),
                            onClick = onDeleteSelected
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionCircleAction(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color = Color.White.copy(alpha = 0.16f),
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        modifier = Modifier
            .size(44.dp)
            .minimumInteractiveComponentSize()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Center "+" FAB Glass Bottom Sheet with Import All Files, Gallery, Folder/Storage Scan,
 * Import PDF, Import CSV, Import TXT, New Note, and New Folder.
 */
@Composable
fun AddToVaultBottomSheet(
    onDismiss: () -> Unit,
    onImportAllFiles: () -> Unit,
    onImportFromGallery: () -> Unit,
    onImportFolderTree: () -> Unit,
    onScanDeviceStorage: () -> Unit,
    onImportPdf: () -> Unit,
    onImportCsv: () -> Unit,
    onImportTxt: () -> Unit,
    onNewNote: () -> Unit,
    onNewFolder: () -> Unit
) {
    val palette = LocalFileTypePalette.current

    GlassBottomSheetContainer(onDismiss = onDismiss) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(44.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Add to Fictions Vault",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Import any files with custom rename, or create notes & folders",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CircleIconButton(
                icon = Icons.Default.Close,
                contentDescription = "Close",
                onClick = onDismiss,
                size = 40.dp
            )
        }

        // Primary Full-Width Import Any / All Files button
        WideActionTile(
            title = "Import All Files (Rename & Save)",
            subtitle = "Pick any PDF, CSV, TXT or document from device storage",
            icon = Icons.Default.FileOpen,
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = {
                onDismiss()
                onImportAllFiles()
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("import_all_files_tile")
        )

        // Specific File Type Tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BigActionTile(
                title = "Import PDF",
                subtitle = "Books & folios",
                icon = Icons.Default.PictureAsPdf,
                badgeBg = palette.pdfBadgeBg,
                badgeFg = palette.pdfBadgeFg,
                onClick = {
                    onDismiss()
                    onImportPdf()
                },
                modifier = Modifier.weight(1f)
            )
            BigActionTile(
                title = "Import CSV",
                subtitle = "Data tables",
                icon = Icons.Default.TableChart,
                badgeBg = palette.csvBadgeBg,
                badgeFg = palette.csvBadgeFg,
                onClick = {
                    onDismiss()
                    onImportCsv()
                },
                modifier = Modifier.weight(1f)
            )
            BigActionTile(
                title = "Import TXT",
                subtitle = "Plain texts",
                icon = Icons.Default.Description,
                badgeBg = palette.txtBadgeBg,
                badgeFg = palette.txtBadgeFg,
                onClick = {
                    onDismiss()
                    onImportTxt()
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Gallery, Folder Tree & Storage Scan Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WideActionTile(
                title = "Gallery & Media",
                subtitle = "Import from Gallery",
                icon = Icons.Default.PhotoLibrary,
                accentColor = MaterialTheme.colorScheme.tertiary,
                onClick = {
                    onDismiss()
                    onImportFromGallery()
                },
                modifier = Modifier.weight(1f)
            )
            WideActionTile(
                title = "Import Folder",
                subtitle = "Entire directory",
                icon = Icons.Default.FolderOpen,
                accentColor = MaterialTheme.colorScheme.secondary,
                onClick = {
                    onDismiss()
                    onImportFolderTree()
                },
                modifier = Modifier.weight(1f)
            )
        }

        WideActionTile(
            title = "Scan Downloads & Documents",
            subtitle = "Find all PDF, CSV & TXT files on device storage to rename & import",
            icon = Icons.Default.Search,
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = {
                onDismiss()
                onScanDeviceStorage()
            },
            modifier = Modifier.fillMaxWidth()
        )

        // New Note & New Folder Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WideActionTile(
                title = "New TXT Note",
                subtitle = "Write inside Fictions",
                icon = Icons.Default.EditNote,
                accentColor = MaterialTheme.colorScheme.primary,
                onClick = {
                    onDismiss()
                    onNewNote()
                },
                modifier = Modifier.weight(1f)
            )
            WideActionTile(
                title = "New Folder",
                subtitle = "Color-coded collection",
                icon = Icons.Default.CreateNewFolder,
                accentColor = MaterialTheme.colorScheme.secondary,
                onClick = {
                    onDismiss()
                    onNewFolder()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Import & Rename Before Saving Sheet:
 * Lets the user customize/rename every imported file's name, pick its vault type (PDF/CSV/TXT),
 * choose a destination folder, and add tags before saving into the vault.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportRenameBottomSheet(
    items: List<PendingImportItem>,
    folders: List<VaultFolder>,
    initialFolderId: Long?,
    onUpdateItemName: (index: Int, newName: String) -> Unit,
    onUpdateItemType: (index: Int, newType: VaultFileType) -> Unit,
    onRemoveItem: (index: Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirmSave: (targetFolderId: Long?, tags: List<String>) -> Unit
) {
    var selectedFolderId by remember(initialFolderId) { mutableStateOf(initialFolderId) }
    val tags = remember { mutableStateListOf<String>() }
    var tagInput by remember { mutableStateOf("") }

    GlassBottomSheetContainer(onDismiss = onDismiss) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(44.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Rename & Save to Vault",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${items.size} file${if (items.size == 1) "" else "s"} ready • Edit names before importing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CircleIconButton(
                icon = Icons.Default.Close,
                contentDescription = "Cancel import",
                onClick = onDismiss,
                size = 40.dp
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // Editable list of files to rename before saving
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.forEachIndexed { index, item ->
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                FileTypeBadge(type = item.detectedType)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Original: ${item.originalName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.sizeBytes > 0L) {
                                        Text(
                                            text = formatBytes(item.sizeBytes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            if (items.size > 1) {
                                CircleIconButton(
                                    icon = Icons.Default.Close,
                                    contentDescription = "Remove file from import",
                                    onClick = { onRemoveItem(index) },
                                    size = 34.dp
                                )
                            }
                        }

                        OutlinedTextField(
                            value = item.customName,
                            onValueChange = { onUpdateItemName(index, it) },
                            label = { Text("Save as name (.${item.detectedType.extension})") },
                            placeholder = { Text("Enter custom file name") },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("import_rename_input_$index")
                        )

                        // Optional type selector pill row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Format:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            VaultFileType.entries.forEach { fileType ->
                                val selected = item.detectedType == fileType
                                Surface(
                                    onClick = { onUpdateItemType(index, fileType) },
                                    shape = CircleShape,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    }
                                ) {
                                    Text(
                                        text = fileType.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Destination Folder Picker
        Text(
            text = "Destination Folder",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = { selectedFolderId = null },
                shape = CircleShape,
                color = if (selectedFolderId == null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = "Unfiled (Root Vault)",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selectedFolderId == null) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            folders.forEach { folder ->
                val selected = selectedFolderId == folder.id
                Surface(
                    onClick = { selectedFolderId = folder.id },
                    shape = CircleShape,
                    color = if (selected) Color(folder.colorHex) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Optional Tags for Imported Files
        if (tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    Surface(
                        onClick = { tags.remove(tag) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it },
                placeholder = { Text("Optional tag (e.g. important)") },
                singleLine = true,
                shape = CircleShape,
                modifier = Modifier.weight(1f)
            )
            CircleIconButton(
                icon = Icons.Default.Add,
                contentDescription = "Add tag",
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = {
                    val clean = tagInput.trim().removePrefix("#")
                    if (clean.isNotEmpty() && !tags.contains(clean)) {
                        tags.add(clean)
                        tagInput = ""
                    }
                }
            )
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                shape = CircleShape,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    onConfirmSave(selectedFolderId, tags.toList())
                },
                shape = CircleShape,
                modifier = Modifier
                    .weight(1.3f)
                    .testTag("confirm_import_rename_save_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save to Vault (${items.size})")
            }
        }
    }
}

@Composable
private fun BigActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeBg: Color,
    badgeFg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shadowElevation = 2.dp,
        modifier = modifier.border(
            width = 1.dp,
            color = badgeFg.copy(alpha = 0.28f),
            shape = RoundedCornerShape(26.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = badgeFg,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WideActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shadowElevation = 2.dp,
        modifier = modifier.border(
            width = 1.dp,
            color = accentColor.copy(alpha = 0.28f),
            shape = RoundedCornerShape(26.dp)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * File Actions Bottom Sheet (32dp top corners, liquid glass):
 * Open, Rename, Move, Favorite, Tag, Share, Export/Save Copy with Rename, Details (size, date), Delete.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FileActionsBottomSheet(
    file: VaultFile,
    folders: List<VaultFolder>,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onEditTxt: (() -> Unit)? = null,
    onRename: (String) -> Unit,
    onMoveToFolder: (Long?) -> Unit,
    onToggleFavorite: () -> Unit,
    onUpdateTags: (List<String>) -> Unit,
    onShare: () -> Unit,
    onExportCopy: (customFileName: String) -> Unit,
    onDelete: () -> Unit
) {
    var subMode by remember { mutableStateOf("MAIN") } // MAIN, RENAME, EXPORT_RENAME, MOVE, TAGS, DETAILS
    var renameText by remember(file.name) {
        mutableStateOf(file.name.substringBeforeLast("."))
    }
    var exportRenameText by remember(file.name) {
        mutableStateOf(file.name.substringBeforeLast("."))
    }
    var newTagInput by remember { mutableStateOf("") }
    val editableTags = remember(file.tags) { mutableStateListOf(*file.tags.toTypedArray()) }

    GlassBottomSheetContainer(onDismiss = onDismiss) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(44.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FileTypeBadge(type = file.type)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${file.formattedSize} • Added ${file.formattedDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CircleIconButton(
                icon = Icons.Default.Close,
                contentDescription = "Close sheet",
                onClick = onDismiss,
                size = 40.dp
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        when (subMode) {
            "MAIN" -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SheetActionRowButton(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        label = "Open",
                        onClick = {
                            onDismiss()
                            onOpen()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SheetActionRowButton(
                        icon = if (file.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        label = if (file.isFavorite) "Starred" else "Favorite",
                        onClick = onToggleFavorite,
                        modifier = Modifier.weight(1f)
                    )
                    SheetActionRowButton(
                        icon = Icons.Default.Share,
                        label = "Share",
                        onClick = {
                            onDismiss()
                            onShare()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (file.type == VaultFileType.TXT && onEditTxt != null) {
                    SheetMenuItem(
                        icon = Icons.Default.EditNote,
                        title = "Edit Note Content",
                        subtitle = "Open in Fictions TXT editor",
                        onClick = {
                            onDismiss()
                            onEditTxt()
                        }
                    )
                }

                SheetMenuItem(
                    icon = Icons.Default.Edit,
                    title = "Rename File",
                    subtitle = "Update display name & file in vault",
                    onClick = { subMode = "RENAME" }
                )
                SheetMenuItem(
                    icon = Icons.AutoMirrored.Filled.DriveFileMove,
                    title = "Move to Folder",
                    subtitle = file.folderName ?: "Currently unfiled",
                    onClick = { subMode = "MOVE" }
                )
                SheetMenuItem(
                    icon = Icons.Default.Label,
                    title = "Manage Tags",
                    subtitle = if (file.tags.isEmpty()) "Add searchable tags" else file.tags.joinToString(", ") { "#$it" },
                    onClick = { subMode = "TAGS" }
                )
                SheetMenuItem(
                    icon = Icons.Default.Download,
                    title = "Save Copy / Export with Rename",
                    subtitle = "Rename & save a copy to device storage",
                    onClick = { subMode = "EXPORT_RENAME" }
                )
                SheetMenuItem(
                    icon = Icons.Default.Info,
                    title = "File Details",
                    subtitle = "Size, timestamps, folder & path",
                    onClick = { subMode = "DETAILS" }
                )
                SheetMenuItem(
                    icon = Icons.Default.DeleteOutline,
                    title = "Delete from Vault",
                    subtitle = "Moves to trash with undo option",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = {
                        onDismiss()
                        onDelete()
                    }
                )
            }

            "RENAME" -> {
                Text(
                    text = "Rename Document in Vault",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("File name (.${file.type.extension})") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { subMode = "MAIN" },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                    Button(
                        onClick = {
                            onRename(renameText)
                            onDismiss()
                        },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Name")
                    }
                }
            }

            "EXPORT_RENAME" -> {
                Text(
                    text = "Rename & Save Copy to Device",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Customize the filename before choosing where to save on your device:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = exportRenameText,
                    onValueChange = { exportRenameText = it },
                    label = { Text("Export file name (.${file.type.extension})") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { subMode = "MAIN" },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                    Button(
                        onClick = {
                            val chosenName = exportRenameText.ifBlank { file.name.substringBeforeLast(".") }
                            onDismiss()
                            onExportCopy(chosenName)
                        },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Choose Folder & Save")
                    }
                }
            }

            "MOVE" -> {
                Text(
                    text = "Choose Destination Folder",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SheetMenuItem(
                    icon = Icons.Default.Folder,
                    title = "Unfiled (Root Vault)",
                    subtitle = "Remove from folder",
                    onClick = {
                        onMoveToFolder(null)
                        onDismiss()
                    }
                )
                folders.forEach { folder ->
                    SheetMenuItem(
                        icon = Icons.Default.Folder,
                        title = folder.name,
                        subtitle = "${folder.fileCount} files",
                        tint = Color(folder.colorHex),
                        onClick = {
                            onMoveToFolder(folder.id)
                            onDismiss()
                        }
                    )
                }
                OutlinedButton(
                    onClick = { subMode = "MAIN" },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back")
                }
            }

            "TAGS" -> {
                Text(
                    text = "Manage Tags",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (editableTags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        editableTags.forEach { tag ->
                            Surface(
                                onClick = { editableTags.remove(tag) },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove $tag",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTagInput,
                        onValueChange = { newTagInput = it },
                        placeholder = { Text("Add tag (e.g. research)") },
                        singleLine = true,
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    )
                    CircleIconButton(
                        icon = Icons.Default.Add,
                        contentDescription = "Add tag",
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = {
                            val clean = newTagInput.trim().removePrefix("#")
                            if (clean.isNotEmpty() && !editableTags.contains(clean)) {
                                editableTags.add(clean)
                                newTagInput = ""
                            }
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { subMode = "MAIN" },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                    Button(
                        onClick = {
                            onUpdateTags(editableTags.toList())
                            onDismiss()
                        },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Tags")
                    }
                }
            }

            "DETAILS" -> {
                DetailRow("File Name", file.name)
                DetailRow("Type", "${file.type.label} (.${file.type.extension})")
                DetailRow("Size", "${file.formattedSize} (${file.sizeBytes} bytes)")
                DetailRow("Date Added", formatDetailedTimestamp(file.dateAdded))
                DetailRow(
                    "Last Opened",
                    file.lastOpened?.let { formatDetailedTimestamp(it) } ?: "Never opened"
                )
                DetailRow("Folder", file.folderName ?: "Unfiled")
                DetailRow(
                    "Tags",
                    if (file.tags.isEmpty()) "None" else file.tags.joinToString(", ") { "#$it" }
                )
                OutlinedButton(
                    onClick = { subMode = "MAIN" },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to Actions")
                }
            }
        }
    }
}

@Composable
private fun SheetActionRowButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        modifier = modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            shape = RoundedCornerShape(22.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SheetMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = tint
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Create / Rename / Color-code Folder Glass Bottom Sheet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FolderEditorBottomSheet(
    existingFolder: VaultFolder? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: Long) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember(existingFolder) { mutableStateOf(existingFolder?.name ?: "") }
    var selectedColorHex by remember(existingFolder) {
        mutableLongStateOf(existingFolder?.colorHex ?: FolderColor.VIOLET.hex)
    }

    GlassBottomSheetContainer(onDismiss = onDismiss) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(44.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
        )

        Text(
            text = if (existingFolder == null) "New Collection Folder" else "Edit Folder",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Folder name") },
            placeholder = { Text("e.g. Literary Essays") },
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Folder Color Badge",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FolderColor.entries.forEach { folderColor ->
                val isSelected = selectedColorHex == folderColor.hex
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(folderColor.hex))
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape
                        )
                        .clickable { selectedColorHex = folderColor.hex },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = folderColor.label,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (existingFolder != null && onDelete != null) {
                OutlinedButton(
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    shape = CircleShape,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            } else {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = CircleShape,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
            }

            Button(
                onClick = {
                    onSave(name.ifBlank { "Untitled Folder" }, selectedColorHex)
                    onDismiss()
                },
                shape = CircleShape,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (existingFolder == null) "Create Folder" else "Save Changes")
            }
        }
    }
}

/**
 * Batch Move to Folder Sheet for Multi-Select Mode.
 */
@Composable
fun BatchMoveToFolderSheet(
    folders: List<VaultFolder>,
    onDismiss: () -> Unit,
    onSelectFolder: (Long?) -> Unit
) {
    GlassBottomSheetContainer(onDismiss = onDismiss) {
        Text(
            text = "Move Selected Files To…",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        SheetMenuItem(
            icon = Icons.Default.Folder,
            title = "Unfiled (Root Vault)",
            subtitle = "Keep in main library without folder",
            onClick = {
                onSelectFolder(null)
                onDismiss()
            }
        )
        folders.forEach { folder ->
            SheetMenuItem(
                icon = Icons.Default.Folder,
                title = folder.name,
                subtitle = "${folder.fileCount} files",
                tint = Color(folder.colorHex),
                onClick = {
                    onSelectFolder(folder.id)
                    onDismiss()
                }
            )
        }
    }
}
