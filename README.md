# DAON

[한국어](README.ko.md) · **English**

A full-screen, always-on kiosk dashboard for an **Amazon Fire HD 10 (2019)** tablet: a large clock, the current month calendar, and live weather on a single dark screen — designed to be read from a few meters away.

## Status

**v0.2.0-dev** — calendar-first redesign implemented; on-device verification is in progress (T-006/T-007/T-021). The v0.1 baseline was verified on the target device: Fire HD 10 (2019), Fire OS 7.3.1.2 (API 28), 1920×1200 landscape, immersive full screen. Product name: **DAON** (다온).

| Metric | Budget | v0.1 | v0.2-dev |
|---|---|---|---|
| Cold start | ≤ 2.0s | 1.54s | 1.85–1.90s |
| Memory (PSS) | ≤ 250MB | 65.5MB | ~94MB (while running) |

## v0.2 — calendar-first kiosk

Calendar 70% + clock/weather 30%, lunar dates, Korean public holidays, 24 solar terms with farming notes, and an idle rotation calendar → weather → clock. Tap a date for a 520dp panel with the solar term, farming tasks, holidays, and events. Weather shows current, 3-hourly, and 7-day forecasts with rain chance and rain/snow/thunder effects. The clock face can be a round analog, large digital, or combined.

Events can be added on the tablet and, when the device has a calendar app, read from the device calendar (read-only). Kiosk settings add screen pinning and optional start-on-boot.

- Interactive mockup: [`docs/mockups/v0.2/index.html`](docs/mockups/v0.2/index.html)
- v0.2 captures: [`docs/screenshots/04-v0.2-dashboard-ko.png`](docs/screenshots/04-v0.2-dashboard-ko.png) … `10-v0.2-settings-ko.png`
- Local events: SharedPreferences JSON. Device calendar: `READ_CALENDAR`, ±12 months, unavailable on Fire OS without a calendar app.
- Kiosk: Android screen pinning; device owner is optional (`adb shell dpm set-device-owner com.borasarang.daon/.admin.AdminReceiver`).

## Screenshots

| v0.2 calendar | Date detail | Weather | Clock | Settings |
|---|---|---|---|---|
| ![calendar](docs/screenshots/04-v0.2-dashboard-ko.png) | ![detail](docs/screenshots/05-detail-term-ko.png) | ![weather](docs/screenshots/08-v0.2-weather-ko.png) | ![clock](docs/screenshots/09-v0.2-clock-ko.png) | ![settings](docs/screenshots/10-v0.2-settings-ko.png) |

v0.1: ![ko](docs/screenshots/01-dashboard-ko.png) · ![en](docs/screenshots/02-dashboard-en.png)


## Target device

| Item | Value |
|---|---|
| Device | Amazon Fire HD 10 (2019, 9th gen) |
| OS | Fire OS 7 (Android 9, API 28) |
| Display | 1920×1200, landscape, always-on |
| Google services | **Not available** (no GMS) |

The app avoids Google Play Services entirely. Weather comes from [Open-Meteo](https://open-meteo.com/) (no API key). Weather uses the device's own location when the tablet can provide one; when it cannot, a neighborhood searched once in Settings (via [Nominatim](https://nominatim.openstreetmap.org/)) is used instead. The Fire HD 10 (2019) has no GPS and its network location provider never returns a fix, so on that unit the saved neighborhood is what drives the weather.

## Features

- Clock: large time with seconds, weekday and date; 12/24-hour option; round analog / large digital / combined face
- Calendar: month grid with lunar dates, Korean public holidays, 24 solar terms, and farming notes; today highlighted; month navigation
- Date detail: 520dp panel with the solar term, farming tasks, holidays, and that day's events
- Events: add/delete local events per day; read device-calendar events when the tablet has a calendar app
- Weather: current, feels-like, humidity, wind, rain chance, 3-hourly and 7-day forecasts, farming note, rain/snow/thunder effects, 15-minute refresh
- Rotation: calendar → weather → clock with per-screen dwell time and two tap behaviours
- Kiosk shell: landscape lock, immersive full screen, keep-screen-on, optional screen pinning and start-on-boot
- Settings panel (in-app): weather point (device location or saved neighborhood), events, clock face, text size, rotation, °C/°F, 12/24-hour, language (System / English / 한국어)

## Requirements

- JDK 17+ (project builds with Temurin 27)
- Android SDK: platform 36, build-tools 36
- Gradle wrapper 9.5.0 (bundled)

## Build

```bash
./gradlew assembleDebug
# or
./build_and_run.sh debug android
```

Install on a connected device:

```bash
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
```

## Test

```bash
./gradlew test
scripts/check-i18n.sh
```

## Documentation

- `docs/plans/PLAN_v0.1_android.md` — v0.1 scope, budgets, milestones
- `docs/plans/PLAN_v0.2_android.md` — v0.2 calendar redesign (implemented)
- `docs/mockups/v0.2/` — interactive mockup and captures
- `docs/DESIGN.md` — design tokens and kiosk layout
- `docs/appstore/` — Amazon Appstore listing (en/ko) and data safety
- `docs/TODO.md` — follow-up work
- `docs/CHANGELOG.md` — change log

## License

Private project. All rights reserved.
