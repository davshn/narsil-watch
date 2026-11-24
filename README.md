# Narsil Watch

<p align="center">
  <img src="https://raw.githubusercontent.com/davshn/narsil-pet/main/assets/readme/greeting-claro.svg" width="150" alt="Narsil says hello (light mode)">
  <img src="https://raw.githubusercontent.com/davshn/narsil-pet/main/assets/readme/greeting-oscuro.svg" width="150" alt="Narsil says hello (dark mode)">
</p>

> **Narsil Workshop · [narsil.davshn.tech](https://narsil.davshn.tech)**
>
> A free workshop, in English, Spanish, Italian, French, and Brazilian Portuguese, that teaches step by step how to build Claude Code mods like [Narsil](https://github.com/davshn/narsil-pet) and [Narsil Check](https://github.com/davshn/narsil-check), and how to build a Narsil desk bot with a body. No sign-up, no analytics, and it never asks for tokens or keys.

Narsil Watch is the Wear OS app of the [Narsil](https://github.com/davshn/narsil-pet) family: it puts Claude's usage limits and your open Claude Code conversations on your wrist. A tile shows the 5-hour and weekly limits as two rings with Narsil in the middle, a complication puts the 5-hour limit on any watch face, and the app lists every conversation with its state.

It needs no server and no Claude token: it reads the state that the Narsil mod already publishes for your phone.

It is an **open source** app, released under the [GPL v3](#license) license. It is not affiliated with Anthropic.

> **Language note:** like Narsil, the watch speaks Spanish. This README keeps those labels as they appear on screen, with their English meaning next to them.

## Contents

- [Features](#features)
- [How it works](#how-it-works)
- [Installation](#installation)
- [Settings](#settings)
- [Supported systems](#supported-systems)
- [Other Narsil mods](#other-narsil-mods)
- [Security](#security)
- [Development](#development)
- [License](#license)

## Features

### Tile
Swipe from the watch face to the **Narsil · Claude** tile. The outer ring is the 5-hour limit and the inner ring the weekly one, both colored as Narsil colors them: green, yellow from 60 %, and red from 85 %. Narsil sits in the middle, in the pose and stage of your most important conversation, above how many conversations need you and how old the reading is.

### Complication
The **Claude 5 h** complication shows the 5-hour limit on any watch face, as an arc (`RANGED_VALUE`) or as a short text.

### App
The app lists every open conversation with its state, the limits with when they reset ("reinicia en", *resets in*), and Narsil's level. The states are the ones Narsil uses:

| State | When |
| --- | --- |
| **TRABAJANDO** (working) | Claude is thinking, reading, searching, editing, or running commands. |
| **PARADA** (idle) | The conversation is waiting for your message. |
| **TE NECESITA** (needs you) | There is a question, a plan to approve, or a permission to decide. |
| **TERMINADA** (done) | Claude has finished its answer. |
| **ERROR** | The turn stopped because of an error. |

### Alerts
Alerts (a question, a finished turn, an error) are not part of this app: [Narsil for Android](https://github.com/davshn/narsil-android) or the [ntfy](https://ntfy.sh) app on your phone already mirror them to the watch.

## How it works

Narsil, the Claude Code mod on your computer, already publishes its state to a private ntfy.sh topic, `<your topic>-estado`. The watch reads the latest message from that topic every 15 minutes, and whenever you open the app or the tile.

```
Claude Code ── Narsil mod ──► ntfy.sh/<topic>-estado ──► Narsil Watch (tile, complication, app)
```

If you turned on `/narsil movil clave`, the state is sealed (HMAC-SHA256 in counter mode, with a tag). Give the watch the same key and it opens the state just as the computer sealed it.

Good to know:

- The limits and the conversation list need a Narsil version that publishes them (`limits` and `sessions` in the state).
- Narsil publishes the state when a turn ends while you are away from the computer (you wrote from your phone or another device), within ntfy.sh's free daily room. At your desk the watch keeps the last reading, and the tile says how old it is.

## Installation

You need a Wear OS 3 or later watch with developer options and wireless debugging on, and on your computer a JDK 17 or later and the Android SDK (`ANDROID_HOME`). Pair the watch over Wi-Fi and, in a copy of this repository, run:

```
adb connect <watch-ip>:<port>
./gradlew :app:installDebug
```

Then add the **Narsil · Claude** tile and the **Claude 5 h** complication from the watch's tile and watch face editors.

To update it, pull the latest version of this repository and run the same command again: the settings are kept.

## Settings

The watch needs your topic and, if you use one, your key. In Claude Code, `/narsil movil` shows your topic and `/narsil movil clave` the key. Then either:

- type them in the app, under **Configurar** (*set up*), or
- send them from the computer:

```
adb shell am start -n tech.davshn.narsilwatch/.presentation.MainActivity --es topic narsil-... --es key ...
```

## Supported systems

| | Supported |
| --- | --- |
| Wear OS | 3 or later (API 30+) |
| Narsil mod | A version that publishes `limits` and `sessions` in its state |
| Tizen, watchOS | Not available |

## Other Narsil mods

The watch app is one of several pieces. Each one installs on its own:

| | What it is |
| --- | --- |
| [Narsil](https://github.com/davshn/narsil-pet) | The Claude Code mod: a band above the prompt, voice, alerts, and the ntfy.sh state this app shows. **The watch needs it.** |
| [Narsil Check](https://github.com/davshn/narsil-check) | A side pane that checks the project you are working on. |
| [Narsil Skin](https://github.com/davshn/narsil-skin) | Color themes and animated wallpapers for the Claude desktop app. |
| [Narsil for Android](https://github.com/davshn/narsil-android) | `claude.ai` in Narsil's colors and her alerts as native notifications, on your phone. |

## Security

- The topic and the key are kept in the app's private storage on the watch, and the watch only reads the topic: it never publishes on it.
- No Claude token or account reaches the watch. It only talks to ntfy.sh, for the state, and to the [narsil-pet](https://github.com/davshn/narsil-pet) repository on GitHub, for Narsil's drawings.
- Without a key, anyone who knows your topic's name can read it. Use `/narsil movil clave` if that matters to you.

## Development

Contributions are welcome: open an issue or a pull request on [GitHub](https://github.com/davshn/narsil-watch).

```
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

The tests check the parsing of Narsil's state, how the watch words and colors it, and the encryption against a vector from Narsil's own `seal.ts`.

- `domain/`: the state as Narsil publishes it (`NarsilState.kt`), and how the watch words and colors it (`Display.kt`).
- `data/`: reading the ntfy topic (`StatusRepository.kt`, `StatusParser.kt`), opening the seal (`Seal.kt`), and keeping the last state.
- `tile/`, `complication/`, `presentation/`: the three surfaces.
- `sync/`: the 15-minute refresh (WorkManager) and the redraws.

## License

Narsil Watch is free software: you can redistribute it and modify it under the terms of the [GNU General Public License, version 3](LICENSE) (GPL v3) or, at your option, any later version, as published by the Free Software Foundation.

In short: you can use, study, modify, and share it freely, but if you distribute a modified version you must also publish it as open source under the same license. It comes with no warranty. See the [LICENSE](LICENSE) file for the full text.

Narsil's art is © its author and comes from the [narsil-pet](https://github.com/davshn/narsil-pet) repository.

Copyright © 2026 Davshn
