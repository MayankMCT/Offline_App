# 🚀 Quick Start Guide

## Installation Steps

### 1. Navigate to the test project
```bash
cd d:\vs\test-offline-sync
```

### 2. Install dependencies
```bash
npm install
```

### 3. Start the development server
```bash
npx expo start
```

### 4. Run on your device
- Press `a` for Android
- Press `i` for iOS
- Or scan the QR code with Expo Go app

## Important Notes

⚠️ **Assets Missing**: The app references icon/splash images that don't exist yet. You can:
- Ignore the warnings (app will still work)
- Or download Expo default assets
- Or create your own 1024x1024 PNG files

## Testing the App

### Quick Test (5 minutes):
1. Open the app
2. Turn OFF WiFi
3. Add a task (e.g., "Fix Pump #123")
4. Notice it saves with "⏳ Pending" status
5. Turn ON WiFi
6. Click "🔄 Manual Sync" button
7. Task should now show "✅ Synced"

### Background Test (20 minutes):
1. Turn OFF WiFi
2. Add several tasks
3. Close the app completely (swipe away)
4. Turn ON WiFi
5. Wait 15-20 minutes
6. Open app again
7. Tasks should be synced automatically!

## Troubleshooting

### "Module not found" errors?
Run: `npm install` again

### App not starting?
Run: `npx expo start --clear`

### Background sync not working?
- Check phone settings → Enable background refresh
- iOS: Background tasks may take 30 min instead of 15 min
- Android: Disable battery optimization for the app

## File Structure

```
test-offline-sync/
├── app/
│   ├── _layout.tsx          ← Registers background sync
│   └── index.tsx            ← Main UI (add/view tasks)
│
├── database/
│   ├── database.ts          ← WatermelonDB setup
│   ├── schema.ts            ← Database tables definition
│   └── models/
│       └── Task.ts          ← Task model (your WorkOrder/Asset)
│
├── services/
│   ├── syncService.ts       ← Sync logic (talks to API)
│   └── backgroundSync.ts    ← Background task (runs every 15 min)
│
├── IMPLEMENTATION_GUIDE.md  ← Detailed technical explanation
├── SIMPLE_GUIDE.md          ← Beginner-friendly explanation
└── README.md                ← Main documentation
```

## Next Steps

1. ✅ Test this sample app
2. ✅ Understand how it works
3. ✅ Read SIMPLE_GUIDE.md for concepts
4. ✅ Read IMPLEMENTATION_GUIDE.md for details
5. ✅ Implement in your real EAM app!

---

Need help? Check the console logs - everything is logged! 🔍
