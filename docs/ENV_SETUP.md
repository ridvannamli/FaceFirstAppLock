# Derleme ortamı kurulumu (geçici bulut oturumu için)
1. `curl -fLo clt.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip`; `unzip` → `/opt/android-sdk/cmdline-tools/latest`.
2. `yes | sdkmanager --licenses`; `sdkmanager platform-tools "platforms;android-36" "build-tools;36.0.0"`.
3. `local.properties`: `sdk.dir=/opt/android-sdk` (Git'e girmez). Gradle 8.14.3 wrapper repoda.
