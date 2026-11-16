# Proje Durumu
## Proje
- FaceFirstAppLock: uygulama içi yüz tanıma > PIN (parmak izi yok), e-posta ile PIN sıfırlama yapan Android uygulama kilidi.
## Mevcut aşama
- Kullanıcı: APK denemesi (docs/DEVICE_TEST_GUIDE.md), sonuçlara göre hata düzeltme; Cloudflare+Brevo kurulumu.
## Tamamlananlar
- F0-F10 tamamlandı (kod+belge); cihaz doğrulaması yok.
## Son doğrulama
- Android: testDebugUnitTest assembleDebug lintDebug BAŞARILI (67 birim testi). Google ile sıfırlama cihazda DOĞRULANMADI.
## Bilinen sorunlar
- BEKLEYEN: Google Cloud kurulumu (docs/GOOGLE_SETUP.md); cihaz testleri Y3-Y10, G1-G4.
## KALDIĞIMIZ YERİ (2026-11-16)
- 0.3.0: PIN kurtarma Google ile giriş oldu (ekran kilidi + Google hesabı + kayıtlıysa yüz); e-posta kodu ve server/ kaldırıldı. Kod derlendi, 67 birim testi; Google akışı cihazda DENENMEDİ çünkü istemci kimliği yok (bu APK'da özellik kapalı).
- BEKLEYEN (kullanıcı): docs/GOOGLE_SETUP.md adımları → web istemci kimliğini bildirmek. Sonra `gradle.properties`e googleClientId yazılıp yeni APK çıkarılacak; testler G1-G4.
- Test imzası: debug anahtarı bu geçici ortamda (SHA-1 GOOGLE_SETUP.md'de); kullanıcıya yedek olarak gönderildi. Ortam sıfırlanırsa kullanıcıdan geri istenmeli, yoksa Google girişi ve üstüne güncelleme bozulur.
- 0.2.0 yüz tanıma A53'te çalışıyor ("hızlı ve güzel"); Y3-Y10 saldırı/sınır testleri ve eşik kalibrasyonu bekliyor.
- Henüz yok: uygulama bazlı güvenlik seviyesi; kalıcı (release) imza anahtarı; model eğitim verisi lisans kontrolü (Play öncesi).
## Sonraki onaylı aşama
- Kullanıcı: APK denemesi (docs/DEVICE_TEST_GUIDE.md), sonuçlara göre hata düzeltme; Cloudflare+Brevo kurulumu.
## Son güncelleme
2026-11-16: 0.3.0 Google ile PIN sıfırlama; e-posta kodu kaldırıldı.
