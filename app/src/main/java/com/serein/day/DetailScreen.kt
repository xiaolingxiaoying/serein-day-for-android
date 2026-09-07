package com.serein.day

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.roundToInt

private data class DetailPendingImage(
    val cover: Boolean,
    val newName: String,
    val oldName: String?,
    val temporary: Boolean
)

@Composable
fun DetailScreen(
    day: Countdown,
    books: List<Book>,
    coverStore: CoverStore,
    minimalMode: Boolean,
    onBack: () -> Unit,
    onPublishNote: (String) -> Unit,
    onEditNote: (String, String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onEditEvent: (Countdown) -> Unit,
    onShare: (Countdown) -> Unit,
    onArchive: (String) -> Unit,
    onRestore: (String) -> Unit,
    onDelete: (String) -> Unit,
    onCoverChange: (String?) -> Unit,
    onCoverScaleChange: (ImageScaleMode) -> Unit,
    onCoverOpacityChange: (Float) -> Unit,
    onWallpaperChange: (String?) -> Unit,
    onWallpaperScaleChange: (ImageScaleMode) -> Unit,
    onWallpaperOpacityChange: (Float) -> Unit,
    onWallpaperDimChange: (Float) -> Unit,
    onAddSubDay: (String, LocalDate) -> Unit,
    onEditSubDay: (String, String, LocalDate) -> Unit,
    onDeleteSubDay: (String) -> Unit
) {
    val s = LocalSerein.current
    var pickingWallpaper by remember { mutableStateOf(false) }
    var pickingCover by remember { mutableStateOf(false) }
    var pendingImage by remember { mutableStateOf<DetailPendingImage?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var selectedSubDay by remember(day.id) { mutableStateOf<SubDay?>(null) }
    var editingSubDay by remember(day.id) { mutableStateOf<SubDay?>(null) }
    var detailsExpanded by remember(day.id) { mutableStateOf(false) }
    val wallpaperBitmap = rememberCoverBitmap(day.wallpaper, coverStore)
    // 壁纸压暗强度：拖动时本地实时生效，松手才落库
    var wallpaperDim by remember(day.id, day.wallpaper) { mutableFloatStateOf(day.wallpaperDim ?: 0.45f) }
    val wallpaperOpacity = day.wallpaperOpacity ?: 1f
    val canEdit = !minimalMode && !day.archived

    BackHandler(enabled = selectedSubDay != null) { selectedSubDay = null }

    if (selectedSubDay != null) {
        SubDayDetailScreen(
            sub = selectedSubDay!!,
            archived = day.archived,
            onBack = { selectedSubDay = null },
            onEdit = if (!day.archived) {
                {
                    editingSubDay = selectedSubDay
                    selectedSubDay = null
                }
            } else null
        )
        return
    }

    Box(Modifier.fillMaxSize().background(s.surface)) {
        // 整页背景壁纸 + 可调压暗遮罩，保证前景文字可读
        if (wallpaperBitmap != null) {
            Box(Modifier.fillMaxSize().alpha(wallpaperOpacity)) {
                Image(
                    bitmap = wallpaperBitmap,
                    contentDescription = "详情页背景",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = day.wallpaperScale.toContentScale()
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = wallpaperDim)))
            }
        }
        Column(Modifier.fillMaxSize()) {
            DetailTopBar(
                title = day.title,
                onWallpaper = wallpaperBitmap != null,
                onBack = onBack,
                onWallpaperClick = { pickingWallpaper = true },
                onCoverClick = if (!day.archived) { { pickingCover = true } } else null,
                onShare = if (!day.archived) { { onShare(day) } } else null,
                onArchive = if (!day.archived) { { onArchive(day.id) } } else null,
                onEdit = if (canEdit) { { onEditEvent(day) } } else null
            )
            if (day.archived) {
                Text(
                    "已归档 · 内容只读",
                    color = if (wallpaperBitmap != null) Color.White.copy(alpha = 0.8f) else s.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 2.dp)
                )
            }
            if (minimalMode || detailsExpanded) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, bottom = if (wallpaperBitmap != null && !day.archived) 110.dp else 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MilestoneCard(day, coverStore, minimal = minimalMode)
                    if (!minimalMode) {
                        DetailToggleButton(expanded = true, onClick = { detailsExpanded = false })
                        ProgressSection(day)
                        SubDaysSection(
                            subs = day.subs,
                            archived = day.archived,
                            onAdd = onAddSubDay,
                            onOpen = { selectedSubDay = it }
                        )
                        NotesSection(
                            day = day,
                            onPublish = onPublishNote,
                            onEditNote = onEditNote,
                            onDeleteNote = onDeleteNote
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    DetailActions(
                        day = day,
                        onRestore = { onRestore(day.id) },
                        onDelete = { confirmDelete = true }
                    )
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    MilestoneCard(day, coverStore, minimal = false)
                    Spacer(Modifier.height(12.dp))
                    DetailToggleButton(expanded = false, onClick = { detailsExpanded = true })
                }
            }
        }

    }

    if (editingSubDay != null) {
        SubDayEditorSheet(
            initial = editingSubDay,
            onConfirm = { title, date ->
                onEditSubDay(editingSubDay!!.id, title, date)
                editingSubDay = null
            },
            onDismiss = { editingSubDay = null }
        )
    }

    if (pickingWallpaper) {
        ImageSourceDialog(
            title = "详情页背景图片来源",
            hasCurrent = day.wallpaper != null,
            onDismiss = { pickingWallpaper = false },
            onImage = { uri ->
                val name = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    coverStore.copyIn(uri, "${day.id}.edit.wallpaper")
                }
                if (name != null) pendingImage = DetailPendingImage(false, name, day.wallpaper, temporary = true)
                name != null
            },
            onEditCurrent = day.wallpaper?.let { current ->
                { pendingImage = DetailPendingImage(false, current, current, temporary = false) }
            },
            onRemove = {
                day.wallpaper?.let { coverStore.remove(it) }
                onWallpaperChange(null)
            }
        )
    }
    if (pickingCover) {
        ImageSourceDialog(
            title = "封面图片来源",
            hasCurrent = day.cover != null,
            onDismiss = { pickingCover = false },
            onImage = { uri ->
                val name = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    coverStore.copyIn(uri, "${day.id}.edit.cover")
                }
                if (name != null) pendingImage = DetailPendingImage(true, name, day.cover, temporary = true)
                name != null
            },
            onEditCurrent = day.cover?.let { current ->
                { pendingImage = DetailPendingImage(true, current, current, temporary = false) }
            },
            onRemove = {
                day.cover?.let { coverStore.remove(it) }
                onCoverChange(null)
            }
        )
    }
    pendingImage?.let { pending ->
        ImageEditSheet(
            title = if (pending.cover) "编辑封面图片" else "编辑详情页背景",
            imageName = pending.newName,
            coverStore = coverStore,
            initialScale = if (pending.cover) day.coverScale else day.wallpaperScale,
            initialOpacity = if (pending.cover) day.coverOpacity ?: 1f else day.wallpaperOpacity ?: 1f,
            targetAspectRatio = if (pending.cover) 4f / 3f else 9f / 16f,
            targetLabel = if (pending.cover) "倒数日封面" else "详情页背景",
            cropOutputBase = "${day.id}.edit.crop.${if (pending.cover) "cover" else "wallpaper"}",
            onSave = { scale, opacity, croppedName ->
                if (croppedName != null) coverStore.remove(pending.newName)
                if (pending.temporary && pending.oldName != null) coverStore.remove(pending.oldName)
                val sourceName = croppedName ?: pending.newName
                val needsFinalName = pending.temporary || croppedName != null
                val finalName = if (!needsFinalName) {
                    sourceName
                } else if (pending.cover) {
                    coverStore.renameDraft(day.id, sourceName)
                } else {
                    coverStore.renameDraft("${day.id}.w", sourceName)
                }
                if (pending.cover) { onCoverScaleChange(scale); onCoverOpacityChange(opacity); onCoverChange(finalName) }
                else { onWallpaperScaleChange(scale); onWallpaperOpacityChange(opacity); onWallpaperChange(finalName) }
                pendingImage = null
            },
            onCancel = {
                if (pending.temporary) coverStore.remove(pending.newName)
                // cropTo writes a second temporary file. It must be removed as well
                // when the user backs out before the result is attached to the event.
                coverStore.removeByBase("${day.id}.edit.crop.${if (pending.cover) "cover" else "wallpaper"}")
                pendingImage = null
            }
        )
    }
    if (confirmDelete) {
        IosAlertDialog(
            title = "彻底删除「${day.title}」？",
            message = "删除后无法恢复；小记、封面与背景图都会一并移除。",
            confirmText = "彻底删除",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                onDelete(day.id)
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

/** 详情展开控制：收起时让主卡保持聚焦，展开后显示完整信息。 */
@Composable
private fun DetailToggleButton(expanded: Boolean, onClick: () -> Unit) {
    val s = LocalSerein.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(s.container)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            if (expanded) "收起详情" else "展开详情",
            color = s.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Icon(
            if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = s.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SubDayDetailScreen(
    sub: SubDay,
    archived: Boolean,
    onBack: () -> Unit,
    onEdit: (() -> Unit)?
) {
    val s = LocalSerein.current
    val remaining = subDayRemaining(sub.date)

    Column(Modifier.fillMaxSize().background(s.surface)) {
        TopBar(
            title = sub.title,
            leadingIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onLeading = onBack,
            trailingText = if (!archived) "编辑" else null,
            onTrailing = onEdit
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(if (s.isDark) Color(0xFF101318) else Ink)
                    .then(if (s.isDark) Modifier.border(1.dp, s.outlineVariant, RoundedCornerShape(30.dp)) else Modifier)
                    .padding(22.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("距目标日", color = OnInkMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            sub.title,
                            color = OnInk,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Sparkle(Modifier.size(22.dp), color = s.accent)
                }
                Spacer(Modifier.height(18.dp))
                Text(subDayStatus(sub.date), color = OnInkMuted, fontSize = 14.sp)
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
                        Text(" 天", color = OnInk, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 16.dp, start = 4.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(s.accent)
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "目标日：${sub.date.format(FmtDot)}（${weekdayShort(sub.date)}）",
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
}

/** 详情顶栏：事件名做标题；有壁纸时前景改白色保证可读；右上角为青柠圆形编辑钮。 */
@Composable
private fun DetailTopBar(
    title: String,
    onWallpaper: Boolean,
    onBack: () -> Unit,
    onWallpaperClick: () -> Unit,
    onCoverClick: (() -> Unit)?,
    onShare: (() -> Unit)?,
    onArchive: (() -> Unit)?,
    onEdit: (() -> Unit)?
) {
    val s = LocalSerein.current
    val fg = if (onWallpaper) Color.White else s.onSurface
    var showMoreMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = fg,
                modifier = Modifier.size(23.dp).clickable { onBack() }
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            title,
            color = fg,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (onWallpaper) Color.White.copy(alpha = 0.85f) else s.container)
                    .clickable { showMoreMenu = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "更多操作",
                    tint = if (onWallpaper) s.onSurface else s.primary,
                    modifier = Modifier.size(21.dp)
                )
            }
            DropdownMenu(
                expanded = showMoreMenu,
                onDismissRequest = { showMoreMenu = false }
            ) {
                onShare?.let { share ->
                    DropdownMenuItem(
                        text = { Text("分享卡片") },
                        leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                        onClick = {
                            showMoreMenu = false
                            share()
                        }
                    )
                }
                onArchive?.let { archive ->
                    DropdownMenuItem(
                        text = { Text("封存") },
                        leadingIcon = { Icon(Icons.Outlined.Inventory2, contentDescription = null) },
                        onClick = {
                            showMoreMenu = false
                            archive()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("更换详情页背景") },
                    leadingIcon = { Icon(Icons.Outlined.Wallpaper, contentDescription = null) },
                    onClick = {
                        showMoreMenu = false
                        onWallpaperClick()
                    }
                )
                onCoverClick?.let { cover ->
                    DropdownMenuItem(
                        text = { Text("更换封面图片") },
                        leadingIcon = { Icon(Icons.Outlined.Wallpaper, contentDescription = null) },
                        onClick = { showMoreMenu = false; cover() }
                    )
                }
            }
        }
        if (onEdit != null) {
            Spacer(Modifier.width(10.dp))
            // 青柠圆形编辑钮（原长条编辑按钮的图标 + 配色）
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .pressScale(0.9f)
                    .clip(CircleShape)
                    .background(s.accent)
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "编辑倒数日", tint = s.onAccent, modifier = Modifier.size(17.dp))
            }
        }
    }
}

/** 详情操作区：分享 / 封存（编辑入口在右上角圆形青柠钮）。 */
@Composable
private fun DetailActions(
    day: Countdown,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val s = LocalSerein.current
    if (day.archived) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Unarchive,
                text = "恢复",
                onClick = onRestore
            )
            SecondaryButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Delete,
                text = "彻底删除",
                contentColor = s.error,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun MilestoneCard(day: Countdown, coverStore: CoverStore, minimal: Boolean) {
    val s = LocalSerein.current
    val remaining = remainingDays(day)
    val heroBg = if (s.isDark) Color(0xFF101318) else Ink
    val heroShape = RoundedCornerShape(30.dp)
    val heroBorder = if (s.isDark) Modifier.border(1.dp, s.outlineVariant, heroShape) else Modifier

    if (minimal) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(heroShape)
                .background(heroBg)
                .then(heroBorder)
                .padding(22.dp)
        ) {
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text("${dateText(day)} · ${weekdayFull(targetDate(day))}", color = OnInkMuted, fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(statusWord(remaining), color = OnInkMuted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 10.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    abs(remaining).toString(),
                    color = s.accent,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp,
                    style = TnumStyle
                )
                if (remaining != 0L) {
                    Text(" 天", color = OnInk, fontSize = 13.sp, modifier = Modifier.padding(bottom = 10.dp))
                }
            }
        }
        return
    }

    HeroCard(day, coverStore)
}

/** 独立的里程碑进度区：放在小倒数日和小记之后，避免挤在主卡片里。 */
@Composable
private fun ProgressSection(day: Countdown) {
    val s = LocalSerein.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(s.container)
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (day.repeatYearly) "每逢此日 · 已陪你走过 ${yearRound(day)} 天" else "已过 ${elapsedSince(day)} 天（起始 ${milestoneStart(day).format(FmtDot)}）",
                color = s.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            Text("${(progressOf(day) * 100).roundToInt()}% 完成", color = s.accent, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        // 进度条入场从 0 弹簧展开，与进度环一致
        var progressPlayed by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { progressPlayed = true }
        val fillProgress by animateFloatAsState(
            targetValue = if (progressPlayed) progressOf(day).coerceIn(0.02f, 1f) else 0f,
            animationSpec = spring(dampingRatio = 1f, stiffness = 160f),
            label = "detailProgressFill"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(s.highest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fillProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(s.accent)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            val start = if (day.repeatYearly) previousOccurrence(day, targetDate(day)) else milestoneStart(day)
            Text("起始 ${start.format(FmtDot)}", color = s.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("目标 ${targetDate(day).format(FmtDot)}", color = s.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

private fun yearRound(day: Countdown): Long =
    java.time.temporal.ChronoUnit.DAYS.between(previousOccurrence(day, targetDate(day)), LocalDate.now()).coerceAtLeast(0)

fun milestoneStart(day: Countdown): LocalDate = day.createdAt ?: day.date.minusDays(90)

/** 小倒数日倒数文案：还有 N 天 / 就是今天 / 已过 N 天。 */
private fun subDayStatus(date: LocalDate): String {
    val r = subDayRemaining(date)
    return when {
        r > 0 -> "还有 $r 天"
        r == 0L -> "就是今天"
        else -> "已过 ${-r} 天"
    }
}

private fun subDayRemaining(date: LocalDate): Long =
    java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date)

/** 小倒数日区：给主日子挂子节点（如报名、打印准考证），可增删改。 */
@Composable
private fun SubDaysSection(
    subs: List<SubDay>,
    archived: Boolean,
    onAdd: (String, LocalDate) -> Unit,
    onOpen: (SubDay) -> Unit
) {
    val s = LocalSerein.current
    var adding by remember { mutableStateOf(false) }
    val sorted = remember(subs) { subs.sortedBy { it.date } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("小倒数日", color = s.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            sorted.forEach { sub ->
                val remaining = subDayRemaining(sub.date)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(s.container)
                        .clickable { onOpen(sub) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(sub.title, color = s.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (remaining == 0L) {
                        PillChip("就是今天", s.accent, s.onAccent, fontSize = 13.0)
                    } else {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                kotlin.math.abs(remaining).toString(),
                                color = if (remaining > 0) s.primary else s.onSurfaceVariant,
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
                }
            }
        }
        if (!archived) {
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { adding = true }
                    .padding(vertical = 10.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = s.onAccentStrong, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("添加小倒数日", color = s.onAccentStrong, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (adding) {
        SubDayEditorSheet(
            initial = null,
            onConfirm = { title, date ->
                onAdd(title, date)
                adding = false
            },
            onDismiss = { adding = false }
        )
    }
}

/** 小倒数日编辑弹层：标题 + 日期（滚轮选择）。 */
@Composable
private fun SubDayEditorSheet(
    initial: SubDay?,
    onConfirm: (String, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalSerein.current
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val weekday = remember(date) { weekdayShort(date) }

    SereinSheet(
        title = if (initial == null) "添加小倒数日" else "编辑小倒数日",
        onDismiss = onDismiss,
        trailingText = "保存",
        onTrailing = { if (title.isNotBlank()) onConfirm(title.trim(), date) }
    ) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box {
                if (title.isEmpty()) {
                    Text("小倒数日名称（如：报名）", color = s.outlineVariant, fontSize = 16.sp)
                }
                BasicTextField(
                    value = title,
                    onValueChange = { if (it.length <= 24) title = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = s.onSurface),
                    cursorBrush = SolidColor(s.onAccentStrong),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(s.high)
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("目标日期", color = s.onSurfaceVariant, fontSize = 12.sp)
                    Spacer(Modifier.height(2.dp))
                    Text("${date.format(FmtCn)} · $weekday", color = s.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                PillChip(subDayStatus(date), s.accent, s.onAccent, fontSize = 12.0)
            }
            Spacer(Modifier.height(4.dp))
        }
    }

    if (showDatePicker) {
        WheelDatePickerSheet(
            initial = date,
            title = "选择小倒数日日期",
            onDismiss = { showDatePicker = false },
            onConfirm = {
                date = it
                showDatePicker = false
            }
        )
    }
}

/** 小记区：随时可编辑、可删除，最新在上。 */
@Composable
private fun NotesSection(
    day: Countdown,
    onPublish: (String) -> Unit,
    onEditNote: (String, String) -> Unit,
    onDeleteNote: (String) -> Unit
) {
    val s = LocalSerein.current
    var draft by remember(day.id) { mutableStateOf("") }
    var editingId by remember(day.id) { mutableStateOf<String?>(null) }

    val notes = remember(day.notes) { day.notes.sortedByDescending { it.createdAt } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(s.container)
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("小记", color = s.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            notes.forEach { note ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(s.container)
                        .padding(16.dp)
                ) {
                    if (editingId == note.id) {
                        NoteEditRow(
                            initial = note.text,
                            onSave = { text ->
                                onEditNote(note.id, text)
                                editingId = null
                            },
                            onCancel = { editingId = null }
                        )
                    } else {
                        NoteRow(
                            note = note,
                            archived = day.archived,
                            onEdit = { editingId = note.id },
                            onDelete = { onDeleteNote(note.id) }
                        )
                    }
                }
            }
        }
        if (!day.archived) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(s.high)
                    .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 200) draft = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = s.onSurface),
                    cursorBrush = SolidColor(s.onAccentStrong),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (draft.isNotBlank()) { onPublish(draft); draft = "" }
                    }),
                    modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                    decorationBox = { inner ->
                        Box {
                            if (draft.isEmpty()) Text("写一条小记…", color = s.onSurfaceVariant, fontSize = 14.sp)
                            inner()
                        }
                    }
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (draft.isNotBlank()) s.accent else s.highest)
                        .clickable(enabled = draft.isNotBlank()) { onPublish(draft); draft = "" }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = if (draft.isNotBlank()) s.onAccent else s.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        "发布",
                        color = if (draft.isNotBlank()) s.onAccent else s.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteRow(note: Note, archived: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val s = LocalSerein.current
    var meta = "发布 ${note.createdAt.format(FmtNote)}"
    note.updatedAt?.let { meta += " · 修改于 ${it.format(FmtNote)}" }
    Column(Modifier.fillMaxWidth()) {
        Text(note.text, color = s.onSurface, fontSize = 14.sp, lineHeight = 21.sp)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(meta, color = s.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.weight(1f))
            if (!archived) {
                Text(
                    "编辑",
                    color = s.onAccentStrong,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onEdit)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
                Text(
                    "删除",
                    color = s.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onDelete)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun NoteEditRow(initial: String, onSave: (String) -> Unit, onCancel: () -> Unit) {
    val s = LocalSerein.current
    var text by remember { mutableStateOf(initial) }
    Column(Modifier.fillMaxWidth()) {
        BasicTextField(
            value = text,
            onValueChange = { if (it.length <= 200) text = it },
            textStyle = TextStyle(fontSize = 14.sp, color = s.onSurface, lineHeight = 21.sp),
            cursorBrush = SolidColor(s.onAccentStrong),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(s.accent)
                    .clickable(enabled = text.isNotBlank()) { onSave(text) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("保存", color = s.onAccent, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(s.highest)
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("取消", color = s.onSurfaceVariant, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** 近黑次级胶囊按钮（分享 / 封存 / 恢复）。 */
@Composable
private fun SecondaryButton(
    modifier: Modifier = Modifier,
    contentColor: Color = OnInk,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    val s = LocalSerein.current
    Row(
        modifier = modifier
            .pressScale(0.97f)
            .clip(RoundedCornerShape(50))
            .background(Ink)
            .then(if (s.isDark) Modifier.border(1.dp, s.outlineVariant, RoundedCornerShape(50)) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text(text, color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}
