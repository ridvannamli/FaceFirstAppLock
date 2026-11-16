# FaceFirstAppLock
Android uygulama kilidi: uygulama içi yüz tanıma, olmazsa PIN; PIN unutulursa Google hesabıyla sıfırlama. Türkçe, çevrimdışı temel işlev.
- Durum: `07_DURUM.md`, görevler: `TASKS.md`
- Kullanım: `docs/KULLANIM_KILAVUZU.md`; cihaz testleri: `docs/DEVICE_TEST_GUIDE.md`
- Sınırlar: `docs/LIMITATIONS.md`; güvenlik: `docs/SECURITY.md`; uyumluluk: `docs/ANDROID_COMPAT.md`
- PIN kurtarma (Google ile giriş) kurulumu: `docs/GOOGLE_SETUP.md`; release: `docs/RELEASE.md`
- Derleme: `ANDROID_HOME=<sdk> ./gradlew testDebugUnitTest assembleDebug` (SDK kurulumu: `docs/ENV_SETUP.md`)
