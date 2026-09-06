package com.serein.day

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PaletteDotColors = listOf(
    Color(0xFF6750A4), Color(0xFF8C4433), Color(0xFF3B5599), Color(0xFF006B5B),
    Color(0xFF8B5A00), Color(0xFF984061), Color(0xFF006874), Color(0xFF3F4754)
)

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
    weekStart: String,
    onWeekStartChange: (String) -> Unit,
    pinnedNotif: Boolean,
    onPinnedNotifChange: (Boolean) -> Unit,
    onBackup: () -> Unit,
    onManageBooks: () -> Unit
) {
    val s = LocalSerein.current
    val context = LocalContext.current
    var showReminderInfo by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showCustomColor by remember { mutableStateOf(false) }
    var showWeekStart by remember { mutableStateOf(false) }
    val modeName = listOf("浅色", "深色", "跟随系统")[modeIndex.coerceIn(0, 2)]

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onPinnedNotifChange(true)
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Settings")
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 112.dp)
        ) {

        // 外观与主题
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(s.container)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PillChip("自定义配色", s.accent, s.onAccentStrong, icon = Icons.Outlined.Palette)
                Spacer(Modifier.weight(1f))
                IconCircle(s.high, Icons.Outlined.Palette, s.onAccentStrong, size = 44.dp, iconSize = 20.dp)
            }
            Spacer(Modifier.height(12.dp))
            Text("外观与主题", color = s.onSurface, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("Serene Forest Mint · $modeName 模式 · Roboto Flex", color = s.onSurfaceVariant, fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(s.surfaceLow)
                    .padding(14.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("配色方案预设", color = s.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text("当前生效", color = s.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PaletteDotColors.forEachIndexed { index, color ->
                        PaletteDot(
                            modifier = Modifier.weight(1f),
                            color = color,
                            label = PaletteNames[index],
                            selected = paletteIndex == index,
                            onSelect = { onPaletteChange(index) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        PaletteDot(
                            color = customPrimary?.let { Color(it) } ?: s.primary,
                            label = "自定义",
                            selected = paletteIndex == CUSTOM_PALETTE_INDEX,
                            onSelect = { showCustomColor = true }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeChip("浅色", Icons.Outlined.LightMode, modeIndex == 0, Modifier.weight(1f)) { onModeChange(0) }
                ModeChip("深色", Icons.Outlined.DarkMode, modeIndex == 1, Modifier.weight(1f)) { onModeChange(1) }
                ModeChip("跟随系统", Icons.Outlined.BrightnessAuto, modeIndex == 2, Modifier.weight(1f)) { onModeChange(2) }
            }
        }

        SettingsSectionLabel("偏好与核心数据")
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsCard(
                iconBg = s.secondaryContainer,
                icon = Icons.Filled.Edit,
                iconTint = s.onAccentStrong,
                title = "倒数本管理",
                subtitle = "共 ${books.size} 本 · 新建、重命名或删除",
                onClick = onManageBooks
            )
            SettingsCard(
                iconBg = s.secondaryContainer,
                icon = Icons.Outlined.Notifications,
                iconTint = s.onAccentStrong,
                title = "常驻通知",
                subtitle = if (pinnedNotif) "已在通知栏显示置顶倒数卡" else "在通知栏常驻显示置顶的倒数卡",
                trailing = {
                    Switch(
                        checked = pinnedNotif,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= 33 && !PinnedNotification.hasPermission(context)) {
                                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onPinnedNotifChange(enabled)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = s.primary, checkedThumbColor = s.onPrimary)
                    )
                }
            )
            SettingsCard(
                iconBg = s.secondaryContainer,
                icon = Icons.Outlined.Notifications,
                iconTint = s.onAccentStrong,
                title = "提醒设置",
                subtitle = "在编辑页为每个日子单独开启提醒",
                onClick = { showReminderInfo = true }
            )
            SettingsCard(
                iconBg = Peach,
                icon = Icons.Outlined.Backup,
                iconTint = OnPeach,
                title = "数据与备份",
                subtitle = "全部记录导出为 JSON 并分享（共 ${days.size} 条）",
                onClick = onBackup
            )
        }

        SettingsSectionLabel("日历与系统行为")
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsCard(
                iconBg = s.secondaryContainer,
                icon = Icons.Outlined.Spa,
                iconTint = s.onAccentStrong,
                title = "周起始日",
                subtitle = "日历视图第一列显示",
                trailing = {
                    PillChip(weekStart, s.accent, s.onAccentStrong)
                    Spacer(Modifier.width(6.dp))
                },
                onClick = { showWeekStart = true }
            )
            SettingsCard(
                iconBg = s.secondaryContainer,
                icon = Icons.Outlined.Vibration,
                iconTint = s.onAccentStrong,
                title = "触感反馈",
                subtitle = "操作按钮与倒数时翻页振动",
                trailing = {
                    Switch(
                        checked = haptics,
                        onCheckedChange = onHapticsChange,
                        colors = SwitchDefaults.colors(checkedTrackColor = s.primary, checkedThumbColor = s.onPrimary)
                    )
                }
            )
        }

        SettingsSectionLabel("关于软件")
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsCard(
                iconBg = s.secondaryContainer,
                icon = Icons.Outlined.Info,
                iconTint = s.onAccentStrong,
                title = "关于 Serein Day",
                subtitle = "v1.1.0 Expressive · Material You 3",
                onClick = { showAbout = true }
            )
            SettingsCard(
                iconBg = s.container,
                icon = Icons.Outlined.VerifiedUser,
                iconTint = s.onSurfaceVariant,
                title = "用户协议与隐私规范",
                subtitle = "完全离线存储 · 数据归属于你",
                onClick = { showPrivacy = true }
            )
        }

        Spacer(Modifier.height(28.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconCircle(s.high, Icons.Outlined.Spa, s.onAccentStrong, size = 40.dp, iconSize = 19.dp)
            Spacer(Modifier.height(10.dp))
            Text("Serein Day · 温柔记录每一个重要的日子", color = s.onSurfaceVariant, fontSize = 12.sp)
        }
        }
    }

    if (showCustomColor) {
        CustomColorDialog(
            initial = customPrimary ?: 0xFF006B5B.toInt(),
            onConfirm = { argb ->
                onCustomPrimaryChange(argb)
                onPaletteChange(CUSTOM_PALETTE_INDEX)
                showCustomColor = false
            },
            onDismiss = { showCustomColor = false }
        )
    }
    if (showWeekStart) {
        WeekStartDialog(
            current = weekStart,
            onSelect = { onWeekStartChange(it); showWeekStart = false },
            onDismiss = { showWeekStart = false }
        )
    }
    if (showReminderInfo) {
        InfoDialog(
            title = "提醒设置",
            text = "在新建或编辑倒数日时打开「开启提醒」，即可为该日子保存提醒计划（提前 7 天及当天 09:00）。通知推送能力将在后续版本中启用。"
        ) { showReminderInfo = false }
    }
    if (showAbout) {
        InfoDialog(
            title = "关于 Serein Day",
            text = "Serein Day v1.1.0\nSerene Forest Mint · Material 3 Expressive\n\n一个温柔、纯净的倒数日应用，把重要的日子留在眼前。"
        ) { showAbout = false }
    }
    if (showPrivacy) {
        InfoDialog(
            title = "用户协议与隐私规范",
            text = "Serein Day 完全离线运行：所有记录只保存在本机应用存储中，不联网、不上传、不收集任何数据。清除应用数据或卸载即会删除全部记录。"
        ) { showPrivacy = false }
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
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color)
                .clickable(onClick = onSelect),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = "已选择", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = if (selected) s.onSurface else s.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/** 自定义配色：色相 + 饱和度双滑杆，实时预览。 */
@Composable
private fun CustomColorDialog(initial: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val s = LocalSerein.current
    val startHsl = FloatArray(3)
    android.graphics.Color.colorToHSV(initial, startHsl)
    var hue by remember { mutableFloatStateOf(startHsl[0]) }
    var sat by remember { mutableFloatStateOf(startHsl[1]) }
    val current = androidx.compose.ui.graphics.Color.hsv(hue, sat, 0.42f)
    val argb = android.graphics.Color.argb(
        255,
        (current.red * 255).toInt(),
        (current.green * 255).toInt(),
        (current.blue * 255).toInt()
    )
    val preview = paletteFromPrimary(current, dark = false)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义配色") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(current))
                    Column {
                        Text("主色预览", color = s.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "#${Integer.toHexString(argb).uppercase().padStart(8, '0').substring(2)}",
                            color = s.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
                Text("卡片、按钮与强调色都会跟随主色", color = s.onSurfaceVariant, fontSize = 12.5.sp)
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
                        valueRange = 0.12f..0.85f,
                        colors = SliderDefaults.colors(thumbColor = current, activeTrackColor = current, inactiveTrackColor = s.highest)
                    )
                }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(preview.container).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(30.dp).clip(CircleShape).background(preview.primary))
                    Text("预览卡片", color = preview.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    PillChip("还有", preview.accent, preview.onAccentStrong)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(argb) }) { Text("使用此颜色", color = s.primary, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun WeekStartDialog(current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val s = LocalSerein.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("周起始日") },
        text = {
            Column {
                listOf("周一", "周六", "周日").forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(option) }
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            option,
                            color = s.onSurface,
                            fontSize = 15.sp,
                            fontWeight = if (option == current) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (option == current) {
                            Icon(Icons.Filled.Check, contentDescription = "已选择", tint = s.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消", color = s.primary) } }
    )
}

@Composable
private fun ModeChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val s = LocalSerein.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) s.highest else if (s.isDark) s.high else Color.White)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) s.onSurface else s.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = if (selected) s.onSurface else s.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    val s = LocalSerein.current
    Text(
        text,
        color = s.primary,
        fontSize = 13.5.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 22.dp, top = 22.dp, bottom = 10.dp)
    )
}

@Composable
private fun SettingsCard(
    iconBg: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val s = LocalSerein.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(s.container)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconCircle(iconBg, icon, iconTint)
        Column(Modifier.weight(1f)) {
            Text(title, color = s.onSurface, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = s.onSurfaceVariant, fontSize = 12.5.sp)
        }
        trailing?.invoke()
    }
}

@Composable
private fun InfoDialog(title: String, text: String, onDismiss: () -> Unit) {
    val s = LocalSerein.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("好的", color = s.primary) }
        }
    )
}
