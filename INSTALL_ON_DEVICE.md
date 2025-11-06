# Install App on Physical Android Device

## Quick Installation Guide

Your app has been **successfully built**! The APK is located at:
```
D:\new working without modules\android\app\build\outputs\apk\debug\app-debug.apk
```

## Option 1: Install via File Transfer (Easiest)

1. **Copy the APK to your phone:**
   - Connect your phone via USB
   - Open File Explorer on Windows
   - Navigate to: `D:\new working without modules\android\app\build\outputs\apk\debug\`
   - Copy `app-debug.apk` to your phone (e.g., Downloads folder)

2. **Install on your phone:**
   - On your phone, go to the Downloads folder (or wherever you copied the APK)
   - Tap on `app-debug.apk`
   - If prompted, allow installation from this source
   - Tap "Install"
   - Once installed, open the app

## Option 2: Install via ADB (When connection is stable)

1. **Ensure device is connected:**
   ```powershell
   adb devices
   ```
   - You should see your device listed as "device" (not "offline" or "unauthorized")
   - If it says "unauthorized", accept the prompt on your phone
   - If offline, try unplugging and replugging your USB cable

2. **Install the APK:**
   ```powershell
   adb install -r "D:\new working without modules\android\app\build\outputs\apk\debug\app-debug.apk"
   ```

## Option 3: Rebuild and Install (Clean Method)

If you want to rebuild and install in one command:

1. **Make sure device is connected and authorized:**
   ```powershell
   adb devices
   ```

2. **Run the build and install:**
   ```powershell
   cd "d:\new working without modules"
   npx expo run:android --device
   ```

## Troubleshooting

### Device shows "offline" or "unauthorized"
- Unplug and replug your USB cable
- On your phone, revoke USB debugging authorizations (Developer Options > Revoke USB debugging authorizations)
- Reconnect and accept the authorization prompt
- Try a different USB cable or USB port

### "Installation failed" error
- Enable "Install unknown apps" for your file manager on your phone
- Go to Settings > Apps > Special app access > Install unknown apps
- Find your file manager and enable it

### App crashes on launch
- The app is a development build and needs Metro bundler running
- After installing, run: `npx expo start --dev-client`
- Make sure your phone and computer are on the same WiFi network
- The app will connect to Metro bundler automatically

## Running the App After Installation

1. **Start Metro Bundler:**
   ```powershell
   cd "d:\new working without modules"
   npx expo start --dev-client
   ```

2. **Open the app on your phone**
   - The app should connect automatically to Metro
   - Or scan the QR code shown in the terminal

## Notes

- This is a **development build**, not Expo Go
- The app runs as a standalone native app on your device
- You don't need Expo Go installed
- Metro bundler provides live reloading during development
- For production, you'd build a release APK with proper signing

## Production Build (Optional)

To create a production-ready APK:

```powershell
cd "d:\new working without modules\android"
./gradlew assembleRelease
```

The release APK will be at:
```
android\app\build\outputs\apk\release\app-release.apk
```

Note: For production, you should set up proper signing keys in `android/app/build.gradle`
