package com.example.utils

import androidx.compose.ui.graphics.ColorMatrix

object FilterUtils {
    
    // Returns a Compose ColorMatrix for real-time Compose image filtering
    fun getColorMatrix(filterId: String): ColorMatrix {
        val matrix = ColorMatrix()
        when (filterId) {
            "warm" -> {
                matrix.setToScale(1.1f, 1.02f, 0.9f, 1.0f)
            }
            "cool" -> {
                matrix.setToScale(0.9f, 1.02f, 1.15f, 1.0f)
            }
            "bw" -> {
                // Classic luminance grayscale weights: R=0.299, G=0.587, B=0.114
                matrix.setToSaturation(0.0f)
            }
            "sepia" -> {
                // Sepia conversion matrix
                val values = floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f,     0f,     0f,     1f, 0f
                )
                System.arraycopy(values, 0, matrix.values, 0, 20)
            }
            "vintage" -> {
                // Vintage warm film tone
                val values = floatArrayOf(
                    0.95f, 0.05f, 0f, 0f, 10f,
                    0f, 0.9f, 0.05f, 0f, 5f,
                    0f, 0f, 0.8f, 0f, 2f,
                    0f, 0f, 0f, 1f, 0f
                )
                System.arraycopy(values, 0, matrix.values, 0, 20)
            }
            "cinematic" -> {
                // High contrast, slightly blue shadow, amber highlights
                val values = floatArrayOf(
                    1.1f, 0f, 0f, 0f, 5f,
                    0f, 1.05f, 0f, 0f, 0f,
                    0f, 0f, 1.15f, 0f, -5f,
                    0f, 0f, 0f, 1f, 0f
                )
                System.arraycopy(values, 0, matrix.values, 0, 20)
            }
            "vivid" -> {
                // Boosted saturation and slight brightness increase
                matrix.setToSaturation(1.4f)
            }
            "noir" -> {
                // Deep high contrast black & white
                val values = floatArrayOf(
                    0.33f, 0.59f, 0.11f, 0f, -25f,
                    0.33f, 0.59f, 0.11f, 0f, -25f,
                    0.33f, 0.59f, 0.11f, 0f, -25f,
                    0f,    0f,    0f,    1f, 0f
                )
                System.arraycopy(values, 0, matrix.values, 0, 20)
            }
            else -> {
                // "none" - identity matrix
                matrix.reset()
            }
        }
        return matrix
    }

    // Returns a native android.graphics.ColorMatrix for Canvas exports
    fun getNativeColorFilter(filterId: String): android.graphics.ColorMatrixColorFilter? {
        if (filterId == "none") return null
        val composeMatrix = getColorMatrix(filterId)
        val nativeMatrix = android.graphics.ColorMatrix(composeMatrix.values)
        return android.graphics.ColorMatrixColorFilter(nativeMatrix)
    }

    fun getFilterName(filterId: String): String {
        return when (filterId) {
            "none" -> "Original"
            "warm" -> "Warm"
            "cool" -> "Cool"
            "bw" -> "B&W"
            "sepia" -> "Sepia"
            "vintage" -> "Vintage"
            "cinematic" -> "Cinematic"
            "vivid" -> "Vivid"
            "noir" -> "Noir"
            else -> "Original"
        }
    }
}
