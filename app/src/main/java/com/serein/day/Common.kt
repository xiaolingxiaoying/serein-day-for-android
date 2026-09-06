package com.serein.day

import android.graphics.BitmapFactory
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlin.math.roundToInt

/**
 * 极简顶栏：左侧为返回箭头或自定义槽位（如倒数本切换菜单），右侧为可选文字按钮或图标槽位。
 */
@Composable
fun TopBar(
    title: String,
    leadingIcon: ImageVector? = null,
    onLeading: (() -> Unit)? = null,
    leadingSlot: (@Composable () -> Unit)? = null,
    trailingText: String? = null,
    onTrailing: (() -> Unit)? = null,
    trailingSlot: (@Composable () -> Unit)? = null
) {
    val s = LocalSerein.current
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingSlot != null || leadingIcon != null) {
            Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                when {
                    leadingSlot != null -> leadingSlot()
                    else -> leadingIcon?.let { icon ->
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = s.onSurface,
                            modifier = Modifier.size(23.dp).clickable(enabled = onLeading != null) { onLeading?.invoke() }
                        )
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
        }
        Text(
            title,
            color = s.onSurface,
            fontSize = 23.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (trailingSlot != null) {
            trailingSlot()
        } else if (trailingText != null) {
            Text(
                trailingText,
                color = s.primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(enabled = onTrailing != null) { onTrailing?.invoke() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun BackTopBar(title: String, onBack: () -> Unit, trailingText: String? = null, onTrailing: (() -> Unit)? = null) {
    TopBar(title = title, leadingIcon = Icons.AutoMirrored.Filled.ArrowBack, onLeading = onBack, trailingText = trailingText, onTrailing = onTrailing)
}

@Composable
fun BottomNavBar(selected: Int, onSelect: (Int) -> Unit) {
    val s = LocalSerein.current
    data class Item(val label: String, val icon: ImageVector, val iconSelected: ImageVector)
    val items = listOf(
        Item("首页", Icons.Outlined.Home, Icons.Filled.Home),
        Item("设置", Icons.Outlined.Settings, Icons.Filled.Settings)
    )
    Row(
        modifier = Modifier.fillMaxWidth().background(s.surface).navigationBarsPadding().padding(top = 6.dp, bottom = 12.dp)
    ) {
        items.forEachIndexed { index, item ->
            Column(
                modifier = Modifier.weight(1f).clickable { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected == index) s.secondaryContainer else Color.Transparent)
                        .padding(horizontal = 18.dp, vertical = 5.dp)
                ) {
                    Icon(
                        if (selected == index) item.iconSelected else item.icon,
                        contentDescription = item.label,
                        tint = if (selected == index) s.onSurface else s.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    item.label,
                    fontSize = 11.5.sp,
                    fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected == index) s.onSurface else s.onSurfaceVariant
                )
            }
        }
    }
}

/** 圆形新建按钮。 */
@Composable
fun AddFab(modifier: Modifier = Modifier, hapticsEnabled: Boolean = true, onClick: () -> Unit) {
    val s = LocalSerein.current
    val view = LocalView.current
    Box(
        modifier = modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(s.primary)
            .clickable {
                if (hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Edit, contentDescription = "新建倒数日", tint = s.onPrimary, modifier = Modifier.size(22.dp))
    }
}

/** 胶囊标签。 */
@Composable
fun PillChip(
    text: String,
    background: Color,
    contentColor: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    fontSize: Double = 11.5,
    maxLines: Int = 1
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(13.dp))
        Text(text, color = contentColor, fontSize = fontSize.sp, fontWeight = FontWeight.SemiBold, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SectionBar(title: String, trailing: String) {
    val s = LocalSerein.current
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = s.onSurface, fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(trailing, color = s.onSurfaceVariant, fontSize = 12.5.sp)
    }
}

/** 圆形里程碑进度环，中心显示百分比。 */
@Composable
fun ProgressArc(
    progress: Float,
    size: Dp = 64.dp,
    stroke: Dp = 6.dp,
    track: Color,
    arcColor: Color,
    textColor: Color
) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val topLeft = Offset(strokePx / 2, strokePx / 2)
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(track, 0f, 360f, false, style = Stroke(strokePx, cap = StrokeCap.Round), topLeft = topLeft, size = arcSize)
            drawArc(
                arcColor, -90f, 360f * progress.coerceIn(0f, 1f), false,
                style = Stroke(strokePx, cap = StrokeCap.Round), topLeft = topLeft, size = arcSize
            )
        }
        Text(
            "${(progress.coerceIn(0f, 1f) * 100).roundToInt()}%",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
fun IconCircle(background: Color, icon: ImageVector, tint: Color, size: Dp = 46.dp, iconSize: Dp = 22.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

fun categoryIcon(category: String): ImageVector = when (category) {
    "旅行" -> Icons.Outlined.FlightTakeoff
    "生日" -> Icons.Outlined.Cake
    "考试" -> Icons.Outlined.School
    else -> Icons.Outlined.FavoriteBorder
}

fun bookIcon(name: String): ImageVector = categoryIcon(name)

/** 分类对应的柔和风景渐变（未设置封面时的卡片底图）。 */
fun heroBrush(bookName: String): Brush = when (bookName) {
    "旅行" -> Brush.verticalGradient(listOf(Color(0xFFD9CBE8), Color(0xFFF3DFD2), Color(0xFFDCEBE3)))
    "生日" -> Brush.verticalGradient(listOf(Color(0xFFF6DCC3), Color(0xFFFBEFE2), Color(0xFFEFE0D8)))
    "考试" -> Brush.verticalGradient(listOf(Color(0xFFCFE0F0), Color(0xFFE4EEF4), Color(0xFFDDEBE2)))
    else -> Brush.verticalGradient(listOf(Color(0xFFD6DFF0), Color(0xFFEAE0DC), Color(0xFFDDEBE3)))
}

/** 加载封面副本文件为位图（解码到目标宽度以内，避免整图内存）。 */
@Composable
fun rememberCoverBitmap(cover: String?, coverStore: CoverStore): androidx.compose.ui.graphics.ImageBitmap? {
    return remember(cover) {
        cover ?: return@remember null
        val file: File = coverStore.resolve(cover)
        if (!file.exists()) return@remember null
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= 1080) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
        }.getOrNull()
    }
}

/** 列表行的小记数徽标（零小记时隐藏）。 */
@Composable
fun NoteBadge(count: Int, tint: Color) {
    if (count <= 0) return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(
            Icons.Outlined.EditNote,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(12.dp)
        )
        Text(count.toString(), color = tint, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EmptyHome() {
    val s = LocalSerein.current
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 60.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconCircle(s.container, Icons.Outlined.Spa, s.onAccentStrong, size = 84.dp, iconSize = 36.dp)
        Spacer(Modifier.height(18.dp))
        Text("还没有倒数日", color = s.onSurface, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("点右下角 +，温柔记下第一个重要时刻", color = s.onSurfaceVariant, fontSize = 13.5.sp)
    }
}

@Composable
fun EmptyArchive() {
    val s = LocalSerein.current
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 60.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconCircle(s.container, Icons.Outlined.Inventory2, s.onAccentStrong, size = 84.dp, iconSize = 32.dp)
        Spacer(Modifier.height(18.dp))
        Text("还没有归档的倒数日", color = s.onSurface, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("在详情里选择「封存」，即可归档到这里", color = s.onSurfaceVariant, fontSize = 13.5.sp)
    }
}

/** 图片填充。 */
@Composable
fun CoverImage(bitmap: androidx.compose.ui.graphics.ImageBitmap?, modifier: Modifier = Modifier, contentDescription: String? = null) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}
