# Screen Switch (مفتاح الشاشة)

Android app that keeps the screen on with one tap on a realistic switch. Published under the developer name **Sahla Labs** (contact: sahla.labs@gmail.com). Package `com.ialn3em.screenswitch`.

## Working with the owner
- The owner is new to code. Reply in Arabic (Gulf/Saudi dialect), short and step by step, one step at a time on setup tasks. Ask for a screenshot after each step.
- **Always show a mockup or sample image before changing anything visual.** The owner picks, then you build.
- The owner tests every build on a OnePlus phone (Android 16) using the **ScreenSwitch-Test** artifact.
- Work on branch `claude/video-stacking-product-uys1ys`. Merge into `main` only after the owner approves (they usually say "ادمج").
- After each push, wait for the GitHub Actions build and give the owner the run link.
- The repo is **public**: never commit the keystore, passwords, the owner's real name, address, phone or ID documents.

## Build
- No local SDK; GitHub Actions builds everything (`.github/workflows/build.yml`): `assembleRelease bundleRelease assembleUnlocked`.
- Artifacts: **ScreenSwitch** (release APK), **ScreenSwitch-Test** (`unlocked` build type: Pro always on, id suffix `.test`), **GooglePlay-AAB**.
- Signing comes from repo secrets KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD (already set).
- versionCode/versionName come from GITHUB_RUN_NUMBER (1.<run>).
- compile/target SDK 36, minSdk 26, AGP 8.10.1, Gradle 8.14.3, Kotlin 1.9.24. Plain `android.app.Activity`, no AppCompat.
- CI fails if the APK asks for INTERNET; the privacy policy promises no internet. Billing's INTERNET and ACCESS_NETWORK_STATE are removed with `tools:node="remove"`.

## Code map (`app/src/main/java/com/awake/app/`)
- `AwakeService` foreground service (specialUse) with an invisible overlay (FLAG_KEEP_SCREEN_ON). Timer, notification with countdown + Stop + "+10", stops at 10% battery when not charging.
- `MainActivity` switch, timer chips, models button (top-left), mute button (top-right), Pro card (`ProEntry`).
- `SwitchView` draws and animates the switch. `SwitchArt` paints the 9 models (also bitmaps for widget/gallery). `SwitchStyle` enum of models: CLASSIC is free, the other 8 are Pro.
- `StylesActivity` model gallery. `ClickSounds` per-model click sounds (SoundPool) plus the mute setting (prefs "settings", key `click_sound`).
- `SwitchTileService` Quick Settings tile, `SwitchWidget` home widget (follows the chosen model).
- `Pro` Google Play Billing, one-time product `pro_unlock`. `AutoAppsActivity` + `WatcherService` + `BootReceiver` Pro "automatic apps" (Usage access).
- Sounds in `res/raw/` were synthesized with numpy; the factory model uses variant "A" (heavy breaker), approved by the owner.

## Decisions already made (do not reopen)
- One app, free + in-app Pro upgrade (not two apps). No ads. No subscriptions.
- Rejected: "keep on while charging" Pro feature, lock-screen flashlight.
- Animations must look real: mechanical move without bounce, then the light ignites with a flicker. Factory buttons sink in place.

## Google Play status (update this section as things change)
Store kit is ready in `store/`: `LISTING.md` (texts), `images/`, `PRIVACY.md`, `PLAY_CONSOLE.md` (form answers), `TESTERS.md`.
- [x] Developer account created: personal account, developer name Sahla Labs, $25 paid, Android device verified.
- [ ] Identity verification: first attempt was rejected because the address proof did not match the payments profile address. Next attempt: a bank statement (dated within the last 60 days) whose address matches payments.google.com exactly. Attempts are limited.
- [ ] Verify contact phone (unlocks after identity is approved).
- [ ] Create the app, upload the AAB, fill App content from `PLAY_CONSOLE.md`, create `pro_unlock` product.
- [ ] Closed testing: 12+ testers for 14 days (owner is collecting tester Gmail addresses).
- [ ] Test a real purchase in closed testing (the app has no INTERNET permission; confirm billing still works).
- [ ] Apply for production.
- Note for the owner: with a personal account, the legal name and country are public, and the address becomes public once monetization is on.
