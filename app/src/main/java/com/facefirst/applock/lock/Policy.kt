package com.facefirst.applock.lock

/** Doğrulama sonrası güven süresi politikası. ms: >0 çıkıştan sonra süre, 0 her girişte, -1 ekran kapanana dek. */
enum class Policy(val key: String, val ms: Long) {
    EVERY_ENTRY("every", 0), SCREEN_OFF("screen_off", -1),
    T30("t30", 30_000), T60("t60", 60_000), T300("t300", 300_000), T600("t600", 600_000);

    companion object {
        fun from(key: String?) = entries.firstOrNull { it.key == key } ?: EVERY_ENTRY
    }
}
