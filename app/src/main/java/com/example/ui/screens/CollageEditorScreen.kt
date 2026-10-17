package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import com.example.utils.CoordinateNormalizationEngine
import com.example.utils.NewGuideLayoutOverlay
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.LayerModel
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.utils.AlignmentGuide
import com.example.utils.AlignmentGuideUtils
import com.example.utils.DynamicAlignmentGuidesOverlay
import com.example.utils.FilterUtils
import com.example.utils.ImagePlaceholderUtils
import com.example.utils.LayerStyleUtils
import com.example.utils.TiledCanvasRenderer
import com.example.utils.TiledExportMode
import com.example.utils.TiledRenderProgress
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollageEditorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val layers by viewModel.layers.collectAsState()
    val selectedId by viewModel.selectedLayerId.collectAsState()
    val activeName by viewModel.activeProjectName.collectAsState()
    val activeAspect by viewModel.activeAspectRatio.collectAsState()
    val activeBgColor by viewModel.activeBackgroundColor.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val progress by viewModel.exportProgress.collectAsState()
    val isTiledExporting by viewModel.isTiledExporting.collectAsState()
    val tiledProgress by viewModel.tiledProgress.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    var activeTool by remember { mutableStateOf<String?>(null) } // null = main menu bar, "template", "photo", "size", "position", "style", "canvas", "cloud"
    
    // Canvas Workspace Zoom & Pan States (Wheel scroll, pinch gesture, floating buttons)
    var canvasScale by remember { mutableStateOf(1f) }
    var canvasOffset by remember { mutableStateOf(Offset.Zero) }
    
    // Export States
    var showExportDialog by remember { mutableStateOf(false) }
    var exportWidth by remember { mutableStateOf(3000f) } // slider 1080 - 13500px
    var exportFormat by remember { mutableStateOf(Bitmap.CompressFormat.PNG) }
    var exportQuality by remember { mutableStateOf(100f) }
    var tiledExportMode by remember { mutableStateOf(TiledExportMode.SINGLE_MASTER) }

    var showNewGuideLayoutDialog by remember { mutableStateOf(false) }
    val guideConfig by viewModel.guideLayoutConfig.collectAsState()
    val snapThreshold by viewModel.snapThresholdRatio.collectAsState()
    var showSnapSettingsDialog by remember { mutableStateOf(false) }

    // Dialog edit title
    var showEditTitleDialog by remember { mutableStateOf(false) }
    var tempTitle by remember { mutableStateOf(activeName) }
    var isSliderInteracting by remember { mutableStateOf(false) }

    // Dynamic Alignment Guides & Snapping States
    val haptic = LocalHapticFeedback.current
    var isSnappingEnabled by remember { mutableStateOf(true) }
    var activeGuides by remember { mutableStateOf<List<AlignmentGuide>>(emptyList()) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapLayerId by remember { mutableStateOf<String?>(null) }

    // Touch-based Layer Selector & Context Menu States (Tap and hold element)
    var showLayerContextMenu by remember { mutableStateOf(false) }
    var contextMenuLayerId by remember { mutableStateOf<String?>(null) }

    // Photo picker for selected layer
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val selectedLayer = layers.find { it.id == selectedId }
                if (selectedLayer != null) {
                    viewModel.recordSnapshot()
                    viewModel.updateLayer(selectedLayer.copy(imageUri = uri.toString()))
                }
            }
        }
    )

    // Back button behavior
    BackHandler {
        viewModel.navigateTo(Screen.Projects)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.clickable { 
                            tempTitle = activeName
                            showEditTitleDialog = true 
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            activeName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Judul",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Projects) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Beranda"
                        )
                    }
                },
                actions = {
                    // Global Undo Button
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = canUndo,
                        modifier = Modifier.testTag("undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
                        )
                    }

                    // Global Redo Button
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = canRedo,
                        modifier = Modifier.testTag("redo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
                        )
                    }

                    // View Menu (Guides > New Guide Layout...)
                    var showViewMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showViewMenu = true }) {
                            Icon(imageVector = Icons.Default.Visibility, contentDescription = "View Menu")
                        }
                        DropdownMenu(
                            expanded = showViewMenu,
                            onDismissRequest = { showViewMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Guides > New Guide Layout...") },
                                onClick = {
                                    showViewMenu = false
                                    showNewGuideLayoutDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Atur Sensitivitas Snap...") },
                                onClick = {
                                    showViewMenu = false
                                    showSnapSettingsDialog = true
                                }
                            )
                        }
                    }

                    // Global 'View' toggle button to quickly show/hide Guide Layout overlays without deleting configuration
                    val guideConfig by viewModel.guideLayoutConfig.collectAsState()
                    IconButton(
                        onClick = { viewModel.toggleGuideLayoutVisibility() },
                        modifier = Modifier.testTag("toggle_guide_layout_button")
                    ) {
                        Icon(
                            imageVector = if (guideConfig.enabled) Icons.Default.GridOn else Icons.Default.GridOff,
                            contentDescription = if (guideConfig.enabled) "Hide Guide Layout" else "Show Guide Layout",
                            tint = if (guideConfig.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.saveProject { Toast.makeText(context, "Proyek disimpan!", Toast.LENGTH_SHORT).show() } },
                        modifier = Modifier.testTag("save_project_button")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Simpan Proyek")
                    }

                    Button(
                        onClick = { showExportDialog = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("export_image_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ekspor", fontSize = 12.sp)
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            
            // 1. Interactive Collage Canvas Work Area (With Full Zoom & Pan Gestures)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    // Mouse/Trackpad scroll wheel zooms the canvas in and out
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Scroll) {
                                    val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                    if (delta != 0f) {
                                        val factor = if (delta < 0f) 1.15f else 0.85f
                                        canvasScale = (canvasScale * factor).coerceIn(0.1f, 8.0f)
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                    }
                    // Multi-touch pinch to zoom and pan the canvas workspace
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            canvasScale = (canvasScale * zoom).coerceIn(0.1f, 8.0f)
                            canvasOffset = Offset(
                                x = (canvasOffset.x + pan.x).coerceIn(-5000f, 5000f),
                                y = (canvasOffset.y + pan.y).coerceIn(-5000f, 5000f)
                            )
                        }
                    }
                    // Double-tap canvas to reset zoom/pan; single tap to deselect
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                canvasScale = 1f
                                canvasOffset = Offset.Zero
                            },
                            onTap = {
                                viewModel.selectedLayerId.value = null
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Calculate Aspect Ratio Multipliers for display safely
                val aspectMultiplier = remember(activeAspect) {
                    val parts = activeAspect.split(":")
                    val w = parts.getOrNull(0)?.toFloatOrNull() ?: 3000f
                    val h = parts.getOrNull(1)?.toFloatOrNull() ?: 3000f
                    if (w <= 0f || h <= 0f || w.isNaN() || h.isNaN() || w.isInfinite() || h.isInfinite()) {
                        1f
                    } else {
                        (w / h).coerceIn(0.05f, 20f)
                    }
                }

                val parentW = maxWidth
                val parentH = maxHeight
                val parentAspect = if (parentH.value > 0f) parentW.value / parentH.value else 1f

                val (canvasBoxW, canvasBoxH) = if (aspectMultiplier > parentAspect) {
                    parentW to (parentW / aspectMultiplier).coerceAtLeast(10.dp)
                } else {
                    (parentH * aspectMultiplier).coerceAtLeast(10.dp) to parentH
                }

                // Render Canvas (transformed by canvasScale and canvasOffset)
                Box(
                    modifier = Modifier
                        .size(width = canvasBoxW, height = canvasBoxH)
                        .graphicsLayer {
                            scaleX = canvasScale
                            scaleY = canvasScale
                            translationX = canvasOffset.x
                            translationY = canvasOffset.y
                        }
                        .shadow(4.dp, RectangleShape)
                        .clip(RectangleShape)
                        .background(Color(activeBgColor))
                ) {
                    val latestLayers = rememberUpdatedState(layers)
                    val layerAnimJobs = remember { mutableMapOf<String, kotlinx.coroutines.Job>() }

                    // Single Canvas Coordinate Resolver
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val canvasWidth = maxWidth
                        val canvasHeight = maxHeight
                        val density = androidx.compose.ui.platform.LocalDensity.current
                        val canvasWidthPx = with(density) { canvasWidth.toPx() }
                        val canvasHeightPx = with(density) { canvasHeight.toPx() }

                        // 1. Draw Layers (Sorted by zIndex)
                        layers.sortedBy { it.zIndex }.forEach { layer ->
                            val isSelected = layer.id == selectedId
                            val leftDp = canvasWidth * layer.x
                            val topDp = canvasHeight * layer.y
                            val widthDp = canvasWidth * layer.width
                            val heightDp = canvasHeight * layer.height

                            Box(
                                modifier = Modifier
                                    .offset(x = leftDp, y = topDp)
                                    .size(width = widthDp, height = heightDp)
                                    // Mouse/Trackpad scroll wheel on photo frame zooms the photo inside the frame!
                                    .pointerInput(layer.id) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                if (event.type == PointerEventType.Scroll) {
                                                    val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                                    if (delta != 0f) {
                                                        val currentLayer = latestLayers.value.find { it.id == layer.id } ?: layer
                                                        if (currentLayer.imageUri != null) {
                                                            viewModel.recordSnapshot()
                                                            val factor = if (delta < 0f) 1.12f else 0.88f
                                                            val newScale = (currentLayer.imageScale * factor).coerceIn(0.1f, 10.0f)
                                                            viewModel.updateLayer(currentLayer.copy(imageScale = newScale))
                                                            event.changes.forEach { it.consume() }
                                                        } else {
                                                            val factor = if (delta < 0f) 1.15f else 0.85f
                                                            canvasScale = (canvasScale * factor).coerceIn(0.1f, 8.0f)
                                                            event.changes.forEach { it.consume() }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    // Unified touch gestures: Drag to move, Two-finger pinch to zoom, Tap to select, Double-tap to reset
                                    .pointerInput(layer.id, canvasWidthPx, canvasHeightPx) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                down.consume()
                                                layerAnimJobs[layer.id]?.cancel()
                                                val startLayer = latestLayers.value.find { it.id == layer.id } ?: layer
                                                val touchStartInLayer = down.position
                                                val startTouchCanvasX = (startLayer.x * canvasWidthPx) + touchStartInLayer.x
                                                val startTouchCanvasY = (startLayer.y * canvasHeightPx) + touchStartInLayer.y
                                                val velocityTracker = VelocityTracker()
                                                velocityTracker.addPosition(down.uptimeMillis, Offset(startTouchCanvasX, startTouchCanvasY))
                                                var isDragging = false
                                                var isPinching = false
                                                var recordedSnapshot = false
                                                var prevSpan = -1f
                                                var prevCenter = Offset.Zero
                                                var lastDraggedX = startLayer.x
                                                var lastDraggedY = startLayer.y

                                                // Immediately select this layer so the selection boundary appears
                                                if (viewModel.selectedLayerId.value != layer.id) {
                                                    viewModel.selectedLayerId.value = layer.id
                                                }

                                                do {
                                                    val event = awaitPointerEvent()
                                                    val pressedPointers = event.changes.filter { it.pressed }
                                                    val currentLayer = latestLayers.value.find { it.id == layer.id } ?: startLayer

                                                    if (pressedPointers.size >= 2) {
                                                         isPinching = true
                                                         val p1 = pressedPointers[0]
                                                         val p2 = pressedPointers[1]
                                                         val currentSpan = (p1.position - p2.position).getDistance()
                                                         val currentCenter = (p1.position + p2.position) / 2f

                                                         if (currentLayer.isLocked) {
                                                             p1.consume()
                                                             p2.consume()
                                                         } else {
                                                             if (!recordedSnapshot) {
                                                                 viewModel.recordSnapshot()
                                                                 recordedSnapshot = true
                                                             }
                                                             if (prevSpan > 0f) {
                                                                 val zoomFactor = currentSpan / prevSpan
                                                                 val panDelta = currentCenter - prevCenter

                                                                 if (currentLayer.imageUri != null) {
                                                                     val newScale = (currentLayer.imageScale * zoomFactor).coerceIn(0.1f, 10.0f)
                                                                     val framePxW = (canvasWidthPx * currentLayer.width).coerceAtLeast(10f)
                                                                     val framePxH = (canvasHeightPx * currentLayer.height).coerceAtLeast(10f)
                                                                     val newOffsetX = (currentLayer.imageOffsetX + panDelta.x / framePxW).coerceIn(-2.0f, 2.0f)
                                                                     val newOffsetY = (currentLayer.imageOffsetY + panDelta.y / framePxH).coerceIn(-2.0f, 2.0f)
                                                                     viewModel.updateLayer(currentLayer.copy(
                                                                         imageScale = newScale,
                                                                         imageOffsetX = newOffsetX,
                                                                         imageOffsetY = newOffsetY
                                                                     ))
                                                                 } else {
                                                                     val minW = if (canvasWidthPx > 0f) (10f / canvasWidthPx).coerceIn(0.001f, 0.02f) else 0.005f
                                                                     val minH = if (canvasHeightPx > 0f) (10f / canvasHeightPx).coerceIn(0.001f, 0.02f) else 0.005f
                                                                     val newW = (currentLayer.width * zoomFactor).coerceIn(minW, 1f - currentLayer.x)
                                                                     val newH = (currentLayer.height * zoomFactor).coerceIn(minH, 1f - currentLayer.y)
                                                                     viewModel.updateLayer(currentLayer.copy(width = newW, height = newH))
                                                                 }
                                                                 p1.consume()
                                                                 p2.consume()
                                                             }
                                                             prevSpan = currentSpan
                                                             prevCenter = currentCenter
                                                         }
                                                    } else if (pressedPointers.size == 1 && !isPinching) {
                                                         val change = pressedPointers.first()
                                                         val currentFingerCanvasX = (currentLayer.x * canvasWidthPx) + change.position.x
                                                         val currentFingerCanvasY = (currentLayer.y * canvasHeightPx) + change.position.y
                                                         velocityTracker.addPosition(change.uptimeMillis, Offset(currentFingerCanvasX, currentFingerCanvasY))
                                                         val totalDeltaX = currentFingerCanvasX - startTouchCanvasX
                                                         val totalDeltaY = currentFingerCanvasY - startTouchCanvasY
                                                         val distanceMoved = kotlin.math.hypot(totalDeltaX, totalDeltaY)

                                                         if (!isDragging && distanceMoved > 6f) {
                                                             isDragging = true
                                                             if (!recordedSnapshot && !startLayer.isLocked) {
                                                                 viewModel.recordSnapshot()
                                                                 recordedSnapshot = true
                                                             }
                                                         }

                                                         if (isDragging) {
                                                             change.consume()
                                                             if (!startLayer.isLocked) {
                                                                 val targetX = startLayer.x + (totalDeltaX / canvasWidthPx)
                                                                 val targetY = startLayer.y + (totalDeltaY / canvasHeightPx)

                                                                 if (isSnappingEnabled) {
                                                                     val snapResult = AlignmentGuideUtils.calculateMoveSnap(
                                                                         targetX = targetX,
                                                                         targetY = targetY,
                                                                         width = startLayer.width,
                                                                         height = startLayer.height,
                                                                         otherLayers = latestLayers.value.filter { it.id != startLayer.id },
                                                                         threshold = snapThreshold,
                                                                         guideConfig = guideConfig,
                                                                         canvasWidthPx = canvasWidthPx,
                                                                         canvasHeightPx = canvasHeightPx
                                                                     )
                                                                     if (snapResult.snapped && activeGuides.isEmpty()) {
                                                                         haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                     }
                                                                     activeGuides = snapResult.guides
                                                                     lastDraggedX = snapResult.x
                                                                     lastDraggedY = snapResult.y
                                                                     viewModel.updateLayer(startLayer.copy(x = snapResult.x, y = snapResult.y))
                                                                 } else {
                                                                     activeGuides = emptyList()
                                                                     val clampedX = targetX.coerceIn(0f, (1f - startLayer.width).coerceAtLeast(0f))
                                                                     val clampedY = targetY.coerceIn(0f, (1f - startLayer.height).coerceAtLeast(0f))
                                                                     lastDraggedX = clampedX
                                                                     lastDraggedY = clampedY
                                                                     viewModel.updateLayer(
                                                                         startLayer.copy(
                                                                             x = clampedX,
                                                                             y = clampedY
                                                                         )
                                                                     )
                                                                 }
                                                             }
                                                         }
                                                    }
                                                } while (event.changes.any { it.pressed })
                                                activeGuides = emptyList()

                                                // Smooth spring animation with velocity momentum and magnetic snap on release
                                                if (isDragging && !startLayer.isLocked) {
                                                    val velocity = velocityTracker.calculateVelocity()
                                                    val velFractionX = (velocity.x / canvasWidthPx).coerceIn(-6f, 6f)
                                                    val velFractionY = (velocity.y / canvasHeightPx).coerceIn(-6f, 6f)

                                                    // Calculate natural momentum based on release velocity
                                                    val flingDistX = (velFractionX * 0.08f).coerceIn(-0.15f, 0.15f)
                                                    val flingDistY = (velFractionY * 0.08f).coerceIn(-0.15f, 0.15f)
                                                    val projectedX = (lastDraggedX + flingDistX).coerceIn(0f, (1f - startLayer.width).coerceAtLeast(0f))
                                                    val projectedY = (lastDraggedY + flingDistY).coerceIn(0f, (1f - startLayer.height).coerceAtLeast(0f))

                                                    val finalTargetX: Float
                                                    val finalTargetY: Float
                                                    if (isSnappingEnabled) {
                                                        val releaseSnap = AlignmentGuideUtils.calculateMoveSnap(
                                                            targetX = projectedX,
                                                            targetY = projectedY,
                                                            width = startLayer.width,
                                                            height = startLayer.height,
                                                            otherLayers = latestLayers.value.filter { it.id != startLayer.id },
                                                            threshold = snapThreshold * 1.25f,
                                                            guideConfig = guideConfig,
                                                            canvasWidthPx = canvasWidthPx,
                                                            canvasHeightPx = canvasHeightPx
                                                        )
                                                        finalTargetX = releaseSnap.x
                                                        finalTargetY = releaseSnap.y
                                                    } else {
                                                        finalTargetX = projectedX
                                                        finalTargetY = projectedY
                                                    }

                                                    val currentLayerAtRelease = latestLayers.value.find { it.id == layer.id } ?: startLayer
                                                    val startAnimX = currentLayerAtRelease.x
                                                    val startAnimY = currentLayerAtRelease.y

                                                    if (kotlin.math.abs(startAnimX - finalTargetX) > 0.001f ||
                                                        kotlin.math.abs(startAnimY - finalTargetY) > 0.001f ||
                                                        kotlin.math.hypot(velFractionX, velFractionY) > 0.05f
                                                    ) {
                                                        layerAnimJobs[layer.id]?.cancel()
                                                        layerAnimJobs[layer.id] = scope.launch {
                                                            val animX = Animatable(startAnimX)
                                                            val animY = Animatable(startAnimY)

                                                            val jobX = launch {
                                                                animX.animateTo(
                                                                    targetValue = finalTargetX,
                                                                    initialVelocity = velFractionX,
                                                                    animationSpec = spring(
                                                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                                                        stiffness = Spring.StiffnessMediumLow
                                                                    )
                                                                ) {
                                                                    val cur = latestLayers.value.find { it.id == layer.id } ?: startLayer
                                                                    viewModel.updateLayer(cur.copy(x = this.value))
                                                                }
                                                            }

                                                            val jobY = launch {
                                                                animY.animateTo(
                                                                    targetValue = finalTargetY,
                                                                    initialVelocity = velFractionY,
                                                                    animationSpec = spring(
                                                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                                                        stiffness = Spring.StiffnessMediumLow
                                                                    )
                                                                ) {
                                                                    val cur = latestLayers.value.find { it.id == layer.id } ?: startLayer
                                                                    viewModel.updateLayer(cur.copy(y = this.value))
                                                                }
                                                            }

                                                            jobX.join()
                                                            jobY.join()
                                                            val finalCur = latestLayers.value.find { it.id == layer.id } ?: startLayer
                                                            viewModel.updateLayer(finalCur.copy(x = finalTargetX, y = finalTargetY))
                                                        }
                                                    }
                                                }

                                                // If user did not drag or pinch, process tap or double-tap
                                                if (!isDragging && !isPinching) {
                                                    val now = System.currentTimeMillis()
                                                    val cur = latestLayers.value.find { it.id == layer.id } ?: layer
                                                    if (lastTapLayerId == layer.id && (now - lastTapTime) < 320) {
                                                        if (!cur.isLocked) {
                                                            viewModel.recordSnapshot()
                                                            viewModel.updateLayer(cur.copy(imageScale = 1.0f, imageOffsetX = 0f, imageOffsetY = 0f))
                                                        }
                                                        lastTapTime = 0L
                                                        lastTapLayerId = null
                                                    } else {
                                                        lastTapTime = now
                                                        lastTapLayerId = layer.id
                                                        // Tap on unfilled photo frame opens photo picker immediately
                                                        if (cur.imageUri == null) {
                                                            photoPickerLauncher.launch(
                                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                            ) {
                                // Draw layer rotated
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            rotationZ = layer.rotation
                                            alpha = if (layer.isVisible) layer.opacity else 0f
                                        }
                                ) {
                                    // Layer content container
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .drawBehind {
                                                val wPx = size.width
                                                val hPx = size.height
                                                val rPx = (layer.cornerRadius / 360f) * wPx
                                                val fRect = android.graphics.RectF(0f, 0f, wPx, hPx)
                                                drawIntoCanvas { canvas ->
                                                    val nativeCanvas = canvas.nativeCanvas
                                                    // 1. Draw Outer Glow (behind frame)
                                                    if (layer.outerGlowEnabled) {
                                                        val gPx = (layer.outerGlowRadius / 360f) * wPx
                                                        LayerStyleUtils.drawOuterGlow(
                                                            canvas = nativeCanvas,
                                                            frameRect = fRect,
                                                            shapeType = layer.shapeType,
                                                            cornerRadiusPx = rPx,
                                                            glowRadiusPx = gPx,
                                                            glowColor = layer.outerGlowColor
                                                        )
                                                    }
                                                    // 2. Draw Drop Shadow (behind frame)
                                                    if (layer.dropShadowEnabled) {
                                                        val bPx = (layer.dropShadowBlur / 360f) * wPx
                                                        val oX = (layer.dropShadowOffsetX / 360f) * wPx
                                                        val oY = (layer.dropShadowOffsetY / 360f) * wPx
                                                        LayerStyleUtils.drawDropShadow(
                                                            canvas = nativeCanvas,
                                                            frameRect = fRect,
                                                            shapeType = layer.shapeType,
                                                            cornerRadiusPx = rPx,
                                                            blurRadiusPx = bPx,
                                                            offsetX = oX,
                                                            offsetY = oY,
                                                            shadowColor = layer.dropShadowColor
                                                        )
                                                    }
                                                }
                                            }
                                            .then(
                                                // Draw base polaroid style before clipping
                                                if (layer.shapeType == "polaroid") {
                                                    Modifier
                                                        .background(Color.White)
                                                        .shadow(2.dp)
                                                } else Modifier
                                                    .background(Color.Transparent)
                                            )
                                    ) {
                                        // Shape Clip resolver
                                        val clipShape = remember(layer.shapeType, layer.cornerRadius) {
                                            getShapeByType(layer.shapeType, layer.cornerRadius)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .then(
                                                    if (layer.shapeType == "polaroid") {
                                                        // Clip image inside Polaroid bounds
                                                        val bottomGap = 0.18f
                                                        Modifier
                                                            .padding(
                                                                start = widthDp * 0.05f,
                                                                top = heightDp * 0.05f,
                                                                end = widthDp * 0.05f,
                                                                bottom = heightDp * bottomGap
                                                            )
                                                            .clip(RoundedCornerShape(2.dp))
                                                    } else {
                                                        Modifier.clip(clipShape)
                                                    }
                                                )
                                                .background(Color(0xFFE5E5EA))
                                        ) {
                                            if (layer.imageUri != null) {
                                                // Loaded Image
                                                AsyncImage(
                                                    model = layer.imageUri,
                                                    contentDescription = "Layer image",
                                                    contentScale = ContentScale.Crop,
                                                    colorFilter = if (layer.filterId != "none") {
                                                        ColorFilter.colorMatrix(FilterUtils.getColorMatrix(layer.filterId))
                                                    } else null,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        // Manual Zoom (Scale) & Pan (Offsets) inside the frame
                                                        .graphicsLayer {
                                                            scaleX = layer.imageScale
                                                            scaleY = layer.imageScale
                                                            translationX = layer.imageOffsetX * this.size.width
                                                            translationY = layer.imageOffsetY * this.size.height
                                                        }
                                                )
                                            } else {
                                                // Unfilled Frame Placeholder
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    val phScale = (1f / canvasScale.coerceAtLeast(0.15f)).coerceIn(0.5f, 1.2f)
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        modifier = Modifier.graphicsLayer {
                                                            scaleX = phScale
                                                            scaleY = phScale
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.AddAPhoto,
                                                            contentDescription = "Tambah Foto",
                                                            tint = Color.Gray,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text("Tambah Foto", fontSize = 10.sp, color = Color.Gray)
                                                    }
                                                }
                                            }

                                            // 3. Draw Inner Shadow overlay inside clipped frame bounds
                                            if (layer.innerShadowEnabled) {
                                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                                    val wPx = size.width
                                                    val hPx = size.height
                                                    val rPx = (layer.cornerRadius / 360f) * wPx
                                                    val fRect = android.graphics.RectF(0f, 0f, wPx, hPx)
                                                    drawIntoCanvas { canvas ->
                                                        val nativeCanvas = canvas.nativeCanvas
                                                        val inBlurPx = (layer.innerShadowBlur / 360f) * wPx
                                                        val inOffX = (layer.innerShadowOffsetX / 360f) * wPx
                                                        val inOffY = (layer.innerShadowOffsetY / 360f) * wPx
                                                        LayerStyleUtils.drawInnerShadow(
                                                            canvas = nativeCanvas,
                                                            frameRect = fRect,
                                                            shapeType = layer.shapeType,
                                                            cornerRadiusPx = rPx,
                                                            blurRadiusPx = inBlurPx,
                                                            offsetX = inOffX,
                                                            offsetY = inOffY,
                                                            shadowColor = layer.innerShadowColor
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Draw Layer Border Outline (on top of clipped content)
                                        if (layer.borderThickness > 0 && layer.shapeType != "polaroid") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .border(
                                                        width = layer.borderThickness.dp,
                                                        color = Color(layer.borderColor),
                                                        shape = clipShape
                                                    )
                                            )
                                        }

                                        // Lock badge indicator on layer
                                        if (layer.isLocked) {
                                            val lockScale = canvasScale.coerceAtLeast(0.15f)
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .graphicsLayer {
                                                        scaleX = 1f / lockScale
                                                        scaleY = 1f / lockScale
                                                        transformOrigin = TransformOrigin(1f, 0f)
                                                    }
                                                    .size(20.dp)
                                                    .background(Color(0xFFFF9500), CircleShape)
                                                    .shadow(2.dp, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "Lapisan Terkunci",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Draw Selection Bounding Box & 4 Interactive Corner Drag Handles (On Top As Siblings!)
                        val selectedLayer = layers.find { it.id == selectedId }
                        if (selectedLayer != null) {
                            val selLeftDp = canvasWidth * selectedLayer.x
                            val selTopDp = canvasHeight * selectedLayer.y
                            val selWidthDp = canvasWidth * selectedLayer.width
                            val selHeightDp = canvasHeight * selectedLayer.height
                            val isLocked = selectedLayer.isLocked
                            val borderColor = if (isLocked) Color(0xFFFF9500) else Color(0xFF007AFF)

                            // Outer Bounding Border
                            Box(
                                modifier = Modifier
                                    .offset(x = selLeftDp, y = selTopDp)
                                    .size(width = selWidthDp, height = selHeightDp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val safeScale = canvasScale.coerceAtLeast(0.15f)
                                    val strokeOuter = (2.2.dp / safeScale).toPx()
                                    val strokeInner = (1.0.dp / safeScale).toPx()
                                    drawRect(
                                        color = borderColor,
                                        style = Stroke(width = strokeOuter)
                                    )
                                    drawRect(
                                        color = Color.White,
                                        style = Stroke(width = strokeInner)
                                    )
                                }

                                if (isLocked) {
                                    val safeScale = canvasScale.coerceAtLeast(0.15f)
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .offset(y = (-14).dp / safeScale)
                                            .graphicsLayer {
                                                scaleX = 1f / safeScale
                                                scaleY = 1f / safeScale
                                                transformOrigin = TransformOrigin(0.5f, 1f)
                                            }
                                            .clickable {
                                                viewModel.toggleLockLayer(selectedLayer.id)
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFFF9500),
                                        shadowElevation = 4.dp
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Terkunci (Tahan untuk opsi)",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            if (!isLocked) {
                                // Flexible Adaptive Handle Sizing:
                                // Inversely scales with canvasScale so handles maintain an optimal physical screen size
                                // across all zoom levels (10% to 800%).
                                // Also smoothly adapts when the layer itself is small, preventing handles from colliding.
                                val safeScale = canvasScale.coerceAtLeast(0.15f)
                                val baseVisualDp = 15.dp / safeScale
                                val baseTouchDp = 42.dp / safeScale

                                val minLayerDimDp = minOf(selWidthDp, selHeightDp)
                                val maxHandleVisualByLayer = minLayerDimDp * 0.40f
                                val handleVisualSize = if (maxHandleVisualByLayer > 0.dp) {
                                    minOf(baseVisualDp, maxHandleVisualByLayer).coerceAtLeast(3.dp / safeScale)
                                } else {
                                    baseVisualDp
                                }
                                // Touch target scales down gracefully when the layer is small so the 4 corner handles NEVER overlap each other (max 48% of dimension each)
                                val maxTouchByLayer = (minLayerDimDp * 0.48f).coerceAtLeast(handleVisualSize)
                                val handleTouchSize = minOf(baseTouchDp, maxTouchByLayer).coerceAtLeast(handleVisualSize)
                                val halfTouch = handleTouchSize / 2
                                val halfTouchPx = with(density) { halfTouch.toPx() }
                                val handleBorderWidth = (2.dp / safeScale).coerceIn(1.dp, 3.dp)
                                val handleInnerDotSize = (handleVisualSize * 0.35f).coerceAtLeast(2.dp / safeScale)

                                // Minimum layer dimension: fine-grained down to ~10-12px (0.0005f to 0.015f), allowing shrinking freely
                                val minFractionX = if (canvasWidthPx > 0f) (10f / canvasWidthPx).coerceIn(0.0005f, 0.015f) else 0.005f
                                val minFractionY = if (canvasHeightPx > 0f) (10f / canvasHeightPx).coerceIn(0.0005f, 0.015f) else 0.005f

                                // Top-Left Corner Handle (Index 0)
                                Box(
                                    modifier = Modifier
                                        .offset(x = selLeftDp - halfTouch, y = selTopDp - halfTouch)
                                        .size(handleTouchSize)
                                        .pointerInput(selectedLayer.id, canvasWidthPx, canvasHeightPx) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    val down = awaitFirstDown(requireUnconsumed = false)
                                                    down.consume()
                                                    viewModel.recordSnapshot()
                                                    val start = latestLayers.value.find { it.id == selectedLayer.id } ?: selectedLayer
                                                    val touchStartInHandle = down.position
                                                    val startHandleCanvasX = (start.x * canvasWidthPx) - halfTouchPx
                                                    val startHandleCanvasY = (start.y * canvasHeightPx) - halfTouchPx
                                                    val startTouchCanvasX = startHandleCanvasX + touchStartInHandle.x
                                                    val startTouchCanvasY = startHandleCanvasY + touchStartInHandle.y

                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        val change = event.changes.find { it.id == down.id } ?: break
                                                        if (!change.pressed) {
                                                            activeGuides = emptyList()
                                                            break
                                                        }
                                                        change.consume()
                                                        val current = latestLayers.value.find { it.id == selectedLayer.id } ?: start
                                                        val currentHandleCanvasX = (current.x * canvasWidthPx) - halfTouchPx
                                                        val currentHandleCanvasY = (current.y * canvasHeightPx) - halfTouchPx
                                                        val currentTouchCanvasX = currentHandleCanvasX + change.position.x
                                                        val currentTouchCanvasY = currentHandleCanvasY + change.position.y
                                                        val totalDeltaX = currentTouchCanvasX - startTouchCanvasX
                                                        val totalDeltaY = currentTouchCanvasY - startTouchCanvasY

                                                        val maxSafeX = (start.x + start.width - minFractionX).coerceAtLeast(0f)
                                                        val rawX = (start.x + totalDeltaX / canvasWidthPx).coerceIn(0f, maxSafeX)
                                                        val maxSafeY = (start.y + start.height - minFractionY).coerceAtLeast(0f)
                                                        val rawY = (start.y + totalDeltaY / canvasHeightPx).coerceIn(0f, maxSafeY)
                                                        val rawW = (start.width - (rawX - start.x)).coerceIn(minFractionX, (1f - rawX).coerceAtLeast(minFractionX))
                                                        val rawH = (start.height - (rawY - start.y)).coerceIn(minFractionY, (1f - rawY).coerceAtLeast(minFractionY))

                                                        if (isSnappingEnabled) {
                                                            val snapResult = AlignmentGuideUtils.calculateResizeSnap(
                                                                targetX = rawX,
                                                                targetY = rawY,
                                                                targetW = rawW,
                                                                targetH = rawH,
                                                                handleIndex = 0,
                                                                otherLayers = latestLayers.value.filter { it.id != selectedLayer.id },
                                                                minWidth = minFractionX,
                                                                minHeight = minFractionY,
                                                                threshold = snapThreshold,
                                                                guideConfig = guideConfig,
                                                                canvasWidthPx = canvasWidthPx,
                                                                canvasHeightPx = canvasHeightPx
                                                            )
                                                            if (snapResult.guides.isNotEmpty() && activeGuides.isEmpty()) {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            }
                                                            activeGuides = snapResult.guides
                                                            viewModel.updateLayer(start.copy(
                                                                x = snapResult.x,
                                                                y = snapResult.y,
                                                                width = snapResult.width,
                                                                height = snapResult.height
                                                            ))
                                                        } else {
                                                            activeGuides = emptyList()
                                                            viewModel.updateLayer(start.copy(x = rawX, y = rawY, width = rawW, height = rawH))
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(handleVisualSize)
                                            .shadow((2.5.dp / safeScale).coerceIn(1.dp, 4.dp), CircleShape)
                                            .background(Color.White, CircleShape)
                                            .border(handleBorderWidth, Color(0xFF007AFF), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(modifier = Modifier.size(handleInnerDotSize).background(Color(0xFF007AFF), CircleShape))
                                    }
                                }

                                // Top-Right Corner Handle (Index 1)
                                Box(
                                    modifier = Modifier
                                        .offset(x = selLeftDp + selWidthDp - halfTouch, y = selTopDp - halfTouch)
                                        .size(handleTouchSize)
                                        .pointerInput(selectedLayer.id, canvasWidthPx, canvasHeightPx) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    val down = awaitFirstDown(requireUnconsumed = false)
                                                    down.consume()
                                                    viewModel.recordSnapshot()
                                                    val start = latestLayers.value.find { it.id == selectedLayer.id } ?: selectedLayer
                                                    val touchStartInHandle = down.position
                                                    val startHandleCanvasX = (start.x + start.width) * canvasWidthPx - halfTouchPx
                                                    val startHandleCanvasY = (start.y * canvasHeightPx) - halfTouchPx
                                                    val startTouchCanvasX = startHandleCanvasX + touchStartInHandle.x
                                                    val startTouchCanvasY = startHandleCanvasY + touchStartInHandle.y

                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        val change = event.changes.find { it.id == down.id } ?: break
                                                        if (!change.pressed) {
                                                            activeGuides = emptyList()
                                                            break
                                                        }
                                                        change.consume()
                                                        val current = latestLayers.value.find { it.id == selectedLayer.id } ?: start
                                                        val currentHandleCanvasX = (current.x + current.width) * canvasWidthPx - halfTouchPx
                                                        val currentHandleCanvasY = (current.y * canvasHeightPx) - halfTouchPx
                                                        val currentTouchCanvasX = currentHandleCanvasX + change.position.x
                                                        val currentTouchCanvasY = currentHandleCanvasY + change.position.y
                                                        val totalDeltaX = currentTouchCanvasX - startTouchCanvasX
                                                        val totalDeltaY = currentTouchCanvasY - startTouchCanvasY

                                                        val maxSafeY = (start.y + start.height - minFractionY).coerceAtLeast(0f)
                                                        val rawY = (start.y + totalDeltaY / canvasHeightPx).coerceIn(0f, maxSafeY)
                                                        val rawH = (start.height - (rawY - start.y)).coerceIn(minFractionY, (1f - rawY).coerceAtLeast(minFractionY))
                                                        val rawW = (start.width + totalDeltaX / canvasWidthPx).coerceIn(minFractionX, (1f - start.x).coerceAtLeast(minFractionX))

                                                        if (isSnappingEnabled) {
                                                            val snapResult = AlignmentGuideUtils.calculateResizeSnap(
                                                                targetX = start.x,
                                                                targetY = rawY,
                                                                targetW = rawW,
                                                                targetH = rawH,
                                                                handleIndex = 1,
                                                                otherLayers = latestLayers.value.filter { it.id != selectedLayer.id },
                                                                minWidth = minFractionX,
                                                                minHeight = minFractionY,
                                                                threshold = snapThreshold,
                                                                guideConfig = guideConfig,
                                                                canvasWidthPx = canvasWidthPx,
                                                                canvasHeightPx = canvasHeightPx
                                                            )
                                                            if (snapResult.guides.isNotEmpty() && activeGuides.isEmpty()) {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            }
                                                            activeGuides = snapResult.guides
                                                            viewModel.updateLayer(start.copy(
                                                                y = snapResult.y,
                                                                width = snapResult.width,
                                                                height = snapResult.height
                                                            ))
                                                        } else {
                                                            activeGuides = emptyList()
                                                            viewModel.updateLayer(start.copy(y = rawY, width = rawW, height = rawH))
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(handleVisualSize)
                                            .shadow((2.5.dp / safeScale).coerceIn(1.dp, 4.dp), CircleShape)
                                            .background(Color.White, CircleShape)
                                            .border(handleBorderWidth, Color(0xFF007AFF), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(modifier = Modifier.size(handleInnerDotSize).background(Color(0xFF007AFF), CircleShape))
                                    }
                                }

                                // Bottom-Left Corner Handle (Index 2)
                                Box(
                                    modifier = Modifier
                                        .offset(x = selLeftDp - halfTouch, y = selTopDp + selHeightDp - halfTouch)
                                        .size(handleTouchSize)
                                        .pointerInput(selectedLayer.id, canvasWidthPx, canvasHeightPx) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    val down = awaitFirstDown(requireUnconsumed = false)
                                                    down.consume()
                                                    viewModel.recordSnapshot()
                                                    val start = latestLayers.value.find { it.id == selectedLayer.id } ?: selectedLayer
                                                    val touchStartInHandle = down.position
                                                    val startHandleCanvasX = (start.x * canvasWidthPx) - halfTouchPx
                                                    val startHandleCanvasY = (start.y + start.height) * canvasHeightPx - halfTouchPx
                                                    val startTouchCanvasX = startHandleCanvasX + touchStartInHandle.x
                                                    val startTouchCanvasY = startHandleCanvasY + touchStartInHandle.y

                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        val change = event.changes.find { it.id == down.id } ?: break
                                                        if (!change.pressed) {
                                                            activeGuides = emptyList()
                                                            break
                                                        }
                                                        change.consume()
                                                        val current = latestLayers.value.find { it.id == selectedLayer.id } ?: start
                                                        val currentHandleCanvasX = (current.x * canvasWidthPx) - halfTouchPx
                                                        val currentHandleCanvasY = (current.y + current.height) * canvasHeightPx - halfTouchPx
                                                        val currentTouchCanvasX = currentHandleCanvasX + change.position.x
                                                        val currentTouchCanvasY = currentHandleCanvasY + change.position.y
                                                        val totalDeltaX = currentTouchCanvasX - startTouchCanvasX
                                                        val totalDeltaY = currentTouchCanvasY - startTouchCanvasY

                                                        val maxSafeX = (start.x + start.width - minFractionX).coerceAtLeast(0f)
                                                        val rawX = (start.x + totalDeltaX / canvasWidthPx).coerceIn(0f, maxSafeX)
                                                        val rawW = (start.width - (rawX - start.x)).coerceIn(minFractionX, (1f - rawX).coerceAtLeast(minFractionX))
                                                        val rawH = (start.height + totalDeltaY / canvasHeightPx).coerceIn(minFractionY, (1f - start.y).coerceAtLeast(minFractionY))

                                                        if (isSnappingEnabled) {
                                                            val snapResult = AlignmentGuideUtils.calculateResizeSnap(
                                                                targetX = rawX,
                                                                targetY = start.y,
                                                                targetW = rawW,
                                                                targetH = rawH,
                                                                handleIndex = 2,
                                                                otherLayers = latestLayers.value.filter { it.id != selectedLayer.id },
                                                                minWidth = minFractionX,
                                                                minHeight = minFractionY,
                                                                threshold = snapThreshold,
                                                                guideConfig = guideConfig,
                                                                canvasWidthPx = canvasWidthPx,
                                                                canvasHeightPx = canvasHeightPx
                                                            )
                                                            if (snapResult.guides.isNotEmpty() && activeGuides.isEmpty()) {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            }
                                                            activeGuides = snapResult.guides
                                                            viewModel.updateLayer(start.copy(
                                                                x = snapResult.x,
                                                                width = snapResult.width,
                                                                height = snapResult.height
                                                            ))
                                                        } else {
                                                            activeGuides = emptyList()
                                                            viewModel.updateLayer(start.copy(x = rawX, width = rawW, height = rawH))
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(handleVisualSize)
                                            .shadow((2.5.dp / safeScale).coerceIn(1.dp, 4.dp), CircleShape)
                                            .background(Color.White, CircleShape)
                                            .border(handleBorderWidth, Color(0xFF007AFF), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(modifier = Modifier.size(handleInnerDotSize).background(Color(0xFF007AFF), CircleShape))
                                    }
                                }

                                // Bottom-Right Corner Handle (Index 3)
                                Box(
                                    modifier = Modifier
                                        .offset(x = selLeftDp + selWidthDp - halfTouch, y = selTopDp + selHeightDp - halfTouch)
                                        .size(handleTouchSize)
                                        .pointerInput(selectedLayer.id, canvasWidthPx, canvasHeightPx) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    val down = awaitFirstDown(requireUnconsumed = false)
                                                    down.consume()
                                                    viewModel.recordSnapshot()
                                                    val start = latestLayers.value.find { it.id == selectedLayer.id } ?: selectedLayer
                                                    val touchStartInHandle = down.position
                                                    val startHandleCanvasX = (start.x + start.width) * canvasWidthPx - halfTouchPx
                                                    val startHandleCanvasY = (start.y + start.height) * canvasHeightPx - halfTouchPx
                                                    val startTouchCanvasX = startHandleCanvasX + touchStartInHandle.x
                                                    val startTouchCanvasY = startHandleCanvasY + touchStartInHandle.y

                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        val change = event.changes.find { it.id == down.id } ?: break
                                                        if (!change.pressed) {
                                                            activeGuides = emptyList()
                                                            break
                                                        }
                                                        change.consume()
                                                        val current = latestLayers.value.find { it.id == selectedLayer.id } ?: start
                                                        val currentHandleCanvasX = (current.x + current.width) * canvasWidthPx - halfTouchPx
                                                        val currentHandleCanvasY = (current.y + current.height) * canvasHeightPx - halfTouchPx
                                                        val currentTouchCanvasX = currentHandleCanvasX + change.position.x
                                                        val currentTouchCanvasY = currentHandleCanvasY + change.position.y
                                                        val totalDeltaX = currentTouchCanvasX - startTouchCanvasX
                                                        val totalDeltaY = currentTouchCanvasY - startTouchCanvasY

                                                        val rawW = (start.width + totalDeltaX / canvasWidthPx).coerceIn(minFractionX, (1f - start.x).coerceAtLeast(minFractionX))
                                                        val rawH = (start.height + totalDeltaY / canvasHeightPx).coerceIn(minFractionY, (1f - start.y).coerceAtLeast(minFractionY))

                                                        if (isSnappingEnabled) {
                                                            val snapResult = AlignmentGuideUtils.calculateResizeSnap(
                                                                targetX = start.x,
                                                                targetY = start.y,
                                                                targetW = rawW,
                                                                targetH = rawH,
                                                                handleIndex = 3,
                                                                otherLayers = latestLayers.value.filter { it.id != selectedLayer.id },
                                                                minWidth = minFractionX,
                                                                minHeight = minFractionY,
                                                                threshold = snapThreshold,
                                                                guideConfig = guideConfig,
                                                                canvasWidthPx = canvasWidthPx,
                                                                canvasHeightPx = canvasHeightPx
                                                            )
                                                            if (snapResult.guides.isNotEmpty() && activeGuides.isEmpty()) {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            }
                                                            activeGuides = snapResult.guides
                                                            viewModel.updateLayer(start.copy(
                                                                width = snapResult.width,
                                                                height = snapResult.height
                                                            ))
                                                        } else {
                                                            activeGuides = emptyList()
                                                            viewModel.updateLayer(start.copy(width = rawW, height = rawH))
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(handleVisualSize)
                                            .shadow((2.5.dp / safeScale).coerceIn(1.dp, 4.dp), CircleShape)
                                            .background(Color.White, CircleShape)
                                            .border(handleBorderWidth, Color(0xFF007AFF), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(modifier = Modifier.size(handleInnerDotSize).background(Color(0xFF007AFF), CircleShape))
                                    }
                                }
                            }
                    }

                        // 3. Dynamic Alignment Guides Overlay (Visible during move and resize gestures)
                        DynamicAlignmentGuidesOverlay(
                            guides = activeGuides,
                            canvasScale = canvasScale,
                            canvasOffset = canvasOffset,
                            viewportWidthDp = parentW,
                            viewportHeightDp = parentH,
                            modifier = Modifier.fillMaxSize()
                        )

                        // New Guide Layout Overlay
                        NewGuideLayoutOverlay(
                            config = guideConfig,
                            canvasScale = canvasScale,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Floating Zoom & Pan Controls Overlay (Instant Zoom In/Out & Reset)
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        // Zoom Out button
                        IconButton(
                            onClick = { canvasScale = (canvasScale * 0.85f).coerceIn(0.4f, 5.0f) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(16.dp))
                        }

                        // Percentage text / Reset button
                        Text(
                            text = "${(canvasScale * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clickable {
                                    canvasScale = 1f
                                    canvasOffset = Offset.Zero
                                }
                                .padding(horizontal = 6.dp)
                        )

                        // Zoom In button
                        IconButton(
                            onClick = { canvasScale = (canvasScale * 1.15f).coerceIn(0.4f, 5.0f) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
                        }

                        // Fit/Reset button
                        if (canvasScale != 1f || canvasOffset != Offset.Zero) {
                            IconButton(
                                onClick = {
                                    canvasScale = 1f
                                    canvasOffset = Offset.Zero
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.FitScreen, contentDescription = "Reset Zoom", modifier = Modifier.size(16.dp))
                            }
                        }

                        // Divider before Snap Guides Toggle
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .width(1.dp)
                                .height(16.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )

                        // Dynamic Alignment Guides / Magnet Snap Toggle
                        IconButton(
                            onClick = {
                                isSnappingEnabled = !isSnappingEnabled
                                Toast.makeText(
                                    context,
                                    if (isSnappingEnabled) "Garis Panduan Snap: Aktif" else "Garis Panduan Snap: Nonaktif",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("toggle_snap_guides")
                        ) {
                            Icon(
                                imageVector = if (isSnappingEnabled) Icons.Default.FilterCenterFocus else Icons.Default.CropFree,
                                contentDescription = "Toggle Dynamic Alignment Guides",
                                tint = if (isSnappingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Removed Tiled Inspector
                    }
                }
            }

            // 2. Editor Bottom Panel Controls
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                    val selectedLayer = layers.find { it.id == selectedId }

                    if (activeTool == null) {
                        // CANVA STYLE MAIN BOTTOM TOOLBAR (Very compact, scrollable)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Template Selector (Always available)
                            BottomActionItem(
                                icon = Icons.Default.Dashboard,
                                label = "Template",
                                onClick = { activeTool = "template" }
                            )

                            // 2. Add layer (Always available)
                            BottomActionItem(
                                icon = Icons.Default.AddBox,
                                label = "Tambah",
                                onClick = { viewModel.addLayer() }
                            )

                            if (selectedLayer != null) {
                                // 3. Replace Photo (Only when layer selected)
                                BottomActionItem(
                                    icon = Icons.Default.AddPhotoAlternate,
                                    label = "Ganti Foto",
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )

                                // 4. Filters & Effects
                                BottomActionItem(
                                    icon = Icons.Default.ColorLens,
                                    label = "Filter",
                                    onClick = { activeTool = "photo" }
                                )

                                // 5. Frame Size
                                BottomActionItem(
                                    icon = Icons.Default.AspectRatio,
                                    label = "Ukuran",
                                    onClick = { activeTool = "size" }
                                )

                                // 6. Rotation and Depth Positions
                                BottomActionItem(
                                    icon = Icons.Default.Transform,
                                    label = "Posisi",
                                    onClick = { activeTool = "position" }
                                )

                                // 7. Borders, Corners & styling
                                BottomActionItem(
                                    icon = Icons.Default.CropFree,
                                    label = "Bingkai",
                                    onClick = { activeTool = "style" }
                                )

                                // 7b. Layer Styles (Drop Shadow, Inner Shadow, Outer Glow)
                                BottomActionItem(
                                    icon = Icons.Default.AutoAwesome,
                                    label = "Layer Style",
                                    color = MaterialTheme.colorScheme.primary,
                                    onClick = { activeTool = "layer_styles" }
                                )
                            }

                            // 8. Layer Selector & Context Manager (Always available)
                            BottomActionItem(
                                icon = Icons.Default.Layers,
                                label = "Lapisan",
                                color = MaterialTheme.colorScheme.primary,
                                onClick = {
                                    contextMenuLayerId = selectedId
                                    showLayerContextMenu = true
                                }
                            )

                            // 9. Canvas Sizing, Custom Dimensions, Background Colors (Always available)
                            BottomActionItem(
                                icon = Icons.Default.Palette,
                                label = "Kanvas",
                                onClick = { activeTool = "canvas" }
                            )

                            // 10. Cloud template saving (Always available)
                            BottomActionItem(
                                icon = Icons.Default.CloudSync,
                                label = "Cloud",
                                onClick = { activeTool = "cloud" }
                            )

                            if (selectedLayer != null) {
                                // 11. Lock / Unlock Layer
                                BottomActionItem(
                                    icon = if (selectedLayer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    label = if (selectedLayer.isLocked) "Buka Kunci" else "Kunci",
                                    color = if (selectedLayer.isLocked) Color(0xFFFF9500) else MaterialTheme.colorScheme.onSurface,
                                    onClick = {
                                        viewModel.toggleLockLayer(selectedLayer.id)
                                        Toast.makeText(
                                            context,
                                            if (selectedLayer.isLocked) "Kunci dibuka!" else "Lapisan dikunci!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )

                                // 12. Duplicate
                                BottomActionItem(
                                    icon = Icons.Default.ContentCopy,
                                    label = "Duplikat",
                                    onClick = { viewModel.duplicateLayer(selectedLayer) }
                                )

                                // 13. Delete
                                BottomActionItem(
                                    icon = Icons.Default.Delete,
                                    label = "Hapus",
                                    color = MaterialTheme.colorScheme.error,
                                    onClick = { viewModel.removeLayer(selectedLayer.id) }
                                )
                            }
                        }
                    } else {
                        // COMPACT TOOL DETAILS PANEL
                        val toolTitle = when (activeTool) {
                            "template" -> "Kumpulan Style Kolase"
                            "photo" -> "Ganti Foto & Filter"
                            "size" -> "Pengaturan Ukuran & Zoom"
                            "position" -> "Posisi, Rotasi & Lapisan"
                            "style" -> "Bingkai & Sudut Lengkung"
                            "layer_styles" -> "Layer Styles (Shadow & Glow 3D)"
                            "canvas" -> "Pengaturan Luas & Warna Kanvas"
                            "cloud" -> "Cloud Template Share"
                            else -> ""
                        }

                        // Sub-Tool Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = toolTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { activeTool = null }) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selesai",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp) // very compact!
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            when (activeTool) {
                                "template" -> {
                                    // Pilihan Kumpulan Style Kolase (horizontal scroll)
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Text(
                                            "Ketuk gaya kolase di bawah untuk merombak tata letak secara instan:",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            items(com.example.utils.TemplatePresets.presets) { preset ->
                                                Card(
                                                    onClick = {
                                                        viewModel.applyTemplatePreset(preset.id)
                                                        Toast.makeText(context, "Style ${preset.name} diterapkan!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier
                                                        .width(130.dp)
                                                        .fillMaxHeight(),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                    )
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(8.dp).fillMaxSize(),
                                                        verticalArrangement = Arrangement.Center,
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Icon(
                                                            imageVector = when(preset.id) {
                                                                "grid_2x2" -> Icons.Default.GridView
                                                                "aesthetic_trio" -> Icons.Default.ViewQuilt
                                                                "polaroid_duo" -> Icons.Default.Style
                                                                "editorial_cover" -> Icons.Default.AutoStories
                                                                "triple_columns" -> Icons.Default.ViewColumn
                                                                else -> Icons.Default.DashboardCustomize
                                                            },
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            preset.name,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp,
                                                            textAlign = TextAlign.Center,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                "photo" -> {
                                    selectedLayer?.let { layer ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        photoPickerLauncher.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                        )
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Pilih Foto", fontSize = 12.sp)
                                                }

                                                // Shape Selection
                                                val shapes = listOf("rect" to "Kotak", "rounded" to "Tumpul", "oval" to "Bulat", "polaroid" to "Polaroid")
                                                shapes.forEach { (type, name) ->
                                                    FilterChip(
                                                        selected = layer.shapeType == type,
                                                        onClick = {
                                                            viewModel.recordSnapshot()
                                                            viewModel.updateLayer(layer.copy(shapeType = type))
                                                        },
                                                        label = { Text(name, fontSize = 10.sp) },
                                                        modifier = Modifier.padding(2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Quick Preset Demo fillers
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Isi Gradasi Demo: ", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    items(listOf(0, 1, 2, 3, 4)) { style ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    when (style) {
                                                                        0 -> Color(0xFFFF9A9E)
                                                                        1 -> Color(0xFFa1c4fd)
                                                                        2 -> Color(0xFF141e30)
                                                                        3 -> Color(0xFF96e6a1)
                                                                        else -> Color(0xFFf5576c)
                                                                    }
                                                                )
                                                                .clickable { viewModel.setDemoImageToSelected(context, style) }
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Filters List
                                            Text("Filter Aesthetic", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                val filters = listOf("none", "warm", "cool", "bw", "sepia", "vintage", "cinematic", "vivid", "noir")
                                                items(filters) { filterId ->
                                                    val isFilterSelected = layer.filterId == filterId
                                                    FilterChip(
                                                        selected = isFilterSelected,
                                                        onClick = {
                                                            viewModel.recordSnapshot()
                                                            viewModel.updateLayer(layer.copy(filterId = filterId))
                                                        },
                                                        label = { Text(FilterUtils.getFilterName(filterId), fontSize = 10.sp) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                "size" -> {
                                    selectedLayer?.let { layer ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            // Frame Size Quick Preset Buttons
                                            Text("Ukuran Cepat Frame", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                                item {
                                                    FilterChip(
                                                        selected = false,
                                                        onClick = {
                                                            viewModel.recordSnapshot()
                                                            val newW = (layer.width * 1.15f).coerceIn(0.01f, 1f - layer.x)
                                                            val newH = (layer.height * 1.15f).coerceIn(0.01f, 1f - layer.y)
                                                            viewModel.updateLayer(layer.copy(width = newW, height = newH))
                                                        },
                                                        label = { Text("+15% Besar", fontSize = 10.sp) }
                                                    )
                                                }
                                                item {
                                                    FilterChip(
                                                        selected = false,
                                                        onClick = {
                                                            viewModel.recordSnapshot()
                                                            val newW = (layer.width * 0.85f).coerceIn(0.01f, 1f)
                                                            val newH = (layer.height * 0.85f).coerceIn(0.01f, 1f)
                                                            viewModel.updateLayer(layer.copy(width = newW, height = newH))
                                                        },
                                                        label = { Text("-15% Kecil", fontSize = 10.sp) }
                                                    )
                                                }
                                                item {
                                                    FilterChip(
                                                        selected = false,
                                                        onClick = {
                                                            viewModel.recordSnapshot()
                                                            val side = ((layer.width + layer.height) / 2f).coerceIn(0.01f, minOf(1f - layer.x, 1f - layer.y))
                                                            viewModel.updateLayer(layer.copy(width = side, height = side))
                                                        },
                                                        label = { Text("Persegi 1:1", fontSize = 10.sp) }
                                                    )
                                                }
                                                item {
                                                    FilterChip(
                                                        selected = false,
                                                        onClick = {
                                                            viewModel.recordSnapshot()
                                                            val newW = 0.5f.coerceAtMost(1f - layer.x)
                                                            val newH = 0.5f.coerceAtMost(1f - layer.y)
                                                            viewModel.updateLayer(layer.copy(width = newW, height = newH))
                                                        },
                                                        label = { Text("Reset 50%", fontSize = 10.sp) }
                                                    )
                                                }
                                                item {
                                                    FilterChip(
                                                        selected = false,
                                                        onClick = {
                                                            viewModel.recordSnapshot()
                                                            viewModel.updateLayer(layer.copy(x = 0.05f, y = 0.05f, width = 0.9f, height = 0.9f))
                                                        },
                                                        label = { Text("Pas Layar (90%)", fontSize = 10.sp) }
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            // Frame Relative Scale Sliders
                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text("Lebar Frame", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                                Text("${(layer.width * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                            Slider(
                                                value = layer.width,
                                                onValueChange = {
                                                    if (!isSliderInteracting) {
                                                        viewModel.recordSnapshot()
                                                        isSliderInteracting = true
                                                    }
                                                    viewModel.updateLayer(layer.copy(width = it))
                                                },
                                                onValueChangeFinished = { isSliderInteracting = false },
                                                valueRange = 0.01f..1.0f,
                                                modifier = Modifier.height(24.dp)
                                            )

                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text("Tinggi Frame", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                                Text("${(layer.height * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                            Slider(
                                                value = layer.height,
                                                onValueChange = {
                                                    if (!isSliderInteracting) {
                                                        viewModel.recordSnapshot()
                                                        isSliderInteracting = true
                                                    }
                                                    viewModel.updateLayer(layer.copy(height = it))
                                                },
                                                onValueChangeFinished = { isSliderInteracting = false },
                                                valueRange = 0.01f..1.0f,
                                                modifier = Modifier.height(24.dp)
                                            )

                                            // Photo zoom/scale & pan sliders
                                            if (layer.imageUri != null) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Zoom & Geser Foto Dalam Frame", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                                    item {
                                                        FilterChip(
                                                            selected = false,
                                                            onClick = {
                                                                viewModel.recordSnapshot()
                                                                val newScale = (layer.imageScale * 1.2f).coerceIn(0.1f, 10.0f)
                                                                viewModel.updateLayer(layer.copy(imageScale = newScale))
                                                            },
                                                            label = { Text("Zoom In +20%", fontSize = 10.sp) }
                                                        )
                                                    }
                                                    item {
                                                        FilterChip(
                                                            selected = false,
                                                            onClick = {
                                                                viewModel.recordSnapshot()
                                                                val newScale = (layer.imageScale * 0.8f).coerceIn(0.1f, 10.0f)
                                                                viewModel.updateLayer(layer.copy(imageScale = newScale))
                                                            },
                                                            label = { Text("Zoom Out -20%", fontSize = 10.sp) }
                                                        )
                                                    }
                                                    item {
                                                        FilterChip(
                                                            selected = false,
                                                            onClick = {
                                                                viewModel.recordSnapshot()
                                                                viewModel.updateLayer(layer.copy(imageScale = 1.0f, imageOffsetX = 0f, imageOffsetY = 0f))
                                                            },
                                                            label = { Text("Reset Zoom 1x", fontSize = 10.sp) }
                                                        )
                                                    }
                                                }

                                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                    Text("Zoom Foto (Skala)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                                    Text("x${String.format("%.2f", layer.imageScale)}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                                Slider(
                                                    value = layer.imageScale,
                                                    onValueChange = {
                                                        if (!isSliderInteracting) {
                                                            viewModel.recordSnapshot()
                                                            isSliderInteracting = true
                                                        }
                                                        viewModel.updateLayer(layer.copy(imageScale = it))
                                                    },
                                                    onValueChangeFinished = { isSliderInteracting = false },
                                                    valueRange = 0.1f..6.0f,
                                                    modifier = Modifier.height(24.dp)
                                                )

                                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                    Text("Geser Foto (Horizontal)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                                    Text(String.format("%.2f", layer.imageOffsetX), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                                Slider(
                                                    value = layer.imageOffsetX,
                                                    onValueChange = {
                                                        if (!isSliderInteracting) {
                                                            viewModel.recordSnapshot()
                                                            isSliderInteracting = true
                                                        }
                                                        viewModel.updateLayer(layer.copy(imageOffsetX = it))
                                                    },
                                                    onValueChangeFinished = { isSliderInteracting = false },
                                                    valueRange = -1.0f..1.0f,
                                                    modifier = Modifier.height(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                "position" -> {
                                    selectedLayer?.let { layer ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            // Frame Rotation Quick Buttons
                                            Text("Preset Rotasi Cepat", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                                item {
                                                    FilterChip(
                                                        selected = layer.rotation == 0f,
                                                        onClick = {
                                                            viewModel.recordSnapshot()
                                                            viewModel.updateLayer(layer.copy(rotation = 0f))
                                                        },
                                                        label = { Text("0° Tegak", fontSize = 10.sp) }
                                                    )
                                                }
                                                item {
                                                    FilterChip(
                                                        selected = false,
                                                        onClick = {
                                                            viewModel.recordSnapshot()
                                                            val newRot = (layer.rotation - 90f) % 360f
                                                            viewModel.updateLayer(layer.copy(rotation = newRot))
                                                        },
                                                        label = { Text("-90° Kiri", fontSize = 10.sp) }
                                                    )
                                                }
                                                item {
                                                    FilterChip(
                                                        selected = false,
                                                        onClick = {
                                                            viewModel.recordSnapshot()
                                                            val newRot = (layer.rotation + 90f) % 360f
                                                            viewModel.updateLayer(layer.copy(rotation = newRot))
                                                        },
                                                        label = { Text("+90° Kanan", fontSize = 10.sp) }
                                                    )
                                                }
                                            }

                                            // Frame Rotation Slider
                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text("Rotasi Frame Presisi", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                                Text("${layer.rotation.toInt()}°", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                            Slider(
                                                value = layer.rotation,
                                                onValueChange = {
                                                    if (!isSliderInteracting) {
                                                        viewModel.recordSnapshot()
                                                        isSliderInteracting = true
                                                    }
                                                    viewModel.updateLayer(layer.copy(rotation = it))
                                                },
                                                onValueChangeFinished = { isSliderInteracting = false },
                                                valueRange = -180f..180f,
                                                modifier = Modifier.height(24.dp)
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Layer zIndex ordering
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = { viewModel.bringToFront(layer.id) },
                                                    modifier = Modifier.weight(1f),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Icon(Icons.Default.VerticalAlignTop, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Ke Depan", fontSize = 11.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = { viewModel.sendToBack(layer.id) },
                                                    modifier = Modifier.weight(1f),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Icon(Icons.Default.VerticalAlignBottom, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Ke Belakang", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                "style" -> {
                                    selectedLayer?.let { layer ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            // Border Thickness
                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text("Tebal Garis Tepi", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                                Text("${layer.borderThickness.toInt()} dp", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                            Slider(
                                                value = layer.borderThickness,
                                                onValueChange = {
                                                    if (!isSliderInteracting) {
                                                        viewModel.recordSnapshot()
                                                        isSliderInteracting = true
                                                    }
                                                    viewModel.updateLayer(layer.copy(borderThickness = it))
                                                },
                                                onValueChangeFinished = { isSliderInteracting = false },
                                                valueRange = 0f..20f,
                                                modifier = Modifier.height(24.dp)
                                            )

                                            // Corner radius
                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text("Lengkung Sudut (Corner)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                                Text("${layer.cornerRadius.toInt()} dp", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                            Slider(
                                                value = layer.cornerRadius,
                                                onValueChange = {
                                                    if (!isSliderInteracting) {
                                                        viewModel.recordSnapshot()
                                                        isSliderInteracting = true
                                                    }
                                                    viewModel.updateLayer(layer.copy(cornerRadius = it))
                                                },
                                                onValueChangeFinished = { isSliderInteracting = false },
                                                valueRange = 0f..80f,
                                                modifier = Modifier.height(24.dp)
                                            )

                                            // Border color
                                            Text("Warna Garis Tepi", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            ) {
                                                val borderColors = listOf(0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFFFF9500.toInt(), 0xFF34C759.toInt(), 0xFF007AFF.toInt(), 0xFF5856D6.toInt(), 0xFFFF2D55.toInt())
                                                items(borderColors) { colorInt ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(colorInt))
                                                            .border(
                                                                width = if (layer.borderColor == colorInt) 2.dp else 1.dp,
                                                                color = if (layer.borderColor == colorInt) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                                shape = CircleShape
                                                            )
                                                            .clickable {
                                                                viewModel.recordSnapshot()
                                                                viewModel.updateLayer(layer.copy(borderColor = colorInt))
                                                            }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                "layer_styles" -> {
                                    selectedLayer?.let { layer ->
                                        var styleTab by remember { mutableStateOf(0) } // 0: Drop Shadow, 1: Inner Shadow, 2: Outer Glow
                                        
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            // 1. Quick Presets Row
                                            Text("Preset Kedalaman & Pendar Cepat", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            ) {
                                                items(LayerStyleUtils.presets) { preset ->
                                                    FilterChip(
                                                        selected = false,
                                                        onClick = {
                                                            viewModel.recordSnapshot()
                                                            val updated = preset.apply(layer)
                                                            viewModel.updateLayer(updated)
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            Toast.makeText(context, "Preset ${preset.name} diterapkan!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        label = { Text(preset.name, fontSize = 10.sp) }
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            // 2. Style Sub Tabs
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                FilterChip(
                                                    selected = styleTab == 0,
                                                    onClick = { styleTab = 0 },
                                                    label = { Text("Bayangan Luar", fontSize = 10.sp) },
                                                    leadingIcon = {
                                                        if (layer.dropShadowEnabled) {
                                                            Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                FilterChip(
                                                    selected = styleTab == 1,
                                                    onClick = { styleTab = 1 },
                                                    label = { Text("Bayangan Dalam", fontSize = 10.sp) },
                                                    leadingIcon = {
                                                        if (layer.innerShadowEnabled) {
                                                            Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                FilterChip(
                                                    selected = styleTab == 2,
                                                    onClick = { styleTab = 2 },
                                                    label = { Text("Pendar (Glow)", fontSize = 10.sp) },
                                                    leadingIcon = {
                                                        if (layer.outerGlowEnabled) {
                                                            Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            when (styleTab) {
                                                0 -> {
                                                    // DROP SHADOW
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text("Aktifkan Bayangan Jatuh (Drop Shadow)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                            Text("Efek bayangan di belakang objek memberi ilusi melayang", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                                        }
                                                        Switch(
                                                            checked = layer.dropShadowEnabled,
                                                            onCheckedChange = { enabled ->
                                                                viewModel.recordSnapshot()
                                                                viewModel.updateLayer(layer.copy(dropShadowEnabled = enabled))
                                                            },
                                                            modifier = Modifier.testTag("toggle_drop_shadow")
                                                        )
                                                    }

                                                    if (layer.dropShadowEnabled) {
                                                        // Blur Slider
                                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                            Text("Kehalusan (Blur Radius)", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                                            Text("${layer.dropShadowBlur.toInt()} dp", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                        }
                                                        Slider(
                                                            value = layer.dropShadowBlur,
                                                            onValueChange = {
                                                                if (!isSliderInteracting) {
                                                                    viewModel.recordSnapshot()
                                                                    isSliderInteracting = true
                                                                }
                                                                viewModel.updateLayer(layer.copy(dropShadowBlur = it))
                                                            },
                                                            onValueChangeFinished = { isSliderInteracting = false },
                                                            valueRange = 0f..40f,
                                                            modifier = Modifier.height(22.dp)
                                                        )

                                                        // Offset X Slider
                                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                            Text("Jarak Geser X (Horizontal)", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                                            Text("${layer.dropShadowOffsetX.toInt()} dp", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                        }
                                                        Slider(
                                                            value = layer.dropShadowOffsetX,
                                                            onValueChange = {
                                                                if (!isSliderInteracting) {
                                                                    viewModel.recordSnapshot()
                                                                    isSliderInteracting = true
                                                                }
                                                                viewModel.updateLayer(layer.copy(dropShadowOffsetX = it))
                                                            },
                                                            onValueChangeFinished = { isSliderInteracting = false },
                                                            valueRange = -30f..30f,
                                                            modifier = Modifier.height(22.dp)
                                                        )

                                                        // Offset Y Slider
                                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                            Text("Jarak Geser Y (Vertikal)", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                                            Text("${layer.dropShadowOffsetY.toInt()} dp", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                        }
                                                        Slider(
                                                            value = layer.dropShadowOffsetY,
                                                            onValueChange = {
                                                                if (!isSliderInteracting) {
                                                                    viewModel.recordSnapshot()
                                                                    isSliderInteracting = true
                                                                }
                                                                viewModel.updateLayer(layer.copy(dropShadowOffsetY = it))
                                                            },
                                                            onValueChangeFinished = { isSliderInteracting = false },
                                                            valueRange = -30f..30f,
                                                            modifier = Modifier.height(22.dp)
                                                        )

                                                        // Shadow Color Palette
                                                        Text("Warna & Intensitas Bayangan", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                                        LazyRow(
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            modifier = Modifier.padding(vertical = 4.dp)
                                                        ) {
                                                            val shadowColors = listOf(
                                                                0x40000000.toInt() to "Soft Hitam",
                                                                0x80000000.toInt() to "Medium Hitam",
                                                                0xCC000000.toInt() to "Solid Hitam",
                                                                0x661A237E.toInt() to "Deep Navy",
                                                                0x663E2723.toInt() to "Warm Brown",
                                                                0x66424242.toInt() to "Charcoal",
                                                                0x66FFFFFF.toInt() to "Soft Putih"
                                                            )
                                                            items(shadowColors) { (colorInt, label) ->
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(24.dp)
                                                                        .clip(CircleShape)
                                                                        .background(Color(colorInt))
                                                                        .border(
                                                                            width = if (layer.dropShadowColor == colorInt) 2.dp else 1.dp,
                                                                            color = if (layer.dropShadowColor == colorInt) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                                            shape = CircleShape
                                                                        )
                                                                        .clickable {
                                                                            viewModel.recordSnapshot()
                                                                            viewModel.updateLayer(layer.copy(dropShadowColor = colorInt))
                                                                        }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                1 -> {
                                                    // INNER SHADOW
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text("Aktifkan Bayangan Dalam (Inner Shadow)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                            Text("Efek kedalaman inset atau lekukan di dalam frame", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                                        }
                                                        Switch(
                                                            checked = layer.innerShadowEnabled,
                                                            onCheckedChange = { enabled ->
                                                                viewModel.recordSnapshot()
                                                                viewModel.updateLayer(layer.copy(innerShadowEnabled = enabled))
                                                            },
                                                            modifier = Modifier.testTag("toggle_inner_shadow")
                                                        )
                                                    }

                                                    if (layer.innerShadowEnabled) {
                                                        // Inner Blur Slider
                                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                            Text("Kedalaman Inset (Blur)", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                                            Text("${layer.innerShadowBlur.toInt()} dp", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                        }
                                                        Slider(
                                                            value = layer.innerShadowBlur,
                                                            onValueChange = {
                                                                if (!isSliderInteracting) {
                                                                    viewModel.recordSnapshot()
                                                                    isSliderInteracting = true
                                                                }
                                                                viewModel.updateLayer(layer.copy(innerShadowBlur = it))
                                                            },
                                                            onValueChangeFinished = { isSliderInteracting = false },
                                                            valueRange = 1f..30f,
                                                            modifier = Modifier.height(22.dp)
                                                        )

                                                        // Inner Offset X
                                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                            Text("Arah Cahaya X", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                                            Text("${layer.innerShadowOffsetX.toInt()} dp", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                        }
                                                        Slider(
                                                            value = layer.innerShadowOffsetX,
                                                            onValueChange = {
                                                                if (!isSliderInteracting) {
                                                                    viewModel.recordSnapshot()
                                                                    isSliderInteracting = true
                                                                }
                                                                viewModel.updateLayer(layer.copy(innerShadowOffsetX = it))
                                                            },
                                                            onValueChangeFinished = { isSliderInteracting = false },
                                                            valueRange = -20f..20f,
                                                            modifier = Modifier.height(22.dp)
                                                        )

                                                        // Inner Offset Y
                                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                            Text("Arah Cahaya Y", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                                            Text("${layer.innerShadowOffsetY.toInt()} dp", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                        }
                                                        Slider(
                                                            value = layer.innerShadowOffsetY,
                                                            onValueChange = {
                                                                if (!isSliderInteracting) {
                                                                    viewModel.recordSnapshot()
                                                                    isSliderInteracting = true
                                                                }
                                                                viewModel.updateLayer(layer.copy(innerShadowOffsetY = it))
                                                            },
                                                            onValueChangeFinished = { isSliderInteracting = false },
                                                            valueRange = -20f..20f,
                                                            modifier = Modifier.height(22.dp)
                                                        )

                                                        // Inner Color Palette
                                                        Text("Warna Bayangan Dalam", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                                        LazyRow(
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            modifier = Modifier.padding(vertical = 4.dp)
                                                        ) {
                                                            val innerColors = listOf(
                                                                0x80000000.toInt() to "Hitam 50%",
                                                                0xB0000000.toInt() to "Hitam 70%",
                                                                0x90212121.toInt() to "Dark Slate",
                                                                0x903E2723.toInt() to "Vintage Brown",
                                                                0x900D47A1.toInt() to "Deep Blue"
                                                            )
                                                            items(innerColors) { (colorInt, label) ->
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(24.dp)
                                                                        .clip(CircleShape)
                                                                        .background(Color(colorInt))
                                                                        .border(
                                                                            width = if (layer.innerShadowColor == colorInt) 2.dp else 1.dp,
                                                                            color = if (layer.innerShadowColor == colorInt) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                                            shape = CircleShape
                                                                        )
                                                                        .clickable {
                                                                            viewModel.recordSnapshot()
                                                                            viewModel.updateLayer(layer.copy(innerShadowColor = colorInt))
                                                                        }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                2 -> {
                                                    // OUTER GLOW
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text("Aktifkan Pendar Luar (Outer Glow)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                            Text("Pendar cahaya neon di sekeliling frame", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                                        }
                                                        Switch(
                                                            checked = layer.outerGlowEnabled,
                                                            onCheckedChange = { enabled ->
                                                                viewModel.recordSnapshot()
                                                                viewModel.updateLayer(layer.copy(outerGlowEnabled = enabled))
                                                            },
                                                            modifier = Modifier.testTag("toggle_outer_glow")
                                                        )
                                                    }

                                                    if (layer.outerGlowEnabled) {
                                                        // Radius Slider
                                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                            Text("Jangkauan Pendar (Radius Glow)", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                                            Text("${layer.outerGlowRadius.toInt()} dp", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                        }
                                                        Slider(
                                                            value = layer.outerGlowRadius,
                                                            onValueChange = {
                                                                if (!isSliderInteracting) {
                                                                    viewModel.recordSnapshot()
                                                                    isSliderInteracting = true
                                                                }
                                                                viewModel.updateLayer(layer.copy(outerGlowRadius = it))
                                                            },
                                                            onValueChangeFinished = { isSliderInteracting = false },
                                                            valueRange = 2f..50f,
                                                            modifier = Modifier.height(22.dp)
                                                        )

                                                        // Glow Color Palette
                                                        Text("Pilihan Warna Neon & Cahaya", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                                        LazyRow(
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            modifier = Modifier.padding(vertical = 4.dp)
                                                        ) {
                                                            val glowColors = listOf(
                                                                0xFF00E5FF.toInt() to "Neon Cyan",
                                                                0xFFFFD700.toInt() to "Gold Sun",
                                                                0xFFFF2D55.toInt() to "Neon Pink",
                                                                0xFF00E676.toInt() to "Lime Green",
                                                                0xFF2979FF.toInt() to "Electric Blue",
                                                                0xFFD500F9.toInt() to "Cyber Violet",
                                                                0xFFFFFFFF.toInt() to "Pure White",
                                                                0xFFFF6D00.toInt() to "Vibrant Orange"
                                                            )
                                                            items(glowColors) { (colorInt, label) ->
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(24.dp)
                                                                        .clip(CircleShape)
                                                                        .background(Color(colorInt))
                                                                        .border(
                                                                            width = if (layer.outerGlowColor == colorInt) 2.dp else 1.dp,
                                                                            color = if (layer.outerGlowColor == colorInt) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                                            shape = CircleShape
                                                                        )
                                                                        .clickable {
                                                                            viewModel.recordSnapshot()
                                                                            viewModel.updateLayer(layer.copy(outerGlowColor = colorInt))
                                                                        }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                "canvas" -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        // -------------------------------------------------------------
                                        // 1. MANUAL INPUT DIMENSI W x H (GAMBAR 1) - BEBAS & KUSTOM
                                        // -------------------------------------------------------------
                                        val ratioParts = activeAspect.split(":")
                                        val curW = ratioParts.getOrNull(0)?.toIntOrNull() ?: 3000
                                        val curH = ratioParts.getOrNull(1)?.toIntOrNull() ?: 3000

                                        var inputW by remember(activeAspect) { mutableStateOf(curW.toString()) }
                                        var inputH by remember(activeAspect) { mutableStateOf(curH.toString()) }

                                        Text("SETTING LUAS DIMENSI KANVAS MANUAL (W x H)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        Text("Ketik dimensi piksel target untuk cetak profesional (Maks 13500px):", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        
                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = inputW,
                                                onValueChange = { newValue ->
                                                    if (newValue.all { it.isDigit() } && newValue.length <= 5) {
                                                        inputW = newValue
                                                    }
                                                },
                                                label = { Text("Lebar (W) px", fontSize = 10.sp) },
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Number
                                                ),
                                                singleLine = true,
                                                modifier = Modifier.weight(1f).testTag("canvas_width_input")
                                            )
                                            
                                            Text("×", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                                            OutlinedTextField(
                                                value = inputH,
                                                onValueChange = { newValue ->
                                                    if (newValue.all { it.isDigit() } && newValue.length <= 5) {
                                                        inputH = newValue
                                                    }
                                                },
                                                label = { Text("Tinggi (H) px", fontSize = 10.sp) },
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Number
                                                ),
                                                singleLine = true,
                                                modifier = Modifier.weight(1f).testTag("canvas_height_input")
                                            )

                                            Button(
                                                onClick = {
                                                    val pw = (inputW.toIntOrNull() ?: curW).coerceIn(100, 13500)
                                                    val ph = (inputH.toIntOrNull() ?: curH).coerceIn(100, 13500)
                                                    inputW = pw.toString()
                                                    inputH = ph.toString()
                                                    viewModel.updateCanvasDimensions(pw, ph)
                                                    exportWidth = pw.toFloat()
                                                    Toast.makeText(context, "Dimensi kanvas: ${pw}×${ph}px", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.testTag("apply_dimensions_button"),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = "Terapkan", modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("Set", fontSize = 11.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // 2. Aspect Ratio / Dimension Presets
                                        Text("Preset Rasio Cepat (Isi W & H Otomatis)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            val aspectPresets = listOf(
                                                "3000:3000" to "Square 1:1",
                                                "3000:3750" to "Sosmed Portrait 4:5",
                                                "3000:5333" to "Story 9:16",
                                                "3840:2160" to "4K Landscape 16:9",
                                                "3000:4000" to "Standard 3:4"
                                            )
                                            items(aspectPresets) { (dimensions, label) ->
                                                FilterChip(
                                                    selected = activeAspect == dimensions,
                                                    onClick = { 
                                                        val parts = dimensions.split(":")
                                                        val pw = parts.getOrNull(0)?.toIntOrNull() ?: 3000
                                                        val ph = parts.getOrNull(1)?.toIntOrNull() ?: 3000
                                                        inputW = pw.toString()
                                                        inputH = ph.toString()
                                                        viewModel.updateCanvasDimensions(pw, ph)
                                                        exportWidth = pw.toFloat()
                                                    },
                                                    label = { Text(label, fontSize = 10.sp) }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // 3. Background Color
                                        Text("Warna Latar Belakang Kanvas", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            val colors = listOf(0xFFFFFFFF.toInt(), 0xFFF5F5F7.toInt(), 0xFFE5E5EA.toInt(), 0xFFF2EFE9.toInt(), 0xFFE2D6CA.toInt(), 0xFFD4E2D5.toInt(), 0xFFE8D3C3.toInt(), 0xFF1C1C1E.toInt())
                                            items(colors) { colorInt ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(colorInt))
                                                        .border(
                                                            width = if (activeBgColor == colorInt) 2.dp else 1.dp,
                                                            color = if (activeBgColor == colorInt) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                            shape = CircleShape
                                                        )
                                                        .clickable { viewModel.updateBackgroundColor(colorInt) }
                                                )
                                            }
                                        }
                                    }
                                }

                                "cloud" -> {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text("Bagikan Konfigurasi Layout ke Cloud", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Salin kode teks di bawah ini untuk dibagikan ke pengguna lain:", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                val json = viewModel.exportLayoutConfigJson()
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("KolaSTIK Layout", json)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Kode Layout disalin!", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Salin Kode Layout", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // New Guide Layout Dialog (Photoshop Style: View > Guides > New Guide Layout...)
    if (showNewGuideLayoutDialog) {
        var enabledState by remember { mutableStateOf(guideConfig.enabled) }
        var colsNumText by remember { mutableStateOf(guideConfig.columnsNumber.toString()) }
        var colWidthText by remember { mutableStateOf(guideConfig.columnWidthPx.toString()) }
        var gutterText by remember { mutableStateOf(guideConfig.gutterPx.toString()) }
        var marginTopText by remember { mutableStateOf(guideConfig.marginTopPx.toString()) }
        var marginBottomText by remember { mutableStateOf(guideConfig.marginBottomPx.toString()) }
        var marginLeftText by remember { mutableStateOf(guideConfig.marginLeftPx.toString()) }
        var marginRightText by remember { mutableStateOf(guideConfig.marginRightPx.toString()) }

        AlertDialog(
            onDismissRequest = { showNewGuideLayoutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.GridOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                    Text("New Guide Layout", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Aktifkan Panduan Layout", fontWeight = FontWeight.Medium)
                        Switch(
                            checked = enabledState,
                            onCheckedChange = { enabledState = it }
                        )
                    }

                    Divider()
                    Text("Columns (Kolom)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = colsNumText,
                        onValueChange = { colsNumText = it },
                        label = { Text("Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = colWidthText,
                        onValueChange = { colWidthText = it },
                        label = { Text("Width (px)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = gutterText,
                        onValueChange = { gutterText = it },
                        label = { Text("Gutter (px) [agar tidak ada jeda antar slide]") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Divider()
                    Text("Margins (Area Aman Teks/Elemen)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = marginTopText,
                        onValueChange = { marginTopText = it },
                        label = { Text("Top (px)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = marginBottomText,
                        onValueChange = { marginBottomText = it },
                        label = { Text("Bottom (px)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = marginLeftText,
                        onValueChange = { marginLeftText = it },
                        label = { Text("Left (px)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = marginRightText,
                        onValueChange = { marginRightText = it },
                        label = { Text("Right (px)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateGuideLayoutConfig(
                            MainViewModel.GuideLayoutConfig(
                                enabled = enabledState,
                                columnsNumber = colsNumText.toIntOrNull() ?: 3,
                                columnWidthPx = colWidthText.toFloatOrNull() ?: 0f,
                                gutterPx = gutterText.toFloatOrNull() ?: 0f,
                                marginTopPx = marginTopText.toFloatOrNull() ?: 100f,
                                marginBottomPx = marginBottomText.toFloatOrNull() ?: 100f,
                                marginLeftPx = marginLeftText.toFloatOrNull() ?: 100f,
                                marginRightPx = marginRightText.toFloatOrNull() ?: 100f
                            )
                        )
                        showNewGuideLayoutDialog = false
                    }
                ) {
                    Text("Terapkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGuideLayoutDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Snapping Sensitivity Settings Dialog
    if (showSnapSettingsDialog) {
        var tempSnapThreshold by remember { mutableStateOf(snapThreshold) }

        AlertDialog(
            onDismissRequest = { showSnapSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                    Text("Sensitivitas Magnetic Snap", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Atur jarak radius magnetik agar objek menempel ke garis panduan.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val percentStr = "${(tempSnapThreshold * 100f).toInt()}%"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Threshold Jarak:", fontWeight = FontWeight.Medium)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = percentStr,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Slider(
                        value = tempSnapThreshold,
                        onValueChange = { tempSnapThreshold = it },
                        valueRange = 0.005f..0.045f,
                        steps = 8
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ketat (0.5%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text("Normal (1.8%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text("Longgar (4.5%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSnapThreshold(tempSnapThreshold)
                        showSnapSettingsDialog = false
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSnapSettingsDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // High-Resolution Export Dialog Modal
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Ekspor Resolusi Tinggi")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Pilih format dan resolusi ekspor profesional. Aplikasi akan merender elemen langsung ke kanvas kualitas asli tanpa pecah.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Format JPG/PNG selector
                    Text("Format File", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedCard(
                            onClick = { exportFormat = Bitmap.CompressFormat.JPEG },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (exportFormat == Bitmap.CompressFormat.JPEG) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Box(modifier = Modifier.padding(10.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("JPG (Kecil & Efisien)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        OutlinedCard(
                            onClick = { exportFormat = Bitmap.CompressFormat.PNG },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (exportFormat == Bitmap.CompressFormat.PNG) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Box(modifier = Modifier.padding(10.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("PNG (Detail Sempurna)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Width Selection presets & slider
                    val isTiledModeRecommended = exportWidth >= 5000f
                    val resolutionText = remember(exportWidth) {
                        val w = exportWidth.toInt()
                        when {
                            w < 2000 -> "1080px (Media Sosial HD)"
                            w < 4000 -> "3000px (Cetak Sedang / Web)"
                            w < 8000 -> "6000px (4K Print / Poster)"
                            else -> "${w}px (Ultra-High Res 13500px Max)"
                        }
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Resolusi Ekspor (Lebar)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(resolutionText, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    Slider(
                        value = exportWidth,
                        onValueChange = { exportWidth = it },
                        valueRange = 1080f..13500f,
                        modifier = Modifier.testTag("export_resolution_slider")
                    )

                    // Presets quick click
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1080f to "1080px", 3000f to "3000px", 6000f to "6000px", 10800f to "10800px", 13500f to "13500px").forEach { (v, name) ->
                            InputChip(
                                selected = exportWidth.toInt() == v.toInt(),
                                onClick = { exportWidth = v },
                                label = { Text(name, fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tiled Rendering Engine Mode & Banner Grid Option
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GridOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isTiledModeRecommended) "Tiled Rendering Engine (Anti-OOM Aktif)" else "Mode Render Ekspor",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "Membagi kanvas menjadi tile 2048px untuk mencegah crash dan menjaga memori tetap ringan pada resolusi masif.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = tiledExportMode == TiledExportMode.SINGLE_MASTER,
                                    onClick = { tiledExportMode = TiledExportMode.SINGLE_MASTER },
                                    label = { Text("1 File Master Utuh", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = tiledExportMode == TiledExportMode.MULTI_PANEL_GRID,
                                    onClick = { tiledExportMode = TiledExportMode.MULTI_PANEL_GRID },
                                    label = { Text("Grid Panel Cetak Banner", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (exportFormat == Bitmap.CompressFormat.JPEG) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Kualitas JPG", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${exportQuality.toInt()}%", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = exportQuality,
                            onValueChange = { exportQuality = it },
                            valueRange = 50f..100f
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportDialog = false
                        val targetW = exportWidth.toInt()
                        if (targetW >= 5000 || tiledExportMode == TiledExportMode.MULTI_PANEL_GRID) {
                            // Run Tiled Ultra-Resolution Pipeline
                            viewModel.exportTiledCollage(
                                context = context,
                                targetWidth = targetW,
                                format = exportFormat,
                                quality = exportQuality.toInt(),
                                mode = tiledExportMode
                            ) { uris, error ->
                                if (error != null) {
                                    Toast.makeText(context, "Selesai: $error", Toast.LENGTH_LONG).show()
                                } else if (!uris.isNullOrEmpty()) {
                                    val msg = if (uris.size > 1) "Sukses! ${uris.size} panel tile resolusi tinggi tersimpan di Galeri." else "Sukses! Kolase ultra-HD berhasil dirender dan disimpan di Galeri."
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            // Standard Single Pass
                            viewModel.exportCollage(
                                context = context,
                                targetWidth = targetW,
                                format = exportFormat,
                                quality = exportQuality.toInt()
                            ) { uri, error ->
                                if (error != null) {
                                    Toast.makeText(context, "Selesai dengan kendala: $error", Toast.LENGTH_LONG).show()
                                } else if (uri != null) {
                                    Toast.makeText(context, "Sukses! Kolase disimpan di Galeri Foto Anda.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_collage_export_button")
                ) {
                    Text("Proses Render & Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Edit Title Dialog
    if (showEditTitleDialog) {
        AlertDialog(
            onDismissRequest = { showEditTitleDialog = false },
            title = { Text("Ubah Nama Proyek") },
            text = {
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_title_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempTitle.trim().isNotEmpty()) {
                            viewModel.activeProjectName.value = tempTitle.trim()
                            showEditTitleDialog = false
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTitleDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Standard Export Loading Dialog Overlay
    if (isExporting) {
        AlertDialog(
            onDismissRequest = { /* force modal */ },
            title = { Text("Sedang Memproses Gambar...", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Merender kolase profesional pada resolusi tinggi secara real-time.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tahapan: ${(progress * 100).toInt()}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {}
        )
    }

    // Tiled Ultra-Resolution Export Loading Dialog Overlay (With Live Tile Grid Matrix & Memory Safety Indicator)
    if (isTiledExporting) {
        val tp = tiledProgress
        AlertDialog(
            onDismissRequest = { /* force modal during tiled export */ },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GridOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mesin Tiled 13.500px Aktif", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Linear Progress & Percent
                    LinearProgressIndicator(
                        progress = { tp?.progress ?: 0f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tile ${tp?.currentTile ?: 0} dari ${tp?.totalTiles ?: 1}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${((tp?.progress ?: 0f) * 100).toInt()}% Selesai",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Matrix Tile Grid Visualization
                    if (tp != null && tp.cols > 0 && tp.rows > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                for (r in 0 until tp.rows) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        for (c in 0 until tp.cols) {
                                            val tileIdx = r * tp.cols + c + 1
                                            val isDone = tileIdx < tp.currentTile
                                            val isCurrent = tileIdx == tp.currentTile

                                            val tileColor = when {
                                                isDone -> Color(0xFF34C759) // Green completed
                                                isCurrent -> Color(0xFF007AFF) // Blue active rendering
                                                else -> Color.LightGray.copy(alpha = 0.5f)
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(tileColor)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = tp?.statusMessage ?: "Merender segmen kanvas...",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "Memori terkontrol (~16MB per tile) - Bebas Force Close",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Removed Tiled Inspector Dialog

    // Photoshop-Style Simple Layers Panel Bottom Sheet
    if (showLayerContextMenu) {
        val targetLayer = layers.find { it.id == (contextMenuLayerId ?: selectedId) } ?: layers.find { it.id == selectedId }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showLayerContextMenu = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = Color(0xFF1E2124), // Photoshop dark slate theme container
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                // 1. Photoshop Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF007AFF), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Layers",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF2C3036)
                                ) {
                                    Text(
                                        text = "${layers.size}",
                                        color = Color(0xFF9E9E9E),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Panel Lapisan Gaya Photoshop",
                                color = Color(0xFF8E8E93),
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { showLayerContextMenu = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Photoshop Quick Properties Bar (Opacity & Blend Mode)
                if (targetLayer != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF282C34),
                        border = BorderStroke(1.dp, Color(0xFF383C44)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Blend mode pill
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1E2124),
                                border = BorderStroke(1.dp, Color(0xFF3A3F47))
                            ) {
                                Text(
                                    text = "Normal",
                                    color = Color(0xFFD1D5DB),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Opacity label & slider
                            Text(
                                text = "Opacity:",
                                color = Color(0xFF9CA3AF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = "${(targetLayer.opacity * 100).toInt()}%",
                                color = Color(0xFF60A5FA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(36.dp)
                            )

                            Slider(
                                value = targetLayer.opacity,
                                onValueChange = {
                                    viewModel.updateLayerOpacity(targetLayer.id, it)
                                },
                                valueRange = 0.05f..1.0f,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 3. Photoshop Vertical Stacked Layer List (Top = Highest Z-Index, Bottom = Background)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF23272D),
                    border = BorderStroke(1.dp, Color(0xFF32363E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        val sortedLayers = layers.sortedByDescending { it.zIndex }

                        items(sortedLayers, key = { it.id }) { layerItem ->
                            val isSelected = layerItem.id == (targetLayer?.id ?: selectedId)
                            val layerNumber = layers.indexOfFirst { it.id == layerItem.id } + 1
                            val hasEffects = layerItem.dropShadowEnabled || layerItem.innerShadowEnabled || layerItem.outerGlowEnabled

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) Color(0xFF2B3A4E) else Color.Transparent
                                    )
                                    .clickable {
                                        viewModel.selectedLayerId.value = layerItem.id
                                        contextMenuLayerId = layerItem.id
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Selection Accent Bar
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(36.dp)
                                        .background(
                                            if (isSelected) Color(0xFF007AFF) else Color.Transparent,
                                            RoundedCornerShape(2.dp)
                                        )
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                // Eye Visibility Toggle (Photoshop iconic eye icon)
                                IconButton(
                                    onClick = {
                                        viewModel.toggleLayerVisibility(layerItem.id)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (layerItem.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (layerItem.isVisible) "Sembunyikan" else "Tampilkan",
                                        tint = if (layerItem.isVisible) Color(0xFFD1D5DB) else Color(0xFF555B66),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                // Lock Icon Toggle
                                IconButton(
                                    onClick = {
                                        viewModel.toggleLockLayer(layerItem.id)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (layerItem.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = if (layerItem.isLocked) "Buka Kunci" else "Kunci",
                                        tint = if (layerItem.isLocked) Color(0xFFFF9500) else Color(0xFF555B66),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Photoshop Layer Thumbnail (Checkerboard background with clipped preview)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF14171A))
                                        .border(1.dp, if (isSelected) Color(0xFF007AFF) else Color(0xFF3F444E), RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (layerItem.imageUri != null) {
                                        AsyncImage(
                                            model = layerItem.imageUri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer { alpha = if (layerItem.isVisible) layerItem.opacity else 0.3f }
                                        )
                                    } else {
                                        Icon(
                                            imageVector = when (layerItem.shapeType) {
                                                "oval" -> Icons.Default.Circle
                                                "polaroid" -> Icons.Default.CropPortrait
                                                else -> Icons.Default.Square
                                            },
                                            contentDescription = null,
                                            tint = if (layerItem.isVisible) Color(0xFF9CA3AF) else Color(0xFF4B5563),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Layer Name & Badges
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = layerItem.name.ifEmpty { "Layer $layerNumber" },
                                            color = if (isSelected) Color.White else Color(0xFFD1D5DB),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        if (hasEffects) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "fx",
                                                    color = Color(0xFFFBBF24),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = when (layerItem.shapeType) {
                                                "oval" -> "Bentuk Oval"
                                                "rounded" -> "Sudut Bulat"
                                                "polaroid" -> "Frame Polaroid"
                                                else -> "Frame Kotak"
                                            },
                                            color = Color(0xFF8E8E93),
                                            fontSize = 10.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "• Z: ${layerItem.zIndex}",
                                            color = Color(0xFF6B7280),
                                            fontSize = 9.sp
                                        )
                                    }
                                }

                                // Quick Reorder Buttons (Move Up / Move Down Z-Order)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            viewModel.bringForward(layerItem.id)
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Maju 1",
                                            tint = Color(0xFF9CA3AF),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.sendBackward(layerItem.id)
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Mundur 1",
                                            tint = Color(0xFF9CA3AF),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFF2E323A), thickness = 0.5.dp)
                        }

                        // Background Layer (Kanvas) - Classic Photoshop Bottom Background
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.width(3.dp).height(36.dp))
                                Spacer(modifier = Modifier.width(6.dp))

                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier
                                        .size(28.dp)
                                        .padding(4.dp)
                                )

                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Terkunci",
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier
                                        .size(28.dp)
                                        .padding(5.dp)
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(activeBgColor))
                                        .border(1.dp, Color(0xFF3F444E), RoundedCornerShape(4.dp))
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Background (Kanvas)",
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Dimensi: $activeAspect",
                                        color = Color(0xFF6B7280),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Photoshop Bottom Action Bar (Iconic Footer Toolbar)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF282C34),
                    border = BorderStroke(1.dp, Color(0xFF383C44)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Add New Layer (+)
                        IconButton(
                            onClick = {
                                viewModel.addLayer()
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                Toast.makeText(context, "Lapisan baru ditambahkan!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddBox, contentDescription = "Tambah Layer", tint = Color(0xFF60A5FA), modifier = Modifier.size(18.dp))
                                Text("+ Layer", fontSize = 8.sp, color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold)
                            }
                        }

                        // 2. FX Layer Styles
                        IconButton(
                            onClick = {
                                showLayerContextMenu = false
                                activeTool = "layer_styles"
                            },
                            enabled = targetLayer != null,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Layer Styles", tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                                Text("FX Style", fontSize = 8.sp, color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold)
                            }
                        }

                        // 3. Duplicate Layer (⧉)
                        IconButton(
                            onClick = {
                                if (targetLayer != null) {
                                    viewModel.duplicateLayer(targetLayer)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    Toast.makeText(context, "Lapisan diduplikasi!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = targetLayer != null,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplikat", tint = Color(0xFFD1D5DB), modifier = Modifier.size(18.dp))
                                Text("Duplikat", fontSize = 8.sp, color = Color(0xFFD1D5DB))
                            }
                        }

                        // 4. Center on Canvas (⌖)
                        IconButton(
                            onClick = {
                                if (targetLayer != null) {
                                    viewModel.centerLayerOnCanvas(targetLayer.id)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    Toast.makeText(context, "Dipusatkan!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = targetLayer != null,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.FilterCenterFocus, contentDescription = "Pusatkan", tint = Color(0xFFD1D5DB), modifier = Modifier.size(18.dp))
                                Text("Pusat", fontSize = 8.sp, color = Color(0xFFD1D5DB))
                            }
                        }

                        // 5. Replace Photo (🖼)
                        IconButton(
                            onClick = {
                                showLayerContextMenu = false
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = targetLayer != null,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Ganti Foto", tint = Color(0xFFD1D5DB), modifier = Modifier.size(18.dp))
                                Text("Foto", fontSize = 8.sp, color = Color(0xFFD1D5DB))
                            }
                        }

                        // 6. Delete Layer (🗑)
                        IconButton(
                            onClick = {
                                if (targetLayer != null) {
                                    viewModel.removeLayer(targetLayer.id)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    Toast.makeText(context, "Lapisan dihapus!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = targetLayer != null && layers.size > 1,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                Text("Hapus", fontSize = 8.sp, color = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Returns a custom visual layout shape based on the type
 */
fun getShapeByType(type: String, cornerRadiusDp: Float): Shape {
    return when (type) {
        "oval" -> CircleShape
        "rounded" -> RoundedCornerShape(cornerRadiusDp.dp)
        else -> RoundedCornerShape(0.dp) // rect
    }
}

@Composable
fun BottomActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .width(58.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = color,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
