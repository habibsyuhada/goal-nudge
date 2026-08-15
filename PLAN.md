# PLAN.md — Goal Nudge (working title)

> Aplikasi Android yang menampilkan goal pribadi user + alasan ("why") mereka sendiri
> di momen paling rentan buang waktu: **detik setelah unlock HP**.
>
> Bukan quote app. Bukan habit tracker. Positioning: *pengingat goal yang menemukanmu,
> bukan yang harus kamu buka.*

---

## 1. Konsep Inti

Saat user unlock HP (pada momen-momen terjadwal, BUKAN setiap unlock), muncul
**overlay ringan 3–5 detik** berisi satu kartu nudge:

```
"Lulus TOEFL 550 sebelum Desember."
Kamu menulis ini 34 hari lalu. Alasanmu: "biar bisa daftar beasiswa itu."
Check-in terakhir: 3 hari lalu.

[Kerjakan sekarang]   [Nanti]
```

Prinsip desain:
- Kata-kata **user sendiri** > quote generik. Quote hanya bumbu.
- **Kelangkaan**: default 2–4 nudge/hari (unlock pertama pagi, siang, malam refleksi).
  Muncul setiap unlock = dibenci = uninstall.
- **Restraint** adalah fitur: tahu kapan harus diam.

## 2. Riset Kompetitor (ringkas)

| App | Apa yang mereka lakukan | Gap |
|---|---|---|
| Intenty (Android) | Full-screen prompt tiap unlock, tanya "niatmu apa?" | Fokus digital wellbeing, tidak terhubung ke goal |
| App Pause | Delay wajib sebelum app distraksi + tampil quote/goal | Anti-distraksi, bukan goal engine |
| Pause Moment | Foto + pesan personal di lock screen | Tidak ada tracking/progres |
| GoalGrid (iOS) | Wallpaper dot-grid progres goal | Pasif, satu goal, tanpa nudge aktif |
| Motivation Lockscreen-Quotes dkk | Quote otomatis di lock screen | Konten generik, cepat diabaikan |
| Strides / Way of Life / TickTick | Goal & habit tracking lengkap | Harus dibuka sendiri; tidak "menemukan" user |

**Celah yang kita isi:**
1. Belum ada yang menghubungkan nudge-on-unlock dengan goal engine (progres, deadline, streak).
2. Semua konten kompetitor statis/generik — kita pakai pesan kontekstual dari data goal user.
3. Tidak ada yang menangani habituation (user auto-swipe minggu ke-2).
4. Pasar berbahasa Indonesia belum tergarap.

## 3. Kendala Teknis (menentukan scope)

### Android — bisa, jalurnya:
- Android 10+ membatasi background activity start. `ACTION_USER_PRESENT` saja tidak cukup.
- Jalur yang dipakai: **`SYSTEM_ALERT_WINDOW`** (Display over other apps) +
  `TYPE_APPLICATION_OVERLAY`, dengan foreground service persistent.
- Android 15: overlay window harus **visible dulu** sebelum start foreground service
  dari background (kalau tidak → `ForegroundServiceStartNotAllowedException`).
- **JANGAN pakai AccessibilityService** — risiko rejection Play Store tinggi.
- OEM Cina (Xiaomi/Oppo/Vivo): autostart manager sendiri → butuh guide manual in-app
  per-OEM + deteksi kalau nudge gagal muncul.

### iOS — auto-launch on unlock TIDAK MUNGKIN.
Tidak ada API-nya; Shortcuts tidak punya trigger "unlocked". Versi iOS nanti harus
widget-first + Live Activity + Screen Time shield. **Ditunda ke Fase 4.**

## 4. Keputusan Produk

### Wajib ada
- Max **3 goal aktif** (fokus; 10 goal = 0 goal).
- Saat buat goal, WAJIB isi: judul, **"why"** (1–2 kalimat, bahasa user sendiri),
  target date. Opsional: foto.
- Tone nudge dipilih user: **Lembut / Netral / Keras** ("mode tampar" = pembeda produk).
- **Rate limiting sejak MVP**: max N/hari, cooldown antar-nudge, quiet hours,
  skip saat panggilan/kamera/navigasi aktif.
- Tombol "jeda 3 hari" yang mudah diakses (mencegah uninstall karena kesal).
- Check-in = satu tombol. Bukan habit tracker lengkap.
- Semua data **lokal** (Room). Tanpa backend, tanpa login.

### Tiga lapis kehadiran (overlay itu rapuh)
1. **Overlay on-unlock** — pukulan utama, 2–4x/hari
2. **Widget home screen** — pasif, selalu terlihat: goal + sisa hari + streak
3. **Notifikasi terjadwal** — fallback + momen khusus ("tinggal 7 hari!")

### Tidak dibuat (sengaja)
- ❌ Habit tracker lengkap
- ❌ Backend / login / sync
- ❌ iOS (dulu)
- ❌ AccessibilityService
- ❌ Muncul di setiap unlock

## 5. Stack

- **Kotlin + Jetpack Compose** (native — overlay/receiver/service harus native anyway,
  Flutter/RN hanya menambah lapisan kompleksitas)
- Room (persistence), DataStore (settings)
- WorkManager (scheduling notifikasi fallback)
- Foreground service + BroadcastReceiver (`ACTION_USER_PRESENT`)
- Min SDK: 26 (Android 8.0), Target: terbaru

## 6. Roadmap

### Fase 0 — Validasi konsep (sudah/berjalan)
- [x] Riset kompetitor
- [ ] Pakai Intenty + App Pause 1 minggu, catat kapan mulai auto-swipe

### Fase 1 — Prototype teknis (Minggu 1) ← **MULAI DI SINI**
Satu tujuan: **buktikan overlay bisa muncul reliably setelah unlock.**
Kalau ini gagal, semua desain di atas bubar.
- [ ] Project setup (Kotlin, Compose, Hilt)
- [ ] Foreground service + BroadcastReceiver `ACTION_USER_PRESENT`
- [ ] Overlay `TYPE_APPLICATION_OVERLAY` full-screen, dismissible
- [ ] Request flow permission `SYSTEM_ALERT_WINDOW`
- [ ] Uji di HP sendiri + minimal 1 HP Xiaomi/Oppo (kasus terburuk)
- [ ] Uji: service survive setelah reboot, setelah swipe dari recent apps

### Fase 2 — MVP fungsional (Minggu 2–4)
- [ ] Goal CRUD: judul + why + target date + tipe (streak / persen / milestone) + foto
- [ ] Check-in satu tombol + streak counter
- [ ] Template engine nudge: 5–10 variasi per tone (Lembut/Netral/Keras)
- [ ] Variabel template: {sisa_hari}, {streak}, {hari_sejak_checkin}, {why}, {judul}
- [ ] Scheduling nudge: pagi (unlock pertama), siang, malam

### Fase 3 — Guard rails & polish (Minggu 5–6)
- [ ] Rate limiting + cooldown + quiet hours
- [ ] Deteksi konteks (jangan muncul saat call/camera/maps)
- [ ] Widget home screen
- [ ] Notifikasi fallback (deteksi overlay gagal → beralih ke notif)
- [ ] Onboarding permission jujur + guide autostart per-OEM
- [ ] Tombol jeda 3 hari

### Fase 4 — Rilis kecil & iterasi
- [ ] Beta ke 10–20 orang
- [ ] **Satu metrik utama: % nudge di-tap vs di-swipe** (ini penentu hidup-mati produk)
- [ ] Iterasi konten berdasar data
- [ ] Baru setelah itu: anti-habituation lanjutan, iOS widget-first, monetisasi freemium

## 7. Risiko

| Risiko | Mitigasi |
|---|---|
| Play Store reject (overlay) | Hanya SYSTEM_ALERT_WINDOW, tanpa AccessibilityService, deklarasi jelas |
| OEM kill background service | Foreground service + guide autostart + deteksi gagal → fallback notif |
| User benci app-nya | Rate limit agresif dari hari 1, tombol jeda, kelangkaan by design |
| Android 16+ makin ketat | Widget & notif sebagai jalur cadangan permanen |
| Habituation (auto-swipe) | Variasi format, frekuensi adaptif (turun saat streak bagus, naik saat bolong) |
