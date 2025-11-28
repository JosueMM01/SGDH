package com.example.sgdh.ui.solicitudes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class SolicitudesFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Retornamos un texto simple por ahora para probar la navegación
        val text = TextView(context)
        text.text = "Pantalla de Solicitudes (Próximamente)"
        text.gravity = android.view.Gravity.CENTER
        return text
    }
}