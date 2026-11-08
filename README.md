# Horizon Mobile

**Horizon Mobile** adalah klien Android native untuk **satu canonical Brain Horizon**.

## Status implementasi
- Nama aplikasi: **Horizon Mobile**
- Android application ID: `com.valtheron.horizon.mobile` (independen dari aplikasi lain)
- Antarmuka: Compose dark theme hitam–cyan–ungu, Chat, Neuron, Memori, Pengaturan
- Chat: mengirim `ObservationEnvelope` dengan token bahasa ke Bridge Go lokal; balasan adalah metadata neural native, **belum generasi kalimat alami**
- Neuron: renderer **HTML Canvas 3D** yang sudah disetujui dibundel sebagai asset APK; data nyata diambil melalui Kotlin `NeuralGraphGateway` secara read-only dari bounded inspection API Horizon
- Memori: ringkasan total neuron, sinaps, identitas Brain, dan revisi secara read-only
- Tidak ada mode API model AI eksternal, model GPT, login provider, atau kunci layanan cloud
- `main` pada repository `horizon-artificial-brain` tidak diubah

## Menjalankan
1. Jalankan runtime **Go Horizon** di perangkat yang sama melalui Termux dengan Bridge pada `127.0.0.1:8765`. Untuk konfigurasi yang memerlukan autentikasi, isi token di layar Pengaturan.
2. Pasang APK Horizon Mobile dan buka.
3. Layar Neuron memakai Canvas internal APK. **Tidak diperlukan server Python atau browser Chrome.** Data diperbarui melalui permintaan inspeksi read-only; bukan setiap frame komputasi neuron.
4. Jika runtime tidak menyala, status OFFLINE ditampilkan. Pilihan **Contoh** pada monitor adalah data simulasi manual, bukan data Horizon.

Batasan penting: posisi titik 3D adalah proyeksi matematis representasi dan topologi sinaps, **bukan koordinat fisik**. Jumlah neuron/sinaps tampak adalah sampel dibatasi; selalu dibedakan dari jumlah total. Endpoint Bridge Go saat ini mengembalikan status, ID neuron dan confidence, bukan paragraf jawaban. Aplikasi tidak mengarang teks bahasa alami.

## Arsitektur
```
Horizon Go canonical Brain (Termux)
      │  127.0.0.1:8765
      ├── POST /v1/observations  → Chat numerik native
      └── GET  /v1/brain/inspection → Kotlin bounded sampling
                                      │
                         HTML + Canvas yang dibundel
                         di WebView origin lokal tepercaya
```

- WebView memuat HTML dari `app/src/main/assets/neural/index.html` melalui `WebViewAssetLoader`.
- Permintaan internal `/api/graph` ditangani langsung Kotlin, bukan oleh server publik.
- Browser WebView tidak diberi hak akses file atau kemampuan mengakses host di luar origin internal aplikasi.
- Bridge terikat pada localhost dan diperlakukan sebagai sumber tunggal untuk data neural.

## Build dan pengujian

Jalankan `./gradlew :app:assembleDebug` menggunakan JDK 17 dan Android SDK 36. GitHub Actions menyediakan workflow build debug serta APK pada artefak workflow yang sukses. PR ini tetap draft hingga ada uji fungsional pada perangkat Android dan koneksi real ke runtime Horizon.

## Keaslian kode dan lisensi

Android/Jetpack Compose scaffold diadaptasi secara selektif dari [GPTMobile](https://github.com/Taewan-P/gpt_mobile) karya Taewan-P dan para kontributor, berlisensi GNU GPL-3.0. Lisensi dan atribusi hukum tetap ada dalam `LICENSE` dan `NOTICE.md`. Bagian itu adalah dokumentasi kepatuhan sumber, **bukan branding aplikasi**.


## Neural Graph: perbaikan pembacaan data asli (0.1.1)

- **Satu permintaan inspeksi**: `GET http://127.0.0.1:8765/v1/brain/inspection?section=neural_units&order=activity&limit=256`.
- Protokol lengkap mengambil `data.items` dan `data.related_synapses` dari **halaman dan revisi yang sama**, bukan menggabungkan beberapa halaman dari state yang terus berubah.
- `data.counts` melaporkan neuron/sinaps *total*; banyaknya titik dan garis tampak adalah sampel maksimal 256 neuron dan koneksi yang berada di antara neuron terpilih.
- Jika Bridge mengembalikan 413, adapter menurunkan `limit` dengan pembacaan terpisah yang konsisten.
- Visualizer tidak otomatis mengarang data jika gagal; menampilkan kesalahan 401/403 (token), 404 (Bridge lawas), 503 (runtime), serta mencoba lagi otomatis setiap 15 detik. Tombol **↻ Data Horizon** dapat memaksa pemuatan ulang.
- Label **BRIDGE** pada header aplikasi menunjukkan health Go; status **DATA HORIZON** pada Canvas menunjukkan inspeksi aktual.
- Pengujian parser terpisah (`NeuralGraphContractTest`) termasuk schema, edges, counts, dan penolakan identitas Brain asing.

### Memasang tanpa kehilangan chat

Build **debug** dipaketkan sebagai `com.valtheron.horizon.mobile.preview` dan berlabel **Horizon Mobile Uji**. APK debug GitHub Actions menggunakan signing key temporer; menginstal versi baru dengan applicationId utama dapat menyebabkan `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, dan uninstall aplikasi asli dapat menghapus riwayat lokal.

Versi Uji bisa dipasang **berdampingan** dengan aplikasi lama. Ia berbagi runtime Go pada `127.0.0.1:8765` tetapi menyimpan pengaturan dan chat uji secara terpisah. Untuk rilis utama, sediakan signing key yang stabil secara aman (jangan commit private key ke repository).

## v0.1.2 — Proyeksi jalur aktivitas neural pada Canvas 3D

Visualisasi kini memuat `activation`, `dynamic.activation`, waktu aktivasi unit/sinaps, bobot serta confidence dari halaman inspeksi *yang sama* dengan topologi. Sinaps berarah sumber → target digambar sebagai garis asli, dan hingga 160 jalur dengan indikasi aktivitas tertinggi diberi animasi cahaya. Warna sian adalah koneksi biasa, amber adalah inhibisi. Tombol **Jalur Aktivitas** mengaktifkan/menonaktifkan lapisan, **Jeda** menghentikan animasi, dan **Data Horizon** membaca snapshot lagi.

**Ketepatan ilmiah:** Jalur yang dipilih dihitung dengan proksi `max(activation_sinaps, dynamic_activation_sinaps, activation_neuron_sumber × |weight_sinaps| × confidence_sinaps)`, dipotong pada [0,1]. Ini hanya indikator aktivitas/kekuatan koneksi dari **satu snapshot**. Arah berasal dari ID synapse yang sebenarnya, tetapi *posisi 3D, kecepatan titik, fase dan jumlah paket* **bukan** data temporal transmisi neural, dan tidak boleh ditafsirkan sebagai pengukuran sinyal fisik atau event neural individual. Bila proksi semua jalur di bawah ambang, tidak ada titik bergerak yang direkayasa.

Monitor mengambil hanya koneksi di antara neuron terpilih pada satu halaman inspeksi; banyak koneksi lain bisa tidak terlihat. Bridge Horizon perlu menambahkan observabilitas event/jalur neural yang benar-benar instrumented agar dapat menampilkan setiap transmisi aktual; fitur itu **belum ada**. Runtime dan penyimpanan canonical Brain tidak dimodifikasi untuk visualisasi ini.

APK debug ini menggunakan package `com.valtheron.horizon.mobile.visualtest` supaya dapat dipasang tanpa mengganti dan menghapus data aplikasi sebelumnya. Label: **Horizon Mobile Aktivitas**.
