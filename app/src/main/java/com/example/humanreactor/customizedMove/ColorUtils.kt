package com.example.humanreactor.customizedMove

import android.graphics.Color

class ColorUtils {
    companion object {
        // Get predefined color list
        fun getPredefinedColors(): List<Int> {
            return listOf(
                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
                Color.CYAN, Color.MAGENTA, Color.GRAY, Color.BLACK,
                Color.rgb(255, 165, 0), // Orange
                Color.rgb(128, 0, 128)  // Purple
            )
        }
    }
}