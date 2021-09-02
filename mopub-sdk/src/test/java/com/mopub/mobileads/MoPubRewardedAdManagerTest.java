// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.AdFormat;
import com.mopub.common.AdType;
import com.mopub.common.CESettingsCacheService;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.SharedPreferencesHelper;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.ConsentData;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.MoPubIdentifierTest;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.privacy.SyncRequest;
import com.mopub.common.util.Reflection;
import com.mopub.common.util.ResponseHeader;
import com.mopub.mobileads.factories.BaseAdFactory;
import com.mopub.mobileads.factories.FullscreenAdAdapterFactory;
import com.mopub.mobileads.test.support.TestBaseAdFactory;
import com.mopub.mobileads.test.support.TestFullscreenAdAdapterFactory;
import com.mopub.network.AdResponse;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.MoPubNetworkResponse;
import com.mopub.network.MoPubRequest;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.MultiAdRequest;
import com.mopub.network.MultiAdResponse;
import com.mopub.network.Networking;
import com.mopub.network.TrackingRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowLooper;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*", "javax.net.ssl.SSLSocketFactory"})
@PrepareForTest(CESettingsCacheService.class)
public class
MoPubRewardedAdManagerTest {

    private static final String MOPUB_REWARD = "mopub_reward";
    private static final String SINGLE_CURRENCY_NAME = "Coins_old";
    private static final int SINGLE_CURRENCY_AMOUNT = 17;
    private static final String MULTI_CURRENCY_JSON_1 =
            "{\"rewards\": [ { \"name\": \"Coins\", \"amount\": 25 } ] }";
    private static final String MULTI_CURRENCIES_JSON_4 =
            "{\n" +
                    "  \"rewards\": [\n" +
                    "    { \"name\": \"Coins\", \"amount\": 8 },\n" +
                    "    { \"name\": \"Diamonds\", \"amount\": 1 },\n" +
                    "    { \"name\": \"Diamonds\", \"amount\": 10 },\n" +
                    "    { \"name\": \"Energy\", \"amount\": 20 }\n" +
                    "  ]\n" +
                    "}\n";
    private static final String TEST_CUSTOM_EVENT_PREF_NAME = "mopubTestCustomEventSettings";
    private static final String CUSTOM_DATA_MAX_LENGTH_EXCEEDED_MESSAGE = "provided rewarded ad custom data parameter longer than supported";
    private static final String adUnitId = "testAdUnit";

    @Mock
    MoPubRequestQueue mockRequestQueue;
    @Mock
    private MoPubRewardedAdListener mockRewardedAdListener;

    private MultiAdRequest.Listener requestListener;
    private MultiAdRequest request;
    private RewardedAdCompletionRequest rewardedAdCompletionRequest;
    private Activity mActivity;
    private SharedPreferences mTestAdAdapterSharedPrefs;
    private PersonalInfoManager mockPersonalInfoManager;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        mActivity = Robolectric.buildActivity(Activity.class).create().get();

        FullscreenAdAdapterFactory.setInstance(new TestFullscreenAdAdapterFactory());
        BaseAdFactory.setInstance(new TestBaseAdFactory());

        new Reflection.MethodBuilder(null, "resetMoPub")
                .setStatic(MoPub.class)
                .setAccessible()
                .execute();
        MoPub.initializeSdk(mActivity, new SdkConfiguration.Builder("adunit")
                .withLogLevel(MoPubLog.LogLevel.DEBUG)
                .build(), null);
        Reflection.getPrivateField(MoPub.class, "sSdkInitialized").setBoolean(null, true);

        MoPubIdentifierTest.writeAdvertisingInfoToSharedPreferences(mActivity, false);
        MoPubRewardedAdManager.init(mActivity);

        // The fact that next call fixes issues in multiple tests proves that Robolectric doesn't
        // teardown singletons properly between tests.
        MoPubRewardedAdManager.updateActivity(mActivity);

        MoPubRewardedAdManager.setRewardedAdListener(mockRewardedAdListener);

        when(TestFullscreenAdAdapterFactory.getSingletonMock().getBaseAdClassName())
                .thenReturn(MoPubFullscreen.class.getName());

        mTestAdAdapterSharedPrefs = SharedPreferencesHelper.getSharedPreferences(
                        mActivity, TEST_CUSTOM_EVENT_PREF_NAME);
        MoPubRewardedAdManager.setBaseAdSharedPrefs(mTestAdAdapterSharedPrefs);

        mockPersonalInfoManager = mock(PersonalInfoManager.class);
        when(mockPersonalInfoManager.getPersonalInfoConsentStatus()).thenReturn(ConsentStatus.UNKNOWN);
        ConsentData mockConsentData = mock(ConsentData.class);
        when(mockPersonalInfoManager.getConsentData()).thenReturn(mockConsentData);

        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        doAnswer((Answer<Object>) invocationOnMock -> {
            MoPubRequest<?> req = ((MoPubRequest<?>) invocationOnMock.getArguments()[0]);
            if (req.getClass().equals(MultiAdRequest.class)) {
                request = (MultiAdRequest) req;
                requestListener = request.mListener;
                return null;
            } else if (req.getClass().equals(RewardedAdCompletionRequest.class)) {
                rewardedAdCompletionRequest = (RewardedAdCompletionRequest) req;
                return null;
            } else if(req.getClass().equals(SyncRequest.class)) {
                return null;
            } else if(req.getClass().equals(TrackingRequest.class)) {
                return null;
            } else {
                throw new Exception(String.format("Request object added to RequestQueue can only be of type " +
                        "MultiAdRequest or RewardedAdCompletionRequest, saw %s instead.", req.getClass()));
            }
        }).when(mockRequestQueue).add(any(MoPubRequest.class));

        Networking.setRequestQueueForTesting(mockRequestQueue);

        PowerMockito.mockStatic(CESettingsCacheService.class);

        PowerMockito.doAnswer(invocation -> {
            CESettingsCacheService.CESettingsCacheListener cacheListener = invocation
                    .getArgumentAt(1,  CESettingsCacheService.CESettingsCacheListener.class);
            cacheListener.onHashReceived("0");
            return null;
        }).when(CESettingsCacheService.class, "getCESettingsHash",
                anyString(),
                any(CESettingsCacheService.CESettingsCacheListener.class),
                any(Context.class));

        PowerMockito.doAnswer(invocation -> {
            CESettingsCacheService.CESettingsCacheListener cacheListener = invocation
                    .getArgumentAt(1,  CESettingsCacheService.CESettingsCacheListener.class);
            cacheListener.onSettingsReceived(CreativeExperienceSettings.getDefaultSettings(true));
            return null;
        }).when(CESettingsCacheService.class, "getCESettings",
                anyString(),
                any(CESettingsCacheService.CESettingsCacheListener.class),
                any(Context.class));

        shadowOf(Looper.getMainLooper()).idle();
    }

    @After
    public void tearDown() throws Exception {
        // Unpause the main looper in case a test terminated while the looper was paused.
        ShadowLooper.unPauseMainLooper();
        // Drain the Main Looper in case a test has unexecuted runnables
        shadowOf(Looper.getMainLooper()).idle();
        MoPubRewardedAdManager.getRewardedAdData().clear();
        MoPubRewardedAdManager.getAdRequestStatusMapping().clearMapping();
        mTestAdAdapterSharedPrefs.edit().clear().apply();
        MoPubIdentifierTest.clearPreferences(mActivity);
        new Reflection.MethodBuilder(null, "resetMoPub")
                .setStatic(MoPub.class)
                .setAccessible()
                .execute();
    }

    @Test
    public void createRequestParameters_withUserDataKeywordsButNoConsent_shouldNotSetUserDataKeywords() throws Exception {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(false);
        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        MoPubRewardedAdManager.RequestParameters requestParameters = new MoPubRewardedAdManager.RequestParameters("keywords", "user_data_keywords",null, "testCustomerId");

        assertThat(requestParameters.mKeywords).isEqualTo("keywords");
        assertThat(requestParameters.mUserDataKeywords).isEqualTo(null);
    }

    @Test
    public void createRequestParameters_withUserDataKeywordsWithConsent_shouldSetUserDataKeywords() throws Exception {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);
        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        MoPubRewardedAdManager.RequestParameters requestParameters = new MoPubRewardedAdManager.RequestParameters("keywords", "user_data_keywords", null, "testCustomerId");

        assertThat(requestParameters.mKeywords).isEqualTo("keywords");
        assertThat(requestParameters.mUserDataKeywords).isEqualTo("user_data_keywords");
    }

    @Test
    public void loadAd_withRequestParameters_shouldGenerateUrlWithKeywords() throws Exception {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);

        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd("testAdUnit", new MoPubRewardedAdManager.RequestParameters("nonsense;garbage;keywords"));

        verify(mockRequestQueue).add(argThat(new RequestBodyContains("nonsense;garbage;keywords")));

        // Finish the request
        requestListener.onErrorResponse(new MoPubNetworkError.Builder("end test").build());
        ShadowLooper.unPauseMainLooper();
    }

    @Test
    public void loadAd_withCustomerIdInRequestParameters_shouldSetCustomerId() throws Exception {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(false);
        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd("testAdUnit", new MoPubRewardedAdManager.RequestParameters("keywords", "user_data_keywords",null, "testCustomerId"));

        assertThat(MoPubRewardedAdManager.getRewardedAdData().getCustomerId()).isEqualTo("testCustomerId");

        // Finish the request
        requestListener.onErrorResponse(new MoPubNetworkError.Builder("end test").build());
        ShadowLooper.unPauseMainLooper();
    }

    @Test
    public void loadAd_withAdAlreadyShowing_shouldNotLoadAd() {
        // To simulate that an ad is showing
        MoPubRewardedAdManager.getRewardedAdData().setCurrentlyShowingAdUnitId("testAdUnit");

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd("testAdUnit", null);

        ShadowLooper.unPauseMainLooper();

        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void loadAd_withDifferentAdAlreadyShowing_shouldLoadAd() {
        // To simulate that an ad is showing
        MoPubRewardedAdManager.getRewardedAdData().setCurrentlyShowingAdUnitId("testAdUnit");

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd("anotherTestAdUnit", null);

        ShadowLooper.unPauseMainLooper();

        verify(mockRequestQueue).add(any(MultiAdRequest.class));
    }

    @Test
    public void loadAd_withAdUnitIdAlreadyLoaded_shouldNotLoadAnotherAd() throws Exception {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200, jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, adUnitId);


        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        // Load the first base ad
        MoPubRewardedAdManager.loadAd(adUnitId, null);

        ShadowLooper.unPauseMainLooper();

        requestListener.onResponse(testResponse);
        MoPubRewardedAdManager.getRewardedAdData().getAdAdapter(adUnitId).onAdLoaded();

        // Verify the first base ad
        assertThat(MoPubRewardedAdManager.hasAd(adUnitId)).isTrue();
        verify(mockRewardedAdListener).onRewardedAdLoadSuccess(eq(adUnitId));
        verifyNoMoreInteractions(mockRewardedAdListener);
        verify(mockRequestQueue).add(any(MoPubRequest.class));
        reset(mockRewardedAdListener);

        ShadowLooper.pauseMainLooper();

        // Load the second base ad
        MoPubRewardedAdManager.loadAd(adUnitId, null);

        ShadowLooper.unPauseMainLooper();

        // Verify the first base ad is still available
        assertThat(MoPubRewardedAdManager.hasAd(adUnitId)).isTrue();
        verify(mockRewardedAdListener).onRewardedAdLoadSuccess(eq(adUnitId));
        verifyNoMoreInteractions(mockRewardedAdListener);
        // Make sure the second load does not attempt to load another ad
        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void loadAd_whenCeSettingsCacheListenerReceivesHash_shouldLoadAd_withUrlHashSet() throws Exception{
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);

        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        PowerMockito.doAnswer(invocation -> {
            CESettingsCacheService.CESettingsCacheListener cacheListener = invocation
                    .getArgumentAt(1,  CESettingsCacheService.CESettingsCacheListener.class);
            cacheListener.onHashReceived("12345");
            return null;
        }).when(CESettingsCacheService.class, "getCESettingsHash",
                anyString(),
                any(CESettingsCacheService.CESettingsCacheListener.class),
                any(Context.class));

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd("testAdUnit", null);

        ShadowLooper.unPauseMainLooper();

        String requestUrl = request.getOriginalUrl();
        assertThat(requestUrl.contains("hash=12345"));
    }

    @Test
    public void callbackMethods_withNullListener_shouldNotError() {
        final FullscreenAdAdapter mockAdAdapter = TestFullscreenAdAdapterFactory.getSingletonMock();
        when(mockAdAdapter.getAdNetworkId()).thenReturn("mock_network_id");
        MoPubRewardedAdManager.getRewardedAdData().updateAdUnitAdAdapterMapping(
                adUnitId, mockAdAdapter);

        // Clients can set RVM null.
        MoPubRewardedAdManager.setRewardedAdListener(null);

        MultiAdResponse multiAdResponse = Mockito.mock(MultiAdResponse.class);

        AdResponse testResponse = new AdResponse.Builder()
                .setBaseAdClassName("com.mopub.mobileads.MoPubRewardedAdManagerTest$TestAdAdapter")
                .setAdType(AdType.CUSTOM)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd("testAdUnit", null);
        // Triggers a call to MoPubRewardedAdManager.onRewardedAdLoadSuccess
        requestListener.onResponse(multiAdResponse);

        ShadowLooper.unPauseMainLooper();

        MoPubRewardedAdManager.onRewardedAdClicked(mockAdAdapter, mockAdAdapter.getAdNetworkId());
        MoPubRewardedAdManager.onRewardedAdStarted(mockAdAdapter, mockAdAdapter.getAdNetworkId());
        MoPubRewardedAdManager.onRewardedAdClosed(mockAdAdapter, mockAdAdapter.getAdNetworkId());
        MoPubRewardedAdManager.onRewardedAdCompleted(mockAdAdapter,
                mockAdAdapter.getAdNetworkId(),
                MoPubReward.success("test", 111));

        // The test passed because none of the above calls threw an exception even though the listener is null.
    }

    @Test
    public void onAdSuccess_withEmptyAdUnitId_shouldNotLoadAd() throws Exception {
        JSONObject jsonResponse = createRewardedJsonResponse();

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200,
                jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse,
                AdFormat.REWARDED_AD, "");

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd("", null);
        requestListener.onResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        verify(mockRequestQueue, never());
    }

    @Test
    public void onAdSuccess_noActivityFound_shouldNotCallFailUrl() {
        MultiAdResponse multiAdResponse = Mockito.mock(MultiAdResponse.class);
        AdResponse testResponse = new AdResponse.Builder()
                .setAdType(AdType.CUSTOM)
                .setBaseAdClassName("com.mopub.mobileads.MoPubRewardedAdManagerTest$TestAdAdapter")
                .setFailoverUrl("fail.url")
                .build();

        MoPubRewardedAdManager.updateActivity(null);
        MoPubRewardedAdManager.loadAd("testAdUnit", null);
        requestListener.onResponse(multiAdResponse);

        verify(mockRequestQueue).add(any(MultiAdRequest.class));
        verifyNoMoreInteractions(mockRequestQueue);

        // Clean up the static state we screwed up:
        MoPubRewardedAdManager.updateActivity(mActivity);
    }

    @Test
    public void onAdSuccess_noCEFound_shouldCallFailCallback() throws Exception {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "doesn't_Exist");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200, jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, adUnitId);

        MoPubRewardedAdManager.loadAd(adUnitId, null);
        requestListener.onResponse(testResponse);
        MoPubRewardedAdManager.getRewardedAdData().getAdAdapter(adUnitId).onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

        verify(mockRewardedAdListener).onRewardedAdLoadFailure(eq(adUnitId),
                eq(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR));
        verifyNoMoreInteractions(mockRewardedAdListener);
    }

    @Test
    public void onAdSuccess_noCEFound_shouldLoadFailUrl() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        jsonResponse.put(ResponseHeader.FAIL_URL.getKey(), "fail.url");
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "doesn't_Exist");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200, jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, adUnitId);

        MoPubRewardedAdManager.loadAd(adUnitId, null);
        requestListener.onResponse(testResponse);
        MoPubRewardedAdManager.getRewardedAdData().getAdAdapter(adUnitId).onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

        assertThat(request.getUrl()).isEqualTo("fail.url");
        // Clear up the static state :(
        requestListener.onErrorResponse(new MoPubNetworkError.Builder("reset").build());
    }

    @Test
    public void onAdSuccess_shouldInstantiateCustomEvent_shouldLoad() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedAdManagerTest$TestAdAdapter");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_VIDEO_CURRENCY_NAME.getKey(), SINGLE_CURRENCY_NAME);
        metadata.put(ResponseHeader.REWARDED_VIDEO_CURRENCY_AMOUNT.getKey(), SINGLE_CURRENCY_AMOUNT);
        metadata.remove(ResponseHeader.REWARDED_CURRENCIES.getKey());

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200, jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd(adUnitId, null);

        ShadowLooper.unPauseMainLooper();

        requestListener.onResponse(testResponse);
        MoPubRewardedAdManager.getRewardedAdData().getAdAdapter(adUnitId).onAdLoaded();

        assertThat(MoPubRewardedAdManager.hasAd(adUnitId)).isTrue();
        verify(mockRewardedAdListener).onRewardedAdLoadSuccess(eq(adUnitId));
        verifyNoMoreInteractions(mockRewardedAdListener);
    }

    @Test
    public void onAdSuccess_withLegacyRewardedCurrencyHeaders_shouldMapAdUnitIdToReward_shouldLoad() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedAdManagerTest$TestAdAdapter");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_VIDEO_CURRENCY_NAME.getKey(), SINGLE_CURRENCY_NAME);
        metadata.put(ResponseHeader.REWARDED_VIDEO_CURRENCY_AMOUNT.getKey(), SINGLE_CURRENCY_AMOUNT);
        metadata.remove(ResponseHeader.REWARDED_CURRENCIES.getKey());

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200, jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd(adUnitId, null);

        ShadowLooper.unPauseMainLooper();

        requestListener.onResponse(testResponse);
        MoPubRewardedAdManager.getRewardedAdData().getAdAdapter(adUnitId).onAdLoaded();

        assertThat(MoPubRewardedAdManager.hasAd(adUnitId)).isTrue();
        verify(mockRewardedAdListener).onRewardedAdLoadSuccess(eq(adUnitId));
        verifyNoMoreInteractions(mockRewardedAdListener);

        // Verify that the reward is mapped to the adunit
        RewardedAdData rewardedAdData = MoPubRewardedAdManager.getRewardedAdData();
        assertThat(rewardedAdData.getMoPubReward(adUnitId)).isNotNull();
        assertThat(rewardedAdData.getMoPubReward(adUnitId).getLabel()).isEqualTo(SINGLE_CURRENCY_NAME);
        assertThat(rewardedAdData.getMoPubReward(adUnitId).getAmount()).isEqualTo(SINGLE_CURRENCY_AMOUNT);
        assertThat(rewardedAdData.getAvailableRewards(adUnitId)).isEmpty();
    }

    @Test
    public void onAdSuccess_withMultiRewardedCurrenciesJsonHeader_shouldMapAdUnitToAvailableRewards_shouldLoad() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedAdManagerTest$TestAdAdapter");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_CURRENCIES.getKey(), new JSONObject(MULTI_CURRENCIES_JSON_4));

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200, jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, adUnitId);


        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd(adUnitId, null);

        ShadowLooper.unPauseMainLooper();

        requestListener.onResponse(testResponse);
        MoPubRewardedAdManager.getRewardedAdData().getAdAdapter(adUnitId).onAdLoaded();

        assertThat(MoPubRewardedAdManager.hasAd(adUnitId)).isTrue();
        verify(mockRewardedAdListener).onRewardedAdLoadSuccess(eq(adUnitId));
        verifyNoMoreInteractions(mockRewardedAdListener);

        // Verify that only available rewards are updated, not the final reward mapped to the adunit
        RewardedAdData rewardedAdData = MoPubRewardedAdManager.getRewardedAdData();
        assertThat(rewardedAdData.getMoPubReward(adUnitId)).isNull();
        assertThat(rewardedAdData.getAvailableRewards(adUnitId).size()).isEqualTo(4);
        assertThat(rewardedAdData.existsInAvailableRewards(adUnitId, "Coins", 8)).isTrue();
        assertThat(rewardedAdData.existsInAvailableRewards(adUnitId, "Diamonds", 1)).isTrue();
        assertThat(rewardedAdData.existsInAvailableRewards(adUnitId, "Diamonds", 10)).isTrue();
        assertThat(rewardedAdData.existsInAvailableRewards(adUnitId, "Energy", 20)).isTrue();
    }

    @Test
    public void onAdSuccess_withSingleRewardedCurrencyJsonHeader_shouldMapAdUnitToRewardAndUpdateAvailableRewards_shouldLoad() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedAdManagerTest$TestAdAdapter");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200, jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd(adUnitId, null);

        ShadowLooper.unPauseMainLooper();

        requestListener.onResponse(testResponse);
        MoPubRewardedAdManager.getRewardedAdData().getAdAdapter(adUnitId).onAdLoaded();

        assertThat(MoPubRewardedAdManager.hasAd(adUnitId)).isTrue();
        verify(mockRewardedAdListener).onRewardedAdLoadSuccess(eq(adUnitId));
        verifyNoMoreInteractions(mockRewardedAdListener);

        // Verify that the single reward is mapped to the adunit, and it's the only available reward
        RewardedAdData rewardedAdData = MoPubRewardedAdManager.getRewardedAdData();
        assertThat(rewardedAdData.getMoPubReward(adUnitId)).isNotNull();
        assertThat(rewardedAdData.getMoPubReward(adUnitId).getLabel()).isEqualTo("Coins");
        assertThat(rewardedAdData.getMoPubReward(adUnitId).getAmount()).isEqualTo(15);
        assertThat(rewardedAdData.getAvailableRewards(adUnitId).size()).isEqualTo(1);
        assertThat(rewardedAdData.existsInAvailableRewards(adUnitId, "Coins", 15)).isTrue();
    }

    @Test
    public void onAdSuccess_withBothLegacyAndJsonHeaders_shouldIgnoreLegacyHeaders_shouldLoad() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedAdManagerTest$TestAdAdapter");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_VIDEO_CURRENCY_NAME.getKey(), SINGLE_CURRENCY_NAME);
        metadata.put(ResponseHeader.REWARDED_VIDEO_CURRENCY_AMOUNT.getKey(), SINGLE_CURRENCY_AMOUNT);
        metadata.put(ResponseHeader.REWARDED_CURRENCIES.getKey(), MULTI_CURRENCIES_JSON_4);

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200, jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd(adUnitId, null);

        ShadowLooper.unPauseMainLooper();

        requestListener.onResponse(testResponse);
        MoPubRewardedAdManager.getRewardedAdData().getAdAdapter(adUnitId).onAdLoaded();

        assertThat(MoPubRewardedAdManager.hasAd(adUnitId)).isTrue();
        verify(mockRewardedAdListener).onRewardedAdLoadSuccess(eq(adUnitId));
        verifyNoMoreInteractions(mockRewardedAdListener);

        // Verify that the legacy headers are ignored, and available rewards are updated from the JSON header
        RewardedAdData rewardedAdData = MoPubRewardedAdManager.getRewardedAdData();
        assertThat(rewardedAdData.getMoPubReward(adUnitId)).isNull();
        assertThat(rewardedAdData.getAvailableRewards(adUnitId).size()).isEqualTo(4);
        assertThat(rewardedAdData.existsInAvailableRewards(adUnitId, "Coins", 8)).isTrue();
        assertThat(rewardedAdData.existsInAvailableRewards(adUnitId, "Diamonds", 1)).isTrue();
        assertThat(rewardedAdData.existsInAvailableRewards(adUnitId, "Diamonds", 10)).isTrue();
        assertThat(rewardedAdData.existsInAvailableRewards(adUnitId, "Energy", 20)).isTrue();
    }

    @Test
    public void onAdSuccess_withMalformedRewardedCurrenciesJsonHeader_shouldNotUpdateRewardMappings_andNotLoad() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedAdManagerTest$TestAdAdapter");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_CURRENCIES.getKey(), "not json");

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200, jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, adUnitId);


        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd(adUnitId, null);
        requestListener.onResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        assertThat(MoPubRewardedAdManager.hasAd(adUnitId)).isFalse();
        verify(mockRewardedAdListener).onRewardedAdLoadFailure(eq(adUnitId),
                eq(MoPubErrorCode.REWARDED_CURRENCIES_PARSING_ERROR));
        verifyNoMoreInteractions(mockRewardedAdListener);

        // Verify that no reward mappings are updated
        RewardedAdData rewardedAdData = MoPubRewardedAdManager.getRewardedAdData();
        assertThat(rewardedAdData.getMoPubReward(adUnitId)).isNull();
        assertThat(rewardedAdData.getAvailableRewards(adUnitId).isEmpty());
    }

    @Test
    public void onAdSuccess_withServerExtras_shouldSaveInitParamsInSharedPrefs() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), TestFullscreenAdAdapterFactory.getSingletonMock().getClass().toString());
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.CUSTOM_EVENT_DATA.getKey(), "{\"k1\":\"v1\",\"k2\":\"v2\"}");

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200, jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd(adUnitId, null);

        ShadowLooper.unPauseMainLooper();

        requestListener.onResponse(testResponse);
        MoPubRewardedAdManager.getRewardedAdData().getAdAdapter(adUnitId).onAdLoaded();

        Map<String, ?> networkInitSettings = mTestAdAdapterSharedPrefs.getAll();
        String testAdAdapterClassName = TestFullscreenAdAdapterFactory.getSingletonMock().getClass().toString();

        // Verify that TestAdAdapter has init params saved in SharedPrefs.
        assertThat(networkInitSettings.size()).isEqualTo(1);
        assertThat(networkInitSettings.containsKey(testAdAdapterClassName)).isTrue();
        assertThat((String)networkInitSettings.get(testAdAdapterClassName))
                .isEqualTo("{\"adunit_format\":\"full\",\"com_mopub_vast_click_exp_enabled\":\"false\",\"k1\":\"v1\",\"k2\":\"v2\"}");
    }

    @Test
    public void onAdSuccess_withNewInitParams_shouldUpdateInitParamsInSharedPrefs() throws JSONException, MoPubNetworkError {
        // Put in {"k1":"v1","k2":"v2"} as existing init params.
        mTestAdAdapterSharedPrefs.edit().putString(
                TestFullscreenAdAdapterFactory.getSingletonMock().getClass().toString(),
                "{\"k1\":\"v1\",\"k2\":\"v2\"}").apply();

        // New init params are {"k3":"v3"}.
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), TestFullscreenAdAdapterFactory.getSingletonMock().getClass().toString());
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.CUSTOM_EVENT_DATA.getKey(), "{\"k3\":\"v3\"}");

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200, jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd(adUnitId, null);
        requestListener.onResponse(testResponse);
        TestFullscreenAdAdapterFactory.getSingletonMock().onAdLoaded();

        ShadowLooper.unPauseMainLooper();

        Map<String, ?> networkInitSettings = mTestAdAdapterSharedPrefs.getAll();
        String testAdAdapterClassName = TestFullscreenAdAdapterFactory.getSingletonMock().getClass().toString();

        // Verify that TestAdAdapter has new init params saved in SharedPrefs.
        assertThat(networkInitSettings.size()).isEqualTo(1);
        assertThat(networkInitSettings.containsKey(testAdAdapterClassName)).isTrue();
        assertThat((String)networkInitSettings.get(testAdAdapterClassName))
                .isEqualTo("{\"adunit_format\":\"full\",\"com_mopub_vast_click_exp_enabled\":\"false\",\"k3\":\"v3\"}");
    }

    @Test
    public void onAdSuccess_shouldHaveUniqueBroadcastIdsSetForEachCustomEvent() throws Exception {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedAdManagerTest$TestAdAdapter");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200, jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, "testAdUnit1");

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        // Load the first base ad
        MoPubRewardedAdManager.loadAd("testAdUnit1", null);

        ShadowLooper.unPauseMainLooper();

        requestListener.onResponse(testResponse);
        final FullscreenAdAdapter adAdapter1 = (FullscreenAdAdapter) MoPubRewardedAdManager.getRewardedAdData().getAdAdapter("testAdUnit1");

        // Get the first base ad's broadcast id
        Long broadcastId1 = adAdapter1.getBroadcastIdentifier();
        assertThat(broadcastId1).isNotNull();

        ShadowLooper.pauseMainLooper();

        // Load the second base ad
        testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, "testAdUnit2");
        MoPubRewardedAdManager.loadAd("testAdUnit2", null);

        ShadowLooper.unPauseMainLooper();

        requestListener.onResponse(testResponse);
        final FullscreenAdAdapter adAdapter2 = (FullscreenAdAdapter) MoPubRewardedAdManager.getRewardedAdData().getAdAdapter("testAdUnit2");

        // Get the second base ad's broadcast id
        Long broadcastId2 = adAdapter2.getBroadcastIdentifier();
        assertThat(broadcastId2).isNotNull();

        // Make sure they're different
        assertThat(broadcastId1).isNotEqualTo(broadcastId2);
    }

    @Test
    public void onAdSuccess_shouldUpdateAdUnitRewardMapping() throws Exception {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedAdManagerTest$TestAdAdapter");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_VIDEO_CURRENCY_NAME.getKey(), SINGLE_CURRENCY_NAME);
        metadata.put(ResponseHeader.REWARDED_VIDEO_CURRENCY_AMOUNT.getKey(), SINGLE_CURRENCY_AMOUNT);
        metadata.remove(ResponseHeader.REWARDED_CURRENCIES.getKey());

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200, jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd(adUnitId, null);
        requestListener.onResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        MoPubReward moPubReward =
                MoPubRewardedAdManager.getRewardedAdData().getMoPubReward(adUnitId);
        assertThat(moPubReward.getAmount()).isEqualTo(SINGLE_CURRENCY_AMOUNT);
        assertThat(moPubReward.getLabel()).isEqualTo(SINGLE_CURRENCY_NAME);
    }

    @Test
    public void onAdSuccess_whenAdResponseContainsNewCeSettings_shouldCacheNewSettings_shouldUseNewSettings_shouldInstantiateAdAdapter() throws Exception {
        JSONObject jsonResponse = createRewardedJsonResponse();
        jsonResponse.put(ResponseHeader.FAIL_URL.getKey(), "fail.url");
        jsonResponse.put(ResponseHeader.REWARDED.getKey(), "1");
        jsonResponse.put(ResponseHeader.CREATIVE_EXPERIENCE_SETTINGS.getKey(),
                CreativeExperienceSettingsParserTest.getCeSettingsJSONObject());
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey())
                .getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(),
                "com.mopub.mobileads.MoPubRewardedAdManagerTest$TestAdAdapter");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200,
                jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse,
                AdFormat.REWARDED_AD, adUnitId);

        CreativeExperienceSettings responseSettings = CreativeExperienceSettingsParser
                .parse(CreativeExperienceSettingsParserTest.getCeSettingsJSONObject(), true);
        PowerMockito.doAnswer(invocation -> {
            CreativeExperienceSettings settingsToCache = invocation
                    .getArgumentAt(1,  CreativeExperienceSettings.class);
            // Verify attempt to cache new settings
            assertEquals(responseSettings, settingsToCache);
            return null;
        }).when(CESettingsCacheService.class, "putCESettings",
                anyString(),
                any(CreativeExperienceSettings.class),
                any(Context.class));

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd(adUnitId, null);
        requestListener.onResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        // Verify adapter is instantiated
        assertNotNull(MoPubRewardedAdManager.getRewardedAdData().getAdAdapter(adUnitId));
        // Verify new settings are used
        CreativeExperienceSettings ceSettingsUnderTest = MoPubRewardedAdManager
                .getCreativeExperienceSettings();
        assertEquals(responseSettings, ceSettingsUnderTest);
    }

    @Test
    public void onAdSuccess_whenAdResponseDoesNotContainNewCeSettings_shouldGetSettingsFromCache_whenSettingsAreNull_shouldUseDefaultSettings_shouldInstantiateAdAdapter() throws Exception {
        JSONObject jsonResponse = createRewardedJsonResponse();
        jsonResponse.put(ResponseHeader.FAIL_URL.getKey(), "fail.url");
        jsonResponse.put(ResponseHeader.REWARDED.getKey(), "1");
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey())
                .getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(),
                "com.mopub.mobileads.MoPubRewardedAdManagerTest$TestAdAdapter");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200,
                jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse,
                AdFormat.REWARDED_AD, adUnitId);

        PowerMockito.doAnswer(invocation -> {
            CESettingsCacheService.CESettingsCacheListener cacheListener = invocation
                    .getArgumentAt(1,  CESettingsCacheService.CESettingsCacheListener.class);
            cacheListener.onSettingsReceived(null);
            return null;
        }).when(CESettingsCacheService.class, "getCESettings",
                anyString(),
                any(CESettingsCacheService.CESettingsCacheListener.class),
                any(Context.class));

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd(adUnitId, null);
        requestListener.onResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        // Verify adapter is instantiated
        assertNotNull(MoPubRewardedAdManager.getRewardedAdData().getAdAdapter(adUnitId));
        // Verify default settings are used
        CreativeExperienceSettings ceSettingsUnderTest = MoPubRewardedAdManager
                .getCreativeExperienceSettings();
        assertEquals(CreativeExperienceSettings.getDefaultSettings(true), ceSettingsUnderTest);
    }

    @Test
    public void onAdSuccess_whenAdResponseDoesNotContainNewCeSettings_shouldGetSettingsFromCache_shouldUseCachedSettings_shouldInstantiateAdAdapter() throws Exception {
        JSONObject jsonResponse = createRewardedJsonResponse();
        jsonResponse.put(ResponseHeader.FAIL_URL.getKey(), "fail.url");
        jsonResponse.put(ResponseHeader.REWARDED.getKey(), "1");
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey())
                .getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(),
                "com.mopub.mobileads.MoPubRewardedAdManagerTest$TestAdAdapter");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200,
                jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse,
                AdFormat.REWARDED_AD, adUnitId);

        CreativeExperienceSettings cachedSettings = CreativeExperienceSettingsParser.parse(
                CreativeExperienceSettingsParserTest.getCeSettingsJSONObject(), true);
        PowerMockito.doAnswer(invocation -> {
            CESettingsCacheService.CESettingsCacheListener cacheListener = invocation
                    .getArgumentAt(1,  CESettingsCacheService.CESettingsCacheListener.class);
            cacheListener.onSettingsReceived(cachedSettings);
            return null;
        }).when(CESettingsCacheService.class, "getCESettings",
                anyString(),
                any(CESettingsCacheService.CESettingsCacheListener.class),
                any(Context.class));

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd(adUnitId, null);
        requestListener.onResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        // Verify adapter is instantiated
        assertNotNull(MoPubRewardedAdManager.getRewardedAdData().getAdAdapter(adUnitId));
        // Verify the cached settings are used
        CreativeExperienceSettings ceSettingsUnderTest = MoPubRewardedAdManager
                .getCreativeExperienceSettings();
        assertEquals(cachedSettings, ceSettingsUnderTest);
    }

    @Test
    public void onRewardedAdClosed_shouldSetHasAdFalse() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedAdManagerTest$TestAdAdapter");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200, jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd(adUnitId, null);

        ShadowLooper.unPauseMainLooper();

        requestListener.onResponse(testResponse);
        MoPubRewardedAdManager.getRewardedAdData().getAdAdapter(adUnitId).onAdLoaded();
        MoPubRewardedAdManager.getRewardedAdData().getAdAdapter(adUnitId).onAdShown();

        assertThat(MoPubRewardedAdManager.hasAd(adUnitId)).isTrue();
        MoPubRewardedAdManager.showAd(adUnitId);
        verify(mockRewardedAdListener).onRewardedAdLoadSuccess(eq(adUnitId));
        assertThat(MoPubRewardedAdManager.hasAd(adUnitId)).isTrue();
        verify(mockRewardedAdListener).onRewardedAdStarted(eq(adUnitId));
        MoPubRewardedAdManager.onRewardedAdClosed(TestFullscreenAdAdapterFactory.getSingletonMock(), adUnitId);
        assertThat(MoPubRewardedAdManager.hasAd(adUnitId)).isFalse();
    }

    @Test
    public void showAd_whenNotHasAd_shouldFail() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedAdManagerTest$NoAdAdAdapter");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200, jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd(adUnitId, null);

        ShadowLooper.unPauseMainLooper();

        requestListener.onResponse(testResponse);
        MoPubRewardedAdManager.getRewardedAdData().getAdAdapter(adUnitId).onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);

        verify(mockRewardedAdListener).onRewardedAdLoadFailure(eq(adUnitId), eq(MoPubErrorCode.NETWORK_NO_FILL));

        assertThat(MoPubRewardedAdManager.hasAd(adUnitId)).isFalse();
        MoPubRewardedAdManager.showAd(adUnitId);
        verify(mockRewardedAdListener).onRewardedAdLoadFailure(eq(adUnitId), eq(MoPubErrorCode.AD_NOT_AVAILABLE));
    }

    @Test
    public void showAd_withMultiRewardedCurrenciesJsonHeader_whenRewardNotSelected_shouldFail() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedAdManagerTest$TestAdAdapter");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_CURRENCIES.getKey(), new JSONObject(MULTI_CURRENCIES_JSON_4));

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200, jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd(adUnitId, null);

        ShadowLooper.unPauseMainLooper();

        requestListener.onResponse(testResponse);
        MoPubRewardedAdManager.getRewardedAdData().getAdAdapter(adUnitId).onAdLoaded();

        // Multiple rewards are available, but a reward is not selected before showing ad
        MoPubRewardedAdManager.showAd(adUnitId);
        verify(mockRewardedAdListener).onRewardedAdLoadFailure(eq(adUnitId), eq(MoPubErrorCode.REWARD_NOT_SELECTED));
    }

    @Test
    public void showAd_withMultiRewardedCurrenciesJsonHeader_whenValidRewardIsSelected_shouldUpdateRewardMappings() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), TestFullscreenAdAdapterFactory.getSingletonMock().getClass().toString());
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_CURRENCIES.getKey(), new JSONObject(MULTI_CURRENCIES_JSON_4));

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200, jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        final RewardedAdData rewardedAdData = MoPubRewardedAdManager.getRewardedAdData();
        MoPubRewardedAdManager.loadAd(adUnitId, null);

        ShadowLooper.unPauseMainLooper();

        requestListener.onResponse(testResponse);
        final AdAdapter adAdapter = rewardedAdData.getAdAdapter(adUnitId);
        adAdapter.onAdLoaded();

        Set<MoPubReward> availableRewards = MoPubRewardedAdManager.getAvailableRewards(adUnitId);
        assertThat(availableRewards.size()).isEqualTo(4);

        // Select the 10 Diamonds reward
        for (MoPubReward reward : availableRewards) {
            if (reward.getLabel().equals("Diamonds") && reward.getAmount() == 10) {
                MoPubRewardedAdManager.selectReward(adUnitId, reward);
                break;
            }
        }

        // AdUnit to MoPubReward mapping
        MoPubReward moPubReward = rewardedAdData.getMoPubReward(adUnitId);
        assertThat(moPubReward.getLabel()).isEqualTo("Diamonds");
        assertThat(moPubReward.getAmount()).isEqualTo(10);

        MoPubRewardedAdManager.showAd(adUnitId);

        // CustomEventRewardedAd class to MoPubReward mapping
        moPubReward = rewardedAdData.getLastShownMoPubReward(adAdapter);
        assertThat(moPubReward.getLabel()).isEqualTo("Diamonds");
        assertThat(moPubReward.getAmount()).isEqualTo(10);
    }

    @Test
    public void showAd_withMultiRewardedCurrenciesJsonHeader_whenSelectRewardWithWrongAdUnit_shouldFail() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedAdManagerTest$TestAdAdapter");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_CURRENCIES.getKey(), new JSONObject(MULTI_CURRENCIES_JSON_4));

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200, jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd(adUnitId, null);

        ShadowLooper.unPauseMainLooper();

        requestListener.onResponse(testResponse);
        MoPubRewardedAdManager.getRewardedAdData().getAdAdapter(adUnitId).onAdLoaded();

        Set<MoPubReward> availableRewards = MoPubRewardedAdManager.getAvailableRewards(adUnitId);
        assertThat(availableRewards.size()).isEqualTo(4);

        // Select the 10 Diamonds reward, but to a wrong AdUnit
        for (MoPubReward reward : availableRewards) {
            if (reward.getLabel().equals("Diamonds") && reward.getAmount() == 10) {
                MoPubRewardedAdManager.selectReward("wrongAdUnit", reward);
                break;
            }
        }

        // No selected reward is mapped to AdUnit
        assertThat(MoPubRewardedAdManager.getRewardedAdData().getMoPubReward(adUnitId)).isNull();

        MoPubRewardedAdManager.showAd(adUnitId);
        verify(mockRewardedAdListener).onRewardedAdLoadFailure(eq(adUnitId), eq(MoPubErrorCode.REWARD_NOT_SELECTED));
    }

    @Test
    public void showAd_withMultiRewardedCurrenciesJsonHeader_whenSelectedRewardIsNotAvailable_shouldFail() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedAdManagerTest$TestAdAdapter");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_CURRENCIES.getKey(), new JSONObject(MULTI_CURRENCIES_JSON_4));

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200, jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd(adUnitId, null);

        ShadowLooper.unPauseMainLooper();

        requestListener.onResponse(testResponse);
        MoPubRewardedAdManager.getRewardedAdData().getAdAdapter(adUnitId).onAdLoaded();

        Set<MoPubReward> availableRewards = MoPubRewardedAdManager.getAvailableRewards(adUnitId);
        assertThat(availableRewards.size()).isEqualTo(4);

        // Select a reward that's not in the returned set of available rewards
        MoPubRewardedAdManager.selectReward(adUnitId, MoPubReward.success("fake reward", 99));

        // No selected reward is mapped to AdUnit
        assertThat(MoPubRewardedAdManager.getRewardedAdData().getMoPubReward(adUnitId)).isNull();

        MoPubRewardedAdManager.showAd(adUnitId);
        verify(mockRewardedAdListener).onRewardedAdLoadFailure(eq(adUnitId), eq(MoPubErrorCode.REWARD_NOT_SELECTED));
    }

    @Test
    public void showAd_withSingleRewardedCurrencyJsonHeader_whenRewardNotSelected_shouldSelectOnlyRewardAutomatically() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), TestFullscreenAdAdapterFactory.getSingletonMock().getClass().toString());
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_CURRENCIES.getKey(), MULTI_CURRENCY_JSON_1);

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200, jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        final RewardedAdData rewardedAdData = MoPubRewardedAdManager.getRewardedAdData();
        MoPubRewardedAdManager.loadAd(adUnitId, null);

        ShadowLooper.unPauseMainLooper();

        requestListener.onResponse(testResponse);
        final AdAdapter adAdapter = rewardedAdData.getAdAdapter(adUnitId);
        adAdapter.onAdLoaded();

        // There's only one reward in the set of available rewards for this AdUnit
        assertThat(MoPubRewardedAdManager.getAvailableRewards(adUnitId).size()).isEqualTo(1);

        // The only reward is automatically mapped to this AdUnit
        MoPubReward moPubReward = rewardedAdData.getMoPubReward(adUnitId);
        assertThat(moPubReward.getLabel()).isEqualTo("Coins");
        assertThat(moPubReward.getAmount()).isEqualTo(25);

        MoPubRewardedAdManager.showAd(adUnitId);

        // CustomEventRewardedAd class to MoPubReward mapping
        moPubReward = rewardedAdData.getLastShownMoPubReward(adAdapter);
        assertThat(moPubReward.getLabel()).isEqualTo("Coins");
        assertThat(moPubReward.getAmount()).isEqualTo(25);
    }

    @Test
    public void showAd_withLegacyRewardedCurrencyHeaders_shouldUpdateLastShownCustomEventRewardMapping() throws Exception {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedAdManagerTest$TestAdAdapter");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_VIDEO_CURRENCY_NAME.getKey(), "currency_name");
        metadata.put(ResponseHeader.REWARDED_VIDEO_CURRENCY_AMOUNT.getKey(), 123);
        metadata.put(ResponseHeader.REWARDED_CURRENCIES.getKey(), "");

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200, jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd(adUnitId, null);

        ShadowLooper.unPauseMainLooper();

        requestListener.onResponse(testResponse);
        final AdAdapter adAdapter = MoPubRewardedAdManager.getRewardedAdData().getAdAdapter(adUnitId);
        adAdapter.onAdLoaded();

        MoPubRewardedAdManager.showAd(adUnitId);
        adAdapter.onAdShown();

        MoPubReward moPubReward =
                MoPubRewardedAdManager.getRewardedAdData().getLastShownMoPubReward(adAdapter);
        assertThat(moPubReward.getAmount()).isEqualTo(123);
        assertThat(moPubReward.getLabel()).isEqualTo("currency_name");
    }

    @Test
    public void showAd_withCustomDataShorterThanLengthMaximum_shouldNotLogWarning() {
        MultiAdResponse multiAdResponse = Mockito.mock(MultiAdResponse.class);
        when(multiAdResponse.getFailURL()).thenReturn("failUrl");
        AdResponse testResponse = new AdResponse.Builder()
                .setBaseAdClassName("com.mopub.mobileads.MoPubRewardedAdManagerTest$TestAdAdapter")
                .setAdType(AdType.CUSTOM)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd(adUnitId, null);
        requestListener.onResponse(multiAdResponse);

        ShadowLooper.unPauseMainLooper();

        MoPubRewardedAdManager.showAd(adUnitId,
                createStringWithLength(MoPubRewardedAdManager.CUSTOM_DATA_MAX_LENGTH_BYTES - 1));

        for (final ShadowLog.LogItem logItem : ShadowLog.getLogs()) {
            if (logItem.msg.toLowerCase().contains(CUSTOM_DATA_MAX_LENGTH_EXCEEDED_MESSAGE)) {
                fail(String.format(Locale.US, "Log item '%s' not expected, found.", CUSTOM_DATA_MAX_LENGTH_EXCEEDED_MESSAGE));
            }
        }
    }

    @Test
    public void showAd_withCustomDataGreaterThanLengthMaximum_shouldLogWarning() {
        MultiAdResponse multiAdResponse = Mockito.mock(MultiAdResponse.class);
        when(multiAdResponse.getFailURL()).thenReturn("failUrl");
        AdResponse testResponse = new AdResponse.Builder()
                .setBaseAdClassName("com.mopub.mobileads.MoPubRewardedAdManagerTest$TestAdAdapter")
                .setAdType(AdType.CUSTOM)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.loadAd(adUnitId, null);
        requestListener.onResponse(multiAdResponse);

        ShadowLooper.unPauseMainLooper();

        MoPubRewardedAdManager.showAd(adUnitId,
                createStringWithLength(MoPubRewardedAdManager.CUSTOM_DATA_MAX_LENGTH_BYTES  + 1));

        for (final ShadowLog.LogItem logItem : ShadowLog.getLogs()) {
            if (logItem.msg.toLowerCase().contains(CUSTOM_DATA_MAX_LENGTH_EXCEEDED_MESSAGE)) {
                // Test passes the first time we see the warning log message
                return;
            }
        }
        fail(String.format(Locale.US, "Expected log item '%s' not found.",
                CUSTOM_DATA_MAX_LENGTH_EXCEEDED_MESSAGE));
    }

    @Test
    public void onAdFailure_shouldCallFailCallback() throws JSONException {
        MoPubNetworkError e = new MoPubNetworkError.Builder("testError!").build();

        MoPubRewardedAdManager.loadAd(adUnitId, null);

        JSONObject jsonBody = new JSONObject(new String(request.getBody()));
        assertThat(jsonBody.get("id")).isEqualTo(adUnitId);
        requestListener.onErrorResponse(e);
        verify(mockRewardedAdListener).onRewardedAdLoadFailure(anyString(), any(MoPubErrorCode.class));
        verifyNoMoreInteractions(mockRewardedAdListener);
    }

    @Test
    public void chooseReward_shouldReturnMoPubRewardOverNetworkReward() {
        MoPubReward moPubReward = MoPubReward.success(MOPUB_REWARD, 123);
        MoPubReward networkReward = MoPubReward.success("network_reward", 456);

        MoPubReward chosenReward =
                MoPubRewardedAdManager.chooseReward(moPubReward, networkReward);
        assertThat(chosenReward).isEqualTo(moPubReward);
    }

    @Test
    public void chooseReward_withNetworkRewardNotSuccessful_shouldReturnNetworkReward() {
        MoPubReward moPubReward = MoPubReward.success(MOPUB_REWARD, 123);
        MoPubReward networkReward = MoPubReward.failure();

        MoPubReward chosenReward =
                MoPubRewardedAdManager.chooseReward(moPubReward, networkReward);
        assertThat(chosenReward).isEqualTo(networkReward);
    }

    @Test
    public void onRewardedAdCompleted_withEmptyServerCompletionUrl_withCurrentlyShowingAdUnitId_shouldNotifyRewardedAdCompletedForOneAdUnitId() {
        MoPubReward moPubReward = MoPubReward.success(MOPUB_REWARD, 123);
        RewardedAdData rewardedAdData = MoPubRewardedAdManager.getRewardedAdData();

        final FullscreenAdAdapter mockAdAdapterOne = mock(FullscreenAdAdapter.class);
        final FullscreenAdAdapter mockAdAdapterTwo = mock(FullscreenAdAdapter.class);

        rewardedAdData.setCurrentlyShowingAdUnitId("testAdUnit1");
        MoPubRewardedAdManager.getRewardedAdData().updateAdUnitAdAdapterMapping(
                "testAdUnit2", mockAdAdapterOne);
        MoPubRewardedAdManager.getRewardedAdData().updateAdUnitAdAdapterMapping(
                "testAdUnit2", mockAdAdapterTwo);
        // Server completion url empty and base ad has no server reward set

        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.onRewardedAdCompleted(mockAdAdapterOne, mockAdAdapterOne.getAdNetworkId(),
                moPubReward);

        ShadowLooper.unPauseMainLooper();

        ArgumentCaptor<Set<String>> rewardedIdsCaptor = ArgumentCaptor.forClass((Class) Set.class);
        verify(mockRewardedAdListener).onRewardedAdCompleted(rewardedIdsCaptor.capture(),
                eq(moPubReward));
        assertThat(rewardedIdsCaptor.getValue()).containsOnly("testAdUnit1");
    }

    @Test
    public void onRewardedAdCompleted_withEmptyServerCompletionUrl_withNoCurrentlyShowingAdUnitId_shouldNotifyRewardedAdCompletedForAllAdUnitIds() {
        MoPubReward moPubReward = MoPubReward.success(MOPUB_REWARD, 123);
        RewardedAdData rewardedAdData = MoPubRewardedAdManager.getRewardedAdData();
        rewardedAdData.setCurrentlyShowingAdUnitId(null);
        rewardedAdData.updateAdUnitAdAdapterMapping("testAdUnit1", TestFullscreenAdAdapterFactory.getSingletonMock() //new TestAdAdapter(),
        );
        rewardedAdData.updateAdUnitAdAdapterMapping("testAdUnit2", TestFullscreenAdAdapterFactory.getSingletonMock() //new TestAdAdapter(),
        );
        rewardedAdData.updateAdUnitAdAdapterMapping("testAdUnit3", TestFullscreenAdAdapterFactory.getSingletonMock() //new TestAdAdapter(),
        );
        // Server completion url empty and base ad has no server reward set

        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.onRewardedAdCompleted(TestFullscreenAdAdapterFactory.getSingletonMock(), TestAdAdapter.AD_NETWORK_ID,
                moPubReward);

        ShadowLooper.unPauseMainLooper();

        ArgumentCaptor<Set<String>> rewardedIdsCaptor = ArgumentCaptor.forClass((Class) Set.class);
        verify(mockRewardedAdListener).onRewardedAdCompleted(rewardedIdsCaptor.capture(),
                eq(moPubReward));
        assertThat(rewardedIdsCaptor.getValue()).containsOnly("testAdUnit1", "testAdUnit2",
                "testAdUnit3");
    }

    @Test
    public void onRewardedAdCompleted_withServerCompletionUrl_shouldMakeRewardedAdCompletionRequest_shouldNotifyRewardedAdCompleted() {
        MoPubReward moPubReward = MoPubReward.success(MOPUB_REWARD, 123);
        RewardedAdData rewardedAdData = MoPubRewardedAdManager.getRewardedAdData();
        rewardedAdData.setCurrentlyShowingAdUnitId("testAdUnit1");

        // Set server-side reward, different from moPubReward, and corresponding server completion URL
        rewardedAdData.updateAdUnitRewardMapping("testAdUnit1", "server-side currency", "777");
        rewardedAdData.updateAdUnitToServerCompletionUrlMapping("testAdUnit1", "testUrl");

        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.onRewardedAdCompleted(TestFullscreenAdAdapterFactory.getSingletonMock(), TestAdAdapter.AD_NETWORK_ID,
                moPubReward);

        ShadowLooper.unPauseMainLooper();

        verify(mockRequestQueue).add(any(RewardedAdCompletionRequest.class));
        assertThat(rewardedAdCompletionRequest.getUrl()).contains("testUrl");
        assertThat(rewardedAdCompletionRequest.getUrl()).contains("&rcn=server-side%20currency");
        assertThat(rewardedAdCompletionRequest.getUrl()).contains("&rca=777");
        ArgumentCaptor<Set<String>> rewardedIdsCaptor = ArgumentCaptor.forClass((Class) Set.class);
        verify(mockRewardedAdListener).onRewardedAdCompleted(rewardedIdsCaptor.capture(),
                eq(moPubReward));
        assertThat(rewardedIdsCaptor.getValue()).containsOnly("testAdUnit1");
    }

    @Test
    public void onRewardedAdCompleted_shouldMakeRewardedAdCompletionRequestIncludingClassName() {
        MoPubReward moPubReward = MoPubReward.success(MOPUB_REWARD, 123);
        RewardedAdData rewardedAdData = MoPubRewardedAdManager.getRewardedAdData();
        rewardedAdData.setCurrentlyShowingAdUnitId("testAdUnit1");
        rewardedAdData.updateAdUnitAdAdapterMapping("testAdUnit1", TestFullscreenAdAdapterFactory.getSingletonMock());
        rewardedAdData.updateAdUnitToServerCompletionUrlMapping("testAdUnit1", "testUrl");
        rewardedAdData.updateAdUnitToCustomDataMapping("testAdUnit1", "very%=custom@[data]");

        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.onRewardedAdCompleted(TestFullscreenAdAdapterFactory.getSingletonMock(),
                TestAdAdapter.AD_NETWORK_ID, moPubReward);

        ShadowLooper.unPauseMainLooper();

        verify(mockRequestQueue).add(any(RewardedAdCompletionRequest.class));
        assertThat(rewardedAdCompletionRequest.getUrl()).contains(
                "cec=" + MoPubFullscreen.class.getName());
    }

    @Test
    public void onRewardedAdCompleted_withCustomData_shouldMakeRewardedAdCompletionRequestIncludingCustomData() {
        MoPubReward moPubReward = MoPubReward.success(MOPUB_REWARD, 123);
        RewardedAdData rewardedAdData = MoPubRewardedAdManager.getRewardedAdData();
        rewardedAdData.setCurrentlyShowingAdUnitId("testAdUnit1");

        rewardedAdData.updateAdUnitToServerCompletionUrlMapping("testAdUnit1", "testUrl");
        rewardedAdData.updateAdUnitToCustomDataMapping("testAdUnit1", "very%=custom@[data]");

        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.onRewardedAdCompleted(TestFullscreenAdAdapterFactory.getSingletonMock(),
                TestAdAdapter.AD_NETWORK_ID, moPubReward);

        ShadowLooper.unPauseMainLooper();

        verify(mockRequestQueue).add(any(RewardedAdCompletionRequest.class));
        assertThat(rewardedAdCompletionRequest.getUrl()).contains(
                "&rcd=very%25%3Dcustom%40%5Bdata%5D");
    }

    @Test
    public void onRewardedAdCompleted_withNullCustomData_shouldMakeRewardedAdCompletionRequestWithoutCustomData() {
        MoPubReward moPubReward = MoPubReward.success(MOPUB_REWARD, 123);
        RewardedAdData rewardedAdData = MoPubRewardedAdManager.getRewardedAdData();
        rewardedAdData.setCurrentlyShowingAdUnitId("testAdUnit1");

        rewardedAdData.updateAdUnitToServerCompletionUrlMapping("testAdUnit1", "testUrl");
        rewardedAdData.updateAdUnitToCustomDataMapping("testAdUnit1", null);

        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.onRewardedAdCompleted(TestFullscreenAdAdapterFactory.getSingletonMock(),
                TestAdAdapter.AD_NETWORK_ID, moPubReward);

        ShadowLooper.unPauseMainLooper();

        verify(mockRequestQueue).add(any(RewardedAdCompletionRequest.class));
        assertThat(rewardedAdCompletionRequest.getUrl()).doesNotContain("&rcd=");
    }

    @Test
    public void onRewardedAdCompleted_withServerCompletionUrl_withNullRewardForCurrentlyShowingAdUnitId_shouldMakeRewardedAdCompletionRequestWithDefaultRewardValues() {
        MoPubReward moPubReward = MoPubReward.success(MOPUB_REWARD, 123);
        RewardedAdData rewardedAdData = MoPubRewardedAdManager.getRewardedAdData();
        rewardedAdData.setCurrentlyShowingAdUnitId("testAdUnit1");

        // Set reward fields to nulls
        rewardedAdData.updateAdUnitRewardMapping("testAdUnit1", null, null);
        rewardedAdData.updateAdUnitToServerCompletionUrlMapping("testAdUnit1", "testUrl");

        ShadowLooper.pauseMainLooper();

        MoPubRewardedAdManager.onRewardedAdCompleted(TestFullscreenAdAdapterFactory.getSingletonMock(), TestAdAdapter.AD_NETWORK_ID,
                moPubReward);

        ShadowLooper.unPauseMainLooper();

        verify(mockRequestQueue).add(any(RewardedAdCompletionRequest.class));
        assertThat(rewardedAdCompletionRequest.getUrl()).contains("testUrl");
        // Default reward values
        assertThat(rewardedAdCompletionRequest.getUrl()).contains("&rcn=&rca=0");
    }

    @Test
    public void onRewardedAdLoadFailure_withExpirationErrorCode_shouldCallFailCallback_shouldNotLoadFailUrl() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        jsonResponse.put(ResponseHeader.FAIL_URL.getKey(), "fail_url");
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedAdManagerTest$TestAdAdapter");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);

        MoPubNetworkResponse netResponse = new MoPubNetworkResponse(200, jsonResponse.toString().getBytes(), Collections.emptyMap());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_AD, adUnitId);

        MoPubRewardedAdManager.loadAd(adUnitId, null);
        requestListener.onResponse(testResponse);
        MoPubRewardedAdManager.getRewardedAdData().getAdAdapter(adUnitId).onAdLoadFailed(MoPubErrorCode.EXPIRED);

        verify(mockRewardedAdListener).onRewardedAdShowError(eq(adUnitId),
                eq(MoPubErrorCode.EXPIRED));
        verifyNoMoreInteractions(mockRewardedAdListener);
        verify(mockRequestQueue).add(any(MultiAdRequest.class));
        verifyNoMoreInteractions(mockRequestQueue);
    }

    private String createStringWithLength(int length) {
        if (length < 1) {
            return "";
        }

        char[] chars = new char[length];
        Arrays.fill(chars, '*');
        return new String(chars);
    }

    public static class TestAdAdapter extends FullscreenAdAdapter {
        public static final String AD_NETWORK_ID = "id!";

        boolean mPlayable = false;
        private AdData mAdData;

        public TestAdAdapter(@NonNull Context context,
                             @NonNull final String className,
                             @NonNull final AdData adData) throws BaseAdNotFoundException {
            super(context, className, adData);
            mPlayable = true;
            mAdData = adData;
            MoPubRewardedAdManager.onRewardedAdLoadSuccess(this, AD_NETWORK_ID);
        }

        @Nullable
        protected LifecycleListener getLifecycleListener() {
            return null;
        }

        @NonNull
        @Override
        protected String getAdNetworkId() {
            return AD_NETWORK_ID;
        }

        @Override
        public void onAdLoaded() {
            mPlayable = true;
            MoPubRewardedAdManager.onRewardedAdLoadSuccess(TestFullscreenAdAdapterFactory.getSingletonMock(),
                    TestAdAdapter.AD_NETWORK_ID);
        }

        @Override
        public boolean isReady() {
            return mPlayable;
        }

        @Override
        public void show(@Nullable MoPubAd moPubAd) {
            MoPubRewardedAdManager.onRewardedAdStarted(TestFullscreenAdAdapterFactory.getSingletonMock(), TestAdAdapter.AD_NETWORK_ID);
        }

        @Override
        void invalidate() {
            mPlayable = false;
        }

        @Nullable
        AdData getAdData() {
            return mAdData;
        }
    }

    public static class NoAdAdAdapter extends TestAdAdapter {

        public NoAdAdAdapter(@NonNull Context context,
                             @NonNull final String className,
                             @NonNull final AdData adData) throws BaseAdNotFoundException {
            super(context, className, adData);
            mPlayable = false;
            MoPubRewardedAdManager.onRewardedAdLoadFailure(this, TestAdAdapter.AD_NETWORK_ID, MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    private static class RequestBodyContains extends ArgumentMatcher<MoPubRequest> {

        private final String mMustContain;

        RequestBodyContains(String stringToFind) {
            mMustContain = stringToFind;
        }

        @Override
        public boolean matches(final Object argument) {
            return argument instanceof MoPubRequest
                        && new String(((MoPubRequest<?>) argument).getBody()).contains(mMustContain);
        }
    }

    private static JSONObject createRewardedJsonResponse() throws JSONException {
        final String jsonString = "{\n" +
                "  \"ad-responses\": [\n" +
                "    {\n" +
                "      \"content\": \"<VAST version=\\\"2.0\\\">\\r\\n  <Ad id=\\\"1\\\">\\r\\n    <InLine>\\r\\n      <AdSystem>MoPub</AdSystem>\\r\\n      <AdTitle>MoPub Video Test Ad</AdTitle>\\r\\n      <Impression>\\r\\n        <![CDATA[https://d30x8mtr3hjnzo.cloudfront.net/client/images/vastimp1x1.png?1519938200329]]>\\r\\n      </Impression>\\r\\n      <Creatives>\\r\\n        <Creative>\\r\\n          <Linear>\\r\\n            <Duration>00:00:30</Duration>\\r\\n            <VideoClicks>\\r\\n              <ClickThrough>\\r\\n                <![CDATA[mopubnativebrowser://navigate?url=http%3A%2F%2Fwww.mopub.com]]>\\r\\n              </ClickThrough>\\r\\n            </VideoClicks>\\r\\n            <MediaFiles>\\r\\n              <MediaFile delivery=\\\"progressive\\\" type=\\\"video/mp4\\\" bitrate=\\\"325\\\" width=\\\"640\\\" height=\\\"360\\\">\\r\\n                <![CDATA[https://d2al1opqne3nsh.cloudfront.net/videos/corgi_30s_640x360_baseline_30.mp4]]>\\r\\n              </MediaFile>\\r\\n            </MediaFiles>\\r\\n          </Linear>\\r\\n        </Creative>\\r\\n        <Creative>\\r\\n          <CompanionAds>\\r\\n            <Companion width=\\\"640\\\" height=\\\"360\\\">\\r\\n              <StaticResource creativeType=\\\"image/jpeg\\\">\\r\\n                <![CDATA[https://d2al1opqne3nsh.cloudfront.net/images/igetbeggin_640x360.jpg]]>\\r\\n              </StaticResource>\\r\\n              <TrackingEvents>\\r\\n                <Tracking event=\\\"creativeView\\\">\\r\\n                  <![CDATA[https://www.mopub.com/?q=companionTracking640x360]]>\\r\\n                </Tracking>\\r\\n              </TrackingEvents>\\r\\n              <CompanionClickThrough>\\r\\n                <![CDATA[https://www.mopub.com/?q=companionClickThrough640x360]]>\\r\\n              </CompanionClickThrough>\\r\\n            </Companion>\\r\\n          </CompanionAds>\\r\\n        </Creative>\\r\\n      </Creatives>\\r\\n    </InLine>\\r\\n  </Ad>\\r\\n</VAST> <MP_TRACKING_URLS>  </MP_TRACKING_URLS> \",\n" +
                "      \"metadata\": {\n" +
                "        \"content-type\": \"text/html; charset=UTF-8\",\n" +
                "        \"x-ad-timeout-ms\": 0,\n" +
                "        \"x-adgroupid\": \"b4148ea9ed7b4003b9d7c1e61036e0b1\",\n" +
                "        \"x-adtype\": \"rewarded_video\",\n" +
                "        \"x-backgroundcolor\": \"\",\n" +
                "        \"x-banner-impression-min-ms\": \"\",\n" +
                "        \"x-banner-impression-min-pixels\": \"\",\n" +
                "        \"x-before-load-url\": \"\",\n" +
                "        \"x-browser-agent\": -1,\n" +
//                "        \"clicktrackers\": \"http://ads-staging.mopub.com/m/aclk?appid=&cid=4652bd83d89a40c5a4e276dbf101499f&city=San%20Francisco&ckv=2&country_code=US&cppck=E3A19&dev=Android%20SDK%20built%20for%20x86&exclude_adgroups=b4148ea9ed7b4003b9d7c1e61036e0b1&id=920b6145fb1546cf8b5cf2ac34638bb7&is_mraid=0&os=Android&osv=8.0.0&req=5e3d79f17abb48468d95fde17e82f7f6&reqt=1519938200.0&rev=0&udid=ifa%3Abd9022e4-5ced-4af2-8cba-dd15ffa715ee&video_type=\",\n" +
                "        \"clicktrackers\": \"\",\n" +
                "        \"x-creativeid\": \"4652bd83d89a40c5a4e276dbf101499f\",\n" +
                "        \"x-custom-event-class-data\": \"\",\n" +
                "        \"x-custom-event-class-name\": \"\",\n" +
                "        \"x-disable-viewability\": 3,\n" +
                "        \"x-dspcreativeid\": \"\",\n" +
                "        \"x-format\": \"\",\n" +
                "        \"x-fulladtype\": \"vast\",\n" +
                "        \"x-height\": -1,\n" +
                "        \"x-imptracker\": \"https://ads.mopub.com/m/imp?appid=&cid=4652bd83d89a40c5a4e276dbf101499f&city=San%20Francisco&ckv=2&country_code=US&cppck=6A575&dev=Android%20SDK%20built%20for%20x86&exclude_adgroups=b4148ea9ed7b4003b9d7c1e61036e0b1&id=920b6145fb1546cf8b5cf2ac34638bb7&is_ab=0&is_mraid=0&os=Android&osv=8.0.0&req=5e3d79f17abb48468d95fde17e82f7f6&reqt=1519938200.0&rev=0.000050&udid=ifa%3Abd9022e4-5ced-4af2-8cba-dd15ffa715ee&video_type=\",\n" +
                "        \"x-interceptlinks\": \"\",\n" +
                "        \"x-nativeparams\": \"\",\n" +
                "        \"x-networktype\": \"\",\n" +
                "        \"x-orientation\": \"l\",\n" +
                "        \"x-precacherequired\": \"1\",\n" +
                "        \"x-refreshtime\": 30,\n" +
                "        \"x-rewarded-currencies\": {\n" +
                "          \"rewards\": [ { \"name\": \"Coins\", \"amount\": 15 } ]\n" +
                "        },\n" +
                "        \"x-rewarded-video-completion-url\": \"\",\n" +
                "        \"x-rewarded-video-currency-amount\": 10,\n" +
                "        \"x-rewarded-video-currency-name\": \"Coins\",\n" +
                "        \"x-vastvideoplayer\": \"\",\n" +
                "        \"x-video-trackers\": \"\",\n" +
                "        \"x-width\": -1\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
//                "  \"x-next-url\": \"http://ads-staging.mopub.com/m/ad?v=6&id=920b6145fb1546cf8b5cf2ac34638bb7&nv=6.1&dn=Google%2CAndroid%20SDK%20built%20for%20x86%2Csdk_gphone_x86&bundle=com.mopub.simpleadsdemo&z=%2B0000&o=p&w=1080&h=1920&sc_a=2.625&mcc=310&mnc=260&iso=us&cn=Android&ct=3&av=4.20.0&udid=ifa%3Abd9022e4-5ced-4af2-8cba-dd15ffa715ee&dnt=0&mr=1&vv=3&exclude=b4148ea9ed7b4003b9d7c1e61036e0b1&request_id=5e3d79f17abb48468d95fde17e82f7f6&fail=1\"\n" +
                "  \"x-next-url\": \"\",\n" +
                "  \"adunit-format\": \"full\"\n" +
                "}";

        return new JSONObject(jsonString);
    }
}
