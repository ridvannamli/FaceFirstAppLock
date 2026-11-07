package com.facefirst.applock.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("locked_apps")
private val LOCKED = stringSetPreferencesKey("locked_packages")

class LockedAppsRepository(private val context: Context) {
    val lockedPackages: Flow<Set<String>> = context.dataStore.data.map { it[LOCKED] ?: emptySet() }

    suspend fun setLocked(packages: Collection<String>, locked: Boolean) {
        context.dataStore.edit { prefs ->
            val cur = prefs[LOCKED] ?: emptySet()
            prefs[LOCKED] = if (locked) cur + packages else cur - packages.toSet()
        }
    }
}
