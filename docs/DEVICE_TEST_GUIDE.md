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

## 0.3.0 Google ile PIN sıfırlama (Google Cloud kurulumu ve istemci kimlikli APK gerekir)
**G1 Bağlama.** Ana ekran → "Google hesabını bağla" → hesabını seç → ana ekranda "PIN kurtarma: r***@gmail.com" yazmalı.
**G2 Sıfırlama.** Kilit ekranında PIN'e geç → "PIN'imi unuttum" → telefon ekran kilidi → Google hesabı → yüz → yeni PIN. Eski PIN çalışmamalı.
**G3 Yanlış hesap.** Başka bir Google hesabı seç → "bağlı hesap değil" demeli.
**G4 Yanlış yüz / fotoğraf.** Yüz adımında başka kişi ya da fotoğraf → sıfırlama iptal olmalı.
(Eski T5 "e-posta kodu" testi geçersiz.)
