package com.example

import com.example.data.LayerModel
import com.example.utils.AlignmentGuideUtils
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testMoveSnapToCanvasCenter() {
        // Frame with width 0.4 and height 0.4
        // Center X = 0.29 + 0.2 = 0.49 (very close to 0.5f)
        val snapResult = AlignmentGuideUtils.calculateMoveSnap(
            targetX = 0.29f,
            targetY = 0.1f,
            width = 0.4f,
            height = 0.4f,
            otherLayers = emptyList()
        )

        assertTrue(snapResult.snapped)
        assertEquals(0.5f - 0.2f, snapResult.x, 0.001f)
        assertTrue(snapResult.guides.any { it.isVertical && it.positionFraction == 0.5f })
    }

    @Test
    fun testMoveSnapToCanvasEdge() {
        // Frame moved near left edge (x = 0.01f)
        val snapResult = AlignmentGuideUtils.calculateMoveSnap(
            targetX = 0.01f,
            targetY = 0.1f,
            width = 0.3f,
            height = 0.3f,
            otherLayers = emptyList()
        )

        assertTrue(snapResult.snapped)
        assertEquals(0.0f, snapResult.x, 0.001f)
        assertTrue(snapResult.guides.any { it.isVertical && it.positionFraction == 0.0f })
    }

    @Test
    fun testMoveSnapToSiblingLayer() {
        val sibling = LayerModel(id = "layer_2", x = 0.2f, y = 0.2f, width = 0.3f, height = 0.3f)
        // Moving target with width 0.2 near sibling's left edge (x = 0.205f)
        val snapResult = AlignmentGuideUtils.calculateMoveSnap(
            targetX = 0.205f,
            targetY = 0.5f,
            width = 0.2f,
            height = 0.2f,
            otherLayers = listOf(sibling)
        )

        assertTrue(snapResult.snapped)
        assertEquals(0.2f, snapResult.x, 0.001f)
        assertTrue(snapResult.guides.any { it.isVertical && it.positionFraction == 0.2f })
    }

    @Test
    fun testResizeSnapRightAndBottomEdges() {
        // Handle 3 (Bottom-Right) resized near right edge 1.0f and bottom edge 1.0f
        val resizeResult = AlignmentGuideUtils.calculateResizeSnap(
            targetX = 0.6f,
            targetY = 0.6f,
            targetW = 0.395f, // right edge = 0.995f, within threshold of 1.0f
            targetH = 0.392f, // bottom edge = 0.992f, within threshold of 1.0f
            handleIndex = 3,
            otherLayers = emptyList()
        )

        // Snapped right edge to 1.0f means width = 1.0f - 0.6f = 0.4f
        assertEquals(0.4f, resizeResult.width, 0.001f)
        // Snapped bottom edge to 1.0f means height = 1.0f - 0.6f = 0.4f
        assertEquals(0.4f, resizeResult.height, 0.001f)
        assertEquals(2, resizeResult.guides.size)
    }

    @Test
    fun testLayerModelLockingAndProperties() {
        val layer = LayerModel(
            id = "layer_lock_test",
            x = 0.1f,
            y = 0.2f,
            width = 0.5f,
            height = 0.5f,
            isLocked = true
        )
        assertTrue(layer.isLocked)
        val unlocked = layer.copy(isLocked = false)
        assertFalse(unlocked.isLocked)
        assertEquals(layer.id, unlocked.id)
    }

    @Test
    fun testLayerStylesAndPresets() {
        val layer = LayerModel(
            id = "layer_style_test",
            x = 0.1f,
            y = 0.1f,
            width = 0.4f,
            height = 0.4f,
            dropShadowEnabled = true,
            dropShadowBlur = 16f,
            dropShadowOffsetX = 5f,
            dropShadowOffsetY = 8f,
            dropShadowColor = 0x80000000.toInt(),
            innerShadowEnabled = true,
            innerShadowBlur = 10f,
            outerGlowEnabled = true,
            outerGlowRadius = 20f,
            outerGlowColor = 0xFF00E5FF.toInt()
        )

        assertTrue(layer.dropShadowEnabled)
        assertEquals(16f, layer.dropShadowBlur, 0.001f)
        assertEquals(5f, layer.dropShadowOffsetX, 0.001f)
        assertEquals(8f, layer.dropShadowOffsetY, 0.001f)
        assertEquals(0x80000000.toInt(), layer.dropShadowColor)
        assertTrue(layer.innerShadowEnabled)
        assertEquals(10f, layer.innerShadowBlur, 0.001f)
        assertTrue(layer.outerGlowEnabled)
        assertEquals(20f, layer.outerGlowRadius, 0.001f)
        assertEquals(0xFF00E5FF.toInt(), layer.outerGlowColor)

        // Test LayerStyleUtils presets
        val neonPreset = com.example.utils.LayerStyleUtils.presets.find { it.id == "neon_cyan_glow" }
        assertNotNull(neonPreset)
        val styled = neonPreset!!.apply(layer)
        assertTrue(styled.outerGlowEnabled)
        assertEquals(0xFF00E5FF.toInt(), styled.outerGlowColor)
        assertFalse(styled.dropShadowEnabled)

        val softElevPreset = com.example.utils.LayerStyleUtils.presets.find { it.id == "soft_elevation" }
        assertNotNull(softElevPreset)
        val elevated = softElevPreset!!.apply(layer)
        assertTrue(elevated.dropShadowEnabled)
        assertFalse(elevated.outerGlowEnabled)
    }

    @Test
    fun testTiledRenderingPartitioningLogic() {
        val totalWidth = 13500
        val totalHeight = 13500
        val tileSize = com.example.utils.TiledCanvasRenderer.DEFAULT_TILE_SIZE // 2048

        val cols = (totalWidth + tileSize - 1) / tileSize
        val rows = (totalHeight + tileSize - 1) / tileSize
        val totalTiles = cols * rows

        assertEquals(7, cols)
        assertEquals(7, rows)
        assertEquals(49, totalTiles)

        // Verify bounds calculation of the last tile
        val lastCol = cols - 1
        val lastRow = rows - 1
        val tileLeft = lastCol * tileSize
        val tileTop = lastRow * tileSize
        val tileW = minOf(tileSize, totalWidth - tileLeft)
        val tileH = minOf(tileSize, totalHeight - tileTop)

        assertEquals(12288, tileLeft)
        assertEquals(12288, tileTop)
        assertEquals(1212, tileW)
        assertEquals(1212, tileH)
        assertEquals(totalWidth, tileLeft + tileW)
        assertEquals(totalHeight, tileTop + tileH)
    }
}
