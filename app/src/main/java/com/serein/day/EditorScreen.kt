package com.serein.day

import android.app.DatePickerDialog
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LowPriority
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlin.math.abs

private val PrioritySteps = listOf(0f, 40f, 100f)
private const val TitleLimit = 24

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    initial: Countdown?,
    books: List<Book>,
    coverStore: CoverStore,
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
    var remind by remember(initial) { mutableStateOf(initial?.remind ?: false) }
    var priority by remember(initial) { mutableIntStateOf(initial?.priority ?: 0) }
    var rawSlider by remember(initial) { mutableFloatStateOf(PrioritySteps[initial?.priority ?: 0]) }

    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val id = initial?.id ?: "draft"
            coverStore.copyIn(uri, id)?.let { cover = it }
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LivePreviewCard(title, date, lunar, repeatYearly, bookId, books, cover, coverStore)
            Column {
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
                        Icon(
                            Icons.Filled.Cancel,
                            contentDescription = "清空",
                            tint = s.outlineVariant,
                            modifier = Modifier.size(20.dp).clickable { title = "" }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
                    Text("必填，用于卡片展示与定时提醒", color = s.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("${title.length}/$TitleLimit", color = s.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (s.isDark) s.container else Color.White)
                        .clickable { pickDate(context, date) { date = it } }
                        .padding(16.dp),
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
                // 公历 / 农历 + 每年重复
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 8.dp),
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
                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (repeatYearly) "每年按此日期重复倒数" else "自动推算倒计时剩余天数", color = s.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.History, contentDescription = null, tint = s.primary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    val r = remainingDays(tempCountdown(date, lunar, repeatYearly))
                    Text(
                        if (repeatYearly) "下次还有 $r 天" else if (r >= 0) "距今尚有 $r 天" else "已过去 ${abs(r)} 天",
                        color = s.primary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(s.high)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconCircle(Color.White, Icons.Outlined.Image, s.primary, size = 44.dp, iconSize = 20.dp)
                Column(Modifier.weight(1f)) {
                    Text("封面图片", color = s.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (cover != null) "已设置封面，卡片与详情都会使用" else "选一张喜欢的图做卡片封面",
                        color = s.onSurfaceVariant,
                        fontSize = 12.5.sp
                    )
                }
                if (cover != null) {
                    Text(
                        "移除",
                        color = s.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                cover?.let { coverStore.remove(it) }
                                cover = null
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Text(
                    if (cover != null) "更换" else "选择",
                    color = s.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(s.accent)
                        .clickable {
                            pickCover.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(s.high)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconCircle(Color.White, Icons.Outlined.NotificationsActive, s.primary, size = 44.dp, iconSize = 20.dp)
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("开启提醒", color = s.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Switch(
                            checked = remind,
                            onCheckedChange = { remind = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = s.primary,
                                checkedThumbColor = s.onPrimary
                            )
                        )
                    }
                    Text("提前 7 天及当天 09:00 推送提醒", color = s.onSurfaceVariant, fontSize = 13.sp)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(s.high)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LowPriority, contentDescription = null, tint = s.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("提醒权重与置顶优先级", color = s.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    PillChip("${PrioritySteps[priority].toInt()}%", s.primary, s.onPrimary)
                }
                Slider(
                    value = rawSlider,
                    onValueChange = { rawSlider = it },
                    onValueChangeFinished = {
                        val snapped = PrioritySteps.minBy { abs(rawSlider - it) }
                        priority = PrioritySteps.indexOf(snapped)
                        rawSlider = snapped
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = s.primary,
                        activeTrackColor = s.primary,
                        inactiveTrackColor = s.highest
                    )
                )
                Row(Modifier.fillMaxWidth()) {
                    PriorityLabel("普通归档", priority == 0, Modifier.weight(1f))
                    PriorityLabel("次要关注", priority == 1, Modifier.weight(1f))
                    PriorityLabel("首页大卡置顶", priority == 2, Modifier.weight(1f))
                }
            }
            Column {
                Text("所属倒数本", color = s.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            }
            Spacer(Modifier.height(6.dp))
            val enabled = title.isNotBlank()
            Row(
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
                                remind = remind,
                                priority = priority,
                                archived = initial?.archived ?: false,
                                createdAt = initial?.createdAt ?: LocalDate.now()
                            )
                        )
                    }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = if (enabled) s.onPrimary else s.onSurfaceVariant, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(6.dp))
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

@Composable
private fun PriorityLabel(text: String, active: Boolean, modifier: Modifier = Modifier) {
    val s = LocalSerein.current
    Text(
        text,
        color = if (active) s.primary else s.onSurfaceVariant,
        fontSize = 12.sp,
        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = modifier
    )
}

/** 编辑页顶部的实时卡片预览，样式与首页 hero 卡一致。 */
@Composable
private fun LivePreviewCard(
    title: String,
    date: LocalDate,
    lunar: Boolean,
    repeatYearly: Boolean,
    bookId: String,
    books: List<Book>,
    cover: String?,
    coverStore: CoverStore
) {
    val s = LocalSerein.current
    val remaining = remainingDays(tempCountdown(date, lunar, repeatYearly))
    val coverFile = cover?.let { coverStore.resolve(it) }
    val coverBitmap = if (coverFile != null && coverFile.exists()) {
        remember(cover, coverFile.lastModified()) {
            runCatching {
                val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 }
                android.graphics.BitmapFactory.decodeFile(coverFile.absolutePath, opts)?.asImageBitmap()
            }.getOrNull()
        }
    } else null
    val bookName = books.find { it.id == bookId }?.name ?: "纪念日"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(s.container)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(heroBrush(bookName))
        ) {
            CoverImage(coverBitmap, Modifier.fillMaxSize())
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Icons.Outlined.Visibility, contentDescription = null, tint = s.onAccentStrong, modifier = Modifier.size(13.dp))
                Text("实时卡片预览", color = s.onAccentStrong, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                PillChip(bookName, s.secondaryContainer, s.onSecondaryContainer, icon = bookIcon(bookName), fontSize = 12.0)
                Spacer(Modifier.height(9.dp))
                Text(
                    title.ifBlank { "未命名倒数日" },
                    color = s.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = s.onSurfaceVariant, modifier = Modifier.size(15.dp))
                    Text(
                        if (lunar) "农历 " + LunarCalendar.fromSolar(date).let { "${it.monthName}${it.dayName}" } + (if (repeatYearly) " · 每年" else "") else "目标日：${date.format(FmtCn)}",
                        color = s.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(s.highest)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(if (remaining >= 0) "还有" else "已过去", color = s.onSurfaceVariant, fontSize = 11.5.sp)
                Text(
                    abs(remaining).toString(),
                    color = s.primary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text("天", color = s.onSurfaceVariant, fontSize = 11.5.sp)
            }
        }
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
