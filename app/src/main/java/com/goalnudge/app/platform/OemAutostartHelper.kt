package com.goalnudge.app.platform

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

enum class OemBrand { XIAOMI, OPPO, VIVO, UNKNOWN }

/**
 * OEM Cina (Xiaomi/Oppo/Vivo) punya autostart manager sendiri di luar sistem izin Android
 * standar — PLAN.md §3. Ini best-effort: intent komponen tidak didokumentasikan resmi dan
 * bisa berubah antar versi ROM, jadi selalu fallback ke halaman detail aplikasi.
 */
object OemAutostartHelper {

    fun detectBrand(): OemBrand = when (Build.MANUFACTURER.lowercase()) {
        "xiaomi" -> OemBrand.XIAOMI
        "oppo", "realme" -> OemBrand.OPPO
        "vivo" -> OemBrand.VIVO
        else -> OemBrand.UNKNOWN
    }

    private val candidateIntents: Map<OemBrand, List<ComponentName>> = mapOf(
        OemBrand.XIAOMI to listOf(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
        ),
        OemBrand.OPPO to listOf(
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
        ),
        OemBrand.VIVO to listOf(
            ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
        )
    )

    /** Coba buka halaman autostart manager OEM; null kalau tidak ada/tidak bisa dibuka. */
    fun autostartIntent(context: Context, brand: OemBrand): Intent? {
        val candidates = candidateIntents[brand].orEmpty()
        for (component in candidates) {
            val intent = Intent().apply {
                this.component = component
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) return intent
        }
        return null
    }

    fun appDetailsFallbackIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    fun tryOpenAutostartSettings(context: Context): Boolean {
        val brand = detectBrand()
        val intent = autostartIntent(context, brand) ?: appDetailsFallbackIntent(context)
        return try {
            context.startActivity(intent.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
