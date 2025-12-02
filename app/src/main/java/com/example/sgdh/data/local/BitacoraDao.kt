package com.example.sgdh.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface BitacoraDao {
    @Insert
    suspend fun insertar(bitacora: BitacoraEntity)

    // CAMBIO: Ahora pedimos solo las del usuario actual
    @Query("SELECT * FROM bitacoras WHERE usuarioId = :userId ORDER BY id DESC")
    suspend fun obtenerPorUsuario(userId: Int): List<BitacoraEntity>

    @Query("SELECT * FROM bitacoras WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Int): BitacoraEntity?

    @Update
    suspend fun actualizar(bitacora: BitacoraEntity)

    @Delete
    suspend fun eliminar(bitacora: BitacoraEntity)
}