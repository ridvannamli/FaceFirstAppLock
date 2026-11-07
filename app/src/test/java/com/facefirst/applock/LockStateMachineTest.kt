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
