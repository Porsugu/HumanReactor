package com.example.humanreactor.databases

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class QuizDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "quiz.db"
        private const val DATABASE_VERSION = 1

        // Quiz Categories Table
        private const val TABLE_QUIZ_CATEGORIES = "quiz_categories"
        private const val COLUMN_CATEGORY_ID = "id"
        private const val COLUMN_CATEGORY_NAME = "name"
        private const val COLUMN_CATEGORY_DESCRIPTION = "description"

        // Quiz Records Table
        private const val TABLE_QUIZ_RECORDS = "quiz_records"
        private const val COLUMN_QUIZ_ID = "id"
        private const val COLUMN_QUIZ_CATEGORY_ID = "category_id"
        private const val COLUMN_QUIZ_AVG_TIME = "avg_answer_time"
        private const val COLUMN_QUIZ_ACCURACY = "accuracy"
        private const val COLUMN_QUIZ_TOTAL_QUESTIONS = "total_questions"
        private const val COLUMN_QUIZ_TIMESTAMP = "timestamp"

        // Quiz Questions Table
        private const val TABLE_QUIZ_QUESTIONS = "quiz_questions"
        private const val COLUMN_QUESTION_ID = "id"
        private const val COLUMN_QUESTION_TEXT = "question_text"
        private const val COLUMN_QUESTION_CATEGORY_ID = "category_id"
        private const val COLUMN_QUESTION_DIFFICULTY = "difficulty"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create Quiz Categories Table
        val createCategoriesTable = """
            CREATE TABLE $TABLE_QUIZ_CATEGORIES (
                $COLUMN_CATEGORY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CATEGORY_NAME TEXT NOT NULL UNIQUE,
                $COLUMN_CATEGORY_DESCRIPTION TEXT
            )
        """.trimIndent()

        // Create Quiz Records Table
        val createQuizRecordsTable = """
            CREATE TABLE $TABLE_QUIZ_RECORDS (
                $COLUMN_QUIZ_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_QUIZ_CATEGORY_ID INTEGER NOT NULL,
                $COLUMN_QUIZ_AVG_TIME REAL NOT NULL,
                $COLUMN_QUIZ_ACCURACY REAL NOT NULL,
                $COLUMN_QUIZ_TOTAL_QUESTIONS INTEGER NOT NULL,
                $COLUMN_QUIZ_TIMESTAMP INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_QUIZ_CATEGORY_ID) REFERENCES $TABLE_QUIZ_CATEGORIES($COLUMN_CATEGORY_ID)
            )
        """.trimIndent()

        // Create Quiz Questions Table
        val createQuestionsTable = """
            CREATE TABLE $TABLE_QUIZ_QUESTIONS (
                $COLUMN_QUESTION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_QUESTION_TEXT TEXT NOT NULL,
                $COLUMN_QUESTION_CATEGORY_ID INTEGER NOT NULL,
                $COLUMN_QUESTION_DIFFICULTY INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_QUESTION_CATEGORY_ID) REFERENCES $TABLE_QUIZ_CATEGORIES($COLUMN_CATEGORY_ID)
            )
        """.trimIndent()

        // Execute SQL statements
        db.execSQL(createCategoriesTable)
        db.execSQL(createQuizRecordsTable)
        db.execSQL(createQuestionsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database version upgrades here
        if (oldVersion < 2) {
            // Add upgrade logic if needed
        }
    }


    // ---- Quiz Category Operations ----

    fun addQuizCategory(categoryName: String, description: String = ""): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CATEGORY_NAME, categoryName)
            put(COLUMN_CATEGORY_DESCRIPTION, description)
        }

        val id = db.insert(TABLE_QUIZ_CATEGORIES, null, values)
        db.close()
        return id
    }

    fun getAllQuizCategories(): List<QuizCategory> {
        val categories = mutableListOf<QuizCategory>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_QUIZ_CATEGORIES"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_NAME))
                val description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_DESCRIPTION))
                categories.add(QuizCategory(id, name, description))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return categories
    }

    fun getQuizCategory(categoryId: Int): QuizCategory? {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_QUIZ_CATEGORIES WHERE $COLUMN_CATEGORY_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(categoryId.toString()))

        var category: QuizCategory? = null
        if (cursor.moveToFirst()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_NAME))
            val description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_DESCRIPTION))
            category = QuizCategory(id, name, description)
        }

        cursor.close()
        db.close()
        return category
    }

    fun getCategoryIdByName(categoryName: String): Int? {
        val db = this.readableDatabase
        val query = "SELECT $COLUMN_CATEGORY_ID FROM $TABLE_QUIZ_CATEGORIES WHERE $COLUMN_CATEGORY_NAME = ?"
        val cursor = db.rawQuery(query, arrayOf(categoryName))

        var categoryId: Int? = null
        if (cursor.moveToFirst()) {
            categoryId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_ID))
        }

        cursor.close()
        db.close()
        return categoryId
    }

    fun updateQuizCategory(category: QuizCategory): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CATEGORY_NAME, category.name)
            put(COLUMN_CATEGORY_DESCRIPTION, category.description)
        }

        val affectedRows = db.update(
            TABLE_QUIZ_CATEGORIES,
            values,
            "$COLUMN_CATEGORY_ID = ?",
            arrayOf(category.id.toString())
        )
        db.close()
        return affectedRows
    }

    fun deleteQuizCategory(categoryId: Int): Boolean {
        val db = this.writableDatabase

        // Delete related questions
        db.delete(
            TABLE_QUIZ_QUESTIONS,
            "$COLUMN_QUESTION_CATEGORY_ID = ?",
            arrayOf(categoryId.toString())
        )

        // Delete related quiz records
        db.delete(
            TABLE_QUIZ_RECORDS,
            "$COLUMN_QUIZ_CATEGORY_ID = ?",
            arrayOf(categoryId.toString())
        )

        // Delete the category
        val result = db.delete(
            TABLE_QUIZ_CATEGORIES,
            "$COLUMN_CATEGORY_ID = ?",
            arrayOf(categoryId.toString())
        )

        db.close()
        return result > 0
    }

    // ---- Quiz Record Operations ----

    fun addQuizRecord(quizRecord: QuizRecord): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_QUIZ_CATEGORY_ID, quizRecord.categoryId)
            put(COLUMN_QUIZ_AVG_TIME, quizRecord.avgAnswerTime)
            put(COLUMN_QUIZ_ACCURACY, quizRecord.accuracy)
            put(COLUMN_QUIZ_TOTAL_QUESTIONS, quizRecord.totalQuestions)
            put(COLUMN_QUIZ_TIMESTAMP, System.currentTimeMillis())
        }

        val id = db.insert(TABLE_QUIZ_RECORDS, null, values)
        db.close()
        return id
    }

    fun getQuizRecordsByCategory(categoryId: Int, limit: Int = 10): List<QuizRecord> {
        val records = mutableListOf<QuizRecord>()
        val db = this.readableDatabase
        val query = """
            SELECT * FROM $TABLE_QUIZ_RECORDS 
            WHERE $COLUMN_QUIZ_CATEGORY_ID = ? 
            ORDER BY $COLUMN_QUIZ_TIMESTAMP DESC 
            LIMIT ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(categoryId.toString(), limit.toString()))

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QUIZ_ID))
                val catId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QUIZ_CATEGORY_ID))
                val avgTime = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_QUIZ_AVG_TIME))
                val accuracy = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_QUIZ_ACCURACY))
                val totalQuestions = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QUIZ_TOTAL_QUESTIONS))
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_QUIZ_TIMESTAMP))

                records.add(QuizRecord(id, catId, avgTime, accuracy, totalQuestions, timestamp))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return records
    }

    fun getAveragePerformanceByCategory(categoryId: Int): Pair<Double, Double>? {
        val db = this.readableDatabase
        val query = """
            SELECT AVG($COLUMN_QUIZ_AVG_TIME) as avg_time, 
                   AVG($COLUMN_QUIZ_ACCURACY) as avg_accuracy 
            FROM $TABLE_QUIZ_RECORDS 
            WHERE $COLUMN_QUIZ_CATEGORY_ID = ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(categoryId.toString()))
        var result: Pair<Double, Double>? = null

        if (cursor.moveToFirst()) {
            val avgTime = cursor.getDouble(cursor.getColumnIndexOrThrow("avg_time"))
            val avgAccuracy = cursor.getDouble(cursor.getColumnIndexOrThrow("avg_accuracy"))
            result = Pair(avgTime, avgAccuracy)
        }

        cursor.close()
        db.close()
        return result
    }

    fun deleteQuizRecord(quizId: Int): Boolean {
        val db = this.writableDatabase
        val result = db.delete(
            TABLE_QUIZ_RECORDS,
            "$COLUMN_QUIZ_ID = ?",
            arrayOf(quizId.toString())
        )
        db.close()
        return result > 0
    }

    // ---- Quiz Question Operations ----

    fun addQuestion(question: QuizQuestion): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_QUESTION_TEXT, question.text)
            put(COLUMN_QUESTION_CATEGORY_ID, question.categoryId)
            put(COLUMN_QUESTION_DIFFICULTY, question.difficulty)
        }

        val id = db.insert(TABLE_QUIZ_QUESTIONS, null, values)
        db.close()
        return id
    }

    fun getQuestionsByCategory(categoryId: Int): List<QuizQuestion> {
        val questions = mutableListOf<QuizQuestion>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_QUIZ_QUESTIONS WHERE $COLUMN_QUESTION_CATEGORY_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(categoryId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QUESTION_ID))
                val text = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_QUESTION_TEXT))
                val catId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QUESTION_CATEGORY_ID))
                val difficulty = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QUESTION_DIFFICULTY))

                questions.add(QuizQuestion(id, text, catId, difficulty))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return questions
    }

    fun updateQuestion(question: QuizQuestion): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_QUESTION_TEXT, question.text)
            put(COLUMN_QUESTION_CATEGORY_ID, question.categoryId)
            put(COLUMN_QUESTION_DIFFICULTY, question.difficulty)
        }

        val affectedRows = db.update(
            TABLE_QUIZ_QUESTIONS,
            values,
            "$COLUMN_QUESTION_ID = ?",
            arrayOf(question.id.toString())
        )
        db.close()
        return affectedRows
    }

    fun deleteQuestion(questionId: Int): Boolean {
        val db = this.writableDatabase
        val result = db.delete(
            TABLE_QUIZ_QUESTIONS,
            "$COLUMN_QUESTION_ID = ?",
            arrayOf(questionId.toString())
        )
        db.close()
        return result > 0
    }
}

// Data Classes
data class QuizCategory(
    val id: Int = 0,
    val name: String,
    val description: String = ""
)

data class QuizRecord(
    val id: Int = 0,
    val categoryId: Int,
    val avgAnswerTime: Double,
    val accuracy: Double,
    val totalQuestions: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class QuizQuestion(
    val id: Int = 0,
    val text: String,
    val categoryId: Int,
    val difficulty: Int
)