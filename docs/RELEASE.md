# Release APK (kendi imza anahtarınla)
1. Anahtar (bir kez; dosyayı ve şifreyi Git'e KOYMA, yedekle — kaybedersen güncelleme yapılamaz):
   `keytool -genkeypair -v -keystore ~/facefirst.jks -alias facefirst -keyalg RSA -keysize 4096 -validity 10000`
2. `ANDROID_HOME=... ./gradlew assembleRelease -PrecoveryUrl=https://<worker-adresi>`
3. `apksigner sign --ks ~/facefirst.jks --out FaceFirstKilit.apk app/build/outputs/apk/release/app-release-unsigned.apk` (build-tools içinde), sonra `apksigner verify`.
Not: R8 (küçültme) etkin; çalışma zamanı cihazda doğrulanmalıdır.
