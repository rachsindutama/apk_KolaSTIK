# Perbaikan Jitter Drag & Drop Elemen pada Zoom Kanvas 200% - 800%

Perbaikan komprehensif pada penanganan gesture sentuhan, perhitungan delta posisi, dan isolasi gesture kanvas untuk mengeliminasi getaran (jitter) saat menggeser gambar/placeholder pada tingkat pembesaran zoom 200% hingga 800%.

## User Review & Critical Decisions

> [!IMPORTANT]
> Masalah jitter saat zoom tinggi (200% - 800%) disebabkan oleh dua faktor utama:
> 1. **Loop Feedback Asinkron Antara State & Layout**: Perhitungan posisi saat ini mencampurkan `currentLayer.x` (dari StateFlow ViewModel) dengan `change.position.x` (dari Layout Box lokal). Karena jeda frame asinkron antara mutasi state dan pass layout Compose, koordinat melompat maju-mundur secara bergantian, yang diamplifikasi hingga 8x saat kanvas di-zoom.
> 2. **Kebocoran Event ke Gesture Pan Kanvas**: `change.consume()` sebelumnya hanya dipanggil jika `distanceMoved > 6f`. Pada zoom 800%, 6 unit kanvas setara dengan 48 piksel layar, sehingga kontainer kanvas di luar salah mengartikan sentuhan sebagai pan kanvas bersamaan dengan pergerakan elemen.

- **Keputusan 1 (Akumulasi Delta Murni)**: Mengganti formula `currentLayer.x + change.position.x` dengan akumulasi delta murni (`accumulatedDeltaX += change.positionChange().x`). Menghilangkan sepenuhnya ketergantungan pada posisi layout yang sedang bergerak sehingga tidak terjadi feedback loop.
- **Keputusan 2 (Konsumsi Event Instan & Isolasi Total)**: Memanggil `change.consume()` segera saat pointer bergerak di atas layer agar gesture pan/zoom kanvas di level parent tidak aktif bersamaan.
- **Keputusan 3 (Penyatuan Spring Animation Release)**: Menggabungkan animasi spring sumbu X dan Y menjadi satu eksekusi terpadu (`Animatable<Offset, ...>`), mencegah race condition saat kedua coroutine memanggil `viewModel.updateLayer`.

---

## 1. Overview & Core Concept

- **Apa yang Diperbaiki**: Pengguna dapat menggeser (swipe / drag & drop) foto, frame, atau placeholder elemen kolase dengan gerakan 1:1 yang sangat mulus (*buttery smooth*), tanpa getaran (*jitter*), patah-patah, atau pergeseran kanvas yang tidak disengaja, bahkan pada zoom maksimal 800%.
- **Target Pengguna**: Pembuat kolase foto yang membutuhkan presisi tinggi saat menyusun layout pada level zoom detail.
- **Nilai Utama**: Stabilitas koordinat mutlak dan performa 60/120 FPS tanpa jitter pada rentang zoom 10% hingga 800%.

---

## 2. User Experience & Visual Design

- **Interaksi Sentuh**:
  - Saat jari menyentuh layer pada zoom 800%, elemen langsung terkunci pada jari dan bergerak secara presisi tanpa ada jeda atau lonjakan tiba-tiba.
  - Garis panduan magnetik (*alignment guides*) tetap aktif dan terdeteksi secara akurat tanpa membuat elemen bergetar di sekitar garis batas.
  - Saat dilepas (*drag release*), elemen meluncur halus (*fling*) dengan pegas (*spring physics*) ke posisi akhir tanpa tumpang-tindih update sumbu X dan Y.
- **Umpan Balik Visual**:
  - Kotak seleksi batas elemen (*selection border*) dan handle sudut beradaptasi secara mulus mengikuti posisi layer secara real-time.

---

## 3. Key Product Decisions & Trade-Offs

- **Formula Delta Akumulatif vs Koordinat Relatif Dinamis**:
  - *Pendekatan*: Menggunakan akumulasi pergeseran posisi instan (`change.positionChange()`) dari titik awal layer (`startLayer.x`, `startLayer.y`).
  - *Alasan*: Menghilangkan fluktuasi layout delay antara frame Compose.
  - *Alternatif Ditolak*: Menggunakan `currentLayer.x + change.position.x` terbukti memicu oscillation jitter yang parah saat zoom diperbesar.
- **Ambang Batas Drag (Drag Slop)**:
  - *Pendekatan*: Menghapus ambang batas `6f` piksel kanvas yang membesar secara proporsional saat zoom, dan langsung mengonsumsi event saat drag terdeteksi.
  - *Alasan*: Mencegah kontainer kanvas luar mengira gerakan mikro sebagai pan kanvas.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│  Workspace Outer Box (Pinch Zoom & Pan Gestures)       │
│  - Multi-touch transform gestures (2 fingers)         │
│  - Ignored when single-finger touches a layer          │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│  Canvas Coordinate Container (BoxWithConstraints)      │
│  - Stable pixel space: (canvasWidthPx, canvasHeightPx) │
│  - Scaled by graphicsLayer (0.1x to 8.0x)              │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│  Layer Item Box                                        │
│  - PointerInput Scope:                                 │
│    • awaitFirstDown -> consume & record start pos     │
│    • Move loop -> accumulate positionChange()         │
│    • targetX = startX + (accumulatedX / canvasWidthPx)│
│    • Magnet snap calculation & single updateLayer     │
│    • Release -> Unified Spring Animatable(Offset)     │
└────────────────────────────────────────────────────────┘
```

### Rencana File yang Dimodifikasi:
1. `app/src/main/java/com/example/ui/screens/CollageEditorScreen.kt`:
   - Refactor modifier `.pointerInput` pada layer agar menggunakan akumulasi `positionChange()` yang bebas dari layout feedback loop.
   - Panggil `change.consume()` secara konsisten untuk mencegah gesture pan kanvas aktif bersamaan saat drag layer.
   - Refactor spring release animation menjadi animasi `Animatable` terkoordinasi (satu update per frame).
