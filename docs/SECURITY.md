# Güvenlik
Biyometrik veri/şablon saklanmaz. PIN: salt + PBKDF2, Keystore ile sarma, artan bekleme, loglanmaz. Düşük entropili (4-6 hane) PIN çevrimdışı kırılabilir → Keystore bağlama + deneme sınırı. Erişilebilirlik: yalnızca paket adı işlenir, ekran metni okunmaz. APK'da gizli anahtar yok. Kurtarma: sunucu tarafı doğrulama, tek kullanımlık kod, 5 dk, sınırlı deneme.
