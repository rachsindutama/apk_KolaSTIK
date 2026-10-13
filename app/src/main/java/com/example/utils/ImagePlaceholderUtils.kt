package com.example.utils

import android.content.Context
import android.graphics.*
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImagePlaceholderUtils {

    // Generates a local file containing a beautiful aesthetic gradient bitmap and returns its file URI string
    fun generatePlaceholderUri(context: Context, width: Int = 1080, height: Int = 1080, style: Int = 0): String {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint().apply {
            isAntiAlias = true
        }

        when (style) {
            0 -> { // Warm Peach Sunset Gradient
                val gradient = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(0xFFFF9A9E.toInt(), 0xFFFECFEF.toInt(), 0xFFFE9A8B.toInt()),
                    null, Shader.TileMode.CLAMP
                )
                paint.shader = gradient
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                
                // Draw some organic warm shapes
                paint.shader = null
                paint.color = 0x33FFFFFF
                canvas.drawCircle(width * 0.3f, height * 0.4f, width * 0.25f, paint)
            }
            1 -> { // Oceanic Breeze
                val gradient = LinearGradient(
                    0f, height.toFloat(), width.toFloat(), 0f,
                    intArrayOf(0xFFa1c4fd.toInt(), 0xFFc2e9fb.toInt()),
                    null, Shader.TileMode.CLAMP
                )
                paint.shader = gradient
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                
                paint.shader = null
                paint.color = 0x20FFFFFF
                canvas.drawCircle(width * 0.7f, height * 0.3f, width * 0.3f, paint)
            }
            2 -> { // Premium Noir Minimal
                val gradient = LinearGradient(
                    width.toFloat(), 0f, 0f, height.toFloat(),
                    intArrayOf(0xFF141e30.toInt(), 0xFF243b55.toInt()),
                    null, Shader.TileMode.CLAMP
                )
                paint.shader = gradient
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
            3 -> { // Creamy Matcha
                val gradient = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(0xFFd4fc79.toInt(), 0xFF96e6a1.toInt()),
                    null, Shader.TileMode.CLAMP
                )
                paint.shader = gradient
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
            4 -> { // Soft Rosewood Editorial
                val gradient = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(0xFFf093fb.toInt(), 0xFFf5576c.toInt()),
                    null, Shader.TileMode.CLAMP
                )
                paint.shader = gradient
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
            else -> { // Retro Sand Grid
                paint.color = 0xFFF2EFE9.toInt()
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                
                // Draw fine grid lines
                paint.color = 0x1A000000
                paint.strokeWidth = 2f
                val step = width / 10f
                for (i in 1..9) {
                    canvas.drawLine(i * step, 0f, i * step, height.toFloat(), paint)
                    canvas.drawLine(0f, i * step, width.toFloat(), i * step, paint)
                }
            }
        }

        // Save to file to get a real URI that can be accessed across the app
        val dir = File(context.cacheDir, "demo_images")
        if (!dir.exists()) dir.mkdirs()
        
        val file = File(dir, "placeholder_$style.jpg")
        try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        bitmap.recycle()
        return Uri.fromFile(file).toString()
    }
}
