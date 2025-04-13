package com.example.humanreactor.databases

// 1. 首先创建数据库辅助类
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ActionDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "actions.db"
        private const val DATABASE_VERSION = 1

        // 动作类别表
        private const val TABLE_CATEGORIES = "categories"
        private const val COLUMN_CATEGORY_ID = "id"
        private const val COLUMN_CATEGORY_NAME = "name"

        // 动作表
        private const val TABLE_ACTIONS = "actions"
        private const val COLUMN_ACTION_ID = "id"
        private const val COLUMN_ACTION_NAME = "name"
        private const val COLUMN_ACTION_COLOR = "color"
        private const val COLUMN_ACTION_CATEGORY_ID = "category_id"

        // 性能统计表
        private const val TABLE_PERFORMANCE = "performance"
        private const val COLUMN_PERFORMANCE_ID = "id"
        private const val COLUMN_PERFORMANCE_CATEGORY_ID = "category_id"
        private const val COLUMN_PERFORMANCE_RESPONSE_TIME = "avg_response_time"
        private const val COLUMN_PERFORMANCE_ACCURACY = "accuracy"

        // 用户设置表
        private const val TABLE_USER_SETTINGS = "user_settings"
        private const val COLUMN_USER_NAME = "user_name"
        private const val COLUMN_PREFERRED_LANGUAGE = "preferred_language"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 创建动作类别表
        val createCategoriesTable = """
            CREATE TABLE $TABLE_CATEGORIES (
                $COLUMN_CATEGORY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CATEGORY_NAME TEXT NOT NULL UNIQUE
            )
        """.trimIndent()

        // 创建动作表
        val createActionsTable = """
            CREATE TABLE $TABLE_ACTIONS (
                $COLUMN_ACTION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ACTION_NAME TEXT NOT NULL,
                $COLUMN_ACTION_COLOR TEXT NOT NULL,
                $COLUMN_ACTION_CATEGORY_ID INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_ACTION_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($COLUMN_CATEGORY_ID)
            )
        """.trimIndent()

        // 创建性能统计表
        val createPerformanceTable = """
            CREATE TABLE $TABLE_PERFORMANCE (
                $COLUMN_PERFORMANCE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PERFORMANCE_CATEGORY_ID INTEGER NOT NULL,
                $COLUMN_PERFORMANCE_RESPONSE_TIME REAL NOT NULL,
                $COLUMN_PERFORMANCE_ACCURACY REAL NOT NULL,
                FOREIGN KEY ($COLUMN_PERFORMANCE_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($COLUMN_CATEGORY_ID)
            )
        """.trimIndent()

        // 创建用户设置表
        val createUserSettingsTable = """
            CREATE TABLE $TABLE_USER_SETTINGS (
                $COLUMN_USER_NAME TEXT,
                $COLUMN_PREFERRED_LANGUAGE TEXT
            )
        """.trimIndent()

        // 执行创建表的SQL语句
        db.execSQL(createCategoriesTable)
        db.execSQL(createActionsTable)
        db.execSQL(createPerformanceTable)
        db.execSQL(createUserSettingsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 简单的升级策略是删除旧表并重建
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ACTIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PERFORMANCE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER_SETTINGS")
        onCreate(db)
    }

    // 2. 动作类别相关操作
    fun addCategory(categoryName: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CATEGORY_NAME, categoryName)
        }

        // 插入新行并返回主键ID
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

    // 3. 动作相关操作
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

    // 4. 性能统计相关操作
    fun addOrUpdatePerformance(performance: Performance): Long {
        val db = this.writableDatabase

        // 检查该类别的性能记录是否已存在
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
            // 更新现有记录
            db.update(
                TABLE_PERFORMANCE,
                values,
                "$COLUMN_PERFORMANCE_CATEGORY_ID = ?",
                arrayOf(performance.categoryId.toString())
            )
            // 获取现有记录的ID
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

    // 2.1 更新和删除类别
    fun updateCategory(category: Category): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CATEGORY_NAME, category.name)
        }

        // 更新并返回受影响的行数
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

        // 删除该类别相关的所有动作
        db.delete(
            TABLE_ACTIONS,
            "$COLUMN_ACTION_CATEGORY_ID = ?",
            arrayOf(categoryId.toString())
        )

        // 删除该类别相关的性能统计
        db.delete(
            TABLE_PERFORMANCE,
            "$COLUMN_PERFORMANCE_CATEGORY_ID = ?",
            arrayOf(categoryId.toString())
        )

        // 删除类别
        val result = db.delete(
            TABLE_CATEGORIES,
            "$COLUMN_CATEGORY_ID = ?",
            arrayOf(categoryId.toString())
        )

        db.close()
        return result > 0
    }

    // 3.1 更新和删除动作
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

    // 5. 用户设置相关操作
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

// 6. 数据模型类
data class Category(
    val id: Int = 0,
    val name: String
)

data class Action(
    val id: Int = 0,
    val name: String,
    val color: Int,  // 改为Int类型而非String
    val categoryId: Int
)

data class Performance(
    val id: Int = 0,
    val categoryId: Int,
    val avgResponseTime: Double,
    val accuracy: Double
)

data class UserSettings(
    val userName: String,
    val preferredLanguage: String
)