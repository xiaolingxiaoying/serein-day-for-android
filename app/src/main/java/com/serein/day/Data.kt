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

/** 一条小记：盖有不可变的发布时刻；写下当天内可编辑并留修改时间，过后只能删除。 */
data class Note(
    val id: String,
    val text: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime? = null
) {
    /** 「当天编辑窗」：发布时刻的本地日历日 == 当前本地日历日。 */
    fun isEditableToday(today: LocalDate = LocalDate.now()): Boolean = createdAt.toLocalDate() == today
}

/** 倒数本：事件的归属分组，可新建、重命名、删除（删除时事件移入首个剩余倒数本）。 */
data class Book(val id: String, val name: String)

enum class NoteOrder { LATEST_FIRST, CHRONOLOGICAL }

/**
 * 倒数事件。
 * date 为公历锚点日期；lunar=true 表示按其农历月日记忆；repeatYearly=true 表示每年重复。
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

    fun loadNoteOrder(): NoteOrder = when (settingsPrefs.getString("noteOrder", null)) {
        "chronological" -> NoteOrder.CHRONOLOGICAL
        else -> NoteOrder.LATEST_FIRST
    }

    fun saveNoteOrder(order: NoteOrder) {
        settingsPrefs.edit().putString(
            "noteOrder",
            if (order == NoteOrder.CHRONOLOGICAL) "chronological" else "latest_first"
        ).apply()
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

/** 封面副本：与原图脱钩，存于应用私有 covers/ 目录，文件名用事件 id。 */
class CoverStore(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val dir = File(context.filesDir, "covers").apply { mkdirs() }

    /** 从内容 URI 复制副本，返回文件名。 */
    fun copyIn(uri: Uri, id: String): String? = runCatching {
        val ext = when (resolver.getType(uri)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        val name = "$id.$ext"
        val target = File(dir, name)
        target.outputStream().use { out -> resolver.openInputStream(uri)?.use { it.copyTo(out) } }
        // 清理同 id 的旧扩展名副本
        dir.listFiles { f -> f.name.startsWith("$id.") && f.name != name }?.forEach { it.delete() }
        name
    }.getOrNull()

    fun resolve(name: String): File = File(dir, name)

    fun remove(name: String) {
        File(dir, name).delete()
    }

    /** 新建流程中封面先落为 draft.*，保存时改为真实事件 id。 */
    fun renameDraft(id: String, draftName: String): String {
        val ext = draftName.substringAfterLast('.', "jpg")
        val target = "$id.$ext"
        if (File(dir, draftName).renameTo(File(dir, target))) return target
        return draftName
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

val Categories = listOf("旅行", "纪念日", "生日", "考试")

val Quotes = listOf(
    "把期待写在日历的扉页上，每天醒来，便离心中的远方近了一步。",
    "温柔的日子，会因为一个值得等待的日期而闪闪发光。",
    "倒数不是焦虑，是把想念安放在时间里的方式。",
    "每一个被认真记下的日子，都会在抵达时加倍明亮。",
    "慢慢来，日子会替你把答案送到眼前。"
)
