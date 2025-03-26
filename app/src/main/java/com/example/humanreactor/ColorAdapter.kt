package com.example.humanreactor

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class ColorAdapter(context: Context, private val colors: List<Int>) :
    ArrayAdapter<Int>(context, android.R.layout.simple_spinner_item, colors) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_spinner_item, parent, false)

        val color = getItem(position) ?: 0
        val colorView = view.findViewById<TextView>(android.R.id.text1)
        colorView.setBackgroundColor(color)
        colorView.text = ""

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_spinner_dropdown_item, parent, false)

        val color = getItem(position) ?: 0
        val colorView = view.findViewById<TextView>(android.R.id.text1)
        colorView.setBackgroundColor(color)
        colorView.text = ""

        return view
    }
}