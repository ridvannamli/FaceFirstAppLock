package com.facefirst.applock.data

data class AppInfo(val packageName: String, val label: String)

/** Aramaya göre süzer (ad veya paket adı, büyük/küçük harf duyarsız). */
fun filterApps(apps: List<AppInfo>, query: String): List<AppInfo> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return apps
    return apps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
}
