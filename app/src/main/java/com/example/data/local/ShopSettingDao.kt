package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ShopSetting
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopSettingDao {
    @Query("SELECT value FROM shop_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Query("SELECT value FROM shop_settings WHERE `key` = :key LIMIT 1")
    fun getSettingFlow(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: ShopSetting)

    @Query("DELETE FROM shop_settings")
    suspend fun clearAll()
}
