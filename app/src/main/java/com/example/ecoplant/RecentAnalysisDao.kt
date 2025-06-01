package com.example.ecoplant

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface RecentAnalysisDao {
    @Query("SELECT * FROM recent_analyses ORDER BY timestamp DESC")
    fun getAllLive(): LiveData<List<RecentAnalysis>>

    @Query("SELECT * FROM recent_analyses ORDER BY timestamp DESC")
    suspend fun getAll(): List<RecentAnalysis>

    @Insert
    suspend fun insert(analysis: RecentAnalysis): Long

    @Update
    suspend fun update(analysis: RecentAnalysis)

    @Query("DELETE FROM recent_analyses WHERE id = :analysisId")
    suspend fun deleteAnalysisById(analysisId: Long)

    @Query("SELECT COUNT(DISTINCT scientific_name) FROM recent_analyses")
    suspend fun getDistinctSpeciesCount(): Int
}