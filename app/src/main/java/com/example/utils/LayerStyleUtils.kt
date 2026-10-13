package com.example.utils

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.Color
import com.example.data.LayerModel

data class LayerStylePreset(
    val id: String,
    val name: String,
    val description: String,
    val apply: (LayerModel) -> LayerModel
)

object LayerStyleUtils {

    val presets = listOf(
        LayerStylePreset(
            id = "none",
            name = "Normal (Polos)",
            description = "Hapus semua efek bayangan & pendar",
            apply = { layer ->
                layer.copy(
                    dropShadowEnabled = false,
                    innerShadowEnabled = false,
                    outerGlowEnabled = false
                )
            }
        ),
        LayerStylePreset(
            id = "soft_elevation",
            name = "Soft Elevation",
            description = "Bayangan halus alami memberi kesan melayang",
            apply = { layer ->
                layer.copy(
                    dropShadowEnabled = true,
                    dropShadowBlur = 12f,
                    dropShadowOffsetX = 2f,
                    dropShadowOffsetY = 6f,
                    dropShadowColor = 0x40000000.toInt(),
                    innerShadowEnabled = false,
                    outerGlowEnabled = false
                )
            }
        ),
        LayerStylePreset(
            id = "deep_float",
            name = "Deep Floating",
            description = "Bayangan tegas berkedalaman tinggi 3D",
            apply = { layer ->
                layer.copy(
                    dropShadowEnabled = true,
                    dropShadowBlur = 20f,
                    dropShadowOffsetX = 6f,
                    dropShadowOffsetY = 12f,
                    dropShadowColor = 0x66000000.toInt(),
                    innerShadowEnabled = false,
                    outerGlowEnabled = false
                )
            }
        ),
        LayerStylePreset(
            id = "neon_cyan_glow",
            name = "Neon Cyan Glow",
            description = "Pendar cahaya biru futuristik terang",
            apply = { layer ->
                layer.copy(
                    outerGlowEnabled = true,
                    outerGlowRadius = 18f,
                    outerGlowColor = 0xFF00E5FF.toInt(),
                    dropShadowEnabled = false,
                    innerShadowEnabled = false
                )
            }
        ),
        LayerStylePreset(
            id = "gold_glow",
            name = "Golden Sun Glow",
            description = "Pendar emas hangat estetik & mewah",
            apply = { layer ->
                layer.copy(
                    outerGlowEnabled = true,
                    outerGlowRadius = 18f,
                    outerGlowColor = 0xFFFFB300.toInt(),
                    dropShadowEnabled = false,
                    innerShadowEnabled = false
                )
            }
        ),
        LayerStylePreset(
            id = "neon_pink_glow",
            name = "Neon Magenta",
            description = "Pendar neon pink cerah modern",
            apply = { layer ->
                layer.copy(
                    outerGlowEnabled = true,
                    outerGlowRadius = 16f,
                    outerGlowColor = 0xFFFF2D55.toInt(),
                    dropShadowEnabled = false,
                    innerShadowEnabled = false
                )
            }
        ),
        LayerStylePreset(
            id = "inner_vignette",
            name = "Inner Inset Depth",
            description = "Bayangan di dalam bingkai seperti bingkai berlekuk",
            apply = { layer ->
                layer.copy(
                    innerShadowEnabled = true,
                    innerShadowBlur = 12f,
                    innerShadowOffsetX = 2f,
                    innerShadowOffsetY = 3f,
                    innerShadowColor = 0x8A000000.toInt(),
                    dropShadowEnabled = false,
                    outerGlowEnabled = false
                )
            }
        ),
        LayerStylePreset(
            id = "cyber_combo",
            name = "Glow + Shadow 3D",
            description = "Kombinasi bayangan jatuh dan pendar magis",
            apply = { layer ->
                layer.copy(
                    dropShadowEnabled = true,
                    dropShadowBlur = 14f,
                    dropShadowOffsetX = 4f,
                    dropShadowOffsetY = 8f,
                    dropShadowColor = 0x55000000.toInt(),
                    outerGlowEnabled = true,
                    outerGlowRadius = 14f,
                    outerGlowColor = 0xFF7C4DFF.toInt(),
                    innerShadowEnabled = false
                )
            }
        )
    )

    /**
     * Draws Outer Glow on native Android Canvas (Used for live canvas and export)
     */
    fun drawOuterGlow(
        canvas: Canvas,
        frameRect: RectF,
        shapeType: String,
        cornerRadiusPx: Float,
        glowRadiusPx: Float,
        glowColor: Int
    ) {
        if (glowRadiusPx <= 0f) return
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = glowColor
            maskFilter = BlurMaskFilter(glowRadiusPx.coerceAtLeast(1f), BlurMaskFilter.Blur.OUTER)
        }

        when (shapeType) {
            "oval" -> canvas.drawOval(frameRect, paint)
            "rounded" -> canvas.drawRoundRect(frameRect, cornerRadiusPx, cornerRadiusPx, paint)
            else -> canvas.drawRect(frameRect, paint)
        }
    }

    /**
     * Draws Drop Shadow on native Android Canvas
     */
    fun drawDropShadow(
        canvas: Canvas,
        frameRect: RectF,
        shapeType: String,
        cornerRadiusPx: Float,
        blurRadiusPx: Float,
        offsetX: Float,
        offsetY: Float,
        shadowColor: Int
    ) {
        if (blurRadiusPx <= 0f && offsetX == 0f && offsetY == 0f) return
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = shadowColor
            setShadowLayer(blurRadiusPx.coerceAtLeast(1f), offsetX, offsetY, shadowColor)
        }

        when (shapeType) {
            "oval" -> canvas.drawOval(frameRect, paint)
            "rounded" -> canvas.drawRoundRect(frameRect, cornerRadiusPx, cornerRadiusPx, paint)
            else -> canvas.drawRect(frameRect, paint)
        }
    }

    /**
     * Draws Inner Shadow on native Android Canvas inside the clipped bounds
     */
    fun drawInnerShadow(
        canvas: Canvas,
        frameRect: RectF,
        shapeType: String,
        cornerRadiusPx: Float,
        blurRadiusPx: Float,
        offsetX: Float,
        offsetY: Float,
        shadowColor: Int
    ) {
        if (blurRadiusPx <= 0f) return

        val width = frameRect.width()
        val height = frameRect.height()
        if (width <= 0 || height <= 0) return

        val shadowAlpha = AndroidColor.alpha(shadowColor)
        val solidColor = (shadowColor and 0x00FFFFFF) or (shadowAlpha shl 24)
        val transparentColor = shadowColor and 0x00FFFFFF

        val insetPx = blurRadiusPx.coerceAtMost(minOf(width, height) * 0.4f)

        // Top edge inner shadow
        val topShader = LinearGradient(
            frameRect.left, frameRect.top + offsetY,
            frameRect.left, frameRect.top + insetPx + offsetY,
            solidColor, transparentColor, Shader.TileMode.CLAMP
        )
        val topPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            shader = topShader
        }
        val topRect = RectF(frameRect.left, frameRect.top, frameRect.right, frameRect.top + insetPx + offsetY)
        drawShapeInternal(canvas, topRect, shapeType, cornerRadiusPx, topPaint)

        // Left edge inner shadow
        val leftShader = LinearGradient(
            frameRect.left + offsetX, frameRect.top,
            frameRect.left + insetPx + offsetX, frameRect.top,
            solidColor, transparentColor, Shader.TileMode.CLAMP
        )
        val leftPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            shader = leftShader
        }
        val leftRect = RectF(frameRect.left, frameRect.top, frameRect.left + insetPx + offsetX, frameRect.bottom)
        drawShapeInternal(canvas, leftRect, shapeType, cornerRadiusPx, leftPaint)

        // Bottom edge inner shadow (subtle depth)
        val bottomShader = LinearGradient(
            frameRect.left, frameRect.bottom,
            frameRect.left, frameRect.bottom - (insetPx * 0.7f),
            solidColor, transparentColor, Shader.TileMode.CLAMP
        )
        val bottomPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            shader = bottomShader
        }
        val bottomRect = RectF(frameRect.left, frameRect.bottom - (insetPx * 0.7f), frameRect.right, frameRect.bottom)
        drawShapeInternal(canvas, bottomRect, shapeType, cornerRadiusPx, bottomPaint)

        // Right edge inner shadow
        val rightShader = LinearGradient(
            frameRect.right, frameRect.top,
            frameRect.right - (insetPx * 0.7f), frameRect.top,
            solidColor, transparentColor, Shader.TileMode.CLAMP
        )
        val rightPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            shader = rightShader
        }
        val rightRect = RectF(frameRect.right - (insetPx * 0.7f), frameRect.top, frameRect.right, frameRect.bottom)
        drawShapeInternal(canvas, rightRect, shapeType, cornerRadiusPx, rightPaint)
    }

    private fun drawShapeInternal(canvas: Canvas, rect: RectF, shapeType: String, cornerRadiusPx: Float, paint: Paint) {
        when (shapeType) {
            "oval" -> canvas.drawOval(rect, paint)
            "rounded" -> canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
            else -> canvas.drawRect(rect, paint)
        }
    }
}
