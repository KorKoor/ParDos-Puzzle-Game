package com.example.pardos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "records")
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val boardSize: Int,
    val score: Int,
    val timeInSeconds: Long,
    val date: Long = System.currentTimeMillis()
)