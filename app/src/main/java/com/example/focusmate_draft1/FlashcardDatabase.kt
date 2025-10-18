package com.example.focusmate_draft1

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Flashcard(val question: String, val answer: String)

class FlashcardDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "flashcards.db"
        private const val DB_VERSION = 1
        private const val TABLE_NAME = "flashcards"
        private const val COLUMN_QUESTION = "question"
        private const val COLUMN_ANSWER = "answer"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = "CREATE TABLE $TABLE_NAME ($COLUMN_QUESTION TEXT PRIMARY KEY, $COLUMN_ANSWER TEXT)"
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun addCard(flashcard: Flashcard) {
        val db = writableDatabase
        val values = ContentValues()
        values.put(COLUMN_QUESTION, flashcard.question)
        values.put(COLUMN_ANSWER, flashcard.answer)
        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    fun updateCard(flashcard: Flashcard) {
        val db = writableDatabase
        val values = ContentValues()
        values.put(COLUMN_ANSWER, flashcard.answer)
        db.update(TABLE_NAME, values, "$COLUMN_QUESTION=?", arrayOf(flashcard.question))
        db.close()
    }

    fun deleteCard(question: String) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_QUESTION=?", arrayOf(question))
        db.close()
    }

    fun getAllCards(): List<Flashcard> {
        val flashcards = mutableListOf<Flashcard>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)
        if (cursor.moveToFirst()) {
            do {
                val question = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_QUESTION))
                val answer = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ANSWER))
                flashcards.add(Flashcard(question, answer))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return flashcards
    }
}
