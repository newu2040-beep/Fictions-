package com.editingcells.fictions.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.editingcells.fictions.data.local.dao.VaultDao
import com.editingcells.fictions.data.local.entity.FolderEntity
import com.editingcells.fictions.data.local.entity.VaultFileEntity

@Database(
    entities = [VaultFileEntity::class, FolderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FictionsDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
}
