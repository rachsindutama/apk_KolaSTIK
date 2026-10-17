# Implementation Plan - Dark Gray Studio Theme (Charcoal Dark Mode)

Transform the entire application into a sleek, professional Dark Gray Studio Theme (Charcoal dark mode) with high-contrast readable text, clean surfaces, and cohesive Material 3 dark styling.

## User Request & Clarification
- **User Request**: Changes Theme from LightColorScheme to DarkColorScheme
- **Clarification Answer**: Dark Gray Studio Theme (Charcoal dark mode)

## Proposed Changes

### 1. Theme Configuration (`Theme.kt`)
- Update `DarkColorScheme` and `LightColorScheme` (or set default theme to use dark charcoal background colors across the app).
- Define charcoal dark background (`0xFF121212` / `0xFF1E1E1E`), surface variant (`0xFF252528`), outline (`0xFF3A3A3E`), and high-contrast text (`0xFFE0E0E0` / `0xFFFFFFFF`).

### 2. Screen Backgrounds & Surfaces
- Update screen backgrounds across home screen, project lists, template selection, and collage editor (`CollageEditorScreen`, `NewProjectScreen`, `ImageSplitterScreen`) to utilize MaterialTheme dark charcoal colors instead of light/purple-tinted surfaces.
- Ensure cards, modals, and bottom sheets use clean dark charcoal tones (`#1E1/E1E` / `#252528`).

### 3. Verification
- Compile applet with `compile_applet` to ensure syntax correctness and build success.
