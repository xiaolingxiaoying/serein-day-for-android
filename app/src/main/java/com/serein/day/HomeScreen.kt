package com.serein.day

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun HomeTab(
    days: List<Countdown>,
    books: List<Book>,
    selectedBookId: String?,
    onBookSelect: (String?) -> Unit,
    onManageBooks: () -> Unit,
    onOpen: (String) -> Unit,
    onOpenArchive: () -> Unit,
    coverStore: CoverStore,
    minimalMode: Boolean,
    sortOrder: SortOrder
) {
    val s = LocalSerein.current
    val active = remember(days) { days.filterNot { it.archived } }
    val visible = remember(active, selectedBookId) {
        if (selectedBookId == null) active else active.filter { it.bookId == selectedBookId }
    }
    val sorted = remember(visible, sortOrder) { sortCountdowns(visible, sortOrder) }
    // 常规模式把第一个事件提为大卡；极简模式整列平铺
    val hero = if (minimalMode) null else sorted.firstOrNull()
    val listDays = remember(sorted, hero) { sorted.filter { it.id != hero?.id } }
    val groups = remember(listDays) {
        listOf(
            "今天" to listDays.filter { remainingDays(it) == 0L },
            "未来" to listDays.filter { remainingDays(it) > 0 },
            "已过去" to listDays.filter { remainingDays(it) < 0 }
        ).filter { it.second.isNotEmpty() }
    }
    val listState = rememberLazyListState()

    Column(Modifier.fillMaxSize()) {
        BrandHeader(
            subtitle = books.find { it.id == selectedBookId }?.name?.let { "倒数本 · $it" }
                ?: "记录每一个重要时刻 · ${LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("M月d日 "))}${weekdayFull(LocalDate.now())}",
            onArchive = onOpenArchive
        )
        BookChipsRow(books, selectedBookId, onBookSelect, onManageBooks)
        if (visible.isEmpty()) {
            EmptyBook(selectedBookId != null)
            return
        }
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 136.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (hero != null) {
                    item(key = "hero", contentType = "hero") {
                        HeroCard(hero, books, coverStore) { onOpen(hero.id) }
                    }
                }
                groups.forEach { (label, itemsInGroup) ->
                    item(key = "header_$label", contentType = "header") {
                        Text(
                            "$label · ${itemsInGroup.size}",
                            color = s.onSurfaceVariant,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                        )
                    }
                    items(itemsInGroup, key = { it.id }, contentType = { "day" }) { day ->
                        if (minimalMode) {
                            MinimalRow(day) { onOpen(day.id) }
                        } else {
                            DayRowCard(day, coverStore, books.find { it.id == day.bookId }?.name ?: "") { onOpen(day.id) }
                        }
                    }
                }
            }
            LazyScrollbar(listState, Modifier.align(Alignment.CenterEnd).padding(end = 4.dp))
        }
    }
}

/** 品牌头部：serein day ✦ 大字标 + 副标题，右侧近黑圆钮（归档）。 */
@Composable
private fun BrandHeader(subtitle: String, onArchive: () -> Unit) {
    val s = LocalSerein.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    "serein day",
                    color = s.onSurface,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.2).sp
                )
                Spacer(Modifier.width(4.dp))
                Sparkle(Modifier.size(16.dp).padding(top = 0.dp))
            }
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = s.onSurfaceVariant, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        InkCircleButton(Icons.Outlined.Inventory2, contentDescription = "归档", onClick = onArchive)
    }
}

/** 顶栏下方的倒数本横滑切换：选中态近黑胶囊，白色未选中。 */
@Composable
private fun BookChipsRow(
    books: List<Book>,
    selectedBookId: String?,
    onSelect: (String?) -> Unit,
    onManage: () -> Unit
) {
    val s = LocalSerein.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BookChip("全部", selectedBookId == null) { onSelect(null) }
        books.forEach { book ->
            BookChip(book.name, selectedBookId == book.id) { onSelect(book.id) }
        }
        BookChip("管理", false, outline = true) { onManage() }
    }
}

@Composable
private fun BookChip(label: String, selected: Boolean, outline: Boolean = false, onClick: () -> Unit) {
    val s = LocalSerein.current
    val bg = animatedColor(
        when {
            selected -> Ink
            outline -> Color.Transparent
            else -> s.container
        }
    )
    val fg = animatedColor(
        when {
            selected -> OnInk
            outline -> s.onSurface
            else -> s.onSurfaceVariant
        }
    )
    Text(
        label,
        color = fg,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier
            .pressScale(0.93f)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = if (outline) s.onSurface.copy(alpha = 0.85f) else s.outlineVariant,
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 7.dp)
    )
}

@Composable
private fun EmptyBook(isBook: Boolean) {
    val s = LocalSerein.current
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 60.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Sparkle(Modifier.size(96.dp), color = s.accent)
            Icon(Icons.Outlined.Spa, contentDescription = null, tint = Ink, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(
            if (isBook) "这本还没有倒数日" else "还没有倒数日",
            color = s.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (isBook) "换个倒数本，或新建一个事件" else "点下方青柠按钮，记下第一个重要时刻",
            color = s.onSurfaceVariant,
            fontSize = 13.5.sp
        )
    }
}

/** 首页置顶的里程碑大卡：近黑底 + 青柠超大数字 + 线稿插画。 */
@Composable
fun HeroCard(day: Countdown, books: List<Book>, coverStore: CoverStore, onOpen: () -> Unit) {
    val s = LocalSerein.current
    val remaining = remainingDays(day)
    val pinned = day.priority == 2
    val cover = rememberCoverBitmap(day.cover, coverStore)
    val bookName = books.find { it.id == day.bookId }?.name ?: "纪念日"
    val heroBg = if (s.isDark) Color(0xFF101318) else Ink

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(0.98f)
            .clip(RoundedCornerShape(30.dp))
            .background(heroBg)
            .clickable(onClick = onOpen)
    ) {
        // 封面图整卡铺底 + 压暗，保证前景数字可读；无封面时右侧出线稿插画
        if (cover != null) {
            CoverImage(cover, Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(heroBg.copy(alpha = 0.68f)))
        }
        Column(Modifier.fillMaxWidth().padding(22.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "距目标日",
                        color = OnInkMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        day.title,
                        color = OnInk,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = OnInkMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(dateText(day), color = OnInkMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (pinned) {
                        PillChip("置顶", s.accent, s.onAccent)
                    } else {
                        Sparkle(Modifier.size(22.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    val latestNote = day.notes.maxByOrNull { it.createdAt }?.text?.take(12)?.ifBlank { null }
                    if (latestNote != null) {
                        Text(
                            latestNote,
                            color = OnInkMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.rotate(-4f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column {
                    Text(statusWord(remaining), color = OnInkMuted, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            kotlin.math.abs(remaining).toString(),
                            color = s.accent,
                            fontSize = 86.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-4).sp,
                            style = TnumStyle
                        )
                        if (remaining != 0L) {
                            Text(
                                " 天",
                                color = OnInk,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                if (cover == null) {
                    HeroIllustration(
                        bookName,
                        modifier = Modifier.size(width = 132.dp, height = 128.dp).padding(bottom = 6.dp),
                        accent = s.accent
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // 目标日青柠胶囊
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(s.accent)
                    .padding(horizontal = 13.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "目标日：${targetDate(day).format(FmtDot)}（${weekdayShort(targetDate(day))}）",
                    color = s.onAccent,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** 小角度旋转（手写批注感）。 */

/** 日程时光列表里的单条倒数卡：白卡 + 马卡龙图标圆 + 右侧大号天数。 */
@Composable
fun DayRowCard(day: Countdown, coverStore: CoverStore? = null, bookName: String = "", onClick: () -> Unit) {
    val s = LocalSerein.current
    val r = remainingDays(day)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(0.97f)
            .clip(RoundedCornerShape(24.dp))
            .background(s.container)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        IconCircle(
            background = bookPastel(bookName),
            icon = bookIcon(bookName),
            tint = Ink,
            size = 48.dp,
            iconSize = 22.dp
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    day.title,
                    color = s.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                NoteBadge(day.notes.size, s.onSurfaceVariant)
            }
            Spacer(Modifier.height(3.dp))
            Text(
                dateText(day),
                color = s.onSurfaceVariant,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (r == 0L) {
            PillChip("就是今天", s.accent, s.onAccent, fontSize = 13.0)
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    kotlin.math.abs(r).toString(),
                    color = if (r > 0) s.primary else s.onSurfaceVariant,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    style = TnumStyle
                )
                Text(
                    "天",
                    color = s.onSurfaceVariant,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(start = 3.dp, bottom = 4.dp)
                )
            }
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = s.outlineVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** 极简模式列表行：名称 + 日期，右侧大号天数，无装饰。 */
@Composable
private fun MinimalRow(day: Countdown, onClick: () -> Unit) {
    val s = LocalSerein.current
    val r = remainingDays(day)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    day.title,
                    color = s.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(dateText(day), color = s.onSurfaceVariant, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (r == 0L) {
                    Text("就是今天", color = s.onAccentStrong, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        if (r > 0) "还有" else "已过去",
                        color = s.onSurfaceVariant,
                        fontSize = 10.5.sp
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            kotlin.math.abs(r).toString(),
                            color = s.primary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            style = TnumStyle
                        )
                        Text(" 天", color = s.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(bottom = 3.dp))
                    }
                }
            }
        }
        HorizontalDivider(color = s.outlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
    }
}

/** 归档页：从首页右上角进入，只读列表。 */
@Composable
fun ArchiveScreen(days: List<Countdown>, books: List<Book>, onBack: () -> Unit, onOpen: (String) -> Unit, coverStore: CoverStore) {
    val archived = remember(days) { days.filter { it.archived }.sortedByDescending { it.date } }
    Column(Modifier.fillMaxSize()) {
        TopBar(title = "归档", leadingIcon = Icons.AutoMirrored.Filled.ArrowBack, onLeading = onBack, large = true)
        if (archived.isEmpty()) {
            EmptyArchive()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Text(
                    "封存时光 · 共 ${archived.size} 条",
                    color = LocalSerein.current.onSurfaceVariant,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                ) }
                items(archived, key = { it.id }, contentType = { "archived" }) { day ->
                    DayRowCard(day, coverStore, books.find { it.id == day.bookId }?.name ?: "") { onOpen(day.id) }
                }
            }
        }
    }
}

/** 极简滚动指示条：滚动时浮现，停止后淡出。 */
@Composable
fun LazyScrollbar(state: LazyListState, modifier: Modifier = Modifier) {
    val s = LocalSerein.current
    val scrolling by remember { derivedStateOf { state.isScrollInProgress } }
    val info by remember {
        derivedStateOf {
            val layout = state.layoutInfo
            val vis = layout.visibleItemsInfo
            if (vis.isEmpty() || layout.totalItemsCount <= vis.size) null
            else {
                val avg = vis.map { it.size }.average().toFloat().coerceAtLeast(1f)
                val viewport = (layout.viewportEndOffset - layout.viewportStartOffset).toFloat()
                val contentHeight = avg * layout.totalItemsCount
                val maxScroll = (contentHeight - viewport).coerceAtLeast(1f)
                val scrolled = (-vis.first().offset + vis.first().index * avg).coerceIn(0f, maxScroll)
                (scrolled / maxScroll).coerceIn(0f, 1f) to (viewport / contentHeight).coerceIn(0.08f, 1f)
            }
        }
    }
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (scrolling && info != null) 0.5f else 0f,
        animationSpec = androidx.compose.animation.core.tween(250),
        label = "scrollbar"
    )
    val current = info
    if (alpha > 0.01f && current != null) {
        val (fraction, thumbFraction) = current
        androidx.compose.foundation.Canvas(modifier.fillMaxHeight().width(4.dp)) {
            val trackHeight = size.height
            val thumbHeight = trackHeight * thumbFraction
            val top = (trackHeight - thumbHeight) * fraction
            drawRoundRect(
                color = s.onSurfaceVariant.copy(alpha = alpha),
                topLeft = Offset(0f, top),
                size = Size(width = size.width, height = thumbHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width / 2)
            )
        }
    }
}
