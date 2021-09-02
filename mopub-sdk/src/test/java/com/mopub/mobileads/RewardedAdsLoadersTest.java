// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class RewardedAdsLoadersTest {
    private static final String AD_UNIT_ID = "ad_unit_id";

    @Mock
    private MoPubRewardedAdManager mockRewardedAdManager;
    @Mock
    private AdLoaderRewardedAd mockLoaderRewardedAd;

    private Activity activity;
    private RewardedAdsLoaders subject;

    @Before
    public void setup() {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        subject = new RewardedAdsLoaders(mockRewardedAdManager);
        subject.getLoadersMap().put(AD_UNIT_ID, mockLoaderRewardedAd);

    }

    @Test
    public void isLoading_fullTest() {
        // when ad unit loader is present
        subject.isLoading(AD_UNIT_ID);
        verify(mockLoaderRewardedAd).isRunning();

        // when ad unit loader is not present
        reset(mockLoaderRewardedAd);
        subject.isLoading("invalid_ad_unit_id");
        verify(mockLoaderRewardedAd, never()).isRunning();
    }

    @Test
    public void markFailed_fullTest() {
        // when ad unit is not present
        subject.markFail("invalid_ad_unit_id");
        assertThat(subject.getLoadersMap().size()).isEqualTo(1);

        // when ad unit is present
        subject.markFail(AD_UNIT_ID);
        assertThat(subject.getLoadersMap().size()).isEqualTo(0);
    }

    @Test
    public void markPlayed_fullTest() {
        // when ad unit is not present
        subject.markPlayed("invalid_ad_unit_id");
        assertThat(subject.getLoadersMap().size()).isEqualTo(1);

        // when ad unit is present
        subject.markPlayed(AD_UNIT_ID);
        assertThat(subject.getLoadersMap().size()).isEqualTo(0);
    }

    @Test
    public void onRewardedAdStarted_fullTest() {
        // when ad unit is not present
        subject.onRewardedAdStarted("invalid_ad_unit_id", activity);
        verify(mockLoaderRewardedAd, never()).trackImpression(any(Context.class));

        // when ad unit is present
        subject.onRewardedAdStarted(AD_UNIT_ID, activity);
        verify(mockLoaderRewardedAd).trackImpression(eq(activity));
    }

    @Test
    public void onRewardedAdClicked_fullTest() {
        // when ad unit is not present
        subject.onRewardedAdClicked("invalid_ad_unit_id", activity);
        verify(mockLoaderRewardedAd, never()).trackClick(any(Context.class));

        // when ad unit is present
        subject.onRewardedAdClicked(AD_UNIT_ID, activity);
        verify(mockLoaderRewardedAd).trackClick(eq(activity));
    }

    @Test
    public void canPlay_fullTest() {
        // when ad unit is not present
        subject.canPlay("invalid_ad_unit_id");
        verify(mockLoaderRewardedAd, never()).getLastDeliveredResponse();

        // when ad unit is present
        subject.canPlay(AD_UNIT_ID);
        verify(mockLoaderRewardedAd).getLastDeliveredResponse();
    }

    @Test
    public void creativeDownloadSuccess_fullTest(){
        // when ad unit is not present
        subject.creativeDownloadSuccess("invalid_ad_unit_id");
        verify(mockLoaderRewardedAd, never()).creativeDownloadSuccess();

        // when ad unit is present
        subject.creativeDownloadSuccess(AD_UNIT_ID);
        verify(mockLoaderRewardedAd).creativeDownloadSuccess();
    }
}
