package com.serein.day

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Monitor
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsTab(
    days: List<Countdown>,
    books: List<Book>,
    paletteIndex: Int,
    onPaletteChange: (Int) -> Unit,
    customPrimary: Int?,
    onCustomPrimaryChange: (Int) -> Unit,
    modeIndex: Int,
    onModeChange: (Int) -> Unit,
    haptics: Boolean,
    onHapticsChange: (Boolean) -> Unit,
    pinnedNotif: Boolean,
    onPinnedNotifChange: (Boolean) -> Unit,
    minimalMode: Boolean,
    onMinimalModeChange: (Boolean) -> Unit,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    onBackup: () -> Unit,
    onManageBooks: () -> Unit,
    onOpenArchive: () -> Unit,
    onBack: () -> Unit
) {
    val s = LocalSerein.current
    val context = LocalContext.current
    var showAbout by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showCustomColor by remember { mutableStateOf(false) }
    var showSortOrder by remember { mutableStateOf(false) }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onPinnedNotifChange(true)
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = "设置",
            large = true,
            leadingIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onLeading = onBack
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 36.dp)
        ) {
            // 外观：模式胶囊 + 色板圆点（与下方设置分组同款白色圆角卡片）
            Column(
                Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(s.container)
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(s.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Palette, contentDescription = null, tint = s.onAccent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("外观", color = s.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("选择你喜欢的界面风格", color = s.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ModePill(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.LightMode,
                        label = "浅色",
                        selected = modeIndex == 0,
                        accent = s.accent,
                        onAccent = s.onAccent
                    ) { onModeChange(0) }
                    ModePill(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.DarkMode,
                        label = "深色",
                        selected = modeIndex == 1,
                        accent = s.accent,
                        onAccent = s.onAccent
                    ) { onModeChange(1) }
                    ModePill(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Monitor,
                        label = "跟随系统",
                        selected = modeIndex == 2,
                        accent = s.accent,
                        onAccent = s.onAccent
                    ) { onModeChange(2) }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    PaletteNames.forEachIndexed { index, name ->
                        PaletteDot(
                            modifier = Modifier.weight(1f),
                            color = if (index == CUSTOM_PALETTE_INDEX) {
                                customPrimary?.let { Color(it) } ?: s.accent
                            } else {
                                paletteDotColor(index)
                            },
                            label = name,
                            selected = paletteIndex == index,
                            onSelect = {
                                if (index == CUSTOM_PALETTE_INDEX) showCustomColor = true else onPaletteChange(index)
                            }
                        )
                    }
                }
            }

            SettingsSectionLabel("通用")
            SettingsGroup {
                SettingsRow(
                    title = "极简模式",
                    subtitle = if (minimalMode) "已开启：纯列表，隐藏装饰与图片设置" else "纯列表展示，隐藏封面、插画等装饰",
                    trailing = {
                        LimeSwitch(checked = minimalMode, onCheckedChange = onMinimalModeChange)
                    }
                )
                GroupDivider()
                SettingsRow(
                    title = "排序方式",
                    subtitle = sortOrderLabel(sortOrder),
                    trailing = { Chevron() },
                    onClick = { showSortOrder = true }
                )
                GroupDivider()
                SettingsRow(
                    title = "触感反馈",
                    subtitle = "操作按钮与倒数时翻页振动",
                    trailing = {
                        LimeSwitch(checked = haptics, onCheckedChange = onHapticsChange)
                    }
                )
            }

            SettingsSectionLabel("提醒")
            SettingsGroup {
                SettingsRow(
                    title = "常驻通知",
                    subtitle = if (pinnedNotif) "已在通知栏显示置顶倒数卡" else "在通知栏常驻显示置顶的倒数卡",
                    trailing = {
                        LimeSwitch(
                            checked = pinnedNotif,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= 33 && !PinnedNotification.hasPermission(context)) {
                                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onPinnedNotifChange(enabled)
                                }
                            }
                        )
                    }
                )
            }

            SettingsSectionLabel("数据管理")
            SettingsGroup {
                SettingsRow(
                    title = "倒数本管理",
                    subtitle = "共 ${books.size} 本 · 新建、重命名或删除",
                    trailing = { Chevron() },
                    onClick = onManageBooks
                )
                GroupDivider()
                SettingsRow(
                    title = "归档管理",
                    subtitle = "共 ${days.count { it.archived }} 条封存的倒数日",
                    trailing = { Chevron() },
                    onClick = onOpenArchive
                )
                GroupDivider()
                SettingsRow(
                    title = "数据与备份",
                    subtitle = "全部记录导出为 JSON 并分享（共 ${days.size} 条）",
                    trailing = { Chevron() },
                    onClick = onBackup
                )
            }

            SettingsSectionLabel("关于软件")
            SettingsGroup {
                SettingsRow(
                    title = "关于 Serein Day",
                    subtitle = "v1.5.2 Acid Lime · Days Matter 式",
                    trailing = { Chevron() },
                    onClick = { showAbout = true }
                )
                GroupDivider()
                SettingsRow(
                    title = "用户协议与隐私规范",
                    subtitle = "完全离线存储 · 数据归属于你",
                    trailing = { Chevron() },
                    onClick = { showPrivacy = true }
                )
            }

            Spacer(Modifier.height(28.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Sparkle(Modifier.size(22.dp))
                Spacer(Modifier.height(8.dp))
                Text("Serein Day · 让重要的日子更清晰", color = s.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }

    if (showCustomColor) {
        CustomColorSheet(
            initial = customPrimary ?: 0xFFC8F531.toInt(),
            onConfirm = { argb ->
                onCustomPrimaryChange(argb)
                onPaletteChange(CUSTOM_PALETTE_INDEX)
                showCustomColor = false
            },
            onDismiss = { showCustomColor = false }
        )
    }
    if (showSortOrder) {
        SortOrderSheet(
            current = sortOrder,
            onSelect = { onSortOrderChange(it); showSortOrder = false },
            onDismiss = { showSortOrder = false }
        )
    }
    if (showAbout) {
        IosAlertDialog(
            title = "关于 Serein Day",
            message = "Serein Day v1.5.2\nAcid Lime 设计系统 · Material 3\n\n一个简约的倒数日应用：Off-white 底 + 黑色大卡 + 荧光青柠，把重要的日子留在眼前。",
            confirmText = "好的",
            dismissText = null,
            onConfirm = { showAbout = false },
            onDismiss = null
        )
    }
    if (showPrivacy) {
        IosAlertDialog(
            title = "用户协议与隐私规范",
            message = "Serein Day 完全离线运行：所有记录只保存在本机应用存储中，不联网、不上传、不收集任何数据。清除应用数据或卸载即会删除全部记录。",
            confirmText = "好的",
            dismissText = null,
            onConfirm = { showPrivacy = false },
            onDismiss = null
        )
    }
}

private fun sortOrderLabel(order: SortOrder): String = when (order) {
    SortOrder.BY_REMAINING -> "按剩余天数"
    SortOrder.BY_DATE -> "按目标日期"
    SortOrder.BY_CREATED -> "按添加时间"
}

@Composable
private fun SettingsSectionLabel(text: String) {
    val s = LocalSerein.current
    Text(
        text,
        color = s.onSurfaceVariant,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(start = 24.dp, top = 22.dp, bottom = 10.dp)
    )
}

/** 设置分组容器：一个纯白圆角卡片，行间用细线分隔。 */
@Composable
private fun SettingsGroup(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val s = LocalSerein.current
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(s.container)
            .padding(vertical = 4.dp),
        content = content
    )
}

@Composable
private fun GroupDivider() {
    val s = LocalSerein.current
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = s.outlineVariant.copy(alpha = 0.5f),
        thickness = 0.5.dp
    )
}

/** 设置行：标题/副标题 + 可选尾随控件（无图标，纯文字排版）。 */
@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val s = LocalSerein.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = s.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(1.dp))
            Text(subtitle, color = s.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        trailing?.invoke()
    }
}

@Composable
private fun LimeSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val s = LocalSerein.current
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = s.accent,
            checkedThumbColor = s.onAccent,
            uncheckedTrackColor = s.highest,
            uncheckedThumbColor = s.container,
            uncheckedBorderColor = s.outlineVariant
        )
    )
}

@Composable
private fun Chevron() {
    val s = LocalSerein.current
    Icon(
        Icons.Outlined.ChevronRight,
        contentDescription = null,
        tint = s.outlineVariant,
        modifier = Modifier.size(18.dp)
    )
}

/** 外观卡内的模式胶囊：选中 = 青柠底黑字，未选中 = 中性底。 */
@Composable
private fun ModePill(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    accent: Color,
    onAccent: Color,
    onSelect: () -> Unit
) {
    val s = LocalSerein.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) accent else s.highest)
            .clickable(onClick = onSelect)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) onAccent else s.onSurfaceVariant,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = if (selected) onAccent else s.onSurfaceVariant,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

/** 排序方式：底部弹层单选。 */
@Composable
private fun SortOrderSheet(current: SortOrder, onSelect: (SortOrder) -> Unit, onDismiss: () -> Unit) {
    val s = LocalSerein.current
    SereinSheet(title = "排序方式", onDismiss = onDismiss) {
        listOf(
            SortOrder.BY_REMAINING to "按剩余天数",
            SortOrder.BY_DATE to "按目标日期",
            SortOrder.BY_CREATED to "按添加时间"
        ).forEach { (option, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(option) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    color = s.onSurface,
                    fontSize = 15.sp,
                    fontWeight = if (option == current) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (option == current) {
                    Icon(Icons.Filled.Check, contentDescription = "已选择", tint = s.onAccentStrong, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun PaletteDot(
    modifier: Modifier = Modifier,
    color: Color,
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val s = LocalSerein.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // 纯圆形色点：选中时外圈描边，留一圈底色间隙
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .then(if (selected) Modifier.border(2.dp, s.onSurface, CircleShape) else Modifier)
                .clickable(onClick = onSelect),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(if (selected) 24.dp else 30.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = if (selected) s.onSurface else s.onSurfaceVariant,
            fontSize = 9.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

/** 自定义配色：色相 + 饱和度双滑杆，实时预览。 */
@Composable
private fun CustomColorSheet(initial: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val s = LocalSerein.current
    val startHsl = FloatArray(3)
    android.graphics.Color.colorToHSV(initial, startHsl)
    var hue by remember { mutableFloatStateOf(startHsl[0]) }
    var sat by remember { mutableFloatStateOf(startHsl[1]) }
    val current = androidx.compose.ui.graphics.Color.hsv(hue, sat, if (hue in 60f..200f) 0.9f else 0.55f)
    val argb = android.graphics.Color.argb(
        255,
        (current.red * 255).toInt(),
        (current.green * 255).toInt(),
        (current.blue * 255).toInt()
    )
    val preview = paletteFromPrimary(current, dark = false)

    SereinSheet(
        title = "自定义配色",
        onDismiss = onDismiss,
        trailingText = "使用此颜色",
        onTrailing = { onConfirm(argb) }
    ) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(current))
                Column {
                    Text("点缀色预览", color = s.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "#${Integer.toHexString(argb).uppercase().padStart(8, '0').substring(2)}",
                        color = s.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
            Text("底色保持 Off-white / 纯黑中性层，点缀色驱动 CTA、进度与高亮", color = s.onSurfaceVariant, fontSize = 12.5.sp)
            Column {
                Text("色相", color = s.onSurfaceVariant, fontSize = 12.sp)
                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(thumbColor = current, activeTrackColor = current, inactiveTrackColor = s.highest)
                )
            }
            Column {
                Text("饱和度", color = s.onSurfaceVariant, fontSize = 12.sp)
                Slider(
                    value = sat,
                    onValueChange = { sat = it },
                    valueRange = 0.3f..0.95f,
                    colors = SliderDefaults.colors(thumbColor = current, activeTrackColor = current, inactiveTrackColor = s.highest)
                )
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(preview.container).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(preview.accent))
                Text("预览卡片", color = preview.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                PillChip("还有", preview.accent, preview.onAccent)
            }
        }
    }
}

/**
 * 倒数本管理弹层：新建、重命名、删除（删除时事件移入首个剩余倒数本）。
 */
@Composable
fun BookManagerSheet(
    books: List<Book>,
    days: List<Countdown>,
    onAdd: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalSerein.current
    var editingId by remember { mutableStateOf<String?>(null) }
    var editName by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    SereinSheet(
        title = "管理倒数本",
        onDismiss = onDismiss,
        trailingText = "完成",
        onTrailing = onDismiss
    ) {
        Column(Modifier.padding(horizontal = 14.dp)) {
            books.forEach { book ->
                val count = days.count { it.bookId == book.id && !it.archived }
                if (editingId == book.id) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = editName,
                            onValueChange = { if (it.length <= 12) editName = it },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 15.sp, color = s.onSurface),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "保存",
                            color = s.onAccentStrong,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { if (editName.isNotBlank()) { onRename(book.id, editName.trim()); editingId = null } }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                        Text(
                            "取消",
                            color = s.onSurfaceVariant,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { editingId = null }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconCircle(bookPastel(book.name), bookIcon(book.name), Ink, size = 34.dp, iconSize = 16.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(book.name, color = s.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$count 个事件", color = s.onSurfaceVariant, fontSize = 11.5.sp)
                        }
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "重命名",
                            tint = s.primary,
                            modifier = Modifier
                                .size(38.dp)
                                .padding(9.dp)
                                .clickable { editingId = book.id; editName = book.name }
                        )
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "删除倒数本",
                            tint = if (books.size > 1) s.error else s.outlineVariant,
                            modifier = Modifier
                                .size(38.dp)
                                .padding(9.dp)
                                .clickable(enabled = books.size > 1) { onDelete(book.id) }
                        )
                    }
                }
            }
            HorizontalDivider(color = s.outlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
            if (adding) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = newName,
                        onValueChange = { if (it.length <= 12) newName = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp, color = s.onSurface),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "添加",
                        color = s.onAccentStrong,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { if (newName.isNotBlank()) { onAdd(newName.trim()); newName = ""; adding = false } }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                    Text(
                        "取消",
                        color = s.onSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { adding = false }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { adding = true }
                        .padding(horizontal = 8.dp, vertical = 14.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = s.onAccentStrong, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("新建倒数本", color = s.onAccentStrong, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
