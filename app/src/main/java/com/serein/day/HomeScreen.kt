package com.serein.day

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
    coverStore: CoverStore
) {
    val active = remember(days) { days.filterNot { it.archived } }
    val visible = remember(active, selectedBookId) {
        if (selectedBookId == null) active else active.filter { it.bookId == selectedBookId }
    }
    val hero = remember(visible) { visible.sortedWith(countdownComparator()).firstOrNull() }
    val listDays = remember(visible, hero) { visible.filter { it.id != hero?.id }.sortedWith(countdownComparator()) }
    val upcoming30 = remember(active) { active.count { remainingDays(it) in 0..30 } }
    val listState = rememberLazyListState()
    val s = LocalSerein.current

    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = books.find { it.id == selectedBookId }?.name ?: "Home",
            leadingSlot = { BookMenuButton(books, selectedBookId, onBookSelect, onManageBooks) }
        )
        if (visible.isEmpty()) {
            EmptyBook(selectedBookId != null)
            return
        }
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "hero", contentType = "hero") {
                    hero?.let { HeroCard(it, books, coverStore) { onOpen(it.id) } }
                }
                item(key = "section", contentType = "section") {
                    SectionBar("日程时光", "共 ${visible.size} 个节点")
                }
                if (upcoming30 > 0) {
                    item(key = "upcoming", contentType = "upcoming") {
                        UpcomingCard(count = upcoming30)
                    }
                }
                items(listDays, key = { it.id }, contentType = { "day" }) { day ->
                    DayRowCard(day, coverStore, books.find { it.id == day.bookId }?.name ?: "") { onOpen(day.id) }
                }
                item(key = "quote", contentType = "quote") { QuoteCard() }
            }
            LazyScrollbar(listState, Modifier.align(Alignment.CenterEnd).padding(end = 2.dp))
            Box(Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 0.dp))
        }
    }
}

/** Home 左侧的倒数本切换菜单。 */
@Composable
private fun BookMenuButton(
    books: List<Book>,
    selectedBookId: String?,
    onSelect: (String?) -> Unit,
    onManage: () -> Unit
) {
    val s = LocalSerein.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        Icon(
            Icons.Filled.Menu,
            contentDescription = "切换倒数本",
            tint = s.onSurface,
            modifier = Modifier
                .size(42.dp)
                .padding(8.dp)
                .clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("全部倒数本", fontWeight = if (selectedBookId == null) FontWeight.SemiBold else FontWeight.Normal) },
                leadingIcon = { if (selectedBookId == null) Icon(Icons.Filled.Check, null, tint = s.primary, modifier = Modifier.size(18.dp)) },
                onClick = { expanded = false; onSelect(null) }
            )
            books.forEach { book ->
                DropdownMenuItem(
                    text = { Text(book.name, fontWeight = if (selectedBookId == book.id) FontWeight.SemiBold else FontWeight.Normal) },
                    leadingIcon = { if (selectedBookId == book.id) Icon(Icons.Filled.Check, null, tint = s.primary, modifier = Modifier.size(18.dp)) },
                    onClick = { expanded = false; onSelect(book.id) }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("管理倒数本", color = s.primary) },
                onClick = { expanded = false; onManage() }
            )
        }
    }
}

@Composable
private fun EmptyBook(isBook: Boolean) {
    val s = LocalSerein.current
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 60.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconCircle(s.container, Icons.Outlined.Spa, s.onAccentStrong, size = 84.dp, iconSize = 36.dp)
        Spacer(Modifier.height(18.dp))
        Text(
            if (isBook) "这本还没有倒数日" else "还没有倒数日",
            color = s.onSurface,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (isBook) "换个倒数本，或新建一个事件" else "点右下角 +，温柔记下第一个重要时刻",
            color = s.onSurfaceVariant,
            fontSize = 13.5.sp
        )
    }
}

/** 首页置顶的里程碑大卡。 */
@Composable
fun HeroCard(day: Countdown, books: List<Book>, coverStore: CoverStore, onOpen: () -> Unit) {
    val s = LocalSerein.current
    val remaining = remainingDays(day)
    val pinned = day.priority == 2
    val cover = rememberCoverBitmap(day.cover, coverStore)
    val bookName = books.find { it.id == day.bookId }?.name ?: "纪念日"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(s.container)
            .clickable(onClick = onOpen)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(heroBrush(bookName))
        ) {
            CoverImage(cover, Modifier.fillMaxSize())
            if (pinned) {
                Text(
                    "特别期待",
                    color = s.onAccentStrong,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xB32A322D))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Outlined.EditNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Text(
                    day.notes.maxByOrNull { it.createdAt }?.text?.take(18)?.ifBlank { null }
                        ?: "${statusWord(remaining)} ${kotlin.math.abs(remaining)} 天",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                day.title,
                color = s.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            HeroStatusChip(remaining)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    kotlin.math.abs(remaining).toString(),
                    color = s.primary,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-2).sp
                )
                if (remaining != 0L) {
                    Text(" 天", color = s.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            ProgressArc(
                progress = progressOf(day),
                track = s.highest,
                arcColor = s.primary,
                textColor = s.onSurface
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = s.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "距 ${dateText(day)} · ${weekdayFull(targetDate(day))}",
                color = s.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text("查看详情", color = s.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = s.onSurface, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun HeroStatusChip(remaining: Long) {
    val s = LocalSerein.current
    when {
        remaining > 0 -> PillChip("还有", s.accent, s.onAccentStrong)
        remaining == 0L -> PillChip("就是今天", Peach, OnPeach)
        else -> PillChip("已过去", s.highest, s.onSurfaceVariant)
    }
}

@Composable
private fun UpcomingCard(count: Int) {
    val s = LocalSerein.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(s.high)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconCircle(Color.White, Icons.Outlined.CalendarMonth, s.primary)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("即将到来", color = s.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                PillChip("近期待办", s.accent, s.onAccentStrong)
            }
            Spacer(Modifier.height(4.dp))
            Text("有 $count 个重要日子将在 30 天内发生", color = s.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}

/** 日程时光列表里的单条倒数卡；生日本使用蜜棕庆祝色。 */
@Composable
fun DayRowCard(day: Countdown, coverStore: CoverStore? = null, bookName: String = "", onClick: () -> Unit) {
    val s = LocalSerein.current
    val honey = bookName == "生日"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (honey) Honey else s.container)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconCircle(
            background = if (honey) Peach else Color.White,
            icon = Icons.Outlined.FavoriteBorder,
            tint = if (honey) Color.White else s.primary
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    day.title,
                    color = if (honey) Color.White else s.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (day.repeatYearly) {
                    PillChip(
                        if (day.lunar) "农历 · 每年" else "每年",
                        if (honey) Peach else s.highest,
                        if (honey) OnPeach else s.onSurfaceVariant
                    )
                } else if (day.lunar) {
                    PillChip("农历", if (honey) Peach else s.highest, if (honey) OnPeach else s.onSurfaceVariant)
                }
                NoteBadge(day.notes.size, if (honey) OnHoney else s.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                remainingSubtitle(day),
                color = if (honey) OnHoney else s.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = if (honey) OnHoney else s.outlineVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

fun remainingSubtitle(day: Countdown): String {
    val r = remainingDays(day)
    return when {
        r > 0 -> "还有 $r 天 · ${dateText(day)}"
        r == 0L -> "就是今天 · ${dateText(day)}"
        else -> "已过去 ${-r} 天 · ${dateText(day)}"
    }
}

@Composable
private fun QuoteCard() {
    val s = LocalSerein.current
    val quote = Quotes[LocalDate.now().dayOfYear % Quotes.size]
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(s.container)
            .padding(16.dp)
    ) {
        Text("日辰心绪", color = s.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("“$quote”", color = s.onSurfaceVariant, fontSize = 13.5.sp, lineHeight = 21.sp)
    }
}

@Composable
fun ArchiveTab(days: List<Countdown>, books: List<Book>, onOpen: (String) -> Unit, coverStore: CoverStore) {
    val archived = remember(days) { days.filter { it.archived }.sortedByDescending { it.date } }
    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Archive", leadingIcon = Icons.Filled.Menu)
        if (archived.isEmpty()) {
            EmptyArchive()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { SectionBar("封存时光", "共 ${archived.size} 条记录") }
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
        animationSpec = tween(250),
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
