# Running Your App on Physical Android Device

## ✅ Your app is now installed and running!

The Metro bundler is currently running. Your app on your phone should automatically connect to it.

## What to do now:

1. **Open the app on your phone** - Look for "test-offline-sync" in your app drawer
2. **The app will connect automatically** to Metro bundler (if on the same WiFi)
3. **Or scan the QR code** shown in the terminal to launch the app

## For future runs:

### Method 1: Quick Run (If app is already installed)
```powershell
cd "d:\new working without modules"
npx expo start --dev-client
```
Then open the app on your phone.

### Method 2: Fresh Install
```powershell
cd "d:\new working without modules"
npx expo run:android --device
```
This rebuilds and reinstalls the app.

### Method 3: Use the PowerShell script
```powershell
.\run-android.ps1
```

## Key Points:

- ✅ **No Expo Go needed** - This is a standalone native app
- ✅ **Development build** - Includes dev tools and connects to Metro
- ✅ **Hot reloading** - Changes in your code will update automatically
- ✅ **Same WiFi required** - Your phone and computer must be on the same network

## Troubleshooting:

### App won't connect to Metro
- Ensure phone and computer are on the same WiFi
- Check if Metro is running (look for the QR code in terminal)
- Shake your phone to open the dev menu
- Select "Enter URL manually" and type: `10.168.1.80:8081`

### Need to rebuild
If you made native changes (android/, ios/, app.json, etc.):
```powershell
npx expo run:android --device
```

### App crashes
- Check Metro bundler terminal for errors
- Try clearing cache: `npx expo start --dev-client --clear`

## What's Different from Expo Go?

| Feature | Expo Go | Your App (Development Build) |
|---------|---------|------------------------------|
| Installation | Pre-installed from Play Store | Built and installed as APK |
| Native modules | Limited to Expo SDK | Any native module supported |
| App name | Shows as "Expo Go" | Shows as "test-offline-sync" |
| Custom native code | Not supported | Fully supported |
| Package name | io.expo.client | com.testoffline.sync |

## Metro Bundler Commands:

While Metro is running, you can press:
- `r` - Reload the app
- `m` - Toggle dev menu on device
- `j` - Open debugger
- `Ctrl+C` - Stop Metro bundler

---

**Current Status:** ✅ App is installed and Metro is running!
