# FaceFirstAppLock
Android uygulama kilidi: uygulama içi yüz tanıma, olmazsa PIN; PIN unutulursa ekran kilidi + yüz ya da kurtarma koduyla sıfırlama. Türkçe, çevrimdışı temel işlev.
- Durum: `07_DURUM.md`, görevler: `TASKS.md`
- Kullanım: `docs/KULLANIM_KILAVUZU.md`; cihaz testleri: `docs/DEVICE_TEST_GUIDE.md`
- Sınırlar: `docs/LIMITATIONS.md`; güvenlik: `docs/SECURITY.md`; uyumluluk: `docs/ANDROID_COMPAT.md`
- Release: `docs/RELEASE.md`
- Derleme: `ANDROID_HOME=<sdk> ./gradlew testDebugUnitTest assembleDebug` (SDK kurulumu: `docs/ENV_SETUP.md`)
