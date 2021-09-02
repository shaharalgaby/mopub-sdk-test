// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import com.mopub.common.ClientMetadata;
import com.mopub.common.Constants;
import com.mopub.common.MoPub;
import com.mopub.nativeads.NativeUrlGeneratorTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.json.*"})
@PrepareForTest(ClientMetadata.class)
public class ConversionUrlGeneratorTest {
    private static final String APP_VERSION = "app_version";
    private static final String CONSENT_STATUS = "consent_status";
    private static final String PRIVACY_VERSION = "privacy_version";
    private static final String VENDOR_LIST_VERSION = "vendor_list_version";
    private static final String AD_UNIT = "ad_unit";

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private Context context;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get();

        ClientMetadata clientMetadata = mock(ClientMetadata.class);
        when(clientMetadata.getAppVersion()).thenReturn(APP_VERSION);
        when(clientMetadata.getDeviceOsVersion()).thenReturn(Build.VERSION.RELEASE);
        when(clientMetadata.getDeviceManufacturer()).thenReturn(Build.MANUFACTURER);
        when(clientMetadata.getDeviceModel()).thenReturn(Build.MODEL);
        when(clientMetadata.getDeviceProduct()).thenReturn(Build.PRODUCT);
        when(clientMetadata.getDeviceHardware()).thenReturn(Build.HARDWARE);

        PowerMockito.mockStatic(ClientMetadata.class);
        when(ClientMetadata.getInstance(context)).thenReturn(clientMetadata);
    }

    //  https://ads.mopub.com/m/open?v=6&av=5.13.1&ifa=mp_tmpl_advertising_id&dnt=mp_tmpl_do_not_track&tas=mp_tmpl_tas&mid=mp_tmpl_mopub_id&os=android&adunit=b195f8dd8ded45fe847ad89ed1d016da&bundle=com.mopub.simpleadsdemo.test&id=com.mopub.simpleadsdemo.test&dn=unknown%2CAndroid%20SDK%20built%20for%20x86%2Csdk_google_phone_x86&st=1&nv=5.13.1&current_consent_status=unknown&gdpr_applies=0&force_gdpr_applies=0
    @Test
    public void generateUrlString_allParametersSet_shouldReturnValidUrl() {
        ConversionUrlGenerator subject = new ConversionUrlGenerator(context, AD_UNIT);

        String url = subject.withGdprApplies(false)
                .withCurrentConsentStatus(CONSENT_STATUS)
                .withConsentedPrivacyPolicyVersion(PRIVACY_VERSION)
                .withConsentedVendorListVersion(VENDOR_LIST_VERSION)
                .withSessionTracker(true)
                .generateUrlString(Constants.HOST);

        assertThat(url).startsWith(Constants.HTTPS + "://" + Constants.HOST + Constants.CONVERSION_TRACKING_HANDLER);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "v")).isEqualTo("6");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "nv")).isEqualTo(MoPub.SDK_VERSION);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "av")).isEqualTo(APP_VERSION);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "ifa")).isEqualTo("mp_tmpl_advertising_id");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "mid")).isEqualTo("mp_tmpl_mopub_id");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "tas")).isEqualTo("mp_tmpl_tas");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "dnt")).isEqualTo("mp_tmpl_do_not_track");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "st")).isEqualTo("1");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "current_consent_status")).isEqualTo(CONSENT_STATUS);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "gdpr_applies")).isEqualTo("0");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "consented_vendor_list_version")).isEqualTo(VENDOR_LIST_VERSION);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "consented_privacy_policy_version")).isEqualTo(PRIVACY_VERSION);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "id")).isEqualTo("com.mopub.mobileads.test");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "adunit")).isEqualTo(AD_UNIT);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "bundle")).isEqualTo("com.mopub.mobileads.test");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "os")).isEqualTo("android");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "dn"))
                .isEqualTo("unknown,robolectric,robolectric");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "osv")).isEqualTo(Build.VERSION.RELEASE);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "make")).isEqualTo(Build.MANUFACTURER);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "model")).isEqualTo(Build.MODEL);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "hwv")).isEqualTo(Build.HARDWARE);
    }

    @Test
    public void generateUrlString_allParametersNoSt_shouldReturnValidUrl() {
        ConversionUrlGenerator subject = new ConversionUrlGenerator(context, AD_UNIT);

        String url = subject.withGdprApplies(false)
                .withCurrentConsentStatus(CONSENT_STATUS)
                .withConsentedPrivacyPolicyVersion(PRIVACY_VERSION)
                .withConsentedVendorListVersion(VENDOR_LIST_VERSION)
                .withSessionTracker(false)
                .generateUrlString(Constants.HOST);

        assertThat(url).startsWith(Constants.HTTPS + "://" + Constants.HOST + Constants.CONVERSION_TRACKING_HANDLER);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "v")).isEqualTo("6");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "nv")).isEqualTo(MoPub.SDK_VERSION);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "av")).isEqualTo(APP_VERSION);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "ifa")).isEqualTo("mp_tmpl_advertising_id");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "mid")).isEqualTo("mp_tmpl_mopub_id");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "tas")).isEqualTo("mp_tmpl_tas");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "dnt")).isEqualTo("mp_tmpl_do_not_track");
        assertThat(url.indexOf("&st=")).isEqualTo(-1);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "current_consent_status")).isEqualTo(CONSENT_STATUS);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "gdpr_applies")).isEqualTo("0");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "consented_vendor_list_version")).isEqualTo(VENDOR_LIST_VERSION);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "consented_privacy_policy_version")).isEqualTo(PRIVACY_VERSION);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "id")).isEqualTo("com.mopub.mobileads.test");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "adunit")).isEqualTo(AD_UNIT);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "bundle")).isEqualTo("com.mopub.mobileads.test");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "os")).isEqualTo("android");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "dn"))
                .isEqualTo("unknown,robolectric,robolectric");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "osv")).isEqualTo(Build.VERSION.RELEASE);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "make")).isEqualTo(Build.MANUFACTURER);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "model")).isEqualTo(Build.MODEL);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "hwv")).isEqualTo(Build.HARDWARE);
    }
}
