// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
public class AdvertisingIdTest {
    private static final String MOPUB_ID = "test-id-mopub";
    private static final String ANDROID_ID = "test-id-android";

    private AdvertisingId subject;

    @Before
    public void setup() {
    }

    @Test
    public void constructor_shouldInitializeCorrectly() {
        subject = new AdvertisingId(ANDROID_ID, MOPUB_ID, false);
        assertThat(subject.mAdvertisingId).isEqualTo(ANDROID_ID);
        assertThat(subject.mMopubId).isEqualTo(MOPUB_ID);
        assertThat(subject.mDoNotTrack).isFalse();

        subject = new AdvertisingId(ANDROID_ID, MOPUB_ID, true);
        assertThat(subject.mDoNotTrack).isTrue();
        // return IFA even when DoNotTrack is true
        assertThat(subject.getIfaWithPrefix()).isEqualTo("ifa:" + ANDROID_ID);
    }

    @Test
    public void getIdWithPrefix_whenDoNotTrackFalse_shouldReturnIfaString() {
        subject = new AdvertisingId(ANDROID_ID, MOPUB_ID, false);
        assertThat(subject.getIdWithPrefix(true)).isEqualTo("ifa:" + ANDROID_ID);
    }

    @Test
    public void getIdWithPrefix_whenAndroidIdUnavailable_shouldReturnMopubString() {
        subject = new AdvertisingId("", MOPUB_ID, false);
        assertThat(subject.getIdWithPrefix(true)).isEqualTo("mopub:" + MOPUB_ID);
    }

    @Test
    public void getIdWithPrefix_whenUserConsentFalse_shouldReturnMopubString() {
        subject = new AdvertisingId(ANDROID_ID, MOPUB_ID, false);
        assertThat(subject.getIdWithPrefix(false)).isEqualTo("mopub:" + MOPUB_ID);
    }

    @Test
    public void getIdWithPrefix_whenUserConsentTrue_shouldReturnIfaString() {
        subject = new AdvertisingId(ANDROID_ID, MOPUB_ID, false);
        assertThat(subject.getIdWithPrefix(true)).isEqualTo("ifa:" + ANDROID_ID);
    }

    @Test
    public void getIdWithPrefix_whenLimitAdTrackingIsTrue_shouldNotDependOnConsent() {
        subject = new AdvertisingId(ANDROID_ID, MOPUB_ID, true);

        assertThat(subject.getIdWithPrefix(true)).isEqualTo("mopub:" + MOPUB_ID);
        assertThat(subject.getIdWithPrefix(false)).isEqualTo("mopub:" + MOPUB_ID);
    }

    @Test
    public void getIdentifier_whenDoNotTrackIsTrue_shouldReturnMoPubid() {
        subject = new AdvertisingId(ANDROID_ID, MOPUB_ID, true);

        assertThat(subject.getIdentifier(true)).isEqualTo(MOPUB_ID);
        assertThat(subject.getIdentifier(false)).isEqualTo(MOPUB_ID);
    }

    @Test
    public void getIdentifier_whenDoNotTrackIsFalse_shouldAnalyzeConsent() {
        subject = new AdvertisingId(ANDROID_ID, MOPUB_ID, false);
        
        assertThat(subject.getIdentifier(true)).isEqualTo(ANDROID_ID);
        assertThat(subject.getIdentifier(false)).isEqualTo(MOPUB_ID);
    }

    @Test
    public void generateIdString_lengthIs16x2plus4() {
        String uuid = AdvertisingId.generateIdString();
        assertThat(uuid.length()).isEqualTo(36);
    }
}
