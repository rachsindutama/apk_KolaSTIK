package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object FileUtils {

    /**
     * Saves a bitmap to the device's public Gallery.
     * Support both JPG and PNG, with custom quality.
     * Returns the Uri of the saved file, or null if failed.
     */
    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        filenamePrefix: String = "KolaSTIK",
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 100
    ): Uri? {
        val extension = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"
        val mimeType = if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"
        val filename = "${filenamePrefix}_${System.currentTimeMillis()}.$extension"

        var outputStream: OutputStream? = null
        var uri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KolaSTIK")
                }
                
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    uri = imageUri
                    outputStream = resolver.openOutputStream(imageUri)
                }
            } else {
                // Pre-Q legacy saving
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(imagesDir, "KolaSTIK")
                if (!appDir.exists()) {
                    appDir.mkdirs()
                }
                val file = File(appDir, filename)
                uri = Uri.fromFile(file)
                outputStream = FileOutputStream(file)
                
                // Trigger media scanner for legacy Android
                @Suppress("DEPRECATION")
                context.sendBroadcast(android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
            }

            if (outputStream != null) {
                val success = bitmap.compress(format, quality, outputStream)
                outputStream.flush()
                outputStream.close()
                if (success) {
                    return uri
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                outputStream?.close()
            } catch (ignored: Exception) {}
        }
        return null
    }

    /**
     * Splits a bitmap into a grid of columns x rows, keeping maximum resolution.
     * Returns a list of cropped bitmaps.
     */
    fun splitBitmap(bitmap: Bitmap, cols: Int, rows: Int): List<Bitmap> {
        val result = mutableListOf<Bitmap>()
        val width = bitmap.width
        val height = bitmap.height
        
        val cellWidth = width / cols
        val cellHeight = height / rows

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = c * cellWidth
                val y = r * cellHeight
                
                // Safely handle rounding errors at boundaries
                val w = if (x + cellWidth > width) width - x else cellWidth
                val h = if (y + cellHeight > height) height - y else cellHeight

                if (w > 0 && h > 0) {
                    val part = Bitmap.createBitmap(bitmap, x, y, w, h)
                    result.add(part)
                }
            }
        }
        return result
    }

    /**
     * Decodes a bitmap from a URI safely.
     * Downscales the bitmap if it exceeds maxDimension to avoid OutOfMemoryError.
     */
    fun decodeBitmapFromUri(context: Context, uriString: String, maxDimension: Int = 2000): Bitmap? {
        try {
            val uri = Uri.parse(uriString)
            val resolver = context.contentResolver
            
            // First decode with inJustDecodeBounds=true to check dimensions
            var input = resolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(input, null, options)
            input.close()

            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            // Calculate sample size safely
            var sampleSize = 1
            val halfWidth = options.outWidth / 2
            val halfHeight = options.outHeight / 2
            while ((halfWidth / sampleSize) >= maxDimension || (halfHeight / sampleSize) >= maxDimension) {
                sampleSize *= 2
            }

            // Decode bitmap with inSampleSize set
            input = resolver.openInputStream(uri) ?: return null
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = if (maxDimension > 2500) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeStream(input, null, decodeOptions)
            input.close()
            
            return bitmap
        } catch (oom: OutOfMemoryError) {
            System.gc()
            try {
                val uri = Uri.parse(uriString)
                val resolver = context.contentResolver
                val input = resolver.openInputStream(uri) ?: return null
                val fallbackOptions = BitmapFactory.Options().apply {
                    inSampleSize = 4
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val bitmap = BitmapFactory.decodeStream(input, null, fallbackOptions)
                input.close()
                return bitmap
            } catch (t: Throwable) {
                t.printStackTrace()
                return null
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            return null
        }
    }

    /**
     * Reads image bounds (width x height) without decoding the full pixel data.
     * Supports content://, file://, and local file paths.
     */
    fun getImageDimensions(context: Context, uriString: String): Pair<Int, Int>? {
        return try {
            val uri = Uri.parse(uriString)
            val stream = try {
                context.contentResolver.openInputStream(uri)
            } catch (_: Exception) {
                null
            } ?: run {
                val path = uri.path ?: uriString
                val file = File(path)
                if (file.exists()) file.inputStream() else null
            }

            stream?.use { input ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(input, null, options)
                if (options.outWidth > 0 && options.outHeight > 0) {
                    Pair(options.outWidth, options.outHeight)
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
