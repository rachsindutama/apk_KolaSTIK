# Implementation Plan - Redesign Layers Panel to Modern M3 Light Theme

## User Requirements
1. **Layers Panel Redesign**: Replace the current dark "Photoshop-style" Layers panel with a clean, modern, light theme Material 3 list design.
2. **Design System**: Use Material 3 surface colors, card elevations, and typography for a professional look.
3. **Strict Scope Constraint**: Do not modify or add anything outside this Layers panel redesign.

## Proposed Changes

### 1. `CollageEditorScreen.kt`
- Locate the `ModalBottomSheet` implementation for `showLayerContextMenu`.
- Update `containerColor` to `MaterialTheme.colorScheme.surface`.
- Redesign the list items (layer rows) to use:
  - Surface cards or background with light colors (`MaterialTheme.colorScheme.surfaceVariant` for selected, `Transparent` for unselected).
  - Clean M3 icons and typography.
  - Remove all dark slate / Photoshop-specific dark styling.
  - Simplify the header and properties bar to follow M3 design guidelines.

## Verification Plan
- Run `compile_applet` to verify zero compilation errors.
- Test the new Layers panel UI.
