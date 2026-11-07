package com.facefirst.applock.lock

/** Karar: kilit ekranı gösterilsin mi? Saf mantık; Android'e bağımlı değil (birim test edilebilir). */
sealed interface LockAction {
    data object None : LockAction
    data class ShowLock(val packageName: String) : LockAction
}

class LockStateMachine(
    private val debounceMs: Long = 1_000,
    private val ignored: () -> Set<String>,
    private val lockedProvider: () -> Set<String>,
    private val policyOf: (String) -> Policy = { Policy.EVERY_ENTRY },
) {
    private var foreground: String? = null
    private val unlockedNow = HashSet<String>()            // şu an ön plandaki doğrulanmış uygulama
    private val validUntil = HashMap<String, Long>()       // çıkış sonrası geçerlilik (Long.MAX_VALUE = ekran kapanana dek)
    private var lastShownPkg: String? = null
    private var lastShownAt = 0L

    @Synchronized
    fun onWindowChanged(pkg: String, now: Long): LockAction {
        if (pkg in ignored()) return LockAction.None
        if (pkg != foreground) {
            foreground?.let { prev ->
                if (unlockedNow.remove(prev)) {
                    val p = policyOf(prev)
                    when {
                        p.ms < 0 -> validUntil[prev] = Long.MAX_VALUE
                        p.ms > 0 -> validUntil[prev] = now + p.ms
                    }
                }
            }
            foreground = pkg
            lastShownPkg = null
        }
        if (pkg !in lockedProvider()) return LockAction.None
        if (pkg in unlockedNow) return LockAction.None
        validUntil[pkg]?.let { until ->
            if (now < until) { validUntil.remove(pkg); unlockedNow.add(pkg); return LockAction.None }
            validUntil.remove(pkg)
        }
        if (lastShownPkg == pkg && now - lastShownAt < debounceMs) return LockAction.None
        lastShownPkg = pkg
        lastShownAt = now
        return LockAction.ShowLock(pkg)
    }

    @Synchronized fun onUnlocked(pkg: String) { unlockedNow.add(pkg); validUntil.remove(pkg); lastShownPkg = null }

    /** Ekran kapandı veya PIN sıfırlandı: tüm doğrulamalar iptal. */
    @Synchronized fun onScreenOff() { unlockedNow.clear(); validUntil.clear(); foreground = null; lastShownPkg = null }

    /** Koruma kapatıldı/yeniden açıldı vb.: oturumları sıfırla (ön planı koru). */
    @Synchronized fun resetSessions() { unlockedNow.clear(); validUntil.clear(); lastShownPkg = null }
}

/** Servis ve kilit ekranı arasındaki süreç içi bağ. */
data class Diag(
    val connected: Boolean = false,
    val events: Int = 0,
    val lastPkg: String = "-",
    val decision: String = "-",
    val lockedCount: Int = 0,
)

object LockRuntime {
    /** Yalnızca test (debug) sürümünde ekranda gösterilir; ekran içeriği değil, paket adı ve karar. */
    val diag = kotlinx.coroutines.flow.MutableStateFlow(Diag())
    @Volatile var machine: LockStateMachine? = null
    @Volatile var serviceConnected: Boolean = false
}
