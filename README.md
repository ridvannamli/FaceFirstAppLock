# FaceFirstAppLock
Android uygulama kilidi: önce yüz tanıma, sonra parmak izi, sonra PIN; PIN unutulursa e-posta ile sıfırlama. Türkçe, çevrimdışı temel işlev.
- Durum: `07_DURUM.md`, görevler: `TASKS.md`
- Kullanım: `docs/KULLANIM_KILAVUZU.md`; cihaz testleri: `docs/DEVICE_TEST_GUIDE.md`
- Sınırlar: `docs/LIMITATIONS.md`; güvenlik: `docs/SECURITY.md`; uyumluluk: `docs/ANDROID_COMPAT.md`
- Kurtarma sunucusu: `server/`, kurulum `docs/RECOVERY_SETUP.md`; release: `docs/RELEASE.md`
- Derleme: `ANDROID_HOME=<sdk> ./gradlew testDebugUnitTest assembleDebug` (SDK kurulumu: `docs/ENV_SETUP.md`)
