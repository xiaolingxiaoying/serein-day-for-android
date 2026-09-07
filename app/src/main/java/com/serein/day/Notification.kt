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
import java.util.Calendar

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
        val today = LocalDate.now()
        val active = days.filterNot { it.archived }
        val pinned = active.filter { it.priority == 2 }.minByOrNull { remainingDays(it, today) }
            ?: active.minByOrNull { remainingDays(it, today) }
        if (pinned == null) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        ensureChannel(context)
        val remaining = remainingDays(pinned, today)
        val title = pinned.title
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
        CountdownWidget.update(context, days)
        PinnedNotification.scheduleMidnightRefresh(context)
    }
}

/** 每天提醒：按事件各自的时间与提醒类型安排下一次闹钟。 */
object DailyReminderScheduler {
    private const val PREFS = "daily_reminders"
    private const val KEY_IDS = "ids"
    const val ACTION = "com.serein.day.DAILY_REMINDER"

    fun sync(context: Context, days: List<Countdown>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val oldIds = prefs.getStringSet(KEY_IDS, emptySet()).orEmpty()
        oldIds.forEach { cancel(context, alarm, it) }
        val active = days.filter { it.dailyRemind && !it.archived }
        active.forEach { schedule(context, alarm, it) }
        prefs.edit().putStringSet(KEY_IDS, active.map { it.id }.toSet()).apply()
    }

    private fun requestCode(id: String): Int = id.hashCode() and 0x7fffffff

    private fun pendingIntent(context: Context, id: String, kind: ReminderKind): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode(id),
            Intent(ACTION).setPackage(context.packageName).putExtra("dayId", id).putExtra("kind", kind.key),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun cancel(context: Context, alarm: AlarmManager, id: String) {
        val intent = Intent(ACTION).setPackage(context.packageName)
        val pending = PendingIntent.getBroadcast(context, requestCode(id), intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pending != null) alarm.cancel(pending)
    }

    private fun schedule(context: Context, alarm: AlarmManager, day: Countdown) {
        val parts = day.dailyRemindTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        val pending = pendingIntent(context, day.id, day.dailyRemindKind)
        runCatching { alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending) }
            .onFailure { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending) }
    }

    fun reschedule(context: Context, day: Countdown) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (day.dailyRemind && !day.archived) schedule(context, alarm, day) else cancel(context, alarm, day.id)
    }
}

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DailyReminderScheduler.ACTION) return
        val id = intent.getStringExtra("dayId") ?: return
        val day = DayRepository(context).load().firstOrNull { it.id == id } ?: return
        if (!day.dailyRemind || day.archived) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!PinnedNotification.hasPermission(context)) return
        val kind = ReminderKind.fromKey(intent.getStringExtra("kind"))
        val channelId = if (kind == ReminderKind.ALARM) "daily_alarm" else "daily_message"
        val importance = if (kind == ReminderKind.ALARM) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
        manager.createNotificationChannel(NotificationChannel(channelId, kind.label, importance))
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_serein)
            .setContentTitle(day.title)
            .setContentText("${dateText(day)} · 每日提醒")
            .setAutoCancel(true)
            .setCategory(if (kind == ReminderKind.ALARM) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .build()
        manager.notify(id.hashCode(), notification)
        DailyReminderScheduler.reschedule(context, day)
    }
}

/** 设备重启或重新授予精确闹钟权限后，按本地数据恢复提醒与常驻通知。 */
class ReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) return

        val days = DayRepository(context).load()
        DailyReminderScheduler.sync(context, days)
        if (context.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("pinnedNotif", false)) {
            PinnedNotification.update(context, days)
            PinnedNotification.scheduleMidnightRefresh(context)
        }
        CountdownWidget.update(context, days)
    }
}
