// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.framework.util;

import android.app.Activity;
import android.app.Instrumentation;

import androidx.test.espresso.core.internal.deps.guava.collect.Iterables;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.mopub.common.MoPub;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.fail;

public class Utils {

    private static final int BUFFER_SIZE = 4 * 1024;
    private static final int DEFAULT_RETRY_COUNT = 6;
    private static final int SAMPLE_TIME_MS = 300;

    /**
     * When running an instrumentation test, this method will return the currently resumed activity.
     *
     * @return the resumed activity
     */
    public static Activity getCurrentActivity() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.waitForIdleSync();
        final Activity[] activity = new Activity[1];
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                Collection<Activity> activities = ActivityLifecycleMonitorRegistry
                        .getInstance().getActivitiesInStage(Stage.RESUMED);
                activity[0] = Iterables.getOnlyElement(activities);
            }
        });
        return activity[0];
    }

    public static List<String> getLogs() {
        Process logcat;
        final List<String> log = new ArrayList<>();
        try {
            logcat = Runtime.getRuntime().exec(new String[]{"logcat", "-d"});
            BufferedReader br =
                    new BufferedReader(
                            new InputStreamReader(logcat.getInputStream()),
                            BUFFER_SIZE
                    );
            String line;
            while ((line = br.readLine()) != null) {
                log.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return log;
    }

    public static void waitFor(final int waitInMills) {
        try {
            Thread.sleep(waitInMills);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    }

    public static void waitForSdkToInitialize() {
        int i = 0;
        while (i++ < DEFAULT_RETRY_COUNT && !MoPub.isSdkInitialized()) {
            Actions.loopMainThreadAtLeast(SAMPLE_TIME_MS);
        }
        if (!MoPub.isSdkInitialized()) {
            fail("SDK failed to initialize");
        }
    }
}
