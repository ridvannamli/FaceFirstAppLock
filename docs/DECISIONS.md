# Kararlar (FAZ 0)
1. minSdk 28. Yüz ile BiometricPrompt entegrasyonu Android 10+; API 28'de yalnızca parmak izi. compile/targetSdk: Play 2026 hedefi API 36 (31 Ağu 2026); FAZ 1'de kurulu kararlı sürümle doğrula.
2. Yüz/parmak sırası standart API ile seçtirilemez; sistem biyometrik seçeneklerini sunar. Sistem yüzü üçüncü tarafa açmıyorsa özel kamera yüzü yapılmaz, PIN'e düşülür.
3. Güç sınıfı: Class 3 (STRONG) varsayılan; Class 2 (WEAK) yalnızca kilit açma için, kullanıcıya gösterilerek. WEAK, işlem-bağlı Keystore anahtarıyla çalışmaz → PIN koruması Keystore'a biyometriden bağımsız bağlanır.
4. Uygulama geçişi: AccessibilityService (olay tabanlı; yalnızca TYPE_WINDOW_STATE_CHANGED, paket adı). UsageStats alternatifi polling gerektirir → yedek/araştırma.
5. Play: erişilebilirlik API'si erişilebilirlik aracı olmayan uygulamalarda ayrı belirgin açıklama + onay + Play Console beyanı ister; app lock kategorisi netleşmedi, belge FAZ 8'de yeniden okunacak. Sideload APK etkilenmez.
6. E-posta kurtarma (önerilen, aylık ücretsiz hedef): Cloudflare Worker + KV (sunucu tarafı OTP: hash, 5 dk TTL, deneme/gönderim sınırı) + ücretsiz e-posta API'si (anahtar Worker gizli değişkeninde, APK'da değil) + cihaz-bağlı Keystore anahtarı (kurulumda açık anahtar kaydı, sıfırlama imza ister). Firebase/Supabase Auth hazır e-posta akışı veriyor ama yerel PIN sıfırlamayı tek başına çözmez; ücretsiz kotalar blog kaynaklıdır, FAZ 6'da resmi sayfalardan doğrulanacak, Worker/e-posta kotaları da öyle.
7. Mimari: Kotlin, Compose M3, MVVM, DataStore (Room yalnız gerekirse), tek modül.
