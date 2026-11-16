# Google ile PIN sıfırlama kurulumu (tek seferlik, ücretsiz)
Gerekenler: paket adı `com.facefirst.applock` ve APK'yı imzalayan anahtarın SHA-1 parmak izi.
Test (debug) anahtarı SHA-1: `F7:49:A2:5D:57:13:AC:CD:5B:AE:6B:B1:27:3C:B9:5B:BF:DD:78:2C` (anahtar değişirse yeniden kaydedilmeli; yayın anahtarı için ayrı bir Android istemcisi eklenir).
1. https://console.cloud.google.com → yeni proje oluştur (ör. "FaceFirst Kilit").
2. "Google Auth Platform" (OAuth izin ekranı) → Başla: uygulama adı, destek e-postası, kitle "Harici" → kaydet. Test aşamasında "Test kullanıcıları"na kendi Gmail adresini ekle.
3. İstemciler → İstemci oluştur → tür **Android**: paket adı `com.facefirst.applock`, SHA-1 yukarıdaki değer → oluştur.
4. İstemciler → İstemci oluştur → tür **Web uygulaması** (ad serbest) → oluştur. Çıkan **İstemci kimliği**ni (`...apps.googleusercontent.com`) kopyala. Bu değer gizli değildir. "İstemci gizli anahtarı"nı KULLANMIYORUZ, kimseyle paylaşma.
5. Web istemci kimliğini `gradle.properties` içine `googleClientId=<kimlik>` olarak yaz (ya da derlemede `-PgoogleClientId=<kimlik>`), APK'yı yeniden derle.
Akış: docs/SECURITY.md "Google ile PIN sıfırlama".
