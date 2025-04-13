package com.example.humanreactor.AI_Question

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences


class SharedPrefManager (context: Context){

    // AI question language shared preference
    val languageSharePred : SharedPreferences = context.getSharedPreferences("language_pref", Context.MODE_PRIVATE)

    // AI Question number type
    val numSharePref : SharedPreferences = context.getSharedPreferences("num_pref", Context.MODE_PRIVATE)

    // AI question category type
    val categorySharePref : SharedPreferences = context.getSharedPreferences("category_pref", Context.MODE_PRIVATE)

    // save and get the language
    fun saveLanguage(language : String){ languageSharePred.edit().putString("langauge", language).apply() }

    fun getLanguage(): String? { return languageSharePred.getString("language", "") }

    // save and get the amount of question
    fun saveNumber(number:Int){ numSharePref.edit().putInt("number", number).apply() }

    fun getNumber():Int{ return numSharePref.getInt("number", 0) }

    // save and get the category of question
    fun saveCategory(category:String){ categorySharePref.edit().putString("category", category).apply() }

    fun getCategory():String?{ return categorySharePref.getString("category", "") }

}