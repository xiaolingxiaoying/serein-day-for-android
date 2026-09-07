package com.serein.day

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

/** 桌面倒数卡：展示置顶事件，未置顶时展示最近的活动事件。 */
class CountdownWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        CountdownWidget.update(context, DayRepository(context).load(), manager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        CountdownWidget.update(context, DayRepository(context).load(), manager, intArrayOf(appWidgetId))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        CountdownWidgetSelection.remove(context, appWidgetIds)
    }
}

object CountdownWidget {
    fun update(context: Context, days: List<Countdown>) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, CountdownWidgetProvider::class.java)
        update(context, days, manager, manager.getAppWidgetIds(component))
    }

    fun update(
        context: Context,
        days: List<Countdown>,
        manager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        appWidgetIds.forEach { id -> manager.updateAppWidget(id, views(context, days, id)) }
    }

    private fun views(context: Context, days: List<Countdown>, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_countdown)
        val backgroundName = CountdownWidgetSelection.backgroundName(context, appWidgetId)
        val background = backgroundName?.let { decodeSampledOrientedBitmap(CoverStore(context).resolve(it), maxDimension = 600) }
        if (background != null) {
            views.setImageViewBitmap(R.id.widget_background, background)
            views.setInt(
                R.id.widget_background,
                "setImageAlpha",
                (CountdownWidgetSelection.backgroundOpacity(context, appWidgetId) * 255).roundToInt()
            )
            views.setViewVisibility(R.id.widget_background, View.VISIBLE)
            views.setViewVisibility(R.id.widget_scrim, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_background, View.GONE)
            views.setViewVisibility(R.id.widget_scrim, View.GONE)
        }
        val today = LocalDate.now()
        val active = days.filterNot { it.archived }
        val selectedId = CountdownWidgetSelection.selectedDayId(context, appWidgetId)
        val selected = active.firstOrNull { it.id == selectedId }
            ?: active.filter { it.priority == 2 }.minByOrNull { abs(remainingDays(it, today)) }
            ?: active.filter { remainingDays(it, today) >= 0 }.minByOrNull { remainingDays(it, today) }
            ?: active.minByOrNull { abs(remainingDays(it, today)) }

        if (selected == null) {
            views.setTextViewText(R.id.widget_title, "还没有倒数日")
            views.setTextViewText(R.id.widget_status, "添加一个重要时刻吧")
            views.setTextViewText(R.id.widget_days, "—")
            views.setTextViewText(R.id.widget_target, "Serein Day")
        } else {
            val remaining = remainingDays(selected, today)
            views.setTextViewText(R.id.widget_title, selected.title)
            views.setTextViewText(R.id.widget_status, statusWord(remaining))
            views.setTextViewText(R.id.widget_days, abs(remaining).toString())
            views.setTextViewText(R.id.widget_target, targetDate(selected).format(FmtDot))
        }

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, openApp)
        return views
    }
}

/** 每个桌面组件独立保存其展示的倒数日；未选时使用置顶/最近事件作为兜底。 */
object CountdownWidgetSelection {
    private const val PREFS = "countdown_widget"
    private const val KEY_PREFIX = "day_"
    private const val BACKGROUND_PREFIX = "background_"
    private const val BACKGROUND_OPACITY_PREFIX = "background_opacity_"

    fun selectedDayId(context: Context, appWidgetId: Int): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("$KEY_PREFIX$appWidgetId", null)

    fun save(context: Context, appWidgetId: Int, dayId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("$KEY_PREFIX$appWidgetId", dayId)
            .apply()
    }

    fun backgroundName(context: Context, appWidgetId: Int): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("$BACKGROUND_PREFIX$appWidgetId", null)

    fun saveBackground(context: Context, appWidgetId: Int, name: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .apply {
                if (name == null) remove("$BACKGROUND_PREFIX$appWidgetId") else putString("$BACKGROUND_PREFIX$appWidgetId", name)
            }
            .apply()
    }

    fun backgroundOpacity(context: Context, appWidgetId: Int): Float =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getFloat("$BACKGROUND_OPACITY_PREFIX$appWidgetId", 1f)
            .coerceIn(0.1f, 1f)

    fun saveBackgroundOpacity(context: Context, appWidgetId: Int, opacity: Float) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putFloat("$BACKGROUND_OPACITY_PREFIX$appWidgetId", opacity.coerceIn(0.1f, 1f))
            .apply()
    }

    fun remove(context: Context, appWidgetIds: IntArray) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        appWidgetIds.forEach {
            editor.remove("$KEY_PREFIX$it")
            backgroundName(context, it)?.let { name -> CoverStore(context).remove(name) }
            editor.remove("$BACKGROUND_PREFIX$it")
            editor.remove("$BACKGROUND_OPACITY_PREFIX$it")
        }
        editor.apply()
    }
}
