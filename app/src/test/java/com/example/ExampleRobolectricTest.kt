package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.MainViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun testUndoRedoFlow() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = MainViewModel(app)
    
    // Initial state: undo should be false
    assertFalse(viewModel.canUndo.value)
    assertFalse(viewModel.canRedo.value)

    // Start a new project with a preset
    viewModel.startNewProject("grid_2x2")
    val initialLayers = viewModel.layers.value
    assertTrue(initialLayers.isNotEmpty())

    // 1. Move a layer (position change)
    viewModel.recordSnapshot()
    val layerToMove = initialLayers.first()
    val updatedLayer = layerToMove.copy(x = 0.4f, y = 0.4f)
    viewModel.updateLayer(updatedLayer)

    assertEquals(0.4f, viewModel.layers.value.first { it.id == layerToMove.id }.x)
    assertTrue(viewModel.canUndo.value)

    // 2. Perform Undo
    viewModel.undo()
    assertEquals(layerToMove.x, viewModel.layers.value.first { it.id == layerToMove.id }.x)
    assertTrue(viewModel.canRedo.value)

    // 3. Perform Redo
    viewModel.redo()
    assertEquals(0.4f, viewModel.layers.value.first { it.id == layerToMove.id }.x)

    // 4. Resize frame
    viewModel.recordSnapshot()
    val resizedLayer = viewModel.layers.value.first { it.id == layerToMove.id }.copy(width = 0.8f, height = 0.7f)
    viewModel.updateLayer(resizedLayer)
    assertEquals(0.8f, viewModel.layers.value.first { it.id == layerToMove.id }.width)

    // Undo resize
    viewModel.undo()
    assertEquals(layerToMove.width, viewModel.layers.value.first { it.id == layerToMove.id }.width)
  }
}

