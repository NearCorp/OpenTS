# OpenTS v1.0.0 — Official First Release 🚀

Welcome to the initial official release of **OpenTS** — a modern, open-source TeamSpeak 3 client for Android built from the ground up with Jetpack Compose, Material 3, and a high-performance native Rust core.

---

## 🌟 What's New / Что нового

### 🎙 Audio & Voice Engine
* **Dynamic Audio Routing:**
  * Automatic detection and switching for wired 3.5mm & USB Type-C headsets.
  * Full Bluetooth SCO / Hands-Free profile support with dynamic mic & speaker switching.
  * Real-time audio routing toggle: switch between loudspeaker and in-call earpiece with a single tap.
* **Microphone Processing & Effects:**
  * Hardware **Noise Suppression** (cuts out keyboard clicks and background noise).
  * Hardware **Acoustic Echo Cancellation** (prevents speaker feedback).
  * **Microphone Gain Amplification** slider from 1.0× to 8.0×.
  * Live microphone energy meter with color-coded threshold indicator.
* **Voice Modes:**
  * **Voice Activity Detection (VAD)** with noise gating and adjustable sensitivity threshold.
  * **Push-To-Talk (PTT)** with instant bottom-bar toggle and responsive talk button.
* **Per-User Volume Control:**
  * Custom volume slider (0% to 200%) and individual mute for each client in the channel.

### 📱 User Interface & Navigation (Material 3)
* **Active Connection Banner:**
  * If you navigate back to the main menu while connected, an active connection card appears with options to return to the server or disconnect.
* **Smart Back Navigation:**
  * Clean back stack management: gracefully handles modals, chat, details, channel list, and shows an exit/minimize dialog when leaving.
* **Channel Tree:**
  * Interactive channel hierarchy with collapsible spacers, client badges, talking indicators, channel codec info, and passwords.
* **Text Chat & Messaging:**
  * Channel chat and private messaging tabs.
  * BBCode parser, clickable hyperlinks, and chat bubble timestamps.
* **Bookmarks & Identities:**
  * Save favorite servers with custom nicknames and channels.
  * Generate and manage cryptographic client identities.

### 🌐 Localization & Links
* **Bilingual Support:** Full English (default) and Russian localizations.
* **Creator Information:** Added direct links to Near Corporation Telegram ([@NearCorporation](https://t.me/NearCorporation)) and GitHub ([@NearCorp](https://github.com/NearCorp)) in the About dialog.

---

## 📦 Package Information / Информация о пакете

| Attribute | Details |
| :--- | :--- |
| **Version** | `1.0.0` (Version Code: `1`) |
| **File Name** | `OpenTS-v1.0.0.apk` |
| **File Size** | ~64 MB |
| **Min Android** | Android 8.0 (API 26) |
| **Target Android** | Android 14 (API 34) |
| **Signature** | Signed with Release Keystore (v2 Scheme) |
| **SHA-256** | `6a84e2e32c5837198e123ccbd7946678e0a3152824c8e1184942b70378902ed2` |

---

## 📥 Download / Скачать

* [**Direct Download APK**](https://raw.githubusercontent.com/NearCorp/OpenTS/release-binaries/OpenTS-v1.0.0.apk)
* Source code is available on the [`main`](https://github.com/NearCorp/OpenTS/tree/main) branch.
