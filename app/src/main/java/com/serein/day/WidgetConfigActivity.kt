package com.serein.day

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private data class WidgetPendingBackground(val name: String, val temporary: Boolean)

/** 小组件配置页：每个组件可单独选择展示的倒数日。 */
class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setResult(RESULT_CANCELED)

        val settings = getSharedPreferences("settings", MODE_PRIVATE)
        val paletteIndex = settings.getInt("palette", 0).coerceIn(0, CUSTOM_PALETTE_INDEX)
        val customPrimary = settings.getInt("customPrimary", 0xFFC8F531.toInt())
        val mode = settings.getInt("mode", 2)
        val dark = when (mode) {
            0 -> false
            1 -> true
            else -> resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        val days = DayRepository(this).load().filterNot { it.archived }.sortedBy { remainingDays(it) }

        setContent {
            SereinTheme(paletteFor(paletteIndex, dark, customPrimary), dark) {
                var backgroundName by remember { mutableStateOf(CountdownWidgetSelection.backgroundName(this, appWidgetId)) }
                var widgetAccent by remember { mutableStateOf(CountdownWidgetSelection.widgetAccent(this, appWidgetId)) }
                var showCustomAccent by remember { mutableStateOf(false) }
                var showBackgroundSource by remember { mutableStateOf(false) }
                var pendingBackground by remember { mutableStateOf<WidgetPendingBackground?>(null) }
                val coverStore = remember { CoverStore(this@WidgetConfigActivity) }
                WidgetConfigScreen(
                    days = days,
                    hasBackground = backgroundName != null,
                    widgetAccent = widgetAccent,
                    onBack = { finish() },
                    onPickBackground = { showBackgroundSource = true },
                    onRemoveBackground = {
                        backgroundName?.let { CoverStore(this).remove(it) }
                        CountdownWidgetSelection.saveBackground(this, appWidgetId, null)
                        backgroundName = null
                        CountdownWidget.update(this, DayRepository(this).load())
                    },
                    onAccentChange = { color ->
                        CountdownWidgetSelection.saveWidgetAccent(this, appWidgetId, color)
                        widgetAccent = color
                        CountdownWidget.update(this, DayRepository(this).load())
                    },
                    onCustomAccent = { showCustomAccent = true },
                    onSelect = { day ->
                        CountdownWidgetSelection.save(this, appWidgetId, day.id)
                        CountdownWidget.update(this, DayRepository(this).load())
                        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                        finish()
                    }
                )
                if (showBackgroundSource) {
                    ImageSourceDialog(
                        title = "组件背景图片",
                        hasCurrent = backgroundName != null,
                        onDismiss = { showBackgroundSource = false },
                        onImage = { uri ->
                            val name = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                coverStore.copyIn(uri, "widget_$appWidgetId.edit.background")
                            }
                            if (name != null) pendingBackground = WidgetPendingBackground(name, temporary = true)
                            name != null
                        },
                        onEditCurrent = backgroundName?.let { current ->
                            { pendingBackground = WidgetPendingBackground(current, temporary = false) }
                        },
                        onRemove = {
                            backgroundName?.let { coverStore.remove(it) }
                            CountdownWidgetSelection.saveBackground(this@WidgetConfigActivity, appWidgetId, null)
                            backgroundName = null
                            CountdownWidget.update(this@WidgetConfigActivity, DayRepository(this@WidgetConfigActivity).load())
                        }
                    )
                }
                pendingBackground?.let { pending ->
                    ImageEditSheet(
                        title = "调整组件背景",
                        imageName = pending.name,
                        coverStore = coverStore,
                        initialScale = ImageScaleMode.CROP,
                        initialOpacity = CountdownWidgetSelection.backgroundOpacity(this@WidgetConfigActivity, appWidgetId),
                        targetAspectRatio = 1f,
                        targetLabel = "2×2 组件背景",
                        cropOutputBase = "widget_$appWidgetId.edit.crop.background",
                        onSave = { _, opacity, croppedName ->
                            val source = croppedName ?: pending.name
                            val finalName = when {
                                croppedName != null -> coverStore.renameDraft("widget_$appWidgetId.background", source)
                                pending.temporary -> coverStore.renameDraft("widget_$appWidgetId.background", source)
                                else -> source
                            }
                            if (croppedName != null && pending.name != finalName) coverStore.remove(pending.name)
                            if (backgroundName != null && backgroundName != finalName) coverStore.remove(backgroundName!!)
                            CountdownWidgetSelection.saveBackground(this@WidgetConfigActivity, appWidgetId, finalName)
                            CountdownWidgetSelection.saveBackgroundOpacity(this@WidgetConfigActivity, appWidgetId, opacity)
                            backgroundName = finalName
                            pendingBackground = null
                            CountdownWidget.update(this@WidgetConfigActivity, DayRepository(this@WidgetConfigActivity).load())
                        },
                        onCancel = {
                            if (pending.temporary) coverStore.remove(pending.name)
                            coverStore.removeByBase("widget_$appWidgetId.edit.crop.background")
                            pendingBackground = null
                        }
                    )
                }
                if (showCustomAccent) {
                    WidgetCustomColorSheet(
                        initial = widgetAccent,
                        onDismiss = { showCustomAccent = false },
                        onConfirm = { color ->
                            CountdownWidgetSelection.saveWidgetAccent(this, appWidgetId, color)
                            widgetAccent = color
                            CountdownWidget.update(this, DayRepository(this).load())
                            showCustomAccent = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetConfigScreen(
    days: List<Countdown>,
    hasBackground: Boolean,
    widgetAccent: Int,
    onBack: () -> Unit,
    onPickBackground: () -> Unit,
    onRemoveBackground: () -> Unit,
    onAccentChange: (Int) -> Unit,
    onCustomAccent: () -> Unit,
    onSelect: (Countdown) -> Unit
) {
    val s = LocalSerein.current
    Column(Modifier.fillMaxSize().background(s.surface)) {
        TopBar(
            title = "选择倒数日",
            leadingIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onLeading = onBack
        )
        Text(
            "选择桌面组件要显示的倒数日",
            color = s.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        Row(
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(s.container)
                .clickable(onClick = onPickBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("组件背景", color = s.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(if (hasBackground) "已设置图片背景，点击更换" else "从相册选择图片", color = s.onSurfaceVariant, fontSize = 12.sp)
            }
            if (hasBackground) {
                Text("移除", color = s.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onRemoveBackground).padding(8.dp))
            }
        }
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onCustomAccent)
                .background(s.container)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(widgetAccent))
            )
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Text("自定义颜色", color = s.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("独立于软件外观色板", color = s.onSurfaceVariant, fontSize = 12.sp)
            }
            Text("调整", color = s.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "组件主题色",
            color = s.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(s.container)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            (0 until 8).forEach { index ->
                val color = paletteDotColor(index)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .then(if (widgetAccent == color.toArgb()) Modifier.border(2.dp, s.onSurface, CircleShape) else Modifier)
                        .background(color)
                        .clickable { onAccentChange(color.toArgb()) }
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(days, key = { it.id }) { day ->
                val remaining = remainingDays(day)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(s.container)
                        .clickable { onSelect(day) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(day.title, color = s.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(3.dp))
                        Text(dateText(day), color = s.onSurfaceVariant, fontSize = 12.5.sp)
                    }
                    Text(
                        abs(remaining).toString(),
                        color = if (remaining >= 0) s.primary else s.onSurfaceVariant,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        style = TnumStyle
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("天", color = s.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun WidgetCustomColorSheet(
    initial: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val start = FloatArray(3)
    android.graphics.Color.colorToHSV(initial, start)
    var hue by remember { mutableFloatStateOf(start[0]) }
    var saturation by remember { mutableFloatStateOf(start[1].coerceIn(0.3f, 1f)) }
    val current = Color.hsv(hue, saturation, 0.9f)
    val argb = android.graphics.Color.argb(
        255,
        (current.red * 255).toInt(),
        (current.green * 255).toInt(),
        (current.blue * 255).toInt()
    )
    SereinSheet(
        title = "自定义组件主题色",
        onDismiss = onDismiss,
        trailingText = "使用此颜色",
        onTrailing = { onConfirm(argb) }
    ) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(current))
                Column(Modifier.padding(start = 12.dp)) {
                    Text("小组件独立配色", color = LocalSerein.current.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("不会改变软件主题色", color = LocalSerein.current.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Text("色相", color = LocalSerein.current.onSurfaceVariant, fontSize = 12.sp)
            Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
            Text("饱和度", color = LocalSerein.current.onSurfaceVariant, fontSize = 12.sp)
            Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0.3f..1f)
        }
    }
}
