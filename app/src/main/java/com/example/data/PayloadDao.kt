package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.PayloadTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface PayloadDao {
    @Query("SELECT * FROM payload_templates ORDER BY isCustom DESC, name ASC")
    fun getAllPayloads(): Flow<List<PayloadTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayload(payload: PayloadTemplate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payloads: List<PayloadTemplate>)

    @Update
    suspend fun updatePayload(payload: PayloadTemplate)

    @Delete
    suspend fun deletePayload(payload: PayloadTemplate)

    @Query("SELECT COUNT(*) FROM payload_templates")
    suspend fun getCount(): Int
}
