package com.serein.day

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

/** 一条小记：盖有发布时刻，随时可编辑、可删除。 */
data class Note(
    val id: String,
    val text: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime? = null
)

/** 倒数本：事件的归属分组，可新建、重命名、删除（删除时事件移入首个剩余倒数本）。 */
data class Book(val id: String, val name: String)

/** 列表排序方式（Days Matter 式：置顶始终在最前）。 */
enum class SortOrder { BY_REMAINING, BY_DATE, BY_CREATED }

/**
 * 倒数事件。
 * date 为公历锚点日期；lunar=true 表示按其农历月日记忆；repeatYearly=true 表示每年重复。
 * cover 为卡片封面副本文件名；wallpaper 为详情页背景壁纸副本文件名（两者独立）。
 */
data class Countdown(
    val id: String,
    val title: String,
    val date: LocalDate,
    val lunar: Boolean = false,
    val repeatYearly: Boolean = false,
    val bookId: String = "",
    val notes: List<Note> = emptyList(),
    val cover: String? = null,
    val wallpaper: String? = null,
    val remind: Boolean = false,
    val priority: Int = 0,
    val archived: Boolean = false,
    val createdAt: LocalDate? = null
)

class DayRepository(context: Context) {
    private val prefs = context.getSharedPreferences("countdowns", Context.MODE_PRIVATE)
    private val settingsPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load(): List<Countdown> = runCatching {
        val array = JSONArray(prefs.getString("items", "[]"))
        val books = loadBooks().ifEmpty { defaultBooks() }
        var changed = false
        val result = List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val notesJson = item.optJSONArray("notes") ?: JSONArray()
            val parsedNotes = List(notesJson.length()) { i ->
                val n = notesJson.getJSONObject(i)
                Note(
                    id = n.getString("id"),
                    text = n.getString("text"),
                    createdAt = LocalDateTime.parse(n.getString("createdAt")),
                    updatedAt = n.optString("updatedAt").takeIf { it.isNotEmpty() }?.let(LocalDateTime::parse)
                )
            }.toMutableList()
            val createdAt = runCatching { LocalDate.parse(item.optString("createdAt")) }.getOrNull()
            val legacyNote = item.optString("note", "")
            if (parsedNotes.isEmpty() && legacyNote.isNotEmpty()) {
                parsedNotes.add(
                    Note(
                        id = newNoteId(),
                        text = legacyNote,
                        createdAt = LocalDateTime.of(createdAt ?: LocalDate.now(), java.time.LocalTime.NOON)
                    )
                )
                changed = true
            }
            var bookId = item.optString("bookId", "")
            if (bookId.isBlank() || books.none { it.id == bookId }) {
                val legacyCategory = item.optString("category", "纪念日")
                bookId = (books.find { it.name == legacyCategory } ?: books.first()).id
                changed = true
            }
            Countdown(
                id = item.getString("id"),
                title = item.getString("title"),
                date = LocalDate.parse(item.getString("date")),
                lunar = item.optBoolean("lunar"),
                repeatYearly = item.optBoolean("repeat"),
                bookId = bookId,
                notes = parsedNotes,
                cover = item.optString("cover").takeIf { it.isNotEmpty() },
                wallpaper = item.optString("wallpaper").takeIf { it.isNotEmpty() },
                remind = item.optBoolean("remind"),
                priority = if (item.optBoolean("pinned")) 2 else item.optInt("priority", 0),
                archived = item.optBoolean("archived"),
                createdAt = createdAt
            )
        }
        if (changed) save(result)
        result
    }.getOrDefault(emptyList())

    fun save(days: List<Countdown>) {
        prefs.edit().putString("items", toJson(days)).apply()
    }

    fun saveBooks(books: List<Book>) {
        val array = JSONArray()
        books.forEach { book -> array.put(JSONObject().put("id", book.id).put("name", book.name)) }
        settingsPrefs.edit().putString("books", array.toString()).apply()
    }

    fun loadBooks(): List<Book> {
        val loaded = runCatching {
            val array = JSONArray(settingsPrefs.getString("books", "[]"))
            List(array.length()) { i ->
                val obj = array.getJSONObject(i)
                Book(obj.getString("id"), obj.getString("name"))
            }
        }.getOrDefault(emptyList())
        if (loaded.isEmpty()) {
            val defaults = defaultBooks()
            saveBooks(defaults)
            return defaults
        }
        return loaded
    }

    fun loadSortOrder(): SortOrder = when (settingsPrefs.getString("sortOrder", null)) {
        "date" -> SortOrder.BY_DATE
        "created" -> SortOrder.BY_CREATED
        else -> SortOrder.BY_REMAINING
    }

    fun saveSortOrder(order: SortOrder) {
        val key = when (order) {
            SortOrder.BY_DATE -> "date"
            SortOrder.BY_CREATED -> "created"
            SortOrder.BY_REMAINING -> "remaining"
        }
        settingsPrefs.edit().putString("sortOrder", key).apply()
    }

    companion object {
        fun toJson(days: List<Countdown>): String {
            val array = JSONArray()
            days.forEach { day ->
                val notes = JSONArray()
                day.notes.forEach { note ->
                    notes.put(JSONObject().apply {
                        put("id", note.id)
                        put("text", note.text)
                        put("createdAt", note.createdAt.toString())
                        note.updatedAt?.let { put("updatedAt", it.toString()) }
                    })
                }
                array.put(JSONObject().apply {
                    put("id", day.id)
                    put("title", day.title)
                    put("date", day.date.toString())
                    put("lunar", day.lunar)
                    put("repeat", day.repeatYearly)
                    put("bookId", day.bookId)
                    put("notes", notes)
                    day.cover?.let { put("cover", it) }
                    day.wallpaper?.let { put("wallpaper", it) }
                    put("remind", day.remind)
                    put("priority", day.priority)
                    put("pinned", day.priority == 2)
                    put("archived", day.archived)
                    day.createdAt?.let { put("createdAt", it.toString()) }
                })
            }
            return array.toString()
        }

        fun defaultBooks(): List<Book> = listOf(
            Book("b_anniversary", "纪念日"),
            Book("b_birthday", "生日"),
            Book("b_travel", "旅行"),
            Book("b_exam", "考试")
        )

        fun newBookId(): String = "b_" + UUID.randomUUID().toString().substring(0, 8)

        fun newNoteId(): String = UUID.randomUUID().toString()
    }
}

/** 图片副本：与原图脱钩，存于应用私有 covers/ 目录。封面文件名为 <id>.*，详情壁纸为 <id>.w.*。 */
class CoverStore(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val dir = File(context.filesDir, "covers").apply { mkdirs() }

    /** 从内容 URI 复制副本，返回文件名。base 为「id」或「id.w」。 */
    fun copyIn(uri: Uri, base: String): String? = runCatching {
        val ext = when (resolver.getType(uri)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        val name = "$base.$ext"
        val target = File(dir, name)
        target.outputStream().use { out -> resolver.openInputStream(uri)?.use { it.copyTo(out) } }
        // 清理同 base 的旧扩展名副本（保留壁纸的 <id>.w.* 文件）
        dir.listFiles { f -> f.name.startsWith("$base.") && f.name != name && !f.name.startsWith("$base.w.") }?.forEach { it.delete() }
        name
    }.getOrNull()

    fun resolve(name: String): File = File(dir, name)

    fun remove(name: String) {
        File(dir, name).delete()
    }

    /** 新建流程中图片先落为 draft.* / draftw.*，保存时改为真实事件 id。 */
    fun renameDraft(base: String, draftName: String): String {
        val ext = draftName.substringAfterLast('.', "jpg")
        val target = "$base.$ext"
        if (File(dir, draftName).renameTo(File(dir, target))) return target
        return draftName
    }

    /** 清理编辑流程遗留的草稿图片（取消、保存后或删除后调用）。 */
    fun removeDrafts() {
        dir.listFiles { f -> f.name.startsWith("draft.") || f.name.startsWith("draftw.") }?.forEach { it.delete() }
    }
}

val FmtCn = DateTimeFormatter.ofPattern("yyyy年M月d日")
val FmtDot = DateTimeFormatter.ofPattern("yyyy.MM.dd")
val FmtNote = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

fun newCountdownId(): String = UUID.randomUUID().toString()

fun weekdayFull(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.CHINA)

fun weekdayShort(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.CHINA)

/** 解析事件的目标日期：重复事件取下一次发生的日期，否则为锚点本身。 */
fun targetDate(day: Countdown, today: LocalDate = LocalDate.now()): LocalDate {
    if (!day.repeatYearly) return day.date
    return if (day.lunar) {
        val anchor = LunarCalendar.fromSolar(day.date)
        LunarCalendar.solarOfLunarAnniversary(anchor, today.year)
            ?.takeIf { !it.isBefore(today) }
            ?: LunarCalendar.solarOfLunarAnniversary(anchor, today.year + 1)
            ?: day.date
    } else {
        val anchor = day.date
        val thisYear = LocalDate.of(today.year, anchor.monthValue, minOf(anchor.dayOfMonth, LocalDate.of(today.year, anchor.monthValue, 1).lengthOfMonth()))
        if (!thisYear.isBefore(today)) thisYear else thisYear.plusYears(1)
    }
}

fun remainingDays(day: Countdown, today: LocalDate = LocalDate.now()): Long =
    ChronoUnit.DAYS.between(today, targetDate(day, today))

fun statusWord(remaining: Long): String = when {
    remaining > 0 -> "还有"
    remaining == 0L -> "就是今天"
    else -> "已过去"
}

/** 重复事件当前一轮的起始日（上一次发生日）。 */
fun previousOccurrence(day: Countdown, target: LocalDate): LocalDate =
    if (!day.repeatYearly) {
        target
    } else if (day.lunar) {
        LunarCalendar.solarOfLunarAnniversary(LunarCalendar.fromSolar(day.date), target.year - 1) ?: target.minusYears(1)
    } else {
        target.minusYears(1)
    }

/** 里程碑进度：重复事件按上一次发生到下一次发生的一轮计算。 */
fun progressOf(day: Countdown, today: LocalDate = LocalDate.now()): Float {
    val target = targetDate(day, today)
    val start: LocalDate = if (!day.repeatYearly) day.createdAt ?: day.date.minusDays(90) else previousOccurrence(day, target)
    val total = ChronoUnit.DAYS.between(start, target).coerceAtLeast(1)
    val elapsed = ChronoUnit.DAYS.between(start, today).coerceIn(0, total)
    return elapsed.toFloat() / total
}

fun elapsedSince(day: Countdown): Long =
    ChronoUnit.DAYS.between(day.createdAt ?: day.date.minusDays(90), LocalDate.now()).coerceAtLeast(0)

/** 目标日期展示文案：农历显示农历，重复事件带「每年」标记。 */
fun dateText(day: Countdown): String {
    val text = if (day.lunar) {
        "农历 " + LunarCalendar.fromSolar(day.date).let { "${it.monthName}${it.dayName}" }
    } else {
        day.date.format(FmtDot)
    }
    return if (day.repeatYearly) "$text · 每年" else text
}

/** 智能排序：置顶 → 今天 → 未来临近 → 已过沉底。 */
fun countdownComparator(today: LocalDate = LocalDate.now()): Comparator<Countdown> =
    compareByDescending<Countdown> { it.priority == 2 }
        .thenByDescending { remainingDays(it, today) >= 0 }
        .thenBy { remainingDays(it, today) }

/** 设置页可选的排序方式：按剩余天数 / 按目标日期 / 按添加时间；置顶始终最前。 */
fun sortCountdowns(days: List<Countdown>, order: SortOrder, today: LocalDate = LocalDate.now()): List<Countdown> {
    val pinnedFirst = compareByDescending<Countdown> { it.priority == 2 }
    val body: Comparator<Countdown> = when (order) {
        SortOrder.BY_REMAINING -> compareBy { remainingDays(it, today) }
        SortOrder.BY_DATE -> compareBy { targetDate(it, today) }
        SortOrder.BY_CREATED -> compareByDescending { it.createdAt ?: LocalDate.MIN }
    }
    return days.sortedWith(pinnedFirst.thenByDescending { remainingDays(it, today) >= 0 }.then(body))
}

val Categories = listOf("旅行", "纪念日", "生日", "考试")
