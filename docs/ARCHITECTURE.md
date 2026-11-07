# Mimari
Tek modül `app`: ui (Compose) → viewmodel → repository → DataStore/Keystore. `LockAccessibilityService` paket değişimini yakalar → `LockStateMachine` (debounce, güven süresi, döngü önleme) → `UnlockActivity` (BiometricPrompt/PIN). Sunucu: kurtarma OTP servisi (bkz. DECISIONS #6). Çevrimdışı: kilit, biyometri, mevcut PIN ağsız çalışır.
