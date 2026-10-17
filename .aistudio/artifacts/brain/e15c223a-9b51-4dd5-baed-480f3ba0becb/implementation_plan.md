# Implementation Plan - Fix Placeholder Distortion on Custom Canvas

## Problem
When a user sets custom canvas dimensions (manual input), new placeholders added are distorted ("gepeng") and not centered because the current `addLayer` logic uses hardcoded fraction calculations that assume a square or specific aspect ratio canvas.

## Proposed Solution
1. **Dynamic Canvas Ratio**: Calculate `canvasRatio` dynamically based on the current active aspect ratio (e.g., from manual input `"W:H"`).
2. **Proporsional Scaling**: Update `addLayer` logic in `CollageEditorScreen.kt` (placeholder dialog) to:
    - Compute `hFrac` using `wFrac * canvasRatio / targetRatio` to ensure the placeholder remains proportional to the custom canvas.
    - Set `initialX` and `initialY` based on center calculation (`(1f - wFrac) / 2f`) to guarantee centering.
3. **Verification**: Compile and verify.
