// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.app.Activity;

import com.mopub.common.AdType;
import com.mopub.common.DataKeys;
import com.mopub.common.ViewabilityVendor;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.CreativeExperienceSettings;
import com.mopub.nativeads.test.support.TestCustomEventNativeFactory;
import com.mopub.network.AdResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class CustomEventNativeAdapterTest {

    private Activity context;
    private HashMap<String, Object> localExtras;
    private CustomEventNative.CustomEventNativeListener mCustomEventNativeListener;
    private CustomEventNative mCustomEventNative;
    private HashMap<String, String> serverExtras;
    private AdResponse testAdResponse;

    private CustomEventNativeAdapter subject;

    @Before
    public void setUp() throws Exception {
        context = new Activity();

        localExtras = new HashMap<>();
        serverExtras = new HashMap<>();
        serverExtras.put("key", "value");

        testAdResponse = new AdResponse.Builder()
                .setAdType(AdType.STATIC_NATIVE)
                .setBaseAdClassName("com.mopub.nativeads.MoPubCustomEventNative")
                .setClickTrackingUrls(Collections.singletonList("clicktrackingurl"))
                .setResponseBody("body")
                .setServerExtras(serverExtras)
                .setCreativeExperienceSettings(CreativeExperienceSettings.getDefaultSettings(false))
                .build();

        mCustomEventNativeListener = mock(CustomEventNative.CustomEventNativeListener.class);

        mCustomEventNative = TestCustomEventNativeFactory.getSingletonMock();

        subject = new CustomEventNativeAdapter(mCustomEventNativeListener);
    }

    @Test
    public void loadNativeAd_withValidInput_shouldCallLoadNativeAdOnTheCustomEvent() {
        Map<String, Object> expectedLocalExtras = new HashMap<>();
        expectedLocalExtras.put(DataKeys.CLICK_TRACKING_URL_KEY, Collections.singletonList("clicktrackingurl"));

        subject.loadNativeAd(context, localExtras, testAdResponse);

        verify(mCustomEventNative).loadNativeAd(eq(context), any(CustomEventNative.CustomEventNativeListener.class), eq(expectedLocalExtras), eq(serverExtras));
        verify(mCustomEventNativeListener, never()).onNativeAdFailed(any(NativeErrorCode.class));
        verify(mCustomEventNativeListener, never()).onNativeAdLoaded(any(BaseNativeAd.class));
    }

    @Test
    public void loadNativeAd_withInvalidClassName_shouldNotifyListenerOfOnNativeAdFailedAndReturn() {
        testAdResponse = testAdResponse.toBuilder()
                .setBaseAdClassName("com.mopub.baaad.invalidinvalid123143")
                .build();

        subject.loadNativeAd(context, localExtras, testAdResponse);

        verify(mCustomEventNativeListener).onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_NOT_FOUND);
        verify(mCustomEventNativeListener, never()).onNativeAdLoaded(any(BaseNativeAd.class));
        verify(mCustomEventNative, never()).loadNativeAd(context, mCustomEventNativeListener, localExtras, serverExtras);
    }

    @Test
    public void loadNativeAd_withInvalidCustomEventNativeData_shouldNotAddToServerExtras() {
        testAdResponse = testAdResponse.toBuilder()
                .setServerExtras(null)
                .build();

        subject.loadNativeAd(context, localExtras, testAdResponse);

        verify(mCustomEventNative).loadNativeAd(eq(context), any(CustomEventNative.CustomEventNativeListener.class), eq(localExtras), eq(new HashMap<String, String>()));
        verify(mCustomEventNativeListener, never()).onNativeAdFailed(any(NativeErrorCode.class));
        verify(mCustomEventNativeListener, never()).onNativeAdLoaded(any(BaseNativeAd.class));
    }
    @Test
    public void loadNativeAd_withViewabilityVendors_shouldAddToLocalExtras() {
        final HashSet<ViewabilityVendor> vendors = new HashSet<>();
        testAdResponse = testAdResponse.toBuilder()
                .setServerExtras(null)
                .setViewabilityVendors(vendors)
                .build();

        subject.loadNativeAd(context, localExtras, testAdResponse);

        assertEquals(vendors, localExtras.get(DataKeys.VIEWABILITY_VENDORS_KEY));
    }

    @Test
    public void stopLoading_withValidCustomEventNative_shouldInvalidateCustomEventNative() {
        subject.loadNativeAd(context, localExtras, testAdResponse);
        Mockito.reset(mCustomEventNative);

        subject.stopLoading();

        verify(mCustomEventNative).onInvalidate();
    }
}
