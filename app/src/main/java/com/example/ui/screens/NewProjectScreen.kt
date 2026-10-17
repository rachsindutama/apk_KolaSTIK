package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProjectEntity
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.utils.TemplatePreset
import com.example.utils.TemplatePresets
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val savedProjects by viewModel.savedProjects.collectAsState()
    var widthInput by remember { mutableStateOf("1080") }
    var heightInput by remember { mutableStateOf("1080") }
    var showManualDialog by remember { mutableStateOf(false) }
    var selectedCanvasBgColor by remember { mutableStateOf(0xFFFFFFFF.toInt()) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    var selectedRatioOption by remember { mutableStateOf("4:5") }
    var isLandscape by remember { mutableStateOf(false) }
    var ratioDropdownExpanded by remember { mutableStateOf(false) }

    val (calcW, calcH, labelText) = remember(selectedRatioOption, isLandscape) {
        when (selectedRatioOption) {
            "1:1" -> Triple(1080, 1080, "1:1 (1080x1080)")
            "3:4" -> if (isLandscape) Triple(1440, 1080, "4:3 (1440x1080)") else Triple(1080, 1440, "3:4 (1080x1440)")
            "4:5" -> if (isLandscape) Triple(1350, 1080, "5:4 (1350x1080)") else Triple(1080, 1350, "4:5 (1080x1350)")
            "2:3" -> if (isLandscape) Triple(1620, 1080, "3:2 (1620x1080)") else Triple(1080, 1620, "2:3 (1080x1620)")
            "9:16" -> if (isLandscape) Triple(1920, 1080, "16:9 (1920x1080)") else Triple(1080, 1920, "9:16 (1080x1920)")
            else -> Triple(1080, 1350, "4:5 (1080x1350)")
        }
    }
    val ratioDisplayOnCard = remember(selectedRatioOption, isLandscape) {
        when (selectedRatioOption) {
            "1:1" -> "1:1"
            "3:4" -> if (isLandscape) "4:3" else "3:4"
            "4:5" -> if (isLandscape) "5:4" else "4:5"
            "2:3" -> if (isLandscape) "3:2" else "2:3"
            "9:16" -> if (isLandscape) "16:9" else "9:16"
            else -> "4:5"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "KolaSTIK",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.testTag("import_layout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Import Cloud Layout",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Hero Welcome Banner matching flow requirement
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Buat Proyek Baru",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Pilih Template atau atur Kanvas secara Manual, lalu mulai Edit.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Quick Actions: Manual Canvas & Split Gambar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Set Canvas Dimension Manual Card
                    Card(
                        onClick = { showManualDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .testTag("manual_canvas_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Set Kanvas",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Atur ukuran piksel & aspek",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Split Gambar Card
                    Card(
                        onClick = { viewModel.navigateTo(Screen.Splitter) },
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .testTag("splitter_card_button"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Split Gambar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Potong grid presisi",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Section: Template Sosmed (Social Media Templates)
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Pilih Template",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Orientation Toggle Button (Vertikal / Horizontal)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isLandscape) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isLandscape = !isLandscape }
                                .testTag("toggle_orientation_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLandscape) Icons.Default.StayCurrentLandscape else Icons.Default.StayCurrentPortrait,
                                    contentDescription = if (isLandscape) "Horizontal" else "Vertikal",
                                    tint = if (isLandscape) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isLandscape) "Horiz" else "Vert",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLandscape) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Ratio Dropdown Menu Button
                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { ratioDropdownExpanded = true }
                                    .testTag("ratio_dropdown_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        labelText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Pilih Rasio",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = ratioDropdownExpanded,
                                onDismissRequest = { ratioDropdownExpanded = false }
                            ) {
                                val ratioOptions = listOf("1:1", "3:4", "4:5", "2:3", "9:16")
                                ratioOptions.forEach { ratio ->
                                    val isSelected = selectedRatioOption == ratio
                                    val itemLabel = when (ratio) {
                                        "1:1" -> "1:1 (Persegi 1080x1080)"
                                        "3:4" -> if (isLandscape) "4:3 (Landscape 1440x1080)" else "3:4 (Portrait 1080x1440)"
                                        "4:5" -> if (isLandscape) "5:4 (Landscape 1350x1080)" else "4:5 (Portrait 1080x1350)"
                                        "2:3" -> if (isLandscape) "3:2 (Landscape 1620x1080)" else "2:3 (Portrait 1080x1620)"
                                        "9:16" -> if (isLandscape) "16:9 (Landscape 1920x1080)" else "9:16 (Story 1080x1920)"
                                        else -> ratio
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.size(16.dp))
                                                }
                                                Text(
                                                    itemLabel,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedRatioOption = ratio
                                            ratioDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val templateChunks = TemplatePresets.presets.chunked(2)
            items(templateChunks) { rowPresets ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowPresets.forEach { preset ->
                        Box(modifier = Modifier.weight(1f)) {
                            NewProjTemplatePresetCard(
                                preset = preset,
                                currentRatioLabel = ratioDisplayOnCard,
                                onClick = {
                                    viewModel.startNewProject(
                                        presetId = preset.id,
                                        targetWidth = calcW,
                                        targetHeight = calcH,
                                        isLandscape = isLandscape
                                    )
                                }
                            )
                        }
                    }
                    if (rowPresets.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Section: Saved Projects
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Proyek Kolase Tersimpan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        "${savedProjects.size} Proyek",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (savedProjects.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoAlbum,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.outlineVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Belum ada desain tersimpan",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                "Pilih Template di atas atau buat Kanvas Manual untuk mulai mengedit",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(savedProjects, key = { it.id }) { project ->
                    NewProjProjectListItemCard(
                        project = project,
                        onLoad = { viewModel.loadProject(project.id) },
                        onDelete = { viewModel.deleteProject(project.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "@rachsindutama",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Manual Canvas Dimension Dialog
    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AspectRatio, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                    Text("Set Kanvas", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Masukkan ukuran lebar dan tinggi dalam piksel:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = widthInput,
                        onValueChange = { widthInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Lebar (px)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("manual_width_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Tinggi (px)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("manual_height_input")
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Preset Cepat:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { widthInput = "1080"; heightInput = "1080" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1E1E1E)
                            )
                        ) {
                            Text("1:1", fontSize = 11.sp, color = Color(0xFF1E1E1E), fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { widthInput = "1080"; heightInput = "1350" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1E1E1E)
                            )
                        ) {
                            Text("4:5", fontSize = 11.sp, color = Color(0xFF1E1E1E), fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { widthInput = "1080"; heightInput = "1920" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1E1E1E)
                            )
                        ) {
                            Text("9:16", fontSize = 11.sp, color = Color(0xFF1E1E1E), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Warna Latar Belakang Kanvas:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    val bgColors = listOf(
                        0xFFFFFFFF.toInt() to "Putih",
                        0xFFF5F5F7.toInt() to "Abu Terang",
                        0xFFE5E5EA.toInt() to "Platinum",
                        0xFFF2EFE9.toInt() to "Cream",
                        0xFFE2D6CA.toInt() to "Beige",
                        0xFFD4E2D5.toInt() to "Sage",
                        0xFFE8D3C3.toInt() to "Peach",
                        0xFF1C1C1E.toInt() to "Charcoal",
                        0xFF000000.toInt() to "Hitam"
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        items(bgColors) { (cInt, name) ->
                            val isSelected = selectedCanvasBgColor == cInt
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(cInt))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) {
                                            if (cInt == 0xFFFFFFFF.toInt() || cInt == 0xFFF5F5F7.toInt() || cInt == 0xFFF2EFE9.toInt()) Color(0xFF1E1E1E) else Color.White
                                        } else Color(0xFF555555),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedCanvasBgColor = cInt },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = name,
                                        tint = if (cInt == 0xFFFFFFFF.toInt() || cInt == 0xFFF5F5F7.toInt() || cInt == 0xFFF2EFE9.toInt() || cInt == 0xFFE5E5EA.toInt() || cInt == 0xFFE2D6CA.toInt() || cInt == 0xFFD4E2D5.toInt() || cInt == 0xFFE8D3C3.toInt()) Color(0xFF1E1E1E) else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val w = widthInput.toIntOrNull() ?: 1080
                        val h = heightInput.toIntOrNull() ?: 1080
                        showManualDialog = false
                        viewModel.startCustomProject(w, h, selectedCanvasBgColor)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1E1E1E)
                    ),
                    modifier = Modifier.testTag("confirm_manual_canvas")
                ) {
                    Text("Mulai Edit", color = Color(0xFF1E1E1E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Layout JSON") },
            text = {
                Column {
                    Text("Paste JSON layout kode untuk diimport ke editor:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        placeholder = { Text("{\"layers\": [...]}") }
                    )
                    if (importError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(importError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    try {
                        val success = viewModel.importLayoutConfigJson(importText)
                        if (success) {
                            showImportDialog = false
                            importText = ""
                            importError = null
                            viewModel.navigateTo(Screen.Editor)
                        } else {
                            importError = "Gagal memproses JSON layout"
                        }
                    } catch (e: Exception) {
                        importError = e.localizedMessage ?: "Invalid JSON"
                    }
                }) {
                    Text("Import & Edit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun NewProjTemplatePresetCard(
    preset: TemplatePreset,
    currentRatioLabel: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(138.dp)
            .testTag("template_card_${preset.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (preset.iconName) {
                            "grid_view" -> Icons.Default.GridView
                            "view_quilt" -> Icons.Default.ViewQuilt
                            "style" -> Icons.Default.Style
                            "auto_stories" -> Icons.Default.AutoStories
                            "view_column" -> Icons.Default.ViewColumn
                            "view_agenda" -> Icons.Default.ViewAgenda
                            "view_stream" -> Icons.Default.ViewStream
                            "view_headline" -> Icons.Default.ViewHeadline
                            "dashboard" -> Icons.Default.Dashboard
                            "dashboard_customize" -> Icons.Default.DashboardCustomize
                            else -> Icons.Default.AutoAwesome
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = currentRatioLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Column {
                Text(
                    text = preset.cleanTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = preset.description,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
private fun NewProjProjectListItemCard(
    project: ProjectEntity,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(project.timestamp) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(project.timestamp))
    }

    Card(
        onClick = onLoad,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("project_item_${project.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = project.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Diperbarui: $dateStr",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_project_${project.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Hapus Proyek",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
