package com.serein.day

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            var paletteIndex by remember { mutableIntStateOf(settingsPrefs.getInt("palette", 0).coerceIn(0, CUSTOM_PALETTE_INDEX)) }
            var customPrimary by remember { mutableIntStateOf(settingsPrefs.getInt("customPrimary", 0xFFC8F531.toInt())) }
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
                // 通知/闹钟等外设副作用与 UI 解耦：snapshotFlow 监听数据与开关，conflate 合并高频连续改动，
                // 处理挪到 Default 调度器，避免每次改动都在主线程同步刷新全部通知/闹钟而卡顿。
                LaunchedEffect(Unit) {
                    snapshotFlow { days to pinnedNotif }
                        .distinctUntilChanged()
                        .conflate()
                        .collect { (d, pin) ->
                            withContext(Dispatchers.Default) {
                                if (pin) {
                                    PinnedNotification.update(context, d)
                                    PinnedNotification.scheduleMidnightRefresh(context)
                                } else {
                                    PinnedNotification.cancel(context)
                                }
                                DailyReminderScheduler.sync(context, d)
                                CountdownWidget.update(context, d)
                            }
                        }
                }
                CompositionLocalProvider(LocalHapticsEnabled provides haptics) {
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
    val scope = rememberCoroutineScope()
    var showSettings by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Countdown?>(null) }
    var detailId by remember { mutableStateOf<String?>(null) }
    var selectedBookId by remember { mutableStateOf<String?>(null) }
    var showBookManager by remember { mutableStateOf(false) }
    var showArchive by remember { mutableStateOf(false) }

    val overlayOpen = adding || editing != null
    // 转场退出期间 detailId / editing 已被清空，但滑出中的页面仍需原内容渲染；
    // 记住最后的有效值，避免退出页中途闪变成另一页。
    var lastDetailId by remember { mutableStateOf<String?>(null) }
    var lastEditorInitial by remember { mutableStateOf<Countdown?>(null) }
    if (detailId != null) lastDetailId = detailId
    if (overlayOpen) lastEditorInitial = editing
    BackHandler(enabled = overlayOpen || detailId != null || showSettings || showArchive) {
        when {
            editing != null -> editing = null
            adding -> adding = false
            detailId != null -> detailId = null
            showArchive -> showArchive = false
            showSettings -> showSettings = false
        }
    }

    fun mutateDay(id: String, transform: (Countdown) -> Countdown) {
        onDaysChange(days.map { if (it.id == id) transform(it) else it })
    }

    /** 打开详情前在后台预热封面与壁纸：转场进入时位图已就绪，不再这一帧纯色、下一帧突现导致闪变/撕裂。 */
    fun openDetail(id: String) {
        days.find { it.id == id }?.let { d ->
            scope.launch {
                prewarmCover(d.cover, coverStore)
                prewarmCover(d.wallpaper, coverStore)
            }
        }
        detailId = id
    }

    /** 页面栈深度：Main(0) → Settings(1) → Archive(2) → Detail(3) → Editor(4)，越深越靠顶层。 */
    fun depthOf(screen: Screen): Int = when (screen) {
        Screen.Main -> 0
        Screen.Settings -> 1
        Screen.Archive -> 2
        Screen.Detail -> 3
        Screen.Editor -> 4
    }
    val screen = when {
        overlayOpen -> Screen.Editor
        detailId != null && days.any { it.id == detailId } -> Screen.Detail
        showArchive -> Screen.Archive
        showSettings -> Screen.Settings
        else -> Screen.Main
    }
    val reducedMotion = rememberReducedMotion()

    Surface(modifier = Modifier.fillMaxSize(), color = s.surface) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                val forward = depthOf(targetState) > depthOf(initialState)
                if (reducedMotion) {
                    fadeIn(snap()).togetherWith(fadeOut(snap()))
                } else {
                    // 转场去双绘：全宽对开滑动改为「短距滑动 + 淡入淡出」。两页整幅同时平移/重叠的窗口
                    // 与边界错位感都显著降低，弱设备转场不再撕裂；方向仍按进/出栈保留。用固定时长 tween
                    // 取代弹簧，让帧预算更可控、且两页 alpha 在同一帧内完成（去掉硬件层错开产生的撕裂）。
                    val move = tween<IntOffset>(220, easing = FastOutSlowInEasing)
                    val intoFade = fadeIn(tween(200))
                    val outFade = fadeOut(tween(160))
                    val into = if (forward)
                        (intoFade + slideInHorizontally(move) { it / 3 })
                    else
                        (intoFade + slideInHorizontally(move) { -it / 3 })
                    val outOf = if (forward)
                        (outFade + slideOutHorizontally(move) { -it / 3 })
                    else
                        (outFade + slideOutHorizontally(move) { it / 3 })
                    into.togetherWith(outOf)
                }
            },
            label = "screen"
        ) { current ->
            // 空间一致性：进栈时新页在最上层，出栈时被弹出的页仍在最上层滑走。
            // 每屏自带不透明底色：转场时新旧两页同屏绘制，若靠外层共享 Surface 透底，
            // 两页内容会叠在一起（设置↔归档转场透字）。
            val pushing = depthOf(screen) > depthOf(current)
            val onTop = if (current == screen) pushing else !pushing
            Box(Modifier.fillMaxSize().background(s.surface).zIndex(if (onTop) 1f else 0f)) {
            when (current) {
                Screen.Editor -> {
                    // 退出转场中 editing 已清空，用缓存快照渲染，滑出页内容保持不变
                    val editorDay = if (overlayOpen) editing else lastEditorInitial
                    EditorScreen(
                        initial = editorDay,
                    books = books,
                    coverStore = coverStore,
                    minimalMode = minimalMode,
                    onCancel = {
                        // 新建和编辑流程取消时都清理未落地的图片副本。
                        coverStore.removeDrafts()
                        adding = false
                        editing = null
                    },
                    onSave = { updated ->
                        var final = updated
                        if (final.cover?.startsWith("draft.") == true) {
                            final = final.copy(cover = coverStore.renameDraft(final.id, final.cover!!))
                        }
                        if (final.wallpaper?.startsWith("draftw.") == true) {
                            final = final.copy(wallpaper = coverStore.renameDraft("${final.id}.w", final.wallpaper!!))
                        }
                        coverStore.removeDrafts()
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
                    onAddBook = { name ->
                        val existing = books.firstOrNull { it.name.equals(name, ignoreCase = true) }
                        if (existing != null) {
                            existing.id
                        } else {
                            val book = Book(DayRepository.newBookId(), name)
                            onBooksChange(books + book)
                            book.id
                        }
                    },
                    onDelete = editorDay?.let { day ->
                        {
                            if (day.cover != null) coverStore.remove(day.cover)
                            if (day.wallpaper != null) coverStore.remove(day.wallpaper)
                            coverStore.removeDrafts()
                            onDaysChange(days.filterNot { it.id == day.id })
                            editing = null
                            detailId = null
                            Toast.makeText(context, "已删除「${day.title}」", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                }
                Screen.Detail -> {
                    // 退出转场中 detailId 已清空，用 lastDetailId 找回事件，让滑出页保持原内容
                    val detailDay = days.find { it.id == (detailId ?: lastDetailId) }
                    if (detailDay != null) {
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
                            onCoverChange = { name ->
                                mutateDay(detailDay.id) { it.copy(cover = name) }
                            },
                            onCoverScaleChange = { scale ->
                                mutateDay(detailDay.id) { it.copy(coverScale = scale) }
                            },
                            onCoverOpacityChange = { opacity ->
                                mutateDay(detailDay.id) { it.copy(coverOpacity = opacity) }
                            },
                            onWallpaperChange = { name ->
                                mutateDay(detailDay.id) { it.copy(wallpaper = name) }
                            },
                            onWallpaperScaleChange = { scale ->
                                mutateDay(detailDay.id) { it.copy(wallpaperScale = scale) }
                            },
                            onWallpaperOpacityChange = { opacity ->
                                mutateDay(detailDay.id) { it.copy(wallpaperOpacity = opacity) }
                            },
                            onWallpaperDimChange = { dim ->
                                mutateDay(detailDay.id) { it.copy(wallpaperDim = dim) }
                            },
                            onDetailCardTransparencyChange = { transparent ->
                                mutateDay(detailDay.id) { it.copy(detailCardTransparent = transparent) }
                            },
                            onAddSubDay = { title, date ->
                                mutateDay(detailDay.id) { d ->
                                    d.copy(subs = d.subs + SubDay(newSubDayId(), title, date))
                                }
                            },
                            onEditSubDay = { subId, title, date ->
                                mutateDay(detailDay.id) { d ->
                                    d.copy(subs = d.subs.map { if (it.id == subId) it.copy(title = title, date = date) else it })
                                }
                            },
                            onDeleteSubDay = { subId ->
                                mutateDay(detailDay.id) { d -> d.copy(subs = d.subs.filterNot { it.id == subId }) }
                            }
                        )
                    } else {
                        // 事件已被彻底删除、正在退出转场：只保持底色，避免闪现别的页面
                        Box(Modifier.fillMaxSize().background(s.surface))
                    }
                }
                Screen.Settings -> SettingsTab(
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
                    onBackup = { shareText(context, "Serein Day 备份", DayRepository.toJson(days)) },
                    onManageBooks = { showBookManager = true },
                    onOpenArchive = { showArchive = true },
                    onBack = { showSettings = false }
                )
                Screen.Archive -> ArchiveScreen(
                    days = days,
                    books = books,
                    onBack = { showArchive = false },
                    // 保留归档栈位：详情返回时回到归档页（清掉 showArchive 会跳回设置页）
                    onOpen = { openDetail(it) },
                    coverStore = coverStore
                )
                Screen.Main -> MainScreen(
                    days = days,
                    books = books,
                    coverStore = coverStore,
                    minimalMode = minimalMode,
                    sortOrder = sortOrder,
                    selectedBookId = selectedBookId,
                    onSelectedBookChange = { selectedBookId = it },
                    onManageBooks = { showBookManager = true },
                    onOpenDetail = { openDetail(it) },
                    onOpenSettings = { showSettings = true },
                    onAdd = { adding = true }
                )
                }
            }
        }
    }

    if (showBookManager) {
        BookManagerSheet(
            books = books,
            days = days,
            onAdd = { name -> onBooksChange(books + Book(DayRepository.newBookId(), name)) },
            onRename = { id, name -> onBooksChange(books.map { if (it.id == id) it.copy(name = name) else it }) },
            onDelete = { id ->
                val remaining = books.filterNot { it.id == id }
                val fallback = remaining.firstOrNull() ?: return@BookManagerSheet
                onBooksChange(remaining)
                onDaysChange(days.map { if (it.bookId == id) it.copy(bookId = fallback.id) else it })
            },
            onDismiss = { showBookManager = false }
        )
    }
}

/** 全屏页面栈标识，用于切换动画的方向判断。 */
private enum class Screen { Main, Settings, Archive, Detail, Editor }

@Composable
private fun MainScreen(
    days: List<Countdown>,
    books: List<Book>,
    coverStore: CoverStore,
    minimalMode: Boolean,
    sortOrder: SortOrder,
    selectedBookId: String?,
    onSelectedBookChange: (String?) -> Unit,
    onManageBooks: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onAdd: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        HomeTab(
            days = days,
            books = books,
            selectedBookId = selectedBookId,
            onBookSelect = onSelectedBookChange,
            onManageBooks = onManageBooks,
            onOpen = onOpenDetail,
            onOpenSettings = onOpenSettings,
            coverStore = coverStore,
            minimalMode = minimalMode,
            sortOrder = sortOrder
        )
        AddPillButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            onClick = onAdd
        )
    }
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
