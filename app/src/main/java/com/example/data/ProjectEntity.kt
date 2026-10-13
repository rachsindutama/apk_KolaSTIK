package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val aspectRatio: String = "1:1", // "1:1", "4:5", "16:9", "9:16", "3:4", "A4"
    val backgroundColor: Int = 0xFFF5F5F7.toInt(),
    val layersJson: String, // JSONArray of layers
    val timestamp: Long = System.currentTimeMillis()
)

data class LayerModel(
    val id: String,
    var x: Float, // relative 0.0 to 1.0
    var y: Float, // relative 0.0 to 1.0
    var width: Float, // relative 0.0 to 1.0
    var height: Float, // relative 0.0 to 1.0
    var rotation: Float = 0f,
    var imageUri: String? = null,
    var zIndex: Int = 0,
    var borderThickness: Float = 0f, // relative to canvas scale or dp
    var borderColor: Int = 0xFFFFFFFF.toInt(),
    var cornerRadius: Float = 0f, // dp
    var filterId: String = "none",
    var imageScale: Float = 1.0f,
    var imageOffsetX: Float = 0f,
    var imageOffsetY: Float = 0f,
    var shapeType: String = "rect", // rect, rounded, oval, polaroid
    var isLocked: Boolean = false,
    var isVisible: Boolean = true,
    var opacity: Float = 1.0f,
    var name: String = "",
    // Layer Styles (Drop Shadow, Inner Shadow, Outer Glow)
    var dropShadowEnabled: Boolean = false,
    var dropShadowBlur: Float = 12f, // dp
    var dropShadowOffsetX: Float = 4f, // dp
    var dropShadowOffsetY: Float = 6f, // dp
    var dropShadowColor: Int = 0x66000000.toInt(),
    var innerShadowEnabled: Boolean = false,
    var innerShadowBlur: Float = 8f, // dp
    var innerShadowOffsetX: Float = 2f, // dp
    var innerShadowOffsetY: Float = 2f, // dp
    var innerShadowColor: Int = 0x80000000.toInt(),
    var outerGlowEnabled: Boolean = false,
    var outerGlowRadius: Float = 14f, // dp
    var outerGlowColor: Int = 0xFFFFD700.toInt()
) {
    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("x", x.toDouble())
        obj.put("y", y.toDouble())
        obj.put("width", width.toDouble())
        obj.put("height", height.toDouble())
        obj.put("rotation", rotation.toDouble())
        obj.put("imageUri", imageUri ?: "")
        obj.put("zIndex", zIndex)
        obj.put("borderThickness", borderThickness.toDouble())
        obj.put("borderColor", borderColor)
        obj.put("cornerRadius", cornerRadius.toDouble())
        obj.put("filterId", filterId)
        obj.put("imageScale", imageScale.toDouble())
        obj.put("imageOffsetX", imageOffsetX.toDouble())
        obj.put("imageOffsetY", imageOffsetY.toDouble())
        obj.put("shapeType", shapeType)
        obj.put("isLocked", isLocked)
        obj.put("isVisible", isVisible)
        obj.put("opacity", opacity.toDouble())
        obj.put("name", name)
        // Layer styles
        obj.put("dropShadowEnabled", dropShadowEnabled)
        obj.put("dropShadowBlur", dropShadowBlur.toDouble())
        obj.put("dropShadowOffsetX", dropShadowOffsetX.toDouble())
        obj.put("dropShadowOffsetY", dropShadowOffsetY.toDouble())
        obj.put("dropShadowColor", dropShadowColor)
        obj.put("innerShadowEnabled", innerShadowEnabled)
        obj.put("innerShadowBlur", innerShadowBlur.toDouble())
        obj.put("innerShadowOffsetX", innerShadowOffsetX.toDouble())
        obj.put("innerShadowOffsetY", innerShadowOffsetY.toDouble())
        obj.put("innerShadowColor", innerShadowColor)
        obj.put("outerGlowEnabled", outerGlowEnabled)
        obj.put("outerGlowRadius", outerGlowRadius.toDouble())
        obj.put("outerGlowColor", outerGlowColor)
        return obj
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): LayerModel {
            return LayerModel(
                id = obj.getString("id"),
                x = obj.getDouble("x").toFloat(),
                y = obj.getDouble("y").toFloat(),
                width = obj.getDouble("width").toFloat(),
                height = obj.getDouble("height").toFloat(),
                rotation = obj.optDouble("rotation", 0.0).toFloat(),
                imageUri = obj.optString("imageUri", "").takeIf { it.isNotEmpty() },
                zIndex = obj.optInt("zIndex", 0),
                borderThickness = obj.optDouble("borderThickness", 0.0).toFloat(),
                borderColor = obj.optInt("borderColor", 0xFFFFFFFF.toInt()),
                cornerRadius = obj.optDouble("cornerRadius", 0.0).toFloat(),
                filterId = obj.optString("filterId", "none"),
                imageScale = obj.optDouble("imageScale", 1.0).toFloat(),
                imageOffsetX = obj.optDouble("imageOffsetX", 0.0).toFloat(),
                imageOffsetY = obj.optDouble("imageOffsetY", 0.0).toFloat(),
                shapeType = obj.optString("shapeType", "rect"),
                isLocked = obj.optBoolean("isLocked", false),
                isVisible = obj.optBoolean("isVisible", true),
                opacity = obj.optDouble("opacity", 1.0).toFloat(),
                name = obj.optString("name", ""),
                dropShadowEnabled = obj.optBoolean("dropShadowEnabled", false),
                dropShadowBlur = obj.optDouble("dropShadowBlur", 12.0).toFloat(),
                dropShadowOffsetX = obj.optDouble("dropShadowOffsetX", 4.0).toFloat(),
                dropShadowOffsetY = obj.optDouble("dropShadowOffsetY", 6.0).toFloat(),
                dropShadowColor = obj.optInt("dropShadowColor", 0x66000000.toInt()),
                innerShadowEnabled = obj.optBoolean("innerShadowEnabled", false),
                innerShadowBlur = obj.optDouble("innerShadowBlur", 8.0).toFloat(),
                innerShadowOffsetX = obj.optDouble("innerShadowOffsetX", 2.0).toFloat(),
                innerShadowOffsetY = obj.optDouble("innerShadowOffsetY", 2.0).toFloat(),
                innerShadowColor = obj.optInt("innerShadowColor", 0x80000000.toInt()),
                outerGlowEnabled = obj.optBoolean("outerGlowEnabled", false),
                outerGlowRadius = obj.optDouble("outerGlowRadius", 14.0).toFloat(),
                outerGlowColor = obj.optInt("outerGlowColor", 0xFFFFD700.toInt())
            )
        }

        fun toJsonArray(layers: List<LayerModel>): String {
            val arr = JSONArray()
            layers.forEach { arr.put(it.toJsonObject()) }
            return arr.toString()
        }

        fun fromJsonArray(jsonStr: String): List<LayerModel> {
            if (jsonStr.isEmpty()) return emptyList()
            val list = mutableListOf<LayerModel>()
            try {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    list.add(fromJsonObject(arr.getJSONObject(i)))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return list
        }
    }
}
