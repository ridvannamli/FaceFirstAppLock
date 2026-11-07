# FaceFirstAppLock
Kurallar: `AGENTS.md` ve `rules/` (kaynak: github.com/ridvannamli/rules). Oturum başında sırayla oku: `AGENTS.md`, `07_DURUM.md`, `TASKS.md`.
Proje yetkisi (kullanıcı, 2026-11-07, güncel): FAZ 0-10 arası geçişte onay İSTENMEZ, kontrol Claude'da. Telefon testi gerektiğinde kullanıcıya bildirim gönder (PushNotification) ve bağımsız işlere devam et; test sonucu gelene dek ilgili özelliği "Gerçek cihaz testi bekleniyor" say. Dur: aynı hatada 2 başarısız düzeltme, geri dönüşsüz/dış sistem işlemi, hesap/anahtar gerektiren adım.
Talimat kaynağı: `docs/BRIEF.md`. Hassas bilgi (SMTP/API anahtarı, PIN) Git'e girmez. Dil: Türkçe.
Derleme: Android SDK /opt/android-sdk (oturum sıfırlanırsa `docs/ENV_SETUP.md`), `ANDROID_HOME=/opt/android-sdk ./gradlew testDebugUnitTest assembleDebug`.
Bildirim kuralı (kullanıcı, 2026-11-07): Faz geçişlerinde PushNotification GÖNDERME. Yalnızca: telefon testi için kullanıcı gerekiyorsa, engel/bilgi gerekiyorsa, iş uçtan uca bittiyse.
