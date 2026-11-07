# OpenTS

Клиент с открытым исходным кодом TeamSpeak 3 / TeamSpeak 6 для Android, построенный на базе [tslib](https://github.com/flamme-demon/tslib_multi) (библиотека Rust) и Jetpack Compose (Material 3).

## Возможности

- **Подключение** к серверам TeamSpeak 3 и TeamSpeak 6
- **Material 3 Design** с адаптивной темной и светлой темой, карточками и современными формами
- **Голосовая связь**:
  - **Настоящий VAD (Voice Activity Detection)**: порог срабатывания с живым визуальным индикатором уровня микрофона
  - **Push-to-Talk (PTT)** с быстрым переключением между режимами
  - **Акустическое эхоподавление (AEC)** и **Шумоподавление (NS)**
  - **Индивидуальная регулировка громкости пользователей** (0% – 200%) и быстрое отключение звука отдельного собеседника
  - Кодек Opus (48 кГц, моно, 20 мс фреймы)
- **Иерархическое дерево каналов** со статусами пользователей (говорит, микрофон выключен, отошел, запись)
- **Чат канала и личные сообщения**:
  - Сохранение истории сообщений
  - BBCode разметка
  - Встроенные превью изображений и видео (YouTube, Dailymotion)
- **Файловый менеджер каналов**: навигация, скачивание, загрузка, переименование, удаление и отправка файлов в чат
- **Закладки** серверов с автоматической загрузкой иконок и функцией автоподключения
- **Многоязычность**: русский, английский и французский языки

## Архитектура

```
app/src/main/
├── java/dev/tslib/          # JNI-классы (мост к нативной Rust-библиотеке)
├── kotlin/dev/tsdroid/
│   ├── bridge/              # TsClient, AudioBridge (VAD, AEC, NS), кэши аватаров и иконок
│   ├── data/                # BookmarkStore, SettingsStore, MessageStore
│   ├── service/             # TsConnectionService (Foreground Service)
│   ├── viewmodel/           # ConnectionViewModel, ServerViewModel
│   └── ui/
│       ├── screen/          # ConnectionScreen, ServerScreen
│       └── component/       # ChannelTree, ChatView, MessageBubble, FileManagerDialog
└── res/                     # Ресурсы и локализации (ru, en, default)
```

## Требования для сборки

- **Android Studio** Ladybug или новее (Gradle 8.7+)
- **JDK 17**
- **Rust** и утилита [cargo-ndk](https://github.com/nickelc/cargo-ndk)
- **Android NDK** 27.x
- **tslib** склонированный рядом с проектом (`../tslib`):
  `git clone https://github.com/flamme-demon/tslib_multi ../tslib`

## Сборка

### 1. Сборка нативной библиотеки tslib
```bash
./gradlew buildRustLibs
```
*(Либо вручную через `cargo ndk -t arm64-v8a -t x86_64 -o app/src/main/jniLibs build --release -p tslib-jni --features vendored-openssl`)*

### 2. Сборка APK
```bash
./gradlew assembleDebug
```
Готовый APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Лицензия

Проект распространяется под свободной лицензией. См. файл LICENSE.
