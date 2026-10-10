package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.VpnConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface VpnConfigDao {
    @Query("SELECT * FROM vpn_configs ORDER BY isFavorite DESC, createdAt DESC")
    fun getAllConfigs(): Flow<List<VpnConfig>>

    @Query("SELECT * FROM vpn_configs")
    suspend fun getAllConfigsSync(): List<VpnConfig>

    @Query("SELECT * FROM vpn_configs WHERE id = :id LIMIT 1")
    suspend fun getConfigById(id: String): VpnConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: VpnConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(configs: List<VpnConfig>)

    @Update
    suspend fun updateConfig(config: VpnConfig)

    @Delete
    suspend fun deleteConfig(config: VpnConfig)

    @Query("DELETE FROM vpn_configs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM vpn_configs")
    suspend fun getCount(): Int
}
