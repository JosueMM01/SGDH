package com.example.sgdh.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface BitacoraDao {
    // CREAR
    @Insert
    suspend fun insertar(bitacora: BitacoraEntity)

    // LEER (Lista completa)
    @Query("SELECT * FROM bitacoras ORDER BY id DESC")
    suspend fun obtenerTodas(): List<BitacoraEntity>

    // LEER (Uno solo para el detalle)
    @Query("SELECT * FROM bitacoras WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Int): BitacoraEntity?

    // ACTUALIZAR (Editar)
    @Update
    suspend fun actualizar(bitacora: BitacoraEntity)

    // BORRAR (Eliminar)
    @Delete
    suspend fun eliminar(bitacora: BitacoraEntity)
}