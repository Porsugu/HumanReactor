package com.example.humanreactor.QuickThinker

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONException


class SharedPrefManager (context: Context){

    // AI question language shared preference
    val languageSharePred : SharedPreferences = context.getSharedPreferences("language_pref", Context.MODE_PRIVATE)

    // AI Question number type
    val numSharePref : SharedPreferences = context.getSharedPreferences("num_pref", Context.MODE_PRIVATE)

    // AI question category type
    val categorySharePref : SharedPreferences = context.getSharedPreferences("category_pref", Context.MODE_PRIVATE)

    // AI question time used list
    val timeListSharedPref : SharedPreferences = context.getSharedPreferences("timeList_pref", Context.MODE_PRIVATE)

    // AI question the question number answer
    val correctNumberSharedPref : SharedPreferences = context.getSharedPreferences("correct_pref", Context.MODE_PRIVATE)


    // save and get the language
    fun saveLanguage(language : String){ languageSharePred.edit().putString("language", language).apply() }

    fun getLanguage(): String? { return languageSharePred.getString("language", "") }

    // save and get the amount of question
    fun saveNumber(number:Int){ numSharePref.edit().putInt("number", number).apply() }

    fun getNumber():Int{ return numSharePref.getInt("number", 0) }

    // save and get the category of question
    fun saveCategory(category:String){ categorySharePref.edit().putString("category", category).apply() }

    fun getCategory():String?{ return categorySharePref.getString("category", "") }

    // save and get the time list shared preference
    fun saveTimeList(timeList : List<Long>){
        val jsonArray = JSONArray()
        for(time in timeList){ jsonArray.put(time) }
        timeListSharedPref.edit().putString("time_list", jsonArray.toString()).apply()
    }

    fun getTimeList() : List<Long> {
        val jsonString = timeListSharedPref.getString("time_list", "[]")
        val timeList = mutableListOf<Long>()

        try{
            val jsonArray = JSONArray(jsonString)
            for(i in 0 until jsonArray.length()){
                timeList.add(jsonArray.getLong(i))
            }
        } catch (e:JSONException){
            e.printStackTrace()
        }
        return timeList
    }

    // save and get the correct number amount of the user has answered correct
    fun saveCorrectNumber(correctNum : Int){ return correctNumberSharedPref.edit().putInt("correctNum", correctNum).apply()}
    fun getCorrectNum():Int{ return correctNumberSharedPref.getInt("correctNum", 0) }

}