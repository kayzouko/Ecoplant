package com.example.ecoplant

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_analyses")
data class RecentAnalysis(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "scientific_name")
    val scientificName: String,

    @ColumnInfo(name = "common_name")
    val commonName: String?,

    val score: Double,

    @ColumnInfo(name = "image_path")
    val imagePath: String,

    val timestamp: Long = System.currentTimeMillis()
)