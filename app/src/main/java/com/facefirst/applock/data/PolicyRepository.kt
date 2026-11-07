package com.facefirst.applock.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.facefirst.applock.lock.Policy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.policyStore by preferencesDataStore("policy_store")

class PolicyRepository(private val context: Context) {
    private val kGlobal = stringPreferencesKey("global")
    private val kOverrides = stringSetPreferencesKey("overrides")   // "paket|politika"
    private val kEnabled = booleanPreferencesKey("enabled")
    private val store get() = context.applicationContext.policyStore

    val global: Flow<Policy> = store.data.map { Policy.from(it[kGlobal]) }
    val overrides: Flow<Map<String, Policy>> = store.data.map { p ->
        (p[kOverrides] ?: emptySet()).mapNotNull { e -> e.split('|').takeIf { it.size == 2 }?.let { it[0] to Policy.from(it[1]) } }.toMap()
    }
    val enabled: Flow<Boolean> = store.data.map { it[kEnabled] ?: true }

    suspend fun setGlobal(p: Policy) { store.edit { it[kGlobal] = p.key } }
    suspend fun setEnabled(v: Boolean) { store.edit { it[kEnabled] = v } }
    /** policy == null: uygulamaya özel ayarı kaldır (genel politika geçerli olur). */
    suspend fun setOverride(pkg: String, policy: Policy?) {
        store.edit { prefs ->
            val cur = (prefs[kOverrides] ?: emptySet()).filterNot { it.startsWith("$pkg|") }.toSet()
            prefs[kOverrides] = if (policy == null) cur else cur + "$pkg|${policy.key}"
        }
    }
}
