# BayarKuy Notifier — GitHub APK Maker

Repository ini sudah dilengkapi workflow GitHub Actions untuk membangun APK secara otomatis di server GitHub.

## Yang dibangun
- Debug APK: dapat dipasang untuk pengujian.
- Release APK: hasil `assembleRelease` tanpa konfigurasi signing, sehingga berupa APK release unsigned. Untuk distribusi publik, tandatangani dengan keystore milik Anda.

## Cara menggunakan
1. Buat repository baru di GitHub.
2. Upload seluruh isi folder proyek ini ke root repository (termasuk folder `.github`).
3. Pastikan branch utama bernama `main` atau `master`.
4. Buka tab **Actions** pada repository.
5. Pilih workflow **GitHub APK Maker - BayarKuy**.
6. Klik **Run workflow** untuk menjalankan manual. Workflow juga berjalan otomatis saat ada push atau pull request ke `main`/`master`.
7. Setelah status workflow hijau, buka hasil run dan unduh artifact **BayarKuy-Notifier-APK**.

## Persyaratan build yang dikunci
- Android Gradle Plugin: 7.0.4 (sesuai `build.gradle` proyek)
- Gradle: 7.2
- JDK: 11
- compileSdk: 30
- Android Build Tools: 30.0.3
- Module: `:app`
- Application ID: `com.bayarkuy.notifier`

## Catatan
- Workflow mengunduh Gradle 7.2 di runner GitHub, jadi proyek tidak memerlukan `gradlew` atau file wrapper.
- APK debug ditandatangani otomatis dengan debug key oleh Android build tools.
- APK release tidak ditandatangani karena proyek belum menyediakan konfigurasi keystore. Jangan menyimpan keystore/password di repository publik.
- Workflow ini membangun source yang ada; ia tidak mengubah atau menjamin kompatibilitas parser notifikasi, endpoint backend, atau integrasi gateway. Uji aplikasi di perangkat sebelum digunakan.
- Jika workflow gagal, buka log pada langkah yang berwarna merah di tab Actions. Jangan membagikan keystore atau password signing ke publik.
