package com.ttdeveloper.scheduler.services

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.ttdeveloper.scheduler.utils.DatabaseHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.joda.time.DateTime

data class ScheduleRow(
    val id: Int,
    val title: String,
    val description: String?,
    val startTime: DateTime,
    val endTime: DateTime,
    val notified: Boolean
)

class ScheduleService(private val context: Context) {
    private val dbHelper = DatabaseHelper(context)

    fun createSchedule(title: String, description: String?, startTime: DateTime, endTime: DateTime): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("title", title)
            put("description", description)
            put("start_time", startTime.millis)
            put("end_time", endTime.millis)
            put("notified", 0)
        }
        return db.insert("schedule", null, values)
    }

    fun readSchedules(): List<ScheduleRow> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "schedule",
            null,
            null,
            null,
            null,
            null,
            "start_time DESC"
        )
        return cursor.use { generateSequence { if (it.moveToNext()) it else null }.map { it.toScheduleRow() }.toList() }
    }

    fun updateSchedule(id: Int, title: String, description: String?, startTime: DateTime, endTime: DateTime) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("title", title)
            put("description", description)
            put("start_time", startTime.millis)
            put("end_time", endTime.millis)
        }
        db.update("schedule", values, "id = ?", arrayOf(id.toString()))
    }

    fun deleteSchedule(id: Int) {
        val db = dbHelper.writableDatabase
        db.delete("schedule", "id = ?", arrayOf(id.toString()))
    }

    private fun Cursor.toScheduleRow(): ScheduleRow {
        return ScheduleRow(
            id = getInt(getColumnIndexOrThrow("id")),
            title = getString(getColumnIndexOrThrow("title")),
            description = getString(getColumnIndexOrThrow("description")),
            startTime = DateTime(getLong(getColumnIndexOrThrow("start_time"))),
            endTime = DateTime(getLong(getColumnIndexOrThrow("end_time"))),
            notified = getInt(getColumnIndexOrThrow("notified")) == 1
        )
    }

    fun checkForNotifications() {
        val currentTime = DateTime.now()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "schedule",
            null,
            "start_time = ? AND notified = 0",
            arrayOf(currentTime.millis.toString()),
            null,
            null,
            null
        )
        cursor.use { 
            generateSequence { if (it.moveToNext()) it else null }.forEach { 
                val id = it.getInt(it.getColumnIndexOrThrow("id"))
                val title = it.getString(it.getColumnIndexOrThrow("title"))
                val description = it.getString(it.getColumnIndexOrThrow("description"))

                println("🔔 Notification: '$title' - $description")

                // Mark as notified
                val values = ContentValues().apply {
                    put("notified", 1)
                }
                db.update("schedule", values, "id = ?", arrayOf(id.toString()))
            }
        }
    }
}

// Start notification service using the ScheduleService instance
fun ScheduleService.startNotificationService() = runBlocking {
    while (true) {
        checkForNotifications()
        delay(60000) // Check every minute
    }
}
