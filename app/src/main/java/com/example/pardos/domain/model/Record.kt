package com.example.pardos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "records")
data class Record(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val score: Int,
    val level: Int,
    val mode: String,
    val date: Long
)