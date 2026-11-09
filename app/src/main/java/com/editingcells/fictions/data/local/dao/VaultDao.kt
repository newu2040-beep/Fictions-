package com.editingcells.fictions.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.editingcells.fictions.data.local.entity.FolderEntity
import com.editingcells.fictions.data.local.entity.VaultFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    @Query("SELECT * FROM vault_files ORDER BY dateAdded DESC")
    fun observeAllFiles(): Flow<List<VaultFileEntity>>

    @Query("SELECT * FROM vault_folders ORDER BY createdAt DESC")
    fun observeAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM vault_files WHERE id = :fileId LIMIT 1")
    suspend fun getFileById(fileId: Long): VaultFileEntity?

    @Query("SELECT * FROM vault_folders WHERE id = :folderId LIMIT 1")
    suspend fun getFolderById(folderId: Long): FolderEntity?

    @Query("SELECT COUNT(*) FROM vault_files")
    suspend fun getFileCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: VaultFileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<VaultFileEntity>): List<Long>

    @Update
    suspend fun updateFile(file: VaultFileEntity)

    @Delete
    suspend fun deleteFile(file: VaultFileEntity)

    @Query("DELETE FROM vault_files WHERE id IN (:fileIds)")
    suspend fun deleteFilesByIds(fileIds: List<Long>)

    @Query("UPDATE vault_files SET folderId = :folderId WHERE id IN (:fileIds)")
    suspend fun moveFilesToFolder(fileIds: List<Long>, folderId: Long?)

    @Query("UPDATE vault_files SET isFavorite = :isFavorite WHERE id = :fileId")
    suspend fun setFavorite(fileId: Long, isFavorite: Boolean)

    @Query("UPDATE vault_files SET lastOpened = :timestamp WHERE id = :fileId")
    suspend fun updateLastOpened(fileId: Long, timestamp: Long)

    @Query("UPDATE vault_files SET name = :newName WHERE id = :fileId")
    suspend fun renameFile(fileId: Long, newName: String)

    @Query("UPDATE vault_files SET tagsSerialized = :tagsSerialized WHERE id = :fileId")
    suspend fun updateTags(fileId: Long, tagsSerialized: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Query("DELETE FROM vault_folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: Long)

    @Query("DELETE FROM vault_files")
    suspend fun deleteAllFiles()

    @Query("DELETE FROM vault_folders")
    suspend fun deleteAllFolders()
}
