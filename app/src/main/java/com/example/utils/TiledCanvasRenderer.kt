package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.LayerModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

enum class TiledExportMode {
    SINGLE_MASTER,     // Assembles high-res master bitmap via tiled pipeline
    MULTI_PANEL_GRID,  // Exports separate high-res tile files (for giant banner/billboard printing)
}

data class TiledRenderProgress(
    val currentTile: Int,
    val totalTiles: Int,
    val cols: Int,
    val rows: Int,
    val currentRow: Int,
    val currentCol: Int,
    val progress: Float,
    val statusMessage: String,
    val previewTile: Bitmap? = null
)

object TiledCanvasRenderer {

    const val DEFAULT_TILE_SIZE = 2048 // 2048x2048px (16MB per tile in ARGB_8888)
    const val MAX_CANVAS_DIMENSION = 13500

    /**
     * Renders a specific rectangular tile region of the project canvas.
     */
    fun renderSingleTile(
        context: Context,
        layers: List<LayerModel>,
        backgroundColor: Int,
        canvasTotalWidth: Int,
        canvasTotalHeight: Int,
        tileLeft: Int,
        tileTop: Int,
        tileWidth: Int,
        tileHeight: Int,
        config: Bitmap.Config = Bitmap.Config.ARGB_8888
    ): Bitmap? {
        if (tileWidth <= 0 || tileHeight <= 0) return null

        try {
            val tileBitmap = Bitmap.createBitmap(tileWidth, tileHeight, config)
            val canvas = Canvas(tileBitmap)

            // Fill tile background color
            canvas.drawColor(backgroundColor)

            // Translate canvas so (0,0) of tile aligns with (tileLeft, tileTop) in global canvas space
            canvas.save()
            canvas.translate(-tileLeft.toFloat(), -tileTop.toFloat())

            val tileBounds = RectF(
                tileLeft.toFloat(),
                tileTop.toFloat(),
                (tileLeft + tileWidth).toFloat(),
                (tileTop + tileHeight).toFloat()
            )

            // Render all layers intersecting this tile
            val sortedLayers = layers.sortedBy { it.zIndex }
            for (layer in sortedLayers) {
                if (!layer.isVisible) continue
                // Calculate absolute coordinates in canvas scale
                val lLeft = layer.x * canvasTotalWidth
                val lTop = layer.y * canvasTotalHeight
                val lRight = (layer.x + layer.width) * canvasTotalWidth
                val lBottom = (layer.y + layer.height) * canvasTotalHeight
                val frameWidth = lRight - lLeft
                val frameHeight = lBottom - lTop

                if (frameWidth <= 0f || frameHeight <= 0f) continue

                // Expand bounding box to account for shadow blur, glow radius, and rotation padding
                val glowPadding = if (layer.outerGlowEnabled) (layer.outerGlowRadius / 360f) * canvasTotalWidth * 2f else 0f
                val shadowPadding = if (layer.dropShadowEnabled) (layer.dropShadowBlur / 360f) * canvasTotalWidth * 2f else 0f
                val extraPadding = maxOf(glowPadding, shadowPadding, 50f)

                val layerBoundingBox = RectF(
                    lLeft - extraPadding,
                    lTop - extraPadding,
                    lRight + extraPadding,
                    lBottom + extraPadding
                )

                // Quick Tile Intersection Test
                if (!RectF.intersects(tileBounds, layerBoundingBox)) {
                    continue // Skip layers that do not touch this tile
                }

                val frameRect = RectF(lLeft, lTop, lRight, lBottom)
                val radiusPx = (layer.cornerRadius / 360f) * canvasTotalWidth

                val clipPath = Path()
                when (layer.shapeType) {
                    "oval" -> clipPath.addOval(frameRect, Path.Direction.CW)
                    "rounded" -> clipPath.addRoundRect(frameRect, radiusPx, radiusPx, Path.Direction.CW)
                    "polaroid" -> {
                        val bottomGap = frameHeight * 0.18f
                        val innerRect = RectF(
                            lLeft + frameWidth * 0.05f,
                            lTop + frameHeight * 0.05f,
                            lRight - frameWidth * 0.05f,
                            lBottom - bottomGap
                        )
                        clipPath.addRect(innerRect, Path.Direction.CW)
                    }
                    else -> clipPath.addRect(frameRect, Path.Direction.CW)
                }

                canvas.save()

                // Rotate around layer center
                val centerX = lLeft + frameWidth / 2f
                val centerY = lTop + frameHeight / 2f
                if (layer.rotation != 0f) {
                    canvas.rotate(layer.rotation, centerX, centerY)
                }

                // 1. Draw Outer Glow (if enabled)
                if (layer.outerGlowEnabled) {
                    val glowRadiusPx = (layer.outerGlowRadius / 360f) * canvasTotalWidth
                    LayerStyleUtils.drawOuterGlow(
                        canvas = canvas,
                        frameRect = frameRect,
                        shapeType = layer.shapeType,
                        cornerRadiusPx = radiusPx,
                        glowRadiusPx = glowRadiusPx,
                        glowColor = layer.outerGlowColor
                    )
                }

                // 2. Draw Drop Shadow (if enabled)
                if (layer.dropShadowEnabled) {
                    val blurPx = (layer.dropShadowBlur / 360f) * canvasTotalWidth
                    val offX = (layer.dropShadowOffsetX / 360f) * canvasTotalWidth
                    val offY = (layer.dropShadowOffsetY / 360f) * canvasTotalWidth
                    LayerStyleUtils.drawDropShadow(
                        canvas = canvas,
                        frameRect = frameRect,
                        shapeType = layer.shapeType,
                        cornerRadiusPx = radiusPx,
                        blurRadiusPx = blurPx,
                        offsetX = offX,
                        offsetY = offY,
                        shadowColor = layer.dropShadowColor
                    )
                }

                // 3. Draw Polaroid Base Frame
                if (layer.shapeType == "polaroid") {
                    val basePaint = Paint().apply {
                        color = AndroidColor.WHITE
                        style = Paint.Style.FILL
                        isAntiAlias = true
                        setShadowLayer(10f, 0f, 4f, 0x40000000)
                    }
                    canvas.drawRect(frameRect, basePaint)
                }

                // Clip Canvas to Frame bounds
                canvas.clipPath(clipPath)

                // 4. Draw Layer Image or Placeholder
                if (layer.imageUri != null) {
                    val maxImageDim = (maxOf(frameWidth, frameHeight) * 1.5f).toInt().coerceIn(800, 3500)
                    val imageBitmap = FileUtils.decodeBitmapFromUri(context, layer.imageUri!!, maxImageDim)
                    if (imageBitmap != null) {
                        val imagePaint = Paint().apply {
                            isAntiAlias = true
                            isFilterBitmap = true
                            colorFilter = FilterUtils.getNativeColorFilter(layer.filterId)
                        }

                        val imgMatrix = Matrix()
                        val srcW = imageBitmap.width.toFloat()
                        val srcH = imageBitmap.height.toFloat()
                        val scale = maxOf(frameWidth / srcW, frameHeight / srcH) * layer.imageScale
                        val imgLeft = centerX - (srcW * scale) / 2f + (layer.imageOffsetX * canvasTotalWidth)
                        val imgTop = centerY - (srcH * scale) / 2f + (layer.imageOffsetY * canvasTotalHeight)

                        imgMatrix.postScale(scale, scale)
                        imgMatrix.postTranslate(imgLeft, imgTop)

                        canvas.drawBitmap(imageBitmap, imgMatrix, imagePaint)
                        imageBitmap.recycle()
                    }
                } else {
                    val emptyPaint = Paint().apply {
                        color = 0xFFE5E5EA.toInt()
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(frameRect, emptyPaint)

                    val textPaint = Paint().apply {
                        color = 0x80000000.toInt()
                        textSize = (frameWidth * 0.08f).coerceIn(16f, 100f)
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    canvas.drawText("+ Foto", centerX, centerY + textPaint.textSize / 3f, textPaint)
                }

                // 5. Draw Inner Shadow
                if (layer.innerShadowEnabled) {
                    val blurPx = (layer.innerShadowBlur / 360f) * canvasTotalWidth
                    val offX = (layer.innerShadowOffsetX / 360f) * canvasTotalWidth
                    val offY = (layer.innerShadowOffsetY / 360f) * canvasTotalWidth
                    LayerStyleUtils.drawInnerShadow(
                        canvas = canvas,
                        frameRect = frameRect,
                        shapeType = layer.shapeType,
                        cornerRadiusPx = radiusPx,
                        blurRadiusPx = blurPx,
                        offsetX = offX,
                        offsetY = offY,
                        shadowColor = layer.innerShadowColor
                    )
                }

                canvas.restore()
            }

            canvas.restore()
            return tileBitmap
        } catch (oom: OutOfMemoryError) {
            System.gc()
            return null
        } catch (t: Throwable) {
            t.printStackTrace()
            return null
        }
    }

    /**
     * Executes the complete Tiled Rendering Pipeline for high resolutions up to 13500px.
     * Keeps UI responsive with coroutine yields and progress reporting.
     */
    suspend fun renderTiledExport(
        context: Context,
        layers: List<LayerModel>,
        backgroundColor: Int,
        targetWidth: Int,
        targetHeight: Int,
        tileSize: Int = DEFAULT_TILE_SIZE,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 100,
        mode: TiledExportMode = TiledExportMode.SINGLE_MASTER,
        onProgress: (TiledRenderProgress) -> Unit,
        onCompleted: (List<Uri>, String?) -> Unit
    ) = withContext(Dispatchers.Default) {
        val safeW = targetWidth.coerceIn(100, MAX_CANVAS_DIMENSION)
        val safeH = targetHeight.coerceIn(100, MAX_CANVAS_DIMENSION)

        val cols = (safeW + tileSize - 1) / tileSize
        val rows = (safeH + tileSize - 1) / tileSize
        val totalTiles = cols * rows

        val savedUris = mutableListOf<Uri>()

        // 1. If SINGLE_MASTER mode, create or attempt stitched master allocation
        var masterBitmap: Bitmap? = null
        var masterCanvas: Canvas? = null

        if (mode == TiledExportMode.SINGLE_MASTER) {
            val config = if (safeW >= 7000) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
            try {
                masterBitmap = Bitmap.createBitmap(safeW, safeH, config)
                if (masterBitmap != null) {
                    masterCanvas = Canvas(masterBitmap)
                    masterCanvas.drawColor(backgroundColor)
                }
            } catch (oom: OutOfMemoryError) {
                System.gc()
                // If single master bitmap allocation fails (e.g. 13500px on low RAM device),
                // automatically fallback to high-res multi-tile grid export so user never loses data!
            }
        }

        val isMasterModeActive = masterBitmap != null && masterCanvas != null
        val exportPrefix = "KolaSTIK_${safeW}x${safeH}"

        var currentTileIndex = 0

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                yield() // Keep coroutine cancellation responsive and allow UI drawing

                currentTileIndex++
                val tileLeft = c * tileSize
                val tileTop = r * tileSize
                val tileW = minOf(tileSize, safeW - tileLeft)
                val tileH = minOf(tileSize, safeH - tileTop)

                val progressFraction = currentTileIndex.toFloat() / totalTiles.toFloat()
                val statusMsg = "Merender Tile [$currentTileIndex/$totalTiles] (Baris ${r + 1}, Kolom ${c + 1})..."

                // Render current tile bitmap
                val tileBitmap = renderSingleTile(
                    context = context,
                    layers = layers,
                    backgroundColor = backgroundColor,
                    canvasTotalWidth = safeW,
                    canvasTotalHeight = safeH,
                    tileLeft = tileLeft,
                    tileTop = tileTop,
                    tileWidth = tileW,
                    tileHeight = tileH,
                    config = if (safeW >= 7000) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
                )

                if (tileBitmap != null) {
                    if (isMasterModeActive) {
                        // Draw tile directly onto master canvas
                        masterCanvas?.drawBitmap(tileBitmap, tileLeft.toFloat(), tileTop.toFloat(), null)
                    }

                    if (!isMasterModeActive || mode == TiledExportMode.MULTI_PANEL_GRID) {
                        // Save individual tile file to gallery
                        val tileFilename = "${exportPrefix}_Panel_R${r + 1}_C${c + 1}"
                        val tileUri = FileUtils.saveBitmapToGallery(
                            context = context,
                            bitmap = tileBitmap,
                            filenamePrefix = tileFilename,
                            format = format,
                            quality = quality
                        )
                        if (tileUri != null) {
                            savedUris.add(tileUri)
                        }
                    }

                    // Emit progress with thumbnail preview
                    onProgress(
                        TiledRenderProgress(
                            currentTile = currentTileIndex,
                            totalTiles = totalTiles,
                            cols = cols,
                            rows = rows,
                            currentRow = r,
                            currentCol = c,
                            progress = progressFraction,
                            statusMessage = statusMsg,
                            previewTile = tileBitmap
                        )
                    )

                    // Recycle individual tile bitmap immediately to prevent RAM accumulation
                    if (isMasterModeActive) {
                        tileBitmap.recycle()
                    }
                }
            }
        }

        // Finalize export
        if (isMasterModeActive && masterBitmap != null) {
            val masterUri = FileUtils.saveBitmapToGallery(
                context = context,
                bitmap = masterBitmap,
                filenamePrefix = "${exportPrefix}_Master",
                format = format,
                quality = quality
            )
            masterBitmap.recycle()

            if (masterUri != null) {
                savedUris.add(masterUri)
                withContext(Dispatchers.Main) {
                    onCompleted(savedUris, null)
                }
                return@withContext
            }
        }

        if (savedUris.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                onCompleted(savedUris, null)
            }
        } else {
            withContext(Dispatchers.Main) {
                onCompleted(emptyList(), "Gagal merender dan menyimpan tile gambar.")
            }
        }
    }
}
