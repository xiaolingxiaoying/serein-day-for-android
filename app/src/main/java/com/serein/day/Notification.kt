package com.serein.day

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** 常驻通知：显示置顶（或最近的）倒数卡片，数据变化与跨午夜时更新。 */
object PinnedNotification {
    const val CHANNEL_ID = "pinned_countdown"
    const val NOTIFICATION_ID = 42
    const val ACTION_REFRESH = "com.serein.day.REFRESH_PINNED"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "置顶倒数卡",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "在通知栏常驻显示置顶的倒数日"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun hasPermission(context: Context): Boolean =
        androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** 更新或撤下常驻通知；无置顶且无未来事件时撤下。 */
    fun update(context: Context, days: List<Countdown>) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!hasPermission(context)) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        val active = days.filterNot { it.archived }
        val pinned = active.filter { it.priority == 2 }.minByOrNull { remainingDays(it) } ?: active.minByOrNull { remainingDays(it) }
        if (pinned == null) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        ensureChannel(context)
        val remaining = remainingDays(pinned)
        val title = when {
            pinned.priority == 2 -> "📌 ${pinned.title}"
            else -> pinned.title
        }
        val text = "${statusWord(remaining)} ${kotlin.math.abs(remaining)} 天 · ${dateText(pinned)}"
        val intent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_serein)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(intent)
            .setShowWhen(false)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
    }

    /** 在下一个本地零点安排一次刷新（跨天后天数变化）。 */
    fun scheduleMidnightRefresh(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ACTION_REFRESH).setPackage(context.packageName)
        val pending = PendingIntent.getBroadcast(
            context, 1001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val next = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT)
        alarm.set(
            AlarmManager.RTC,
            next.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            pending
        )
    }
}

/** 零点广播：刷新常驻通知并安排下一次。 */
class MidnightReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PinnedNotification.ACTION_REFRESH) return
        val days = DayRepository(context).load()
        if (context.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("pinnedNotif", false)) {
            PinnedNotification.update(context, days)
        }
        PinnedNotification.scheduleMidnightRefresh(context)
    }
}
