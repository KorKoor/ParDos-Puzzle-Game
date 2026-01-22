package com.example.pardos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.pardos.domain.model.Record // Importa la clase nueva

@Database(entities = [Record::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
}