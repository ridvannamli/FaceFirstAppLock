# Android sürüm uyumluluğu ve biyometrik uyumluluk (belge tabanlı; cihazda DOĞRULANMADI)
| API | Sürüm | Not |
|---|---|---|
| 28 | Android 9 | minSdk. BiometricPrompt yalnızca parmak izi (yüz entegrasyonu yok) → yüz tanıma burada kullanılamaz, PIN yedeği. |
| 29 | Android 10 | BiometricPrompt'a yüz entegrasyonu eklendi (cihaz/üretici desteğine bağlı). |
| 30+ | Android 11+ | BiometricManager.Authenticators sınıfları; BIOMETRIC_WEAK yüz (Sınıf 2) dahil kabul edilir. |
| 33 | Android 13 | APK ile kurulu erişilebilirlik servisi için "Kısıtlı ayara izin ver" gerekir. |
| 35/36 | Android 15/16 | Kenardan kenara zorunlu: arayüz safeDrawingPadding ile uyumlu. Play hedefi API 36 (31 Ağu 2026). |
## Biyometrik cihaz sınıfları
- Yüz+parmak izi, yalnız parmak izi, yalnız yüz, biyometrisiz, yüzü üçüncü tarafa açmayan cihazlar için durum ana ekranda gösterilir (güçlü / zayıf / kayıtlı yok / desteklenmiyor).
- Sistem yüz/parmak sırasını seçtirmez; sistemin sunduğu biyometrik açılır. Yüz yoksa özel kamera yüzü TANIMA YAPILMAZ, PIN'e düşülür.
- Gerçek cihaz sonuçları docs/TEST_PLAN.md'de (şu an: bekleniyor).
