package com.editingcells.fictions.ui.viewmodel

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.editingcells.fictions.data.preferences.PreferencesManager
import com.editingcells.fictions.data.repository.DeletedFileBackup
import com.editingcells.fictions.data.repository.VaultRepository
import com.editingcells.fictions.domain.model.CsvTableData
import com.editingcells.fictions.domain.model.PastelTheme
import com.editingcells.fictions.domain.model.PendingImportItem
import com.editingcells.fictions.domain.model.SortOption
import com.editingcells.fictions.domain.model.StorageSummary
import com.editingcells.fictions.domain.model.ThemeMode
import com.editingcells.fictions.domain.model.UserPreferences
import com.editingcells.fictions.domain.model.VaultFile
import com.editingcells.fictions.domain.model.VaultFileType
import com.editingcells.fictions.domain.model.VaultFolder
import com.editingcells.fictions.domain.model.ViewMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface VaultUiEvent {
    data class ShowUndoDeleteSnackbar(val message: String) : VaultUiEvent
    data class ShowMessage(val message: String) : VaultUiEvent
    data class LaunchShareIntent(val intent: Intent) : VaultUiEvent
    data class RequestExportDestination(val file: VaultFile, val suggestedFileName: String) : VaultUiEvent
    data class NavigateToViewer(val fileId: Long) : VaultUiEvent
}

data class ViewerUiState(
    val isLoading: Boolean = false,
    val file: VaultFile? = null,
    val txtContent: String = "",
    val csvData: CsvTableData? = null,
    val error: String? = null
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: VaultRepository,
    private val preferencesManager: PreferencesManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesManager.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    val allFiles: StateFlow<List<VaultFile>> = repository.filesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val folders: StateFlow<List<VaultFolder>> = repository.foldersFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val storageSummary: StateFlow<StorageSummary> = repository.storageSummaryFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StorageSummary()
        )

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Search & filter state (process-death safe via SavedStateHandle)
    val searchQuery: StateFlow<String> = savedStateHandle.getStateFlow("search_query", "")

    private val _selectedTypeFilter = MutableStateFlow<VaultFileType?>(null)
    val selectedTypeFilter: StateFlow<VaultFileType?> = _selectedTypeFilter.asStateFlow()

    private val _overrideViewMode = MutableStateFlow<ViewMode?>(null)
    private val _overrideSortOption = MutableStateFlow<SortOption?>(null)
    private val _overrideSortAscending = MutableStateFlow<Boolean?>(null)

    val effectiveViewMode: StateFlow<ViewMode> = combine(
        preferences,
        _overrideViewMode
    ) { prefs, override ->
        override ?: prefs.defaultViewMode
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewMode.GRID)

    val effectiveSortOption: StateFlow<SortOption> = combine(
        preferences,
        _overrideSortOption
    ) { prefs, override ->
        override ?: prefs.defaultSortOption
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SortOption.DATE)

    val effectiveSortAscending: StateFlow<Boolean> = combine(
        preferences,
        _overrideSortAscending
    ) { prefs, override ->
        override ?: prefs.sortAscending
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Filtered + Sorted library files
    val filteredLibraryFiles: StateFlow<List<VaultFile>> = combine(
        allFiles,
        searchQuery,
        _selectedTypeFilter,
        effectiveSortOption,
        effectiveSortAscending
    ) { files, query, typeFilter, sortOption, ascending ->
        val filtered = files.filter { file ->
            val matchesType = typeFilter == null || file.type == typeFilter
            val q = query.trim()
            val matchesQuery = q.isEmpty() ||
                file.name.contains(q, ignoreCase = true) ||
                file.folderName?.contains(q, ignoreCase = true) == true ||
                file.tags.any { it.contains(q, ignoreCase = true) }
            matchesType && matchesQuery
        }

        val sorted = when (sortOption) {
            SortOption.DATE -> filtered.sortedBy { it.dateAdded }
            SortOption.NAME -> filtered.sortedBy { it.name.lowercase() }
            SortOption.SIZE -> filtered.sortedBy { it.sizeBytes }
            SortOption.TYPE -> filtered.sortedBy { it.type.label }
        }
        if (ascending) sorted else sorted.reversed()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentFiles: StateFlow<List<VaultFile>> = allFiles.combine(_selectedTypeFilter) { files, filter ->
        files
            .filter { filter == null || it.type == filter }
            .sortedByDescending { it.lastOpened ?: it.dateAdded }
            .take(8)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteFiles: StateFlow<List<VaultFile>> = allFiles.combine(_selectedTypeFilter) { files, filter ->
        files.filter { it.isFavorite && (filter == null || it.type == filter) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Multi-select state
    private val _selectedFileIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedFileIds: StateFlow<Set<Long>> = _selectedFileIds.asStateFlow()

    // Active file for bottom sheet actions
    private val _activeActionFile = MutableStateFlow<VaultFile?>(null)
    val activeActionFile: StateFlow<VaultFile?> = _activeActionFile.asStateFlow()

    // Pending import items for Import & Rename before Saving sheet
    private val _pendingImportItems = MutableStateFlow<List<PendingImportItem>>(emptyList())
    val pendingImportItems: StateFlow<List<PendingImportItem>> = _pendingImportItems.asStateFlow()

    private val _pendingImportFolderId = MutableStateFlow<Long?>(null)
    val pendingImportFolderId: StateFlow<Long?> = _pendingImportFolderId.asStateFlow()

    // Viewer state
    private val _viewerState = MutableStateFlow(ViewerUiState())
    val viewerState: StateFlow<ViewerUiState> = _viewerState.asStateFlow()

    // Pending export file + custom export name
    private var pendingExportFile: VaultFile? = null
    private var pendingExportCustomName: String? = null

    // Last deleted backups for Undo Snackbar
    private var lastDeletedBackups: List<DeletedFileBackup> = emptyList()

    private val _events = MutableSharedFlow<VaultUiEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                repository.purgeAnyLegacyDemoData()
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "Failed to initialize vault"
            }
            _isLoading.value = false
        }
    }

    fun setSearchQuery(query: String) {
        savedStateHandle["search_query"] = query
    }

    fun setTypeFilter(type: VaultFileType?) {
        _selectedTypeFilter.value = if (_selectedTypeFilter.value == type) null else type
    }

    fun setTypeFilterExplicit(type: VaultFileType?) {
        _selectedTypeFilter.value = type
    }

    fun toggleViewMode() {
        val next = if (effectiveViewMode.value == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
        _overrideViewMode.value = next
        viewModelScope.launch {
            preferencesManager.setDefaultViewMode(next)
        }
    }

    fun setSortOption(option: SortOption) {
        if (effectiveSortOption.value == option) {
            val nextAsc = !effectiveSortAscending.value
            _overrideSortAscending.value = nextAsc
            viewModelScope.launch { preferencesManager.setSortAscending(nextAsc) }
        } else {
            _overrideSortOption.value = option
            viewModelScope.launch { preferencesManager.setDefaultSortOption(option) }
        }
    }

    // Multi-select operations
    fun toggleFileSelection(fileId: Long) {
        val current = _selectedFileIds.value.toMutableSet()
        if (!current.add(fileId)) {
            current.remove(fileId)
        }
        _selectedFileIds.value = current
    }

    fun selectAllVisible(files: List<VaultFile>) {
        _selectedFileIds.value = files.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedFileIds.value = emptySet()
    }

    // Bottom sheet trigger
    fun openFileActionsSheet(file: VaultFile) {
        _activeActionFile.value = file
    }

    fun dismissFileActionsSheet() {
        _activeActionFile.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Import & Rename before Saving operations
    fun prepareImportFromUris(
        uris: List<Uri>,
        folderId: Long? = null,
        forcedType: VaultFileType? = null
    ) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                val inspected = repository.inspectPickedUris(uris, forcedType)
                _pendingImportFolderId.value = folderId
                _pendingImportItems.value = inspected
            }.onFailure { err ->
                _errorMessage.value = "Failed to inspect selected files: ${err.localizedMessage}"
            }
            _isLoading.value = false
        }
    }

    fun prepareImportFromFolderTree(
        treeUri: Uri,
        folderId: Long? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                val inspected = repository.inspectDirectoryTree(treeUri)
                if (inspected.isEmpty()) {
                    _events.emit(VaultUiEvent.ShowMessage("No readable files found in selected folder"))
                } else {
                    _pendingImportFolderId.value = folderId
                    _pendingImportItems.value = inspected
                }
            }.onFailure { err ->
                _errorMessage.value = "Folder scan failed: ${err.localizedMessage}"
            }
            _isLoading.value = false
        }
    }

    fun scanDeviceStorageForImport(folderId: Long? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                val found = repository.scanDeviceDocumentsForImport()
                if (found.isEmpty()) {
                    _events.emit(
                        VaultUiEvent.ShowMessage("No PDF, CSV, or TXT files found in device Downloads/Documents")
                    )
                } else {
                    _pendingImportFolderId.value = folderId
                    _pendingImportItems.value = found
                }
            }.onFailure { err ->
                _errorMessage.value = "Storage scan failed: ${err.localizedMessage}"
            }
            _isLoading.value = false
        }
    }

    fun updatePendingImportItemName(index: Int, newName: String) {
        val current = _pendingImportItems.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(customName = newName)
            _pendingImportItems.value = current
        }
    }

    fun updatePendingImportItemType(index: Int, newType: VaultFileType) {
        val current = _pendingImportItems.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(detectedType = newType)
            _pendingImportItems.value = current
        }
    }

    fun removePendingImportItem(index: Int) {
        val current = _pendingImportItems.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _pendingImportItems.value = current
        }
    }

    fun dismissPendingImport() {
        _pendingImportItems.value = emptyList()
        _pendingImportFolderId.value = null
    }

    fun confirmPendingImportWithRename(
        targetFolderId: Long?,
        tags: List<String>
    ) {
        val items = _pendingImportItems.value
        if (items.isEmpty()) return
        _pendingImportItems.value = emptyList()
        _pendingImportFolderId.value = null

        viewModelScope.launch {
            _isLoading.value = true
            repository.importPendingItemsWithRename(
                items = items,
                targetFolderId = targetFolderId,
                tags = tags
            ).onSuccess { count ->
                _events.emit(
                    VaultUiEvent.ShowMessage("Saved $count file${if (count == 1) "" else "s"} into vault")
                )
            }.onFailure { err ->
                _errorMessage.value = "Import failed: ${err.localizedMessage ?: "Unknown error"}"
            }
            _isLoading.value = false
        }
    }

    fun saveTxtNote(
        existingFileId: Long?,
        title: String,
        body: String,
        folderId: Long?,
        tags: List<String>,
        onSaved: (Long) -> Unit
    ) {
        viewModelScope.launch {
            repository.saveTxtNote(existingFileId, title, body, folderId, tags)
                .onSuccess { savedId ->
                    if (_viewerState.value.file?.id == savedId) {
                        loadFileForViewer(savedId)
                    }
                    _events.emit(VaultUiEvent.ShowMessage("Saved note to vault"))
                    onSaved(savedId)
                }
                .onFailure { err ->
                    _errorMessage.value = "Could not save note: ${err.localizedMessage}"
                }
        }
    }

    fun loadFileForViewer(fileId: Long) {
        viewModelScope.launch {
            _viewerState.value = ViewerUiState(isLoading = true)
            repository.markFileOpened(fileId)
            val file = repository.getFileById(fileId)
            if (file == null) {
                _viewerState.value = ViewerUiState(
                    isLoading = false,
                    error = "Document could not be found in vault."
                )
                return@launch
            }

            when (file.type) {
                VaultFileType.PDF -> {
                    _viewerState.value = ViewerUiState(
                        isLoading = false,
                        file = file
                    )
                }
                VaultFileType.CSV -> {
                    repository.parseCsvFile(file.filePath)
                        .onSuccess { table ->
                            _viewerState.value = ViewerUiState(
                                isLoading = false,
                                file = file,
                                csvData = table
                            )
                        }
                        .onFailure { err ->
                            _viewerState.value = ViewerUiState(
                                isLoading = false,
                                file = file,
                                error = err.localizedMessage ?: "Failed to parse CSV file"
                            )
                        }
                }
                VaultFileType.TXT -> {
                    repository.readTextFileContent(file.filePath)
                        .onSuccess { text ->
                            _viewerState.value = ViewerUiState(
                                isLoading = false,
                                file = file,
                                txtContent = text
                            )
                        }
                        .onFailure { err ->
                            _viewerState.value = ViewerUiState(
                                isLoading = false,
                                file = file,
                                error = err.localizedMessage ?: "Failed to read TXT file"
                            )
                        }
                }
            }
        }
    }

    fun toggleFavorite(file: VaultFile) {
        viewModelScope.launch {
            repository.toggleFavorite(file.id, !file.isFavorite)
            if (_activeActionFile.value?.id == file.id) {
                _activeActionFile.value = file.copy(isFavorite = !file.isFavorite)
            }
            if (_viewerState.value.file?.id == file.id) {
                _viewerState.value = _viewerState.value.copy(
                    file = file.copy(isFavorite = !file.isFavorite)
                )
            }
        }
    }

    fun renameFile(fileId: Long, newName: String) {
        viewModelScope.launch {
            repository.renameFile(fileId, newName)
            if (_viewerState.value.file?.id == fileId) {
                loadFileForViewer(fileId)
            }
            _events.emit(VaultUiEvent.ShowMessage("File renamed"))
        }
    }

    fun moveFileToFolder(fileId: Long, folderId: Long?) {
        viewModelScope.launch {
            repository.moveFilesToFolder(listOf(fileId), folderId)
            if (_viewerState.value.file?.id == fileId) {
                loadFileForViewer(fileId)
            }
            _events.emit(VaultUiEvent.ShowMessage("Moved file"))
        }
    }

    fun moveSelectedFilesToFolder(folderId: Long?) {
        val ids = _selectedFileIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.moveFilesToFolder(ids, folderId)
            clearSelection()
            _events.emit(VaultUiEvent.ShowMessage("Moved ${ids.size} file${if (ids.size == 1) "" else "s"}"))
        }
    }

    fun updateFileTags(fileId: Long, tags: List<String>) {
        viewModelScope.launch {
            repository.updateFileTags(fileId, tags)
            if (_viewerState.value.file?.id == fileId) {
                loadFileForViewer(fileId)
            }
            _events.emit(VaultUiEvent.ShowMessage("Tags updated"))
        }
    }

    fun deleteFilesWithUndo(fileIds: List<Long>) {
        if (fileIds.isEmpty()) return
        viewModelScope.launch {
            lastDeletedBackups = repository.deleteFilesWithUndoBackup(fileIds)
            clearSelection()
            _events.emit(
                VaultUiEvent.ShowUndoDeleteSnackbar(
                    "Deleted ${fileIds.size} file${if (fileIds.size == 1) "" else "s"}"
                )
            )
        }
    }

    fun undoLastDelete() {
        val backups = lastDeletedBackups
        if (backups.isEmpty()) return
        viewModelScope.launch {
            repository.restoreDeletedFiles(backups)
            lastDeletedBackups = emptyList()
            _events.emit(VaultUiEvent.ShowMessage("Restored to vault"))
        }
    }

    fun shareFiles(files: List<VaultFile>) {
        viewModelScope.launch {
            val intent = repository.createShareIntent(files)
            if (intent != null) {
                _events.emit(
                    VaultUiEvent.LaunchShareIntent(
                        Intent.createChooser(intent, "Share from Fictions Vault")
                    )
                )
            } else {
                _errorMessage.value = "Unable to share file"
            }
        }
    }

    fun shareSelectedFiles() {
        val ids = _selectedFileIds.value
        val selected = allFiles.value.filter { it.id in ids }
        shareFiles(selected)
        clearSelection()
    }

    fun requestExportCopy(file: VaultFile, customFileName: String = file.name) {
        val ext = ".${file.type.extension}"
        val cleanBase = customFileName.trim()
            .removeSuffix(ext)
            .removeSuffix(ext.uppercase())
            .ifBlank { file.name.substringBeforeLast(".") }
        val finalExportName = "$cleanBase$ext"

        pendingExportFile = file
        pendingExportCustomName = finalExportName
        viewModelScope.launch {
            _events.emit(VaultUiEvent.RequestExportDestination(file, finalExportName))
        }
    }

    fun completeExportCopy(destinationUri: Uri?) {
        val file = pendingExportFile ?: return
        val exportedName = pendingExportCustomName ?: file.name
        pendingExportFile = null
        pendingExportCustomName = null
        if (destinationUri == null) return
        viewModelScope.launch {
            repository.exportFileToUri(file, destinationUri, exportedName)
                .onSuccess {
                    _events.emit(VaultUiEvent.ShowMessage("Saved copy as $exportedName"))
                }
                .onFailure { err ->
                    _errorMessage.value = "Export failed: ${err.localizedMessage}"
                }
        }
    }

    // Folder operations
    fun createFolder(name: String, colorHex: Long) {
        viewModelScope.launch {
            repository.createFolder(name, colorHex)
            _events.emit(VaultUiEvent.ShowMessage("Created folder \"$name\""))
        }
    }

    fun updateFolder(folderId: Long, name: String, colorHex: Long) {
        viewModelScope.launch {
            repository.updateFolder(folderId, name, colorHex)
            _events.emit(VaultUiEvent.ShowMessage("Folder updated"))
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            repository.deleteFolder(folderId)
            _events.emit(VaultUiEvent.ShowMessage("Folder deleted (files kept in vault)"))
        }
    }

    // Settings / Preferences operations
    fun setPastelTheme(theme: PastelTheme) {
        viewModelScope.launch { preferencesManager.setTheme(theme) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesManager.setThemeMode(mode) }
    }

    fun setDefaultViewMode(mode: ViewMode) {
        _overrideViewMode.value = mode
        viewModelScope.launch { preferencesManager.setDefaultViewMode(mode) }
    }

    fun setDefaultSortOption(option: SortOption) {
        _overrideSortOption.value = option
        viewModelScope.launch { preferencesManager.setDefaultSortOption(option) }
    }

    fun adjustReaderFontSize(deltaSp: Float) {
        val current = preferences.value.readerFontSizeSp
        viewModelScope.launch {
            preferencesManager.setReaderFontSize((current + deltaSp).coerceIn(12f, 30f))
        }
    }

    fun clearAllVaultData() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.clearAllVaultData()
            _isLoading.value = false
            _events.emit(VaultUiEvent.ShowMessage("All vault files and folders cleared"))
        }
    }
}
