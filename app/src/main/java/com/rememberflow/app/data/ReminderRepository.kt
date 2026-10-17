package com.rememberflow.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.rememberflow.app.core.RecurrenceCodec
import com.rememberflow.app.core.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local-only persistence for reminders (a private SQLite database inside the app's own storage).
 * Nothing here touches the network, and the app declares no INTERNET permission.
 *
 * [reminders] is a hot StateFlow refreshed after every write, so the UI updates live even when a
 * change comes from a background alarm or boot receiver.
 */
class ReminderRepository private constructor(context: Context) {

    private val helper = Helper(context.applicationContext)
    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    init {
        refresh()
    }

    fun getAll(): List<Reminder> = helper.readableDatabase.query(
        TABLE, null, null, null, null, null, "$COL_START ASC, $COL_ID ASC"
    ).use { c ->
        val list = ArrayList<Reminder>(c.count)
        while (c.moveToNext()) list.add(c.toReminder())
        list
    }

    fun get(id: Long): Reminder? = helper.readableDatabase.query(
        TABLE, null, "$COL_ID = ?", arrayOf(id.toString()), null, null, null
    ).use { c -> if (c.moveToFirst()) c.toReminder() else null }

    /** Inserts a new reminder (its [Reminder.id] is ignored) and returns the stored copy. */
    fun insert(reminder: Reminder): Reminder {
        val values = reminder.toValues()
        val id = helper.writableDatabase.insertOrThrow(TABLE, null, values)
        refresh()
        return reminder.copy(id = id)
    }

    fun update(reminder: Reminder) {
        helper.writableDatabase.update(TABLE, reminder.toValues(), "$COL_ID = ?", arrayOf(reminder.id.toString()))
        refresh()
    }

    fun delete(id: Long) {
        helper.writableDatabase.delete(TABLE, "$COL_ID = ?", arrayOf(id.toString()))
        refresh()
    }

    fun refresh() {
        _reminders.value = getAll()
    }

    private fun Reminder.toValues() = ContentValues().apply {
        put(COL_TITLE, title)
        put(COL_DETAILS, details)
        put(COL_START, startMillis)
        put(COL_ZONE, zoneId)
        put(COL_RECURRENCE, RecurrenceCodec.encode(recurrence))
        put(COL_PAUSED, if (paused) 1 else 0)
        put(COL_CREATED, createdAtMillis)
        put(COL_HANDLED, lastHandledIndex)
        put(COL_FIRED, lastFiredMillis)
        put(COL_MISSED, missedCount)
    }

    private fun Cursor.toReminder() = Reminder(
        id = getLong(getColumnIndexOrThrow(COL_ID)),
        title = getString(getColumnIndexOrThrow(COL_TITLE)) ?: "",
        details = getString(getColumnIndexOrThrow(COL_DETAILS)) ?: "",
        startMillis = getLong(getColumnIndexOrThrow(COL_START)),
        zoneId = getString(getColumnIndexOrThrow(COL_ZONE)) ?: "UTC",
        recurrence = RecurrenceCodec.decode(getString(getColumnIndexOrThrow(COL_RECURRENCE))),
        paused = getInt(getColumnIndexOrThrow(COL_PAUSED)) != 0,
        createdAtMillis = getLong(getColumnIndexOrThrow(COL_CREATED)),
        lastHandledIndex = getLong(getColumnIndexOrThrow(COL_HANDLED)),
        lastFiredMillis = getLong(getColumnIndexOrThrow(COL_FIRED)),
        missedCount = getInt(getColumnIndexOrThrow(COL_MISSED))
    )

    private class Helper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE $TABLE (
                    $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_TITLE TEXT NOT NULL,
                    $COL_DETAILS TEXT NOT NULL DEFAULT '',
                    $COL_START INTEGER NOT NULL,
                    $COL_ZONE TEXT NOT NULL,
                    $COL_RECURRENCE TEXT NOT NULL DEFAULT '',
                    $COL_PAUSED INTEGER NOT NULL DEFAULT 0,
                    $COL_CREATED INTEGER NOT NULL,
                    $COL_HANDLED INTEGER NOT NULL DEFAULT -1,
                    $COL_FIRED INTEGER NOT NULL DEFAULT 0,
                    $COL_MISSED INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Version 1 is the first schema; future migrations go here.
        }
    }

    companion object {
        private const val DB_NAME = "rememberflow.db"
        private const val DB_VERSION = 1
        private const val TABLE = "reminders"
        private const val COL_ID = "id"
        private const val COL_TITLE = "title"
        private const val COL_DETAILS = "details"
        private const val COL_START = "start_ms"
        private const val COL_ZONE = "zone_id"
        private const val COL_RECURRENCE = "recurrence"
        private const val COL_PAUSED = "paused"
        private const val COL_CREATED = "created_ms"
        private const val COL_HANDLED = "last_handled_index"
        private const val COL_FIRED = "last_fired_ms"
        private const val COL_MISSED = "missed_count"

        @Volatile
        private var instance: ReminderRepository? = null

        fun get(context: Context): ReminderRepository =
            instance ?: synchronized(this) {
                instance ?: ReminderRepository(context.applicationContext).also { instance = it }
            }
    }
}
