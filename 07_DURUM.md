# Proje Durumu
## Proje
- FaceFirstAppLock: uygulama içi yüz tanıma > PIN (parmak izi yok), e-posta ile PIN sıfırlama yapan Android uygulama kilidi.
## Mevcut aşama
- Kullanıcı: APK denemesi (docs/DEVICE_TEST_GUIDE.md), sonuçlara göre hata düzeltme; Cloudflare+Brevo kurulumu.
## Tamamlananlar
- F0-F10 tamamlandı (kod+belge); cihaz doğrulaması yok.
## Son doğrulama
- Android: testDebugUnitTest assembleDebug lintDebug BAŞARILI (69 birim testi). PIN sıfırlama ve kaldırma koruması cihazda DOĞRULANMADI.
## Bilinen sorunlar
- BEKLEYEN: cihaz testleri Y3-Y10, R1-R5, K1-K6.
## KALDIĞIMIZ YERİ (2026-11-16)
- 0.4.0: KALDIRMA KORUMASI eklendi (isteğe bağlı anahtar): Ayarlar + Play Store + sistem kaldırma/yükleme penceresi kilitlenir (docs/DECISIONS.md #13). 69 birim testi; cihazda DENENMEDİ → testler K1-K6. Samsung kaldırma penceresinin paket adı doğrulanmadı.
- 0.3.1: PIN sıfırlama SUNUCUSUZ: (A) ekran kilidi + yüz, (B) kurtarma kodu. Google ile giriş ve e-posta kodu kaldırıldı (kullanıcı: şimdilik kişisel kullanım, ileride çok kullanıcı → o zaman e-posta kodu geri eklenebilir). Google Cloud/Cloudflare/Brevo kurulumu GEREKMİYOR.
- Doğrulanan: 63 birim testi, derleme, lint. Sıfırlama akışı cihazda DENENMEDİ → testler R1-R5 (docs/DEVICE_TEST_GUIDE.md).
- 0.2.0 yüz tanıma A53'te çalışıyor ("hızlı ve güzel"); Y3-Y10 saldırı/sınır testleri ve eşik kalibrasyonu bekliyor.
- Test imzası: debug anahtarı bu geçici ortamda; kullanıcıya yedek gönderildi (facefirst-test-imza.keystore). Ortam sıfırlanırsa geri istenmeli, yoksa üstüne güncelleme yapılamaz.
- Henüz yok: uygulama bazlı güvenlik seviyesi; kalıcı (release) imza anahtarı; model eğitim verisi lisans kontrolü (Play öncesi); çok kullanıcı için e-posta kurtarma.
## Sonraki onaylı aşama
- Kullanıcı: APK denemesi (docs/DEVICE_TEST_GUIDE.md), sonuçlara göre hata düzeltme; Cloudflare+Brevo kurulumu.
## Son güncelleme
2026-11-16: 0.4.0 kaldırma koruması.
