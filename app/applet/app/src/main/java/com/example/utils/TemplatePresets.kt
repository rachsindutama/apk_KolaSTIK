package com.example.utils

import com.example.data.LayerModel
import java.util.UUID

data class TemplatePreset(
    val id: String,
    val name: String,
    val description: String,
    val defaultAspect: String, // "1:1", "9:16", "4:5", "16:9", "3:4", "2:3"
    val iconName: String,
    val generateLayers: () -> List<LayerModel>
)

object TemplatePresets {
    val presets = listOf(
        // 1. Instagram Square 1:1 - Single Post
        TemplatePreset(
            id = "ig_square_single",
            name = "IG Square Single",
            description = "1:1 Classic single photo post for Instagram feed.",
            defaultAspect = "1:1",
            iconName = "grid_view",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.05f, 0.90f, 0.90f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 2. Instagram Square 1:1 - Dual Split Horizontal
        TemplatePreset(
            id = "ig_square_dual_h",
            name = "IG Square Dual Horizontal",
            description = "Two horizontal split photos for 1:1 post.",
            defaultAspect = "1:1",
            iconName = "view_quilt",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.05f, 0.90f, 0.43f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.52f, 0.90f, 0.43f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 3. Instagram Square 1:1 - Dual Split Vertical
        TemplatePreset(
            id = "ig_square_dual_v",
            name = "IG Square Dual Vertical",
            description = "Two vertical split photos side-by-side.",
            defaultAspect = "1:1",
            iconName = "view_column",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.05f, 0.43f, 0.90f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.52f, 0.05f, 0.43f, 0.90f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 4. Instagram Square 1:1 - Grid 4 Photos
        TemplatePreset(
            id = "ig_square_grid_4",
            name = "IG Square Grid 4",
            description = "Four equal quadrant photos for square post.",
            defaultAspect = "1:1",
            iconName = "grid_view",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.04f, 0.04f, 0.44f, 0.44f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.52f, 0.04f, 0.44f, 0.44f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.04f, 0.52f, 0.44f, 0.44f, zIndex = 3, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.52f, 0.52f, 0.44f, 0.44f, zIndex = 4, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 5. Instagram Square 1:1 - Triple Column
        TemplatePreset(
            id = "ig_square_triple_v",
            name = "IG Square Triple Column",
            description = "Three vertical column layout for feed.",
            defaultAspect = "1:1",
            iconName = "view_column",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.03f, 0.05f, 0.30f, 0.90f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.35f, 0.05f, 0.30f, 0.90f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.67f, 0.05f, 0.30f, 0.90f, zIndex = 3, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 6. Instagram Square 1:1 - Hero Top + 2 Bottom
        TemplatePreset(
            id = "ig_square_hero_top",
            name = "IG Square Hero Top",
            description = "Large top showcase with two bottom thumbnails.",
            defaultAspect = "1:1",
            iconName = "view_quilt",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.05f, 0.90f, 0.55f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.63f, 0.43f, 0.32f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.52f, 0.63f, 0.43f, 0.32f, zIndex = 3, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 7. Instagram Portrait 4:5 - Classic Single
        TemplatePreset(
            id = "ig_portrait_single",
            name = "IG Portrait Single",
            description = "4:5 Portrait feed post optimizing screen space.",
            defaultAspect = "4:5",
            iconName = "style",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.05f, 0.90f, 0.90f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 8. Instagram Portrait 4:5 - Dual Stack
        TemplatePreset(
            id = "ig_portrait_dual",
            name = "IG Portrait Dual Stack",
            description = "Two stacked portrait photos.",
            defaultAspect = "4:5",
            iconName = "view_quilt",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.04f, 0.90f, 0.44f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.52f, 0.90f, 0.44f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 9. Instagram Portrait 4:5 - Trio Layout
        TemplatePreset(
            id = "ig_portrait_trio",
            name = "IG Portrait Trio",
            description = "One top featured image and two bottom columns.",
            defaultAspect = "4:5",
            iconName = "grid_view",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.04f, 0.90f, 0.48f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.55f, 0.43f, 0.41f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.52f, 0.55f, 0.43f, 0.41f, zIndex = 3, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 10. Instagram Portrait 4:5 - Grid 4
        TemplatePreset(
            id = "ig_portrait_grid4",
            name = "IG Portrait Grid 4",
            description = "Balanced 2x2 grid for portrait ratio.",
            defaultAspect = "4:5",
            iconName = "grid_view",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.05f, 0.43f, 0.43f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.52f, 0.05f, 0.43f, 0.43f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.52f, 0.43f, 0.43f, zIndex = 3, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.52f, 0.52f, 0.43f, 0.43f, zIndex = 4, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 11. Instagram Story 9:16 - Single Full
        TemplatePreset(
            id = "ig_story_single",
            name = "IG Story Full",
            description = "9:16 Vertical full screen story layout.",
            defaultAspect = "9:16",
            iconName = "auto_stories",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.0f, 0.0f, 1.0f, 1.0f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 12. Instagram Story 9:16 - Dual Vertical Split
        TemplatePreset(
            id = "ig_story_dual",
            name = "IG Story Dual Split",
            description = "Top and bottom split for stories.",
            defaultAspect = "9:16",
            iconName = "view_quilt",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.05f, 0.90f, 0.43f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.52f, 0.90f, 0.43f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 13. Instagram Story 9:16 - Triple Stack
        TemplatePreset(
            id = "ig_story_triple",
            name = "IG Story Triple Stack",
            description = "Three stacked photos for story highlights.",
            defaultAspect = "9:16",
            iconName = "view_column",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.03f, 0.90f, 0.29f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.35f, 0.90f, 0.29f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.67f, 0.90f, 0.29f, zIndex = 3, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 14. Instagram Story 9:16 - Polaroid Collage
        TemplatePreset(
            id = "ig_story_polaroid",
            name = "IG Story Polaroid Vibe",
            description = "Aesthetic polaroid frame style on story.",
            defaultAspect = "9:16",
            iconName = "style",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.08f, 0.08f, 0.84f, 0.52f, rotation = -3f, zIndex = 1, shapeType = "polaroid", borderThickness = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.12f, 0.60f, 0.76f, 0.35f, rotation = 4f, zIndex = 2, shapeType = "polaroid", borderThickness = 0f)
                )
            }
        ),
        // 15. Landscape 16:9 - Cinematic Single
        TemplatePreset(
            id = "landscape_cinematic",
            name = "Landscape Cinematic",
            description = "16:9 Widescreen layout for YouTube / Facebook / Twitter.",
            defaultAspect = "16:9",
            iconName = "grid_view",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.05f, 0.90f, 0.90f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 16. Landscape 16:9 - Side by Side
        TemplatePreset(
            id = "landscape_dual",
            name = "Landscape Side by Side",
            description = "Two widescreen photos side by side.",
            defaultAspect = "16:9",
            iconName = "view_column",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.04f, 0.05f, 0.44f, 0.90f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.52f, 0.05f, 0.44f, 0.90f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 17. Landscape 16:9 - Triple Wide
        TemplatePreset(
            id = "landscape_triple",
            name = "Landscape Triple Wide",
            description = "Three horizontal columns in widescreen.",
            defaultAspect = "16:9",
            iconName = "view_column",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.03f, 0.05f, 0.30f, 0.90f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.35f, 0.05f, 0.30f, 0.90f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.67f, 0.05f, 0.30f, 0.90f, zIndex = 3, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 18. Pinterest Pin 2:3 - Classic Single
        TemplatePreset(
            id = "pinterest_classic",
            name = "Pinterest Pin Classic",
            description = "2:3 Tall pin layout optimized for Pinterest.",
            defaultAspect = "2:3",
            iconName = "style",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.04f, 0.90f, 0.92f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 19. Pinterest Pin 2:3 - Dual Stack
        TemplatePreset(
            id = "pinterest_dual",
            name = "Pinterest Pin Dual",
            description = "Two tall photos stacked for step-by-step pins.",
            defaultAspect = "2:3",
            iconName = "view_quilt",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.04f, 0.90f, 0.44f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.52f, 0.90f, 0.44f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 20. TikTok / Reels Cover 9:16 - Title Card + Video Frame
        TemplatePreset(
            id = "tiktok_cover",
            name = "TikTok / Reels Cover",
            description = "Optimized vertical layout for video thumbnail covers.",
            defaultAspect = "9:16",
            iconName = "auto_stories",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.10f, 0.10f, 0.80f, 0.60f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.10f, 0.74f, 0.80f, 0.16f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 21. Polaroid Retro 4-Pack (1:1)
        TemplatePreset(
            id = "polaroid_quad_1x1",
            name = "Retro Polaroid 4-Pack",
            description = "Four tilted polaroid cards in square layout.",
            defaultAspect = "1:1",
            iconName = "style",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.05f, 0.43f, 0.43f, rotation = -5f, zIndex = 1, shapeType = "polaroid", borderThickness = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.52f, 0.05f, 0.43f, 0.43f, rotation = 4f, zIndex = 2, shapeType = "polaroid", borderThickness = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.52f, 0.43f, 0.43f, rotation = 3f, zIndex = 3, shapeType = "polaroid", borderThickness = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.52f, 0.52f, 0.43f, 0.43f, rotation = -4f, zIndex = 4, shapeType = "polaroid", borderThickness = 0f)
                )
            }
        ),
        // 22. Aesthetic Grid 3x3 (Square 1:1)
        TemplatePreset(
            id = "ig_square_grid_9",
            name = "IG Square Grid 9",
            description = "Nine mini photo grid collage.",
            defaultAspect = "1:1",
            iconName = "grid_view",
            generateLayers = {
                val list = mutableListOf<LayerModel>()
                val size = 0.30f
                val gap = 0.03f
                var z = 1
                for (r in 0 until 3) {
                    for (c in 0 until 3) {
                        val x = 0.04f + c * (size + gap)
                        val y = 0.04f + r * (size + gap)
                        list.add(LayerModel(UUID.randomUUID().toString(), x, y, size, size, zIndex = z++, borderThickness = 0f, cornerRadius = 0f))
                    }
                }
                list
            }
        ),
        // 23. Asymmetric Artistic Layout (1:1)
        TemplatePreset(
            id = "asymmetric_art",
            name = "Asymmetric Artistic",
            description = "Modern asymmetric layout for creative portfolios.",
            defaultAspect = "1:1",
            iconName = "grid_view",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.05f, 0.60f, 0.90f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.68f, 0.05f, 0.27f, 0.43f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.68f, 0.52f, 0.27f, 0.43f, zIndex = 3, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 24. Magazine Cover Layout (Portrait 4:5)
        TemplatePreset(
            id = "magazine_cover",
            name = "Magazine Cover",
            description = "Large feature image with subtitle banner frame.",
            defaultAspect = "4:5",
            iconName = "style",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.04f, 0.04f, 0.92f, 0.75f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.04f, 0.81f, 0.92f, 0.15f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 25. Dual Diagonal Vibe (Square 1:1)
        TemplatePreset(
            id = "dual_diagonal",
            name = "Dual Diagonal Vibe",
            description = "Two diagonal offset cards for dynamic look.",
            defaultAspect = "1:1",
            iconName = "style",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.10f, 0.55f, 0.55f, rotation = -6f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.40f, 0.35f, 0.55f, 0.55f, rotation = 6f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 26. Facebook Post (Landscape 16:9) - Triple Split
        TemplatePreset(
            id = "fb_post_triple",
            name = "FB Post Triple Horizon",
            description = "Three horizontal panels for Facebook updates.",
            defaultAspect = "16:9",
            iconName = "view_quilt",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.04f, 0.05f, 0.92f, 0.28f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.04f, 0.36f, 0.92f, 0.28f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.04f, 0.67f, 0.92f, 0.28f, zIndex = 3, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 27. Twitter / X Header Banner (3:1)
        TemplatePreset(
            id = "twitter_banner",
            name = "X / Twitter Banner Split",
            description = "Widescreen profile header collage layout.",
            defaultAspect = "16:9",
            iconName = "view_column",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.10f, 0.43f, 0.80f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.52f, 0.10f, 0.43f, 0.80f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 28. Minimalist Duo (Portrait 3:4)
        TemplatePreset(
            id = "minimalist_duo",
            name = "Minimalist Duo 3:4",
            description = "Two clean stacked photos in 3:4 ratio.",
            defaultAspect = "3:4",
            iconName = "view_quilt",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.08f, 0.06f, 0.84f, 0.42f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.08f, 0.52f, 0.84f, 0.42f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 29. Modern Quad Showcase (Portrait 4:5)
        TemplatePreset(
            id = "modern_quad_45",
            name = "Modern Quad Showcase",
            description = "Four clean frames for portrait feed post.",
            defaultAspect = "4:5",
            iconName = "grid_view",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.04f, 0.43f, 0.44f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.52f, 0.04f, 0.43f, 0.44f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.05f, 0.52f, 0.43f, 0.44f, zIndex = 3, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.52f, 0.52f, 0.43f, 0.44f, zIndex = 4, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        ),
        // 30. Ultimate Social Story Collage (9:16)
        TemplatePreset(
            id = "ultimate_story_collage",
            name = "Ultimate Story Collage",
            description = "Multi-photo artistic collage for daily stories.",
            defaultAspect = "9:16",
            iconName = "auto_stories",
            generateLayers = {
                listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.06f, 0.05f, 0.88f, 0.42f, zIndex = 1, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.06f, 0.50f, 0.42f, 0.28f, zIndex = 2, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.52f, 0.50f, 0.42f, 0.28f, zIndex = 3, borderThickness = 0f, cornerRadius = 0f),
                    LayerModel(UUID.randomUUID().toString(), 0.06f, 0.80f, 0.88f, 0.16f, zIndex = 4, borderThickness = 0f, cornerRadius = 0f)
                )
            }
        )
    )
}
