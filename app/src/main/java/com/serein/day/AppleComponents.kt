package com.serein.day

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * iOS 风格弹窗：居中标题与正文，底部按钮以细线分隔，破坏性操作用红色强调。
 */
@Composable
fun IosAlertDialog(
    title: String,
    message: String? = null,
    confirmText: String,
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    dismissText: String? = "取消",
    onDismiss: (() -> Unit)? = null
) {
    val s = LocalSerein.current
    val reduced = rememberReducedMotion()
    // iOS 弹窗式入场：缩放与淡入一起到位，读作"一块材质浮现"而非普通淡入
    val appearScale = remember { Animatable(if (reduced) 1f else 1.13f) }
    val appearAlpha = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        launch { appearScale.animateTo(1f, spring(dampingRatio = 1f, stiffness = 460f)) }
        launch { appearAlpha.animateTo(1f, tween(150)) }
    }
    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = appearScale.value
                    scaleY = appearScale.value
                    alpha = appearAlpha.value
                }
                .clip(RoundedCornerShape(18.dp))
                .background(if (s.isDark) s.high else IosWhite)
        ) {
            Spacer(Modifier.height(20.dp))
            Text(
                title,
                color = s.onSurface,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
            )
            if (message != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    message,
                    color = s.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
                )
            }
            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = s.outlineVariant.copy(alpha = 0.6f), thickness = 0.5.dp)
            Row(Modifier.fillMaxWidth().height(46.dp)) {
                if (dismissText != null && onDismiss != null) {
                    IosDialogButton(dismissText, s.onSurface, FontWeight.Normal) { onDismiss() }
                    Box(
                        Modifier.fillMaxHeight().width(0.5.dp)
                            .background(s.outlineVariant.copy(alpha = 0.6f))
                    )
                }
                IosDialogButton(
                    confirmText,
                    if (destructive) s.error else s.primary,
                    FontWeight.SemiBold
                ) { onConfirm() }
            }
        }
    }
}

/** iOS 弹窗按钮：按下整块泛灰（非 Android 涟漪），抬起即触发。 */
@Composable
private fun RowScope.IosDialogButton(text: String, color: Color, fontWeight: FontWeight, onClick: () -> Unit) {
    val s = LocalSerein.current
    val pressed = remember { mutableStateOf(false) }
    val highlight by animateColorAsState(
        targetValue = if (pressed.value) s.onSurface.copy(alpha = 0.08f) else Color.Transparent,
        animationSpec = tween(90),
        label = "dialogPress"
    )
    Box(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(highlight)
            .clickable { onClick() }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed.value = true
                    waitForUpOrCancellation()
                    pressed.value = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, fontSize = 15.5.sp, fontWeight = fontWeight)
    }
}

/** 弹层、分段滑块等使用的近似纯白（深色下走 palette 层级色）。 */
val IosWhite = androidx.compose.ui.graphics.Color(0xFFFDFDFC)

/**
 * iOS 风格底部弹层：顶部拖拽把手 + 居中标题，可选左右文字按钮。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SereinSheet(
    title: String,
    onDismiss: () -> Unit,
    leadingText: String? = null,
    onLeading: (() -> Unit)? = null,
    trailingText: String? = null,
    onTrailing: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val s = LocalSerein.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = if (s.isDark) s.container else Color.White,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .width(38.dp)
                        .height(4.5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(s.outlineVariant)
                )
            }
        }
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)) {
            if (leadingText != null && onLeading != null) {
                Text(
                    leadingText,
                    color = s.onSurfaceVariant,
                    fontSize = 14.5.sp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(50))
                        .clickable { onLeading() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Text(
                title,
                color = s.onSurface,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
            if (trailingText != null && onTrailing != null) {
                Text(
                    trailingText,
                    color = s.primary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(50))
                        .clickable { onTrailing() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        content()
        Spacer(Modifier.navigationBarsPadding())
        Spacer(Modifier.height(18.dp))
        // 键盘弹出时把整块弹层顶到键盘上方：否则键盘会盖住输入框与保存按钮，
        // 用户点键盘外区域收键盘时误触遮罩，整个弹层连同已输入内容一起被关掉
        Spacer(Modifier.fillMaxWidth().imePadding())
    }
}

/**
 * 分段控件：整块圆角轨道，青柠滑块在选中项之间平移动画（选中项黑字）。
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit
) {
    val s = LocalSerein.current
    BoxWithConstraints(
        modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(s.highest.copy(alpha = if (s.isDark) 0.9f else 1f))
            .padding(2.dp)
    ) {
        val cellWidth = maxWidth / options.size
        val thumbX by androidx.compose.animation.core.animateDpAsState(
            targetValue = cellWidth * selected,
            animationSpec = spring(dampingRatio = 1f, stiffness = 400f),
            label = "segmentThumb"
        )
        Box(
            Modifier
                .offset(x = thumbX)
                .width(cellWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(s.accent)
        )
        Row(Modifier.fillMaxWidth().height(34.dp)) {
            options.forEachIndexed { index, option ->
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        option,
                        color = if (index == selected) s.onAccent else s.onSurfaceVariant,
                        fontSize = 13.5.sp,
                        fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * 单列滚轮选择器：吸附滚动居中，越靠边越淡，静止时回报选中索引。
 */
@Composable
fun WheelColumn(
    items: List<String>,
    startIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 42.dp,
    visibleCount: Int = 5
) {
    val s = LocalSerein.current
    val density = LocalDensity.current
    val itemPx = with(density) { itemHeight.toPx() }
    val state = rememberLazyListState(startIndex.coerceIn(0, items.lastIndex))
    val fling = rememberSnapFlingBehavior(lazyListState = state)

    // 列表缩短（如月份变化后天数变少）时把位置夹回有效范围
    LaunchedEffect(items.size) {
        if (state.firstVisibleItemIndex > items.lastIndex) {
            state.scrollToItem(items.lastIndex)
            onSelected(items.lastIndex)
        }
    }
    // 静止时回报居中的索引
    LaunchedEffect(items.size) {
        snapshotFlow { state.firstVisibleItemIndex to state.isScrollInProgress }
            .filter { !it.second }
            .map { it.first.coerceIn(0, items.lastIndex) }
            .distinctUntilChanged()
            .collect { onSelected(it) }
    }
    // 滚轮划过每一格时轻点一下（可在设置中关闭）。按 ~50ms 节流：快滑/飞滑瞬间不再逐格触感，
    // 避免触感回调密集落在主线程形成卡段。
    val view = LocalView.current
    val hapticsOn = LocalHapticsEnabled.current
    LaunchedEffect(hapticsOn) {
        if (!hapticsOn) return@LaunchedEffect
        var lastTick = 0L
        snapshotFlow { state.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect {
                val now = android.os.SystemClock.uptimeMillis()
                if (now - lastTick >= 50) {
                    lastTick = now
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
            }
    }

    Box(modifier.height(itemHeight * visibleCount)) {
        // 中部选择带
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(if (s.isDark) s.high else s.container.copy(alpha = 0.55f))
        )
        LazyColumn(
            state = state,
            flingBehavior = fling,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                vertical = itemHeight * ((visibleCount - 1) / 2)
            )
        ) {
            items(items.size, key = { it }) { index ->
                val distance by remember(index, items.size) {
                    derivedStateOf {
                        val info = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                        if (info == null) 3f
                        else {
                            val viewportCenter = (state.layoutInfo.viewportStartOffset + state.layoutInfo.viewportEndOffset) / 2f
                            kotlin.math.abs(info.offset + info.size / 2f - viewportCenter) / itemPx
                        }
                    }
                }
                val alpha = (1f - distance * 0.42f).coerceIn(0.2f, 1f)
                val selectedNow = distance < 0.5f
                Box(
                    Modifier.fillMaxWidth().height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        items[index],
                        color = if (selectedNow) s.onSurface else s.onSurfaceVariant.copy(alpha = alpha),
                        fontSize = if (selectedNow) 17.sp else 15.sp,
                        fontWeight = if (selectedNow) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * iOS 风格滚轮日期选择弹层：年 / 月 / 日三列，月份天数随选择自动收拢。
 */
@Composable
fun WheelDatePickerSheet(
    initial: LocalDate,
    title: String = "选择日期",
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val s = LocalSerein.current
    var year by remember { mutableIntStateOf(initial.year) }
    var month by remember { mutableIntStateOf(initial.monthValue) }
    var day by remember { mutableIntStateOf(initial.dayOfMonth) }

    val years = remember { (1900..2100).map { "$it 年" } }
    val months = remember { (1..12).map { "$it 月" } }
    val maxDay = remember(year, month) { LocalDate.of(year, month, 1).lengthOfMonth() }
    val days = remember(maxDay) { (1..maxDay).map { "$it 日" } }

    // 月份变化导致当天数超出时收拢
    LaunchedEffect(maxDay) { if (day > maxDay) day = maxDay }

    // 不用 ModalBottomSheet：滚轮列需要纵向手势，而 BottomSheet 自身也拦截纵向拖拽，
    // 两条手势管道抢同一根手指造成滚轮「卡段」。改为 Dialog 实现同款底部弹层外观，
    // 语法上彻底去掉 ModalBottomSheet 的嵌套滚动/拖拽机制。
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(Modifier.fillMaxSize()) {
            // 遮罩点击关闭（ModalBottomSheet 同款行为）
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable { onDismiss() }
            )
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .background(if (s.isDark) s.container else Color.White)
                    // 消费面板空白处点击，避免点击落到底下遮罩直接关闭（保留遮罩点击关场）
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .navigationBarsPadding()
                    .padding(top = 14.dp, bottom = 18.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "取消",
                        color = s.onSurfaceVariant,
                        fontSize = 14.5.sp,
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onDismiss() }.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(title, color = s.onSurface, fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "确定",
                        color = s.primary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onConfirm(LocalDate.of(year, month, day.coerceAtMost(maxDay))) }.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                val weekday = remember(year, month, day, maxDay) {
                    val safe = day.coerceAtMost(maxDay)
                    weekdayFull(LocalDate.of(year, month, safe))
                }
                Text(
                    "${year}年${month}月${day.coerceAtMost(maxDay)}日 · $weekday",
                    color = s.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.weight(1.2f)) {
                        WheelColumn(
                            items = years,
                            startIndex = year - 1900,
                            onSelected = { year = 1900 + it }
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        WheelColumn(
                            items = months,
                            startIndex = month - 1,
                            onSelected = { month = it + 1 }
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        WheelColumn(
                            items = days,
                            startIndex = (day - 1).coerceIn(0, maxDay - 1),
                            onSelected = { day = it + 1 }
                        )
                    }
                }
            }
        }
    }
}

/** 颜色渐变辅助（chips / 分段等状态切换），弹簧驱动以便打断续接。 */
@Composable
fun animatedColor(target: Color): Color {
    return animateColorAsState(target, spring(dampingRatio = 1f, stiffness = 300f), label = "colorAnim").value
}
