package com.example.pardos.data.local

import androidx.room.*
import com.example.pardos.domain.model.Record // Importamos el modelo de dominio
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    // Usamos OnConflictStrategy.REPLACE por seguridad si hay IDs duplicados
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: Record)

    // Ajustado para filtrar por modo de juego (Carrera, Libre, Desafío)
    @Query("SELECT * FROM records WHERE mode = :gameMode ORDER BY score DESC LIMIT 10")
    fun getTopRecordsByMode(gameMode: String): Flow<List<Record>>

    // Obtener todo el historial ordenado por fecha
    @Query("SELECT * FROM records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<Record>>
}