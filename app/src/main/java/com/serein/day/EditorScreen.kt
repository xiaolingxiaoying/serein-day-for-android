package com.serein.day

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlin.math.abs

private const val TitleLimit = 24

/** 编辑页里正在挑选的图片来源。 */
private enum class ImageTarget { COVER, WALLPAPER }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    initial: Countdown?,
    books: List<Book>,
    coverStore: CoverStore,
    minimalMode: Boolean,
    onCancel: () -> Unit,
    onSave: (Countdown) -> Unit,
    onDelete: (() -> Unit)?
) {
    val s = LocalSerein.current
    val context = LocalContext.current
    var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
    var date by remember(initial) { mutableStateOf(initial?.date ?: LocalDate.now()) }
    var lunar by remember(initial) { mutableStateOf(initial?.lunar ?: false) }
    var repeatYearly by remember(initial) { mutableStateOf(initial?.repeatYearly ?: false) }
    var bookId by remember(initial) { mutableStateOf(initial?.bookId ?: (books.firstOrNull()?.id ?: "")) }
    var cover by remember(initial) { mutableStateOf(initial?.cover) }
    var wallpaper by remember(initial) { mutableStateOf(initial?.wallpaper) }
    var remind by remember(initial) { mutableStateOf(initial?.remind ?: false) }
    var pinned by remember(initial) { mutableStateOf(initial?.priority == 2) }
    var picking by remember { mutableStateOf<ImageTarget?>(null) }

    // 新建流程先落为草稿文件，保存时由调用方改成真实事件 id
    val coverBase = initial?.id ?: "draft"
    val wallpaperBase = initial?.id?.let { "$it.w" } ?: "draftw"

    Column(Modifier.fillMaxSize().background(s.surface).imePadding()) {
        TopBar(
            title = "Add Countdown",
            leadingIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onLeading = onCancel
        )
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 名称
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (s.isDark) s.container else Color.White)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Outlined.EditNote, contentDescription = null, tint = s.onSurfaceVariant, modifier = Modifier.size(22.dp))
                Box(Modifier.weight(1f)) {
                    if (title.isEmpty()) {
                        Text("给这个日子起个名字", color = s.outlineVariant, fontSize = 17.sp)
                    }
                    BasicTextField(
                        value = title,
                        onValueChange = { if (it.length <= TitleLimit) title = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = s.onSurface),
                        cursorBrush = SolidColor(s.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (title.isNotEmpty()) {
                    Text("${title.length}/$TitleLimit", color = s.outlineVariant, fontSize = 11.sp)
                    Icon(
                        Icons.Filled.Cancel,
                        contentDescription = "清空",
                        tint = s.outlineVariant,
                        modifier = Modifier.size(20.dp).clickable { title = "" }
                    )
                }
            }
            // 目标日期 + 公历/农历 + 每年重复
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (s.isDark) s.container else Color.White)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { pickDate(context, date) { date = it } },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = s.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text("目标日期", color = s.onSurfaceVariant, fontSize = 12.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (lunar) "农历 " + LunarCalendar.fromSolar(date).let { "${it.monthName}${it.dayName}" } + "（${date.format(FmtDot)}）" else date.format(FmtCn),
                            color = s.onSurface,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    PillChip(weekdayShort(date), s.highest, s.onSurfaceVariant)
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "选择日期", tint = s.onSurfaceVariant)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(Modifier.clip(RoundedCornerShape(50)).background(s.highest).padding(3.dp)) {
                        SegChip("公历", !lunar, s.primary) { lunar = false }
                        SegChip("农历", lunar, s.primary) { lunar = true }
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Outlined.Repeat, contentDescription = null, tint = if (repeatYearly) s.primary else s.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("每年重复", color = s.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = repeatYearly,
                        onCheckedChange = { repeatYearly = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = s.primary, checkedThumbColor = s.onPrimary)
                    )
                }
                val r = remainingDays(tempCountdown(date, lunar, repeatYearly))
                Text(
                    when {
                        repeatYearly && r >= 0 -> "每年重复 · 下次还有 $r 天"
                        r >= 0 -> "距今还有 $r 天"
                        else -> "已过去 ${abs(r)} 天"
                    },
                    color = s.primary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            // 倒数本
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                books.forEach { book ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (bookId == book.id) s.secondaryContainer else if (s.isDark) s.container else Color.White)
                            .clickable { bookId = book.id }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            bookIcon(book.name),
                            contentDescription = null,
                            tint = if (bookId == book.id) s.onSecondaryContainer else s.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            book.name,
                            color = if (bookId == book.id) s.onSecondaryContainer else s.onSurface,
                            fontSize = 14.sp,
                            fontWeight = if (bookId == book.id) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }
            }
            if (!minimalMode) {
                ImageRow(
                    icon = Icons.Outlined.Image,
                    label = "封面图片",
                    hint = if (cover != null) "已设置，卡片与详情都会使用" else "选一张喜欢的图做卡片封面",
                    hasCurrent = cover != null,
                    onPick = { picking = ImageTarget.COVER }
                )
                ImageRow(
                    icon = Icons.Outlined.Wallpaper,
                    label = "详情页背景",
                    hint = if (wallpaper != null) "已设置，详情页会铺满展示" else "给详情页也换一张壁纸",
                    hasCurrent = wallpaper != null,
                    onPick = { picking = ImageTarget.WALLPAPER }
                )
            }
            // 提醒
            if (!minimalMode) {
                ToggleRow(
                    icon = Icons.Outlined.NotificationsActive,
                    title = "开启提醒",
                    subtitle = "提前 7 天及当天 09:00 提醒",
                    checked = remind,
                    onChecked = { remind = it }
                )
            }
            // 置顶
            ToggleRow(
                icon = Icons.Outlined.Flag,
                title = "置顶",
                subtitle = "在首页大卡与列表最前展示",
                checked = pinned,
                onChecked = { pinned = it }
            )
            Spacer(Modifier.height(6.dp))
            val enabled = title.isNotBlank()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(if (enabled) s.primary else s.highest)
                    .clickable(enabled = enabled) {
                        onSave(
                            Countdown(
                                id = initial?.id ?: newCountdownId(),
                                title = title.trim(),
                                date = date,
                                lunar = lunar,
                                repeatYearly = repeatYearly,
                                bookId = bookId,
                                notes = initial?.notes ?: emptyList(),
                                cover = cover,
                                wallpaper = wallpaper,
                                remind = remind,
                                priority = if (pinned) 2 else 0,
                                archived = initial?.archived ?: false,
                                createdAt = initial?.createdAt ?: LocalDate.now()
                            )
                        )
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "保存倒数日",
                    color = if (enabled) s.onPrimary else s.onSurfaceVariant,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (onDelete != null) {
                Text(
                    "删除此倒数日",
                    color = s.error,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onDelete).padding(vertical = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    when (picking) {
        ImageTarget.COVER -> ImageSourceDialog(
            title = "封面图片来源",
            hasCurrent = cover != null,
            onDismiss = { picking = null },
            onImage = { uri -> coverStore.copyIn(uri, coverBase)?.let { cover = it } },
            onRemove = {
                cover?.let { coverStore.remove(it) }
                cover = null
            }
        )
        ImageTarget.WALLPAPER -> ImageSourceDialog(
            title = "详情页背景图片来源",
            hasCurrent = wallpaper != null,
            onDismiss = { picking = null },
            onImage = { uri -> coverStore.copyIn(uri, wallpaperBase)?.let { wallpaper = it } },
            onRemove = {
                wallpaper?.let { coverStore.remove(it) }
                wallpaper = null
            }
        )
        null -> Unit
    }
}

/** 图片设置行：整行可点，弹出三来源选择。 */
@Composable
private fun ImageRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    hint: String,
    hasCurrent: Boolean,
    onPick: () -> Unit
) {
    val s = LocalSerein.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(s.high)
            .clickable(onClick = onPick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconCircle(Color.White, icon, s.primary, size = 44.dp, iconSize = 20.dp)
        Column(Modifier.weight(1f)) {
            Text(label, color = s.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(hint, color = s.onSurfaceVariant, fontSize = 12.5.sp)
        }
        Text(
            if (hasCurrent) "更换" else "选择",
            color = s.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(s.accent)
                .padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun ToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    val s = LocalSerein.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(s.high)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconCircle(Color.White, icon, s.primary, size = 44.dp, iconSize = 20.dp)
        Column(Modifier.weight(1f)) {
            Text(title, color = s.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = s.onSurfaceVariant, fontSize = 12.5.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedTrackColor = s.primary, checkedThumbColor = s.onPrimary)
        )
    }
}

@Composable
private fun SegChip(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val s = LocalSerein.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            color = if (selected) s.onSurface else s.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

private fun tempCountdown(date: LocalDate, lunar: Boolean, repeatYearly: Boolean): Countdown =
    Countdown(id = "preview", title = "", date = date, lunar = lunar, repeatYearly = repeatYearly)

private fun pickDate(context: Context, current: LocalDate, onPicked: (LocalDate) -> Unit) {
    DatePickerDialog(
        context,
        { _, year, month, day -> onPicked(LocalDate.of(year, month + 1, day)) },
        current.year, current.monthValue - 1, current.dayOfMonth
    ).show()
}
