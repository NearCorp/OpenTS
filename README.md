# TS6 Droid

Client libre et léger TeamSpeak 3/6 pour Android, construit avec [tslib](https://github.com/flamme-demon/tslib_multi) (bibliothèque Rust) et Jetpack Compose.

## Fonctionnalités

- **Connexion** aux serveurs TeamSpeak 3 et 6
- **Favoris** avec nom du serveur et icône (récupérés automatiquement)
- **Reconnexion automatique** au dernier serveur
- **Chat** channel et messages privés avec historique persistant
- **Push-to-Talk** avec codec Opus (48 kHz, mono)
- **Arborescence des channels** avec liste des utilisateurs
- **Gestionnaire de fichiers** — navigation, upload, download, création de dossiers
- **Parcours des channels** avant connexion (sélecteur de channel)
- **BBCode** dans les messages

## Architecture

```
app/src/main/
├── java/dev/tslib/          # Classes JNI (pont vers la lib Rust)
├── kotlin/dev/tsdroid/
│   ├── bridge/              # TsClient (coroutines + StateFlow)
│   ├── data/                # BookmarkStore, ServerBookmark, MessageStore
│   ├── service/             # TsConnectionService (foreground service)
│   ├── viewmodel/           # ConnectionViewModel, ServerViewModel
│   └── ui/
│       ├── screen/          # ConnectionScreen, ServerScreen
│       └── component/       # ChannelTree, ChatView, MessageBubble, ...
└── res/                     # Ressources Android
```

La bibliothèque native `tslib` (Rust) gère le protocole TeamSpeak via JNI. Le service Android `TsConnectionService` maintient la connexion en arrière-plan avec une notification persistante.

## Prérequis

- **Android Studio** (ou Gradle 8.14+)
- **JDK 17**
- **Rust** avec [cargo-ndk](https://github.com/nickelc/cargo-ndk)
- **Android NDK** 27.x (via SDK Manager)
- **tslib** cloné à côté du projet (`../tslib`)

## Build

### 1. Compiler la bibliothèque native

```bash
cd ../tslib

ANDROID_NDK=~/Android/Sdk/ndk/27.2.12479018 \
CMAKE_POLICY_VERSION_MINIMUM=3.5 \
cargo ndk -t arm64-v8a \
  -o ../TS6_Droid/app/src/main/jniLibs \
  build --release -p tslib-jni --features vendored-openssl
```

Ou via la tâche Gradle intégrée :

```bash
./gradlew buildRustLibs
```

### 2. Compiler l'APK

```bash
./gradlew assembleDebug
```

L'APK se trouve dans `app/build/outputs/apk/debug/`.

### 3. Installer sur un appareil

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Configuration

| Paramètre | Valeur |
|-----------|--------|
| `minSdk` | 29 (Android 10) |
| `targetSdk` | 35 |
| `compileSdk` | 35 |
| Audio | Opus, 48 kHz, mono, frames 20 ms |

## Licence

Ce projet est un logiciel libre. Voir le fichier [LICENSE](LICENSE) pour plus de détails.
