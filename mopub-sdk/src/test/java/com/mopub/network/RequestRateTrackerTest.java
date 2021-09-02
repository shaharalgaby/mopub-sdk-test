// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import android.os.SystemClock;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.network.RequestRateTracker.TimeRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(SdkTestRunner.class)
public class RequestRateTrackerTest {
    private static final long DELTA = 100;
    private static final int BACKOFF_TIME = 88;
    private static final String AD_UNIT_1 = "ad_unit_1";
    private static final String AD_UNIT_2 = "ad_unit_2";
    private static final String REASON = "backoff_reason";
    private static final String REASON2 = "reason2";
    private static final String NO_BACKOFF = "no_backoff";

    private RequestRateTracker subject;

    @Before
    public void setup() {
        subject = RequestRateTracker.getInstance();
    }

    @After
    public void tearDown() {
        clearRequestRateTracker();
    }

    @Test
    public void registerRateLimit_withValidData_shouldSetRateLimitForAdUnit() {
        long currentTime = SystemClock.uptimeMillis();

        subject.registerRateLimit(AD_UNIT_1, BACKOFF_TIME, REASON);

        TimeRecord record = subject.getRecordForAdUnit(AD_UNIT_1);
        assertNotNull(record);
        assertThat(record.mBlockIntervalMs).isEqualTo(BACKOFF_TIME);
        assertThat(record.mReason).isEqualTo(REASON);
        assertThat(record.mBlockStartTime - currentTime).isLessThan(DELTA);
    }

    @Test
    public void registerRateLimit_withEmptyAdUnit_shouldDoNothing() {
        long currentTime = SystemClock.uptimeMillis();

        subject.registerRateLimit(AD_UNIT_1, BACKOFF_TIME, REASON);
        subject.registerRateLimit("", BACKOFF_TIME, REASON);

        assertNull(subject.getRecordForAdUnit(""));

        TimeRecord record = subject.getRecordForAdUnit(AD_UNIT_1);
        assertNotNull(record);
        assertThat(record.mBlockIntervalMs).isEqualTo(BACKOFF_TIME);
        assertThat(record.mReason).isEqualTo(REASON);
        assertThat(record.mBlockStartTime - currentTime).isLessThan(DELTA);
    }

    @Test
    public void registerRateLimit_withNegativeBackoffTime_shouldResetAdUnit() {
        long currentTime = SystemClock.uptimeMillis();

        subject.registerRateLimit(AD_UNIT_1, BACKOFF_TIME, REASON);

        TimeRecord record = subject.getRecordForAdUnit(AD_UNIT_1);
        assertNotNull(record);
        assertThat(record.mBlockIntervalMs).isEqualTo(BACKOFF_TIME);
        assertThat(record.mReason).isEqualTo(REASON);
        assertThat(record.mBlockStartTime - currentTime).isLessThan(DELTA);

        subject.registerRateLimit(AD_UNIT_1, -1, REASON2);
        assertNull(subject.getRecordForAdUnit(AD_UNIT_1));
    }

    @Test
    public void registerRateLimit_withTwoAdUnits_shouldStoreBoth() {
        long currentTime = SystemClock.uptimeMillis();

        subject.registerRateLimit(AD_UNIT_1, BACKOFF_TIME, REASON);
        subject.registerRateLimit(AD_UNIT_2, 66, REASON2);

        TimeRecord record1 = subject.getRecordForAdUnit(AD_UNIT_1);
        assertNotNull(record1);
        assertThat(record1.mBlockIntervalMs).isEqualTo(BACKOFF_TIME);
        assertThat(record1.mReason).isEqualTo(REASON);
        assertThat(record1.mBlockStartTime - currentTime).isLessThan(DELTA);

        TimeRecord record2 = subject.getRecordForAdUnit(AD_UNIT_2);
        assertNotNull(record2);
        assertThat(record2.mBlockIntervalMs).isEqualTo(66);
        assertThat(record2.mReason).isEqualTo(REASON2);
        assertThat(record2.mBlockStartTime - currentTime).isLessThan(DELTA);
    }

    @Test
    public void registerRateLimit_withTwoAdUnits_deleteSecondAdUnit_shouldNotDeleteFirst() {
        long currentTime = SystemClock.uptimeMillis();

        subject.registerRateLimit(AD_UNIT_1, BACKOFF_TIME, REASON);
        subject.registerRateLimit(AD_UNIT_2, 66, REASON2);

        TimeRecord record1 = subject.getRecordForAdUnit(AD_UNIT_1);
        assertNotNull(record1);
        assertThat(record1.mBlockIntervalMs).isEqualTo(BACKOFF_TIME);
        assertThat(record1.mReason).isEqualTo(REASON);
        assertThat(record1.mBlockStartTime - currentTime).isLessThan(DELTA);

        TimeRecord record2 = subject.getRecordForAdUnit(AD_UNIT_2);
        assertNotNull(record2);
        assertThat(record2.mBlockIntervalMs).isEqualTo(66);
        assertThat(record2.mReason).isEqualTo(REASON2);
        assertThat(record2.mBlockStartTime - currentTime).isLessThan(DELTA);

        subject.registerRateLimit(AD_UNIT_2, 0, "");

        assertNull(subject.getRecordForAdUnit(AD_UNIT_2));
        assertThat(subject.getRecordForAdUnit(AD_UNIT_1)).isEqualTo(record1);
    }

    @Test
    public void getRecordForAdUnit_withNullAdUnitId_shouldReturnNull() {
        subject.registerRateLimit(AD_UNIT_1, BACKOFF_TIME, REASON);

        assertNull(subject.getRecordForAdUnit(null));
    }

    @Test
    public void getRecordForAdUnit_withEmptyStringAdUnitId_shouldReturnNull() {
        subject.registerRateLimit(AD_UNIT_1, BACKOFF_TIME, REASON);

        assertNull(subject.getRecordForAdUnit(""));
    }

    @Test
    public void isBlockedByRateLimit_afterRateLimitSetToZero_shouldReturnFalse() {
        assertThat(subject.isBlockedByRateLimit(AD_UNIT_1)).isFalse();

        subject.registerRateLimit(AD_UNIT_1, 0, NO_BACKOFF);

        assertThat(subject.isBlockedByRateLimit(AD_UNIT_1)).isFalse();
    }

    @Test
    public void isBlockedByRateLimit_afterRateLimitSet_shouldReturnTrue() {
        assertThat(subject.isBlockedByRateLimit(AD_UNIT_1)).isFalse();

        subject.registerRateLimit(AD_UNIT_1, BACKOFF_TIME, REASON);

        assertThat(subject.isBlockedByRateLimit(AD_UNIT_1)).isTrue();
    }

    @Test
    public void isBlockedByRateLimit_afterFailedResponse_afterSuccessResponse_shouldReturnTrue() {
        assertThat(subject.isBlockedByRateLimit(AD_UNIT_1)).isFalse();

        subject.registerRateLimit(AD_UNIT_1, BACKOFF_TIME, REASON);
        subject.registerRateLimit(AD_UNIT_1, 0, NO_BACKOFF);

        assertThat(subject.isBlockedByRateLimit(AD_UNIT_1)).isFalse();
    }

    @Test
    public void isBlockedByRateLimit_withTwoAdUnits_adUnitsDoNotInterfere() {
        assertThat(subject.isBlockedByRateLimit(AD_UNIT_1)).isFalse();
        assertThat(subject.isBlockedByRateLimit(AD_UNIT_2)).isFalse();

        subject.registerRateLimit(AD_UNIT_1, BACKOFF_TIME, REASON);
        assertThat(subject.isBlockedByRateLimit(AD_UNIT_1)).isTrue();
        assertThat(subject.isBlockedByRateLimit(AD_UNIT_2)).isFalse();

        subject.registerRateLimit(AD_UNIT_2, 0, NO_BACKOFF);
        assertThat(subject.isBlockedByRateLimit(AD_UNIT_1)).isTrue();
        assertThat(subject.isBlockedByRateLimit(AD_UNIT_2)).isFalse();

        subject.registerRateLimit(AD_UNIT_1, 0, NO_BACKOFF);
        subject.registerRateLimit(AD_UNIT_2, BACKOFF_TIME, REASON);
        assertThat(subject.isBlockedByRateLimit(AD_UNIT_1)).isFalse();
        assertThat(subject.isBlockedByRateLimit(AD_UNIT_2)).isTrue();
    }

    @Test
    public void isBlockedByRateLimit_afterFailedResponse_afterTimeoutExpires_returnsTrue() {
        subject.registerRateLimit(AD_UNIT_1, BACKOFF_TIME, REASON);

        assertThat(subject.isBlockedByRateLimit(AD_UNIT_1)).isTrue();
        Robolectric.getForegroundThreadScheduler().advanceBy(BACKOFF_TIME * 2, TimeUnit.MILLISECONDS);
        assertThat(subject.isBlockedByRateLimit(AD_UNIT_1)).isFalse();
    }

    /**
     * Utility functions
     */
    public static void prepareRequestRateTracker(String adUnitId, Integer backoffMs, String backoffReason) {
        RequestRateTracker.getInstance().registerRateLimit(adUnitId, backoffMs, backoffReason);
    }

    public static void clearRequestRateTracker() {
        RequestRateTracker.setInstance(new RequestRateTracker());
    }
}
