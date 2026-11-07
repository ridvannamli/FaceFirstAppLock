# Proje Durumu
## Proje
- FaceFirstAppLock: yüz > parmak izi > PIN, e-posta ile PIN sıfırlama yapan Android uygulama kilidi.
## Mevcut aşama
- FAZ 4 kod tamam (BiometricPrompt + durum göstergesi; yedek ekran hâlâ geçici); sıradaki: FAZ 5 PIN.
## Tamamlananlar
- F0-F4: araştırma, iskelet, liste, servis+durum makinesi, biyometrik doğrulama (BIOMETRIC_WEAK kabul, negatif düğme "PIN kullan").
## Son doğrulama
- testDebugUnitTest assembleDebug lintDebug BAŞARILI; 21/21 birim testi. Biyometrik istem cihazda denenmedi.
## Bilinen sorunlar
- Gerçek cihaz testi bekleniyor: liste, servis (Takvim), yüz, parmak izi, izin akışı. APK teslim yolu belirlenmedi (public repo: dış yayın için kullanıcı kararı).
## Sonraki onaylı aşama
- FAZ 5 (otomatik ilerleme onaylı).
## Son güncelleme
2026-11-07: FAZ 4 biyometrik doğrulama eklendi.
