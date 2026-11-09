package com.editingcells.fictions.data.repository

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.editingcells.fictions.data.local.dao.VaultDao
import com.editingcells.fictions.data.local.entity.FolderEntity
import com.editingcells.fictions.data.local.entity.VaultFileEntity
import com.editingcells.fictions.data.preferences.PreferencesManager
import com.editingcells.fictions.domain.model.CsvTableData
import com.editingcells.fictions.domain.model.PendingImportItem
import com.editingcells.fictions.domain.model.StorageSummary
import com.editingcells.fictions.domain.model.VaultFile
import com.editingcells.fictions.domain.model.VaultFileType
import com.editingcells.fictions.domain.model.VaultFolder
import com.example.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

data class DeletedFileBackup(
    val entity: VaultFileEntity,
    val backupFile: File?
)

@Singleton
class VaultRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultDao: VaultDao,
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val CHANNEL_ID = "fictions_vault_operations"
        private val LEGACY_DEMO_FILENAMES = setOf(
            "The_Library_of_Babel_Compendium.pdf",
            "Garden_of_Forking_Paths_Dossier.pdf",
            "Catalog_of_Infinite_Volumes.csv",
            "Reading_Archive_Metrics_2026.csv",
            "On_Exactitude_in_Science.txt",
            "Architecture_of_Liquid_Light.txt"
        )
    }

    private val vaultDir: File
        get() = File(context.filesDir, "vault").apply { if (!exists()) mkdirs() }

    private val trashBackupDir: File
        get() = File(context.cacheDir, "trash_backup").apply { if (!exists()) mkdirs() }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Vault File Operations",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when files are imported, renamed, saved, or exported"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun sendVaultNotification(title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        runCatching {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context)
                .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        }
    }

    val filesFlow: Flow<List<VaultFile>> = combine(
        vaultDao.observeAllFiles(),
        vaultDao.observeAllFolders()
    ) { files, folders ->
        val folderMap = folders.associateBy { it.id }
        files.map { it.toDomain(folderMap) }
    }

    val foldersFlow: Flow<List<VaultFolder>> = combine(
        vaultDao.observeAllFolders(),
        vaultDao.observeAllFiles()
    ) { folders, files ->
        val grouped = files.groupBy { it.folderId }
        folders.map { folder ->
            val folderFiles = grouped[folder.id].orEmpty()
            folder.toDomain(
                fileCount = folderFiles.size,
                totalSizeBytes = folderFiles.sumOf { it.sizeBytes }
            )
        }
    }

    val storageSummaryFlow: Flow<StorageSummary> = vaultDao.observeAllFiles().combine(
        vaultDao.observeAllFolders()
    ) { files, _ ->
        val pdfs = files.filter { it.type == VaultFileType.PDF.name }
        val csvs = files.filter { it.type == VaultFileType.CSV.name }
        val txts = files.filter { it.type == VaultFileType.TXT.name }
        StorageSummary(
            totalBytes = files.sumOf { it.sizeBytes },
            pdfCount = pdfs.size,
            csvCount = csvs.size,
            txtCount = txts.size,
            pdfBytes = pdfs.sumOf { it.sizeBytes },
            csvBytes = csvs.sumOf { it.sizeBytes },
            txtBytes = txts.sumOf { it.sizeBytes }
        )
    }

    /**
     * Removes any legacy demo/sample files from earlier builds so the vault contains
     * strictly real user-imported and user-created files.
     */
    suspend fun purgeAnyLegacyDemoData() = withContext(Dispatchers.IO) {
        if (preferencesManager.isDemoDataCleaned()) return@withContext
        val existingFiles = vaultDir.listFiles().orEmpty()
        val hasDemoFile = existingFiles.any { it.name in LEGACY_DEMO_FILENAMES }
        if (hasDemoFile) {
            vaultDao.deleteAllFiles()
            vaultDao.deleteAllFolders()
            existingFiles.forEach { it.delete() }
        }
        preferencesManager.markDemoDataCleaned()
    }

    suspend fun clearAllVaultData() = withContext(Dispatchers.IO) {
        vaultDao.deleteAllFiles()
        vaultDao.deleteAllFolders()
        vaultDir.listFiles()?.forEach { it.delete() }
        trashBackupDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * Inspects picked URIs (from system file picker or gallery) so the user can review
     * and rename every file before saving it into the vault.
     */
    suspend fun inspectPickedUris(
        uris: List<Uri>,
        forcedType: VaultFileType? = null
    ): List<PendingImportItem> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        uris.mapNotNull { uri ->
            runCatching {
                runCatching {
                    resolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val mimeType = resolver.getType(uri)
                var displayName = "Document_${UUID.randomUUID().toString().take(6)}"
                var sizeBytes = 0L

                resolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIdx >= 0) {
                            val resolved = cursor.getString(nameIdx)
                            if (!resolved.isNullOrBlank()) {
                                displayName = resolved
                            }
                        }
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                            sizeBytes = cursor.getLong(sizeIdx)
                        }
                    }
                }

                val detectedType = forcedType ?: VaultFileType.fromFileNameOrMime(displayName, mimeType)
                val baseNameWithoutExt = displayName.substringBeforeLast(".", displayName)

                PendingImportItem(
                    uri = uri,
                    originalName = displayName,
                    customName = baseNameWithoutExt,
                    detectedType = detectedType,
                    sizeBytes = sizeBytes
                )
            }.getOrNull()
        }
    }

    /**
     * Copies the user-confirmed and renamed [PendingImportItem]s into app-private storage
     * and saves their metadata in Room.
     */
    suspend fun importPendingItemsWithRename(
        items: List<PendingImportItem>,
        targetFolderId: Long? = null,
        tags: List<String> = emptyList()
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            var importedCount = 0
            val now = System.currentTimeMillis()
            val resolver = context.contentResolver
            val serializedCustomTags = tags.map { it.trim() }.filter { it.isNotEmpty() }

            for (item in items) {
                val fileType = item.detectedType
                val originalExt = item.originalName.substringAfterLast(".", "")
                    .lowercase()
                    .takeIf { it.isNotBlank() && it.length <= 8 }
                    ?: fileType.extension

                val cleanCustom = item.customName.trim().ifEmpty {
                    item.originalName.substringBeforeLast(".", "Imported_File")
                }

                val finalDisplayName = if (cleanCustom.lowercase().endsWith(".$originalExt") ||
                    cleanCustom.lowercase().endsWith(".${fileType.extension}")
                ) {
                    cleanCustom
                } else {
                    "$cleanCustom.$originalExt"
                }

                val safeDiskName = "${now + importedCount}_${
                    finalDisplayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                }"
                val destFile = File(vaultDir, safeDiskName)

                resolver.openInputStream(item.uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output, bufferSize = 16 * 1024)
                    }
                } ?: continue

                val combinedTags = (serializedCustomTags + listOf(fileType.extension))
                    .distinct()
                    .joinToString(",")

                val entity = VaultFileEntity(
                    name = finalDisplayName,
                    type = fileType.name,
                    sizeBytes = destFile.length(),
                    dateAdded = now + importedCount,
                    lastOpened = null,
                    folderId = targetFolderId,
                    tagsSerialized = combinedTags,
                    isFavorite = false,
                    filePath = destFile.absolutePath
                )
                vaultDao.insertFile(entity)
                importedCount++
            }

            if (importedCount > 0) {
                sendVaultNotification(
                    title = "Imported $importedCount file${if (importedCount == 1) "" else "s"}",
                    message = if (items.size == 1) {
                        "Saved as \"${items.first().customName}\" in Fictions Vault."
                    } else {
                        "Saved $importedCount files into your Fictions Vault."
                    }
                )
            }
            importedCount
        }
    }

    /**
     * Scans and resolves all files from a picked SAF directory tree.
     */
    suspend fun inspectDirectoryTree(treeUri: Uri): List<PendingImportItem> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        runCatching {
            resolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val items = mutableListOf<PendingImportItem>()

        runCatching {
            resolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext() && items.size < 50) {
                    val childId = if (idIdx >= 0) cursor.getString(idIdx) else continue
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "File" else "File"
                    val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) else null
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) continue
                    val size = if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 0L
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                    val detectedType = VaultFileType.fromFileNameOrMime(name, mime)
                    items.add(
                        PendingImportItem(
                            uri = docUri,
                            originalName = name,
                            customName = name.substringBeforeLast(".", name),
                            detectedType = detectedType,
                            sizeBytes = size
                        )
                    )
                }
            }
        }
        items
    }

    /**
     * Scans top-level Downloads & Documents directories when Full Storage Access (MANAGE_EXTERNAL_STORAGE)
     * or legacy storage permission is granted, returning files ready to import & rename.
     */
    suspend fun scanDeviceDocumentsForImport(): List<PendingImportItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<PendingImportItem>()
        val dirs = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        )
        for (dir in dirs) {
            if (!dir.exists() || !dir.canRead()) continue
            dir.listFiles()
                ?.filter { it.isFile && it.canRead() && it.length() > 0L }
                ?.sortedByDescending { it.lastModified() }
                ?.take(30)
                ?.forEach { file ->
                    val type = VaultFileType.fromFileNameOrMime(file.name, null)
                    results.add(
                        PendingImportItem(
                            uri = Uri.fromFile(file),
                            originalName = file.name,
                            customName = file.name.substringBeforeLast(".", file.name),
                            detectedType = type,
                            sizeBytes = file.length()
                        )
                    )
                }
        }
        results
    }

    suspend fun saveTxtNote(
        existingFileId: Long?,
        title: String,
        body: String,
        folderId: Long?,
        tags: List<String>
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanTitle = title.trim().ifEmpty { "Untitled Note" }
            val fileName = if (cleanTitle.lowercase().endsWith(".txt")) cleanTitle else "$cleanTitle.txt"
            val serializedTags = tags.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(",")
            val now = System.currentTimeMillis()

            if (existingFileId != null && existingFileId > 0L) {
                val existing = vaultDao.getFileById(existingFileId)
                if (existing != null) {
                    val targetFile = File(existing.filePath)
                    targetFile.parentFile?.mkdirs()
                    targetFile.writeText(body)
                    val updated = existing.copy(
                        name = fileName,
                        sizeBytes = targetFile.length(),
                        lastOpened = now,
                        folderId = folderId,
                        tagsSerialized = serializedTags
                    )
                    vaultDao.updateFile(updated)
                    sendVaultNotification("Note Updated", "Saved changes to $fileName")
                    return@runCatching existing.id
                }
            }

            val safeName = "${now}_${fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")}"
            val targetFile = File(vaultDir, safeName)
            targetFile.writeText(body)

            val newEntity = VaultFileEntity(
                name = fileName,
                type = VaultFileType.TXT.name,
                sizeBytes = targetFile.length(),
                dateAdded = now,
                lastOpened = now,
                folderId = folderId,
                tagsSerialized = serializedTags.ifEmpty { "note,txt" },
                isFavorite = false,
                filePath = targetFile.absolutePath
            )
            val id = vaultDao.insertFile(newEntity)
            sendVaultNotification("Note Saved", "Created $fileName in Fictions Vault")
            id
        }
    }

    suspend fun readTextFileContent(filePath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(filePath)
            if (!file.exists()) error("File not found on disk")
            val maxBytes = 2 * 1024 * 1024
            if (file.length() > maxBytes) {
                file.inputStream().bufferedReader().use { reader ->
                    val buffer = CharArray(256 * 1024)
                    val read = reader.read(buffer)
                    String(buffer, 0, read.coerceAtLeast(0)) + "\n\n[... Preview truncated for large file ...]"
                }
            } else {
                file.readText()
            }
        }
    }

    suspend fun parseCsvFile(filePath: String): Result<CsvTableData> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(filePath)
            if (!file.exists()) error("CSV file not found on disk")
            val lines = file.bufferedReader().useLines { seq ->
                seq.filter { it.isNotBlank() }.take(2000).toList()
            }
            if (lines.isEmpty()) {
                return@runCatching CsvTableData(headers = listOf("Empty CSV"), rows = emptyList())
            }
            val parsedRows = lines.map { parseCsvLine(it) }
            val maxCols = parsedRows.maxOfOrNull { it.size } ?: 1
            val normalizedRows = parsedRows.map { row ->
                if (row.size < maxCols) row + List(maxCols - row.size) { "" } else row
            }
            CsvTableData(
                headers = normalizedRows.first(),
                rows = normalizedRows.drop(1)
            )
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    suspend fun getFileById(fileId: Long): VaultFile? = withContext(Dispatchers.IO) {
        val entity = vaultDao.getFileById(fileId) ?: return@withContext null
        val folder = entity.folderId?.let { vaultDao.getFolderById(it) }
        val folderMap = if (folder != null) mapOf(folder.id to folder) else emptyMap()
        entity.toDomain(folderMap)
    }

    suspend fun markFileOpened(fileId: Long) = withContext(Dispatchers.IO) {
        vaultDao.updateLastOpened(fileId, System.currentTimeMillis())
    }

    suspend fun toggleFavorite(fileId: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        vaultDao.setFavorite(fileId, isFavorite)
    }

    suspend fun renameFile(fileId: Long, newName: String) = withContext(Dispatchers.IO) {
        val existing = vaultDao.getFileById(fileId) ?: return@withContext
        val ext = existing.name.substringAfterLast(".", VaultFileType.valueOf(existing.type).extension)
        val cleanName = newName.trim().ifEmpty { existing.name }
        val finalName = if (cleanName.lowercase().endsWith(".$ext")) cleanName else "$cleanName.$ext"
        vaultDao.renameFile(fileId, finalName)
        sendVaultNotification("File Renamed", "Renamed to $finalName")
    }

    suspend fun updateFileTags(fileId: Long, tags: List<String>) = withContext(Dispatchers.IO) {
        val serialized = tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(",")
        vaultDao.updateTags(fileId, serialized)
    }

    suspend fun moveFilesToFolder(fileIds: List<Long>, folderId: Long?) = withContext(Dispatchers.IO) {
        if (fileIds.isEmpty()) return@withContext
        vaultDao.moveFilesToFolder(fileIds, folderId)
    }

    suspend fun deleteFilesWithUndoBackup(fileIds: List<Long>): List<DeletedFileBackup> =
        withContext(Dispatchers.IO) {
            val backups = mutableListOf<DeletedFileBackup>()
            for (id in fileIds) {
                val entity = vaultDao.getFileById(id) ?: continue
                val sourceFile = File(entity.filePath)
                var backupFile: File? = null
                if (sourceFile.exists()) {
                    val candidate = File(trashBackupDir, "${entity.id}_${sourceFile.name}")
                    runCatching {
                        sourceFile.copyTo(candidate, overwrite = true)
                        sourceFile.delete()
                        backupFile = candidate
                    }
                }
                vaultDao.deleteFile(entity)
                backups.add(DeletedFileBackup(entity = entity, backupFile = backupFile))
            }
            backups
        }

    suspend fun restoreDeletedFiles(backups: List<DeletedFileBackup>) = withContext(Dispatchers.IO) {
        for (item in backups) {
            val destFile = File(item.entity.filePath)
            destFile.parentFile?.mkdirs()
            item.backupFile?.let { backup ->
                if (backup.exists()) {
                    runCatching {
                        backup.copyTo(destFile, overwrite = true)
                        backup.delete()
                    }
                }
            }
            vaultDao.insertFile(item.entity)
        }
    }

    suspend fun exportFileToUri(vaultFile: VaultFile, targetUri: Uri, exportedName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val source = File(vaultFile.filePath)
                if (!source.exists()) error("Source file does not exist")
                context.contentResolver.openOutputStream(targetUri)?.use { output ->
                    source.inputStream().use { input ->
                        input.copyTo(output, bufferSize = 16 * 1024)
                    }
                } ?: error("Unable to open output stream")
                sendVaultNotification("File Exported", "Saved \"$exportedName\" to device storage")
                Unit
            }
        }

    fun createShareIntent(files: List<VaultFile>): Intent? {
        val validFiles = files.filter { File(it.filePath).exists() }
        if (validFiles.isEmpty()) return null

        val authority = "${context.packageName}.fileprovider"
        if (validFiles.size == 1) {
            val vf = validFiles.first()
            val uri = FileProvider.getUriForFile(context, authority, File(vf.filePath))
            val mime = vf.type.mimeTypes.firstOrNull() ?: "*/*"
            return Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, vf.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            val uris = ArrayList<Uri>(
                validFiles.map { vf ->
                    FileProvider.getUriForFile(context, authority, File(vf.filePath))
                }
            )
            return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    suspend fun createFolder(name: String, colorHex: Long): Long = withContext(Dispatchers.IO) {
        val cleanName = name.trim().ifEmpty { "New Collection" }
        vaultDao.insertFolder(
            FolderEntity(
                name = cleanName,
                colorHex = colorHex,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateFolder(folderId: Long, name: String, colorHex: Long) = withContext(Dispatchers.IO) {
        val existing = vaultDao.getFolderById(folderId) ?: return@withContext
        vaultDao.updateFolder(
            existing.copy(
                name = name.trim().ifEmpty { existing.name },
                colorHex = colorHex
            )
        )
    }

    suspend fun deleteFolder(folderId: Long) = withContext(Dispatchers.IO) {
        vaultDao.deleteFolderById(folderId)
    }
}
