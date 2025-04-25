package com.example.humanreactor.util

import android.content.Context
import android.graphics.Color
import com.example.humanreactor.databases.Action
import com.example.humanreactor.databases.ActionDatabaseHelper
import com.example.humanreactor.databases.Performance
import com.example.humanreactor.databases.PerformanceRecord
import java.util.Calendar
import kotlin.random.Random

object DatabaseUtil {

    fun populateSampleData(context: Context) {
        val dbHelper = ActionDatabaseHelper(context)

        // 添加一些示例类别
        val cat1Id = dbHelper.addCategory("跑步")
        val cat2Id = dbHelper.addCategory("跳跃")
        val cat3Id = dbHelper.addCategory("投掷")

        // 添加一些示例动作
        dbHelper.addAction(Action(name = "短跑", color = Color.RED, categoryId = cat1Id.toInt()))
        dbHelper.addAction(Action(name = "慢跑", color = Color.GREEN, categoryId = cat1Id.toInt()))
        dbHelper.addAction(Action(name = "高跳", color = Color.BLUE, categoryId = cat2Id.toInt()))
        dbHelper.addAction(Action(name = "跳远", color = Color.YELLOW, categoryId = cat2Id.toInt()))
        dbHelper.addAction(Action(name = "掷球", color = Color.CYAN, categoryId = cat3Id.toInt()))

        // 为每个类别添加5条历史记录
        addHistoryRecords(dbHelper, cat1Id.toInt())
        addHistoryRecords(dbHelper, cat2Id.toInt())
        addHistoryRecords(dbHelper, cat3Id.toInt())
    }

    private fun addHistoryRecords(dbHelper: ActionDatabaseHelper, categoryId: Int) {
        val calendar = Calendar.getInstance()

        // 添加5条历史记录，从5天前到今天
        for (i in 4 downTo 0) {
            // 设置日期为i天前
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            val timestamp = calendar.timeInMillis

            val responseTime = Random.nextDouble(500.0, 3000.0) // 0.5-3秒
            val accuracy = Random.nextDouble(0.5, 1.0) // 50%-100%

            // 创建记录并设置时间戳
            val record = PerformanceRecord(
                categoryId = categoryId,
                responseTime = responseTime,
                accuracy = accuracy,
                timestamp = timestamp
            )

            // 手动添加到数据库
            val db = dbHelper.writableDatabase
            db.execSQL(
                "INSERT INTO performance_history (category_id, response_time, accuracy, timestamp) VALUES (?, ?, ?, ?)",
                arrayOf(categoryId, responseTime, accuracy, timestamp)
            )
        }

        // 更新平均值表
        dbHelper.getPerformanceByCategory(categoryId)
    }

    fun addRandomRecord(context: Context, categoryId: Int) {
        val dbHelper = ActionDatabaseHelper(context)

        val responseTime = Random.nextDouble(500.0, 3000.0) // 0.5-3秒
        val accuracy = Random.nextDouble(0.5, 1.0) // 50%-100%

        dbHelper.addPerformanceRecord(categoryId, responseTime, accuracy)
    }
}