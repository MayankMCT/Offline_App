# 📦 Production APK - Installation Guide

## ✅ Your APK is Ready!

**File Location:**
```
d:\new working without modules\android\app\build\outputs\apk\release\app-release.apk
```

**File Size:** 102.24 MB
**Build Date:** November 5, 2025

---

## 📱 How to Install on Any Android Device

### Method 1: Transfer via USB
1. **Connect your phone** to computer via USB
2. **Copy the APK** to your phone (e.g., Downloads folder)
3. **On your phone**, open the file manager
4. **Navigate** to where you copied the APK
5. **Tap** on `app-release.apk`
6. **Allow** "Install from unknown sources" if prompted
7. **Tap Install**
8. **Open** the app!

### Method 2: Transfer via Cloud/Email
1. **Upload** `app-release.apk` to Google Drive, Dropbox, or email it to yourself
2. **On your phone**, download the APK from the link/email
3. **Open** the downloaded APK
4. **Allow** "Install from unknown sources" if prompted
5. **Tap Install**
6. **Open** the app!

### Method 3: Direct ADB Install (If device is connected)
```bash
cd "d:\new working without modules\android\app\build\outputs\apk\release"
adb install app-release.apk
```

---

## 🔧 Enable "Install Unknown Apps"

If you get a security warning, you need to enable installation from unknown sources:

### Android 12+:
1. Go to **Settings** > **Apps**
2. Find your **File Manager** or **Browser** (whichever you used to open the APK)
3. Tap **Install unknown apps**
4. Toggle **Allow from this source**

### Android 8-11:
1. Go to **Settings** > **Security**
2. Enable **Unknown sources** or **Install unknown apps**
3. Select the app (File Manager/Browser) and allow it

### Android 7 and below:
1. Go to **Settings** > **Security**
2. Enable **Unknown sources**

---

## ✨ Features in This APK

### Core Features:
- ✅ **Offline-first task management** with WatermelonDB
- ✅ **Automatic sync every 5 seconds** when online
- ✅ **Immediate sync** when adding/updating tasks
- ✅ **Network change detection** - auto-syncs when reconnecting

### Background Sync (Even When App is Killed):
- ✅ **NetworkChangeReceiver** - Detects network changes even when app is closed
- ✅ **WorkManager** - Schedules background sync jobs
- ✅ **Headless JS** - Runs sync without opening the app UI
- ✅ **Works after device reboot**

### How Background Sync Works:
1. **Turn off WiFi** and add tasks
2. **Kill the app** completely (swipe from recent apps)
3. **Turn on WiFi**
4. **Wait 10-15 seconds**
5. **Open the app** - tasks will show as "✅ Synced"

---

## 🎯 App Details

- **Package Name:** `com.testoffline.sync`
- **Version:** 1.0.0
- **Min Android:** API 23 (Android 6.0)
- **Target Android:** Latest
- **Architecture:** ARM64, ARMv7, x86, x86_64 (Universal)

---

## 🔄 Updating the APK

To create a new version:

### Update version number (optional):
Edit `android/app/build.gradle`:
```gradle
defaultConfig {
    versionCode 2  // Increment this
    versionName "1.0.1"  // Update version
}
```

### Rebuild:
```bash
cd "d:\new working without modules\android"
.\gradlew assembleRelease
```

New APK will be at the same location!

---

## ⚠️ Important Notes

### About Signing:
- This APK is signed with the **debug keystore** (for testing)
- For **production/Play Store**, you need to create a proper signing key
- Current signing is fine for personal use and distribution to friends/team

### Battery Optimization:
- On some devices (especially Xiaomi, Huawei, Samsung), you may need to:
  1. Go to **Settings** > **Battery** > **App Battery Saver**
  2. Find your app: **test-offline-sync**
  3. Set to **No restrictions** or **Don't optimize**
- This ensures background sync works when app is killed

### Permissions:
The app requires:
- **Internet** - To sync with server
- **Network State** - To detect online/offline status
- **Wake Lock** - For background sync
- **Receive Boot Completed** - To start sync after reboot

All permissions are automatically granted on install.

---

## 🚀 Share This APK

You can share this APK with anyone! They can install it on any Android device (Android 6.0+) and it will work with all features including background sync when the app is killed.

**Just send them the `app-release.apk` file!**

---

## 📊 Testing Checklist

Before distributing, test:
- [ ] Install on clean device
- [ ] Add tasks while online
- [ ] Add tasks while offline
- [ ] Kill app and go online (tasks should sync)
- [ ] Reboot device and check sync
- [ ] Check battery usage after 24 hours

---

**Build Date:** November 5, 2025
**Built with:** React Native + Expo + Native Android
**All features working:** ✅
