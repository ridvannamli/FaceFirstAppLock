package com.facefirst.applock

import com.facefirst.applock.lock.LockAction
import com.facefirst.applock.lock.LockStateMachine
import org.junit.Assert.assertEquals
import org.junit.Test

class LockStateMachineTest {
    private val locked = setOf("cal", "wa")
    private fun sm() = LockStateMachine(
        ignored = { setOf("me", "systemui", "keyboard") },
        lockedProvider = { locked },
    )
    private val show = { p: String -> LockAction.ShowLock(p) }

    @Test fun lockedAppShowsLock() = assertEquals(show("cal"), sm().onWindowChanged("cal", 0))
    @Test fun unlockedAppNotLockedAgain() {
        val m = sm(); m.onWindowChanged("cal", 0); m.onUnlocked("cal")
        assertEquals(LockAction.None, m.onWindowChanged("cal", 5_000))
    }
    @Test fun nonLockedAppIgnored() = assertEquals(LockAction.None, sm().onWindowChanged("maps", 0))
    @Test fun ownAndSystemPackagesIgnored() {
        val m = sm()
        assertEquals(LockAction.None, m.onWindowChanged("me", 0))
        assertEquals(LockAction.None, m.onWindowChanged("systemui", 0))
        assertEquals(LockAction.None, m.onWindowChanged("keyboard", 0))
    }
    @Test fun leavingAppEndsUnlock() {
        val m = sm(); m.onWindowChanged("cal", 0); m.onUnlocked("cal")
        m.onWindowChanged("launcher", 3_000)
        assertEquals(show("cal"), m.onWindowChanged("cal", 6_000))
    }
    @Test fun ownLockScreenDoesNotEndUnlock() {
        val m = sm(); m.onWindowChanged("cal", 0); m.onUnlocked("cal")
        m.onWindowChanged("me", 100); m.onWindowChanged("systemui", 200)
        assertEquals(LockAction.None, m.onWindowChanged("cal", 300))
    }
    @Test fun debouncePreventsRepeatedLockScreens() {
        val m = sm()
        assertEquals(show("cal"), m.onWindowChanged("cal", 0))
        assertEquals(LockAction.None, m.onWindowChanged("cal", 300))
        assertEquals(show("cal"), m.onWindowChanged("cal", 1_500))
    }
    @Test fun screenOffResetsUnlock() {
        val m = sm(); m.onWindowChanged("cal", 0); m.onUnlocked("cal"); m.onScreenOff()
        assertEquals(show("cal"), m.onWindowChanged("cal", 9_000))
    }
    @Test fun switchBetweenLockedApps() {
        val m = sm(); m.onWindowChanged("cal", 0); m.onUnlocked("cal")
        assertEquals(show("wa"), m.onWindowChanged("wa", 2_000))
    }
}

class LockPolicyTest {
    private fun sm(p: com.facefirst.applock.lock.Policy, per: Map<String, com.facefirst.applock.lock.Policy> = emptyMap()) = LockStateMachine(
        ignored = { setOf("me") }, lockedProvider = { setOf("cal", "wa") }, policyOf = { per[it] ?: p },
    )
    private val show = { p: String -> LockAction.ShowLock(p) }

    @Test fun trustWindowAllowsReturnWithinTime() {
        val m = sm(com.facefirst.applock.lock.Policy.T30)
        m.onWindowChanged("cal", 0); m.onUnlocked("cal"); m.onWindowChanged("launcher", 1_000)
        assertEquals(LockAction.None, m.onWindowChanged("cal", 20_000))
    }
    @Test fun trustWindowExpires() {
        val m = sm(com.facefirst.applock.lock.Policy.T30)
        m.onWindowChanged("cal", 0); m.onUnlocked("cal"); m.onWindowChanged("launcher", 1_000)
        assertEquals(show("cal"), m.onWindowChanged("cal", 40_000))
    }
    @Test fun screenOffPolicyKeepsUnlockUntilScreenOff() {
        val m = sm(com.facefirst.applock.lock.Policy.SCREEN_OFF)
        m.onWindowChanged("cal", 0); m.onUnlocked("cal"); m.onWindowChanged("launcher", 1_000)
        assertEquals(LockAction.None, m.onWindowChanged("cal", 3_600_000))
        m.onWindowChanged("launcher", 3_700_000); m.onScreenOff()
        assertEquals(show("cal"), m.onWindowChanged("cal", 3_800_000))
    }
    @Test fun perAppOverrideBeatsGlobal() {
        val m = sm(com.facefirst.applock.lock.Policy.T600, mapOf("wa" to com.facefirst.applock.lock.Policy.EVERY_ENTRY))
        m.onWindowChanged("wa", 0); m.onUnlocked("wa"); m.onWindowChanged("launcher", 1_000)
        assertEquals(show("wa"), m.onWindowChanged("wa", 2_000))
        m.onWindowChanged("cal", 3_000); m.onUnlocked("cal"); m.onWindowChanged("launcher", 4_000)
        assertEquals(LockAction.None, m.onWindowChanged("cal", 5_000))
    }
    @Test fun trustedReentryContinuesSessionThenRestartsWindow() {
        val m = sm(com.facefirst.applock.lock.Policy.T30)
        m.onWindowChanged("cal", 0); m.onUnlocked("cal"); m.onWindowChanged("launcher", 1_000)
        m.onWindowChanged("cal", 10_000)                       // süre içinde döndü
        m.onWindowChanged("launcher", 100_000)                 // yeniden çıktı: yeni 30 sn penceresi
        assertEquals(LockAction.None, m.onWindowChanged("cal", 120_000))
        m.onWindowChanged("launcher", 121_000)
        assertEquals(show("cal"), m.onWindowChanged("cal", 200_000))
    }
    @Test fun resetSessionsRevokesTrust() {
        val m = sm(com.facefirst.applock.lock.Policy.SCREEN_OFF)
        m.onWindowChanged("cal", 0); m.onUnlocked("cal"); m.onWindowChanged("launcher", 1_000)
        m.resetSessions()
        assertEquals(show("cal"), m.onWindowChanged("cal", 2_000))
    }
}
