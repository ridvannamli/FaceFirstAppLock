# Sınırlar
- Normal yetkilerle mutlak koruma yok; kilit ekranı hedef uygulama açıldıktan kısa süre sonra gelebilir (FAZ 9'da ölçülecek).
- Sistem arayüzü, launcher, izin yöneticisi, acil çağrı kilitlenemez.
- Yüz/parmak sırası ve yüzün üçüncü tarafa açılması cihaz/üretici bağımlı.
- Erişilebilirlik izni kaldırılırsa koruma durur; uygulama bunu bildirir.
- Üretici pil optimizasyonu servisi öldürebilir.
- E-posta kurtarma internet gerektirir.
- PIN bekleme süresi duvar saatine dayanır; kullanıcı saati geri alarak beklemeyi atlayabilir. İyileştirme: SystemClock.elapsedRealtime + yeniden başlatma kontrolü (FAZ 8).
