// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.mopub.common.AppEngineInfo;
import com.mopub.common.BaseUrlGenerator;
import com.mopub.common.LocationService;
import com.mopub.common.MoPub;
import com.mopub.common.ViewabilityManager;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.MoPubIdentifierTest;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Reflection;
import com.mopub.mobileads.BuildConfig;
import com.mopub.mobileads.test.support.MoPubShadowConnectivityManager;
import com.mopub.mobileads.test.support.MoPubShadowTelephonyManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLocationManager;
import org.robolectric.shadows.ShadowNetworkInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(shadows = {MoPubShadowTelephonyManager.class, MoPubShadowConnectivityManager.class})
public class NativeUrlGeneratorTest {
    private static final String AD_UNIT_ID = "1234";
    private static final String TEST_BUNDLE = "test.bundle";
    private static final int TEST_SCREEN_WIDTH = 320;
    private static final int TEST_SCREEN_HEIGHT = 470;
    private static final int TEST_SCREEN_SAFE_WIDTH = 0;
    private static final int TEST_SCREEN_SAFE_HEIGHT = 0;
    private static final float TEST_DENSITY = 1.0f;
    private Activity context;
    private NativeUrlGenerator subject;
    private MoPubShadowTelephonyManager shadowTelephonyManager;
    private MoPubShadowConnectivityManager shadowConnectivityManager;
    private PersonalInfoManager mockPersonalInfoManager;

    @Before
    public void setup() throws Exception {
        context = spy(Robolectric.buildActivity(Activity.class).create().get());
        Shadows.shadowOf(context).grantPermissions(ACCESS_NETWORK_STATE);
        Shadows.shadowOf(context).grantPermissions(ACCESS_FINE_LOCATION);
        when(context.getPackageName()).thenReturn(TEST_BUNDLE);
        shadowTelephonyManager = (MoPubShadowTelephonyManager)
                Shadows.shadowOf((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
        shadowConnectivityManager = (MoPubShadowConnectivityManager)
                Shadows.shadowOf((ConnectivityManager) RuntimeEnvironment.application.getSystemService(Context.CONNECTIVITY_SERVICE));
        shadowConnectivityManager.setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null,
                ConnectivityManager.TYPE_MOBILE, TelephonyManager.NETWORK_TYPE_UNKNOWN, true,
                true));

        // Set the expected screen dimensions to arbitrary numbers
        final Resources spyResources = spy(context.getResources());
        final DisplayMetrics mockDisplayMetrics = mock(DisplayMetrics.class);
        mockDisplayMetrics.widthPixels = TEST_SCREEN_WIDTH;
        mockDisplayMetrics.heightPixels = TEST_SCREEN_HEIGHT;
        mockDisplayMetrics.density = TEST_DENSITY;
        when(spyResources.getDisplayMetrics()).thenReturn(mockDisplayMetrics);
        when(context.getResources()).thenReturn(spyResources);

        final Context spyApplicationContext = spy(context.getApplicationContext());
        when(spyApplicationContext.getPackageName()).thenReturn(TEST_BUNDLE);
        PackageManager mockPackageManager = mock(PackageManager.class);
        PackageInfo mockPackageInfo = mock(PackageInfo.class);
        mockPackageInfo.versionName = MoPub.SDK_VERSION;
        when(mockPackageManager.getPackageInfo(any(String.class), anyInt())).thenReturn(mockPackageInfo);
        when(spyApplicationContext.getPackageManager()).thenReturn(mockPackageManager);

        final WindowManager mockWindowManager = mock(WindowManager.class);
        final Display mockDisplay = mock(Display.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) {
                final Point point = (Point) invocationOnMock.getArguments()[0];
                point.x = TEST_SCREEN_WIDTH;
                point.y = TEST_SCREEN_HEIGHT;
                return null;
            }
        }).when(mockDisplay).getRealSize(any(Point.class));
        when(mockWindowManager.getDefaultDisplay()).thenReturn(mockDisplay);
        when(spyApplicationContext.getSystemService(Context.WINDOW_SERVICE)).thenReturn(mockWindowManager);
        when(context.getApplicationContext()).thenReturn(spyApplicationContext);

        mockPersonalInfoManager = mock(PersonalInfoManager.class);
        when(mockPersonalInfoManager.getPersonalInfoConsentStatus()).thenReturn(ConsentStatus.UNKNOWN);
        LocationService.clearLastKnownLocation();
        MoPubIdentifierTest.writeAdvertisingInfoToSharedPreferences(context, false);
        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();
        ViewabilityManager.setViewabilityEnabled(true);
    }

    @After
    public void tearDown() throws Exception {
        MoPubIdentifierTest.clearPreferences(context);
        new Reflection.MethodBuilder(null, "resetMoPub")
                .setStatic(MoPub.class)
                .setAccessible()
                .execute();
        BaseUrlGenerator.setAppEngineInfo(null);
        BaseUrlGenerator.setWrapperVersion("");
    }

    @Test
    public void requestParametersBuilder_whenKeywordsHaveBeenProvidedButNoUserConsent_shouldNotSaveKeywords() {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(false);

        RequestParameters requestParameters = new RequestParameters.Builder()
                .keywords("keywords")
                .userDataKeywords("user_data_keywords")
                .build();

        assertThat(requestParameters.getKeywords()).isEqualTo("keywords");
        assertThat(requestParameters.getUserDataKeywords()).isNull();
    }

    @Test
    public void requestParametersBuilder_whenKeywordsHaveBeenProvidedAndUserConsent_shouldSaveKeywords() {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);

        RequestParameters requestParameters = new RequestParameters.Builder()
                .keywords("keywords")
                .userDataKeywords("user_data_keywords")
                .build();

        assertThat(requestParameters.getKeywords()).isEqualTo("keywords");
        assertThat(requestParameters.getUserDataKeywords()).isEqualTo("user_data_keywords");
    }

    @Test
    public void generateUrlString_whenViewabilityEnabled_shouldSetViewabilityVendors() {
        ViewabilityManager.setViewabilityEnabled(true);
        subject = new NativeUrlGenerator(context);

        String url = subject.generateUrlString("server.com");

        assertThat(getParameterFromRequestUrl(url, "vv")).isEqualTo("4");
        assertThat(getParameterFromRequestUrl(url, "vver")).isEqualTo("1.3.16-Mopub");
    }

    @Test
    public void generateUrlString_whenViewabilityEnabled_shouldSetViewabilityVendorsToZero() {
        ViewabilityManager.setViewabilityEnabled(false);
        subject = new NativeUrlGenerator(context);

        String url = subject.generateUrlString("server.com");

        assertThat(getParameterFromRequestUrl(url, "vv")).isEqualTo("0");
        assertThat(getParameterFromRequestUrl(url, "vver")).isEqualTo("1.3.16-Mopub");
    }

    @Test
    public void generateUrlString_whenKeywordsHaveBeenProvidedButNoUserConsent_shouldNotUseKeywords() {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(false);

        RequestParameters requestParameters = new RequestParameters.Builder()
                .keywords("keywords")
                .userDataKeywords("user_data_keywords")
                .build();
        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);
        String adUrl = subject.withRequest(requestParameters)
                .generateUrlString("ads.mopub.com");

        assertThat(getParameterFromRequestUrl(adUrl, "q")).isEqualTo("keywords");
        assertThat(getParameterFromRequestUrl(adUrl, "user_data_q")).isEqualTo("");
    }

    @Test
    public void generateUrlString_whenKeywordsHaveBeenProvidedAndUserConsent_shouldUseKeywords() {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);

        RequestParameters requestParameters = new RequestParameters.Builder()
                .keywords("keywords")
                .userDataKeywords("user_data_keywords")
                .build();
        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);
        String adUrl = subject.withRequest(requestParameters)
                .generateUrlString("ads.mopub.com");

        assertThat(getParameterFromRequestUrl(adUrl, "q")).isEqualTo("keywords");
        assertThat(getParameterFromRequestUrl(adUrl, "user_data_q")).isEqualTo("user_data_keywords");
    }

    @Test
    public void generateUrlString_shouldIncludeDesiredAssetIfSet() {
        EnumSet<RequestParameters.NativeAdAsset> assetsSet = EnumSet.of(RequestParameters.NativeAdAsset.TITLE);
        RequestParameters requestParameters = new RequestParameters.Builder().desiredAssets(assetsSet).build();

        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID).withRequest(requestParameters);

        String requestString = generateMinimumUrlString();
        List<String> desiredAssets = getDesiredAssetsListFromRequestUrlString(requestString);

        assertThat(desiredAssets.size()).isEqualTo(1);
        assertThat(desiredAssets).contains("title");
    }

    @Test
    public void generateUrlString_shouldIncludeDesiredAssetsIfSet() {
        EnumSet<RequestParameters.NativeAdAsset> assetsSet = EnumSet.of(RequestParameters.NativeAdAsset.TITLE, RequestParameters.NativeAdAsset.TEXT, RequestParameters.NativeAdAsset.ICON_IMAGE);
        RequestParameters requestParameters = new RequestParameters.Builder().desiredAssets(assetsSet).build();

        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID).withRequest(requestParameters);

        String requestString = generateMinimumUrlString();
        List<String> desiredAssets = getDesiredAssetsListFromRequestUrlString(requestString);

        assertThat(desiredAssets.size()).isEqualTo(3);
        assertThat(desiredAssets).contains("title", "text", "iconimage");
    }

    @Test
    public void generateUrlString_shouldNotIncludeDesiredAssetsIfNotSet() {
        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);

        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(false);

        String requestString = generateMinimumUrlString();
        List<String> desiredAssets = getDesiredAssetsListFromRequestUrlString(requestString);

        assertThat(desiredAssets.size()).isEqualTo(0);
    }

    @Test
    public void generateUrlString_shouldNotIncludeDesiredAssetsIfNoAssetsAreSet() {
        EnumSet<RequestParameters.NativeAdAsset> assetsSet = EnumSet.noneOf(RequestParameters.NativeAdAsset.class);
        RequestParameters requestParameters = new RequestParameters.Builder().desiredAssets(assetsSet).build();

        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID).withRequest(requestParameters);

        String requestString = generateMinimumUrlString();
        List<String> desiredAssets = getDesiredAssetsListFromRequestUrlString(requestString);

        assertThat(desiredAssets.size()).isEqualTo(0);
    }

    @Test
    public void generateUrlString_needsButDoesNotHaveReadPhoneState_shouldNotContainOperatorName() {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(false);

        shadowTelephonyManager.setNeedsReadPhoneState(true);
        shadowTelephonyManager.setReadPhoneStatePermission(false);
        shadowTelephonyManager.setNetworkOperatorName("TEST_CARRIER");

        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);
        String requestString = generateMinimumUrlString();

        assertThat(getNetworkOperatorNameFromRequestUrl(requestString)).isNullOrEmpty();
    }

    @Test
    public void generateUrlString_needsAndHasReadPhoneState_shouldContainOperatorName() {
        shadowTelephonyManager.setNeedsReadPhoneState(true);
        shadowTelephonyManager.setReadPhoneStatePermission(true);
        shadowTelephonyManager.setNetworkOperatorName("TEST_CARRIER");

        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);
        String requestString = generateMinimumUrlString();

        assertThat(getNetworkOperatorNameFromRequestUrl(requestString)).isEqualTo("TEST_CARRIER");
    }

    @Test
    public void generateUrlString_doesNotNeedReadPhoneState_shouldContainOperatorName() {
        shadowTelephonyManager.setNeedsReadPhoneState(false);
        shadowTelephonyManager.setReadPhoneStatePermission(false);
        shadowTelephonyManager.setNetworkOperatorName("TEST_CARRIER");

        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);
        String requestString = generateMinimumUrlString();

        assertThat(getNetworkOperatorNameFromRequestUrl(requestString)).isEqualTo("TEST_CARRIER");
    }

    @Test
    public void generateUrlString_shouldUseLocationServiceValue() {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);

        // Mock out the LocationManager's last known location to be more recent than the
        // developer-supplied location.
        ShadowLocationManager shadowLocationManager = Shadows.shadowOf(
                (LocationManager) RuntimeEnvironment.application.getSystemService(Context.LOCATION_SERVICE));
        Location locationFromSdk = new Location("");
        locationFromSdk.setLatitude(37);
        locationFromSdk.setLongitude(-122);
        locationFromSdk.setAccuracy(5.0f);
        locationFromSdk.setTime(System.currentTimeMillis() - 555555);
        shadowLocationManager.setLastKnownLocation(LocationManager.GPS_PROVIDER, locationFromSdk);

        RequestParameters requestParameters = new RequestParameters.Builder()
                .build();
        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);
        String adUrl = subject.withRequest(requestParameters)
                .generateUrlString("ads.mopub.com");
        assertThat(getParameterFromRequestUrl(adUrl, "ll")).isEqualTo("37.0,-122.0");
        assertThat(getParameterFromRequestUrl(adUrl, "lla")).isEqualTo("5");
        assertThat(getParameterFromRequestUrl(adUrl, "llsdk")).isEqualTo("1");
        // Only test to the full second (as there may be small differences)
        assertThat(getParameterFromRequestUrl(adUrl, "llf")).startsWith("555");
        assertThat(getParameterFromRequestUrl(adUrl, "llf").length()).isEqualTo(6);
    }

    @Test
    public void generateUrlString_whenConsentIsFalse_shouldNotHaveLocationValue() {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(false);

        Location locationFromDeveloper = new Location("");
        locationFromDeveloper.setLatitude(42);
        locationFromDeveloper.setLongitude(-42);
        locationFromDeveloper.setAccuracy(3.5f);
        locationFromDeveloper.setTime(1000);

        // Mock out the LocationManager's last known location to be more recent than the
        // developer-supplied location.
        ShadowLocationManager shadowLocationManager = Shadows.shadowOf(
                (LocationManager) RuntimeEnvironment.application.getSystemService(Context.LOCATION_SERVICE));
        Location locationFromSdk = new Location("");
        locationFromSdk.setLatitude(37);
        locationFromSdk.setLongitude(-122);
        locationFromSdk.setAccuracy(5.0f);
        locationFromSdk.setTime(System.currentTimeMillis() - 555555);
        shadowLocationManager.setLastKnownLocation(LocationManager.GPS_PROVIDER, locationFromSdk);

        RequestParameters requestParameters = new RequestParameters.Builder()
                .location(locationFromDeveloper)
                .build();
        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);
        String adUrl = subject.withRequest(requestParameters)
                .generateUrlString("ads.mopub.com");
        assertThat(getParameterFromRequestUrl(adUrl, "ll")).isEqualTo("");
        assertThat(getParameterFromRequestUrl(adUrl, "lla")).isEqualTo("");
        assertThat(getParameterFromRequestUrl(adUrl, "llsdk")).isEqualTo("");
        // Only test to the full second (as there may be small differences)
        assertThat(getParameterFromRequestUrl(adUrl, "llf")).startsWith("");
    }

    @Test
    public void generateUrlString_withOnlyAdUnitSet_shouldReturnMinimumUrl() {
        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);
        final String adUrl = generateMinimumUrlString();

        assertThat(adUrl).isEqualTo(
                "https://ads.mopub.com/m/ad?id=" +
                        AD_UNIT_ID +
                        "&nv=" + Uri.encode(MoPub.SDK_VERSION) +
                        "&os=" + "android" +
                        "&dn=unknown%2Crobolectric%2Crobolectric" +
                        "&osv=" + Build.VERSION.RELEASE +
                        "&make=" + Build.MANUFACTURER +
                        "&model=" + Build.MODEL +
                        "&hwv=" + Build.HARDWARE +
                        "&bundle=" + TEST_BUNDLE +
                        "&z=-0700" +
                        "&o=p" +
                        "&cw=" +
                        TEST_SCREEN_SAFE_WIDTH +
                        "&ch=" +
                        TEST_SCREEN_SAFE_HEIGHT +
                        "&w=" +
                        TEST_SCREEN_WIDTH +
                        "&h=" +
                        TEST_SCREEN_HEIGHT +
                        "&sc=" +
                        TEST_DENSITY +
                        "&ct=3&av=" + Uri.encode(MoPub.SDK_VERSION) +
                        "&ifa=mp_tmpl_advertising_id&dnt=mp_tmpl_do_not_track" +
                        "&tas=mp_tmpl_tas" +
                        "&mid=mp_tmpl_mopub_id" +
                        "&gdpr_applies=0" +
                        "&current_consent_status=unknown" +
                        "&vv=4&vver=1.3.16-Mopub" +
                        "&ce_settings_hash_key=0");
    }

    @Test
    public void enableLocation_shouldIncludeLocationInUrl() {
        MoPub.setLocationAwareness(MoPub.LocationAwareness.NORMAL);
        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);

        String requestString = generateMinimumUrlString();
        assertThat(getParameterFromRequestUrl(requestString, "ll")).isNotNull();
    }

    @Test
    public void disableLocation_shouldNotIncludeLocationInUrl() {
        MoPub.setLocationAwareness(MoPub.LocationAwareness.DISABLED);
        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);

        String requestString = generateMinimumUrlString();
        assertThat(getParameterFromRequestUrl(requestString, "ll")).isNullOrEmpty();
    }

    @Test
    public void disableLocationCollection_whenLocationServiceHasMostRecentLocation_shouldNotIncludeLocationInUrl() {
        MoPub.setLocationAwareness(MoPub.LocationAwareness.DISABLED);
        subject = new NativeUrlGenerator(context);

        // Mock out the LocationManager's last known location.
        ShadowLocationManager shadowLocationManager = Shadows.shadowOf(
                (LocationManager) RuntimeEnvironment.application.getSystemService(Context.LOCATION_SERVICE));
        Location locationFromSdk = new Location("");
        locationFromSdk.setLatitude(37);
        locationFromSdk.setLongitude(-122);
        locationFromSdk.setAccuracy(5.0f);
        locationFromSdk.setTime(2000);
        shadowLocationManager.setLastKnownLocation(LocationManager.GPS_PROVIDER, locationFromSdk);

        String requestString = generateMinimumUrlString();
        assertThat(getParameterFromRequestUrl(requestString, "ll")).isNullOrEmpty();
    }

    @Test
    public void generateUrlString_whenEngineInfoIsNotSet_shouldIncludeEngineInfo() {
        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);
        String adUrl = generateMinimumUrlString();

        assertThat(getParameterFromRequestUrl(adUrl, "e_name")).isNullOrEmpty();
        assertThat(getParameterFromRequestUrl(adUrl, "e_ver")).isNullOrEmpty();
    }

    @Test
    public void generateUrlString_whenEngineInfoIsSet_shouldIncludeEngineInfo() {
        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);
        MoPub.setEngineInformation(new AppEngineInfo("ename", "eversion"));
        String adUrl = generateMinimumUrlString();

        assertEquals(getParameterFromRequestUrl(adUrl, "e_name"), "ename");
        assertEquals(getParameterFromRequestUrl(adUrl, "e_ver"), "eversion");
    }

    @Test
    public void generateUrlString_withWrapperVersion_shouldIncludeWrapperVersion() {
        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);
        MoPub.setWrapperVersion("NativeUrlGeneratorTestVersion");
        String adUrl = generateMinimumUrlString();

        assertEquals(getParameterFromRequestUrl(adUrl, "w_ver"), "NativeUrlGeneratorTestVersion");
    }

    private List<String> getDesiredAssetsListFromRequestUrlString(String requestString) {
        Uri requestUri = Uri.parse(requestString);

        String desiredAssetsString = requestUri.getQueryParameter("assets");
        return (desiredAssetsString == null) ? new ArrayList<String>() : Arrays.asList(desiredAssetsString.split(","));
    }

    private String getNetworkOperatorNameFromRequestUrl(String requestString) {
        Uri requestUri = Uri.parse(requestString);

        String networkOperatorName = requestUri.getQueryParameter("cn");

        if (TextUtils.isEmpty(networkOperatorName)) {
            return "";
        }

        return networkOperatorName;
    }

    public static String getParameterFromRequestUrl(String requestString, String key) {
        Uri requestUri = Uri.parse(requestString);
        String parameter = requestUri.getQueryParameter(key);

        if (TextUtils.isEmpty(parameter)) {
            return "";
        }

        return parameter;
    }

    private String generateMinimumUrlString() {
        return subject.generateUrlString("ads.mopub.com");
    }
}
