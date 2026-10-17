# Rencana Implementasi: Pemindahan Footer @rachsindutama ke Footer Halaman Pertama

Memindahkan teks footer `@rachsindutama` dari bagian header ke bagian bawah (*footer*) halaman pertama (`NewProjectScreen`) dengan gaya teks abu-abu redup (*subtle*).

---

## Konfirmasi & Keputusan Pengguna

> [!IMPORTANT]
> - **Lokasi Baru**: Bagian bawah (*footer*) halaman pertama (`NewProjectScreen`), bukan di header.
> - **Gaya Visual**: Teks sederhana berwarna abu-abu redup (*subtle*).

---

## 1. Perubahan Kode

1. **Hapus dari Header**: Hapus teks `@rachsindutama` dari `TopAppBar` actions di `NewProjectScreen.kt`.
2. **Tambahkan di Footer**: Tambahkan item footer di bagian paling bawah `LazyColumn` di `NewProjectScreen.kt` agar tampil sebagai footer halaman pertama saat di-scroll ke bawah (atau di luar list dengan padding yang rapi).

---

## 2. Verifikasi

1. Kompilasi aplikasi menggunakan `compile_applet`.
2. Verifikasi bahwa `@rachsindutama` tampil di bagian bawah (*footer*) halaman pertama.
