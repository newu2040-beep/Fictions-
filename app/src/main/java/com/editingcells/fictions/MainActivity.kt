package com.editingcells.fictions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.editingcells.fictions.domain.model.VaultFile
import com.editingcells.fictions.domain.model.VaultFileType
import com.editingcells.fictions.domain.model.VaultFolder
import com.editingcells.fictions.ui.components.AddToVaultBottomSheet
import com.editingcells.fictions.ui.components.AmbientVaultBackground
import com.editingcells.fictions.ui.components.BatchMoveToFolderSheet
import com.editingcells.fictions.ui.components.FileActionsBottomSheet
import com.editingcells.fictions.ui.components.FloatingPillBottomBar
import com.editingcells.fictions.ui.components.FolderEditorBottomSheet
import com.editingcells.fictions.ui.components.ImportRenameBottomSheet
import com.editingcells.fictions.ui.components.LocalGlassScrollPhase
import com.editingcells.fictions.ui.components.LocalVaultBackdrop
import com.editingcells.fictions.ui.components.MainTab
import com.editingcells.fictions.ui.components.SelectionFloatingActionPill
import com.editingcells.fictions.ui.screens.FavoritesScreen
import com.editingcells.fictions.ui.screens.FolderDetailScreen
import com.editingcells.fictions.ui.screens.FoldersScreen
import com.editingcells.fictions.ui.screens.HomeScreen
import com.editingcells.fictions.ui.screens.LibraryScreen
import com.editingcells.fictions.ui.screens.NoteEditorScreen
import com.editingcells.fictions.ui.screens.SearchScreen
import com.editingcells.fictions.ui.screens.SettingsScreen
import com.editingcells.fictions.ui.screens.SplashScreen
import com.editingcells.fictions.ui.screens.ViewerScreen
import com.editingcells.fictions.ui.theme.FictionsTheme
import com.editingcells.fictions.ui.theme.LocalResponsiveLayout
import com.editingcells.fictions.ui.viewmodel.VaultUiEvent
import com.editingcells.fictions.ui.viewmodel.VaultViewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

sealed interface VaultDestination {
    data object Splash : VaultDestination
    data class Main(val tab: MainTab) : VaultDestination
    data object Favorites : VaultDestination
    data object Search : VaultDestination
    data class FolderDetail(val folderId: Long) : VaultDestination
    data class Viewer(val fileId: Long) : VaultDestination
    data class NoteEditor(
        val existingFile: VaultFile? = null,
        val initialBody: String = "",
        val folderId: Long? = null
    ) : VaultDestination
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: VaultViewModel = hiltViewModel()
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()

            FictionsTheme(
                pastelTheme = preferences.theme,
                themeMode = preferences.themeMode
            ) {
                FictionsVaultRoot(viewModel = viewModel)
            }
        }
    }
}

private fun checkNotificationPermissionGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun checkGalleryPermissionGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val images = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
        val video = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_VIDEO
        ) == PackageManager.PERMISSION_GRANTED
        val visualSelected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED
        } else false
        images || video || visualSelected
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun checkAllFilesAccessGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        runCatching { Environment.isExternalStorageManager() }.getOrDefault(false)
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun buildRuntimePermissionsArray(): Array<String> {
    val perms = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        perms.add(Manifest.permission.POST_NOTIFICATIONS)
        perms.add(Manifest.permission.READ_MEDIA_IMAGES)
        perms.add(Manifest.permission.READ_MEDIA_VIDEO)
        perms.add(Manifest.permission.READ_MEDIA_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            perms.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        }
    } else {
        perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    return perms.toTypedArray()
}

@Composable
fun FictionsVaultRoot(
    viewModel: VaultViewModel
) {
    val context = LocalContext.current
    val responsive = LocalResponsiveLayout.current
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val allFiles by viewModel.allFiles.collectAsStateWithLifecycle()
    val filteredLibraryFiles by viewModel.filteredLibraryFiles.collectAsStateWithLifecycle()
    val recentFiles by viewModel.recentFiles.collectAsStateWithLifecycle()
    val favoriteFiles by viewModel.favoriteFiles.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val storageSummary by viewModel.storageSummary.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val viewMode by viewModel.effectiveViewMode.collectAsStateWithLifecycle()
    val sortOption by viewModel.effectiveSortOption.collectAsStateWithLifecycle()
    val sortAscending by viewModel.effectiveSortAscending.collectAsStateWithLifecycle()
    val selectedFileIds by viewModel.selectedFileIds.collectAsStateWithLifecycle()
    val activeActionFile by viewModel.activeActionFile.collectAsStateWithLifecycle()
    val pendingImportItems by viewModel.pendingImportItems.collectAsStateWithLifecycle()
    val pendingImportSheetFolderId by viewModel.pendingImportFolderId.collectAsStateWithLifecycle()
    val viewerState by viewModel.viewerState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var currentDestination by remember { mutableStateOf<VaultDestination>(VaultDestination.Splash) }
    var previousTab by remember { mutableStateOf(MainTab.HOME) }
    var bottomBarScrollVisible by remember { mutableStateOf(true) }
    var glassScrollPhase by remember { mutableFloatStateOf(0f) }

    // Permission states
    var hasNotificationPermission by remember {
        mutableStateOf(checkNotificationPermissionGranted(context))
    }
    var hasGalleryPermission by remember {
        mutableStateOf(checkGalleryPermissionGranted(context))
    }
    var hasAllFilesAccess by remember {
        mutableStateOf(checkAllFilesAccessGranted(context))
    }
    var initialPermissionPromptDone by remember { mutableStateOf(false) }

    val refreshPermissionsState = {
        hasNotificationPermission = checkNotificationPermissionGranted(context)
        hasGalleryPermission = checkGalleryPermissionGranted(context)
        hasAllFilesAccess = checkAllFilesAccessGranted(context)
    }

    val runtimePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshPermissionsState()
    }

    val allFilesAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        refreshPermissionsState()
    }

    val launchAllFilesAccessSettings = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val specificIntent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            runCatching {
                allFilesAccessLauncher.launch(specificIntent)
            }.onFailure {
                runCatching { allFilesAccessLauncher.launch(fallbackIntent) }
            }
        } else {
            runtimePermissionsLauncher.launch(buildRuntimePermissionsArray())
        }
    }

    // Sheet & Dialog states
    var showAddSheet by remember { mutableStateOf(false) }
    var showFolderEditorSheet by remember { mutableStateOf(false) }
    var folderBeingEdited by remember { mutableStateOf<VaultFolder?>(null) }
    var showBatchMoveSheet by remember { mutableStateOf(false) }

    // SAF import context state
    var pendingPickerFolderId by remember { mutableStateOf<Long?>(null) }
    var pendingPickerForcedType by remember { mutableStateOf<VaultFileType?>(null) }

    val multiDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            viewModel.prepareImportFromUris(
                uris = uris,
                folderId = pendingPickerFolderId,
                forcedType = pendingPickerForcedType
            )
        }
        pendingPickerFolderId = null
        pendingPickerForcedType = null
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.prepareImportFromUris(
                uris = uris,
                folderId = pendingPickerFolderId,
                forcedType = null
            )
        }
        pendingPickerFolderId = null
        pendingPickerForcedType = null
    }

    val folderTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.prepareImportFromFolderTree(
                treeUri = treeUri,
                folderId = pendingPickerFolderId
            )
        }
        pendingPickerFolderId = null
    }

    val exportCopyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { targetUri ->
        viewModel.completeExportCopy(targetUri)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is VaultUiEvent.ShowUndoDeleteSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoLastDelete()
                    }
                }
                is VaultUiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is VaultUiEvent.LaunchShareIntent -> {
                    runCatching { context.startActivity(event.intent) }
                }
                is VaultUiEvent.RequestExportDestination -> {
                    exportCopyLauncher.launch(event.suggestedFileName)
                }
                is VaultUiEvent.NavigateToViewer -> {
                    viewModel.loadFileForViewer(event.fileId)
                    currentDestination = VaultDestination.Viewer(event.fileId)
                }
            }
        }
    }

    // Automatically request runtime permissions (Notification, Gallery, Storage) once Splash finishes
    LaunchedEffect(currentDestination) {
        if (currentDestination !is VaultDestination.Splash && !initialPermissionPromptDone) {
            initialPermissionPromptDone = true
            refreshPermissionsState()
            if (!hasNotificationPermission || !hasGalleryPermission) {
                val perms = buildRuntimePermissionsArray()
                if (perms.isNotEmpty()) {
                    runCatching { runtimePermissionsLauncher.launch(perms) }
                }
            }
        }
    }

    // Back handler when multi-select mode is active or on non-HOME tab
    val selectionMode = selectedFileIds.isNotEmpty()
    BackHandler(enabled = selectionMode || (currentDestination is VaultDestination.Main && (currentDestination as VaultDestination.Main).tab != MainTab.HOME)) {
        if (selectionMode) {
            viewModel.clearSelection()
        } else {
            previousTab = MainTab.HOME
            bottomBarScrollVisible = true
            currentDestination = VaultDestination.Main(MainTab.HOME)
        }
    }

    // Kyant0/AndroidLiquidGlass LayerBackdrop that captures the screen content
    val mainBackdrop = rememberLayerBackdrop()

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Z-Index 0: Recorded background & solid screen content layer
        // (LocalVaultBackdrop is null inside here to prevent RenderNode self-draw cycles)
        CompositionLocalProvider(LocalVaultBackdrop provides null) {
            AmbientVaultBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f)
                    .layerBackdrop(mainBackdrop)
            ) {
                Crossfade(
                    targetState = currentDestination,
                    animationSpec = tween(durationMillis = 240),
                    label = "vault_screen_transition"
                ) { destination ->
                    when (destination) {
                        VaultDestination.Splash -> {
                            SplashScreen(
                                onFinished = {
                                    currentDestination = VaultDestination.Main(MainTab.HOME)
                                }
                            )
                        }

                        is VaultDestination.Main -> {
                            when (destination.tab) {
                                MainTab.HOME -> {
                                    HomeScreen(
                                        storageSummary = storageSummary,
                                        recentFiles = recentFiles,
                                        favoriteFiles = favoriteFiles,
                                        folders = folders,
                                        selectedTypeFilter = selectedTypeFilter,
                                        selectedFileIds = selectedFileIds,
                                        isLoading = isLoading,
                                        errorMessage = errorMessage,
                                        onDismissError = viewModel::clearError,
                                        onSelectTypeFilter = viewModel::setTypeFilter,
                                        onOpenSearch = {
                                            currentDestination = VaultDestination.Search
                                        },
                                        onOpenFavorites = {
                                            currentDestination = VaultDestination.Favorites
                                        },
                                        onOpenSettings = {
                                            previousTab = MainTab.SETTINGS
                                            bottomBarScrollVisible = true
                                            currentDestination = VaultDestination.Main(MainTab.SETTINGS)
                                        },
                                        onOpenLibraryWithType = { type ->
                                            viewModel.setTypeFilterExplicit(type)
                                            previousTab = MainTab.LIBRARY
                                            bottomBarScrollVisible = true
                                            currentDestination = VaultDestination.Main(MainTab.LIBRARY)
                                        },
                                        onOpenFolder = { folder ->
                                            currentDestination = VaultDestination.FolderDetail(folder.id)
                                        },
                                        onOpenAllFolders = {
                                            previousTab = MainTab.FOLDERS
                                            bottomBarScrollVisible = true
                                            currentDestination = VaultDestination.Main(MainTab.FOLDERS)
                                        },
                                        onNewFolder = {
                                            folderBeingEdited = null
                                            showFolderEditorSheet = true
                                        },
                                        onAddClick = { showAddSheet = true },
                                        onFileClick = { file ->
                                            viewModel.loadFileForViewer(file.id)
                                            currentDestination = VaultDestination.Viewer(file.id)
                                        },
                                        onFileLongClick = { file ->
                                            viewModel.toggleFileSelection(file.id)
                                        },
                                        onToggleFavorite = viewModel::toggleFavorite,
                                        onOpenFileMenu = viewModel::openFileActionsSheet,
                                        onScrollDirectionChanged = { up ->
                                            bottomBarScrollVisible = up
                                        },
                                        onScrollPhaseChanged = { phase ->
                                            glassScrollPhase = phase
                                        }
                                    )
                                }

                                MainTab.LIBRARY -> {
                                    LibraryScreen(
                                        files = filteredLibraryFiles,
                                        storageSummary = storageSummary,
                                        searchQuery = searchQuery,
                                        selectedTypeFilter = selectedTypeFilter,
                                        viewMode = viewMode,
                                        sortOption = sortOption,
                                        sortAscending = sortAscending,
                                        selectedFileIds = selectedFileIds,
                                        isLoading = isLoading,
                                        errorMessage = errorMessage,
                                        onDismissError = viewModel::clearError,
                                        onSearchQueryChange = viewModel::setSearchQuery,
                                        onSelectTypeFilter = viewModel::setTypeFilter,
                                        onToggleViewMode = viewModel::toggleViewMode,
                                        onSelectSortOption = viewModel::setSortOption,
                                        onSelectAll = {
                                            if (selectedFileIds.size == filteredLibraryFiles.size) {
                                                viewModel.clearSelection()
                                            } else {
                                                viewModel.selectAllVisible(filteredLibraryFiles)
                                            }
                                        },
                                        onAddClick = { showAddSheet = true },
                                        onFileClick = { file ->
                                            viewModel.loadFileForViewer(file.id)
                                            currentDestination = VaultDestination.Viewer(file.id)
                                        },
                                        onFileLongClick = { file ->
                                            viewModel.toggleFileSelection(file.id)
                                        },
                                        onToggleFavorite = viewModel::toggleFavorite,
                                        onOpenFileMenu = viewModel::openFileActionsSheet,
                                        onScrollDirectionChanged = { up ->
                                            bottomBarScrollVisible = up
                                        },
                                        onScrollPhaseChanged = { phase ->
                                            glassScrollPhase = phase
                                        }
                                    )
                                }

                                MainTab.FOLDERS -> {
                                    FoldersScreen(
                                        folders = folders,
                                        unfiledCount = allFiles.count { it.folderId == null },
                                        onOpenFolder = { folder ->
                                            currentDestination = VaultDestination.FolderDetail(folder.id)
                                        },
                                        onEditFolder = { folder ->
                                            folderBeingEdited = folder
                                            showFolderEditorSheet = true
                                        },
                                        onCreateFolder = {
                                            folderBeingEdited = null
                                            showFolderEditorSheet = true
                                        },
                                        onScrollDirectionChanged = { up ->
                                            bottomBarScrollVisible = up
                                        },
                                        onScrollPhaseChanged = { phase ->
                                            glassScrollPhase = phase
                                        }
                                    )
                                }

                                MainTab.SETTINGS -> {
                                    SettingsScreen(
                                        preferences = preferences,
                                        storageSummary = storageSummary,
                                        hasNotificationPermission = hasNotificationPermission,
                                        hasGalleryPermission = hasGalleryPermission,
                                        hasAllFilesAccess = hasAllFilesAccess,
                                        onRequestPermissions = {
                                            runtimePermissionsLauncher.launch(buildRuntimePermissionsArray())
                                        },
                                        onRequestAllFilesAccess = {
                                            launchAllFilesAccessSettings()
                                        },
                                        onImportAllFiles = {
                                            pendingPickerFolderId = null
                                            pendingPickerForcedType = null
                                            multiDocumentLauncher.launch(arrayOf("*/*"))
                                        },
                                        onScanDeviceStorage = {
                                            if (!hasAllFilesAccess) {
                                                launchAllFilesAccessSettings()
                                            } else {
                                                viewModel.scanDeviceStorageForImport(null)
                                            }
                                        },
                                        onSelectTheme = viewModel::setPastelTheme,
                                        onSelectThemeMode = viewModel::setThemeMode,
                                        onSelectDefaultView = viewModel::setDefaultViewMode,
                                        onSelectDefaultSort = viewModel::setDefaultSortOption,
                                        onClearAllVaultData = viewModel::clearAllVaultData,
                                        onScrollDirectionChanged = { up ->
                                            bottomBarScrollVisible = up
                                        },
                                        onScrollPhaseChanged = { phase ->
                                            glassScrollPhase = phase
                                        }
                                    )
                                }
                            }
                        }

                        VaultDestination.Favorites -> {
                            FavoritesScreen(
                                favoriteFiles = favoriteFiles,
                                viewMode = viewMode,
                                selectedFileIds = selectedFileIds,
                                onBack = {
                                    currentDestination = VaultDestination.Main(previousTab)
                                },
                                onToggleViewMode = viewModel::toggleViewMode,
                                onFileClick = { file ->
                                    viewModel.loadFileForViewer(file.id)
                                    currentDestination = VaultDestination.Viewer(file.id)
                                },
                                onFileLongClick = { file ->
                                    viewModel.toggleFileSelection(file.id)
                                },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onOpenFileMenu = viewModel::openFileActionsSheet
                            )
                        }

                        VaultDestination.Search -> {
                            SearchScreen(
                                allFiles = allFiles,
                                searchQuery = searchQuery,
                                selectedTypeFilter = selectedTypeFilter,
                                onSearchQueryChange = viewModel::setSearchQuery,
                                onSelectTypeFilter = viewModel::setTypeFilter,
                                onBack = {
                                    viewModel.setSearchQuery("")
                                    currentDestination = VaultDestination.Main(previousTab)
                                },
                                onFileClick = { file ->
                                    viewModel.loadFileForViewer(file.id)
                                    currentDestination = VaultDestination.Viewer(file.id)
                                },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onOpenFileMenu = viewModel::openFileActionsSheet
                            )
                        }

                        is VaultDestination.FolderDetail -> {
                            val folder = folders.firstOrNull { it.id == destination.folderId }
                            if (folder == null) {
                                LaunchedEffect(Unit) {
                                    currentDestination = VaultDestination.Main(MainTab.FOLDERS)
                                }
                            } else {
                                val folderFiles = allFiles.filter { it.folderId == folder.id }
                                FolderDetailScreen(
                                    folder = folder,
                                    files = folderFiles,
                                    viewMode = viewMode,
                                    selectedFileIds = selectedFileIds,
                                    onBack = {
                                        currentDestination = VaultDestination.Main(MainTab.FOLDERS)
                                    },
                                    onEditFolder = {
                                        folderBeingEdited = folder
                                        showFolderEditorSheet = true
                                    },
                                    onToggleViewMode = viewModel::toggleViewMode,
                                    onImportToFolder = {
                                        pendingPickerFolderId = folder.id
                                        pendingPickerForcedType = null
                                        multiDocumentLauncher.launch(arrayOf("*/*"))
                                    },
                                    onNewNoteInFolder = {
                                        currentDestination = VaultDestination.NoteEditor(folderId = folder.id)
                                    },
                                    onFileClick = { file ->
                                        viewModel.loadFileForViewer(file.id)
                                        currentDestination = VaultDestination.Viewer(file.id)
                                    },
                                    onFileLongClick = { file ->
                                        viewModel.toggleFileSelection(file.id)
                                    },
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onOpenFileMenu = viewModel::openFileActionsSheet
                                )
                            }
                        }

                        is VaultDestination.Viewer -> {
                            ViewerScreen(
                                viewerState = viewerState,
                                readerFontSizeSp = preferences.readerFontSizeSp,
                                onBack = {
                                    currentDestination = VaultDestination.Main(previousTab)
                                },
                                onOpenActionsSheet = viewModel::openFileActionsSheet,
                                onShareFile = { file -> viewModel.shareFiles(listOf(file)) },
                                onEditTxtNote = { file, content ->
                                    currentDestination = VaultDestination.NoteEditor(
                                        existingFile = file,
                                        initialBody = content,
                                        folderId = file.folderId
                                    )
                                },
                                onAdjustFontSize = viewModel::adjustReaderFontSize
                            )
                        }

                        is VaultDestination.NoteEditor -> {
                            NoteEditorScreen(
                                existingFile = destination.existingFile,
                                initialBody = destination.initialBody,
                                folders = folders,
                                preselectedFolderId = destination.folderId,
                                onBack = {
                                    if (destination.existingFile != null) {
                                        viewModel.loadFileForViewer(destination.existingFile.id)
                                        currentDestination = VaultDestination.Viewer(destination.existingFile.id)
                                    } else {
                                        currentDestination = VaultDestination.Main(previousTab)
                                    }
                                },
                                onSaveNote = { existingId, title, body, folderId, tags ->
                                    viewModel.saveTxtNote(
                                        existingFileId = existingId,
                                        title = title,
                                        body = body,
                                        folderId = folderId,
                                        tags = tags
                                    ) { savedId ->
                                        viewModel.loadFileForViewer(savedId)
                                        currentDestination = VaultDestination.Viewer(savedId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // 2. Foreground Liquid Glass overlays sampling mainBackdrop (outside layerBackdrop to prevent cycles)
        // Strict z-order: Content (0) -> Floating Bars & Docked FAB (1) -> Sheets (2) -> Snackbars (3)
        // Max 3 glass layers visible at once: when any sheet is open, hide the bottom bar & FAB.
        CompositionLocalProvider(
            LocalVaultBackdrop provides mainBackdrop,
            LocalGlassScrollPhase provides glassScrollPhase
        ) {
            val isMainScreen = currentDestination is VaultDestination.Main
            val activeTab = (currentDestination as? VaultDestination.Main)?.tab ?: previousTab
            val anySheetOpen = showAddSheet ||
                pendingImportItems.isNotEmpty() ||
                activeActionFile != null ||
                showFolderEditorSheet ||
                showBatchMoveSheet

            FloatingPillBottomBar(
                currentTab = activeTab,
                visible = isMainScreen && !selectionMode && bottomBarScrollVisible && !anySheetOpen,
                onSelectTab = { tab ->
                    previousTab = tab
                    bottomBarScrollVisible = true
                    currentDestination = VaultDestination.Main(tab)
                },
                onAddClick = { showAddSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(1f)
            )

            // Multi-select Floating Dark Glass Action Pill
            SelectionFloatingActionPill(
                selectedCount = selectedFileIds.size,
                visible = selectionMode && !anySheetOpen,
                onSelectAll = {
                    viewModel.selectAllVisible(filteredLibraryFiles)
                },
                onMoveSelected = { showBatchMoveSheet = true },
                onShareSelected = viewModel::shareSelectedFiles,
                onDeleteSelected = {
                    viewModel.deleteFilesWithUndo(selectedFileIds.toList())
                },
                onClearSelection = viewModel::clearSelection,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(1f)
            )

            // 1. Center "+" FAB Glass Bottom Sheet
            if (showAddSheet) {
                AddToVaultBottomSheet(
                    onDismiss = { showAddSheet = false },
                    onImportAllFiles = {
                        pendingPickerFolderId = null
                        pendingPickerForcedType = null
                        multiDocumentLauncher.launch(arrayOf("*/*"))
                    },
                    onImportFromGallery = {
                        if (!hasGalleryPermission) {
                            runtimePermissionsLauncher.launch(buildRuntimePermissionsArray())
                        }
                        pendingPickerFolderId = null
                        pendingPickerForcedType = null
                        galleryPickerLauncher.launch("*/*")
                    },
                    onImportFolderTree = {
                        pendingPickerFolderId = null
                        folderTreeLauncher.launch(null)
                    },
                    onScanDeviceStorage = {
                        if (!hasAllFilesAccess) {
                            launchAllFilesAccessSettings()
                        } else {
                            viewModel.scanDeviceStorageForImport(null)
                        }
                    },
                    onImportPdf = {
                        pendingPickerFolderId = null
                        pendingPickerForcedType = VaultFileType.PDF
                        multiDocumentLauncher.launch(VaultFileType.PDF.mimeTypes)
                    },
                    onImportCsv = {
                        pendingPickerFolderId = null
                        pendingPickerForcedType = VaultFileType.CSV
                        multiDocumentLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
                    },
                    onImportTxt = {
                        pendingPickerFolderId = null
                        pendingPickerForcedType = VaultFileType.TXT
                        multiDocumentLauncher.launch(VaultFileType.TXT.mimeTypes)
                    },
                    onNewNote = {
                        currentDestination = VaultDestination.NoteEditor()
                    },
                    onNewFolder = {
                        folderBeingEdited = null
                        showFolderEditorSheet = true
                    }
                )
            }

            // 2. Import & Rename Before Saving Sheet
            if (pendingImportItems.isNotEmpty()) {
                ImportRenameBottomSheet(
                    items = pendingImportItems,
                    folders = folders,
                    initialFolderId = pendingImportSheetFolderId,
                    onUpdateItemName = viewModel::updatePendingImportItemName,
                    onUpdateItemType = viewModel::updatePendingImportItemType,
                    onRemoveItem = viewModel::removePendingImportItem,
                    onDismiss = viewModel::dismissPendingImport,
                    onConfirmSave = { targetFolderId, tags ->
                        viewModel.confirmPendingImportWithRename(targetFolderId, tags)
                    }
                )
            }

            // 3. File Actions Glass Bottom Sheet
            activeActionFile?.let { actionFile ->
                FileActionsBottomSheet(
                    file = actionFile,
                    folders = folders,
                    onDismiss = viewModel::dismissFileActionsSheet,
                    onOpen = {
                        viewModel.loadFileForViewer(actionFile.id)
                        currentDestination = VaultDestination.Viewer(actionFile.id)
                    },
                    onEditTxt = if (actionFile.type == VaultFileType.TXT) {
                        {
                            viewModel.loadFileForViewer(actionFile.id)
                            currentDestination = VaultDestination.NoteEditor(
                                existingFile = actionFile,
                                initialBody = viewerState.txtContent,
                                folderId = actionFile.folderId
                            )
                        }
                    } else null,
                    onRename = { newName ->
                        viewModel.renameFile(actionFile.id, newName)
                    },
                    onMoveToFolder = { folderId ->
                        viewModel.moveFileToFolder(actionFile.id, folderId)
                    },
                    onToggleFavorite = {
                        viewModel.toggleFavorite(actionFile)
                    },
                    onUpdateTags = { updatedTags ->
                        viewModel.updateFileTags(actionFile.id, updatedTags)
                    },
                    onShare = {
                        viewModel.shareFiles(listOf(actionFile))
                    },
                    onExportCopy = { customFileName ->
                        viewModel.requestExportCopy(actionFile, customFileName)
                    },
                    onDelete = {
                        val deletingViewedFile =
                            (currentDestination as? VaultDestination.Viewer)?.fileId == actionFile.id
                        viewModel.deleteFilesWithUndo(listOf(actionFile.id))
                        if (deletingViewedFile) {
                            currentDestination = VaultDestination.Main(previousTab)
                        }
                    }
                )
            }

            // 4. Create / Edit Folder Glass Bottom Sheet
            if (showFolderEditorSheet) {
                FolderEditorBottomSheet(
                    existingFolder = folderBeingEdited,
                    onDismiss = {
                        showFolderEditorSheet = false
                        folderBeingEdited = null
                    },
                    onSave = { name, colorHex ->
                        val editing = folderBeingEdited
                        if (editing == null) {
                            viewModel.createFolder(name, colorHex)
                        } else {
                            viewModel.updateFolder(editing.id, name, colorHex)
                        }
                    },
                    onDelete = folderBeingEdited?.let { folder ->
                        {
                            viewModel.deleteFolder(folder.id)
                            if (currentDestination == VaultDestination.FolderDetail(folder.id)) {
                                currentDestination = VaultDestination.Main(MainTab.FOLDERS)
                            }
                        }
                    }
                )
            }

            // 5. Batch Move Selected Files Sheet
            if (showBatchMoveSheet) {
                BatchMoveToFolderSheet(
                    folders = folders,
                    onDismiss = { showBatchMoveSheet = false },
                    onSelectFolder = { targetFolderId ->
                        viewModel.moveSelectedFilesToFolder(targetFolderId)
                    }
                )
            }

            // 6. Topmost Z-Index (3f): Snackbar Host (Undo Delete & Status Messages)
            // Always positioned above the floating bar & FAB, never overlapping them.
            val barElevated = (isMainScreen && !anySheetOpen && bottomBarScrollVisible) || selectionMode
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(3f)
                    .widthIn(max = responsive.floatingBarMaxWidth)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
                    .padding(
                        bottom = if (barElevated) responsive.bottomBarClearance else 24.dp,
                        start = responsive.screenMargin,
                        end = responsive.screenMargin
                    )
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(24.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.inversePrimary
                )
            }
        }
    }
}
