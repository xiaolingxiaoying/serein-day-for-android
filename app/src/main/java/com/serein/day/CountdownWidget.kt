package com.serein.day

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
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
        applyAppearance(context, views)
        applySizeProfile(context, views, appWidgetId)
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
            views.setTextViewText(R.id.widget_title, "还没有倒数日 · 添加一个重要时刻吧")
            views.setTextViewText(R.id.widget_days, "—")
            views.setTextViewText(R.id.widget_target, "Serein Day")
        } else {
            val remaining = remainingDays(selected, today)
            views.setTextViewText(R.id.widget_title, "${selected.title} ${statusWord(remaining)}")
            views.setTextViewText(R.id.widget_days, abs(remaining).toString())
            val target = targetDate(selected, today)
            views.setTextViewText(R.id.widget_target, "${target.format(FmtIso)} ${weekdayFull(target)}")
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

    private fun applyAppearance(context: Context, views: RemoteViews) {
        val settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val mode = settings.getInt("mode", 2).coerceIn(0, 2)
        val dark = when (mode) {
            0 -> false
            1 -> true
            else -> (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        val palette = paletteFor(
            settings.getInt("palette", 0).coerceIn(0, CUSTOM_PALETTE_INDEX),
            dark,
            settings.getInt("customPrimary", AcidLime.toArgb())
        )
        val accent = palette.accent.toArgb()
        views.setTextColor(R.id.widget_days, accent)
        views.setTextColor(R.id.widget_target, accent)
    }

    /**
     * Xiaomi launchers can give a 2x2 widget a taller-than-wide rectangle because
     * their grid cells are not square. Keep the outer widget controlled by the
     * host, but make the information hierarchy more compact in that profile.
     */
    private fun applySizeProfile(context: Context, views: RemoteViews, appWidgetId: Int) {
        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val squareApplied = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && width > 0 && height > 0) {
            val side = minOf(width, height).toFloat()
            views.setViewLayoutWidth(R.id.widget_card, side, android.util.TypedValue.COMPLEX_UNIT_DIP)
            views.setViewLayoutHeight(R.id.widget_card, side, android.util.TypedValue.COMPLEX_UNIT_DIP)
            true
        } else {
            false
        }
        val tall = !squareApplied && width > 0 && height > width * TALL_WIDGET_RATIO

        val horizontalPadding = dpToPx(context, 14f)
        val verticalPadding = dpToPx(context, if (tall) 10f else 14f)
        views.setViewPadding(
            R.id.widget_content,
            horizontalPadding,
            verticalPadding,
            horizontalPadding,
            verticalPadding
        )
        views.setTextViewTextSize(R.id.widget_title, android.util.TypedValue.COMPLEX_UNIT_SP, if (tall) 16f else 18f)
        views.setTextViewTextSize(R.id.widget_days, android.util.TypedValue.COMPLEX_UNIT_SP, if (tall) 40f else 46f)
        views.setTextViewTextSize(R.id.widget_target, android.util.TypedValue.COMPLEX_UNIT_SP, if (tall) 12f else 13f)
    }

    private fun dpToPx(context: Context, value: Float): Int =
        (value * context.resources.displayMetrics.density).roundToInt()

    private const val TALL_WIDGET_RATIO = 1.15f
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
