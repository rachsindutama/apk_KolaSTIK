package com.example.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LayerModel
import kotlin.math.abs

data class AlignmentGuide(
    val isVertical: Boolean,
    val positionFraction: Float, // 0.0f to 1.0f on canvas
    val label: String,
    val color: Color = Color(0xFFFF2D55) // High-contrast magenta guide line
)

data class SnapResult(
    val x: Float,
    val y: Float,
    val guides: List<AlignmentGuide>,
    val snapped: Boolean
)

data class ResizeSnapResult(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val guides: List<AlignmentGuide>
)

object AlignmentGuideUtils {

    const val SNAP_THRESHOLD_RATIO = 0.018f // ~1.8% of canvas dimension (~10-15dp)

    val COLOR_CENTER = Color(0xFF00E5FF)       // Glowing Neon Cyan for Canvas Center
    val COLOR_NEIGHBOR = Color(0xFFFF2D55)     // Vibrant Magenta for Sibling Alignment
    val COLOR_EDGE = Color(0xFF007AFF)         // Electric Blue for Canvas Edges & Margins
    val COLOR_TOUCHING = Color(0xFFFF9500)     // Amber for Abutting / Adjacent frames

    private fun getLayoutGuideFractions(
        config: com.example.ui.MainViewModel.GuideLayoutConfig?,
        canvasWidthPx: Float,
        canvasHeightPx: Float
    ): Pair<List<Pair<Float, String>>, List<Pair<Float, String>>> {
        val verticalGuides = mutableListOf<Pair<Float, String>>()
        val horizontalGuides = mutableListOf<Pair<Float, String>>()

        if (config != null && config.enabled && canvasWidthPx > 0f && canvasHeightPx > 0f) {
            val leftFrac = config.marginLeftPx / canvasWidthPx
            val rightFrac = (canvasWidthPx - config.marginRightPx) / canvasWidthPx
            val topFrac = config.marginTopPx / canvasHeightPx
            val bottomFrac = (canvasHeightPx - config.marginBottomPx) / canvasHeightPx

            if (leftFrac > 0f && leftFrac < 1f) verticalGuides.add(leftFrac to "Margin Kiri")
            if (rightFrac > 0f && rightFrac < 1f) verticalGuides.add(rightFrac to "Margin Kanan")
            if (topFrac > 0f && topFrac < 1f) horizontalGuides.add(topFrac to "Margin Atas")
            if (bottomFrac > 0f && bottomFrac < 1f) horizontalGuides.add(bottomFrac to "Margin Bawah")

            val cols = config.columnsNumber
            if (cols > 0) {
                val totalWidth = canvasWidthPx - config.marginLeftPx - config.marginRightPx
                val gutter = config.gutterPx
                val colWidth = if (config.columnWidthPx > 0f) config.columnWidthPx else (totalWidth - (gutter * (cols - 1))) / cols

                var currentX = config.marginLeftPx
                for (i in 0 until cols) {
                    val f1 = currentX / canvasWidthPx
                    if (f1 in 0f..1f) verticalGuides.add(f1 to "Kolom ${i+1}")
                    currentX += colWidth
                    val f2 = currentX / canvasWidthPx
                    if (f2 in 0f..1f) verticalGuides.add(f2 to "Kolom ${i+1}")
                    if (i < cols - 1) {
                        if (gutter > 0f) {
                            val gutterCenter = (currentX + gutter / 2f) / canvasWidthPx
                            if (gutterCenter in 0f..1f) verticalGuides.add(gutterCenter to "Gutter")
                        }
                        currentX += gutter
                    }
                }
            }
        }
        return verticalGuides to horizontalGuides
    }

    /**
     * Calculate magnetic snapping and dynamic guides when moving a frame
     */
    fun calculateMoveSnap(
        targetX: Float,
        targetY: Float,
        width: Float,
        height: Float,
        otherLayers: List<LayerModel>,
        threshold: Float = SNAP_THRESHOLD_RATIO,
        guideConfig: com.example.ui.MainViewModel.GuideLayoutConfig? = null,
        canvasWidthPx: Float = 1f,
        canvasHeightPx: Float = 1f
    ): SnapResult {
        var snappedX = targetX
        var snappedY = targetY
        val guides = mutableListOf<AlignmentGuide>()
        var didSnap = false

        val centerX = targetX + width / 2f
        val rightX = targetX + width
        val centerY = targetY + height / 2f
        val bottomY = targetY + height

        val (verticalGuides, horizontalGuides) = getLayoutGuideFractions(guideConfig, canvasWidthPx, canvasHeightPx)

        // 1. VERTICAL SNAPPING (X AXIS)
        var snappedVert = false
        // a. Canvas Center
        if (abs(centerX - 0.5f) < threshold) {
            snappedX = 0.5f - width / 2f
            guides.add(AlignmentGuide(isVertical = true, positionFraction = 0.5f, label = "Pusat"))
            didSnap = true
            snappedVert = true
        }
        // b. Canvas Left Edge
        else if (abs(targetX - 0f) < threshold) {
            snappedX = 0f
            guides.add(AlignmentGuide(isVertical = true, positionFraction = 0f, label = "Tepi Kiri"))
            didSnap = true
            snappedVert = true
        }
        // c. Canvas Left Margin (5%)
        else if (abs(targetX - 0.05f) < threshold) {
            snappedX = 0.05f
            guides.add(AlignmentGuide(isVertical = true, positionFraction = 0.05f, label = "Margin 5%"))
            didSnap = true
            snappedVert = true
        }
        // d. Canvas Right Edge
        else if (abs(rightX - 1f) < threshold) {
            snappedX = 1f - width
            guides.add(AlignmentGuide(isVertical = true, positionFraction = 1f, label = "Tepi Kanan"))
            didSnap = true
            snappedVert = true
        }
        // e. Canvas Right Margin (95%)
        else if (abs(rightX - 0.95f) < threshold) {
            snappedX = 0.95f - width
            guides.add(AlignmentGuide(isVertical = true, positionFraction = 0.95f, label = "Margin 95%"))
            didSnap = true
            snappedVert = true
        }
        // f. Custom Layout Guides
        if (!snappedVert) {
            for ((gx, glabel) in verticalGuides) {
                if (abs(targetX - gx) < threshold) {
                    snappedX = gx
                    guides.add(AlignmentGuide(isVertical = true, positionFraction = gx, label = glabel, color = Color(0xFFFF2D55)))
                    didSnap = true
                    snappedVert = true
                    break
                } else if (abs(centerX - gx) < threshold) {
                    snappedX = gx - width / 2f
                    guides.add(AlignmentGuide(isVertical = true, positionFraction = gx, label = "$glabel (Pusat)", color = Color(0xFFFF2D55)))
                    didSnap = true
                    snappedVert = true
                    break
                } else if (abs(rightX - gx) < threshold) {
                    snappedX = gx - width
                    guides.add(AlignmentGuide(isVertical = true, positionFraction = gx, label = "$glabel (Sisi)", color = Color(0xFFFF2D55)))
                    didSnap = true
                    snappedVert = true
                    break
                }
            }
        }
        // g. Align with other layers on canvas
        if (!snappedVert) {
            for (other in otherLayers) {
                val otherCenterX = other.x + other.width / 2f
                if (abs(centerX - otherCenterX) < threshold) {
                    snappedX = otherCenterX - width / 2f
                    guides.add(AlignmentGuide(isVertical = true, positionFraction = otherCenterX, label = "Sejajar"))
                    didSnap = true
                    break
                } else if (abs(targetX - other.x) < threshold) {
                    snappedX = other.x
                    guides.add(AlignmentGuide(isVertical = true, positionFraction = other.x, label = "Sisi Kiri"))
                    didSnap = true
                    break
                } else if (abs(rightX - (other.x + other.width)) < threshold) {
                    snappedX = other.x + other.width - width
                    guides.add(AlignmentGuide(isVertical = true, positionFraction = other.x + other.width, label = "Sisi Kanan"))
                    didSnap = true
                    break
                }
            }
        }

        // 2. HORIZONTAL SNAPPING (Y AXIS)
        var snappedHoriz = false
        // a. Canvas Center
        if (abs(centerY - 0.5f) < threshold) {
            snappedY = 0.5f - height / 2f
            guides.add(AlignmentGuide(isVertical = false, positionFraction = 0.5f, label = "Tengah"))
            didSnap = true
            snappedHoriz = true
        }
        // b. Canvas Top Edge
        else if (abs(targetY - 0f) < threshold) {
            snappedY = 0f
            guides.add(AlignmentGuide(isVertical = false, positionFraction = 0f, label = "Tepi Atas"))
            didSnap = true
            snappedHoriz = true
        }
        // c. Canvas Top Margin (5%)
        else if (abs(targetY - 0.05f) < threshold) {
            snappedY = 0.05f
            guides.add(AlignmentGuide(isVertical = false, positionFraction = 0.05f, label = "Margin 5%"))
            didSnap = true
            snappedHoriz = true
        }
        // d. Canvas Bottom Edge
        else if (abs(bottomY - 1f) < threshold) {
            snappedY = 1f - height
            guides.add(AlignmentGuide(isVertical = false, positionFraction = 1f, label = "Tepi Bawah"))
            didSnap = true
            snappedHoriz = true
        }
        // e. Canvas Bottom Margin (95%)
        else if (abs(bottomY - 0.95f) < threshold) {
            snappedY = 0.95f - height
            guides.add(AlignmentGuide(isVertical = false, positionFraction = 0.95f, label = "Margin 95%"))
            didSnap = true
            snappedHoriz = true
        }
        // f. Custom Layout Guides
        if (!snappedHoriz) {
            for ((gy, glabel) in horizontalGuides) {
                if (abs(targetY - gy) < threshold) {
                    snappedY = gy
                    guides.add(AlignmentGuide(isVertical = false, positionFraction = gy, label = glabel, color = Color(0xFFFF2D55)))
                    didSnap = true
                    snappedHoriz = true
                    break
                } else if (abs(centerY - gy) < threshold) {
                    snappedY = gy - height / 2f
                    guides.add(AlignmentGuide(isVertical = false, positionFraction = gy, label = "$glabel (Tengah)", color = Color(0xFFFF2D55)))
                    didSnap = true
                    snappedHoriz = true
                    break
                } else if (abs(bottomY - gy) < threshold) {
                    snappedY = gy - height
                    guides.add(AlignmentGuide(isVertical = false, positionFraction = gy, label = "$glabel (Bawah)", color = Color(0xFFFF2D55)))
                    didSnap = true
                    snappedHoriz = true
                    break
                }
            }
        }
        // g. Align with other layers
        if (!snappedHoriz) {
            for (other in otherLayers) {
                val otherCenterY = other.y + other.height / 2f
                if (abs(centerY - otherCenterY) < threshold) {
                    snappedY = otherCenterY - height / 2f
                    guides.add(AlignmentGuide(isVertical = false, positionFraction = otherCenterY, label = "Sejajar"))
                    didSnap = true
                    break
                } else if (abs(targetY - other.y) < threshold) {
                    snappedY = other.y
                    guides.add(AlignmentGuide(isVertical = false, positionFraction = other.y, label = "Sisi Atas"))
                    didSnap = true
                    break
                } else if (abs(bottomY - (other.y + other.height)) < threshold) {
                    snappedY = other.y + other.height - height
                    guides.add(AlignmentGuide(isVertical = false, positionFraction = other.y + other.height, label = "Sisi Bawah"))
                    didSnap = true
                    break
                }
            }
        }

        return SnapResult(
            x = snappedX.coerceIn(0f, 1f - width),
            y = snappedY.coerceIn(0f, 1f - height),
            guides = guides,
            snapped = didSnap
        )
    }

    /**
     * Calculate magnetic snapping and dynamic guides when resizing via corner handles
     */
    fun calculateResizeSnap(
        targetX: Float,
        targetY: Float,
        targetW: Float,
        targetH: Float,
        handleIndex: Int, // 0 = TopLeft, 1 = TopRight, 2 = BottomLeft, 3 = BottomRight
        otherLayers: List<LayerModel> = emptyList(),
        threshold: Float = SNAP_THRESHOLD_RATIO,
        minWidth: Float = 0.005f,
        minHeight: Float = 0.005f,
        guideConfig: com.example.ui.MainViewModel.GuideLayoutConfig? = null,
        canvasWidthPx: Float = 1f,
        canvasHeightPx: Float = 1f
    ): ResizeSnapResult {
        var x = targetX
        var y = targetY
        var w = targetW
        var h = targetH
        val guides = mutableListOf<AlignmentGuide>()
        val (verticalGuides, horizontalGuides) = getLayoutGuideFractions(guideConfig, canvasWidthPx, canvasHeightPx)

        when (handleIndex) {
            0 -> {
                // Top-Left: x and y are moving
                var snappedX = false
                if (abs(x - 0f) < threshold) {
                    w += (x - 0f); x = 0f; guides.add(AlignmentGuide(isVertical = true, positionFraction = 0f, label = "Tepi Kiri")); snappedX = true
                } else if (abs(x - 0.05f) < threshold) {
                    w += (x - 0.05f); x = 0.05f; guides.add(AlignmentGuide(isVertical = true, positionFraction = 0.05f, label = "Margin 5%")); snappedX = true
                } else if (abs(x - 0.5f) < threshold) {
                    w += (x - 0.5f); x = 0.5f; guides.add(AlignmentGuide(isVertical = true, positionFraction = 0.5f, label = "Pusat")); snappedX = true
                } else {
                    for ((gx, glabel) in verticalGuides) {
                        if (abs(x - gx) < threshold) {
                            w += (x - gx); x = gx; guides.add(AlignmentGuide(isVertical = true, positionFraction = gx, label = glabel, color = Color(0xFFFF2D55))); snappedX = true; break
                        }
                    }
                }
                if (!snappedX) {
                    for (s in otherLayers) {
                        if (abs(x - s.x) < threshold) {
                            w += (x - s.x); x = s.x; guides.add(AlignmentGuide(isVertical = true, positionFraction = s.x, label = "Sisi Kiri", color = Color(0xFFFF9500))); break
                        } else if (abs(x - (s.x + s.width)) < threshold) {
                            w += (x - (s.x + s.width)); x = s.x + s.width; guides.add(AlignmentGuide(isVertical = true, positionFraction = s.x + s.width, label = "Sejajar", color = Color(0xFFFF9500))); break
                        }
                    }
                }

                var snappedY = false
                if (abs(y - 0f) < threshold) {
                    h += (y - 0f); y = 0f; guides.add(AlignmentGuide(isVertical = false, positionFraction = 0f, label = "Tepi Atas")); snappedY = true
                } else if (abs(y - 0.05f) < threshold) {
                    h += (y - 0.05f); y = 0.05f; guides.add(AlignmentGuide(isVertical = false, positionFraction = 0.05f, label = "Margin 5%")); snappedY = true
                } else if (abs(y - 0.5f) < threshold) {
                    h += (y - 0.5f); y = 0.5f; guides.add(AlignmentGuide(isVertical = false, positionFraction = 0.5f, label = "Tengah")); snappedY = true
                } else {
                    for ((gy, glabel) in horizontalGuides) {
                        if (abs(y - gy) < threshold) {
                            h += (y - gy); y = gy; guides.add(AlignmentGuide(isVertical = false, positionFraction = gy, label = glabel, color = Color(0xFFFF2D55))); snappedY = true; break
                        }
                    }
                }
                if (!snappedY) {
                    for (s in otherLayers) {
                        if (abs(y - s.y) < threshold) {
                            h += (y - s.y); y = s.y; guides.add(AlignmentGuide(isVertical = false, positionFraction = s.y, label = "Sisi Atas", color = Color(0xFFFF9500))); break
                        } else if (abs(y - (s.y + s.height)) < threshold) {
                            h += (y - (s.y + s.height)); y = s.y + s.height; guides.add(AlignmentGuide(isVertical = false, positionFraction = s.y + s.height, label = "Sejajar", color = Color(0xFFFF9500))); break
                        }
                    }
                }
            }
            1 -> {
                // Top-Right: y and right edge (x + w) are moving
                var snappedX = false
                val rightEdge = x + w
                if (abs(rightEdge - 1f) < threshold) {
                    w = 1f - x; guides.add(AlignmentGuide(isVertical = true, positionFraction = 1f, label = "Tepi Kanan")); snappedX = true
                } else if (abs(rightEdge - 0.95f) < threshold) {
                    w = 0.95f - x; guides.add(AlignmentGuide(isVertical = true, positionFraction = 0.95f, label = "Margin 95%")); snappedX = true
                } else if (abs(rightEdge - 0.5f) < threshold) {
                    w = 0.5f - x; guides.add(AlignmentGuide(isVertical = true, positionFraction = 0.5f, label = "Pusat")); snappedX = true
                } else {
                    for ((gx, glabel) in verticalGuides) {
                        if (abs(rightEdge - gx) < threshold) {
                            w = gx - x; guides.add(AlignmentGuide(isVertical = true, positionFraction = gx, label = glabel, color = Color(0xFFFF2D55))); snappedX = true; break
                        }
                    }
                }
                if (!snappedX) {
                    for (s in otherLayers) {
                        val sRight = s.x + s.width
                        if (abs(rightEdge - sRight) < threshold) {
                            w = sRight - x; guides.add(AlignmentGuide(isVertical = true, positionFraction = sRight, label = "Sisi Kanan", color = Color(0xFFFF9500))); break
                        } else if (abs(rightEdge - s.x) < threshold) {
                            w = s.x - x; guides.add(AlignmentGuide(isVertical = true, positionFraction = s.x, label = "Sejajar", color = Color(0xFFFF9500))); break
                        }
                    }
                }

                var snappedY = false
                if (abs(y - 0f) < threshold) {
                    h += (y - 0f); y = 0f; guides.add(AlignmentGuide(isVertical = false, positionFraction = 0f, label = "Tepi Atas")); snappedY = true
                } else if (abs(y - 0.05f) < threshold) {
                    h += (y - 0.05f); y = 0.05f; guides.add(AlignmentGuide(isVertical = false, positionFraction = 0.05f, label = "Margin 5%")); snappedY = true
                } else if (abs(y - 0.5f) < threshold) {
                    h += (y - 0.5f); y = 0.5f; guides.add(AlignmentGuide(isVertical = false, positionFraction = 0.5f, label = "Tengah")); snappedY = true
                } else {
                    for ((gy, glabel) in horizontalGuides) {
                        if (abs(y - gy) < threshold) {
                            h += (y - gy); y = gy; guides.add(AlignmentGuide(isVertical = false, positionFraction = gy, label = glabel, color = Color(0xFFFF2D55))); snappedY = true; break
                        }
                    }
                }
                if (!snappedY) {
                    for (s in otherLayers) {
                        if (abs(y - s.y) < threshold) {
                            h += (y - s.y); y = s.y; guides.add(AlignmentGuide(isVertical = false, positionFraction = s.y, label = "Sisi Atas", color = Color(0xFFFF9500))); break
                        } else if (abs(y - (s.y + s.height)) < threshold) {
                            h += (y - (s.y + s.height)); y = s.y + s.height; guides.add(AlignmentGuide(isVertical = false, positionFraction = s.y + s.height, label = "Sejajar", color = Color(0xFFFF9500))); break
                        }
                    }
                }
            }
            2 -> {
                // Bottom-Left: x and bottom edge (y + h) are moving
                var snappedX = false
                if (abs(x - 0f) < threshold) {
                    w += (x - 0f); x = 0f; guides.add(AlignmentGuide(isVertical = true, positionFraction = 0f, label = "Tepi Kiri")); snappedX = true
                } else if (abs(x - 0.05f) < threshold) {
                    w += (x - 0.05f); x = 0.05f; guides.add(AlignmentGuide(isVertical = true, positionFraction = 0.05f, label = "Margin 5%")); snappedX = true
                } else if (abs(x - 0.5f) < threshold) {
                    w += (x - 0.5f); x = 0.5f; guides.add(AlignmentGuide(isVertical = true, positionFraction = 0.5f, label = "Pusat")); snappedX = true
                } else {
                    for ((gx, glabel) in verticalGuides) {
                        if (abs(x - gx) < threshold) {
                            w += (x - gx); x = gx; guides.add(AlignmentGuide(isVertical = true, positionFraction = gx, label = glabel, color = Color(0xFFFF2D55))); snappedX = true; break
                        }
                    }
                }
                if (!snappedX) {
                    for (s in otherLayers) {
                        if (abs(x - s.x) < threshold) {
                            w += (x - s.x); x = s.x; guides.add(AlignmentGuide(isVertical = true, positionFraction = s.x, label = "Sisi Kiri", color = Color(0xFFFF9500))); break
                        } else if (abs(x - (s.x + s.width)) < threshold) {
                            w += (x - (s.x + s.width)); x = s.x + s.width; guides.add(AlignmentGuide(isVertical = true, positionFraction = s.x + s.width, label = "Sejajar", color = Color(0xFFFF9500))); break
                        }
                    }
                }

                var snappedY = false
                val bottomEdge = y + h
                if (abs(bottomEdge - 1f) < threshold) {
                    h = 1f - y; guides.add(AlignmentGuide(isVertical = false, positionFraction = 1f, label = "Tepi Bawah")); snappedY = true
                } else if (abs(bottomEdge - 0.95f) < threshold) {
                    h = 0.95f - y; guides.add(AlignmentGuide(isVertical = false, positionFraction = 0.95f, label = "Margin 95%")); snappedY = true
                } else if (abs(bottomEdge - 0.5f) < threshold) {
                    h = 0.5f - y; guides.add(AlignmentGuide(isVertical = false, positionFraction = 0.5f, label = "Tengah")); snappedY = true
                } else {
                    for ((gy, glabel) in horizontalGuides) {
                        if (abs(bottomEdge - gy) < threshold) {
                            h = gy - y; guides.add(AlignmentGuide(isVertical = false, positionFraction = gy, label = glabel, color = Color(0xFFFF2D55))); snappedY = true; break
                        }
                    }
                }
                if (!snappedY) {
                    for (s in otherLayers) {
                        val sBottom = s.y + s.height
                        if (abs(bottomEdge - sBottom) < threshold) {
                            h = sBottom - y; guides.add(AlignmentGuide(isVertical = false, positionFraction = sBottom, label = "Sisi Bawah", color = Color(0xFFFF9500))); break
                        } else if (abs(bottomEdge - s.y) < threshold) {
                            h = s.y - y; guides.add(AlignmentGuide(isVertical = false, positionFraction = s.y, label = "Sejajar", color = Color(0xFFFF9500))); break
                        }
                    }
                }
            }
            3 -> {
                // Bottom-Right: right edge (x + w) and bottom edge (y + h) are moving
                var snappedX = false
                val rightEdge = x + w
                if (abs(rightEdge - 1f) < threshold) {
                    w = 1f - x; guides.add(AlignmentGuide(isVertical = true, positionFraction = 1f, label = "Tepi Kanan")); snappedX = true
                } else if (abs(rightEdge - 0.95f) < threshold) {
                    w = 0.95f - x; guides.add(AlignmentGuide(isVertical = true, positionFraction = 0.95f, label = "Margin 95%")); snappedX = true
                } else if (abs(rightEdge - 0.5f) < threshold) {
                    w = 0.5f - x; guides.add(AlignmentGuide(isVertical = true, positionFraction = 0.5f, label = "Pusat")); snappedX = true
                } else {
                    for ((gx, glabel) in verticalGuides) {
                        if (abs(rightEdge - gx) < threshold) {
                            w = gx - x; guides.add(AlignmentGuide(isVertical = true, positionFraction = gx, label = glabel, color = Color(0xFFFF2D55))); snappedX = true; break
                        }
                    }
                }
                if (!snappedX) {
                    for (s in otherLayers) {
                        val sRight = s.x + s.width
                        if (abs(rightEdge - sRight) < threshold) {
                            w = sRight - x; guides.add(AlignmentGuide(isVertical = true, positionFraction = sRight, label = "Sisi Kanan", color = Color(0xFFFF9500))); break
                        } else if (abs(rightEdge - s.x) < threshold) {
                            w = s.x - x; guides.add(AlignmentGuide(isVertical = true, positionFraction = s.x, label = "Sejajar", color = Color(0xFFFF9500))); break
                        }
                    }
                }

                var snappedY = false
                val bottomEdge = y + h
                if (abs(bottomEdge - 1f) < threshold) {
                    h = 1f - y; guides.add(AlignmentGuide(isVertical = false, positionFraction = 1f, label = "Tepi Bawah")); snappedY = true
                } else if (abs(bottomEdge - 0.95f) < threshold) {
                    h = 0.95f - y; guides.add(AlignmentGuide(isVertical = false, positionFraction = 0.95f, label = "Margin 95%")); snappedY = true
                } else if (abs(bottomEdge - 0.5f) < threshold) {
                    h = 0.5f - y; guides.add(AlignmentGuide(isVertical = false, positionFraction = 0.5f, label = "Tengah")); snappedY = true
                } else {
                    for ((gy, glabel) in horizontalGuides) {
                        if (abs(bottomEdge - gy) < threshold) {
                            h = gy - y; guides.add(AlignmentGuide(isVertical = false, positionFraction = gy, label = glabel, color = Color(0xFFFF2D55))); snappedY = true; break
                        }
                    }
                }
                if (!snappedY) {
                    for (s in otherLayers) {
                        val sBottom = s.y + s.height
                        if (abs(bottomEdge - sBottom) < threshold) {
                            h = sBottom - y; guides.add(AlignmentGuide(isVertical = false, positionFraction = sBottom, label = "Sisi Bawah", color = Color(0xFFFF9500))); break
                        } else if (abs(bottomEdge - s.y) < threshold) {
                            h = s.y - y; guides.add(AlignmentGuide(isVertical = false, positionFraction = s.y, label = "Sejajar", color = Color(0xFFFF9500))); break
                        }
                    }
                }
            }
        }

        val safeMinW = minWidth.coerceIn(0.0005f, 0.2f)
        val safeMinH = minHeight.coerceIn(0.0005f, 0.2f)
        return ResizeSnapResult(
            x = x.coerceIn(0f, (1f - safeMinW).coerceAtLeast(0f)),
            y = y.coerceIn(0f, (1f - safeMinH).coerceAtLeast(0f)),
            width = w.coerceIn(safeMinW, (1f - x).coerceAtLeast(safeMinW)),
            height = h.coerceIn(safeMinH, (1f - y).coerceAtLeast(safeMinH)),
            guides = guides
        )
    }
}

/**
 * Overlay Composable to draw smart dynamic alignment guides and badges
 */
@Composable
fun DynamicAlignmentGuidesOverlay(
    guides: List<AlignmentGuide>,
    modifier: Modifier = Modifier
) {
    if (guides.isEmpty()) return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val density = androidx.compose.ui.platform.LocalDensity.current

        val hasVerticalCenter = guides.any { it.isVertical && kotlin.math.abs(it.positionFraction - 0.5f) < 0.001f }
        val hasHorizontalCenter = guides.any { !it.isVertical && kotlin.math.abs(it.positionFraction - 0.5f) < 0.001f }
        val isFullCenterSnap = hasVerticalCenter && hasHorizontalCenter

        Canvas(modifier = Modifier.fillMaxSize()) {
            val dashPattern = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)

            guides.forEach { guide ->
                if (guide.isVertical) {
                    val x = guide.positionFraction * size.width
                    // Outer glow stroke for high-contrast visibility
                    drawLine(
                        color = Color.White.copy(alpha = 0.95f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 3.5.dp.toPx()
                    )
                    // High-contrast primary neon guide stroke
                    drawLine(
                        color = guide.color,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 2.0.dp.toPx(),
                        pathEffect = dashPattern
                    )
                } else {
                    val y = guide.positionFraction * size.height
                    // Outer glow stroke for high-contrast visibility
                    drawLine(
                        color = Color.White.copy(alpha = 0.95f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 3.5.dp.toPx()
                    )
                    // High-contrast primary neon guide stroke
                    drawLine(
                        color = guide.color,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2.0.dp.toPx(),
                        pathEffect = dashPattern
                    )
                }
            }

            // Draw glowing center bullseye if snapped at canvas center
            if (isFullCenterSnap) {
                val centerOffset = Offset(size.width * 0.5f, size.height * 0.5f)
                drawCircle(
                    color = Color.White,
                    radius = 16.dp.toPx(),
                    center = centerOffset
                )
                drawCircle(
                    color = AlignmentGuideUtils.COLOR_CENTER,
                    radius = 12.dp.toPx(),
                    center = centerOffset
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = centerOffset
                )
            }
        }

        val maxWDp = with(density) { widthPx.toDp() }
        val maxHDp = with(density) { heightPx.toDp() }

        // Draw Guide Badges along edges
        guides.forEach { guide ->
            if (guide.isVertical) {
                val xDp = with(density) { (guide.positionFraction * widthPx).toDp() }
                Box(
                    modifier = Modifier
                        .offset(x = (xDp - 32.dp).coerceIn(6.dp, (maxWDp - 76.dp).coerceAtLeast(6.dp)), y = 6.dp)
                        .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(guide.color, androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = guide.label,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                val yDp = with(density) { (guide.positionFraction * heightPx).toDp() }
                Box(
                    modifier = Modifier
                        .offset(x = 6.dp, y = (yDp - 12.dp).coerceIn(6.dp, (maxHDp - 28.dp).coerceAtLeast(6.dp)))
                        .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(guide.color, androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = guide.label,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Overlay Composable to draw New Guide Layout (Columns, Gutter, and Margins)
 */
@Composable
fun NewGuideLayoutOverlay(
    config: com.example.ui.MainViewModel.GuideLayoutConfig,
    modifier: Modifier = Modifier
) {
    if (!config.enabled) return
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.5.dp.toPx()
        val guideColor = Color(0xFFFF2D55).copy(alpha = 0.75f)
        val marginColor = Color(0xFF00E5FF).copy(alpha = 0.85f)

        val marginTop = config.marginTopPx
        val marginBottom = config.marginBottomPx
        val marginLeft = config.marginLeftPx
        val marginRight = config.marginRightPx

        // 1. Draw Margins (Top, Bottom, Left, Right)
        drawLine(marginColor, Offset(0f, marginTop), Offset(w, marginTop), strokeWidth)
        drawLine(marginColor, Offset(0f, h - marginBottom), Offset(w, h - marginBottom), strokeWidth)
        drawLine(marginColor, Offset(marginLeft, 0f), Offset(marginLeft, h), strokeWidth)
        drawLine(marginColor, Offset(w - marginRight, 0f), Offset(w - marginRight, h), strokeWidth)

        // 2. Draw Columns
        val cols = config.columnsNumber
        if (cols > 0) {
            val totalWidth = w - marginLeft - marginRight
            val gutter = config.gutterPx
            val colWidth = if (config.columnWidthPx > 0f) config.columnWidthPx else (totalWidth - (gutter * (cols - 1))) / cols

            var currentX = marginLeft
            for (i in 0 until cols) {
                drawLine(guideColor, Offset(currentX, marginTop), Offset(currentX, h - marginBottom), strokeWidth)
                currentX += colWidth
                if (i < cols - 1) {
                    if (gutter > 0f) {
                        drawLine(guideColor.copy(alpha = 0.35f), Offset(currentX, marginTop), Offset(currentX, h - marginBottom), strokeWidth)
                    }
                    currentX += gutter
                }
                drawLine(guideColor, Offset(currentX, marginTop), Offset(currentX, h - marginBottom), strokeWidth)
            }
        }
    }
}

