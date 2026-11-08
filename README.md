<div align="center">

# 🎧 OpenTS

**Modern Open-Source TeamSpeak 3 / TeamSpeak 6 Client for Android**  
*Crafted with love for fast, high-quality, and cozy voice communication :3*

[![Platform](https://img.shields.io/badge/Platform-Android%2010%2B%20(API%2029%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Rust](https://img.shields.io/badge/Core-Rust%20tslib-DEA584?style=for-the-badge&logo=rust&logoColor=black)](https://github.com/flamme-demon/tslib_multi)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge)](LICENSE)
[![Creator](https://img.shields.io/badge/Creator-NearCorp-FF4081?style=for-the-badge)](https://github.com/NearCorp)

[🇬🇧 English](README.md) • [🇷🇺 Русский](README_RU.md) • [📥 Download APK](#-quick-start--installation) • [🛠 Build from Source](#-building-from-source)

</div>

---

## 🌟 About the Project

**OpenTS** is an unofficial, modern Android client for **TeamSpeak 3** and **TeamSpeak 6** voice protocols.

Unlike outdated legacy clients, **OpenTS** was designed from the ground up prioritizing **Material You (Material 3)** aesthetics, ultra-low audio latency powered by a high-performance Rust native engine ([tslib](https://github.com/flamme-demon/tslib_multi)), rich UI personalization, and full control over your microphone and audio output routing.

---

## ✨ Key Features

### 🎙 Advanced Audio & Voice Communication
* **Adaptive Voice Activity Detection (VAD):**
  * Real-time voice activation with a dynamic live microphone energy level meter and an adjustable threshold slider.
  * When silent, transmission is completely gated (Noise Gating), ensuring absolute silence in the channel.
* **Push-To-Talk (PTT):**
  * Large, responsive talk button with an instant one-tap switcher between PTT and VAD modes on the bottom bar.
* **Audio Output Routing (Speakerphone / Earpiece Toggle):**
  * Quick switch button in the top bar: listen out loud through external phone stereo speakers, or privately through the in-call earpiece.
  * Clear toast notifications confirm the active routing mode.
* **Microphone Gain Boost:**
  * Hardware & software amplification ranging from **1.0× to 8.0×**. Teammates will hear you clearly even with a quiet microphone.
* **Hardware Audio Effects:**
  * **Noise Suppression (NoiseSuppressor):** cuts out fan hum, background noise, and keyboard clicks.
  * **Acoustic Echo Cancellation (AcousticEchoCanceler):** eliminates speakerphone audio bleeding back into the microphone.
  * Automatic hardware capability check with live device support indicators.
* **Microphone Input Device Selector:**
  * Choose between bottom main mic, camera mic, wired headsets, and external USB/Bluetooth audio interfaces.
* **Per-User Volume Control:**
  * Individually adjust any user's volume from 0% to 200%, or mute specific noisy users.
* **Opus Voice Codec:**
  * 48 kHz, mono, 20 ms frames for crystal-clear voice clarity with minimal bandwidth and latency.

### 🎨 Customization & Material 3
* **Custom Background Wallpaper:**
  * Pick any photo or image from your gallery as the app's background.
  * Adjustable dimming slider (10% to 90%) ensures cards, channel trees, and chat remain crisp and readable over any wallpaper.
* **Material You Design:**
  * Modern cards, smooth animations, talking ripples, and dynamic theme support.
* **Multilingual:**
  * Complete Russian and English translations, plus auto-detection from system settings.

### 💬 Chat & Multimedia
* **Channel & Private Messaging (PM):**
  * Unread badges, conversation persistence across sessions.
  * BBCode formatting (bold, italic, colors, links).
  * Automatic image preview and video link thumbnails.
* **Screen Broadcast Viewer:**
  * Built-in player for external live video streams (RTSP, RTMP, HTTP, HLS / .m3u8).
  * Auto-detection of video links posted in channel chat.
  * Aspect ratio toggle (Fit / Fill) and fullscreen mode.

### 📁 Channel File Manager
* Browse files and folders in the current channel on the server.
* Download files directly to your phone's `Downloads` directory.
* Upload files from phone storage, create directories, rename, and delete files.

### 🔖 Bookmarks & Usability
* Save your favorite servers with automatic icon fetching and caching.
* Instant connection and optional **Auto-Reconnect** on app launch.

---

## 📥 Quick Start & Installation

You can download the pre-compiled APK from the [**Releases**](../../releases) page:

1. Download **`app-debug.apk`**.
2. Install it on your Android device (requires Android 10 / API 29 or higher).
3. Grant Audio Recording permission on first launch.
4. Connect to any TeamSpeak 3 server and start chatting!

---

## 🛠 Building from Source

### Prerequisites
* **JDK 17** (Amazon Corretto, Eclipse Temurin, or OpenJDK 17)
* **Android Studio Ladybug** (or newer) / Android SDK 35 / NDK 27.x
* **Rust** (stable toolchain via `rustup`)
* **`cargo-ndk`**:
  ```bash
  cargo install cargo-ndk
  ```
* Native library **tslib** cloned next to `OpenTS`:
  ```bash
  git clone https://github.com/flamme-demon/tslib_multi ../tslib
  ```

### Build Steps

1. **Clone the OpenTS repository:**
   ```bash
   git clone https://github.com/NearCorp/OpenTS.git
   cd OpenTS
   ```

2. **Compile native Rust JNI libraries (arm64-v8a & x86_64):**
   ```bash
   ./gradlew buildRustLibs
   ```

3. **Build the debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```

The compiled APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🏗 Architecture Overview

```
OpenTS/
├── app/src/main/
│   ├── java/dev/tslib/          # JNI bridge to Rust tslib native library
│   ├── kotlin/dev/tsdroid/
│   │   ├── bridge/              # Audio engine (AudioBridge), cache managers
│   │   ├── data/                # DataStore preferences, bookmarks, message database
│   │   ├── service/             # TsConnectionService (Foreground connection service)
│   │   ├── viewmodel/           # Architecture ViewModels (ConnectionViewModel, ServerViewModel)
│   │   └── ui/
│   │       ├── screen/          # Primary screens (ConnectionScreen, ServerScreen)
│   │       └── component/       # Compose components (ChannelTree, ChatView, SettingsDialog...)
│   └── res/                     # Vector icons, themes, and string localizations (ru, en)
├── gradle/                      # Version catalogs and Gradle build configuration
└── LICENSE                      # GNU General Public License v3.0
```

---

## 🤝 Contributing

Contributions are welcome!
* Encountered an issue? [Open an Issue](../../issues/new?template=bug_report.md) with steps to reproduce and your device model.
* Have an idea for a feature? [Submit a Feature Request](../../issues/new?template=feature_request.md).
* Want to contribute code? Read our [CONTRIBUTING.md](CONTRIBUTING.md) guide and submit a PR!

---

## 📜 License

**OpenTS** is released under the **GNU General Public License v3.0 (GPL-3.0)**.  
You are free to inspect, modify, and redistribute the source code under the terms of the license.

See the [LICENSE](LICENSE) file for complete details.

---

<div align="center">

**Creator:** [**NearCorp**](https://github.com/NearCorp)  
*Crafted with love for cozy voice chats :3*

</div>
