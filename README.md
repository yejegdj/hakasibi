# Qwen Touch AI — two variants

Android 11+; Java 17 / Gradle 8.10.2 / Android SDK 35.

## Safe APK (recommended)
- Uses **only INTERNET** permission. No Accessibility Service, overlay, automatic touch or background screenshots.
- You select a screenshot using Android's file picker. The app uploads it to the DashScope international endpoint to ask Qwen3-VL for one suggested action. **You perform the action yourself**.
- API key entered each time and kept in memory, never stored on disk. Use a key with low spending limits.
- This variant is an alternative for users blocked by Play Protect, **not a guarantee Google will permit installation**.

## Experimental APK
- Uses Android Accessibility Service for screenshots and gestures.
- **Each screenshot submission and each proposed gesture requires a separate button press** in an always-visible overlay. Max 8 steps per run.
- No view-hierarchy/active-window content access. No text input. App intentionally disallows unreviewed autonomous operation.
- Sensitive apps may still be visible in screenshot and should not be used. Play Protect might still block installation.

Neither variant is production-ready; they have not been tested on a physical device. Do NOT disable Play Protect to install them. Do not use them for banking, passwords, authentication or purchases. No APK re-signing/name change can guarantee security approval.

## API
Model: qwen3-vl-plus
Endpoint: https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions

## Build
GitHub Actions: Build Android APK; or:
`gradle assembleSafeDebug assembleAdvancedDebug`

Release signing requires a developer-managed, protected signing key. The uploaded files here are debug-signed, for testing only.
