package com.facefirst.applock

import com.facefirst.applock.data.AppInfo
import com.facefirst.applock.data.filterApps
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterAppsTest {
    private val apps = listOf(
        AppInfo("com.google.android.calendar", "Takvim"),
        AppInfo("org.telegram.messenger", "Telegram"),
        AppInfo("com.whatsapp", "WhatsApp"),
    )

    @Test fun emptyQueryReturnsAll() = assertEquals(apps, filterApps(apps, "  "))
    @Test fun matchesLabelCaseInsensitive() = assertEquals(listOf(apps[0]), filterApps(apps, "takv"))
    @Test fun matchesPackageName() = assertEquals(listOf(apps[2]), filterApps(apps, "whatsapp"))
    @Test fun noMatchReturnsEmpty() = assertEquals(emptyList<AppInfo>(), filterApps(apps, "zzz"))
}
