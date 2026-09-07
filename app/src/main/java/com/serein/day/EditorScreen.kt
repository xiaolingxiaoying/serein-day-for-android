package com.serein.day

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TitleLimit = 24

/** 编辑页里正在挑选的图片来源。 */
private enum class ImageTarget { COVER, WALLPAPER }
private data class PendingImageEdit(
    val target: ImageTarget,
    val newName: String,
    val oldName: String?,
    val temporary: Boolean
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    initial: Countdown?,
    books: List<Book>,
    coverStore: CoverStore,
    minimalMode: Boolean,
    onCancel: () -> Unit,
    onSave: (Countdown) -> Unit,
    onAddBook: (String) -> String,
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
    var coverScale by remember(initial) { mutableStateOf(initial?.coverScale ?: ImageScaleMode.CROP) }
    var coverOpacity by remember(initial) { mutableStateOf(initial?.coverOpacity ?: 1f) }
    var wallpaper by remember(initial) { mutableStateOf(initial?.wallpaper) }
    var wallpaperScale by remember(initial) { mutableStateOf(initial?.wallpaperScale ?: ImageScaleMode.CROP) }
    var wallpaperOpacity by remember(initial) { mutableStateOf(initial?.wallpaperOpacity ?: 1f) }
    var remind by remember(initial) { mutableStateOf(initial?.remind ?: false) }
    var dailyRemind by remember(initial) { mutableStateOf(initial?.dailyRemind ?: false) }
    var dailyRemindTime by remember(initial) { mutableStateOf(initial?.dailyRemindTime ?: "09:00") }
    var dailyRemindKind by remember(initial) { mutableStateOf(initial?.dailyRemindKind ?: ReminderKind.MESSAGE) }
    var pinned by remember(initial) { mutableStateOf(initial?.priority == 2) }
    var picking by remember { mutableStateOf<ImageTarget?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var addingBook by remember { mutableStateOf(false) }
    var showDailyReminderSheet by remember { mutableStateOf(false) }
    var pendingImageEdit by remember { mutableStateOf<PendingImageEdit?>(null) }

    // 新建流程先落为草稿文件，保存时由调用方改成真实事件 id
    val coverBase = initial?.id ?: "draft"
    val wallpaperBase = initial?.id?.let { "$it.w" } ?: "draftw"
    val cardBg = s.container

    fun save() {
        if (title.isBlank()) return
        if (initial?.cover != null && initial.cover != cover) coverStore.remove(initial.cover)
        if (initial?.wallpaper != null && initial.wallpaper != wallpaper) coverStore.remove(initial.wallpaper)
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
                coverScale = coverScale,
                coverOpacity = coverOpacity,
                wallpaper = wallpaper,
                wallpaperScale = wallpaperScale,
                wallpaperOpacity = wallpaperOpacity,
                wallpaperDim = initial?.wallpaperDim,
                subs = initial?.subs ?: emptyList(),
                remind = remind,
                dailyRemind = dailyRemind,
                dailyRemindTime = dailyRemindTime,
                dailyRemindKind = dailyRemindKind,
                priority = if (pinned) 2 else 0,
                archived = initial?.archived ?: false,
                createdAt = initial?.createdAt ?: LocalDate.now()
            )
        )
    }

    Column(Modifier.fillMaxSize().background(s.surface).imePadding()) {
        TopBar(
            title = if (initial == null) "新建倒数日" else "编辑倒数日",
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
            // 名称 + 日期 + 历法：一组内嵌表单（iOS 分组样式）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
            ) {
                // 名称
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
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
                HairlineInset(s)
                // 目标日期
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }.padding(horizontal = 16.dp, vertical = 14.dp),
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
                }
                HairlineInset(s)
                // 公历/农历 + 每年重复
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SegmentedControl(
                            options = listOf("公历", "农历"),
                            selected = if (lunar) 1 else 0,
                            modifier = Modifier.width(150.dp)
                        ) { lunar = it == 1 }
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.Outlined.Repeat,
                            contentDescription = null,
                            tint = if (repeatYearly) s.primary else s.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("每年重复", color = s.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = repeatYearly,
                            onCheckedChange = { repeatYearly = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = s.accent, checkedThumbColor = s.onAccent)
                        )
                    }
                    // 输入标题等引起的重组不重算；农历换算有缓存，这里也避免每次组合重复求值
                    val r = remember(date, lunar, repeatYearly) {
                        remainingDays(tempCountdown(date, lunar, repeatYearly))
                    }
                    Text(
                        when {
                            repeatYearly && r >= 0 -> "每年重复 · 下次还有 $r 天"
                            r >= 0 -> "距今还有 $r 天"
                            else -> "已过去 ${abs(r)} 天"
                        },
                        color = s.onAccentStrong,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
                            .background(if (bookId == book.id) Ink else cardBg)
                            .clickable { bookId = book.id }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            book.name,
                            color = if (bookId == book.id) OnInk else s.onSurface,
                            fontSize = 14.sp,
                            fontWeight = if (bookId == book.id) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(cardBg)
                        .clickable { addingBook = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = s.onAccentStrong, modifier = Modifier.size(16.dp))
                    Text("新建倒数本", color = s.onAccentStrong, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            // 图片 / 提醒 / 置顶：一组
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
            ) {
                if (!minimalMode) {
                    ImageRow(
                        label = "封面图片",
                        hint = if (cover != null) "已设置 · ${coverScale.label} · 透明度 ${(coverOpacity * 100).roundToInt()}%" else "选一张喜欢的图做卡片封面",
                        hasCurrent = cover != null,
                        onPick = { picking = ImageTarget.COVER }
                    )
                    HairlineInset(s)
                    ImageRow(
                        label = "详情页背景",
                        hint = if (wallpaper != null) "已设置 · ${wallpaperScale.label}" else "给详情页也换一张壁纸",
                        hasCurrent = wallpaper != null,
                        onPick = { picking = ImageTarget.WALLPAPER }
                    )
                    HairlineInset(s)
                    ToggleRow(
                        title = "开启提醒",
                        subtitle = "提前 7 天及当天 09:00 提醒",
                        checked = remind,
                        onChecked = {
                            remind = it
                            if (!it) dailyRemind = false
                        }
                    )
                    HairlineInset(s)
                    ToggleRow(
                        title = "每天提醒",
                        subtitle = "每天 $dailyRemindTime · ${dailyRemindKind.label}",
                        checked = dailyRemind,
                        onChecked = {
                            dailyRemind = it
                            if (it) {
                                remind = true
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val alarm = context.getSystemService(AlarmManager::class.java)
                                    if (!alarm.canScheduleExactAlarms()) {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                                    .setData(Uri.parse("package:${context.packageName}"))
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        onClick = { showDailyReminderSheet = true }
                    )
                    HairlineInset(s)
                }
                ToggleRow(
                    title = "置顶",
                    subtitle = "在首页大卡与列表最前展示",
                    checked = pinned,
                    onChecked = { pinned = it }
                )
            }
            Spacer(Modifier.height(6.dp))
            val enabled = title.isNotBlank()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale(0.97f)
                    .clip(RoundedCornerShape(50))
                    .background(if (enabled) s.accent else s.highest)
                    .clickable(enabled = enabled) { save() }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "保存倒数日",
                    color = if (enabled) s.onAccent else s.onSurfaceVariant,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (onDelete != null) {
                Text(
                    "删除此倒数日",
                    color = s.error,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth().clickable { confirmDelete = true }.padding(vertical = 12.dp),
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
            onImage = { uri ->
                val name = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    coverStore.copyIn(uri, "draft.edit.cover")
                }
                if (name != null) pendingImageEdit = PendingImageEdit(ImageTarget.COVER, name, cover, temporary = true)
                name != null
            },
            onEditCurrent = cover?.let { current ->
                { pendingImageEdit = PendingImageEdit(ImageTarget.COVER, current, current, temporary = false) }
            },
            onRemove = {
                cover?.let { coverStore.remove(it) }
                cover = null
            }
        )
        ImageTarget.WALLPAPER -> ImageSourceDialog(
            title = "详情页背景图片来源",
            hasCurrent = wallpaper != null,
            onDismiss = { picking = null },
            onImage = { uri ->
                val name = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    coverStore.copyIn(uri, "draftw.edit.wallpaper")
                }
                if (name != null) pendingImageEdit = PendingImageEdit(ImageTarget.WALLPAPER, name, wallpaper, temporary = true)
                name != null
            },
            onEditCurrent = wallpaper?.let { current ->
                { pendingImageEdit = PendingImageEdit(ImageTarget.WALLPAPER, current, current, temporary = false) }
            },
            onRemove = {
                wallpaper?.let { coverStore.remove(it) }
                wallpaper = null
            }
        )
        null -> Unit
    }

    if (showDatePicker) {
        WheelDatePickerSheet(
            initial = date,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                date = it
                showDatePicker = false
            }
        )
    }
    if (addingBook) {
        NewBookSheet(
            onConfirm = { name ->
                bookId = onAddBook(name)
                addingBook = false
            },
            onDismiss = { addingBook = false }
        )
    }
    if (showDailyReminderSheet) {
        DailyReminderSheet(
            time = dailyRemindTime,
            kind = dailyRemindKind,
            onTimeChange = { dailyRemindTime = it },
            onKindChange = { dailyRemindKind = it },
            onDismiss = { showDailyReminderSheet = false }
        )
    }
    pendingImageEdit?.let { pending ->
        ImageEditSheet(
            title = if (pending.target == ImageTarget.COVER) "编辑封面图片" else "编辑详情页背景",
            imageName = pending.newName,
            coverStore = coverStore,
            initialScale = if (pending.target == ImageTarget.COVER) coverScale else wallpaperScale,
            initialOpacity = if (pending.target == ImageTarget.COVER) coverOpacity else wallpaperOpacity,
            targetAspectRatio = if (pending.target == ImageTarget.COVER) 4f / 3f else 9f / 16f,
            targetLabel = if (pending.target == ImageTarget.COVER) "倒数日封面" else "详情页背景",
            cropOutputBase = if (pending.target == ImageTarget.COVER) "draft.edit.crop.cover" else "draftw.edit.crop.wallpaper",
            onSave = { scale, opacity, croppedName ->
                val selectedName = croppedName ?: pending.newName
                if (croppedName != null && pending.temporary) coverStore.remove(pending.newName)
                if (pending.target == ImageTarget.COVER) {
                    cover = selectedName
                    coverScale = scale
                    coverOpacity = opacity
                } else {
                    wallpaper = selectedName
                    wallpaperScale = scale
                    wallpaperOpacity = opacity
                }
                pendingImageEdit = null
            },
            onCancel = {
                if (pending.temporary) coverStore.remove(pending.newName)
                if (pending.target == ImageTarget.COVER) cover = pending.oldName else wallpaper = pending.oldName
                pendingImageEdit = null
            }
        )
    }
    if (confirmDelete) {
        IosAlertDialog(
            title = "删除「${initial?.title ?: title.trim()}」？",
            message = "删除后无法恢复，小记与图片会一并移除。",
            confirmText = "删除",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                onDelete?.invoke()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

/** 编辑倒数日时就地创建倒数本，创建完成后自动选中。 */
@Composable
private fun NewBookSheet(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val s = LocalSerein.current
    var name by remember { mutableStateOf("") }

    SereinSheet(
        title = "新建倒数本",
        onDismiss = onDismiss,
        trailingText = "创建",
        onTrailing = { if (name.isNotBlank()) onConfirm(name.trim()) }
    ) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
                if (name.isEmpty()) {
                    Text("倒数本名称", color = s.outlineVariant, fontSize = 16.sp)
                }
                BasicTextField(
                    value = name,
                    onValueChange = { if (it.length <= 12) name = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = s.onSurface),
                    cursorBrush = SolidColor(s.onAccentStrong),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )
            }
            Text("创建后会自动选中", color = s.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
        }
    }
}

/** 组内细分隔线（自文字左缘起）。 */
@Composable
private fun HairlineInset(s: SereinPalette) {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = s.outlineVariant.copy(alpha = 0.5f),
        thickness = 0.5.dp
    )
}

/** 图片设置行：整行可点，弹出三来源选择（无图标）。 */
@Composable
private fun ImageRow(
    label: String,
    hint: String,
    hasCurrent: Boolean,
    onPick: () -> Unit
) {
    val s = LocalSerein.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = s.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(hint, color = s.onSurfaceVariant, fontSize = 12.5.sp)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            if (hasCurrent) "更换" else "选择",
            color = s.onAccentStrong,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null
) {
    val s = LocalSerein.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = s.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = s.onSurfaceVariant, fontSize = 12.5.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedTrackColor = s.accent, checkedThumbColor = s.onAccent)
        )
    }
}

@Composable
private fun DailyReminderSheet(
    time: String,
    kind: ReminderKind,
    onTimeChange: (String) -> Unit,
    onKindChange: (ReminderKind) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalSerein.current
    val parts = time.split(":")
    var hour by remember(time) { mutableFloatStateOf(parts.getOrNull(0)?.toFloatOrNull()?.coerceIn(0f, 23f) ?: 9f) }
    var minute by remember(time) { mutableFloatStateOf(parts.getOrNull(1)?.toFloatOrNull()?.coerceIn(0f, 59f) ?: 0f) }
    SereinSheet(title = "每天提醒设置", onDismiss = onDismiss, trailingText = "完成", onTrailing = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("提醒时间", color = s.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("%02d:%02d".format(hour.roundToInt(), minute.roundToInt()), color = s.onSurface, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Slider(value = hour, onValueChange = { hour = it; onTimeChange("%02d:%02d".format(it.roundToInt(), minute.roundToInt())) }, valueRange = 0f..23f, steps = 22)
            Slider(value = minute, onValueChange = { minute = it; onTimeChange("%02d:%02d".format(hour.roundToInt(), it.roundToInt())) }, valueRange = 0f..59f, steps = 11)
            Text("提醒方式", color = s.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReminderKind.entries.forEach { option ->
                    Text(option.label, color = if (option == kind) OnInk else s.onSurface, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (option == kind) Ink else s.highest).clickable { onKindChange(option) }.padding(horizontal = 16.dp, vertical = 10.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

private fun tempCountdown(date: LocalDate, lunar: Boolean, repeatYearly: Boolean): Countdown =
    Countdown(id = "preview", title = "", date = date, lunar = lunar, repeatYearly = repeatYearly)
