# Güvenlik
Biyometrik veri/şablon saklanmaz. PIN: salt + PBKDF2, Keystore ile sarma, artan bekleme, loglanmaz. Düşük entropili (4-6 hane) PIN çevrimdışı kırılabilir → Keystore bağlama + deneme sınırı. Erişilebilirlik: yalnızca paket adı işlenir, ekran metni okunmaz. APK'da gizli anahtar yok. Kurtarma: sunucu tarafı doğrulama, tek kullanımlık kod, 5 dk, sınırlı deneme.

## FAZ 8 sertleştirmeleri
- Bekleme: tek yönlü süre sayacı + açılış sayısı (saat oynamasına dayanıklı), testli.
- FLAG_SECURE: PIN/doğrulama ekranları ekran kaydı ve son uygulamalar önizlemesinde görünmez.
- Kurtarma istemcisi yalnızca https adresini kabul eder; istekler Keystore ECDSA anahtarıyla imzalıdır.
- allowBackup=false: PIN hash'i/anahtarlar yedeğe girmez. Keystore anahtarları cihazdan çıkmaz.
- Kendi ayarlarımız: açılışta doğrulama, uygulamadan çıkınca yeniden doğrulama; korumayı kapatmak PIN ister.
- Erişilebilirlik: canRetrieveWindowContent=false, yalnızca typeWindowStateChanged; paket adı dışında veri okunmaz.

## Uygulama içi yüz tanıma (0.2.0)
- Kareler yalnızca bellekte işlenir; kaydedilmez, uygulama kodu tarafından ağa gönderilmez (modeller cihazda çalışır).
- İnternet izni VAR: e-posta kurtarma için gerekli; ayrıca Google ML Kit kütüphanesi kendi kullanım/performans ölçümlerini Google'a gönderebilir (Google belgelerine göre görüntü içermez; bu trafik tarafımızca incelenmedi). Play veri güvenliği formunda beyan edilmeli.
- Şablonlar (5 adet, 128 boyut) Keystore AES-256-GCM ile şifreli; anahtar cihazdan çıkmaz; yedekleme kapalı.
- Kabul koşulu: tek yüz, yeterli yakınlık ve ışık, düz bakış, iki göz açık, canlılık ≥ eşik, benzerlik ≥ eşik; art arda 3 kare.
- Deneme sınırı: her yüz denemesi başlarken sayılır (yarıda bırakma/yeniden açma sınırsız deneme vermez); 3 başarısız denemeden sonra yalnızca PIN. PIN girişi ve başarılı yüz sayacı sıfırlar.
- PIN zorunluluğu: cihaz yeniden başladıktan sonra ve son PIN girişinden 48 saat sonra yüz kabul edilmez.
- Yüz kaydı/değişimi PIN ister. Yüksek güvenlik modunda rastgele hareket + yüz takibi.
- Bilinen açıklar: docs/LIMITATIONS.md.

## Google ile PIN sıfırlama (0.3.0)
- Bağlama: Google girişi → hesabın kimliği (sub) ve maskeli e-posta yalnızca cihazda saklanır. Hesabı değiştirmek PIN ister.
- Sıfırlama sırası: (1) telefonun ekran kilidi (Android 11+: yalnızca PIN/desen/şifre), (2) aynı Google hesabıyla giriş, (3) yüz kayıtlıysa canlılık + rastgele hareketli yüz doğrulaması → yeni PIN; önceki kilit açma oturumları sıfırlanır.
- Belirteç kontrolleri: hedef kitle (istemci kimliği), yayıncı, süre, tek kullanımlık nonce (birim testli). İmza cihazda ayrıca doğrulanmaz; belirteç Google Play Hizmetleri'nden doğrudan alınır.
- Neden üç etken: Google hesabı telefonda zaten açıktır; tek başına Google girişi, telefonu eline alan herkese PIN'i sıfırlatırdı.
