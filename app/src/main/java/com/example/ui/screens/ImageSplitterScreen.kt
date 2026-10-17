package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.utils.FileUtils
import com.example.utils.ImagePlaceholderUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSplitterScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val sourceUri by viewModel.splitterSourceUri.collectAsState()
    val cols by viewModel.splitterCols.collectAsState()
    val rows by viewModel.splitterRows.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val progress by viewModel.exportProgress.collectAsState()

    // Helper counts
    val totalSlices_count = cols * rows
    val totalSlices_text = "$totalSlices_count"

    var outputFormat by remember { mutableStateOf(Bitmap.CompressFormat.JPEG) }
    var outputQuality by remember { mutableStateOf(100f) } // slider
    var showExportModal by remember { mutableStateOf(false) }
    var previewSplitImages by remember { mutableStateOf<List<Bitmap>?>(null) }
    var showPreviewGridDialog by remember { mutableStateOf(false) }
    var splitScale by remember { mutableStateOf(1f) }
    var splitOffset by remember { mutableStateOf(Offset.Zero) }

    // Navigation back handler
    BackHandler {
        viewModel.navigateTo(Screen.NewProject)
    }

    // Photo Picker Launcher
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.splitterSourceUri.value = uri
                previewSplitImages = null // Reset preview
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aesthetic Image Splitter", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.NewProject) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Beranda"
                        )
                    }
                },
                actions = {
                    if (sourceUri != null) {
                        TextButton(
                            onClick = {
                                // Generate quick splits for preview dialog
                                val b = FileUtils.decodeBitmapFromUri(context, sourceUri.toString(), 1500)
                                if (b != null) {
                                    previewSplitImages = FileUtils.splitBitmap(b, cols, rows)
                                    showPreviewGridDialog = true
                                } else {
                                    Toast.makeText(context, "Gagal memproses pratinjau gambar", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("preview_grid_slices_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pratinjau")
                            }
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (sourceUri == null) {
                // Empty state: Select image to split
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                            .clickable {
                                pickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Pilih Gambar untuk Memulai",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Ketuk untuk membuka galeri foto Anda dan memotong satu gambar besar menjadi potongan grid presisi",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                pickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("select_split_photo_button")
                        ) {
                            Text("Buka Galeri")
                        }
                    }
                }
            } else {
                // Main editor workspace
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Image container with cutlines
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.05f))
                            // Mouse scroll wheel zooms preview
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.type == PointerEventType.Scroll) {
                                            val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                            if (delta != 0f) {
                                                val factor = if (delta < 0f) 1.15f else 0.85f
                                                splitScale = (splitScale * factor).coerceIn(0.5f, 4.0f)
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    }
                                }
                            }
                            // Multi-touch pinch to zoom and pan preview
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    splitScale = (splitScale * zoom).coerceIn(0.5f, 4.0f)
                                    splitOffset = Offset(
                                        x = (splitOffset.x + pan.x).coerceIn(-1500f, 1500f),
                                        y = (splitOffset.y + pan.y).coerceIn(-1500f, 1500f)
                                    )
                                }
                            }
                            // Double tap to reset
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        splitScale = 1f
                                        splitOffset = Offset.Zero
                                    }
                                )
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                                .graphicsLayer {
                                    scaleX = splitScale
                                    scaleY = splitScale
                                    translationX = splitOffset.x
                                    translationY = splitOffset.y
                                }
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            AsyncImage(
                                model = sourceUri,
                                contentDescription = "Source to split",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f) // force clean square box for visual grids
                            )

                            // Grid Overlay lines
                            Canvas(
                                modifier = Modifier
                                    .matchParentSize()
                            ) {
                                val w = size.width
                                val h = size.height

                                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                                // Draw vertical cutlines
                                for (i in 1 until cols) {
                                    val x = (i.toFloat() / cols) * w
                                    drawLine(
                                        color = Color.White,
                                        start = Offset(x, 0f),
                                        end = Offset(x, h),
                                        strokeWidth = 3f,
                                        pathEffect = dashEffect
                                    )
                                    // Drop shadow line for visibility
                                    drawLine(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        start = Offset(x + 1.5f, 0f),
                                        end = Offset(x + 1.5f, h),
                                        strokeWidth = 1.5f,
                                        pathEffect = dashEffect
                                    )
                                }

                                // Draw horizontal cutlines
                                for (i in 1 until rows) {
                                    val y = (i.toFloat() / rows) * h
                                    drawLine(
                                        color = Color.White,
                                        start = Offset(0f, y),
                                        end = Offset(w, y),
                                        strokeWidth = 3f,
                                        pathEffect = dashEffect
                                    )
                                    // Drop shadow line
                                    drawLine(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        start = Offset(0f, y + 1.5f),
                                        end = Offset(w, y + 1.5f),
                                        strokeWidth = 1.5f,
                                        pathEffect = dashEffect
                                    )
                                }
                            }
                        }
                    }

                    // Floating Zoom Controls Overlay
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
                            IconButton(
                                onClick = { splitScale = (splitScale * 0.85f).coerceIn(0.5f, 4.0f) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(16.dp))
                            }

                            Text(
                                text = "${(splitScale * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .clickable {
                                        splitScale = 1f
                                        splitOffset = Offset.Zero
                                    }
                                    .padding(horizontal = 6.dp)
                            )

                            IconButton(
                                onClick = { splitScale = (splitScale * 1.15f).coerceIn(0.5f, 4.0f) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
                            }

                            if (splitScale != 1f || splitOffset != Offset.Zero) {
                                IconButton(
                                    onClick = {
                                        splitScale = 1f
                                        splitOffset = Offset.Zero
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.FitScreen, contentDescription = "Reset Zoom", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Split controller bottom panel
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Presets of grids
                        Text(
                            "Preset Slices Grid",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            val presetsList = listOf(
                                Triple(1, 3, "Column 1x3"),
                                Triple(3, 1, "Row 3x1"),
                                Triple(2, 2, "Square 2x2"),
                                Triple(3, 3, "Instagram 3x3"),
                                Triple(3, 4, "Portrait 3x4")
                            )
                            items(presetsList) { preset ->
                                val isSelected = cols == preset.first && rows == preset.second
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.splitterCols.value = preset.first
                                        viewModel.splitterRows.value = preset.second
                                        previewSplitImages = null
                                    },
                                    label = { Text(preset.third, fontSize = 12.sp) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }

                        // Custom Grid Sliders
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Kolom (Vertikal)", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    Text("$cols", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Slider(
                                    value = cols.toFloat(),
                                    onValueChange = { 
                                        viewModel.splitterCols.value = it.toInt()
                                        previewSplitImages = null
                                    },
                                    valueRange = 1f..6f,
                                    steps = 4,
                                    modifier = Modifier.testTag("columns_slider")
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Baris (Horizontal)", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    Text("$rows", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Slider(
                                    value = rows.toFloat(),
                                    onValueChange = { 
                                        viewModel.splitterRows.value = it.toInt()
                                        previewSplitImages = null
                                    },
                                    valueRange = 1f..6f,
                                    steps = 4,
                                    modifier = Modifier.testTag("rows_slider")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Trigger splits
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { 
                                    viewModel.splitterSourceUri.value = null 
                                    previewSplitImages = null
                                },
                                modifier = Modifier
                                    .weight(0.4f)
                                    .height(52.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ganti")
                            }

                            Button(
                                onClick = { showExportModal = true },
                                modifier = Modifier
                                    .weight(0.6f)
                                    .height(52.dp)
                                    .testTag("process_split_button")
                            ) {
                                Icon(Icons.Default.ContentCut, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Proses Potongan")
                            }
                        }
                    }
                }
            }
        }
    }

    // High-Res Export Settings Dialog
    if (showExportModal) {
        AlertDialog(
            onDismissRequest = { showExportModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Pengaturan Ekspor Grid")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Setiap irisan grid akan disimpan secara terpisah ke Galeri Anda dengan resolusi asli gambar.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Format
                    Text("Format File Ekspor", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedCard(
                            onClick = { outputFormat = Bitmap.CompressFormat.JPEG },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (outputFormat == Bitmap.CompressFormat.JPEG) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Box(modifier = Modifier.padding(12.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("JPG (Ringan)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        OutlinedCard(
                            onClick = { outputFormat = Bitmap.CompressFormat.PNG },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (outputFormat == Bitmap.CompressFormat.PNG) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Box(modifier = Modifier.padding(12.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("PNG (Tanpa Kompresi)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quality (Only for JPG)
                    if (outputFormat == Bitmap.CompressFormat.JPEG) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Kualitas JPG", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("${outputQuality.toInt()}%", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = outputQuality,
                            onValueChange = { outputQuality = it },
                            valueRange = 50f..100f,
                            modifier = Modifier.testTag("quality_slider")
                        )
                        Text(
                            "Kualitas 100% menghasilkan foto tajam tanpa noise kompresi.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportModal = false
                        viewModel.splitSelectedImage(
                            context = context,
                            cols = cols,
                            rows = rows,
                            format = outputFormat,
                            quality = outputQuality.toInt()
                        ) { uris, error ->
                            if (error != null) {
                                Toast.makeText(context, "Selesai dengan kendala: $error", Toast.LENGTH_LONG).show()
                            } else if (uris != null) {
                                Toast.makeText(context, "Sukses! $totalSlices_text bagian berhasil disimpan ke Galeri.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_split_export_button")
                ) {
                    Text("Ekspor $totalSlices_count Potongan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportModal = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Real-time Grid Preview Dialog
    if (showPreviewGridDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPreviewGridDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Pratinjau Hasil Potongan")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Berikut adalah simulasi visual pembagian gambar Anda ke dalam $cols x $rows grid:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.03f))
                            .padding(8.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(cols),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            previewSplitImages?.let { list ->
                                itemsIndexed(list) { index, bmp ->
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White)
                                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = bmp,
                                            contentDescription = "slice preview $index",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .align(Alignment.TopStart)
                                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(bottomEnd = 4.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "${index + 1}",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPreviewGridDialog = false }
                ) {
                    Text("Tutup Pratinjau")
                }
            }
        )
    }

    // Export Processing Loading Overlay
    if (isExporting) {
        AlertDialog(
            onDismissRequest = { /* Force non-cancelable */ },
            title = { Text("Memproses Potongan...", fontWeight = FontWeight.Bold) },
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
                        "Sedang memotong dan mengompresi gambar resolusi tinggi tanpa mengurangi detail asli file.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Progres: ${(progress * 100).toInt()}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {}
        )
    }
}
