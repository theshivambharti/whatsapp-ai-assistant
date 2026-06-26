package com.shivam.whatsappai.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val id: Long = 0,
    val timestamp: String,
    val type: String,
    val message: String
)

class LogDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "whatsapp_logs.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_LOGS = "logs"
        private const val KEY_ID = "id"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val KEY_TYPE = "type"
        private const val KEY_MESSAGE = "message"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE " + TABLE_LOGS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TIMESTAMP + " TEXT,"
                + KEY_TYPE + " TEXT,"
                + KEY_MESSAGE + " TEXT" + ")")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOGS")
        onCreate(db)
    }

    fun addLog(type: String, message: String) {
        try {
            val db = this.writableDatabase
            val values = ContentValues().apply {
                put(KEY_TIMESTAMP, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                put(KEY_TYPE, type)
                put(KEY_MESSAGE, message)
            }
            db.insert(TABLE_LOGS, null, values)
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAllLogs(): List<LogEntry> {
        val logs = mutableListOf<LogEntry>()
        val selectQuery = "SELECT * FROM $TABLE_LOGS ORDER BY $KEY_ID DESC LIMIT 200"
        try {
            val db = this.readableDatabase
            val cursor = db.rawQuery(selectQuery, null)
            if (cursor.moveToFirst()) {
                do {
                    val log = LogEntry(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                        timestamp = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TIMESTAMP)),
                        type = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TYPE)),
                        message = cursor.getString(cursor.getColumnIndexOrThrow(KEY_MESSAGE))
                    )
                    logs.add(log)
                } while (cursor.moveToNext())
            }
            cursor.close()
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return logs
    }

    fun clearAllLogs() {
        try {
            val db = this.writableDatabase
            db.delete(TABLE_LOGS, null, null)
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLastLogMessage(type: String): String {
        var lastMsg = "None"
        val selectQuery = "SELECT $KEY_TIMESTAMP, $KEY_MESSAGE FROM $TABLE_LOGS WHERE $KEY_TYPE = ? ORDER BY $KEY_ID DESC LIMIT 1"
        try {
            val db = this.readableDatabase
            val cursor = db.rawQuery(selectQuery, arrayOf(type))
            if (cursor.moveToFirst()) {
                val timestamp = cursor.getString(0)
                val message = cursor.getString(1)
                lastMsg = "[$timestamp] $message"
            }
            cursor.close()
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return lastMsg
    }

    fun getLogCount(type: String): Int {
        var count = 0
        val selectQuery = "SELECT COUNT(*) FROM $TABLE_LOGS WHERE $KEY_TYPE = ?"
        try {
            val db = this.readableDatabase
            val cursor = db.rawQuery(selectQuery, arrayOf(type))
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return count
    }

    fun getLogCountContaining(type: String, keyword: String): Int {
        var count = 0
        val selectQuery = "SELECT COUNT(*) FROM $TABLE_LOGS WHERE $KEY_TYPE = ? AND $KEY_MESSAGE LIKE ?"
        try {
            val db = this.readableDatabase
            val cursor = db.rawQuery(selectQuery, arrayOf(type, "%$keyword%"))
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return count
    }
}
