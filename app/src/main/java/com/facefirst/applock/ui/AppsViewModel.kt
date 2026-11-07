package com.facefirst.applock.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.facefirst.applock.data.AppInfo
import com.facefirst.applock.data.InstalledApps
import com.facefirst.applock.data.LockedAppsRepository
import com.facefirst.applock.data.PolicyRepository
import com.facefirst.applock.data.filterApps
import com.facefirst.applock.lock.LockRuntime
import com.facefirst.applock.lock.Policy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppsUiState(
    val loading: Boolean = true,
    val query: String = "",
    val apps: List<AppInfo> = emptyList(),
    val locked: Set<String> = emptySet(),
    val totalLocked: Int = 0,
    val global: Policy = Policy.EVERY_ENTRY,
    val overrides: Map<String, Policy> = emptyMap(),
    val enabled: Boolean = true,
)

class AppsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = LockedAppsRepository(app)
    private val pol = PolicyRepository(app)
    private val all = MutableStateFlow<List<AppInfo>?>(null)
    private val query = MutableStateFlow("")

    val state: StateFlow<AppsUiState> =
        combine(all, query, repo.lockedPackages, combine(pol.global, pol.overrides, pol.enabled) { g, o, e -> Triple(g, o, e) }) { list, q, locked, p ->
            AppsUiState(list == null, q, filterApps(list.orEmpty(), q), locked, locked.size, p.first, p.second, p.third)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppsUiState())

    init { viewModelScope.launch { all.value = InstalledApps.load(getApplication()) } }

    fun setGlobal(p: Policy) { viewModelScope.launch { pol.setGlobal(p); LockRuntime.machine?.resetSessions() } }
    fun setOverride(pkg: String, p: Policy?) { viewModelScope.launch { pol.setOverride(pkg, p); LockRuntime.machine?.resetSessions() } }
    fun enable() { viewModelScope.launch { pol.setEnabled(true) } }
    fun setQuery(q: String) { query.value = q }
    fun toggle(pkg: String, locked: Boolean) { viewModelScope.launch { repo.setLocked(listOf(pkg), locked) } }
    /** Yalnızca şu an süzülmüş (görünen) uygulamaları etkiler. */
    fun setAllVisible(locked: Boolean) {
        val pkgs = state.value.apps.map { it.packageName }
        viewModelScope.launch { repo.setLocked(pkgs, locked) }
    }
}
