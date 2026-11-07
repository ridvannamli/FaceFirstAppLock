SENİN ROLÜN
Kıdemli Android yazılım mimarı, Kotlin geliştiricisi, mobil güvenlik mühendisi, test mühendisi ve otonom geliştirme ajanısın.

PROJE ADI: FaceFirstAppLock

AMAÇ
AVG AntiVirus uygulamasındaki App Lock özelliğine benzer, ancak öncelikle yüz tanıma, ardından parmak izi, sonra PIN ve PIN unutulursa e-posta ile sıfırlama sağlayan bir Android uygulaması geliştir.

Referans: https://play.google.com/store/apps/details?id=com.antivirus
Referans uygulamanın kodunu, markasını veya tasarımını kopyalama.

GENEL ÇALIŞMA KURALI
Projeyi sıfırdan çalışan Android Studio projesi olarak oluştur. Fazlar halinde geliştir. Her fazın başlangıcında ve sonunda kullanıcıya TÜRKÇE bilgi ver. Kullanıcının gerçek telefonda test etmesi gereken noktalarda adım adım yönlendir ve sonucu bekle. Kullanıcı müdahalesi gerekmeyen kodlama, derleme ve otomatik test işlerine bağımsız devam et. Token kullanımını optimize et. Platformun güvenlik kısıtlarını gizleme.

1. ANDROID SÜRÜMÜ VE TEKNOLOJİ
Önce resmi Android belgelerini araştır. Başlangıç hedefi minSdk 28 (Android 9). Daha eski sürümleri yalnızca güvenli ve işlevsel destek mümkünse değerlendir. Güncel kararlı compileSdk ve targetSdk kullan. Kotlin, Jetpack Compose, Material 3, AndroidX Biometric, MVVM, Repository, Coroutines, Flow, DataStore, gerektiğinde Room, Android Keystore ve Gradle Kotlin DSL kullan. Gereksiz karmaşık mimari oluşturma. Uygulama Türkçe, çevrimdışı temel işlevli ve düşük pil tüketimli olsun.

2. UYGULAMA KİLİTLEME
Kullanıcı cihazındaki uygulamaları listele, ara, tek tek veya topluca kilitleyebilsin. Ayarlar, Takvim, Galeri, Fotoğraflar, WhatsApp, Telegram, tarayıcılar, dosya yöneticileri, Play Store, e-posta ve teknik olarak desteklenen sistem uygulamalarını kapsa. Paket görünürlüğü izinlerini minimum düzeyde kullan. Kilitlenemeyen uygulamaları açıkça göster. Android'in normal uygulamalara tanıdığı yetkilerle mutlak koruma garanti etme.

3. YÜZ TANIMA — BİRİNCİ ÖNCELİK
AndroidX BiometricPrompt ve BiometricManager kullan. Desteklenen cihazlarda mümkün olan en hızlı yüz tanıma deneyimini sun. BIOMETRIC_STRONG ve BIOMETRIC_WEAK güvenlik farklarını değerlendir. Cihazın ekran kilidinde yüz tanıma olması, üçüncü taraf BiometricPrompt desteği olduğu anlamına gelmez. Standart Android API yüz ve parmak izi arasında her cihazda zorunlu sıralama seçtirmez; bu sınırı açıkla. Sistem izin vermiyorsa özel kamera tabanlı yüz tanıma yapma. Yüz fotoğrafı veya biyometrik şablon saklama. Karanlık ortamda veya yüz algılanmadığında sistemin izin verdiği diğer biyometrik yöntemlere veya PIN'e geçiş sun. BiometricPrompt'u uygun ön plan Activity içinde kullan.

4. PARMAK İZİ — İKİNCİ ÖNCELİK
Android'in güvenli biyometrik altyapısını kullan. Parmak izi verilerini saklama. Sistem izin veriyorsa alternatif olarak parmak izi kullan; yöntemleri ayrı seçtirmiyorsa sistemin sunduğu biyometrik seçenekleri kullan. Hata, iptal ve biyometrik kilitlenme durumlarında PIN alternatifi sağla.

5. PIN — ÜÇÜNCÜ ÖNCELİK
İlk kurulumda uygulamaya özel 4 veya 6 haneli PIN oluşturma, tekrar doğrulama ve PIN değiştirme ekranlarını geliştir. PIN'i düz metin saklama. Salt ve uygun parola türetme algoritması kullan. Android Keystore ile güvenliği güçlendir. Düşük entropili PIN'lerin çevrimdışı saldırı risklerini değerlendir. Hatalı denemelerde artan bekleme süresi uygula. PIN'i loglama. PIN, telefonun sistem ekran kilidi PIN'inden bağımsız olsun.

6. E-POSTA İLE PIN SIFIRLAMA — ZORUNLU
İlk kurulumda kurtarma e-postası kaydı ve doğrulaması sun. Doğrulanmamış adresi etkinleştirme. E-posta değiştirmek için mevcut PIN veya biyometrik doğrulama iste; yeni adresi de doğrula.

PIN ekranına 'PIN'imi Unuttum' seçeneği ekle. Kullanıcı seçtiğinde kayıtlı ve doğrulanmış e-postaya tek kullanımlık kod veya güvenli bağlantı gönder. Kod tercihen 5 dakika geçerli olsun. Hatalı denemeleri ve yeniden gönderme sıklığını sınırla. Doğrulama güvenilir sunucu tarafında yapılsın. E-posta adresini ekranda maskele. Başarılı doğrulama ve gerekli ek cihaz güvenliği kontrollerinden sonra yeni PIN oluşturulsun, eski PIN geçersiz kılınsın, önceki kilit açma oturumları sıfırlansın ve e-posta bildirimi gönderilsin.

Firebase Authentication, Supabase Auth ve uygun alternatifleri güvenlik, ücretsiz kota ve bakım açısından karşılaştır. Standart e-posta oturum açma özelliğinin tek başına güvenli yerel PIN sıfırlama sağlamadığını dikkate al. Gerekirse cihaz bağlı anahtar veya ek doğrulama kullan. SMTP şifresi, sunucu gizli anahtarı veya hassas API anahtarlarını APK'ya gömme. E-posta kurtarması internet gerektirebilir; temel kilitleme, biyometri ve mevcut PIN internetsiz çalışmalı. Aylık ek ücret olmadan çalışabilecek mimariyi önceliklendir; ücretsiz kotaları ve olası ücretleri açıkla. Hizmet hesabı açılması gerektiğinde kullanıcıyı adım adım yönlendir.

7. UYGULAMA GEÇİŞİ ALGILAMA
Öncelikle AccessibilityService yaklaşımını araştır. Kullanıcıdan açık izin al. Yalnızca uygulama geçişini belirlemek için gerekli olayları işle. Mesaj, şifre, ekran metni veya gereksiz kullanıcı verisi toplama. UsageStatsManager alternatifini karşılaştır. Android arka plan Activity başlatma kısıtlarını ele al. Olay tabanlı durum makinesi, debounce ve döngü önleme kullan. İzin kaldırılırsa korumanın durduğunu açıkça bildir. Gereksiz polling veya sürekli foreground service kullanma. Google Play erişilebilirlik politikalarını değerlendir.

8. KİLİT DAVRANIŞI
Kilitli uygulama açıldığında mümkün olan en kısa sürede doğrulama ekranını göster. Başarılı doğrulamada hedef uygulamaya dön. İptalde güvenli ekrana dön. Hatalı doğrulamada kilidi açma. Tekrarlanan doğrulama pencerelerini önle. Ekran kapatıldığında, uygulamadan çıkıldığında ve yeniden başlatma sonrasında kilit durumlarını yönet. Son uygulamalar, bildirimler, çoklu pencere ve PiP durumlarını test et. Korunan içeriğin doğrulama öncesinde kısa süre görünmesi riskini ölç. Android'in izin vermediği engellemeleri garanti etme.

9. KİLİTLEME POLİTİKALARI
Her girişte doğrulama, ekran kapatılınca yeniden doğrulama, 30 saniye, 1 dakika, 5 dakika ve 10 dakika güven süresi sun. Uygulama bazlı politika seçimi sağla. Genel korumayı kapatırken biyometrik veya PIN doğrulaması iste. Uygulamanın kendi ayarlarını da koru.

10. SİSTEM UYGULAMALARI VE SINIRLAR
Özellikle Ayarlar uygulamasının kilitlenmesini araştır ve normal Android yetkileriyle mümkün olan en güvenilir yöntemi uygula. Root veya Device Owner yetkisi varsayma. Android güvenlik modelini aşma. Kullanıcının izinleri iptal etmesini veya uygulamayı kaldırmasını gizlice engelleme. Acil durum işlevlerini engelleme. Sistem arayüzleri, launcher ve izin yönetimi gibi kilitlenemeyen alanları açıkça belirt. Root, Shizuku, ADB veya kurumsal cihaz yönetimi alternatiflerini yalnızca isteğe bağlı araştırma olarak raporla.

11. PERFORMANS
Düşük gecikme, düşük RAM ve düşük pil tüketimi hedefle. Olay tabanlı mimari kullan. Gereksiz ağ isteği yapma. Bellek sızıntılarını önle. Android üreticilerinin pil optimizasyonu kısıtlarını değerlendir. Gerçek cihazda kilit ekranı açılma gecikmesini ölç. Ölçmediğin değerleri sonuç olarak sunma.

12. TÜRKÇE ARAYÜZ
Modern ve sade Jetpack Compose arayüzü oluştur. Açık/koyu tema destekle.
Ekranlar:
- Ana ekran: koruma durumu, kilitli uygulama sayısı, izin ve biyometrik durum.
- Uygulamalar: ikon, isim, arama, toplu seçim, kilit anahtarı.
- Kilit açma: biyometrik doğrulama, PIN, PIN'imi Unuttum.
- PIN kurtarma: e-posta kodu, doğrulama, yeni PIN.
- Ayarlar: politikalar, PIN değiştirme, kurtarma e-postası, izinler.
- İlk kurulum: PIN, e-posta doğrulama, biyometrik test, izinler, uygulama seçimi.

13. GELİŞTİRME FAZLARI
FAZ 0: Resmi Android dokümantasyonu araştırması, minimum SDK, biyometri, erişilebilirlik ve e-posta kurtarma mimarisi.
FAZ 1: Android Studio proje iskeleti, Kotlin, Gradle, Compose.
FAZ 2: Uygulama listesi, seçim ve yerel veri saklama.
FAZ 3: AccessibilityService, uygulama geçişi ve kilit durum makinesi.
FAZ 4: BiometricPrompt, yüz uyumluluğu ve parmak izi.
FAZ 5: Güvenli PIN oluşturma, doğrulama ve değiştirme.
FAZ 6: Kurtarma e-postası, sunucu tarafı doğrulama ve PIN sıfırlama.
FAZ 7: Uygulama bazlı kilit politikaları, güven süreleri ve oturum yönetimi.
FAZ 8: Güvenlik, sistem uygulamaları, Android sürüm uyumluluğu ve Play politikaları.
FAZ 9: Otomatik testler, gerçek cihaz testleri, performans ve hata düzeltmeleri.
FAZ 10: Debug APK, release build, kurulum kılavuzu ve teslimat.

14. HER FAZDA KULLANICIYI BİLGİLENDİR — ZORUNLU
FAZ 0'dan FAZ 10'a kadar her fazın başlangıcında Türkçe ve kısa olarak şunları bildir:
- Faz numarası ve adı.
- Bu aşamada ne yapılacak?
- Kullanıcıdan şu anda işlem gerekiyor mu?

Her faz sonunda şunları bildir:
- Tamamlanan işler.
- Derleme durumu.
- Yapılan otomatik testler ve sonuçları.
- Eksikler veya engeller.
- Sıradaki aşama.

Kullanıcı müdahalesi gerekmeyen kodlama ve otomatik testlerde onay beklemeden devam et. Ancak her fazın sonucunu kullanıcıya bildir. Test edilmemiş özelliklere 'başarılı' deme.

15. KULLANICIYA GERÇEK TELEFON TESTLERİNİ ADIM ADIM YAPTIR — ZORUNLU
Gerçek cihaz testi gerektiğinde kullanıcıya:
- Testin amacını,
- APK'nın nasıl kurulacağını,
- Telefonda hangi menüye girileceğini,
- Hangi izinlerin açılacağını,
- Hangi uygulamanın seçileceğini,
- Hangi işlemin yapılacağını,
- Beklenen sonucu,
- Sonucun nasıl bildirileceğini
açık ve numaralı adımlarla anlat.

İlk uygulama kilitleme testini mümkünse Takvim ile yap. Takvim başarıyla test edildikten sonra Ayarlar gibi kritik sistem uygulamalarına geç.

Ayrı ayrı test et:
- Yüz tanıma.
- Parmak izi.
- PIN.
- PIN'imi Unuttum.
- E-posta doğrulama kodu.
- Yeni PIN oluşturma.
- Yanlış PIN ve yanlış e-posta kodu.
- İnternet bağlantısı olmadan temel kilitleme.
- Ekran kapatıp açma.
- Cihaz yeniden başlatma.
- İzin iptali.
- Ayarlar uygulamasını kilitleme.
- Çoklu pencere ve son uygulamalar.

Kullanıcı testi yapmadan o testi başarılı sayma. Kullanıcı sonucu 'başarılı', 'başarısız' veya 'sonra deneyeceğim' şeklinde bildirebilsin. Başarısızsa gerekli hata ayrıntılarını iste, düzelt ve tekrar test yaptır. Ertelenirse 'Gerçek cihaz testi bekleniyor' olarak kaydet. Bağımsız diğer görevlere devam edebilirsin.

16. KULLANICI MÜDAHALESİ KURALLARI
Telefon modeli, Android sürümü, biyometrik destek, APK kurulumu, erişilebilirlik izni, kullanım erişimi izni, e-posta altyapısı hesabı ve gerçek cihaz testleri için gerekli bilgileri ihtiyaç duyulduğunda sor. Hepsini başlangıçta isteme. Kullanıcıya aynı anda çok fazla işlem verme. Her kritik işlemde tek veya az sayıda adım göster ve sonucu bekle. Kullanıcıdan PIN, doğrulama kodu, hesap şifresi veya gizli API anahtarını sohbete yazmasını isteme.

17. TEST PLANI
Android 9 ve sonraki sürümler için uygun test kapsamı hazırla. Yüz tanımalı, parmak izli, her ikisine sahip, biyometrisiz ve BiometricPrompt tarafından yüzü desteklenmeyen cihazları değerlendir. Karanlık ortam, yanlış PIN, yanlış/süresi dolmuş/kullanılmış e-posta kodu, yeniden gönderme sınırı, internet kesintisi, uygulama geçişleri, yeniden başlatma, zorla durdurma, izin iptali, veri silme ve yeniden yükleme senaryolarını kapsa. Otomatik testlerle manuel cihaz testlerini ayrı raporla.

18. TOKEN OPTİMİZASYONU VE OTONOM ÇALIŞMA
Başlangıçta CLAUDE.md, PROGRESS.md, TASKS.md, README.md, docs/ARCHITECTURE.md, docs/DECISIONS.md, docs/LIMITATIONS.md, docs/TEST_PLAN.md ve docs/SECURITY.md oluştur.
- Kalıcı kararları ve ilerlemeyi kısa biçimde kaydet.
- Büyük dosyaları gereksiz yere tekrar okuma.
- İlgili fonksiyon ve dosyaları hedefleyerek incele.
- Her göreve kabul kriteri koy.
- Küçük ve doğrulanabilir değişiklikler yap.
- Her faz sonunda derleme ve ilgili testleri çalıştır.
- Hataları çözmeden başarılı deme.
- Aynı başarısız komutu sebepsiz tekrar etme.
- Çalışan fazlardan sonra Git commit oluştur.
- Gizli bilgileri Git'e ekleme.
- Gereksiz alt ajan çalıştırma.
- Kullanıcı müdahalesi gerekmiyorsa otomatik devam et.
- Bağlam daralınca ilerlemeyi dosyalara kaydet.
- Oturum yeniden başladığında CLAUDE.md, PROGRESS.md ve TASKS.md üzerinden devam et.
- Kullanıcı testlerini ve sonuçlarını kalıcı kaydet.
- Araç, süre veya token limitlerini aşabileceğini varsayma.
- Limit veya kesinti olduğunda kaldığın yeri ve sonraki komutu kaydet.

19. KABUL KRİTERLERİ
- Android Studio projesi açılabilmeli.
- Gradle build başarılı olmalı.
- Uygulamalar listelenip seçilebilmeli.
- Desteklenen uygulamalar kilitlenebilmeli.
- Desteklenen cihazlarda biyometrik doğrulama çalışmalı.
- Yüz tanıma uyumluluğu doğru gösterilmeli.
- Parmak izi ve PIN alternatifleri çalışmalı.
- PIN unutulursa doğrulanmış e-postayla güvenli kurtarma yapılabilmeli.
- Temel kilitleme internetsiz çalışmalı.
- Kullanıcı testleri raporlanmalı.
- Android platform kısıtları belgelenmeli.
- Debug APK üretilebilmeli.
- Release build talimatları hazırlanmalı.
- Türkçe kullanım kılavuzu bulunmalı.

20. TESLİMAT
Tam kaynak kodu, derlenebilir Android Studio projesi, debug APK, release APK oluşturma adımları, Türkçe kurulum ve kullanım kılavuzu, Android sürüm uyumluluk tablosu, biyometrik uyumluluk raporu, e-posta kurtarma mimarisi, güvenlik raporu, test sonuçları, bilinen sınırlamalar ve kalan görevleri teslim et.

21. BAŞLAMA TALİMATI
Önce kullanıcıya Türkçe olarak FAZ 0'a başladığını ve ne yapacağını bildir. Resmi Android dokümanlarıyla teknik uygulanabilirliği değerlendir. Ardından fazları sırasıyla uygula. Her faz başında ve sonunda Türkçe rapor ver. Gerçek telefon testi gerektiğinde kullanıcıya adım adım rehberlik et ve sonucu bekle. Teknik engel veya kullanıcı müdahalesi gerekmiyorsa sonraki faza otomatik geç. Gerekli harici hesap veya cihaz erişimini kendiliğinden var sayma. Tamamlanmamış testleri tamamlandı diye raporlama.

NİHAİ HEDEF
Yüz tanımayı mümkün olan Android cihazlarda önceliklendiren, parmak izi ve PIN alternatifleri sunan, e-posta üzerinden güvenli PIN kurtarma sağlayan, Ayarlar dahil teknik olarak desteklenen uygulamaları kilitleyen, hızlı, düşük pil tüketimli, Türkçe, güvenlik sınırları açıkça belgelenmiş ve gerçek Android telefonlarda kullanılabilir FaceFirst App Lock APK'sını üret.

Şimdi FAZ 0 ile başla.