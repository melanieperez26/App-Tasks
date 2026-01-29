package com.example.unitrack20.util
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Reminder"
        val message = intent.getStringExtra("message") ?: "You have a task or exam coming up."
        val notificationId = intent.getIntExtra("notificationId", 0)
        NotificationHelper.createNotificationChannel(context)
        NotificationHelper.showNotification(context, title, message, notificationId)
    }
}
