package com.example.utils

import com.example.data.LayerModel
import org.json.JSONArray
import java.util.UUID

data class TemplatePreset(
    val id: String,
    val name: String,
    val cleanTitle: String = name.replace(Regex("""^\d+\.\s*"""), "").trim(),
    val description: String,
    val iconName: String,
    val defaultAspect: String = "1080:1350",
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val generateLayers: () -> List<LayerModel>,
    val generateAdaptedLayers: (targetW: Int, targetH: Int, isLandscape: Boolean) -> List<LayerModel> = { _, _, _ -> generateLayers() }
)

object TemplatePresets {

    private val descriptions = mapOf(
        "preset_01" to ("2 frame vertikal minimalis terpisah" to "view_quilt"),
        "preset_02" to ("2 kolom frame simetris bersih" to "view_quilt"),
        "preset_03" to ("1 portrait besar kiri + 2 kanan" to "view_quilt"),
        "preset_04" to ("1 frame hero atas + 2 kotak bawah" to "view_quilt"),
        "preset_05" to ("4 frame simetris tanpa sela" to "view_quilt"),
        "preset_06" to ("3 baris horizontal strip film" to "view_quilt"),
        "preset_07" to ("3 pilar vertikal ramping bersisian" to "view_quilt"),
        "preset_08" to ("5 frame editorial dinamis terstruktur" to "view_quilt"),
        "preset_09" to ("6 kotak galeri rapi 2x3" to "view_quilt"),
        "preset_10" to ("1 frame fokus mengambang di tengah" to "view_quilt"),
        "preset_11" to ("2 frame bertumpuk dengan drop shadow" to "view_quilt"),
        "preset_12" to ("3 frame asimetris modern" to "view_quilt"),
        "preset_13" to ("4 frame moodboard lookbook estetik" to "view_quilt"),
        "preset_14" to ("2 frame atas + 1 hero penuh bawah" to "view_quilt"),
        "preset_15" to ("4 kolom vertikal seamless rapat" to "view_quilt"),
        "preset_16" to ("2 frame sudut membulat lembut r20" to "view_quilt"),
        "preset_17" to ("4 frame sudut membulat estetik r16" to "view_quilt"),
        "preset_18" to ("1 frame tengah fokus + 2 strip batas" to "view_quilt"),
        "preset_19" to ("5 frame cross grid minimalis" to "view_quilt"),
        "preset_20" to ("3 strip horizontal full-bleed" to "view_quilt")
    )

    private const val TEMPLATES_JSON = """[
  {
    "name": "Dual Minimalist Vertical Split",
    "aspectRatio": "1080:1350",
    "backgroundColor": -1,
    "layers": [
      {"id": "a01-1", "x": 0.04, "y": 0.04, "width": 0.92, "height": 0.45, "rotation": 0, "imageUri": "", "zIndex": 1, "borderThickness": 0, "borderColor": 0, "cornerRadius": 4, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a01-2", "x": 0.04, "y": 0.51, "width": 0.92, "height": 0.45, "rotation": 0, "imageUri": "", "zIndex": 2, "borderThickness": 0, "borderColor": 0, "cornerRadius": 4, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false}
    ]
  },
  {
    "name": "Dual Side by Side Clean",
    "aspectRatio": "1080:1350",
    "backgroundColor": -1,
    "layers": [
      {"id": "a02-1", "x": 0.04, "y": 0.04, "width": 0.45, "height": 0.92, "rotation": 0, "imageUri": "", "zIndex": 1, "borderThickness": 0, "borderColor": 0, "cornerRadius": 4, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a02-2", "x": 0.51, "y": 0.04, "width": 0.45, "height": 0.92, "rotation": 0, "imageUri": "", "zIndex": 2, "borderThickness": 0, "borderColor": 0, "cornerRadius": 4, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false}
    ]
  },
  {
    "name": "Editorial Portrait Feature Left",
    "aspectRatio": "1080:1350",
    "backgroundColor": -1,
    "layers": [
      {"id": "a03-1", "x": 0.03, "y": 0.03, "width": 0.61, "height": 0.94, "rotation": 0, "imageUri": "", "zIndex": 1, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a03-2", "x": 0.66, "y": 0.03, "width": 0.31, "height": 0.46, "rotation": 0, "imageUri": "", "zIndex": 2, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a03-3", "x": 0.66, "y": 0.51, "width": 0.31, "height": 0.46, "rotation": 0, "imageUri": "", "zIndex": 3, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false}
    ]
  },
  {
    "name": "Editorial Hero Top",
    "aspectRatio": "1080:1350",
    "backgroundColor": -1,
    "layers": [
      {"id": "a04-1", "x": 0.03, "y": 0.03, "width": 0.94, "height": 0.60, "rotation": 0, "imageUri": "", "zIndex": 1, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a04-2", "x": 0.03, "y": 0.65, "width": 0.46, "height": 0.32, "rotation": 0, "imageUri": "", "zIndex": 2, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a04-3", "x": 0.51, "y": 0.65, "width": 0.46, "height": 0.32, "rotation": 0, "imageUri": "", "zIndex": 3, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false}
    ]
  },
  {
    "name": "Clean Grid 2x2 Seamless",
    "aspectRatio": "1080:1350",
    "backgroundColor": -1,
    "layers": [
      {"id": "a05-1", "x": 0.02, "y": 0.02, "width": 0.47, "height": 0.47, "rotation": 0, "imageUri": "", "zIndex": 1, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a05-2", "x": 0.51, "y": 0.02, "width": 0.47, "height": 0.47, "rotation": 0, "imageUri": "", "zIndex": 2, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a05-3", "x": 0.02, "y": 0.51, "width": 0.47, "height": 0.47, "rotation": 0, "imageUri": "", "zIndex": 3, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a05-4", "x": 0.51, "y": 0.51, "width": 0.47, "height": 0.47, "rotation": 0, "imageUri": "", "zIndex": 4, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false}
    ]
  },
  {
    "name": "Triptych Vertical Film Strip",
    "aspectRatio": "1080:1350",
    "backgroundColor": -1,
    "layers": [
      {"id": "a06-1", "x": 0.05, "y": 0.04, "width": 0.90, "height": 0.28, "rotation": 0, "imageUri": "", "zIndex": 1, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a06-2", "x": 0.05, "y": 0.36, "width": 0.90, "height": 0.28, "rotation": 0, "imageUri": "", "zIndex": 2, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a06-3", "x": 0.05, "y": 0.68, "width": 0.90, "height": 0.28, "rotation": 0, "imageUri": "", "zIndex": 3, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false}
    ]
  },
  {
    "name": "Triptych Horizontal Pillars",
    "aspectRatio": "1080:1350",
    "backgroundColor": -1,
    "layers": [
      {"id": "a07-1", "x": 0.04, "y": 0.05, "width": 0.28, "height": 0.90, "rotation": 0, "imageUri": "", "zIndex": 1, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a07-2", "x": 0.36, "y": 0.05, "width": 0.28, "height": 0.90, "rotation": 0, "imageUri": "", "zIndex": 2, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a07-3", "x": 0.68, "y": 0.05, "width": 0.28, "height": 0.90, "rotation": 0, "imageUri": "", "zIndex": 3, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false}
    ]
  },
  {
    "name": "Modern 5 Grid Editorial",
    "aspectRatio": "1080:1350",
    "backgroundColor": -1,
    "layers": [
      {"id": "a08-1", "x": 0.03, "y": 0.03, "width": 0.46, "height": 0.60, "rotation": 0, "imageUri": "", "zIndex": 1, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a08-2", "x": 0.51, "y": 0.03, "width": 0.46, "height": 0.29, "rotation": 0, "imageUri": "", "zIndex": 2, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a08-3", "x": 0.51, "y": 0.34, "width": 0.46, "height": 0.29, "rotation": 0, "imageUri": "", "zIndex": 3, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a08-4", "x": 0.03, "y": 0.65, "width": 0.46, "height": 0.32, "rotation": 0, "imageUri": "", "zIndex": 4, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a08-5", "x": 0.51, "y": 0.65, "width": 0.46, "height": 0.32, "rotation": 0, "imageUri": "", "zIndex": 5, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false}
    ]
  },
  {
    "name": "6 Tile Neat Gallery Grid",
    "aspectRatio": "1080:1350",
    "backgroundColor": -1,
    "layers": [
      {"id": "a09-1", "x": 0.03, "y": 0.03, "width": 0.46, "height": 0.30, "rotation": 0, "imageUri": "", "zIndex": 1, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a09-2", "x": 0.51, "y": 0.03, "width": 0.46, "height": 0.30, "rotation": 0, "imageUri": "", "zIndex": 2, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a09-3", "x": 0.03, "y": 0.35, "width": 0.46, "height": 0.30, "rotation": 0, "imageUri": "", "zIndex": 3, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a09-4", "x": 0.51, "y": 0.35, "width": 0.46, "height": 0.30, "rotation": 0, "imageUri": "", "zIndex": 4, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a09-5", "x": 0.03, "y": 0.67, "width": 0.46, "height": 0.30, "rotation": 0, "imageUri": "", "zIndex": 5, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a09-6", "x": 0.51, "y": 0.67, "width": 0.46, "height": 0.30, "rotation": 0, "imageUri": "", "zIndex": 6, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false}
    ]
  },
  {
    "name": "Single Floating Canvas Center",
    "aspectRatio": "1080:1350",
    "backgroundColor": -1,
    "layers": [
      {"id": "a10-1", "x": 0.08, "y": 0.08, "width": 0.84, "height": 0.84, "rotation": 0, "imageUri": "", "zIndex": 1, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false}
    ]
  },
  {
    "name": "Offset Floating Pair",
    "aspectRatio": "1080:1350",
    "backgroundColor": -1,
    "layers": [
      {"id": "a11-1", "x": 0.06, "y": 0.06, "width": 0.58, "height": 0.58, "rotation": 0, "imageUri": "", "zIndex": 1, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a11-2", "x": 0.36, "y": 0.36, "width": 0.58, "height": 0.58, "rotation": 0, "imageUri": "", "zIndex": 2, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": true, "dropShadowBlur": 24, "dropShadowOffsetX": 0, "dropShadowOffsetY": 12, "dropShadowColor": 402653184, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false}
    ]
  },
  {
    "name": "Asymmetric 3 Tile Staggered",
    "aspectRatio": "1080:1350",
    "backgroundColor": -1,
    "layers": [
      {"id": "a12-1", "x": 0.04, "y": 0.04, "width": 0.52, "height": 0.52, "rotation": 0, "imageUri": "", "zIndex": 1, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a12-2", "x": 0.58, "y": 0.04, "width": 0.38, "height": 0.40, "rotation": 0, "imageUri": "", "zIndex": 2, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a12-3", "x": 0.04, "y": 0.58, "width": 0.92, "height": 0.38, "rotation": 0, "imageUri": "", "zIndex": 3, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false}
    ]
  },
  {
    "name": "Moodboard 4 Tile Lookbook",
    "aspectRatio": "1080:1350",
    "backgroundColor": -1,
    "layers": [
      {"id": "a13-1", "x": 0.04, "y": 0.04, "width": 0.44, "height": 0.62, "rotation": 0, "imageUri": "", "zIndex": 1, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a13-2", "x": 0.52, "y": 0.04, "width": 0.44, "height": 0.34, "rotation": 0, "imageUri": "", "zIndex": 2, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a13-3", "x": 0.52, "y": 0.41, "width": 0.44, "height": 0.55, "rotation": 0, "imageUri": "", "zIndex": 3, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a13-4", "x": 0.04, "y": 0.69, "width": 0.44, "height": 0.27, "rotation": 0, "imageUri": "", "zIndex": 4, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false}
    ]
  },
  {
    "name": "Full bleed Hero Bottom 3 Tile",
    "aspectRatio": "1080:1350",
    "backgroundColor": -1,
    "layers": [
      {"id": "a14-1", "x": 0.00, "y": 0.00, "width": 0.495, "height": 0.38, "rotation": 0, "imageUri": "", "zIndex": 1, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a14-2", "x": 0.505, "y": 0.00, "width": 0.495, "height": 0.38, "rotation": 0, "imageUri": "", "zIndex": 2, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a14-3", "x": 0.00, "y": 0.39, "width": 1.00, "height": 0.61, "rotation": 0, "imageUri": "", "zIndex": 3, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false}
    ]
  },
  {
    "name": "Quad Vertical Columns Gapless",
    "aspectRatio": "1080:1350",
    "backgroundColor": -1,
    "layers": [
      {"id": "a15-1", "x": 0.00, "y": 0.00, "width": 0.24, "height": 1.00, "rotation": 0, "imageUri": "", "zIndex": 1, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a15-2", "x": 0.253, "y": 0.00, "width": 0.24, "height": 1.00, "rotation": 0, "imageUri": "", "zIndex": 2, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a15-3", "x": 0.506, "y": 0.00, "width": 0.24, "height": 1.00, "rotation": 0, "imageUri": "", "zIndex": 3, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a15-4", "x": 0.760, "y": 0.00, "width": 0.24, "height": 1.00, "rotation": 0, "imageUri": "", "zIndex": 4, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false}
    ]
  },
  {
    "name": "Soft Round Inner Frame Dual",
    "aspectRatio": "1080:1350",
    "backgroundColor": -1,
    "layers": [
      {"id": "a16-1", "x": 0.05, "y": 0.05, "width": 0.90, "height": 0.43, "rotation": 0, "imageUri": "", "zIndex": 1, "borderThickness": 0, "borderColor": 0, "cornerRadius": 20, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a16-2", "x": 0.05, "y": 0.52, "width": 0.90, "height": 0.43, "rotation": 0, "imageUri": "", "zIndex": 2, "borderThickness": 0, "borderColor": 0, "cornerRadius": 20, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false}
    ]
  },
  {
    "name": "Soft Round Inner Frame Quad",
    "aspectRatio": "1080:1350",
    "backgroundColor": -1,
    "layers": [
      {"id": "a17-1", "x": 0.04, "y": 0.04, "width": 0.44, "height": 0.44, "rotation": 0, "imageUri": "", "zIndex": 1, "borderThickness": 0, "borderColor": 0, "cornerRadius": 16, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a17-2", "x": 0.52, "y": 0.04, "width": 0.44, "height": 0.44, "rotation": 0, "imageUri": "", "zIndex": 2, "borderThickness": 0, "borderColor": 0, "cornerRadius": 16, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a17-3", "x": 0.04, "y": 0.52, "width": 0.44, "height": 0.44, "rotation": 0, "imageUri": "", "zIndex": 3, "borderThickness": 0, "borderColor": 0, "cornerRadius": 16, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a17-4", "x": 0.52, "y": 0.52, "width": 0.44, "height": 0.44, "rotation": 0, "imageUri": "", "zIndex": 4, "borderThickness": 0, "borderColor": 0, "cornerRadius": 16, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false}
    ]
  },
  {
    "name": "Minimal Center Focus Strip",
    "aspectRatio": "1080:1350",
    "backgroundColor": -1,
    "layers": [
      {"id": "a18-1", "x": 0.04, "y": 0.04, "width": 0.92, "height": 0.22, "rotation": 0, "imageUri": "", "zIndex": 1, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a18-2", "x": 0.04, "y": 0.28, "width": 0.92, "height": 0.44, "rotation": 0, "imageUri": "", "zIndex": 2, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a18-3", "x": 0.04, "y": 0.74, "width": 0.92, "height": 0.22, "rotation": 0, "imageUri": "", "zIndex": 3, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false}
    ]
  },
  {
    "name": "5 Tile Cross Grid Minimal",
    "aspectRatio": "1080:1350",
    "backgroundColor": -1,
    "layers": [
      {"id": "a19-1", "x": 0.03, "y": 0.03, "width": 0.46, "height": 0.46, "rotation": 0, "imageUri": "", "zIndex": 1, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a19-2", "x": 0.51, "y": 0.03, "width": 0.46, "height": 0.46, "rotation": 0, "imageUri": "", "zIndex": 2, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a19-3", "x": 0.03, "y": 0.51, "width": 0.30, "height": 0.46, "rotation": 0, "imageUri": "", "zIndex": 3, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a19-4", "x": 0.35, "y": 0.51, "width": 0.30, "height": 0.46, "rotation": 0, "imageUri": "", "zIndex": 4, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a19-5", "x": 0.67, "y": 0.51, "width": 0.30, "height": 0.46, "rotation": 0, "imageUri": "", "zIndex": 5, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false}
    ]
  },
  {
    "name": "Full bleed Seamless 3 Split Horizontal",
    "aspectRatio": "1080:1350",
    "backgroundColor": -1,
    "layers": [
      {"id": "a20-1", "x": 0.00, "y": 0.00, "width": 1.00, "height": 0.33, "rotation": 0, "imageUri": "", "zIndex": 1, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a20-2", "x": 0.00, "y": 0.335, "width": 1.00, "height": 0.33, "rotation": 0, "imageUri": "", "zIndex": 2, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false},
      {"id": "a20-3", "x": 0.00, "y": 0.67, "width": 1.00, "height": 0.33, "rotation": 0, "imageUri": "", "zIndex": 3, "borderThickness": 0, "borderColor": 0, "cornerRadius": 0, "filterId": "none", "imageScale": 1, "imageOffsetX": 0, "imageOffsetY": 0, "shapeType": "rect", "isLocked": false, "isVisible": true, "opacity": 1, "name": "", "dropShadowEnabled": false, "dropShadowBlur": 0, "dropShadowOffsetX": 0, "dropShadowOffsetY": 0, "dropShadowColor": 0, "innerShadowEnabled": false, "innerShadowBlur": 0, "innerShadowOffsetX": 0, "innerShadowOffsetY": 0, "innerShadowColor": 0, "outerGlowEnabled": false, "outerGlowRadius": 0, "outerGlowColor": 0, "fitImage": false, "lockAspectRatio": false}
    ]
  }
]"""

    val presets: List<TemplatePreset> by lazy {
        parseTemplates(TEMPLATES_JSON)
    }

    private fun parseTemplates(jsonStr: String): List<TemplatePreset> {
        val list = mutableListOf<TemplatePreset>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val name = obj.getString("name")
                val prefix = name.take(2)
                val id = "preset_$prefix"
                val aspect = obj.optString("aspectRatio", "1080:1350")
                val bg = obj.optInt("backgroundColor", -1)
                val layersArr = obj.getJSONArray("layers")

                val descPair = descriptions[id]
                val desc = descPair?.first ?: "${layersArr.length()} layer kolase estetik"
                val icon = descPair?.second ?: "grid_view"

                val baseLayers = mutableListOf<LayerModel>()
                for (j in 0 until layersArr.length()) {
                    val lObj = layersArr.getJSONObject(j)
                    baseLayers.add(LayerModel.fromJsonObject(lObj))
                }

                list.add(
                    TemplatePreset(
                        id = id,
                        name = name,
                        cleanTitle = name.replace(Regex("""^\d+\.\s*"""), "").trim(),
                        description = desc,
                        iconName = icon,
                        defaultAspect = aspect,
                        backgroundColor = bg,
                        generateLayers = {
                            baseLayers.map { it.copy(id = UUID.randomUUID().toString()) }
                        },
                        generateAdaptedLayers = { targetW, targetH, isLandscape ->
                            adaptLayers(baseLayers, targetW, targetH, isLandscape)
                        }
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun adaptLayers(
        originalLayers: List<LayerModel>,
        targetWidth: Int,
        targetHeight: Int,
        isLandscape: Boolean
    ): List<LayerModel> {
        if (originalLayers.isEmpty()) return emptyList()

        val baseAspect = 1080f / 1350f // 0.8f (standard portrait)
        val targetAspect = targetWidth.toFloat() / targetHeight.toFloat()
        val currentAspect = if (isLandscape) 1f / baseAspect else baseAspect

        return originalLayers.map { layer ->
            val newId = UUID.randomUUID().toString()

            var lx = layer.x
            var ly = layer.y
            var lw = layer.width
            var lh = layer.height

            if (isLandscape) {
                // Transpose coordinates for landscape mode (x <-> y, w <-> h)
                val tempX = lx
                val tempW = lw
                lx = ly
                lw = lh
                ly = tempX
                lh = tempW
            }

            // Proportional scale to fit canvas margins without distortion
            if (targetAspect > currentAspect) {
                val scaleFactor = currentAspect / targetAspect
                val newLh = lh * scaleFactor
                val offsetY = (1f - scaleFactor) / 2f
                val newLy = ly * scaleFactor + offsetY
                layer.copy(
                    id = newId,
                    x = lx.coerceIn(0f, 1f),
                    y = newLy.coerceIn(0f, 1f),
                    width = lw.coerceIn(0.01f, 1f),
                    height = newLh.coerceIn(0.01f, 1f)
                )
            } else if (targetAspect < currentAspect) {
                val scaleFactor = targetAspect / currentAspect
                val newLw = lw * scaleFactor
                val offsetX = (1f - scaleFactor) / 2f
                val newLx = lx * scaleFactor + offsetX
                layer.copy(
                    id = newId,
                    x = newLx.coerceIn(0f, 1f),
                    y = ly.coerceIn(0f, 1f),
                    width = newLw.coerceIn(0.01f, 1f),
                    height = lh.coerceIn(0.01f, 1f)
                )
            } else {
                layer.copy(
                    id = newId,
                    x = lx.coerceIn(0f, 1f),
                    y = ly.coerceIn(0f, 1f),
                    width = lw.coerceIn(0.01f, 1f),
                    height = lh.coerceIn(0.01f, 1f)
                )
            }
        }
    }
}
