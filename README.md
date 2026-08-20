# Goal Nudge

Implementasi kode untuk semua fase di [PLAN.md](./PLAN.md) — overlay on-unlock, goal
engine, guard rails, dan metrik lokal. Kotlin + Jetpack Compose, native Android.

## Yang sudah diimplementasikan

**Fase 1 — Prototype teknis**
- `service/NudgeListenerService.kt` — foreground service persisten yang mendaftarkan
  receiver `ACTION_USER_PRESENT` secara runtime (bukan lewat manifest — sejak Android 8.0
  broadcast implisit itu tidak dikirim ke receiver manifest)
- `overlay/OverlayManager.kt` + `overlay/NudgeOverlayCard.kt` — overlay
  `TYPE_APPLICATION_OVERLAY` full-screen, dismissible (tombol / swipe / timeout)
- `service/UnlockOverlayService.kt` — foreground service, di-start **setelah** overlay
  visible (urutan sesuai catatan Android 15 di PLAN.md §3)
- `service/BootCompletedReceiver.kt` — re-schedule setelah reboot
- Onboarding meminta `SYSTEM_ALERT_WINDOW` dengan penjelasan jujur

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
- `scheduling/NudgeScheduleWorker.kt` — fallback notifikasi terjadwal kalau jendela
  nudge terlewat atau overlay gagal ditambahkan
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
  overlay-on-unlock sangat bergantung perilaku OEM, tidak bisa disimulasikan.
- Reliability service setelah reboot & setelah di-swipe dari recent apps.
- Perilaku `ForegroundServiceStartNotAllowedException` di Android 15 nyata.

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
├── overlay/          # WindowManager overlay + Compose card
├── scheduling/       # WorkManager fallback
├── notification/     # notifikasi fallback
├── platform/          # permission & OEM autostart helpers
├── widget/            # Glance home screen widget
└── ui/                # Compose screens (goal, settings, metrics, onboarding)
```
