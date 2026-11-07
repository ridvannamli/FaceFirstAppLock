package com.facefirst.applock.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object InstalledApps {
    /** Başlatılabilir (launcher'da görünen) uygulamalar; kendi uygulamamız hariç. */
    suspend fun load(context: Context): List<AppInfo> = withContext(Dispatchers.Default) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .map { AppInfo(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    suspend fun icon(context: Context, packageName: String, sizePx: Int = 96): Bitmap? =
        withContext(Dispatchers.Default) {
            runCatching {
                val d = context.packageManager.getApplicationIcon(packageName)
                Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).also {
                    d.setBounds(0, 0, sizePx, sizePx); d.draw(Canvas(it))
                }
            }.getOrNull()
        }
}
