package com.example.chaturbateclient.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class CatalogMetadata(
    val fetchedAtMs: Long,
    val complete: Boolean,
    val successfulFeeds: Int,
    val totalFeeds: Int,
    val successfulPages: Int,
    val failedPages: Int,
    val diagnostics: String?
)

class RoomCatalogDatabase(context: Context) :
    SQLiteOpenHelper(context, "room_catalog.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE rooms (
                username TEXT PRIMARY KEY COLLATE NOCASE,
                viewers INTEGER NOT NULL,
                subject TEXT NOT NULL,
                gender TEXT NOT NULL,
                image_url TEXT NOT NULL,
                location TEXT NOT NULL,
                country TEXT NOT NULL,
                languages TEXT NOT NULL,
                tags TEXT NOT NULL,
                observed_at INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_rooms_subject ON rooms(subject)")
        db.execSQL("CREATE INDEX idx_rooms_gender ON rooms(gender)")
        db.execSQL("CREATE INDEX idx_rooms_country ON rooms(country)")
        db.execSQL("""
            CREATE TABLE metadata (
                id INTEGER PRIMARY KEY CHECK(id = 1),
                fetched_at INTEGER NOT NULL,
                complete INTEGER NOT NULL,
                successful_feeds INTEGER NOT NULL,
                total_feeds INTEGER NOT NULL,
                successful_pages INTEGER NOT NULL,
                failed_pages INTEGER NOT NULL,
                diagnostics TEXT
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE rooms ADD COLUMN observed_at INTEGER NOT NULL DEFAULT 0")
        }
    }

    fun readRooms(): List<ApiRoom> {
        val result = mutableListOf<ApiRoom>()
        readableDatabase.query("rooms", null, null, null, null, null, "username COLLATE NOCASE").use { c ->
            val username = c.getColumnIndexOrThrow("username")
            val viewers = c.getColumnIndexOrThrow("viewers")
            val subject = c.getColumnIndexOrThrow("subject")
            val gender = c.getColumnIndexOrThrow("gender")
            val image = c.getColumnIndexOrThrow("image_url")
            val location = c.getColumnIndexOrThrow("location")
            val country = c.getColumnIndexOrThrow("country")
            val languages = c.getColumnIndexOrThrow("languages")
            val tags = c.getColumnIndexOrThrow("tags")
            while (c.moveToNext()) {
                result += ApiRoom(
                    username = c.getString(username),
                    viewers = c.getInt(viewers),
                    subject = c.getString(subject),
                    category = c.getString(subject),
                    imageUrl = c.getString(image),
                    gender = c.getString(gender),
                    location = c.getString(location),
                    country = c.getString(country),
                    spokenLanguages = c.getString(languages),
                    tags = c.getString(tags).split("\u001f").filter(String::isNotBlank),
                    observedAtMs = c.getLong(c.getColumnIndexOrThrow("observed_at"))
                )
            }
        }
        return result
    }

    fun readMetadata(): CatalogMetadata? {
        readableDatabase.query("metadata", null, "id = 1", null, null, null, null).use { c ->
            if (!c.moveToFirst()) return null
            return CatalogMetadata(
                fetchedAtMs = c.getLong(c.getColumnIndexOrThrow("fetched_at")),
                complete = c.getInt(c.getColumnIndexOrThrow("complete")) != 0,
                successfulFeeds = c.getInt(c.getColumnIndexOrThrow("successful_feeds")),
                totalFeeds = c.getInt(c.getColumnIndexOrThrow("total_feeds")),
                successfulPages = c.getInt(c.getColumnIndexOrThrow("successful_pages")),
                failedPages = c.getInt(c.getColumnIndexOrThrow("failed_pages")),
                diagnostics = c.getString(c.getColumnIndexOrThrow("diagnostics"))
            )
        }
    }

    fun replaceSnapshot(rooms: List<ApiRoom>, metadata: CatalogMetadata) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("rooms", null, null)
            rooms.forEach { room ->
                db.insertOrThrow("rooms", null, ContentValues().apply {
                    put("username", room.username)
                    put("viewers", room.viewers)
                    put("subject", room.subject)
                    put("gender", room.gender)
                    put("image_url", room.imageUrl)
                    put("location", room.location)
                    put("country", room.country)
                    put("languages", room.spokenLanguages)
                    put("tags", room.tags.joinToString("\u001f"))
                    put("observed_at", room.observedAtMs)
                })
            }
            db.delete("metadata", "id = 1", null)
            db.insertOrThrow("metadata", null, ContentValues().apply {
                put("id", 1)
                put("fetched_at", metadata.fetchedAtMs)
                put("complete", if (metadata.complete) 1 else 0)
                put("successful_feeds", metadata.successfulFeeds)
                put("total_feeds", metadata.totalFeeds)
                put("successful_pages", metadata.successfulPages)
                put("failed_pages", metadata.failedPages)
                put("diagnostics", metadata.diagnostics)
            })
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun upsertSnapshotRooms(rooms: List<ApiRoom>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            rooms.forEach { room ->
                db.insertWithOnConflict("rooms", null, ContentValues().apply {
                    put("username", room.username)
                    put("viewers", room.viewers)
                    put("subject", room.subject)
                    put("gender", room.gender)
                    put("image_url", room.imageUrl)
                    put("location", room.location)
                    put("country", room.country)
                    put("languages", room.spokenLanguages)
                    put("tags", room.tags.joinToString("\u001f"))
                    put("observed_at", room.observedAtMs)
                }, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
