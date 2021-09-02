// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import android.app.Activity;
import android.content.Context;

import com.mopub.common.AppEngineInfo;
import com.mopub.common.BaseUrlGenerator;
import com.mopub.common.ClientMetadata;
import com.mopub.common.Constants;
import com.mopub.common.MoPub;

import org.junit.After;
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

import java.net.URLEncoder;
import java.util.Map;

import static com.mopub.common.privacy.ConsentStatus.EXPLICIT_NO;
import static com.mopub.common.privacy.ConsentStatus.EXPLICIT_YES;
import static com.mopub.common.privacy.ConsentStatus.UNKNOWN;
import static com.mopub.common.test.support.UrlSupport.HOST_KEY;
import static com.mopub.common.test.support.UrlSupport.PATH_KEY;
import static com.mopub.common.test.support.UrlSupport.SCHEME_KEY;
import static com.mopub.common.test.support.UrlSupport.urlToMap;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;

@SuppressWarnings("ConstantConditions")
@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.json.*"})
@PrepareForTest(ClientMetadata.class)
public class ConsentDialogUrlGeneratorTest {
    private static final String AD_UNIT_ID = "ad_unit_id";
    private static final String CURRENT_LANGUAGE = "current_language";
    private static final String BUNDLE = "test.bundle";
    private static final String POLICY_VERSION = "policy.version";
    private static final String VENDOR_LIST_VERSION = "vendor.list.version";

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private Context context;
    private ConsentDialogUrlGenerator subject;

    @Before
    public void setup() {
        Activity activity = Robolectric.buildActivity(Activity.class).get();
        context = activity.getApplicationContext();

        PowerMockito.mockStatic(ClientMetadata.class);
        PowerMockito.when(ClientMetadata.getCurrentLanguage(context)).thenReturn(CURRENT_LANGUAGE);
        ClientMetadata mockClientMetadata = PowerMockito.mock(ClientMetadata.class);
        PowerMockito.when(mockClientMetadata.getAppPackageName()).thenReturn(BUNDLE);
        PowerMockito.when(ClientMetadata.getInstance(any(Context.class))).thenReturn(
                mockClientMetadata);
    }

    @After
    public void tearDown() {
        BaseUrlGenerator.setAppEngineInfo(null);
        BaseUrlGenerator.setWrapperVersion("");
    }

    @Test(expected = NullPointerException.class)
    public void constructor_withContextNull_shouldThrowException() {
        subject = new ConsentDialogUrlGenerator(null, AD_UNIT_ID, UNKNOWN.getValue());
    }

    @Test(expected = NullPointerException.class)
    public void constructor_withAdUnitIdlNull_shouldThrowException() {
        subject = new ConsentDialogUrlGenerator(context, null, UNKNOWN.getValue());
    }

    @Test(expected = NullPointerException.class)
    public void constructor_withConsentStatusNull_shouldThrowException() {
        subject = new ConsentDialogUrlGenerator(context, AD_UNIT_ID, null);
    }

    @Test
    public void generateUrlString_withMinimumParametersSet_shouldGenerateValidUrl() throws java.io.UnsupportedEncodingException {
        String validUrl = createTestUrl();

        subject = new ConsentDialogUrlGenerator(context, AD_UNIT_ID, UNKNOWN.getValue());
        String url = subject.generateUrlString(Constants.HOST);
        assertThat(url).isEqualTo(validUrl);
    }

    @Test
    public void generateUrlString_withAllParameters_shouldGenerateValidUrl() {
        subject = new ConsentDialogUrlGenerator(context, AD_UNIT_ID, EXPLICIT_YES.getValue());
        subject.withConsentedPrivacyPolicyVersion(POLICY_VERSION)
                .withConsentedVendorListVersion(VENDOR_LIST_VERSION)
                .withForceGdprApplies(true)
                .withGdprApplies(true);

        String url = subject.generateUrlString(Constants.HOST);
        Map<String, String> map = urlToMap(url);

        assertThat(map.get(HOST_KEY)).isEqualTo(Constants.HOST);
        assertThat(map.get(SCHEME_KEY)).isEqualTo(Constants.HTTPS);
        assertThat(map.get(PATH_KEY)).isEqualTo(Constants.GDPR_CONSENT_HANDLER);
        assertThat(map.get("id")).isEqualTo(AD_UNIT_ID);
        assertThat(map.get("current_consent_status")).isEqualTo(EXPLICIT_YES.getValue());
        assertThat(map.get("nv")).isEqualTo(MoPub.SDK_VERSION);
        assertThat(map.get("language")).isEqualTo(CURRENT_LANGUAGE);
        assertThat(map.get("gdpr_applies")).isEqualTo("1");
        assertThat(map.get("force_gdpr_applies")).isEqualTo("1");
        assertThat(map.get("consented_vendor_list_version")).isEqualTo(VENDOR_LIST_VERSION);
        assertThat(map.get("consented_privacy_policy_version")).isEqualTo(POLICY_VERSION);
        assertThat(map.get("bundle")).isEqualTo(BUNDLE);
        assertThat(map.size()).isEqualTo(12);
    }

    @Test
    public void generateUrlString_withAllParameters_withAppEngine_shouldGenerateValidUrl() {
        MoPub.setEngineInformation(new AppEngineInfo("unity", "123"));
        MoPub.setWrapperVersion("ConsentDialogUrlGeneratorTestVersion");
        subject = new ConsentDialogUrlGenerator(context, AD_UNIT_ID, EXPLICIT_YES.getValue());
        subject.withConsentedPrivacyPolicyVersion(POLICY_VERSION)
                .withConsentedVendorListVersion(VENDOR_LIST_VERSION)
                .withForceGdprApplies(true)
                .withGdprApplies(true);

        String url = subject.generateUrlString(Constants.HOST);
        Map<String, String> map = urlToMap(url);

        assertThat(map.get(HOST_KEY)).isEqualTo(Constants.HOST);
        assertThat(map.get(SCHEME_KEY)).isEqualTo(Constants.HTTPS);
        assertThat(map.get(PATH_KEY)).isEqualTo(Constants.GDPR_CONSENT_HANDLER);
        assertThat(map.get("id")).isEqualTo(AD_UNIT_ID);
        assertThat(map.get("current_consent_status")).isEqualTo(EXPLICIT_YES.getValue());
        assertThat(map.get("nv")).isEqualTo(MoPub.SDK_VERSION);
        assertThat(map.get("e_name")).isEqualTo("unity");
        assertThat(map.get("e_ver")).isEqualTo("123");
        assertThat(map.get("w_ver")).isEqualTo("ConsentDialogUrlGeneratorTestVersion");
        assertThat(map.get("language")).isEqualTo(CURRENT_LANGUAGE);
        assertThat(map.get("gdpr_applies")).isEqualTo("1");
        assertThat(map.get("force_gdpr_applies")).isEqualTo("1");
        assertThat(map.get("consented_vendor_list_version")).isEqualTo(VENDOR_LIST_VERSION);
        assertThat(map.get("consented_privacy_policy_version")).isEqualTo(POLICY_VERSION);
        assertThat(map.get("bundle")).isEqualTo(BUNDLE);
        assertThat(map.size()).isEqualTo(15);
    }

    @Test
    public void generateUrlString_withGdprAppliesNotSet_shouldNotIncludeGdprParam() {
        subject = new ConsentDialogUrlGenerator(context, AD_UNIT_ID, EXPLICIT_NO.getValue());
        subject.withConsentedPrivacyPolicyVersion(POLICY_VERSION)
                .withConsentedVendorListVersion(VENDOR_LIST_VERSION)
                .withForceGdprApplies(false);

        String url = subject.generateUrlString(Constants.HOST);
        Map<String, String> map = urlToMap(url);

        assertThat(map.get(HOST_KEY)).isEqualTo(Constants.HOST);
        assertThat(map.get(SCHEME_KEY)).isEqualTo(Constants.HTTPS);
        assertThat(map.get(PATH_KEY)).isEqualTo(Constants.GDPR_CONSENT_HANDLER);
        assertThat(map.get("id")).isEqualTo(AD_UNIT_ID);
        assertThat(map.get("current_consent_status")).isEqualTo(EXPLICIT_NO.getValue());
        assertThat(map.get("nv")).isEqualTo(MoPub.SDK_VERSION);
        assertThat(map.get("language")).isEqualTo(CURRENT_LANGUAGE);
        assertThat(map.get("gdpr_applies")).isEqualTo(null);
        assertThat(map.get("force_gdpr_applies")).isEqualTo("0");
        assertThat(map.get("consented_vendor_list_version")).isEqualTo(VENDOR_LIST_VERSION);
        assertThat(map.get("consented_privacy_policy_version")).isEqualTo(POLICY_VERSION);
        assertThat(map.get("bundle")).isEqualTo(BUNDLE);
        assertThat(map.size()).isEqualTo(11);
    }

    // unit test utils
    private String createTestUrl() throws java.io.UnsupportedEncodingException {
        return "https://" + Constants.HOST + "/m/gdpr_consent_dialog" +
                "?id=" + AD_UNIT_ID +
                "&current_consent_status=" + UNKNOWN.getValue() +
                "&nv=" + URLEncoder.encode(MoPub.SDK_VERSION, "UTF-8") +
                "&language=" + CURRENT_LANGUAGE +
                "&force_gdpr_applies=" + "0" +
                "&bundle=" + BUNDLE;
    }

}
