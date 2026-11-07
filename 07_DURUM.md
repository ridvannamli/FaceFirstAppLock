# Proje Durumu
## Proje
- FaceFirstAppLock: yüz > parmak izi > PIN, e-posta ile PIN sıfırlama yapan Android uygulama kilidi.
## Mevcut aşama
- FAZ 5 kod tamam (PIN: PBKDF2+Keystore HMAC, artan bekleme, kurulum/değiştirme/kilit ekranı); sıradaki: FAZ 6.
## Tamamlananlar
- F0-F5: araştırma, iskelet, liste, servis, biyometrik, güvenli PIN.
## Son doğrulama
- testDebugUnitTest assembleDebug lintDebug BAŞARILI; 27/27 birim testi. PIN ekranları cihazda denenmedi.
## Bilinen sorunlar
- Gerçek cihaz testi bekleniyor: liste, servis (Takvim), yüz, parmak izi, PIN kurulum/kilit/bekleme, izin akışı. APK teslim yolu belirlenmedi. Bekleme süresi duvar saatine bağlı (saat geri alınırsa atlanabilir): docs/LIMITATIONS.md.
## Sonraki onaylı aşama
- FAZ 6 (otomatik ilerleme onaylı).
## Son güncelleme
2026-11-07: FAZ 5 PIN eklendi.
