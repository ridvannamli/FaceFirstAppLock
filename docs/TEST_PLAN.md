# Test planı
Otomatik (birim/enstrümantasyon): PIN türetme/bekleme, durum makinesi, politika, depo. Manuel (gerçek cihaz): yüz, parmak izi, PIN, PIN'imi unuttum, e-posta kodu, yeni PIN, yanlış PIN/kod, internetsiz kilit, ekran kapat/aç, yeniden başlatma, izin iptali, Ayarlar kilidi, çoklu pencere/son uygulamalar. İlk uygulama: Takvim. Sonuçlar: başarılı/başarısız/sonra → burada kalıcı kayıt.
| Test | Sonuç |
|---|---|
| Uygulama listesi, arama, anahtar kalıcılığı (F2) | Gerçek cihaz testi bekleniyor |
| Servis tetikleme: Takvim kilidi, ekran kapat/aç, izin iptali (F3) | Gerçek cihaz testi bekleniyor |
| Yüz, parmak izi, biyometrik iptal/hata, karanlıkta yüz (F4) | Gerçek cihaz testi bekleniyor |
| PIN kurulum, değiştirme, yanlış PIN, 5 hata sonrası bekleme (F5) | Gerçek cihaz testi bekleniyor |
| E-posta doğrulama, PIN\x27imi unuttum, yanlış/süresi dolmuş kod, yeniden gönderme, yeni PIN (F6, canlı servis gerekir) | Bekliyor: hesap kurulumu + cihaz |
| Politikalar: 30 sn/1-5-10 dk, ekran kapanana dek, uygulama bazlı, koruma kapatma, kendi ayarları kilidi (F7) | Gerçek cihaz testi bekleniyor |
| Diğer tüm testler | Gerçek cihaz testi bekleniyor |
