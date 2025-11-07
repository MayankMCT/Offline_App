package com.testoffline.sync;

import android.content.Intent;
import android.os.Bundle;
import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;
import javax.annotation.Nullable;

public class SyncHeadlessTaskService extends HeadlessJsTaskService {

    @Override
    protected @Nullable HeadlessJsTaskConfig getTaskConfig(Intent intent) {
        Bundle extras = intent.getExtras();
        return new HeadlessJsTaskConfig(
            "BackgroundSync", // Name of the JS task
            extras != null ? Arguments.fromBundle(extras) : Arguments.createMap(),
            30000, // Timeout for the task in milliseconds
            true // Whether or not the task is allowed to run in foreground
        );
    }
}
