package com.ttdeveloper.scheduler.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "scheduler.db"
        private const val DATABASE_VERSION = 1

        // Table creation SQL statement
        private const val CREATE_SCHEDULE_TABLE = """
            CREATE TABLE IF NOT EXISTS schedule (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT,
                start_time INTEGER NOT NULL,
                end_time INTEGER NOT NULL,
                notified INTEGER DEFAULT 0
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_SCHEDULE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // For now, just drop and recreate
        db.execSQL("DROP TABLE IF EXISTS schedule")
        onCreate(db)
    }
}