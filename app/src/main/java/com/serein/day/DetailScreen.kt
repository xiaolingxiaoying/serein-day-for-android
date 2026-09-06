package com.serein.day

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun DetailScreen(
    day: Countdown,
    books: List<Book>,
    noteOrder: NoteOrder,
    coverStore: CoverStore,
    onBack: () -> Unit,
    onToggleNoteOrder: () -> Unit,
    onPublishNote: (String) -> Unit,
    onEditNote: (String, String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onEditEvent: (Countdown) -> Unit,
    onShare: (Countdown) -> Unit,
    onArchive: (String) -> Unit,
    onRestore: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val s = LocalSerein.current
    var expanded by rememberSaveable(day.id) { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmTwoStepDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(s.surface)) {
        BackTopBar(
            title = if (expanded) "Countdown Detail" else "",
            onBack = onBack,
            trailingText = if (!expanded && !day.archived) "编辑" else null,
            onTrailing = { expanded = true }
        )
        if (day.archived) {
            Text(
                "已归档 · 内容只读",
                color = s.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 2.dp)
            )
        }
        if (!expanded) {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                MilestoneCard(day, books, coverStore)
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MilestoneCard(day, books, coverStore)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReminderStatusCard(day)
                    BookCard(day, books)
                    CreatedCard(day)
                }
                NotesSection(
                    day = day,
                    noteOrder = noteOrder,
                    onToggleOrder = onToggleNoteOrder,
                    onPublish = onPublishNote,
                    onEditNote = onEditNote,
                    onDeleteNote = onDeleteNote
                )
                Spacer(Modifier.height(6.dp))
                if (!day.archived) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(s.primary)
                            .clickable { onEditEvent(day) }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("编辑资料", color = s.onPrimary, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SecondaryButton(
                            modifier = Modifier.weight(1f),
                            background = s.secondaryContainer,
                            contentColor = s.onSecondaryContainer,
                            icon = Icons.Outlined.Share,
                            text = "分享卡片",
                            onClick = { onShare(day) }
                        )
                        SecondaryButton(
                            modifier = Modifier.weight(1f),
                            background = if (s.isDark) s.container else Color.White,
                            contentColor = s.onSurface,
                            icon = Icons.Outlined.Inventory2,
                            text = "封存",
                            onClick = { onArchive(day.id) }
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SecondaryButton(
                            modifier = Modifier.weight(1f),
                            background = s.secondaryContainer,
                            contentColor = s.onSecondaryContainer,
                            icon = Icons.Outlined.Unarchive,
                            text = "恢复",
                            onClick = { onRestore(day.id) }
                        )
                        SecondaryButton(
                            modifier = Modifier.weight(1f),
                            background = if (confirmTwoStepDelete) s.error else if (s.isDark) s.container else Color.White,
                            contentColor = if (confirmTwoStepDelete) Color.White else s.error,
                            icon = Icons.Filled.Delete,
                            text = if (confirmTwoStepDelete) "确认删除？" else "彻底删除",
                            onClick = {
                                if (confirmTwoStepDelete) {
                                    confirmTwoStepDelete = false
                                    onDelete(day.id)
                                } else {
                                    confirmTwoStepDelete = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除这条倒数日？") },
            text = { Text("「${day.title}」将被永久删除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(day.id) }) { Text("删除", color = s.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun MilestoneCard(day: Countdown, books: List<Book>, coverStore: CoverStore) {
    val s = LocalSerein.current
    val remaining = remainingDays(day)
    val cover = rememberCoverBitmap(day.cover, coverStore)
    val bookName = books.find { it.id == day.bookId }?.name ?: "纪念日"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(s.container)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(heroBrush(bookName))
        ) {
            CoverImage(cover, Modifier.fillMaxSize())
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    bookName,
                    color = s.onAccentStrong,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            if (day.lunar) "农历倒数" else "COUNTDOWN MILESTONE",
            color = s.onSurfaceVariant,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            day.title,
            color = s.onSurface,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(statusWord(remaining), color = s.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    abs(remaining).toString(),
                    color = s.primary,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-1).sp
                )
                if (remaining != 0L) {
                    Text(" 天", color = s.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(targetDate(day).format(FmtDot), color = s.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "${weekdayFull(targetDate(day))} · ${if (day.repeatYearly) "每年" else "启程"}",
                    color = s.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (s.isDark) s.high else Color.White)
                .padding(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (day.repeatYearly) "每逢此日 · 已陪你走过 ${yearRound(day)} 天" else "已过 ${elapsedSince(day)} 天（起始 ${milestoneStart(day).format(FmtDot)}）",
                    color = s.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Text("${(progressOf(day) * 100).roundToInt()}% 完成", color = s.primary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(s.highest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressOf(day).coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(s.primary)
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
}

private fun yearRound(day: Countdown): Long =
    java.time.temporal.ChronoUnit.DAYS.between(previousOccurrence(day, targetDate(day)), java.time.LocalDate.now()).coerceAtLeast(0)

fun milestoneStart(day: Countdown): LocalDate = day.createdAt ?: day.date.minusDays(90)

@Composable
private fun InfoCardShell(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    val s = LocalSerein.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(s.container)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun ReminderStatusCard(day: Countdown) {
    val s = LocalSerein.current
    InfoCardShell {
        IconCircle(Color.White, Icons.Outlined.Notifications, s.primary)
        Column(Modifier.weight(1f)) {
            Text("提醒状态", color = s.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                if (day.remind) "提前 7 天 09:00 与当天即时提醒" else "未开启提醒",
                color = s.onSurface,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
        PillChip(if (day.remind) "已设响铃" else "未开启", s.highest, s.onSurfaceVariant)
    }
}

@Composable
private fun BookCard(day: Countdown, books: List<Book>) {
    val s = LocalSerein.current
    val bookName = books.find { it.id == day.bookId }?.name ?: "纪念日"
    InfoCardShell {
        IconCircle(Color.White, Icons.Outlined.EditNote, s.primary)
        Column(Modifier.weight(1f)) {
            Text("所属倒数本", color = s.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            PillChip(bookName, s.accent, s.onAccentStrong)
        }
    }
}

@Composable
private fun CreatedCard(day: Countdown) {
    val s = LocalSerein.current
    InfoCardShell {
        IconCircle(s.accent, Icons.Outlined.CalendarMonth, s.onAccentStrong)
        Column(Modifier.weight(1f)) {
            Text("创建时间", color = s.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                "${milestoneStart(day).format(FmtCn)}（创建已 ${elapsedSince(day)} 天）",
                color = s.onSurface,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Icon(Icons.Outlined.History, contentDescription = null, tint = s.outlineVariant, modifier = Modifier.size(20.dp))
    }
}

/** 小记区：与桌面版完全一致的时间流。 */
@Composable
private fun NotesSection(
    day: Countdown,
    noteOrder: NoteOrder,
    onToggleOrder: () -> Unit,
    onPublish: (String) -> Unit,
    onEditNote: (String, String) -> Unit,
    onDeleteNote: (String) -> Unit
) {
    val s = LocalSerein.current
    var draft by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var editText by remember { mutableStateOf("") }

    val notes = remember(day.notes, noteOrder) {
        if (noteOrder == NoteOrder.LATEST_FIRST) day.notes.sortedByDescending { it.createdAt }
        else day.notes.sortedBy { it.createdAt }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(s.container)
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("小记", color = s.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text(day.notes.size.toString(), color = s.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            if (day.notes.isNotEmpty() && !day.archived) {
                Text(
                    if (noteOrder == NoteOrder.LATEST_FIRST) "最新在上" else "编年体",
                    color = s.primary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onToggleOrder)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (notes.isEmpty()) {
            Text("还没有小记，写下第一条吧", color = s.onSurfaceVariant, fontSize = 13.5.sp)
        } else {
            Column {
                notes.forEachIndexed { index, note ->
                    if (index > 0) {
                        HorizontalDivider(color = s.outlineVariant, thickness = 0.5.dp)
                    }
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
                            onEdit = {
                                editText = note.text
                                editingId = note.id
                            },
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
                    .background(if (s.isDark) s.high else Color.White)
                    .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 200) draft = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = s.onSurface),
                    cursorBrush = SolidColor(s.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (draft.isNotBlank()) { onPublish(draft); draft = "" }
                    }),
                    modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                    decorationBox = { inner ->
                        Box {
                            if (draft.isEmpty()) Text("写一条小记…", color = s.outlineVariant, fontSize = 14.sp)
                            inner()
                        }
                    }
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (draft.isNotBlank()) s.primary else s.highest)
                        .clickable(enabled = draft.isNotBlank()) { onPublish(draft); draft = "" }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = if (draft.isNotBlank()) s.onPrimary else s.onSurfaceVariant, modifier = Modifier.size(15.dp))
                        Text("发布", color = if (draft.isNotBlank()) s.onPrimary else s.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
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
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(note.text, color = s.onSurface, fontSize = 14.sp, lineHeight = 21.sp)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(meta, color = s.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.weight(1f))
            if (!archived && note.isEditableToday()) {
                Text(
                    "编辑",
                    color = s.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onEdit)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            if (!archived) {
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
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        BasicTextField(
            value = text,
            onValueChange = { if (it.length <= 200) text = it },
            textStyle = TextStyle(fontSize = 14.sp, color = s.onSurface, lineHeight = 21.sp),
            cursorBrush = SolidColor(s.primary),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(s.primary)
                    .clickable(enabled = text.isNotBlank()) { onSave(text) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("保存", color = s.onPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
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

@Composable
private fun SecondaryButton(
    modifier: Modifier = Modifier,
    background: Color,
    contentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text(text, color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}
