//package com.example.humanreactor.databases

//import android.content.ContentValues
//import android.content.Context
//import android.database.sqlite.SQLiteDatabase
//import android.database.sqlite.SQLiteOpenHelper
//
//class ActionDatabaseHelper(context: Context) :
//    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
//
//    companion object {
//        private const val DATABASE_NAME = "actions.db"
//        private const val DATABASE_VERSION = 1
//
//        private const val TABLE_CATEGORIES = "categories"
//        private const val COLUMN_CATEGORY_ID = "id"
//        private const val COLUMN_CATEGORY_NAME = "name"
//
//        private const val TABLE_ACTIONS = "actions"
//        private const val COLUMN_ACTION_ID = "id"
//        private const val COLUMN_ACTION_NAME = "name"
//        private const val COLUMN_ACTION_COLOR = "color"
//        private const val COLUMN_ACTION_CATEGORY_ID = "category_id"
//
//        private const val TABLE_PERFORMANCE = "performance"
//        private const val COLUMN_PERFORMANCE_ID = "id"
//        private const val COLUMN_PERFORMANCE_CATEGORY_ID = "category_id"
//        private const val COLUMN_PERFORMANCE_RESPONSE_TIME = "avg_response_time"
//        private const val COLUMN_PERFORMANCE_ACCURACY = "accuracy"
//
//        private const val TABLE_USER_SETTINGS = "user_settings"
//        private const val COLUMN_USER_NAME = "user_name"
//        private const val COLUMN_PREFERRED_LANGUAGE = "preferred_language"
//    }
//
//    override fun onCreate(db: SQLiteDatabase) {
//        val createCategoriesTable = """
//            CREATE TABLE $TABLE_CATEGORIES (
//                $COLUMN_CATEGORY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
//                $COLUMN_CATEGORY_NAME TEXT NOT NULL UNIQUE
//            )
//        """.trimIndent()
//
//        // 创建动作表
//        val createActionsTable = """
//            CREATE TABLE $TABLE_ACTIONS (
//                $COLUMN_ACTION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
//                $COLUMN_ACTION_NAME TEXT NOT NULL,
//                $COLUMN_ACTION_COLOR TEXT NOT NULL,
//                $COLUMN_ACTION_CATEGORY_ID INTEGER NOT NULL,
//                FOREIGN KEY ($COLUMN_ACTION_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($COLUMN_CATEGORY_ID)
//            )
//        """.trimIndent()
//

//        val createPerformanceTable = """
//            CREATE TABLE $TABLE_PERFORMANCE (
//                $COLUMN_PERFORMANCE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
//                $COLUMN_PERFORMANCE_CATEGORY_ID INTEGER NOT NULL,
//                $COLUMN_PERFORMANCE_RESPONSE_TIME REAL NOT NULL,
//                $COLUMN_PERFORMANCE_ACCURACY REAL NOT NULL,
//                FOREIGN KEY ($COLUMN_PERFORMANCE_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($COLUMN_CATEGORY_ID)
//            )
//        """.trimIndent()
//

//        val createUserSettingsTable = """
//            CREATE TABLE $TABLE_USER_SETTINGS (
//                $COLUMN_USER_NAME TEXT,
//                $COLUMN_PREFERRED_LANGUAGE TEXT
//            )
//        """.trimIndent()
//
//        db.execSQL(createCategoriesTable)
//        db.execSQL(createActionsTable)
//        db.execSQL(createPerformanceTable)
//        db.execSQL(createUserSettingsTable)
//    }
//
//    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
//        db.execSQL("DROP TABLE IF EXISTS $TABLE_ACTIONS")
//        db.execSQL("DROP TABLE IF EXISTS $TABLE_PERFORMANCE")
//        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
//        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER_SETTINGS")
//        onCreate(db)
//    }
//
//    fun addCategory(categoryName: String): Long {
//        val db = this.writableDatabase
//        val values = ContentValues().apply {
//            put(COLUMN_CATEGORY_NAME, categoryName)
//        }
//
//        val id = db.insert(TABLE_CATEGORIES, null, values)
//        db.close()
//        return id
//    }
//
//    fun getAllCategories(): List<Category> {
//        val categories = mutableListOf<Category>()
//        val db = this.readableDatabase
//        val query = "SELECT * FROM $TABLE_CATEGORIES"
//        val cursor = db.rawQuery(query, null)
//
//        if (cursor.moveToFirst()) {
//            do {
//                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_ID))
//                val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_NAME))
//                categories.add(Category(id, name))
//            } while (cursor.moveToNext())
//        }
//
//        cursor.close()
//        db.close()
//        return categories
//    }
//
//    fun addAction(action: Action): Long {
//        val db = this.writableDatabase
//        val values = ContentValues().apply {
//            put(COLUMN_ACTION_NAME, action.name)
//            put(COLUMN_ACTION_COLOR, action.color)  // 直接存储Int值
//            put(COLUMN_ACTION_CATEGORY_ID, action.categoryId)
//        }
//
//        val id = db.insert(TABLE_ACTIONS, null, values)
//        db.close()
//        return id
//    }
//
//    fun getActionsByCategory(categoryId: Int): List<Action> {
//        val actions = mutableListOf<Action>()
//        val db = this.readableDatabase
//        val query = "SELECT * FROM $TABLE_ACTIONS WHERE $COLUMN_ACTION_CATEGORY_ID = ?"
//        val cursor = db.rawQuery(query, arrayOf(categoryId.toString()))
//
//        if (cursor.moveToFirst()) {
//            do {
//                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTION_ID))
//                val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACTION_NAME))
//                val color = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTION_COLOR))  // 读取为Int
//                val catId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTION_CATEGORY_ID))
//                actions.add(Action(id, name, color, catId))
//            } while (cursor.moveToNext())
//        }
//
//        cursor.close()
//        db.close()
//        return actions
//    }
//
//    fun addOrUpdatePerformance(performance: Performance): Long {
//        val db = this.writableDatabase
//
//        val query = "SELECT * FROM $TABLE_PERFORMANCE WHERE $COLUMN_PERFORMANCE_CATEGORY_ID = ?"
//        val cursor = db.rawQuery(query, arrayOf(performance.categoryId.toString()))
//        val exists = cursor.count > 0
//        cursor.close()
//
//        val values = ContentValues().apply {
//            put(COLUMN_PERFORMANCE_CATEGORY_ID, performance.categoryId)
//            put(COLUMN_PERFORMANCE_RESPONSE_TIME, performance.avgResponseTime)
//            put(COLUMN_PERFORMANCE_ACCURACY, performance.accuracy)
//        }
//
//        val id: Long
//        if (exists) {
//            db.update(
//                TABLE_PERFORMANCE,
//                values,
//                "$COLUMN_PERFORMANCE_CATEGORY_ID = ?",
//                arrayOf(performance.categoryId.toString())
//            )
//            val idQuery = "SELECT $COLUMN_PERFORMANCE_ID FROM $TABLE_PERFORMANCE WHERE $COLUMN_PERFORMANCE_CATEGORY_ID = ?"
//            val idCursor = db.rawQuery(idQuery, arrayOf(performance.categoryId.toString()))
//            idCursor.moveToFirst()
//            id = idCursor.getLong(idCursor.getColumnIndexOrThrow(COLUMN_PERFORMANCE_ID))
//            idCursor.close()
//        } else {
//            id = db.insert(TABLE_PERFORMANCE, null, values)
//        }
//
//        db.close()
//        return id
//    }
//
//    fun getPerformanceByCategory(categoryId: Int): Performance? {
//        val db = this.readableDatabase
//        val query = "SELECT * FROM $TABLE_PERFORMANCE WHERE $COLUMN_PERFORMANCE_CATEGORY_ID = ?"
//        val cursor = db.rawQuery(query, arrayOf(categoryId.toString()))
//
//        var performance: Performance? = null
//        if (cursor.moveToFirst()) {
//            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PERFORMANCE_ID))
//            val catId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PERFORMANCE_CATEGORY_ID))
//            val responseTime = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PERFORMANCE_RESPONSE_TIME))
//            val accuracy = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PERFORMANCE_ACCURACY))
//            performance = Performance(id, catId, responseTime, accuracy)
//        }
//
//        cursor.close()
//        db.close()
//        return performance
//    }
//
//    fun updateCategory(category: Category): Int {
//        val db = this.writableDatabase
//        val values = ContentValues().apply {
//            put(COLUMN_CATEGORY_NAME, category.name)
//        }
//
//        val affectedRows = db.update(
//            TABLE_CATEGORIES,
//            values,
//            "$COLUMN_CATEGORY_ID = ?",
//            arrayOf(category.id.toString())
//        )
//        db.close()
//        return affectedRows
//    }
//
//    fun deleteCategory(categoryId: Int): Boolean {
//        val db = this.writableDatabase
//
//        db.delete(
//            TABLE_ACTIONS,
//            "$COLUMN_ACTION_CATEGORY_ID = ?",
//            arrayOf(categoryId.toString())
//        )
//
//        db.delete(
//            TABLE_PERFORMANCE,
//            "$COLUMN_PERFORMANCE_CATEGORY_ID = ?",
//            arrayOf(categoryId.toString())
//        )
//
//        val result = db.delete(
//            TABLE_CATEGORIES,
//            "$COLUMN_CATEGORY_ID = ?",
//            arrayOf(categoryId.toString())
//        )
//
//        db.close()
//        return result > 0
//    }
//
//    fun updateAction(action: Action): Int {
//        val db = this.writableDatabase
//        val values = ContentValues().apply {
//            put(COLUMN_ACTION_NAME, action.name)
//            put(COLUMN_ACTION_COLOR, action.color)  // 直接存储Int值
//            put(COLUMN_ACTION_CATEGORY_ID, action.categoryId)
//        }
//
//        val affectedRows = db.update(
//            TABLE_ACTIONS,
//            values,
//            "$COLUMN_ACTION_ID = ?",
//            arrayOf(action.id.toString())
//        )
//        db.close()
//        return affectedRows
//    }
//
//    fun deleteAction(actionId: Int): Boolean {
//        val db = this.writableDatabase
//        val result = db.delete(
//            TABLE_ACTIONS,
//            "$COLUMN_ACTION_ID = ?",
//            arrayOf(actionId.toString())
//        )
//        db.close()
//        return result > 0
//    }
//
//    fun saveUserSettings(userName: String, preferredLanguage: String): Boolean {
//        val db = this.writableDatabase
//
//        db.delete(TABLE_USER_SETTINGS, null, null)
//
//        val values = ContentValues().apply {
//            put(COLUMN_USER_NAME, userName)
//            put(COLUMN_PREFERRED_LANGUAGE, preferredLanguage)
//        }
//
//        val id = db.insert(TABLE_USER_SETTINGS, null, values)
//        db.close()
//        return id != -1L
//    }
//
//    fun getUserSettings(): UserSettings? {
//        val db = this.readableDatabase
//        val query = "SELECT * FROM $TABLE_USER_SETTINGS LIMIT 1"
//        val cursor = db.rawQuery(query, null)
//
//        var userSettings: UserSettings? = null
//        if (cursor.moveToFirst()) {
//            val userName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_NAME))
//            val language = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PREFERRED_LANGUAGE))
//            userSettings = UserSettings(userName, language)
//        }
//
//        cursor.close()
//        db.close()
//        return userSettings
//    }
//}
//
//data class Category(
//    val id: Int = 0,
//    val name: String
//)
//
//data class Action(
//    val id: Int = 0,
//    val name: String,
//    val color: Int,
//    val categoryId: Int
//)
//
//data class Performance(
//    val id: Int = 0,
//    val categoryId: Int,
//    val avgResponseTime: Double,
//    val accuracy: Double
//)
//
//data class UserSettings(
//    val userName: String,
//    val preferredLanguage: String
//)

package com.example.humanreactor.databases

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Date

class ActionDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "actions.db"
        private const val DATABASE_VERSION = 2

        private const val TABLE_CATEGORIES = "categories"
        private const val COLUMN_CATEGORY_ID = "id"
        private const val COLUMN_CATEGORY_NAME = "name"

        private const val TABLE_ACTIONS = "actions"
        private const val COLUMN_ACTION_ID = "id"
        private const val COLUMN_ACTION_NAME = "name"
        private const val COLUMN_ACTION_COLOR = "color"
        private const val COLUMN_ACTION_CATEGORY_ID = "category_id"

        private const val TABLE_PERFORMANCE = "performance"
        private const val COLUMN_PERFORMANCE_ID = "id"
        private const val COLUMN_PERFORMANCE_CATEGORY_ID = "category_id"
        private const val COLUMN_PERFORMANCE_RESPONSE_TIME = "avg_response_time"
        private const val COLUMN_PERFORMANCE_ACCURACY = "accuracy"

        private const val TABLE_PERFORMANCE_HISTORY = "performance_history"
        private const val COLUMN_HISTORY_ID = "id"
        private const val COLUMN_HISTORY_CATEGORY_ID = "category_id"
        private const val COLUMN_HISTORY_RESPONSE_TIME = "response_time"
        private const val COLUMN_HISTORY_ACCURACY = "accuracy"
        private const val COLUMN_HISTORY_TIMESTAMP = "timestamp"

        private const val TABLE_USER_SETTINGS = "user_settings"
        private const val COLUMN_USER_NAME = "user_name"
        private const val COLUMN_PREFERRED_LANGUAGE = "preferred_language"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createCategoriesTable = """
            CREATE TABLE $TABLE_CATEGORIES (
                $COLUMN_CATEGORY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CATEGORY_NAME TEXT NOT NULL UNIQUE
            )
        """.trimIndent()

        val createActionsTable = """
            CREATE TABLE $TABLE_ACTIONS (
                $COLUMN_ACTION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ACTION_NAME TEXT NOT NULL,
                $COLUMN_ACTION_COLOR TEXT NOT NULL,
                $COLUMN_ACTION_CATEGORY_ID INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_ACTION_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($COLUMN_CATEGORY_ID)
            )
        """.trimIndent()

        val createPerformanceTable = """
            CREATE TABLE $TABLE_PERFORMANCE (
                $COLUMN_PERFORMANCE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PERFORMANCE_CATEGORY_ID INTEGER NOT NULL,
                $COLUMN_PERFORMANCE_RESPONSE_TIME REAL NOT NULL,
                $COLUMN_PERFORMANCE_ACCURACY REAL NOT NULL,
                FOREIGN KEY ($COLUMN_PERFORMANCE_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($COLUMN_CATEGORY_ID)
            )
        """.trimIndent()

        val createPerformanceHistoryTable = """
            CREATE TABLE $TABLE_PERFORMANCE_HISTORY (
                $COLUMN_HISTORY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_HISTORY_CATEGORY_ID INTEGER NOT NULL,
                $COLUMN_HISTORY_RESPONSE_TIME REAL NOT NULL,
                $COLUMN_HISTORY_ACCURACY REAL NOT NULL,
                $COLUMN_HISTORY_TIMESTAMP INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_HISTORY_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($COLUMN_CATEGORY_ID)
            )
        """.trimIndent()

        val createUserSettingsTable = """
            CREATE TABLE $TABLE_USER_SETTINGS (
                $COLUMN_USER_NAME TEXT,
                $COLUMN_PREFERRED_LANGUAGE TEXT
            )
        """.trimIndent()

        db.execSQL(createCategoriesTable)
        db.execSQL(createActionsTable)
        db.execSQL(createPerformanceTable)
        db.execSQL(createPerformanceHistoryTable)  // 添加新表
        db.execSQL(createUserSettingsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            val createPerformanceHistoryTable = """
                CREATE TABLE IF NOT EXISTS $TABLE_PERFORMANCE_HISTORY (
                    $COLUMN_HISTORY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_HISTORY_CATEGORY_ID INTEGER NOT NULL,
                    $COLUMN_HISTORY_RESPONSE_TIME REAL NOT NULL,
                    $COLUMN_HISTORY_ACCURACY REAL NOT NULL,
                    $COLUMN_HISTORY_TIMESTAMP INTEGER NOT NULL,
                    FOREIGN KEY ($COLUMN_HISTORY_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($COLUMN_CATEGORY_ID)
                )
            """.trimIndent()
            db.execSQL(createPerformanceHistoryTable)
        }
    }

    fun addPerformanceRecord(categoryId: Int, responseTime: Double, accuracy: Double): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_HISTORY_CATEGORY_ID, categoryId)
            put(COLUMN_HISTORY_RESPONSE_TIME, responseTime)
            put(COLUMN_HISTORY_ACCURACY, accuracy)
            put(COLUMN_HISTORY_TIMESTAMP, System.currentTimeMillis())
        }

        val id = db.insert(TABLE_PERFORMANCE_HISTORY, null, values)

        updateAveragePerformance(categoryId)

        db.close()
        return id
    }

    fun getRecentPerformanceRecords(categoryId: Int, limit: Int = 5): List<PerformanceRecord> {
        val records = mutableListOf<PerformanceRecord>()
        val db = this.readableDatabase
        val query = """
            SELECT * FROM $TABLE_PERFORMANCE_HISTORY 
            WHERE $COLUMN_HISTORY_CATEGORY_ID = ? 
            ORDER BY $COLUMN_HISTORY_TIMESTAMP DESC 
            LIMIT ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(categoryId.toString(), limit.toString()))

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_ID))
                val catId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_CATEGORY_ID))
                val responseTime = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_RESPONSE_TIME))
                val accuracy = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_ACCURACY))
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_TIMESTAMP))

                records.add(PerformanceRecord(id, catId, responseTime, accuracy, timestamp))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return records
    }

    private fun updateAveragePerformance(categoryId: Int) {
        val db = this.writableDatabase
        val query = """
            SELECT AVG($COLUMN_HISTORY_RESPONSE_TIME) as avg_response_time, 
                   AVG($COLUMN_HISTORY_ACCURACY) as avg_accuracy 
            FROM $TABLE_PERFORMANCE_HISTORY 
            WHERE $COLUMN_HISTORY_CATEGORY_ID = ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(categoryId.toString()))

        if (cursor.moveToFirst()) {
            val avgResponseTime = cursor.getDouble(cursor.getColumnIndexOrThrow("avg_response_time"))
            val avgAccuracy = cursor.getDouble(cursor.getColumnIndexOrThrow("avg_accuracy"))

            val performance = Performance(
                categoryId = categoryId,
                avgResponseTime = avgResponseTime,
                accuracy = avgAccuracy
            )

            addOrUpdatePerformance(performance)
        }

        cursor.close()
        db.close()
    }

    fun addCategory(categoryName: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CATEGORY_NAME, categoryName)
        }

        val id = db.insert(TABLE_CATEGORIES, null, values)
        db.close()
        return id
    }

    fun getAllCategories(): List<Category> {
        val categories = mutableListOf<Category>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_CATEGORIES"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_NAME))
                categories.add(Category(id, name))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return categories
    }

    fun addAction(action: Action): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ACTION_NAME, action.name)
            put(COLUMN_ACTION_COLOR, action.color)  // 直接存储Int值
            put(COLUMN_ACTION_CATEGORY_ID, action.categoryId)
        }

        val id = db.insert(TABLE_ACTIONS, null, values)
        db.close()
        return id
    }

    fun getActionsByCategory(categoryId: Int): List<Action> {
        val actions = mutableListOf<Action>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_ACTIONS WHERE $COLUMN_ACTION_CATEGORY_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(categoryId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTION_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACTION_NAME))
                val color = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTION_COLOR))  // 读取为Int
                val catId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTION_CATEGORY_ID))
                actions.add(Action(id, name, color, catId))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return actions
    }

    fun addOrUpdatePerformance(performance: Performance): Long {
        val db = this.writableDatabase

        val query = "SELECT * FROM $TABLE_PERFORMANCE WHERE $COLUMN_PERFORMANCE_CATEGORY_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(performance.categoryId.toString()))
        val exists = cursor.count > 0
        cursor.close()

        val values = ContentValues().apply {
            put(COLUMN_PERFORMANCE_CATEGORY_ID, performance.categoryId)
            put(COLUMN_PERFORMANCE_RESPONSE_TIME, performance.avgResponseTime)
            put(COLUMN_PERFORMANCE_ACCURACY, performance.accuracy)
        }

        val id: Long
        if (exists) {
            db.update(
                TABLE_PERFORMANCE,
                values,
                "$COLUMN_PERFORMANCE_CATEGORY_ID = ?",
                arrayOf(performance.categoryId.toString())
            )
            val idQuery = "SELECT $COLUMN_PERFORMANCE_ID FROM $TABLE_PERFORMANCE WHERE $COLUMN_PERFORMANCE_CATEGORY_ID = ?"
            val idCursor = db.rawQuery(idQuery, arrayOf(performance.categoryId.toString()))
            idCursor.moveToFirst()
            id = idCursor.getLong(idCursor.getColumnIndexOrThrow(COLUMN_PERFORMANCE_ID))
            idCursor.close()
        } else {
            // 插入新记录
            id = db.insert(TABLE_PERFORMANCE, null, values)
        }

        db.close()
        return id
    }

    fun getPerformanceByCategory(categoryId: Int): Performance? {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_PERFORMANCE WHERE $COLUMN_PERFORMANCE_CATEGORY_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(categoryId.toString()))

        var performance: Performance? = null
        if (cursor.moveToFirst()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PERFORMANCE_ID))
            val catId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PERFORMANCE_CATEGORY_ID))
            val responseTime = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PERFORMANCE_RESPONSE_TIME))
            val accuracy = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PERFORMANCE_ACCURACY))
            performance = Performance(id, catId, responseTime, accuracy)
        }

        cursor.close()
        db.close()
        return performance
    }

    fun updateCategory(category: Category): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CATEGORY_NAME, category.name)
        }

        val affectedRows = db.update(
            TABLE_CATEGORIES,
            values,
            "$COLUMN_CATEGORY_ID = ?",
            arrayOf(category.id.toString())
        )
        db.close()
        return affectedRows
    }

    fun deleteCategory(categoryId: Int): Boolean {
        val db = this.writableDatabase

        db.delete(
            TABLE_ACTIONS,
            "$COLUMN_ACTION_CATEGORY_ID = ?",
            arrayOf(categoryId.toString())
        )

        db.delete(
            TABLE_PERFORMANCE,
            "$COLUMN_PERFORMANCE_CATEGORY_ID = ?",
            arrayOf(categoryId.toString())
        )

        db.delete(
            TABLE_PERFORMANCE_HISTORY,
            "$COLUMN_HISTORY_CATEGORY_ID = ?",
            arrayOf(categoryId.toString())
        )


        val result = db.delete(
            TABLE_CATEGORIES,
            "$COLUMN_CATEGORY_ID = ?",
            arrayOf(categoryId.toString())
        )

        db.close()
        return result > 0
    }

    fun updateAction(action: Action): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ACTION_NAME, action.name)
            put(COLUMN_ACTION_COLOR, action.color)  // 直接存储Int值
            put(COLUMN_ACTION_CATEGORY_ID, action.categoryId)
        }

        val affectedRows = db.update(
            TABLE_ACTIONS,
            values,
            "$COLUMN_ACTION_ID = ?",
            arrayOf(action.id.toString())
        )
        db.close()
        return affectedRows
    }

    fun deleteAction(actionId: Int): Boolean {
        val db = this.writableDatabase
        val result = db.delete(
            TABLE_ACTIONS,
            "$COLUMN_ACTION_ID = ?",
            arrayOf(actionId.toString())
        )
        db.close()
        return result > 0
    }

    fun saveUserSettings(userName: String, preferredLanguage: String): Boolean {
        val db = this.writableDatabase

        // 先清空表
        db.delete(TABLE_USER_SETTINGS, null, null)

        val values = ContentValues().apply {
            put(COLUMN_USER_NAME, userName)
            put(COLUMN_PREFERRED_LANGUAGE, preferredLanguage)
        }

        val id = db.insert(TABLE_USER_SETTINGS, null, values)
        db.close()
        return id != -1L
    }

    fun getUserSettings(): UserSettings? {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_USER_SETTINGS LIMIT 1"
        val cursor = db.rawQuery(query, null)

        var userSettings: UserSettings? = null
        if (cursor.moveToFirst()) {
            val userName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_NAME))
            val language = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PREFERRED_LANGUAGE))
            userSettings = UserSettings(userName, language)
        }

        cursor.close()
        db.close()
        return userSettings
    }
}

data class Category(
    val id: Int = 0,
    val name: String
)

data class Action(
    val id: Int = 0,
    val name: String,
    val color: Int,
    val categoryId: Int
)

data class Performance(
    val id: Int = 0,
    val categoryId: Int,
    val avgResponseTime: Double,
    val accuracy: Double
)

data class PerformanceRecord(
    val id: Int = 0,
    val categoryId: Int,
    val responseTime: Double,
    val accuracy: Double,
    val timestamp: Long
)

data class UserSettings(
    val userName: String,
    val preferredLanguage: String
)