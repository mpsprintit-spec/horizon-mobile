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
