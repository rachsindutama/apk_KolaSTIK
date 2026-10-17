package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.utils.FileUtils
import com.example.utils.FilterUtils
import com.example.utils.ImagePlaceholderUtils
import com.example.utils.LayerStyleUtils
import com.example.utils.TemplatePresets
import com.example.utils.TiledCanvasRenderer
import com.example.utils.TiledExportMode
import com.example.utils.TiledRenderProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

sealed class Screen {
    object NewProject : Screen()
    object Projects : Screen()
    object Editor : Screen()
    object Splitter : Screen()
}

data class CanvasSnapshot(
    val layers: List<LayerModel>,
    val aspectRatio: String,
    val backgroundColor: Int
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ProjectDatabase.getDatabase(application)
    private val repository = ProjectRepository(database.projectDao())

    // Global Undo / Redo Stacks for canvas operations
    private val undoStack = mutableListOf<CanvasSnapshot>()
    private val redoStack = mutableListOf<CanvasSnapshot>()
    val canUndo = MutableStateFlow(false)
    val canRedo = MutableStateFlow(false)

    private fun updateUndoRedoState() {
        canUndo.value = undoStack.isNotEmpty()
        canRedo.value = redoStack.isNotEmpty()
    }

    fun clearUndoRedo() {
        undoStack.clear()
        redoStack.clear()
        updateUndoRedoState()
    }

    // Call this before any mutation begins to record the previous state
    fun recordSnapshot() {
        val currentSnapshot = CanvasSnapshot(
            layers = _layers.value,
            aspectRatio = activeAspectRatio.value,
            backgroundColor = activeBackgroundColor.value
        )
        if (undoStack.isNotEmpty() && undoStack.last() == currentSnapshot) {
            return
        }
        undoStack.add(currentSnapshot)
        if (undoStack.size > 50) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        updateUndoRedoState()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val currentSnapshot = CanvasSnapshot(
                layers = _layers.value,
                aspectRatio = activeAspectRatio.value,
                backgroundColor = activeBackgroundColor.value
            )
            redoStack.add(currentSnapshot)
            val previousSnapshot = undoStack.removeAt(undoStack.lastIndex)
            _layers.value = previousSnapshot.layers
            activeAspectRatio.value = previousSnapshot.aspectRatio
            activeBackgroundColor.value = previousSnapshot.backgroundColor
            if (selectedLayerId.value !in _layers.value.map { it.id }) {
                selectedLayerId.value = _layers.value.firstOrNull()?.id
            }
            updateUndoRedoState()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val currentSnapshot = CanvasSnapshot(
                layers = _layers.value,
                aspectRatio = activeAspectRatio.value,
                backgroundColor = activeBackgroundColor.value
            )
            undoStack.add(currentSnapshot)
            val nextSnapshot = redoStack.removeAt(redoStack.lastIndex)
            _layers.value = nextSnapshot.layers
            activeAspectRatio.value = nextSnapshot.aspectRatio
            activeBackgroundColor.value = nextSnapshot.backgroundColor
            if (selectedLayerId.value !in _layers.value.map { it.id }) {
                selectedLayerId.value = _layers.value.firstOrNull()?.id
            }
            updateUndoRedoState()
        }
    }

    // Navigation State
    private val _currentScreen = MutableStateFlow<Screen>(Screen.NewProject)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Saved Projects List
    val savedProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Project in Editor
    val activeProjectId = MutableStateFlow<Int?>(null)
    val activeProjectName = MutableStateFlow("Unsaved Collage")
    val activeAspectRatio = MutableStateFlow("1:1") // "1:1", "4:5", "16:9", "9:16", "3:4"
    val activeBackgroundColor = MutableStateFlow(0xFFFFFFFF.toInt())
    
    private val _layers = MutableStateFlow<List<LayerModel>>(emptyList())
    val layers: StateFlow<List<LayerModel>> = _layers.asStateFlow()

    // Selection
    val selectedLayerId = MutableStateFlow<String?>(null)

    // Image Splitter State
    val splitterSourceUri = MutableStateFlow<Uri?>(null)
    val splitterCols = MutableStateFlow(3)
    val splitterRows = MutableStateFlow(3)

    // Magnetic Snapping Sensitivity Threshold State
    val snapThresholdRatio = MutableStateFlow(0.018f)
    fun updateSnapThreshold(threshold: Float) {
        snapThresholdRatio.value = threshold.coerceIn(0.005f, 0.05f)
    }
    data class GuideLayoutConfig(
        val enabled: Boolean = false,
        val columnsNumber: Int = 0,
        val columnWidthPx: Float = 0f,
        val gutterPx: Float = 0f,
        val marginTopPx: Float = 0f,
        val marginBottomPx: Float = 0f,
        val marginLeftPx: Float = 0f,
        val marginRightPx: Float = 0f
    )
    val guideLayoutConfig = MutableStateFlow(GuideLayoutConfig())

    fun updateGuideLayoutConfig(config: GuideLayoutConfig) {
        guideLayoutConfig.value = config
    }

    fun toggleGuideLayoutVisibility() {
        val current = guideLayoutConfig.value
        guideLayoutConfig.value = current.copy(enabled = !current.enabled)
    }
    
    // Export Status state
    val isExporting = MutableStateFlow(false)
    val exportProgress = MutableStateFlow(0f) // 0.0 to 1.0

    // Tiled Ultra-Resolution Rendering State (Supports up to 13500px)
    val isTiledExporting = MutableStateFlow(false)
    val tiledProgress = MutableStateFlow<TiledRenderProgress?>(null)

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // Initialize blank freestyle or template
    fun startNewProject(presetId: String, targetWidth: Int = 1080, targetHeight: Int = 1350, isLandscape: Boolean = false) {
        viewModelScope.launch {
            clearUndoRedo()
            val preset = TemplatePresets.presets.find { it.id == presetId }
            activeProjectId.value = null
            activeProjectName.value = if (preset != null) "KolaSTIK ${preset.cleanTitle}" else "Untitled Collage"
            activeAspectRatio.value = "$targetWidth:$targetHeight"
            activeBackgroundColor.value = preset?.backgroundColor ?: 0xFFFFFFFF.toInt()
            if (preset != null) {
                _layers.value = preset.generateAdaptedLayers(targetWidth, targetHeight, isLandscape)
                selectedLayerId.value = _layers.value.firstOrNull()?.id
            } else {
                _layers.value = emptyList()
                selectedLayerId.value = null
            }
            navigateTo(Screen.Editor)
        }
    }

    fun startCustomProject(width: Int, height: Int, backgroundColor: Int = 0xFFFFFFFF.toInt()) {
        viewModelScope.launch {
            clearUndoRedo()
            val safeW = width.coerceIn(100, 13500)
            val safeH = height.coerceIn(100, 13500)
            activeProjectId.value = null
            activeProjectName.value = "Custom ${safeW}x${safeH}"
            activeAspectRatio.value = "$safeW:$safeH"
            activeBackgroundColor.value = backgroundColor
            _layers.value = emptyList()
            selectedLayerId.value = null
            navigateTo(Screen.Editor)
        }
    }

    // Apply template layout style to current project without altering canvas dimensions
    fun applyTemplatePreset(presetId: String) {
        val preset = TemplatePresets.presets.find { it.id == presetId }
        if (preset != null) {
            recordSnapshot()
            _layers.value = preset.generateLayers()
            selectedLayerId.value = _layers.value.firstOrNull()?.id
            // Do NOT overwrite activeAspectRatio to keep user custom canvas size
        }
    }

    // Update canvas dimensions while preserving frame aspect ratio and sizes (no stretching/squashing)
    fun updateCanvasDimensions(newW: Int, newH: Int) {
        val safeW = newW.coerceIn(100, 13500)
        val safeH = newH.coerceIn(100, 13500)

        val oldParts = activeAspectRatio.value.split(":")
        val oldW = oldParts.getOrNull(0)?.toFloatOrNull() ?: 3000f
        val oldH = oldParts.getOrNull(1)?.toFloatOrNull() ?: 3000f

        val oldAspect = if (oldH > 0f) oldW / oldH else 1f
        val newAspect = if (safeH > 0) safeW.toFloat() / safeH.toFloat() else 1f

        // If aspect ratio is unchanged (e.g. 3000:3000 vs 10800:10800), preserve exact layer coordinates!
        if (Math.abs(oldAspect - newAspect) < 0.005f) {
            activeAspectRatio.value = "$safeW:$safeH"
            return
        }

        recordSnapshot()

        // If aspect ratio changed (e.g. 1:1 to 9:16), adapt layer coordinates safely
        val aspectFactor = oldAspect / newAspect
        val updatedLayers = _layers.value.map { layer ->
            val adjW = if (aspectFactor < 1f) (layer.width * aspectFactor).coerceIn(0.01f, 1f) else layer.width.coerceIn(0.01f, 1f)
            val adjH = if (aspectFactor > 1f) (layer.height / aspectFactor).coerceIn(0.01f, 1f) else layer.height.coerceIn(0.01f, 1f)
            val adjX = layer.x.coerceIn(0f, (1f - adjW).coerceAtLeast(0f))
            val adjY = layer.y.coerceIn(0f, (1f - adjH).coerceAtLeast(0f))

            layer.copy(
                x = adjX,
                y = adjY,
                width = adjW,
                height = adjH
            )
        }

        _layers.value = updatedLayers
        activeAspectRatio.value = "$safeW:$safeH"
    }

    // Load project from Database
    fun loadProject(id: Int) {
        viewModelScope.launch {
            val entity = repository.getProjectById(id)
            if (entity != null) {
                clearUndoRedo()
                activeProjectId.value = entity.id
                activeProjectName.value = entity.name
                activeAspectRatio.value = entity.aspectRatio
                activeBackgroundColor.value = entity.backgroundColor
                _layers.value = LayerModel.fromJsonArray(entity.layersJson)
                selectedLayerId.value = _layers.value.firstOrNull()?.id
                navigateTo(Screen.Editor)
            }
        }
    }

    // Save project to DB
    fun saveProject(onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val json = LayerModel.toJsonArray(_layers.value)
            val entity = ProjectEntity(
                id = activeProjectId.value ?: 0,
                name = activeProjectName.value,
                aspectRatio = activeAspectRatio.value,
                backgroundColor = activeBackgroundColor.value,
                layersJson = json,
                timestamp = System.currentTimeMillis()
            )
            val generatedId = repository.insertProject(entity)
            if (activeProjectId.value == null) {
                activeProjectId.value = generatedId.toInt()
            }
            onSaved()
        }
    }

    // Delete a project
    fun deleteProject(projectId: Int) {
        viewModelScope.launch {
            repository.deleteProjectById(projectId)
        }
    }

    // Add a new blank custom layer
    fun addLayer(
        imageUri: String? = null,
        initialWidth: Float = 0.5f,
        initialHeight: Float = 0.5f,
        initialX: Float = 0.25f,
        initialY: Float = 0.25f,
        name: String = "",
        fitImage: Boolean = false
    ): String {
        recordSnapshot()
        val newId = UUID.randomUUID().toString()
        val newLayer = LayerModel(
            id = newId,
            x = initialX,
            y = initialY,
            width = initialWidth,
            height = initialHeight,
            zIndex = (_layers.value.maxOfOrNull { it.zIndex } ?: 0) + 1,
            borderThickness = 0f,
            borderColor = 0xFFFFFFFF.toInt(),
            cornerRadius = 0f,
            shapeType = "rect",
            imageUri = imageUri,
            name = name,
            fitImage = fitImage,
            lockAspectRatio = true
        )
        _layers.value = _layers.value + newLayer
        selectedLayerId.value = newLayer.id
        return newId
    }

    // Remove layer
    fun removeLayer(layerId: String) {
        recordSnapshot()
        _layers.value = _layers.value.filter { it.id != layerId }
        if (selectedLayerId.value == layerId) {
            selectedLayerId.value = _layers.value.firstOrNull()?.id
        }
    }

    // Duplicate selected layer
    fun duplicateLayer(layer: LayerModel) {
        recordSnapshot()
        val duplicated = layer.copy(
            id = UUID.randomUUID().toString(),
            x = (layer.x + 0.05f).coerceAtMost(0.9f),
            y = (layer.y + 0.05f).coerceAtMost(0.9f),
            zIndex = (_layers.value.maxOfOrNull { it.zIndex } ?: 0) + 1
        )
        _layers.value = _layers.value + duplicated
        selectedLayerId.value = duplicated.id
    }

    // Update layer in list
    fun updateLayer(updated: LayerModel) {
        _layers.value = _layers.value.map {
            if (it.id == updated.id) updated else it
        }
    }

    // Change selected layer depth z-index
    fun bringToFront(layerId: String) {
        recordSnapshot()
        val currentMax = _layers.value.maxOfOrNull { it.zIndex } ?: 0
        _layers.value = _layers.value.map {
            if (it.id == layerId) it.copy(zIndex = currentMax + 1) else it
        }
    }

    fun sendToBack(layerId: String) {
        recordSnapshot()
        val currentMin = _layers.value.minOfOrNull { it.zIndex } ?: 0
        _layers.value = _layers.value.map {
            if (it.id == layerId) it.copy(zIndex = currentMin - 1) else it
        }
    }

    fun bringForward(layerId: String) {
        recordSnapshot()
        val sorted = _layers.value.sortedBy { it.zIndex }
        val index = sorted.indexOfFirst { it.id == layerId }
        if (index in 0 until sorted.size - 1) {
            val current = sorted[index]
            val next = sorted[index + 1]
            val newCurrentZ = next.zIndex
            val newNextZ = current.zIndex
            _layers.value = _layers.value.map {
                when (it.id) {
                    current.id -> it.copy(zIndex = if (newCurrentZ == newNextZ) newCurrentZ + 1 else newCurrentZ)
                    next.id -> it.copy(zIndex = newNextZ)
                    else -> it
                }
            }
        } else if (index == sorted.size - 1 && sorted.isNotEmpty()) {
            bringToFront(layerId)
        }
    }

    fun sendBackward(layerId: String) {
        recordSnapshot()
        val sorted = _layers.value.sortedBy { it.zIndex }
        val index = sorted.indexOfFirst { it.id == layerId }
        if (index > 0) {
            val current = sorted[index]
            val prev = sorted[index - 1]
            val newCurrentZ = prev.zIndex
            val newPrevZ = current.zIndex
            _layers.value = _layers.value.map {
                when (it.id) {
                    current.id -> it.copy(zIndex = if (newCurrentZ == newPrevZ) newCurrentZ - 1 else newCurrentZ)
                    prev.id -> it.copy(zIndex = newPrevZ)
                    else -> it
                }
            }
        } else if (index == 0 && sorted.isNotEmpty()) {
            sendToBack(layerId)
        }
    }

    // Toggle layer lock
    fun toggleLockLayer(layerId: String) {
        recordSnapshot()
        _layers.value = _layers.value.map {
            if (it.id == layerId) it.copy(isLocked = !it.isLocked) else it
        }
    }

    // Toggle layer visibility (Photoshop eye icon)
    fun toggleLayerVisibility(layerId: String) {
        recordSnapshot()
        _layers.value = _layers.value.map {
            if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it
        }
    }

    // Update layer opacity (Photoshop opacity slider)
    fun updateLayerOpacity(layerId: String, opacity: Float) {
        _layers.value = _layers.value.map {
            if (it.id == layerId) it.copy(opacity = opacity.coerceIn(0f, 1f)) else it
        }
    }

    // Update layer custom name
    fun updateLayerName(layerId: String, newName: String) {
        recordSnapshot()
        _layers.value = _layers.value.map {
            if (it.id == layerId) it.copy(name = newName) else it
        }
    }

    // Center layer on canvas
    fun centerLayerOnCanvas(layerId: String) {
        recordSnapshot()
        _layers.value = _layers.value.map {
            if (it.id == layerId) {
                it.copy(
                    x = (0.5f - it.width / 2f).coerceIn(0f, 1f - it.width),
                    y = (0.5f - it.height / 2f).coerceIn(0f, 1f - it.height)
                )
            } else it
        }
    }

    // Generate Layout JSON string for Cloud Export
    fun exportLayoutConfigJson(): String {
        val json = LayerModel.toJsonArray(_layers.value)
        val meta = org.json.JSONObject().apply {
            put("name", activeProjectName.value)
            put("aspectRatio", activeAspectRatio.value)
            put("backgroundColor", activeBackgroundColor.value)
            put("layers", org.json.JSONArray(json))
        }
        return meta.toString()
    }

    // Import Layout JSON string from Cloud
    fun importLayoutConfigJson(jsonStr: String): Boolean {
        return try {
            clearUndoRedo()
            val meta = org.json.JSONObject(jsonStr)
            activeProjectId.value = null
            activeProjectName.value = meta.optString("name", "Imported Layout") + " (Cloud)"
            activeAspectRatio.value = meta.optString("aspectRatio", "1:1")
            activeBackgroundColor.value = meta.optInt("backgroundColor", 0xFFFFFFFF.toInt())
            
            val layersArray = meta.getJSONArray("layers")
            _layers.value = LayerModel.fromJsonArray(layersArray.toString())
            selectedLayerId.value = _layers.value.firstOrNull()?.id
            navigateTo(Screen.Editor)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Set demo image for selected layer
    fun setDemoImageToSelected(context: Context, style: Int) {
        val selectedId = selectedLayerId.value ?: return
        recordSnapshot()
        val demoUri = ImagePlaceholderUtils.generatePlaceholderUri(context, style = style)
        _layers.value = _layers.value.map {
            if (it.id == selectedId) it.copy(imageUri = demoUri) else it
        }
    }

    // Update background color with snapshot
    fun updateBackgroundColor(color: Int) {
        recordSnapshot()
        activeBackgroundColor.value = color
    }

    /**
     * HIGH RESOLUTION EXPORT ENGINE
     * Paints relative frames onto a high resolution bitmap.
     * Prevents OutOfMemory issues by handling bitmap memory safely.
     */
    fun exportCollage(
        context: Context,
        targetWidth: Int,
        format: Bitmap.CompressFormat,
        quality: Int,
        onResult: (Uri?, String?) -> Unit
    ) {
        viewModelScope.launch {
            isExporting.value = true
            exportProgress.value = 0.1f
            
            withContext(Dispatchers.IO) {
                try {
                    // 1. Calculate Target Aspect Ratio Height
                    val ratioParts = activeAspectRatio.value.split(":")
                    val ratioWidth = ratioParts.getOrNull(0)?.toFloatOrNull() ?: 1f
                    val ratioHeight = ratioParts.getOrNull(1)?.toFloatOrNull() ?: 1f
                    val heightWidthMultiplier = ratioHeight / ratioWidth
                    val targetHeight = (targetWidth * heightWidthMultiplier).toInt()

                    exportProgress.value = 0.2f

                    // 2. Safely create high-res bitmap with robust memory fallbacks
                    var actualTargetWidth = targetWidth
                    var actualTargetHeight = (targetWidth * heightWidthMultiplier).toInt()
                    var highResBitmap: Bitmap? = null

                    val candidateSizes = listOf(
                        targetWidth,
                        minOf(targetWidth, 10800),
                        minOf(targetWidth, 8000),
                        minOf(targetWidth, 6000),
                        minOf(targetWidth, 4000),
                        minOf(targetWidth, 2500)
                    ).distinct()

                    for (candidateW in candidateSizes) {
                        val candidateH = (candidateW * heightWidthMultiplier).toInt()
                        try {
                            val config = if (candidateW >= 7000) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
                            highResBitmap = Bitmap.createBitmap(candidateW, candidateH, config)
                            if (highResBitmap != null) {
                                actualTargetWidth = candidateW
                                actualTargetHeight = candidateH
                                break
                            }
                        } catch (oom: OutOfMemoryError) {
                            System.gc()
                        }
                    }

                    if (highResBitmap == null) {
                        withContext(Dispatchers.Main) {
                            onResult(null, "Gagal mengalokasi memori untuk resolusi ini. Silakan pilih resolusi lebih rendah.")
                        }
                        return@withContext
                    }

                    exportProgress.value = 0.3f

                    val canvas = Canvas(highResBitmap)
                    canvas.drawColor(activeBackgroundColor.value)

                    val sortedLayers = _layers.value.sortedBy { it.zIndex }
                    val totalLayers = sortedLayers.size
                    
                    sortedLayers.forEachIndexed { index, layer ->
                        if (!layer.isVisible) return@forEachIndexed
                        // Calculate absolute coordinates in high-res scale
                        val left = layer.x * actualTargetWidth
                        val top = layer.y * actualTargetHeight
                        val right = (layer.x + layer.width) * actualTargetWidth
                        val bottom = (layer.y + layer.height) * actualTargetHeight
                        val frameWidth = right - left
                        val frameHeight = bottom - top

                        if (frameWidth <= 0 || frameHeight <= 0) return@forEachIndexed

                        // Create Layer Path for Clipping (Rounded, Oval, Polaroid, Custom)
                        val clipPath = Path()
                        val frameRect = RectF(left, top, right, bottom)
                        
                        when (layer.shapeType) {
                            "oval" -> {
                                clipPath.addOval(frameRect, Path.Direction.CW)
                            }
                            "rounded" -> {
                                val radiusPx = (layer.cornerRadius / 360f) * actualTargetWidth
                                clipPath.addRoundRect(frameRect, radiusPx, radiusPx, Path.Direction.CW)
                            }
                            "polaroid" -> {
                                val bottomGap = frameHeight * 0.18f
                                val innerRect = RectF(
                                    left + frameWidth * 0.05f,
                                    top + frameHeight * 0.05f,
                                    right - frameWidth * 0.05f,
                                    bottom - bottomGap
                                )
                                clipPath.addRect(innerRect, Path.Direction.CW)
                            }
                            else -> { // rect
                                clipPath.addRect(frameRect, Path.Direction.CW)
                            }
                        }

                        // Save Canvas State
                        canvas.save()

                        // Handle rotation around layer center
                        val centerX = left + frameWidth / 2f
                        val centerY = top + frameHeight / 2f
                        if (layer.rotation != 0f) {
                            canvas.rotate(layer.rotation, centerX, centerY)
                        }

                        val radiusPx = (layer.cornerRadius / 360f) * actualTargetWidth

                        // 1. Draw Outer Glow (behind frame)
                        if (layer.outerGlowEnabled) {
                            val glowRadiusPx = (layer.outerGlowRadius / 360f) * actualTargetWidth
                            LayerStyleUtils.drawOuterGlow(
                                canvas = canvas,
                                frameRect = frameRect,
                                shapeType = layer.shapeType,
                                cornerRadiusPx = radiusPx,
                                glowRadiusPx = glowRadiusPx,
                                glowColor = layer.outerGlowColor
                            )
                        }

                        // 2. Draw Drop Shadow (behind frame)
                        if (layer.dropShadowEnabled) {
                            val blurPx = (layer.dropShadowBlur / 360f) * actualTargetWidth
                            val offX = (layer.dropShadowOffsetX / 360f) * actualTargetWidth
                            val offY = (layer.dropShadowOffsetY / 360f) * actualTargetWidth
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

                        // Draw Polaroid Base Frame
                        if (layer.shapeType == "polaroid") {
                            val basePaint = Paint().apply {
                                color = Color.WHITE
                                style = Paint.Style.FILL
                                isAntiAlias = true
                                setShadowLayer(10f, 0f, 4f, 0x40000000)
                            }
                            canvas.drawRect(frameRect, basePaint)
                        }

                        // Clip Canvas to Frame bounds
                        canvas.clipPath(clipPath)

                        // Draw Image inside Frame
                        if (layer.imageUri != null) {
                            // Decode Image efficiently scaled to frame bounds
                            val maxImageDim = (frameWidth * 1.5f).toInt().coerceIn(800, 3500)
                            val imageBitmap = FileUtils.decodeBitmapFromUri(context, layer.imageUri!!, maxImageDim)
                            if (imageBitmap != null) {
                                val imagePaint = Paint().apply {
                                    isAntiAlias = true
                                    isFilterBitmap = true
                                    // Apply Layer Filter
                                    colorFilter = FilterUtils.getNativeColorFilter(layer.filterId)
                                }

                                // Apply image transforms (Scale, Pan/Offsets)
                                val imgMatrix = Matrix()
                                
                                // Source & Destination Rect scaling
                                val srcW = imageBitmap.width.toFloat()
                                val srcH = imageBitmap.height.toFloat()
                                
                                // Calculate scale to fit/fill
                                val scale = maxOf(frameWidth / srcW, frameHeight / srcH) * layer.imageScale
                                val imgLeft = centerX - (srcW * scale) / 2f + (layer.imageOffsetX * actualTargetWidth)
                                val imgTop = centerY - (srcH * scale) / 2f + (layer.imageOffsetY * actualTargetHeight)

                                imgMatrix.postScale(scale, scale)
                                imgMatrix.postTranslate(imgLeft, imgTop)

                                canvas.drawBitmap(imageBitmap, imgMatrix, imagePaint)
                                imageBitmap.recycle()
                            }
                        } else {
                            // Empty frame layout color
                            val emptyPaint = Paint().apply {
                                color = 0xFFE5E5EA.toInt()
                                style = Paint.Style.FILL
                            }
                            canvas.drawRect(frameRect, emptyPaint)
                            
                            val textPaint = Paint().apply {
                                color = 0x80000000.toInt()
                                textSize = frameWidth * 0.08f
                                textAlign = Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            canvas.drawText("+", centerX, centerY + (textPaint.textSize / 3f), textPaint)
                        }

                        // 3. Draw Inner Shadow inside clipped bounds
                        if (layer.innerShadowEnabled) {
                            val innerBlurPx = (layer.innerShadowBlur / 360f) * actualTargetWidth
                            val innerOffX = (layer.innerShadowOffsetX / 360f) * actualTargetWidth
                            val innerOffY = (layer.innerShadowOffsetY / 360f) * actualTargetWidth
                            LayerStyleUtils.drawInnerShadow(
                                canvas = canvas,
                                frameRect = frameRect,
                                shapeType = layer.shapeType,
                                cornerRadiusPx = radiusPx,
                                blurRadiusPx = innerBlurPx,
                                offsetX = innerOffX,
                                offsetY = innerOffY,
                                shadowColor = layer.innerShadowColor
                            )
                        }

                        // Restore clipping and rotation bounds
                        canvas.restore()

                        // Draw borders on top (not clipped, so borders are drawn beautifully clean!)
                        if (layer.borderThickness > 0 && layer.shapeType != "polaroid") {
                            canvas.save()
                            if (layer.rotation != 0f) {
                                canvas.rotate(layer.rotation, centerX, centerY)
                            }
                            val borderPaint = Paint().apply {
                                color = layer.borderColor
                                style = Paint.Style.STROKE
                                strokeWidth = (layer.borderThickness / 360f) * actualTargetWidth
                                isAntiAlias = true
                            }
                            
                            when (layer.shapeType) {
                                "oval" -> {
                                    canvas.drawOval(frameRect, borderPaint)
                                }
                                "rounded" -> {
                                    val rPx = (layer.cornerRadius / 360f) * actualTargetWidth
                                    canvas.drawRoundRect(frameRect, rPx, rPx, borderPaint)
                                }
                                else -> { // rect
                                    canvas.drawRect(frameRect, borderPaint)
                                }
                            }
                            canvas.restore()
                        }

                        // Update Progress
                        exportProgress.value = 0.3f + (0.5f * (index + 1) / totalLayers)
                    }

                    exportProgress.value = 0.9f

                    // Save high resolution output bitmap
                    val savedUri = FileUtils.saveBitmapToGallery(
                        context,
                        highResBitmap,
                        "KolaSTIK_Collage",
                        format,
                        quality
                    )

                    highResBitmap.recycle()

                    withContext(Dispatchers.Main) {
                        exportProgress.value = 1.0f
                        isExporting.value = false
                        if (savedUri != null) {
                            onResult(savedUri, null)
                        } else {
                            onResult(null, "Gagal menyimpan file ke galeri.")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        isExporting.value = false
                        onResult(null, "Sistem bermasalah saat mengekspor: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * ULTRA-HIGH RESOLUTION TILED EXPORT ENGINE (Supports up to 13500px)
     * Divides the canvas into manageable memory-safe tiles to avoid OOM,
     * maintaining high UI responsiveness with background coroutine yields.
     */
    fun exportTiledCollage(
        context: Context,
        targetWidth: Int,
        format: Bitmap.CompressFormat,
        quality: Int,
        mode: TiledExportMode = TiledExportMode.SINGLE_MASTER,
        tileSize: Int = TiledCanvasRenderer.DEFAULT_TILE_SIZE,
        onResult: (List<Uri>?, String?) -> Unit
    ) {
        viewModelScope.launch {
            isTiledExporting.value = true
            tiledProgress.value = TiledRenderProgress(
                currentTile = 0,
                totalTiles = 1,
                cols = 1,
                rows = 1,
                currentRow = 0,
                currentCol = 0,
                progress = 0f,
                statusMessage = "Menyiapkan mesin render tile..."
            )

            val ratioParts = activeAspectRatio.value.split(":")
            val ratioWidth = ratioParts.getOrNull(0)?.toFloatOrNull() ?: 1f
            val ratioHeight = ratioParts.getOrNull(1)?.toFloatOrNull() ?: 1f
            val heightWidthMultiplier = ratioHeight / ratioWidth
            val targetHeight = (targetWidth * heightWidthMultiplier).toInt()

            TiledCanvasRenderer.renderTiledExport(
                context = context,
                layers = _layers.value,
                backgroundColor = activeBackgroundColor.value,
                targetWidth = targetWidth,
                targetHeight = targetHeight,
                tileSize = tileSize,
                format = format,
                quality = quality,
                mode = mode,
                onProgress = { progress ->
                    tiledProgress.value = progress
                },
                onCompleted = { uris, error ->
                    isTiledExporting.value = false
                    tiledProgress.value = null
                    onResult(uris, error)
                }
            )
        }
    }

    /**
     * SOCIAL MEDIA SPLIT IMAGE ENGINE
     * Splits a selected image into an equal columns x rows grid.
     */
    fun splitSelectedImage(
        context: Context,
        cols: Int,
        rows: Int,
        format: Bitmap.CompressFormat,
        quality: Int,
        onResult: (List<Uri>?, String?) -> Unit
    ) {
        val uri = splitterSourceUri.value ?: return
        viewModelScope.launch {
            isExporting.value = true
            exportProgress.value = 0.1f
            
            withContext(Dispatchers.IO) {
                try {
                    // Decode image in high resolution
                    val fullBitmap = FileUtils.decodeBitmapFromUri(context, uri.toString(), 4000)
                    if (fullBitmap == null) {
                        withContext(Dispatchers.Main) {
                            isExporting.value = false
                            onResult(null, "Gagal memproses gambar sumber.")
                        }
                        return@withContext
                    }

                    exportProgress.value = 0.4f
                    
                    // Split
                    val parts = FileUtils.splitBitmap(fullBitmap, cols, rows)
                    fullBitmap.recycle()
                    
                    if (parts.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            isExporting.value = false
                            onResult(null, "Gagal memotong gambar.")
                        }
                        return@withContext
                    }

                    exportProgress.value = 0.6f

                    val savedUris = mutableListOf<Uri>()
                    val totalParts = parts.size
                    
                    parts.forEachIndexed { index, part ->
                        val saved = FileUtils.saveBitmapToGallery(
                            context,
                            part,
                            "KolaSTIK_Split_${index + 1}",
                            format,
                            quality
                        )
                        part.recycle()
                        if (saved != null) {
                            savedUris.add(saved)
                        }
                        exportProgress.value = 0.6f + (0.3f * (index + 1) / totalParts)
                    }

                    withContext(Dispatchers.Main) {
                        isExporting.value = false
                        exportProgress.value = 1.0f
                        if (savedUris.size == totalParts) {
                            onResult(savedUris, null)
                        } else {
                            onResult(savedUris, "Terdapat ${totalParts - savedUris.size} potongan yang gagal disimpan.")
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        isExporting.value = false
                        onResult(null, "Terjadi kesalahan saat membagi gambar: ${e.message}")
                    }
                }
            }
        }
    }
}
