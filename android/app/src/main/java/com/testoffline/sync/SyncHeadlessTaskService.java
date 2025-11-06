package com.testoffline.sync;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;

import javax.annotation.Nullable;

/**
 * Headless JS Task Service that runs JavaScript code in background
 * even when the app UI is not visible
 */
public class SyncHeadlessTaskService extends HeadlessJsTaskService {
    private static final String TAG = "SyncHeadlessTask";

    @Override
    protected @Nullable HeadlessJsTaskConfig getTaskConfig(Intent intent) {
        Log.d(TAG, "🚀 Creating headless JS task for background sync");

        Bundle extras = intent.getExtras();
        if (extras != null) {
            return new HeadlessJsTaskConfig(
                    "BackgroundSync",  // Task name registered in JS
                    Arguments.fromBundle(extras),
                    10000,  // timeout in ms (10 seconds)
                    true  // allow task in foreground
            );
        }
        return null;
    }
}
