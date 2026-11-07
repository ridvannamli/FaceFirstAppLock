# Güvenlik
Biyometrik veri/şablon saklanmaz. PIN: salt + PBKDF2, Keystore ile sarma, artan bekleme, loglanmaz. Düşük entropili (4-6 hane) PIN çevrimdışı kırılabilir → Keystore bağlama + deneme sınırı. Erişilebilirlik: yalnızca paket adı işlenir, ekran metni okunmaz. APK'da gizli anahtar yok. Kurtarma: sunucu tarafı doğrulama, tek kullanımlık kod, 5 dk, sınırlı deneme.

## FAZ 8 sertleştirmeleri
- Bekleme: tek yönlü süre sayacı + açılış sayısı (saat oynamasına dayanıklı), testli.
- FLAG_SECURE: PIN/doğrulama ekranları ekran kaydı ve son uygulamalar önizlemesinde görünmez.
- Kurtarma istemcisi yalnızca https adresini kabul eder; istekler Keystore ECDSA anahtarıyla imzalıdır.
- allowBackup=false: PIN hash'i/anahtarlar yedeğe girmez. Keystore anahtarları cihazdan çıkmaz.
- Kendi ayarlarımız: açılışta doğrulama, uygulamadan çıkınca yeniden doğrulama; korumayı kapatmak PIN ister.
- Erişilebilirlik: canRetrieveWindowContent=false, yalnızca typeWindowStateChanged; paket adı dışında veri okunmaz.
