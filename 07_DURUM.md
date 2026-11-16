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
## KALDIĞIMIZ YERİ (2026-11-16)
- T1 BAŞARILI (kullanıcı, Samsung A53 5G): "Kısıtlı ayara izin ver" + erişilebilirlik izni sonrası Adidas kilidi çalışıyor. Telefondaki sürüm 0.1.3 veya 0.1.4 (kullanıcı belirtmedi).
- Kullanıcı isteği: yalnız yüz, parmak izi olmasın. Uygulamadan yapılamaz (Android sınırı). Önerilen: Samsung ayarı "Tercih edilen biyometri = Yüz" (kullanıcı deneyecek; menünün A53'te varlığı doğrulanmadı).
- Sıradaki testler: kilit ekranı gecikmesi, yüz, PIN (T4), ekran kapat/aç (T7), yeniden başlatma (T8), izin iptali (T9), Ayarlar kilidi (T10).
- Açık: e-posta kurtarma hesapları (Cloudflare+Brevo), release imza anahtarı.
## Sonraki onaylı aşama
- Kullanıcı: APK denemesi (docs/DEVICE_TEST_GUIDE.md), sonuçlara göre hata düzeltme; Cloudflare+Brevo kurulumu.
## Son güncelleme
2026-11-16: T1 cihazda başarılı; minSdk 29, sürüm 0.1.4.
