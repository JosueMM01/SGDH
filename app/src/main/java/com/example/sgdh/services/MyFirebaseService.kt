package com.example.sgdh.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.sgdh.R
import com.example.sgdh.ui.main.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Solo mostramos notificación si trae datos visibles
        val title = message.notification?.title ?: "SGDH"
        val body = message.notification?.body ?: "Nueva actualización"
        mostrarNotificacion(title, body)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Guardamos temporalmente para enviarlo en el próximo login si fuera necesario
        getSharedPreferences("sgdh_prefs", Context.MODE_PRIVATE)
            .edit().putString("fcm_token_temp", token).apply()
    }

    private fun mostrarNotificacion(title: String, body: String) {
        val channelId = "sgdh_channel_01"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Notificaciones SGDH", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Asegúrate que este icono exista
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        manager.notify(Random.nextInt(), builder.build())
    }
}