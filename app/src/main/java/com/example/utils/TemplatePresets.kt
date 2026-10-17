package com.example.utils

import com.example.data.LayerModel
import java.util.UUID

data class TemplatePreset(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val defaultAspect: String = "1:1",
    val generateLayers: () -> List<LayerModel>
)

object TemplatePresets {
    val presets = listOf(
        TemplatePreset(
            id = "grid_2x2",
            name = "Classic Grid 2x2",
            description = "Empat kotak simetris sempurna",
            iconName = "grid_view",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.02f, 0.02f, 0.46f, 0.46f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.52f, 0.02f, 0.46f, 0.46f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.02f, 0.52f, 0.46f, 0.46f, zIndex = 3, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.52f, 0.52f, 0.46f, 0.46f, zIndex = 4, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        TemplatePreset(
            id = "aesthetic_trio",
            name = "Aesthetic Trio",
            description = "Satu horizontal besar di atas, dua kotak di bawah",
            iconName = "view_quilt",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.02f, 0.02f, 0.96f, 0.46f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.02f, 0.51f, 0.46f, 0.47f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.52f, 0.51f, 0.46f, 0.47f, zIndex = 3, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        TemplatePreset(
            id = "polaroid_duo",
            name = "Polaroid Duo Tilt",
            description = "Dua polaroid estetik melayang bergantian",
            iconName = "style",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.08f, 0.12f, 0.4f, 0.65f, rotation = -5f, zIndex = 1, shapeType = "polaroid", borderThickness = 0f, borderColor = 0xFFFFFFFF.toInt(), cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.52f, 0.22f, 0.4f, 0.65f, rotation = 5f, zIndex = 2, shapeType = "polaroid", borderThickness = 0f, borderColor = 0xFFFFFFFF.toInt(), cornerRadius = 0f)
                )
            }
        ),
        TemplatePreset(
            id = "editorial_cover",
            name = "Editorial Cover",
            description = "Satu background utama dengan frame mengambang",
            iconName = "auto_stories",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0f, 0f, 1f, 1f, zIndex = 0, shapeType = "rect", borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.15f, 0.1f, 0.7f, 0.45f, zIndex = 1, borderThickness = 0f, borderColor = 0xFFFFFFFF.toInt(), cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.1f, 0.62f, 0.38f, 0.3f, zIndex = 2, borderThickness = 0f, borderColor = 0xFFFFFFFF.toInt(), cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.52f, 0.62f, 0.38f, 0.3f, zIndex = 3, borderThickness = 0f, borderColor = 0xFFFFFFFF.toInt(), cornerRadius = 0f)
                )
            }
        ),
        TemplatePreset(
            id = "scrapbook_quad",
            name = "Mixed Scrapbook",
            description = "Multi-bentuk: Oval, Polaroid, Bulat, dan Kotak",
            iconName = "auto_awesome",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.06f, 0.42f, 0.4f, rotation = -8f, zIndex = 1, shapeType = "oval", borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.52f, 0.05f, 0.42f, 0.42f, rotation = 6f, zIndex = 2, shapeType = "rounded", borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.06f, 0.52f, 0.42f, 0.42f, rotation = 4f, zIndex = 3, shapeType = "rect", borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.52f, 0.52f, 0.42f, 0.42f, rotation = -6f, zIndex = 4, shapeType = "polaroid", borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        TemplatePreset(
            id = "triple_columns",
            name = "Minimalist Columns",
            description = "Tiga kolom ramping modern bersisian",
            iconName = "view_column",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.02f, 0.02f, 0.30f, 0.96f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.35f, 0.02f, 0.30f, 0.96f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.68f, 0.02f, 0.30f, 0.96f, zIndex = 3, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        TemplatePreset(
            id = "freestyle",
            name = "Freestyle Canvas",
            description = "Kanvas kosong sepenuhnya untuk kebebasan kreasimu",
            iconName = "dashboard_customize",
            generateLayers = {
                emptyList()
            }
        )
    )
}
