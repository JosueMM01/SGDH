package com.example.sgdh.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bitacoras")
data class BitacoraEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titulo: String,
    val descripcion: String,
    val fecha: String,
    val fotoPath: String?,
    val usuarioId: Int
)