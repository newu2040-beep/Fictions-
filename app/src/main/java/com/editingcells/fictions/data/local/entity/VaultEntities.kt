package com.editingcells.fictions.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.editingcells.fictions.domain.model.VaultFile
import com.editingcells.fictions.domain.model.VaultFileType
import com.editingcells.fictions.domain.model.VaultFolder

@Entity(tableName = "vault_folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val colorHex: Long,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(fileCount: Int = 0, totalSizeBytes: Long = 0L): VaultFolder = VaultFolder(
        id = id,
        name = name,
        colorHex = colorHex,
        createdAt = createdAt,
        fileCount = fileCount,
        totalSizeBytes = totalSizeBytes
    )
}

@Entity(
    tableName = "vault_files",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["folderId"]), Index(value = ["type"]), Index(value = ["isFavorite"])]
)
data class VaultFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val type: String, // PDF, CSV, TXT
    val sizeBytes: Long,
    val dateAdded: Long,
    val lastOpened: Long? = null,
    val folderId: Long? = null,
    val tagsSerialized: String = "", // comma-separated tags
    val isFavorite: Boolean = false,
    val filePath: String
) {
    fun toDomain(folderMap: Map<Long, FolderEntity> = emptyMap()): VaultFile {
        val folder = folderId?.let { folderMap[it] }
        val parsedTags = tagsSerialized
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val resolvedType = runCatching { VaultFileType.valueOf(type) }
            .getOrDefault(VaultFileType.TXT)

        return VaultFile(
            id = id,
            name = name,
            type = resolvedType,
            sizeBytes = sizeBytes,
            dateAdded = dateAdded,
            lastOpened = lastOpened,
            folderId = folderId,
            folderName = folder?.name,
            folderColorHex = folder?.colorHex,
            tags = parsedTags,
            isFavorite = isFavorite,
            filePath = filePath
        )
    }
}
