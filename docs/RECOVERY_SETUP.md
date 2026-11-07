# PIN kurtarma sunucusu kurulumu (tek seferlik)
Mimari: uygulama → Cloudflare Worker (`server/worker.js`, KV) → Brevo e-posta API'si. Gizli anahtarlar yalnızca Worker sırlarında; APK ve Git'te yok.
Ücretsiz kotalar (resmi sayfalardan doğrulanmalı): Workers 100 bin istek/gün; KV 100 bin okuma, 1000 yazma/gün; Brevo 300 e-posta/gün. Aşılırsa istekler hata verir, otomatik ücret yok (ücretli plana geçmediğin sürece).
1. cloudflare.com'da ücretsiz hesap aç. `npm i -g wrangler && wrangler login`.
2. `cd server && wrangler kv namespace create KV` → çıkan id'yi `wrangler.toml` içindeki KV bloğuna yaz.
3. brevo.com'da ücretsiz hesap aç, gönderen adresini (kendi e-postan) doğrula, bir API anahtarı üret.
4. `wrangler secret put MAIL_API_KEY` ve `wrangler secret put MAIL_FROM` (değerleri terminale yaz, sohbete YAZMA).
5. `wrangler deploy` → çıkan https adresini al (gizli değil).
6. APK'yı `./gradlew assembleDebug -PrecoveryUrl=https://<adres>` ile derle.
Test: `cd server && npm test`.
