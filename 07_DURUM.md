# Proje Durumu
## Proje
- FaceFirstAppLock: uygulama içi yüz tanıma > PIN (parmak izi yok), e-posta ile PIN sıfırlama yapan Android uygulama kilidi.
## Mevcut aşama
- Kullanıcı: APK denemesi (docs/DEVICE_TEST_GUIDE.md), sonuçlara göre hata düzeltme; Cloudflare+Brevo kurulumu.
## Tamamlananlar
- F0-F10 tamamlandı (kod+belge); cihaz doğrulaması yok.
## Son doğrulama
- Android: testDebugUnitTest assembleDebug lintDebug BAŞARILI (62 birim testi). Sunucu 12/12. Yüz tanıma cihazda DOĞRULANMADI.
## Bilinen sorunlar
- BEKLEYEN: kurtarma servisi hesapları (Cloudflare+Brevo, docs/RECOVERY_SETUP.md) ve gerçek cihaz testleri (liste, servis, yüz, parmak izi, PIN, kurtarma). APK teslim yolu belirlenmedi.
## KALDIĞIMIZ YERİ (2026-11-16)
- 0.2.0: UYGULAMA İÇİ YÜZ TANIMA eklendi (kullanıcı kararı: her telefonda aynı, yalnızca yüz + PIN; sistem biyometrisi/parmak izi kaldırıldı). Ayrıntı: docs/DECISIONS.md #10, docs/THIRD_PARTY.md.
- Doğrulanan: 62 birim testi, derleme, lint; tanıma modeli masaüstünde LFW yüz çiftleriyle (hizalamasız: aynı kişi ort. 0.51, farklı 0.04), canlılık modeli gerçek fotoğraflarda sınıf dizini/ön işleme sınandı.
- Kullanıcı (A53): 0.2.0 yüz tanıma çalışıyor, "hızlı ve güzel". Kalan: Y3-Y10 (fotoğraf/video saldırısı, başka kişi, deneme sınırı vb.).
- DOĞRULANMADI: eşikler (0.60/0.70), fotoğraf/video saldırısı, pil. Kullanıcı testi: docs/DEVICE_TEST_GUIDE.md Y1-Y10.
- Daha önce: T1 uygulama kilidi A53'te başarılı (erişilebilirlik + "Kısıtlı ayara izin ver").
- Sonraki: cihaz sonuçlarına göre eşik kalibrasyonu; uygulama bazlı seviye (yalnız PIN / yüz+PIN) henüz YOK; PIN/ekran/yeniden başlatma testleri (T4, T7-T10).
- Açık: e-posta kurtarma hesapları (Cloudflare+Brevo), release imza anahtarı, model eğitim verisi lisans kontrolü (Play öncesi).
## Sonraki onaylı aşama
- Kullanıcı: APK denemesi (docs/DEVICE_TEST_GUIDE.md), sonuçlara göre hata düzeltme; Cloudflare+Brevo kurulumu.
## Son güncelleme
2026-11-16: 0.2.0 uygulama içi yüz tanıma (canlılık, deneme sınırı, periyodik PIN).
