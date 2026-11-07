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
