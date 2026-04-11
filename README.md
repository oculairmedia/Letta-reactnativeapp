# Letta Mobile

Native Android client for [Letta](https://github.com/letta-ai/letta) AI agents — built with Kotlin and Jetpack Compose.

## Features

- 📱 Native Android experience with Material 3 design
- 🤖 Full agent management — create, edit, clone, delete
- 💬 Rich chat with tool output rendering, code highlighting, and thinking indicators
- 🧠 Memory management — core memory, archival search, block library
- 🔧 Tool & MCP server administration
- 📊 Job monitoring, run tracking, and schedule management
- 🎨 Custom Lucide icon library, shared element transitions, chat background themes

## Getting Started

The app lives in `android-compose/`. See [CONTRIBUTING.md](CONTRIBUTING.md) for setup instructions.

```bash
cd android-compose
cp local.properties.example local.properties  # Set your SDK path
export JAVA_HOME="/path/to/Android Studio/jbr"
./gradlew :app:assembleDebug
```

## Architecture

| Module | Purpose |
|--------|---------|
| `core/` | Data layer — API client, Room database, repositories, models |
| `designsystem/` | Reusable Compose components, theming, LettaIcons |
| `app/` | UI screens, ViewModels, navigation, Hilt DI |

## Development

```bash
./gradlew :app:compileDebugKotlin    # Compile check
./gradlew :app:testDebugUnitTest     # Unit tests
./gradlew :app:assembleDebug         # Build APK
./gradlew installDebug               # Install on device
```
