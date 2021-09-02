// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.VisibleForTesting;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton to cache ad request rate limit time interval and reason.
 */
public class RequestRateTracker {

    public static class TimeRecord {
        final long mBlockStartTime;
        public final int mBlockIntervalMs;
        @NonNull
        public final String mReason;

        TimeRecord(int interval, @Nullable final String reason) {
            mBlockStartTime = currentTimeMs();
            mBlockIntervalMs = interval;
            mReason = reason == null ? "unknown" : reason;
        }

        long getTargetTime() {
            return mBlockStartTime + mBlockIntervalMs;
        }
    }

    @NonNull
    private Map<String, TimeRecord> mTimeRecordMap;

    private static class Helper {
        @NonNull
        private static RequestRateTracker sInstance = new RequestRateTracker();
    }

    RequestRateTracker() {
        mTimeRecordMap = Collections.synchronizedMap(new HashMap<String, TimeRecord>());
    }

    @NonNull
    public static RequestRateTracker getInstance() {
        return Helper.sInstance;
    }

    void registerRateLimit(@Nullable final String adUnit, @Nullable final Integer blockIntervalMs, @Nullable final String reason) {
        if (TextUtils.isEmpty(adUnit)) {
            return;
        }

        if (blockIntervalMs != null && blockIntervalMs > 0) {
            mTimeRecordMap.put(adUnit, new TimeRecord(blockIntervalMs, reason));
        } else {
            mTimeRecordMap.remove(adUnit);
        }
    }

    boolean isBlockedByRateLimit(@Nullable final String adUnitId) {
        return getTimeUntilLimitEnds(adUnitId) > 0;
    }

    @Nullable
    public TimeRecord getRecordForAdUnit(@Nullable final String adUnitId) {
        return mTimeRecordMap.get(adUnitId);
    }

    private long getTimeUntilLimitEnds(@Nullable final String adUnitId) {
        final TimeRecord record = mTimeRecordMap.get(adUnitId);
        if (record == null) {
            return 0;
        }

        return record.getTargetTime() - currentTimeMs();
    }

    private static long currentTimeMs() {
        return SystemClock.elapsedRealtime();
    }

    @Deprecated
    @VisibleForTesting
    static void setInstance(@NonNull RequestRateTracker mockInstance) {
        Helper.sInstance = mockInstance;
    }
}
