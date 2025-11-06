# Offline Sync Test App 📱

A simple React Native app to test offline synchronization functionality before implementing in production.

## 🎯 What This App Does

1. **Works Offline**: Add and edit tasks even without internet
2. **Auto Sync**: Automatically syncs every 15 minutes in the background
3. **Manual Sync**: Button to force sync when needed
4. **Smart Storage**: Uses WatermelonDB to store data locally

## 🚀 How to Run

### Step 1: Install Dependencies
```bash
npm install
```

### Step 2: Start the App
```bash
npm start
```

Then press:
- `a` for Android
- `i` for iOS

## 🧪 How to Test

### Test Offline Mode:
1. Turn OFF WiFi on your phone
2. Add some tasks in the app
3. Notice they save with "⏳ Pending" status
4. Turn WiFi back ON
5. Click "Manual Sync" button
6. Tasks should show "✅ Synced"

### Test Background Sync:
1. Add tasks while offline
2. Close the app completely
3. Turn WiFi ON
4. Wait 15-20 minutes (background sync runs)
5. Open app again - tasks should be synced

## 📁 Project Structure

```
test-offline-sync/
├── app/
│   ├── _layout.tsx          # App layout (registers background sync)
│   └── index.tsx            # Main screen (add/view tasks)
├── database/
│   ├── database.ts          # WatermelonDB setup
│   ├── schema.ts            # Database schema
│   └── models/
│       └── Task.ts          # Task model
├── services/
│   ├── syncService.ts       # Sync logic (send to server)
│   └── backgroundSync.ts    # Background task setup
└── package.json
```

## 🔧 How It Works (Simple Explanation)

### 1. **WatermelonDB** (Local Storage)
- Stores tasks on your phone
- Each task has a `syncStatus`: "pending" or "synced"
- When you add/edit offline → status = "pending"

### 2. **NetInfo** (Check Internet)
- Constantly monitors if you have internet
- Shows green/red status at top of app

### 3. **Background Fetch** (Auto Sync)
- Runs every 15 minutes automatically
- Checks: "Do I have internet?"
- If YES → Sync all "pending" tasks
- If NO → Try again in 15 minutes

### 4. **Sync Flow**
```
User adds task offline
         ↓
Save to WatermelonDB (status: pending)
         ↓
Background task runs every 15 min
         ↓
Has internet? → YES → Send to server → Update status to "synced"
              ↘ NO → Do nothing, retry later
```

## ⚠️ Important Notes

### Limitations:
- **iOS**: Background sync may run every 15-30 minutes (not exact)
- **Battery Saver**: Android may stop background tasks
- **User Settings**: Users can disable background refresh
- **Not Guaranteed**: OS decides when to run background tasks

### Testing Tips:
- Use "Manual Sync" button for immediate testing
- Background sync is best-effort, not guaranteed
- Check phone settings → Allow background refresh

## 🔄 Implementing in Your Real App

To use this in your EAM app:

1. **Replace Fake API**: Change `FAKE_API_URL` in `syncService.ts` to your real EAM API
2. **Add Your Models**: Create models for WorkOrder, Asset, etc.
3. **Update Sync Logic**: Modify `syncPendingChanges()` to match your API structure
4. **Add Authentication**: Include auth tokens in API calls
5. **Error Handling**: Add retry logic for failed syncs

## 📚 Libraries Used

| Library | Purpose | Why We Need It |
|---------|---------|----------------|
| WatermelonDB | Local database | Store data offline |
| NetInfo | Check internet | Know when we can sync |
| Background Fetch | Run tasks in background | Auto-sync every 15 min |

## 🐛 Troubleshooting

**Background sync not working?**
- Check phone settings → Enable background refresh
- Make sure app isn't in battery optimization
- iOS: Background sync is less frequent than Android

**Tasks not syncing?**
- Check internet connection
- Look at console logs for errors
- Try manual sync button first

## 📝 Next Steps

Once this works, implement the same pattern in your real app:
1. Add WatermelonDB models for your data (WorkOrder, Asset)
2. Mark changes as "pending" when offline
3. Use the same background sync setup
4. Connect to your real EAM API

---

**Need Help?** Check the code comments - everything is explained! 🚀
