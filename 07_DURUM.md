# Proje Durumu
## Proje
- FaceFirstAppLock: yüz > parmak izi > PIN, e-posta ile PIN sıfırlama yapan Android uygulama kilidi.
## Mevcut aşama
- Kullanıcı: APK denemesi (docs/DEVICE_TEST_GUIDE.md), sonuçlara göre hata düzeltme; Cloudflare+Brevo kurulumu.
## Tamamlananlar
- F0-F10 tamamlandı (kod+belge); cihaz doğrulaması yok.
## Son doğrulama
- Android: testDebugUnitTest assembleDebug lintDebug assembleRelease BAŞARILI (40 birim testi). Sunucu 12/12. Cihaz/canlı e-posta/R8 çalışma zamanı DOĞRULANMADI.
## Bilinen sorunlar
- BEKLEYEN: kurtarma servisi hesapları (Cloudflare+Brevo, docs/RECOVERY_SETUP.md) ve gerçek cihaz testleri (liste, servis, yüz, parmak izi, PIN, kurtarma). APK teslim yolu belirlenmedi.
## KALDIĞIMIZ YERİ (kullanıcı ertelendi, 2026-11-08)
- Sürüm 0.1.3 (debug) telefonda (Samsung A53 5G) kurulu. KÖK NEDEN BULUNDU: Adidas kilitlenmiyor çünkü erişilebilirlik izni verilmedi (TEŞHİS: servis BAĞLI DEĞİL).
- Kullanıcı sırada yapacak (ertelendi): 1) "Kullanılmayan uygulamayı yönet" kapat, 2) Ayarlar>Erişilebilirlik>Yüklü uygulamalar>FaceFirst Kilit aç (gerekirse ⋮ "Kısıtlı ayara izin ver"), 3) FaceFirst'i aç, "Koruma açık/servis bağlı" gör, Adidas'ı aç, TEŞHİS satırını bildir.
- Sonuç gelince: T1 (Takvim/Adidas kilidi), sonra kilit ekranı gecikmesi, PIN, yeniden başlatma, izin iptali testleri; "TEŞHİS: kilit ekranı açılıyor ama görünmüyor" çıkarsa arka plandan ekran açma (BAL) sorununa bak.
- Kararlar: yalnız sistem yüz tanıması, parmak izi ayrıca kapatılamaz (Android sınırı, kullanıcı bilgilendirildi); yüz tanınmazsa PIN (2 hata sonrası otomatik); uygulama içi kamera tanıma YOK; genel süre varsayılan "Her girişte".
- Açık: e-posta kurtarma servisi hesapları (Cloudflare+Brevo) kurulmadı; release imza anahtarı yok.
## Sonraki onaylı aşama
- Kullanıcı: APK denemesi (docs/DEVICE_TEST_GUIDE.md), sonuçlara göre hata düzeltme; Cloudflare+Brevo kurulumu.
## Son güncelleme
2026-11-07: FAZ 10 kılavuzlar, release talimatı, test APK teslimi.
