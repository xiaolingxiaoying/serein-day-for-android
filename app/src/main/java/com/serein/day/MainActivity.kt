package com.serein.day

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = DayRepository(this)
        val coverStore = CoverStore(this)
        val settingsPrefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        setContent {
            var days by remember { mutableStateOf(repository.load()) }
            var books by remember { mutableStateOf(repository.loadBooks()) }
            var paletteIndex by remember { mutableIntStateOf(settingsPrefs.getInt("palette", 3).coerceIn(0, CUSTOM_PALETTE_INDEX)) }
            var customPrimary by remember { mutableIntStateOf(settingsPrefs.getInt("customPrimary", 0xFF006B5B.toInt())) }
            var modeIndex by remember { mutableIntStateOf(settingsPrefs.getInt("mode", 2).coerceIn(0, 2)) }
            var haptics by remember { mutableStateOf(settingsPrefs.getBoolean("haptics", true)) }
            var pinnedNotif by remember { mutableStateOf(settingsPrefs.getBoolean("pinnedNotif", false)) }
            var minimalMode by remember { mutableStateOf(settingsPrefs.getBoolean("minimalMode", false)) }
            var sortOrder by remember { mutableStateOf(repository.loadSortOrder()) }

            val dark = when (modeIndex) {
                0 -> false
                1 -> true
                else -> isSystemInDarkTheme()
            }
            SereinTheme(palette = paletteFor(paletteIndex, dark, customPrimary), dark = dark) {
                val context = LocalContext.current
                LaunchedEffect(days, pinnedNotif) {
                    if (pinnedNotif) {
                        PinnedNotification.update(context, days)
                        PinnedNotification.scheduleMidnightRefresh(context)
                    } else {
                        PinnedNotification.cancel(context)
                    }
                }
                SereinApp(
                    days = days,
                    onDaysChange = { days = it; repository.save(it) },
                    books = books,
                    onBooksChange = { books = it; repository.saveBooks(it) },
                    coverStore = coverStore,
                    paletteIndex = paletteIndex,
                    onPaletteChange = { paletteIndex = it; settingsPrefs.edit().putInt("palette", it).apply() },
                    customPrimary = customPrimary,
                    onCustomPrimaryChange = { customPrimary = it; settingsPrefs.edit().putInt("customPrimary", it).apply() },
                    modeIndex = modeIndex,
                    onModeChange = { modeIndex = it; settingsPrefs.edit().putInt("mode", it).apply() },
                    haptics = haptics,
                    onHapticsChange = { haptics = it; settingsPrefs.edit().putBoolean("haptics", it).apply() },
                    pinnedNotif = pinnedNotif,
                    onPinnedNotifChange = { pinnedNotif = it; settingsPrefs.edit().putBoolean("pinnedNotif", it).apply() },
                    minimalMode = minimalMode,
                    onMinimalModeChange = { minimalMode = it; settingsPrefs.edit().putBoolean("minimalMode", it).apply() },
                    sortOrder = sortOrder,
                    onSortOrderChange = { sortOrder = it; repository.saveSortOrder(it) }
                )
            }
        }
    }
}

@Composable
private fun SereinApp(
    days: List<Countdown>,
    onDaysChange: (List<Countdown>) -> Unit,
    books: List<Book>,
    onBooksChange: (List<Book>) -> Unit,
    coverStore: CoverStore,
    paletteIndex: Int,
    onPaletteChange: (Int) -> Unit,
    customPrimary: Int,
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
    onSortOrderChange: (SortOrder) -> Unit
) {
    val s = LocalSerein.current
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Countdown?>(null) }
    var detailId by remember { mutableStateOf<String?>(null) }
    var selectedBookId by remember { mutableStateOf<String?>(null) }
    var showBookManager by remember { mutableStateOf(false) }
    var showArchive by remember { mutableStateOf(false) }

    val overlayOpen = adding || editing != null
    BackHandler(enabled = overlayOpen || detailId != null || showArchive) {
        when {
            editing != null -> editing = null
            adding -> adding = false
            detailId != null -> detailId = null
            else -> showArchive = false
        }
    }

    fun mutateDay(id: String, transform: (Countdown) -> Countdown) {
        onDaysChange(days.map { if (it.id == id) transform(it) else it })
    }

    Surface(modifier = Modifier.fillMaxSize(), color = s.surface) {
        Box(Modifier.fillMaxSize()) {
            when {
                overlayOpen -> EditorScreen(
                    initial = editing,
                    books = books,
                    coverStore = coverStore,
                    minimalMode = minimalMode,
                    onCancel = { adding = false; editing = null },
                    onSave = { updated ->
                        var final = updated
                        if (final.cover?.startsWith("draft.") == true) {
                            final = final.copy(cover = coverStore.renameDraft(final.id, final.cover!!))
                        }
                        if (final.wallpaper?.startsWith("draftw.") == true) {
                            final = final.copy(wallpaper = coverStore.renameDraft("${final.id}.w", final.wallpaper!!))
                        }
                        if (editing == null) {
                            onDaysChange(days + final)
                            Toast.makeText(context, "已保存「${final.title}」", Toast.LENGTH_SHORT).show()
                        } else {
                            onDaysChange(days.map { if (it.id == final.id) final else it })
                            detailId = final.id
                        }
                        adding = false
                        editing = null
                    },
                    onDelete = editing?.let { day ->
                        {
                            if (day.cover != null) coverStore.remove(day.cover)
                            if (day.wallpaper != null) coverStore.remove(day.wallpaper)
                            onDaysChange(days.filterNot { it.id == day.id })
                            editing = null
                            detailId = null
                            Toast.makeText(context, "已删除「${day.title}」", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                detailId != null -> {
                    val detailDay = days.find { it.id == detailId }
                    if (detailDay == null) {
                        MainTabs(
                            days, onDaysChange, books, onBooksChange, coverStore,
                            tab, { tab = it }, haptics, { adding = true },
                            selectedBookId, { selectedBookId = it }, { showBookManager = true },
                            { detailId = it }, { showArchive = true },
                            paletteIndex, onPaletteChange, customPrimary, onCustomPrimaryChange,
                            modeIndex, onModeChange, onHapticsChange, pinnedNotif, onPinnedNotifChange,
                            minimalMode, onMinimalModeChange, sortOrder, onSortOrderChange
                        )
                    } else {
                        DetailScreen(
                            day = detailDay,
                            books = books,
                            coverStore = coverStore,
                            minimalMode = minimalMode,
                            onBack = { detailId = null },
                            onPublishNote = { text ->
                                mutateDay(detailDay.id) { d ->
                                    d.copy(notes = d.notes + Note(DayRepository.newNoteId(), text.trim(), LocalDateTime.now()))
                                }
                            },
                            onEditNote = { noteId, text ->
                                mutateDay(detailDay.id) { d ->
                                    d.copy(notes = d.notes.map { n ->
                                        if (n.id == noteId) n.copy(text = text.trim(), updatedAt = LocalDateTime.now()) else n
                                    })
                                }
                            },
                            onDeleteNote = { noteId ->
                                mutateDay(detailDay.id) { d -> d.copy(notes = d.notes.filterNot { it.id == noteId }) }
                            },
                            onEditEvent = { editing = it },
                            onShare = { shareText(context, "分享卡片", detailShareText(it)) },
                            onArchive = { id ->
                                onDaysChange(days.map { if (it.id == id) it.copy(archived = true) else it })
                                Toast.makeText(context, "已归档，可随时恢复", Toast.LENGTH_SHORT).show()
                            },
                            onRestore = { id ->
                                onDaysChange(days.map { if (it.id == id) it.copy(archived = false) else it })
                                Toast.makeText(context, "已恢复到主列表", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = { id ->
                                val day = days.find { it.id == id }
                                if (day?.cover != null) coverStore.remove(day.cover)
                                if (day?.wallpaper != null) coverStore.remove(day.wallpaper)
                                onDaysChange(days.filterNot { it.id == id })
                                detailId = null
                                Toast.makeText(context, "已彻底删除", Toast.LENGTH_SHORT).show()
                            },
                            onWallpaperChange = { name ->
                                mutateDay(detailDay.id) { it.copy(wallpaper = name) }
                            }
                        )
                    }
                }
                showArchive -> ArchiveScreen(
                    days = days,
                    books = books,
                    onBack = { showArchive = false },
                    onOpen = { detailId = it; showArchive = false },
                    coverStore = coverStore
                )
                else -> MainTabs(
                    days, onDaysChange, books, onBooksChange, coverStore,
                    tab, { tab = it }, haptics, { adding = true },
                    selectedBookId, { selectedBookId = it }, { showBookManager = true },
                    { detailId = it }, { showArchive = true },
                    paletteIndex, onPaletteChange, customPrimary, onCustomPrimaryChange,
                    modeIndex, onModeChange, onHapticsChange, pinnedNotif, onPinnedNotifChange,
                    minimalMode, onMinimalModeChange, sortOrder, onSortOrderChange
                )
            }
        }
    }

    if (showBookManager) {
        BookManagerDialog(
            books = books,
            days = days,
            onAdd = { name -> onBooksChange(books + Book(DayRepository.newBookId(), name.trim())) },
            onRename = { id, name -> onBooksChange(books.map { if (it.id == id) it.copy(name = name.trim()) else it }) },
            onDelete = { id ->
                val remaining = books.filterNot { it.id == id }
                val fallback = remaining.firstOrNull() ?: return@BookManagerDialog
                onBooksChange(remaining)
                onDaysChange(days.map { if (it.bookId == id) it.copy(bookId = fallback.id) else it })
            },
            onDismiss = { showBookManager = false }
        )
    }
}

@Composable
private fun MainTabs(
    days: List<Countdown>,
    onDaysChange: (List<Countdown>) -> Unit,
    books: List<Book>,
    onBooksChange: (List<Book>) -> Unit,
    coverStore: CoverStore,
    tab: Int,
    onTabChange: (Int) -> Unit,
    haptics: Boolean,
    onAdd: () -> Unit,
    selectedBookId: String?,
    onSelectedBookChange: (String?) -> Unit,
    onManageBooks: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onOpenArchive: () -> Unit,
    paletteIndex: Int,
    onPaletteChange: (Int) -> Unit,
    customPrimary: Int,
    onCustomPrimaryChange: (Int) -> Unit,
    modeIndex: Int,
    onModeChange: (Int) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    pinnedNotif: Boolean,
    onPinnedNotifChange: (Boolean) -> Unit,
    minimalMode: Boolean,
    onMinimalModeChange: (Boolean) -> Unit,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> HomeTab(
                    days = days,
                    books = books,
                    selectedBookId = selectedBookId,
                    onBookSelect = onSelectedBookChange,
                    onManageBooks = onManageBooks,
                    onOpen = onOpenDetail,
                    onOpenArchive = onOpenArchive,
                    coverStore = coverStore,
                    minimalMode = minimalMode,
                    sortOrder = sortOrder
                )
                else -> SettingsTab(
                    days = days,
                    books = books,
                    paletteIndex = paletteIndex,
                    onPaletteChange = onPaletteChange,
                    customPrimary = customPrimary,
                    onCustomPrimaryChange = onCustomPrimaryChange,
                    modeIndex = modeIndex,
                    onModeChange = onModeChange,
                    haptics = haptics,
                    onHapticsChange = onHapticsChange,
                    pinnedNotif = pinnedNotif,
                    onPinnedNotifChange = onPinnedNotifChange,
                    minimalMode = minimalMode,
                    onMinimalModeChange = onMinimalModeChange,
                    sortOrder = sortOrder,
                    onSortOrderChange = onSortOrderChange,
                    onBackup = { shareText(context = null, title = "Serein Day 备份", text = DayRepository.toJson(days)) },
                    onManageBooks = onManageBooks
                )
            }
            AddFab(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 20.dp),
                hapticsEnabled = haptics,
                onClick = onAdd
            )
        }
        BottomNavBar(selected = tab, onSelect = onTabChange)
    }
}

/** 倒数本管理：新建、重命名、删除（删除时事件移入首个剩余倒数本）。 */
@Composable
private fun BookManagerDialog(
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理倒数本") },
        text = {
            Column {
                books.forEach { book ->
                    val count = days.count { it.bookId == book.id && !it.archived }
                    if (editingId == book.id) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = editName,
                                onValueChange = { if (it.length <= 12) editName = it },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = s.onSurface),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { if (editName.isNotBlank()) { onRename(book.id, editName); editingId = null } }) { Text("保存", color = s.primary) }
                            TextButton(onClick = { editingId = null }) { Text("取消") }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(book.name, color = s.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("$count 个事件", color = s.onSurfaceVariant, fontSize = 11.5.sp)
                            }
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "重命名",
                                tint = s.primary,
                                modifier = Modifier
                                    .size(36.dp)
                                    .padding(8.dp)
                                    .clickable { editingId = book.id; editName = book.name }
                            )
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "删除倒数本",
                                tint = if (books.size > 1) s.error else s.outlineVariant,
                                modifier = Modifier
                                    .size(36.dp)
                                    .padding(8.dp)
                                    .clickable(enabled = books.size > 1) { onDelete(book.id) }
                            )
                        }
                    }
                }
                Spacer(Modifier.size(4.dp))
                if (adding) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = newName,
                            onValueChange = { if (it.length <= 12) newName = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = s.onSurface),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { if (newName.isNotBlank()) { onAdd(newName); newName = ""; adding = false } }) { Text("添加", color = s.primary) }
                        TextButton(onClick = { adding = false }) { Text("取消") }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { adding = true }.padding(vertical = 8.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = s.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("新建倒数本", color = s.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成", color = s.primary) } }
    )
}

private fun detailShareText(day: Countdown): String {
    val r = remainingDays(day)
    val status = when {
        r > 0 -> "还有 $r 天"
        r == 0L -> "就是今天"
        else -> "已过去 ${-r} 天"
    }
    return "【Serein Day】${day.title}\n${dateText(day)}\n$status\n\n—— 来自 Serein Day 倒数日"
}

private fun shareText(context: Context?, title: String, text: String) {
    if (context == null) return
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
