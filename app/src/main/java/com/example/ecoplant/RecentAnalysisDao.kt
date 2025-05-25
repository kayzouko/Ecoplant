package com.example.ecoplant

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RecentAnalysisDao {
    @Query("SELECT * FROM recent_analyses ORDER BY timestamp DESC")
    suspend fun getAll(): List<RecentAnalysis>

    @Insert
    suspend fun insert(analysis: RecentAnalysis): Long
}