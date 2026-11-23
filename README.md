# Narsil Watch

Narsil on your Wear OS watch: Claude's usage limits and your open Claude Code conversations at a glance, in [Narsil](https://github.com/davshn/narsil-pet)'s style.

> **Language note:** like Narsil, the watch speaks Spanish ("TE NECESITA", *needs you*; "reinicia en", *resets in*).

## What it shows

- **Tile** (swipe from the watch face): the 5-hour limit as the outer ring, the weekly limit as the inner ring, both colored as Narsil colors them (green, yellow from 60 %, red from 85 %). Narsil sits in the middle in the pose and stage of your most important conversation, above how many conversations need you and how old the reading is.
- **Complication**: the 5-hour limit on any watch face, as an arc (`RANGED_VALUE`) or a short text.
- **App**: every open conversation with its state (TRABAJANDO, PARADA, TE NECESITA, TERMINADA, ERROR), the limits with when they reset, and Narsil's level.

Alerts (a question, a finished turn, an error) are not part of this app: the [ntfy](https://ntfy.sh) app on your phone already mirrors them to the watch.

## How it works

No server, no Claude token on the watch. Narsil (the Claude Code mod on your PC) already publishes its state to a private ntfy.sh topic, `<your topic>-estado`. The watch reads the latest message from that topic every 15 minutes, and when you open the app or the tile.

```
Claude Code ── Narsil mod ──► ntfy.sh/<topic>-estado ──► Narsil Watch (tile, complication, app)
```

If you turned on `/narsil movil clave`, the state is sealed (HMAC-SHA256 in counter mode, with a tag). Give the watch the same key and it opens the state just as the PC sealed it ([`Seal.kt`](app/src/main/kotlin/tech/davshn/narsilwatch/data/Seal.kt) is tested against a vector from Narsil's own `seal.ts`).

**Good to know:**

- The limits and the conversation list need a Narsil version that publishes them (`limits` and `sessions` in the state).
- Narsil publishes the state when a turn ends while you are away from the PC (you wrote from your phone or another device), within ntfy.sh's free daily room. At your desk the watch keeps the last reading, and the tile says how old it is.
- Whoever knows an unsealed topic's name can read it. Use `/narsil movil clave` if that matters to you.

## Install

You need a Wear OS 3+ watch (API 30+) with developer options and wireless debugging on, and the Android SDK with JDK 17.

```
adb connect <watch-ip>:<port>
./gradlew :app:installDebug
```

## Set up

In Claude Code, `/narsil movil` shows your topic, and `/narsil movil clave` the key if you use one. Then either type them in the app (**Configurar**), or send them from the PC:

```
adb shell am start -n tech.davshn.narsilwatch/.presentation.MainActivity --es topic narsil-... --es key ...
```

Add the **Narsil · Claude** tile and the **Claude 5 h** complication from the watch's tile and watch face editors.

## Development

```
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

- `domain/`: the state as Narsil publishes it, and how the watch words and colors it.
- `data/`: reading the ntfy topic, opening the seal, and keeping the last state.
- `tile/`, `complication/`, `presentation/`: the three surfaces.
- `sync/`: the 15-minute refresh (WorkManager) and the redraws.

## License

[GPL v3](LICENSE), like Narsil. Narsil's art is © its author and comes from the [narsil-pet](https://github.com/davshn/narsil-pet) repository.
