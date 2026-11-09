package com.editingcells.fictions.di

import android.content.Context
import androidx.room.Room
import com.editingcells.fictions.data.local.FictionsDatabase
import com.editingcells.fictions.data.local.dao.VaultDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFictionsDatabase(
        @ApplicationContext context: Context
    ): FictionsDatabase {
        return Room.databaseBuilder(
            context,
            FictionsDatabase::class.java,
            "fictions_vault.db"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideVaultDao(database: FictionsDatabase): VaultDao {
        return database.vaultDao()
    }
}
