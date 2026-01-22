package com.korkoor.pardos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.korkoor.pardos.domain.model.Record // Importa la clase nueva

@Database(entities = [Record::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
}