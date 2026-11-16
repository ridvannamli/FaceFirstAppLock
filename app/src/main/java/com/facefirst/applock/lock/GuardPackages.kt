package com.facefirst.applock.lock

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

/** Kaldırma koruması açıkken otomatik kilitlenen sistem paketleri. */
object GuardPackages {
    private val INSTALLER_FALLBACK = setOf("com.google.android.packageinstaller", "com.android.packageinstaller", "com.samsung.android.packageinstaller")
    private const val SETTINGS_FALLBACK = "com.android.settings"
    private const val PLAY_STORE = "com.android.vending"

    /** Kaldırma/yükleme onay penceresini gösteren paket(ler). */
    fun installers(context: Context): Set<String> {
        val pm = context.packageManager
        val resolved = runCatching {
            pm.resolveActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${context.packageName}")), PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
        }.getOrNull()
        return merge(resolved, INSTALLER_FALLBACK)
    }

    /** Ayarlar ve Play Store: kaldırma, zorla durdurma ve erişilebilirlik izni buradan yapılır. */
    fun others(context: Context): Set<String> {
        val pm = context.packageManager
        val settings = runCatching { pm.resolveActivity(Intent(Settings.ACTION_SETTINGS), PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName }.getOrNull()
        return merge(settings, setOf(SETTINGS_FALLBACK, PLAY_STORE))
    }

    /** "android" çözümleyici (birden çok aday) gerçek paket sayılmaz. */
    fun merge(resolved: String?, fallback: Set<String>): Set<String> =
        if (resolved.isNullOrBlank() || resolved == "android") fallback else fallback + resolved
}
