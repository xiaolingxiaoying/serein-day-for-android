package com.serein.day

import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlin.math.roundToInt

/** 数字等宽（tnum），大号天数在计时刷新时不会左右跳动。 */
val TnumStyle = TextStyle(fontFeatureSettings = "tnum")

@Volatile
private var reducedMotionCache: Boolean? = null

/** 无障碍「移除动画」开关查询（进程内缓存，改动后重启生效——该设置极少变化）。 */
fun isReducedMotion(context: android.content.Context): Boolean =
    reducedMotionCache ?: runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }.getOrDefault(false).also { reducedMotionCache = it }

/** 系统关闭了动画时长（无障碍「移除动画」）时返回 true，动效应降级为即时切换。 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember { isReducedMotion(context) }
}

/**
 * 按压缩放：手指按下的瞬间即缩到 pressedScale（iOS 式即时反馈，不等抬起），松开用弹簧弹回。
 * 弹簧可被打断并从当前值重新出发；系统关闭动画时不缩放，只保留涟漪。
 */
fun Modifier.pressScale(pressedScale: Float = 0.97f): Modifier = composed {
    val reduced = rememberReducedMotion()
    val pressed = remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed.value && !reduced) pressedScale else 1f,
        animationSpec = if (pressed.value) spring(dampingRatio = 1f, stiffness = 1600f) else spring(dampingRatio = 1f, stiffness = 480f),
        label = "pressScale"
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed.value = true
                waitForUpOrCancellation()
                pressed.value = false
            }
        }
}

/**
 * 顶栏：大标题模式用 Black 超大字号（Oversized Typography），
 * 左侧为返回箭头或自定义槽位，右侧为可选文字按钮或图标槽位。
 */
@Composable
fun TopBar(
    title: String,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    onLeading: (() -> Unit)? = null,
    leadingSlot: (@Composable () -> Unit)? = null,
    trailingText: String? = null,
    onTrailing: (() -> Unit)? = null,
    trailingSlot: (@Composable () -> Unit)? = null,
    large: Boolean = false
) {
    val s = LocalSerein.current
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
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
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = s.onSurface,
                fontSize = if (large) 32.sp else 20.sp,
                fontWeight = if (large) FontWeight.Black else FontWeight.Bold,
                letterSpacing = if (large) (-1.2).sp else (-0.3).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, color = s.onSurfaceVariant, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
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

/** 圆形深色按钮（设置入口等）：近黑圆底 + 白色图标，深色下补一圈细描边保持可辨。 */
@Composable
fun InkCircleButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val s = LocalSerein.current
    Box(
        modifier = Modifier
            .size(44.dp)
            .pressScale(0.9f)
            .clip(CircleShape)
            .background(Ink)
            .then(if (s.isDark) Modifier.border(1.dp, s.outlineVariant, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = OnInk, modifier = Modifier.size(20.dp))
    }
}

/** 底部主 CTA：整宽青柠胶囊（＋ 添加倒数日），参考稿同款。 */
@Composable
fun AddPillButton(
    modifier: Modifier = Modifier,
    label: String = "添加倒数日",
    onClick: () -> Unit
) {
    val s = LocalSerein.current
    val view = LocalView.current
    val hapticsOn = LocalHapticsEnabled.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .pressScale(0.97f)
            .clip(RoundedCornerShape(50))
            .background(s.accent)
            .clickable {
                if (hapticsOn) view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onClick()
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Add, contentDescription = "新建倒数日", tint = s.onAccent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = s.onAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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

/**
 * 圆形里程碑进度环；中心默认显示百分比，也可传入自定义内容
 * （详情页在环心放「还有 N 天」）。首次进入从 0 展开到当前进度。
 */
@Composable
fun ProgressArc(
    progress: Float,
    size: Dp = 64.dp,
    stroke: Dp = 6.dp,
    track: Color,
    arcColor: Color,
    textColor: Color,
    centerContent: (@Composable () -> Unit)? = null
) {
    var played by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { played = true }
    val animated by animateFloatAsState(
        targetValue = if (played) progress.coerceIn(0f, 1f) else 0f,
        animationSpec = spring(dampingRatio = 1f, stiffness = 160f),
        label = "arcProgress"
    )
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val topLeft = Offset(strokePx / 2, strokePx / 2)
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(track, 0f, 360f, false, style = Stroke(strokePx, cap = StrokeCap.Round), topLeft = topLeft, size = arcSize)
            drawArc(
                arcColor, -90f, 360f * animated, false,
                style = Stroke(strokePx, cap = StrokeCap.Round), topLeft = topLeft, size = arcSize
            )
        }
        if (centerContent != null) {
            centerContent()
        } else {
            Text(
                "${(progress.coerceIn(0f, 1f) * 100).roundToInt()}%",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

@Composable
fun IconCircle(background: Color, icon: ImageVector, tint: Color, size: Dp = 46.dp, iconSize: Dp = 22.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

fun categoryIcon(category: String): ImageVector = when {
    category.contains("旅行") -> Icons.Outlined.FlightTakeoff
    category.contains("生日") -> Icons.Outlined.Cake
    category.contains("考试") || category.contains("学习") -> Icons.Outlined.School
    else -> Icons.Outlined.FavoriteBorder
}

fun bookIcon(name: String): ImageVector = categoryIcon(name)

/** 类别对应的马卡龙底色（图标圆）：底色低饱和、图标用近黑墨色。 */
fun bookPastel(name: String): Color = when {
    name.contains("旅行") -> PastelLavender
    name.contains("生日") -> PastelButter
    name.contains("考试") || name.contains("学习") -> PastelSky
    name.contains("纪念") -> PastelBlush
    else -> PastelMint
}

/** 四角星装饰（参考稿中的 ✦ 荧光星）。 */
@Composable
fun Sparkle(modifier: Modifier = Modifier, color: Color = AcidLime) {
    Canvas(modifier) { drawSparkle(center.x, center.y, size.minDimension / 2f, color, filled = true) }
}

/** 四角星路径：菱形轮廓 + 向内收的四段贝塞尔，读作「闪光」。 */
private fun DrawScope.drawSparkle(cx: Float, cy: Float, r: Float, color: Color, filled: Boolean) {
    val path = Path().apply {
        moveTo(cx, cy - r)
        quadraticTo(cx + r * 0.14f, cy - r * 0.14f, cx + r, cy)
        quadraticTo(cx + r * 0.14f, cy + r * 0.14f, cx, cy + r)
        quadraticTo(cx - r * 0.14f, cy + r * 0.14f, cx - r, cy)
        quadraticTo(cx - r * 0.14f, cy - r * 0.14f, cx, cy - r)
        close()
    }
    if (filled) drawPath(path, color) else drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
}

/** 封面位图 Lru 内存缓存：列表滚动与页面往返不再反复解码（修复归档页首次打开卡顿）。 */
private object CoverBitmapCache {
    private const val MAX_DIMEN = 720
    private const val KB = 1024
    // 按位图字节计费，上限取堆的 1/8（6–32MB）：张数计费时大图会悄悄挤爆低内存设备
    private val cache = object : androidx.collection.LruCache<String, androidx.compose.ui.graphics.ImageBitmap>(
        (Runtime.getRuntime().maxMemory() / 8 / KB).toInt().coerceIn(6 * KB, 32 * KB)
    ) {
        override fun sizeOf(key: String, value: androidx.compose.ui.graphics.ImageBitmap): Int =
            value.asAndroidBitmap().allocationByteCount / KB
    }

    fun peek(name: String): androidx.compose.ui.graphics.ImageBitmap? = cache.get(name)

    fun load(name: String, coverStore: CoverStore): androidx.compose.ui.graphics.ImageBitmap? {
        cache.get(name)?.let { return it }
        val file: File = coverStore.resolve(name)
        if (!file.exists()) return null
        val bitmap = decodeSampledOrientedBitmap(file, MAX_DIMEN) ?: return null
        val imageBitmap = bitmap.asImageBitmap()
        cache.put(name, imageBitmap)
        return imageBitmap
    }
}

/**
 * 进界面/转场前预热封面或壁纸：在 IO 线程预先解码并写入内存缓存。
 * 之后 rememberCoverBitmap 命中缓存即同步返回，避免转场中位图「啪地出现」造成闪变/撕裂。
 */
suspend fun prewarmCover(name: String?, coverStore: CoverStore) {
    if (name == null) return
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        CoverBitmapCache.load(name, coverStore)
    }
}

/** 加载封面副本为位图：IO 线程解码、主线程零阻塞，命中缓存即同步返回。 */
@Composable
fun rememberCoverBitmap(cover: String?, coverStore: CoverStore): androidx.compose.ui.graphics.ImageBitmap? {
    if (cover == null) return null
    val state = androidx.compose.runtime.produceState(
        initialValue = CoverBitmapCache.peek(cover),
        key1 = cover
    ) {
        if (value == null) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                CoverBitmapCache.load(cover, coverStore)
            }
        }
    }
    return state.value
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
fun EmptyArchive() {
    val s = LocalSerein.current
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 60.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Sparkle(Modifier.size(84.dp), color = s.accent.copy(alpha = 0.9f))
            Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = s.onSurface, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("还没有归档的倒数日", color = s.onSurface, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("在详情里选择「封存」，即可归档到这里", color = s.onSurfaceVariant, fontSize = 13.5.sp)
    }
}

/** 将持久化的图片显示方式映射到 Compose。 */
fun ImageScaleMode.toContentScale(): ContentScale = when (this) {
    ImageScaleMode.CROP -> ContentScale.Crop
    ImageScaleMode.FIT -> ContentScale.Fit
    ImageScaleMode.FILL -> ContentScale.FillBounds
    ImageScaleMode.WIDTH -> ContentScale.FillWidth
    ImageScaleMode.HEIGHT -> ContentScale.FillHeight
    ImageScaleMode.INSIDE -> ContentScale.Inside
}

/** 背景图片；默认裁剪铺满，也支持完整、拉伸、按宽高适配与原图居中。 */
@Composable
fun CoverImage(
    bitmap: androidx.compose.ui.graphics.ImageBitmap?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    scaleMode: ImageScaleMode = ImageScaleMode.CROP
) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = scaleMode.toContentScale()
        )
    }
}
