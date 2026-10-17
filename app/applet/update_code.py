import os

# 1. Update ProjectEntity.kt
entity_path = 'app/src/main/java/com/example/data/ProjectEntity.kt'
with open(entity_path, 'r') as f:
    content = f.read()

if 'fitImage' not in content:
    content = content.replace(
        'var outerGlowColor: Int = 0xFFFFD700.toInt()',
        'var outerGlowColor: Int = 0xFFFFD700.toInt(),\n    var fitImage: Boolean = false'
    )
    content = content.replace(
        'obj.put("outerGlowColor", outerGlowColor)',
        'obj.put("outerGlowColor", outerGlowColor)\n        obj.put("fitImage", fitImage)'
    )
    content = content.replace(
        'outerGlowColor = obj.optInt("outerGlowColor", 0xFFFFD700.toInt())',
        'outerGlowColor = obj.optInt("outerGlowColor", 0xFFFFD700.toInt()),\n                fitImage = obj.optBoolean("fitImage", false)'
    )
    with open(entity_path, 'w') as f:
        f.write(content)
    print('Updated ProjectEntity.kt successfully')

# 2. Update MainViewModel.kt
vm_path = 'app/src/main/java/com/example/ui/MainViewModel.kt'
with open(vm_path, 'r') as f:
    vm_content = f.read()

old_start_new = '''    fun startNewProject(presetId: String) {
        viewModelScope.launch {
            clearUndoRedo()
            val preset = TemplatePresets.presets.find { it.id == presetId }
            if (preset != null) {
                activeProjectId.value = null
                activeProjectName.value = "KolaSTIK ${preset.name}"
                activeAspectRatio.value = preset.defaultAspect
                activeBackgroundColor.value = 0xFFFFFFFF.toInt()
                _layers.value = preset.generateLayers()
                selectedLayerId.value = _layers.value.firstOrNull()?.id
            } else {
                // Default fallback
                activeProjectId.value = null
                activeProjectName.value = "Untitled Collage"
                activeAspectRatio.value = "1:1"
                activeBackgroundColor.value = 0xFFF5F5F7.toInt()
                _layers.value = listOf(
                    LayerModel(UUID.randomUUID().toString(), 0.1f, 0.1f, 0.8f, 0.8f, zIndex = 1, borderThickness = 2f, cornerRadius = 8f)
                )
                selectedLayerId.value = _layers.value.firstOrNull()?.id
            }
            navigateTo(Screen.Editor)
        }
    }'''

new_start_new = '''    fun startNewProject(presetId: String) {
        viewModelScope.launch {
            clearUndoRedo()
            val preset = TemplatePresets.presets.find { it.id == presetId }
            activeProjectId.value = null
            activeProjectName.value = if (preset != null) "KolaSTIK ${preset.name}" else "Untitled Collage"
            activeAspectRatio.value = preset?.defaultAspect ?: "1:1"
            activeBackgroundColor.value = 0xFFF5F5F7.toInt()
            _layers.value = emptyList()
            selectedLayerId.value = null
            navigateTo(Screen.Editor)
        }
    }'''

old_start_custom = '''    fun startCustomProject(width: Int, height: Int) {
        viewModelScope.launch {
            clearUndoRedo()
            val safeW = width.coerceIn(100, 13500)
            val safeH = height.coerceIn(100, 13500)
            activeProjectId.value = null
            activeProjectName.value = "Custom ${safeW}x${safeH}"
            activeAspectRatio.value = "$safeW:$safeH"
            activeBackgroundColor.value = 0xFFF5F5F7.toInt()
            _layers.value = listOf(
                LayerModel(UUID.randomUUID().toString(), 0.1f, 0.1f, 0.8f, 0.8f, zIndex = 1, borderThickness = 2f, cornerRadius = 8f)
            )
            selectedLayerId.value = _layers.value.firstOrNull()?.id
            navigateTo(Screen.Editor)
        }
    }'''

new_start_custom = '''    fun startCustomProject(width: Int, height: Int) {
        viewModelScope.launch {
            clearUndoRedo()
            val safeW = width.coerceIn(100, 13500)
            val safeH = height.coerceIn(100, 13500)
            activeProjectId.value = null
            activeProjectName.value = "Custom ${safeW}x${safeH}"
            activeAspectRatio.value = "$safeW:$safeH"
            activeBackgroundColor.value = 0xFFF5F5F7.toInt()
            _layers.value = emptyList()
            selectedLayerId.value = null
            navigateTo(Screen.Editor)
        }
    }'''

old_add_layer = '''        val newLayer = LayerModel(
            id = newId,
            x = 0.25f,
            y = 0.25f,
            width = 0.5f,
            height = 0.5f,
            zIndex = (_layers.value.maxOfOrNull { it.zIndex } ?: 0) + 1,
            borderThickness = 2f,
            borderColor = 0xFFFFFFFF.toInt(),
            cornerRadius = 8f,
            shapeType = "rect",
            imageUri = imageUri
        )'''

new_add_layer = '''        val newLayer = LayerModel(
            id = newId,
            x = 0.25f,
            y = 0.25f,
            width = 0.5f,
            height = 0.5f,
            zIndex = (_layers.value.maxOfOrNull { it.zIndex } ?: 0) + 1,
            borderThickness = 0f,
            borderColor = 0xFFFFFFFF.toInt(),
            cornerRadius = 0f,
            shapeType = "rect",
            imageUri = imageUri
        )'''

if old_start_new in vm_content:
    vm_content = vm_content.replace(old_start_new, new_start_new)
    print('Replaced startNewProject')
else:
    print('Warning: old_start_new not found')

if old_start_custom in vm_content:
    vm_content = vm_content.replace(old_start_custom, new_start_custom)
    print('Replaced startCustomProject')
else:
    print('Warning: old_start_custom not found')

if old_add_layer in vm_content:
    vm_content = vm_content.replace(old_add_layer, new_add_layer)
    print('Replaced addLayer')
else:
    print('Warning: old_add_layer not found')

with open(vm_path, 'w') as f:
    f.write(vm_content)
print('Updated MainViewModel.kt successfully')
