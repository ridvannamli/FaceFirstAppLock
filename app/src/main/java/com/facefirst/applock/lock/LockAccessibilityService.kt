package com.facefirst.applock.lock

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.facefirst.applock.BuildConfig
import com.facefirst.applock.data.LockedAppsRepository
import com.facefirst.applock.data.PinRepository
import com.facefirst.applock.data.PolicyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Yalnızca pencere değişimi olaylarının paket adını işler; ekran metni/içerik okunmaz
 * (canRetrieveWindowContent=false). Polling yok, olay tabanlı.
 */
class LockAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile private var locked: Set<String> = emptySet()
    @Volatile private var ime: Set<String> = emptySet()
    @Volatile private var globalPolicy: Policy = Policy.EVERY_ENTRY
    @Volatile private var overrides: Map<String, Policy> = emptyMap()

    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { LockRuntime.machine?.onScreenOff() }
    }

    override fun onServiceConnected() {
        refreshIme()
        LockRuntime.machine = LockStateMachine(
            ignored = { ime + setOf(packageName, "com.android.systemui") },
            lockedProvider = { locked },
            policyOf = { overrides[it] ?: globalPolicy },
        )
        LockRuntime.serviceConnected = true
        if (BuildConfig.DEBUG) LockRuntime.diag.value = Diag(connected = true)
        scope.launch {
            val pol = PolicyRepository(applicationContext)
            launch { pol.global.collect { globalPolicy = it } }
            launch { pol.overrides.collect { overrides = it } }
            combine(
                LockedAppsRepository(applicationContext).lockedPackages,
                PinRepository.create(applicationContext).hasPin,
                pol.enabled,
            ) { apps, hasPin, on -> if (hasPin && on) apps else emptySet() }.collect { locked = it }
        }
        registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val action = LockRuntime.machine?.onWindowChanged(pkg, System.currentTimeMillis())
        var decision = when {
            action is LockAction.ShowLock -> "kilit ekranı açılıyor"
            pkg == packageName || pkg == "com.android.systemui" || pkg in ime -> "yok sayıldı (sistem/klavye/kendi uygulama)"
            pkg !in locked -> "kilitli listede değil"
            else -> "kilitli ama doğrulanmış/bekleme"
        }
        if (action is LockAction.ShowLock) {
            try {
                startActivity(
                    Intent(this, UnlockActivity::class.java)
                        .putExtra(UnlockActivity.EXTRA_PACKAGE, action.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                )
            } catch (e: Exception) { decision = "kilit ekranı AÇILAMADI: ${e.javaClass.simpleName}" }
        }
        if (BuildConfig.DEBUG) {
            val d = LockRuntime.diag.value
            LockRuntime.diag.value = d.copy(connected = true, events = d.events + 1, lastPkg = pkg, decision = decision, lockedCount = locked.size)
        }
    }

    private fun refreshIme() {
        ime = getSystemService(InputMethodManager::class.java)
            ?.enabledInputMethodList?.map { it.packageName }?.toSet().orEmpty()
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        LockRuntime.serviceConnected = false
        if (BuildConfig.DEBUG) LockRuntime.diag.value = Diag()
        LockRuntime.machine = null
        runCatching { unregisterReceiver(screenOff) }
        scope.cancel()
        return super.onUnbind(intent)
    }
}
