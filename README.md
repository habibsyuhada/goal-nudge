# Goal Nudge

Implementasi kode untuk semua fase di [PLAN.md](./PLAN.md) — overlay on-unlock, goal
engine, guard rails, dan metrik lokal. Kotlin + Jetpack Compose, native Android.

## Yang sudah diimplementasikan

**Fase 1 — Prototype teknis**
- `service/NudgeListenerService.kt` — foreground service persisten yang mendaftarkan
  receiver `ACTION_SCREEN_ON` secara runtime (bukan lewat manifest — sejak Android 8.0
  broadcast implisit itu tidak dikirim ke receiver manifest)
- `notification/NotificationHelper.kt` (`showUrgentNudge`) + `ui/nudge/NudgeFullScreenActivity.kt`
  + `overlay/NudgeOverlayCard.kt` — kartu nudge dibuka lewat `setFullScreenIntent`, tampil penuh
  layar bahkan di atas lock screen, dismissible (tombol / swipe / timeout). **Bukan lagi**
  `TYPE_APPLICATION_OVERLAY` + `SYSTEM_ALERT_WINDOW`: pendekatan overlay lama gagal reliable
  di banyak HP (background service-nya keburu dibunuh OS/OEM sebelum sempat menambahkan window),
  jadi diganti ke jalur notifikasi resmi Android yang tidak butuh izin overlay dan tetap jalan
  walau proses app baru saja dibangunkan dari kondisi mati. Trigger-nya sengaja layar-menyala
  (`ACTION_SCREEN_ON`), bukan setelah-unlock (`ACTION_USER_PRESENT`) — `setFullScreenIntent`
  cuma auto-muncul di atas lock screen kalau notifikasinya di-post SAAT layar masih
  mati/terkunci (pembatasan resmi Android sejak API 29); begitu user selesai unlock, Android
  cuma mau menampilkannya sebagai notifikasi biasa.
- `service/BootCompletedReceiver.kt` — re-schedule setelah reboot
- Onboarding (Android 14+) meminta `USE_FULL_SCREEN_INTENT` lewat halaman Settings khusus,
  dengan penjelasan jujur

**Fase 2 — MVP fungsional**
- `data/repository/GoalRepository.kt` + `ui/goal/*` — CRUD goal (judul, why, target
  date, tipe streak/persen/milestone, foto opsional), max 3 goal aktif
- Check-in satu tombol + streak counter (`GoalRepository.checkIn`)
- `domain/template/NudgeTemplateEngine.kt` — 5 variasi per tone (Lembut/Netral/Keras),
  variabel `{sisa_hari}`, `{streak}`, `{hari_sejak_checkin}`, `{why}`, `{judul}`
- `domain/model/NudgeWindow.kt` + `service/NudgeSelector.kt` — jendela pagi/siang/malam

**Fase 3 — Guard rails & polish**
- `domain/guard/RateLimiter.kt` — max N/hari, cooldown, quiet hours
- `domain/guard/ContextGuard.kt` + `CameraUsageObserver.kt` — skip saat telepon/kamera
  aktif, deteksi app navigasi via Usage Access (best-effort, fail-open)
- `widget/GoalWidget.kt` — widget home screen (Glance)
- `scheduling/NudgeScheduleWorker.kt` — jaring pengaman terjadwal, memicu nudge lewat jalur
  yang sama (`showUrgentNudge`) kalau `NudgeListenerService` tidak sempat menangkap unlock
- `ui/onboarding/OnboardingScreen.kt` + `platform/OemAutostartHelper.kt` — onboarding
  izin jujur + guide autostart Xiaomi/Oppo/Vivo (best-effort, fallback ke halaman app)
- Tombol "jeda 3 hari" di `ui/settings/SettingsScreen.kt`

**Fase 4 — Rilis kecil & iterasi**
- `ui/metrics/MetricsScreen.kt` — % nudge ditap vs di-swipe, dihitung lokal dari tabel
  `nudge_events`, tanpa backend/analytics pihak ketiga
- Beta rollout, monetisasi, dan iterasi konten berdasar data pengguna nyata adalah
  keputusan produk/bisnis yang butuh device fisik & pengguna asli — di luar cakupan kode.

## Yang TIDAK bisa diverifikasi di sesi ini

Lingkungan eksekusi sandbox ini **tidak punya akses ke Android SDK maupun Google Maven**
(`dl.google.com` diblokir proxy jaringan) — jadi Android Gradle Plugin, `compileSdk`, dan
seluruh dependency AndroidX tidak bisa di-resolve, dan proyek ini belum pernah berhasil
di-build/dijalankan dari sesi ini. Semua kode ditulis & direview manual mengikuti API
Android/Compose/Hilt/Room/WorkManager standar, tapi **wajib dibuild & diuji di mesin
dengan Android SDK sebelum dianggap berjalan**, terutama:

- Uji fisik di HP sendiri + minimal 1 HP Xiaomi/Oppo/Vivo (PLAN.md §6 Fase 1) —
  reliability `NudgeListenerService` di background sangat bergantung perilaku OEM, tidak
  bisa disimulasikan.
- Reliability service setelah reboot & setelah di-swipe dari recent apps.
- Perilaku `ForegroundServiceStartNotAllowedException` di Android 15 nyata.
- Full-screen intent notification di Android 14+ (API 34): perlu dicek apakah user harus
  mengaktifkan `USE_FULL_SCREEN_INTENT` manual lewat halaman Settings (onboarding sudah
  mengarahkan ke sana), dan apakah beberapa OEM mendemosikannya jadi heads-up notification
  biasa alih-alih benar-benar membuka activity secara otomatis.

## Build & run

```bash
./gradlew assembleDebug   # butuh Android SDK terpasang (ANDROID_HOME) + akses internet
./gradlew installDebug    # ke device/emulator yang terhubung
```

`gradlew` dan `gradle/wrapper/` sudah disertakan (Gradle 8.7).

### Build APK lewat GitHub Actions (tanpa Android SDK lokal)

Ada workflow manual di `.github/workflows/build-apk.yml`:

1. Buka tab **Actions** di repo GitHub → pilih workflow **Build APK**.
2. Klik **Run workflow** (trigger manual, tidak jalan otomatis tiap push).
3. Setelah selesai, buka run-nya → unduh artifact **goal-nudge-debug-apk** di
   bagian bawah halaman.
4. Extract zip-nya, dapat `app-debug.apk` — install langsung ke HP (perlu
   mengizinkan "install dari sumber tidak dikenal" untuk APK debug ini).

APK ini ditandatangani dengan debug keystore bawaan Android, cukup untuk testing
sendiri tapi bukan untuk rilis ke Play Store.

## Struktur singkat

```
app/src/main/java/com/goalnudge/app/
├── data/            # Room (entity/dao), DataStore settings, repository
├── domain/          # model, template engine, guard (rate limit/context)
├── service/         # foreground service, BroadcastReceiver, NudgeSelector
├── overlay/          # Compose card nudge (dipakai activity full-screen)
├── scheduling/       # WorkManager jaring pengaman
├── notification/     # notifikasi full-screen intent
├── platform/          # permission & OEM autostart helpers
├── widget/            # Glance home screen widget
└── ui/                # Compose screens (goal, settings, metrics, onboarding, nudge)
```
