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
) {
    private var foreground: String? = null
    private val unlocked = HashSet<String>()
    private var lastShownPkg: String? = null
    private var lastShownAt = 0L

    /** Ön plana yeni bir pencere geldi. */
    @Synchronized
    fun onWindowChanged(pkg: String, now: Long): LockAction {
        if (pkg in ignored()) return LockAction.None
        if (pkg != foreground) {
            // Uygulamadan çıkıldı: önceki uygulamanın doğrulaması biter (her girişte doğrulama).
            foreground?.let { unlocked.remove(it) }
            foreground = pkg
            lastShownPkg = null
        }
        if (pkg !in lockedProvider() || pkg in unlocked) return LockAction.None
        if (lastShownPkg == pkg && now - lastShownAt < debounceMs) return LockAction.None
        lastShownPkg = pkg
        lastShownAt = now
        return LockAction.ShowLock(pkg)
    }

    @Synchronized fun onUnlocked(pkg: String) { unlocked.add(pkg); lastShownPkg = null }

    /** Ekran kapandı: tüm doğrulamalar sıfırlanır. */
    @Synchronized fun onScreenOff() { unlocked.clear(); foreground = null; lastShownPkg = null }
}

/** Servis ve kilit ekranı arasındaki süreç içi bağ. */
object LockRuntime {
    @Volatile var machine: LockStateMachine? = null
    @Volatile var serviceConnected: Boolean = false
}
