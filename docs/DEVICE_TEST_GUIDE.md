# Gerçek cihaz test yönergesi (her test: Amaç / Adımlar / Beklenen / Sonuç: başarılı, başarısız veya sonra)
Hazırlık: APK kurulu, PIN kurulu, erişilebilirlik izni açık, Takvim kilitli. Sonucu bana şöyle yaz: "T1 başarılı", "T3 başarısız: <ne gördün>", "T5 sonra".
**T1 Takvim kilidi.** Takvim'i aç → hemen kilit ekranı gelmeli. Süreyi kabaca not et (yavaş/ani).
**T2 Yüz tanıma.** (Telefonda yüz kayıtlıysa) T1'de yüzünü göster → Takvim açılmalı. Yüz istemi hiç çıkmazsa ana ekrandaki biyometrik satırını yaz.
**T3 Parmak izi.** Kayıtlı parmakla doğrula → açılmalı. Yanlış parmak: açılmamalı.
**T4 PIN.** "PIN kullan" → doğru PIN açar. Yanlış PIN 5 kez → bekleme mesajı çıkmalı; bekleme bitmeden doğru PIN kabul EDİLMEMELİ.
**T5 PIN'imi unuttum / e-posta kodu.** (Kurtarma servisi kuruluysa) "PIN'imi unuttum" → e-postaya kod gelmeli (5 dk). Yanlış kod reddedilmeli; doğru kodla yeni PIN belirle; eski PIN artık çalışmamalı; bildirim e-postası gelmeli.
**T6 İnternetsiz.** Uçak modu aç → Takvim kilidi, biyometrik ve mevcut PIN yine çalışmalı.
**T7 Ekran kapat/aç.** Takvim'i aç/doğrula → ekranı kapat/aç → "ekran kapanana dek" seçiliyken tekrar doğrulama istemeli.
**T8 Yeniden başlatma.** Telefonu yeniden başlat → Takvim yine kilitli olmalı (servis açık kalmış olmalı; kalmadıysa yaz).
**T9 İzin iptali.** Erişilebilirlik iznini kapat → ana ekranda "Koruma kapalı" yazmalı ve Takvim açılmalı. Tekrar aç → korunmalı.
**T10 Ayarlar kilidi.** Takvim başarılıysa Ayarlar'ı kilitle → Ayarlar açılırken kilit gelmeli. (Erişilebilirlik ayarlarına girmek için önce korumayı geçici kapatman gerekebilir; PIN ister.)
**T11 Çoklu pencere / son uygulamalar.** Kilitli uygulamayı bölünmüş ekranda aç; son uygulamalardan geri dön → kilit gelmeli. İçeriğin kısa görünüp görünmediğini yaz.
**T12 Süreler.** 30 sn seç → doğrula, çık, 10 sn sonra dön → sormamalı; 40 sn sonra dön → sormalı.
Hata olursa: ne yaptın, ekranda ne yazdı, telefon modeli + Android sürümü.

## 0.2.0 Uygulama içi yüz tanıma testleri (test sürümünde ekranda "benzerlik / canlılık / göz" değerleri görünür; bunları bana yaz)
**Y1 Kayıt.** Aç → PIN gir → "Yüz kaydet" → kamera izni ver → PIN → kameraya düz bak, 5/5 dolmalı.
**Y2 Açma.** Kilitli uygulamayı aç → kamera çıkar, yüzünle açılmalı. Süre (hızlı/yavaş) ve ekrandaki benzerlik değeri?
**Y3 Fotoğraf saldırısı.** Başka bir telefonda/kâğıtta kendi fotoğrafını kameraya göster → AÇILMAMALI. Canlılık değeri kaç?
**Y4 Video saldırısı.** Başka telefonda kendi videonu oynat → açılıyor mu? (Bilinen risk; sonucu yaz.)
**Y5 Başka kişi.** Başka biri denesin → açılmamalı. Benzerlik değeri kaç?
**Y6 Gözler kapalı.** Gözlerin kapalıyken → açılmamalı.
**Y7 Karanlık / gözlük / maske.** Her birinde ne oluyor?
**Y8 Deneme sınırı.** 3 kez başarısız yüz → "Yüzü tekrar dene" kaybolmalı, yalnızca PIN.
**Y9 Yeniden başlatma.** Telefonu yeniden başlat → ilk açılışta yalnızca PIN istenmeli.
**Y10 Yüksek güvenlik.** Anahtarı aç → açılışta "göz kırp / başını çevir / gülümse" istenmeli; yapmadan açılmamalı.

## 0.3.1 PIN sıfırlama (sunucusuz)
**R1 Kurtarma kodu oluştur.** Ana ekran → "Kurtarma kodu oluştur" → PIN → kod bir kez gösterilir, not et → "Not ettim".
**R2 Kodla sıfırla.** Kilit ekranı → PIN'e geç → "PIN'imi unuttum" → "Kurtarma kodu ile sıfırla" → kodu gir → yeni PIN → YENİ kod gösterilmeli (eskisi artık geçersiz).
**R3 Yüzle sıfırla.** "PIN'imi unuttum" → "Ekran kilidi ve yüz ile sıfırla" → telefon kilidi → istenen hareketi yap → yeni PIN. Eski PIN çalışmamalı.
**R4 Yanlış kod.** 5 kez yanlış kod → bekleme mesajı; bekleme sırasında doğru kod da reddedilmeli.
**R5 Yüz saldırısı.** R3'te fotoğraf/başka kişi → "Yüz doğrulanamadı", PIN değişmemeli.
(Eski T5 "e-posta kodu" ve G1-G4 testleri geçersiz.)

## 0.4.0 Kaldırma koruması (ana ekrandaki anahtarı aç)
**K1 Ana ekrandan kaldırma.** FaceFirst Kilit simgesine basılı tut → Kaldır → pencere hemen kapanmalı ve kilit ekranı gelmeli. Gelmezse FaceFirst'i aç, TEŞHİS satırındaki "son:" paket adını bana yaz.
**K2 Ayarlar.** Ayarlar'ı aç → kilit ekranı gelmeli. Doğrulayınca Ayarlar açılmalı.
**K3 Play Store.** Play Store'u aç → kilit ekranı gelmeli.
**K4 Sahibi kaldırabilmeli.** K1'de doğrula → 1 dakika içinde yeniden "Kaldır" de → bu kez pencere kalmalı (İptal'e bas, gerçekten kaldırma).
**K5 Hızlı dokunuş.** K1'de pencere çıkar çıkmaz "Tamam"a en hızlı şekilde bas → uygulama kaldırılabildi mi? (Yarış testi; sonucu yaz.)
**K6 Korumayı kapatma.** Anahtarı kapat → PIN istemeli.

## 0.5.0 Cihaz yöneticisi (ana ekrandaki anahtar)
**D1 Açma.** Anahtarı aç → Android'in "Cihaz yönetici uygulaması etkinleştirilsin mi?" ekranı çıkmalı → "Etkinleştir". Ana ekranda anahtar açık görünmeli.
**D2 Kaldırma engeli.** (Kaldırma koruması kapalıyken dene.) Simgeye basılı tut → Kaldır → sistem kaldırmayı reddetmeli ya da "önce cihaz yöneticisini kapat" demeli. Kaldırma gerçekleşirse sonucu bana yaz.
**D3 Ayarlar yolu.** Kaldırma koruması AÇIKKEN Ayarlar > Güvenlik > Cihaz yönetici uygulamaları'na gir → kilit ekranı gelmeli.
**D4 Uygulamadan kapatma.** Anahtarı kapat → PIN istemeli → kapatınca Kaldır normal çalışmalı.
**D5 Sıfırlama sonrası.** Cihaz yöneticisi açıkken PIN sıfırlama yine çalışmalı.
