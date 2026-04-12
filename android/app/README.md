# PNA Android Service

Android app for **call screening + backend integration**.
Detects incoming calls and sends the phone number to the backend.

## Stack

* Kotlin
* Android SDK (`minSdk 27`, `targetSdk 36`)
* Retrofit + OkHttp
* Coroutines
* Google Identity (ID token)
* `CallScreeningService` API

## Quick Start

Open in **Android Studio**, then:

```bash
./gradlew build
```

Create local config:

```bash
cp dev.properties.example dev.properties
```

> `dev.properties` **must be created from** `dev.properties.example`.

Run a **Gradle Sync** after creating it.

## Configuration

Local environment values live in:

```bash
dev.properties
```

Typical values:

* backend base URL
* auth config


## Networking

Uses Retrofit + OkHttp.

Key point:

* backend must be reachable from the device
* base URL must match your environment

## Emulator vs Real Device

### Emulator

* `localhost` ≠ your machine
* use ADB host mapping (e.g. `10.0.2.2`) to reach backend

### Real device

* no ADB port mapping
* backend must be reachable via:

    * local IP (e.g. `192.168.x.x`) or
    * public endpoint

## Notes

* Requires Call Screening role
* Google OAuth must be configured for **Android client**
* Behavior may differ between emulator and real device
* Most issues are networking or auth related
