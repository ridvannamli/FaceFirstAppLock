# Google Play notları (yayın düşünülürse; APK ile dağıtımda zorunlu değil)
- AccessibilityService: erişilebilirlik aracı olmayan uygulamalar için Play Console'da bildirim formu, uygulama içi belirgin açıklama + açık onay gerekir. Açıklama metni uygulamada var (`a11y_disclosure`); onay, kullanıcının ayarlarda izni kendisinin açmasıdır. App Lock kategorisi politikada açıkça adlandırılmamıştır; yayından önce güncel politika metni yeniden okunmalıdır (belirsiz: kabul garantisi yok).
- Veri güvenliği formu: yerelde yalnızca kilitli paket adları, PIN hash'i. Hesap/e-posta toplanmaz; kendi sunucumuz yoktur.
- QUERY_ALL_PACKAGES kullanılmaz; yalnızca LAUNCHER <queries>.
- Hedef API: 36.
