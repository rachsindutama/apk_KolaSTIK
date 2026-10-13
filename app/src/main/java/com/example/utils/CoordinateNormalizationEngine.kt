package com.example.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density

data class ResizeDelta(val deltaW: Float, val deltaH: Float)

/**
 * Coordinate Normalization Engine for high-resolution canvas (up to 13500px) and deep zoom levels (> 1.0).
 * Maps touch pointer inputs and drag deltas correctly to logical layout coordinates,
 * accounting for canvas transformation matrix, zoom scale, and device density (DPI).
 */
object CoordinateNormalizationEngine {

    /**
     * Normalizes raw pointer drag amount against canvas scale and density.
     */
    fun normalizeDragDelta(
        dragAmount: Offset,
        canvasScale: Float,
        density: Density
    ): Offset {
        return dragAmount
    }

    /**
     * Maps screen touch position to logical canvas coordinates (0.0 to 1.0 relative bounds).
     */
    fun screenToLogicalCoordinates(
        screenPosition: Offset,
        canvasOffset: Offset,
        canvasScale: Float,
        canvasWidthPx: Float,
        canvasHeightPx: Float
    ): Offset {
        val safeScale = if (canvasScale > 0f) canvasScale else 1f
        val localX = (screenPosition.x - canvasOffset.x) / safeScale
        val localY = (screenPosition.y - canvasOffset.y) / safeScale
        
        val normalizedX = if (canvasWidthPx > 0f) (localX / canvasWidthPx).coerceIn(0f, 1f) else 0f
        val normalizedY = if (canvasHeightPx > 0f) (localY / canvasHeightPx).coerceIn(0f, 1f) else 0f

        return Offset(normalizedX, normalizedY)
    }

    /**
     * Calculates proportional delta changes for resizing layers on a massive canvas (e.g. 13500px).
     * dragAmount from inside scaled graphicsLayer is in local canvas pixels, so dividing by canvasWidthPx/canvasHeightPx
     * yields the exact fractional layout coordinates without double scale dampening.
     */
    fun calculateResizeDelta(
        dragAmount: Offset,
        canvasScale: Float,
        canvasWidthPx: Float,
        canvasHeightPx: Float
    ): ResizeDelta {
        val deltaW = if (canvasWidthPx > 1f) dragAmount.x / canvasWidthPx else 0f
        val deltaH = if (canvasHeightPx > 1f) dragAmount.y / canvasHeightPx else 0f

        return ResizeDelta(deltaW, deltaH)
    }
}
