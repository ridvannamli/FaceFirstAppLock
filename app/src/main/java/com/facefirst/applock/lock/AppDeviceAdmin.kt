package com.facefirst.applock.lock

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.facefirst.applock.R

/**
 * İsteğe bağlı "cihaz yöneticisi": etkinken sistem uygulamanın kaldırılmasını reddeder.
 * Hiçbir politika (kamera, kilit, silme vb.) İSTENMEZ; yalnızca kaldırma engeli için kullanılır.
 * Kullanıcı sistemin kendi onay ekranından açar; uygulamadan (PIN ile) ya da Ayarlar'dan kapatabilir.
 */
class AppDeviceAdmin : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        context.getString(R.string.da_disable_warning)

    companion object {
        fun component(context: Context) = ComponentName(context, AppDeviceAdmin::class.java)
        fun isActive(context: Context): Boolean =
            context.getSystemService(DevicePolicyManager::class.java)?.isAdminActive(component(context)) == true
        fun enableIntent(context: Context): Intent =
            Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component(context))
                .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, context.getString(R.string.da_explain))
        fun disable(context: Context) {
            context.getSystemService(DevicePolicyManager::class.java)?.removeActiveAdmin(component(context))
        }
    }
}
