// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

/*
    {
        "x-next-url": "fail_url",
        "adunit-format": "mock_format",
        "rewarded": 0,
        "ad-responses": [
        {
            "content": "content_body",
            "metadata": {
                    "content-type": "text/html; charset=UTF-8",
                    "x-adgroupid": "some_ad_group_id",
                    "x-adtype": "html",
                    "x-backgroundcolor": "",
                    "x-banner-impression-min-ms": "",
                    "x-banner-impression-min-pixels": "",
                    "x-browser-agent": -1,
                    "clicktrackers": "clicktracking_url",
                    "x-creativeid": "d06f9bde98134f76931cdf04951b60dd",
                    "x-custom-event-class-data": "",
                    "x-custom-event-class-name": "",
                    "x-disable-viewability": 3,
                    "x-dspcreativeid": "",
                    "x-format": "",
                    "x-fulladtype": "",
                    "x-height": 50,
                    "x-imptracker": "imptracker_url",
                    "x-before-load-url": "before_load_url",
                    "x-after-load-url": "after_load_url",
                    "x-after-load-success-url": "after_load_success_url",
                    "x-after-load-fail-url": "after_load_fail_url",
                    "x-interceptlinks": "",
                    "x-nativeparams": "",
                    "x-networktype": "",
                    "x-orientation": "",
                    "x-precacherequired": "",
                    "x-refreshtime": 15,
                    "x-rewarded-currencies": "",
                    "x-rewarded-video-completion-url": "",
                    "x-rewarded-video-currency-amount": -1,
                    "x-rewarded-video-currency-name": "",
                    "x-vastvideoplayer": "",
                    "x-video-trackers": "",
                    "x-width": 320
                }
            }
        ],
        "creative_experience_settings": {
                "hash" : "12345",
                "main_ad": {
                    "min_next_action_secs" :  10,
                    "cd_delay_secs" : 5,
                    "show_cd" : 0
                },
                "end_card": {
                    "cd_delay_secs" : 5,
                    "show_cd" : 1
                },
                "max_ad_time_secs": 30,
                "ec_durs_secs" : {
                    "static" : 2,
                    "interactive" : 30,
                    "min_static" : 2,
                    "min_interactive" : 25
                },
                "video_skip_thresholds_secs" : [
                    {
                        "min": 15,
                        "after": 5
                    },
                    {
                        "min": 30,
                        "after": 5
                    }
                ]
         }
    }
*/

import android.app.Activity;

import com.mopub.common.AdFormat;
import com.mopub.common.AdType;
import com.mopub.common.Constants;
import com.mopub.common.DataKeys;
import com.mopub.common.ViewabilityManager;
import com.mopub.common.ViewabilityVendor;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.ResponseHeader;
import com.mopub.mobileads.CreativeExperienceSettings;
import com.mopub.mobileads.CreativeExperienceSettingsParser;
import com.mopub.mobileads.CreativeExperienceSettingsParserTest;
import com.mopub.mobileads.MoPubFullscreen;
import com.mopub.mobileads.MoPubInline;
import com.mopub.nativeads.MoPubCustomEventNative;
import com.mopub.network.MultiAdResponse.ServerOverrideListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.mopub.common.BrowserAgentManager.BrowserAgent.IN_APP;
import static com.mopub.common.BrowserAgentManager.BrowserAgent.NATIVE;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class MultiAdResponseTest {
    private static final String CONTENT_TYPE = "text/html; charset=UTF-8";
    private static final String FAIL_URL = "fail_url";
    private static final String CLICKTRACKING_URL = "clicktracking_url";
    private static final JSONArray CLICKTRACKING_URLS = new JSONArray().put(CLICKTRACKING_URL + "1")
            .put(CLICKTRACKING_URL + "2");
    private static final String AD_GROUP_ID = "some_ad_group_id";
    private static final String IMPTRACKER_URL = "imptracker_url";
    private static final JSONArray IMPTRACKER_URLS = new JSONArray().put("imptracker_url1").put(
            "imptracker_url2");
    private static final List<String> IMPTRACKER_URLS_LIST =
            Arrays.asList("imptracker_url1", "imptracker_url2");
    private static final String BEFORE_LOAD_URL = "before_load_url";
    private static final JSONArray BEFORE_LOAD_URLS = new JSONArray().put(BEFORE_LOAD_URL + "1")
            .put(BEFORE_LOAD_URL + "2");
    private static final String AFTER_LOAD_URL = "after_load_url";
    private static final JSONArray AFTER_LOAD_URLS = new JSONArray().put("after_load_url1").put(
            "after_load_url2");
    private static final List<String> AFTER_LOAD_URLS_LIST =
            Arrays.asList("after_load_url1", "after_load_url2");

    private static final String AFTER_LOAD_SUCCESS_URL = "after_load_success_url";
    private static final JSONArray AFTER_LOAD_SUCCESS_URLS = new JSONArray().put("after_load_success_url1").put(
            "after_load_success_url2");
    private static final List<String> AFTER_LOAD_SUCCESS_URLS_LIST =
            Arrays.asList("after_load_success_url1", "after_load_success_url2");

    private static final String AFTER_LOAD_FAIL_URL = "after_load_fail_url";
    private static final JSONArray AFTER_LOAD_FAIL_URLS = new JSONArray().put("after_load_fail_url1").put(
            "after_load_fail_url2");
    private static final List<String> AFTER_LOAD_FAIL_URLS_LIST =
            Arrays.asList("after_load_fail_url1", "after_load_fail_url2");

    private static final String ADM_VALUE = "adm_value";
    private static final String REQUEST_ID_VALUE = "request_id_value";
    private static final int REFRESH_TIME = 15;
    private static final int HEIGHT = 50;
    private static final int WIDTH = 320;
    private static final String ADUNIT_FORMAT = "mock_format";
    private static final boolean REWARDED = false;
    private static final JSONObject CREATIVE_EXPERIENCE_SETTINGS_OBJECT =
            CreativeExperienceSettingsParserTest.getCeSettingsJSONObject();

    private Activity activity;
    private String adUnitId;
    private JSONObject singleAdResponse;

    @Before
    public void setup() throws JSONException {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        adUnitId = "testAdUnitId";

        JSONObject metadata = new JSONObject();
        metadata.put(ResponseHeader.CONTENT_TYPE.getKey(), CONTENT_TYPE);
        metadata.put(ResponseHeader.AD_TYPE.getKey(), "html");
        metadata.put(ResponseHeader.AD_GROUP_ID.getKey(), AD_GROUP_ID);
        metadata.put(ResponseHeader.CLICK_TRACKING_URL.getKey(), CLICKTRACKING_URLS);
        metadata.put(ResponseHeader.IMPRESSION_URLS.getKey(), IMPTRACKER_URLS);
        metadata.put(ResponseHeader.BEFORE_LOAD_URL.getKey(), BEFORE_LOAD_URLS);
        metadata.put(ResponseHeader.AFTER_LOAD_URL.getKey(), AFTER_LOAD_URLS);
        metadata.put(ResponseHeader.AFTER_LOAD_SUCCESS_URL.getKey(), AFTER_LOAD_SUCCESS_URLS);
        metadata.put(ResponseHeader.AFTER_LOAD_FAIL_URL.getKey(), AFTER_LOAD_FAIL_URLS);
        metadata.put(ResponseHeader.REFRESH_TIME.getKey(), REFRESH_TIME);
        metadata.put(ResponseHeader.HEIGHT.getKey(), HEIGHT);
        metadata.put(ResponseHeader.WIDTH.getKey(), WIDTH);

        singleAdResponse = new JSONObject();
        singleAdResponse.put(ResponseHeader.CONTENT.getKey(), "content_text");
        singleAdResponse.put(ResponseHeader.METADATA.getKey(), metadata);
    }

    @After
    public void teardown() {
        RequestRateTrackerTest.clearRequestRateTracker();
        Locale.setDefault(Locale.US);
    }

    @Test
    public void constructor_withSingleAdResponse_shouldSucceed() throws Exception {
        byte[] body = createResponseBody(FAIL_URL, singleAdResponse);
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(200, body, Collections.emptyMap());
        MultiAdResponse subject = new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        assertThat(subject.hasNext()).isTrue();
        assertThat(subject.next()).isNotNull();
        assertThat(subject.getFailURL()).isEqualTo(FAIL_URL);
    }

    @Test
    public void constructor_withForceGdprSet_shouldCallListener() throws Exception {
        ServerOverrideListener mockOverrideListener = mock(ServerOverrideListener.class);
        MultiAdResponse.setServerOverrideListener(mockOverrideListener);

        JSONObject jsonObject = createJsonBody(FAIL_URL, singleAdResponse);
        jsonObject.put(ResponseHeader.FORCE_GDPR_APPLIES.getKey(), 1);

        byte[] body = jsonObject.toString().getBytes();
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(200, body, Collections.emptyMap());
        MultiAdResponse subject = new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        verify(mockOverrideListener).onForceGdprApplies();

        assertTrue(subject.hasNext());
        assertNotNull(subject.next());
        assertThat(subject.getFailURL()).isEqualTo(FAIL_URL);
    }

    @Test
    public void constructor_withForceExplicitNoSet_shouldCallListener() throws Exception {
        ServerOverrideListener mockOverrideListener = mock(ServerOverrideListener.class);
        MultiAdResponse.setServerOverrideListener(mockOverrideListener);

        JSONObject jsonObject = createJsonBody(FAIL_URL, singleAdResponse);
        jsonObject.put(ResponseHeader.FORCE_EXPLICIT_NO.getKey(), 1);
        jsonObject.put(ResponseHeader.CONSENT_CHANGE_REASON.getKey(), "change_reason");

        byte[] body = jsonObject.toString().getBytes();
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(200, body, Collections.emptyMap());
        new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        verify(mockOverrideListener).onForceExplicitNo("change_reason");
    }

    @Test
    public void constructor_withInvalidateConsentSet_shouldCallListener() throws Exception {
        ServerOverrideListener mockOverrideListener = mock(ServerOverrideListener.class);
        MultiAdResponse.setServerOverrideListener(mockOverrideListener);

        JSONObject jsonObject = createJsonBody(FAIL_URL, singleAdResponse);
        jsonObject.put(ResponseHeader.INVALIDATE_CONSENT.getKey(), 1);
        jsonObject.put(ResponseHeader.CONSENT_CHANGE_REASON.getKey(), "change_reason");

        byte[] body = jsonObject.toString().getBytes();
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(200, body, Collections.emptyMap());
        new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        verify(mockOverrideListener).onInvalidateConsent("change_reason");
    }

    @Test
    public void constructor_withReaquireConsentSet_shouldCallListener() throws Exception {
        ServerOverrideListener mockOverrideListener = mock(ServerOverrideListener.class);
        MultiAdResponse.setServerOverrideListener(mockOverrideListener);

        JSONObject jsonObject = createJsonBody(FAIL_URL, singleAdResponse);
        jsonObject.put(ResponseHeader.REACQUIRE_CONSENT.getKey(), 1);
        jsonObject.put(ResponseHeader.CONSENT_CHANGE_REASON.getKey(), "change_reason");

        byte[] body = jsonObject.toString().getBytes();
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(200, body, Collections.emptyMap());
        new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        verify(mockOverrideListener).onReacquireConsent("change_reason");
    }

    @Test
    public void constructor_withServerOverrideListener_shouldCallOnRequestSuccess() throws Exception {
        final ServerOverrideListener mockOverrideListener = mock(ServerOverrideListener.class);
        MultiAdResponse.setServerOverrideListener(mockOverrideListener);

        final JSONObject jsonObject = createJsonBody(FAIL_URL, singleAdResponse);

        final byte[] body = jsonObject.toString().getBytes();
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(200, body, Collections.emptyMap());
        new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        verify(mockOverrideListener).onRequestSuccess(adUnitId);
    }

    @Test(expected = JSONException.class)
    public void constructor_NonJsonBodyShouldThrowException() throws Exception {
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(400, "abc".getBytes(), Collections.emptyMap());
        new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);
    }

    @Test
    public void constructor_withResponseClear_shouldThrowNoFill() throws JSONException {
        JSONObject jsonClear = createClearAdResponse();
        byte[] body = createResponseBody(FAIL_URL, jsonClear);
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(200, body, Collections.emptyMap());

        try {
            new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);
        } catch (Exception ex) {
            if (ex instanceof MoPubNetworkError) {
                MoPubNetworkError error = (MoPubNetworkError) ex;
                assertThat(error.getReason()).isEqualTo(MoPubNetworkError.Reason.NO_FILL);
                assertThat(error.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
                return; // success
            }
        }
        assert false;
    }

    @Test
    public void constructor_withResponseClear_withNoRefreshTime_shouldThrowNoFill() throws JSONException {
        JSONObject jsonClear = createClearAdResponse();
        jsonClear.getJSONObject(ResponseHeader.METADATA.getKey()).remove(ResponseHeader.REFRESH_TIME.getKey());
        byte[] body = createResponseBody(FAIL_URL, jsonClear);
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(200, body, Collections.emptyMap());

        try {
            new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);
        } catch (Exception ex) {
            if (ex instanceof MoPubNetworkError) {
                MoPubNetworkError error = (MoPubNetworkError) ex;
                assertThat(error.getReason()).isEqualTo(MoPubNetworkError.Reason.NO_FILL);
                assertNull(error.getRefreshTimeMillis());
                return; // success
            }
        }
        assert false;
    }

    @Test
    public void constructor_withResponseWarmup_shouldThrowException() throws JSONException {
        JSONObject jsonClear = createWarmupAdResponse();
        byte[] body = createResponseBody(FAIL_URL, jsonClear);
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(200, body, Collections.emptyMap());

        try {
            new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);
        } catch (Exception ex) {
            if (ex instanceof MoPubNetworkError) {
                MoPubNetworkError error = (MoPubNetworkError) ex;
                assertThat(error.getReason()).isEqualTo(MoPubNetworkError.Reason.WARMING_UP);
                assertThat(error.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
                return; // success
            }
        }
        assert false;
    }

    @Test
    public void constructor_withResponseWarmup_withRateLimitSet_shouldSetBackoffTime() throws JSONException {
        JSONObject jsonClear = createWarmupAdResponse();
        JSONObject body = createJsonBody(FAIL_URL, jsonClear);
        addBackoffParameters(body, 50, "reason");
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(200, body.toString().getBytes(),
                Collections.emptyMap());

        try {
            new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);
        } catch (Exception ignored) {
        }

        RequestRateTracker.TimeRecord record = RequestRateTracker.getInstance().getRecordForAdUnit(adUnitId);
        assertNotNull(record);
        assertEquals(50, record.mBlockIntervalMs);
        assertEquals("reason", record.mReason);
    }

    @Test
    public void constructor_withResponseClear_withRateLimitSet_shouldSetBackoffTime() throws JSONException {
        JSONObject jsonClear = createClearAdResponse();
        JSONObject body = createJsonBody(FAIL_URL, jsonClear);
        addBackoffParameters(body, 50, "reason");
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(200, body.toString().getBytes(),
                Collections.emptyMap());

        try {
            new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);
        } catch (Exception ignored) {
        }

        RequestRateTracker.TimeRecord record = RequestRateTracker.getInstance().getRecordForAdUnit(adUnitId);
        assertNotNull(record);
        assertEquals(50, record.mBlockIntervalMs);
        assertEquals("reason", record.mReason);
    }

    @Test
    public void constructor_withRateLimitSetValue_shouldSetBackoffTimeLimit() throws Exception {
        JSONObject jsonObject = createJsonBody(FAIL_URL, singleAdResponse);
        addBackoffParameters(jsonObject, 20, "reason");
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(200, jsonObject.toString().getBytes(),
                Collections.emptyMap());

        new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        RequestRateTracker.TimeRecord record = RequestRateTracker.getInstance().getRecordForAdUnit(adUnitId);
        assertNotNull(record);
        assertThat(record.mBlockIntervalMs).isEqualTo(20);
        assertEquals("reason", record.mReason);
    }

    @Test
    public void constructor_withRateLimitSetZero_shouldResetBackoffTimeLimit() throws Exception {
        RequestRateTrackerTest.prepareRequestRateTracker(adUnitId, 99, "some_reason");

        JSONObject jsonObject = createJsonBody(FAIL_URL, singleAdResponse);
        addBackoffParameters(jsonObject, 0, "reason");
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(200, jsonObject.toString().getBytes(),
                Collections.emptyMap());

        MultiAdResponse subject = new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        RequestRateTracker.TimeRecord record = RequestRateTracker.getInstance().getRecordForAdUnit(adUnitId);
        assertNull(record);
    }

    @Test
    public void constructor_withEmptyResponseArray_shouldThrowError_shouldUseDefaultTimeout() throws JSONException {
        byte[] body = createResponseBody(FAIL_URL, null);
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(200, body, Collections.emptyMap());

        try {
            new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);
        } catch (Exception ex) {
            if (ex instanceof MoPubNetworkError) {
                MoPubNetworkError error = (MoPubNetworkError) ex;
                assertThat(error.getReason()).isEqualTo(MoPubNetworkError.Reason.NO_FILL);
                assertThat(error.getRefreshTimeMillis()).isEqualTo(Constants.THIRTY_SECONDS_MILLIS);
                return; // success
            }
        }
        assert false;
    }

    @Test
    public void constructor_withTwoAdResponses_withNonEmptyFailUrl_fullTest() throws MoPubNetworkError, JSONException {
        JSONObject secondResponse = new JSONObject(singleAdResponse.toString());
        JSONObject metadata = secondResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.AD_GROUP_ID.getKey(), AD_GROUP_ID + "_2");
        metadata.put(ResponseHeader.CLICK_TRACKING_URL.getKey(),
                new JSONArray().put(CLICKTRACKING_URL + "_2").put(CLICKTRACKING_URL + "_3"));
        metadata.put(ResponseHeader.IMPRESSION_URLS.getKey(),
                new JSONArray().put("imptracker_url3"));
        metadata.put(ResponseHeader.BEFORE_LOAD_URL.getKey(),
                new JSONArray().put(BEFORE_LOAD_URL + "_2").put(BEFORE_LOAD_URL + "_3"));
        metadata.put(ResponseHeader.AFTER_LOAD_URL.getKey(),
                new JSONArray().put(AFTER_LOAD_URL + "_2"));
        metadata.put(ResponseHeader.AFTER_LOAD_SUCCESS_URL.getKey(),
                new JSONArray().put(AFTER_LOAD_URL + "_success_2"));
        metadata.put(ResponseHeader.AFTER_LOAD_FAIL_URL.getKey(),
                new JSONArray().put(AFTER_LOAD_URL + "_fail_2"));

        JSONObject jsonObject = createJsonBody(FAIL_URL, singleAdResponse);
        jsonObject.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).put(secondResponse);
        byte[] body = jsonObject.toString().getBytes();
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(200, body, Collections.emptyMap());

        MultiAdResponse subject = new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        assertThat(subject.hasNext()).isTrue();
        assertThat(subject.getFailURL()).isEqualTo(FAIL_URL);
        AdResponse first = subject.next();
        assertNotNull(first);
        assertThat(first.getAdType()).isEqualTo("html");
        assertThat(first.getAdGroupId()).isEqualTo(AD_GROUP_ID);
        assertThat(first.getClickTrackingUrls().get(0)).isEqualTo(CLICKTRACKING_URL + "1");
        assertThat(first.getClickTrackingUrls().get(1)).isEqualTo(CLICKTRACKING_URL + "2");
        assertThat(first.getClickTrackingUrls().size()).isEqualTo(2);
        assertThat(first.getBeforeLoadUrls().get(0)).isEqualTo(BEFORE_LOAD_URL + "1");
        assertThat(first.getBeforeLoadUrls().get(1)).isEqualTo(BEFORE_LOAD_URL + "2");
        assertThat(first.getBeforeLoadUrls().size()).isEqualTo(2);
        assertThat(first.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(first.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(first.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(first.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertTrue(subject.hasNext());
        AdResponse second = subject.next();
        assertThat(second.getAdType()).isEqualTo("html");
        assertThat(second.getAdGroupId()).isEqualTo(AD_GROUP_ID + "_2");
        assertThat(second.getClickTrackingUrls().get(0)).isEqualTo(CLICKTRACKING_URL +"_2");
        assertThat(second.getClickTrackingUrls().get(1)).isEqualTo(CLICKTRACKING_URL +"_3");
        assertThat(second.getClickTrackingUrls().size()).isEqualTo(2);
        assertThat(second.getBeforeLoadUrls().get(0)).isEqualTo(BEFORE_LOAD_URL+"_2");
        assertThat(second.getBeforeLoadUrls().get(1)).isEqualTo(BEFORE_LOAD_URL+"_3");
        assertThat(second.getBeforeLoadUrls().size()).isEqualTo(2);
        assertThat(second.getAfterLoadUrls()).isEqualTo(Collections.singletonList(AFTER_LOAD_URL+"_2"));
        assertThat(second.getAfterLoadSuccessUrls()).isEqualTo(Collections.singletonList(AFTER_LOAD_URL+"_success_2"));
        assertThat(second.getAfterLoadFailUrls()).isEqualTo(Collections.singletonList(AFTER_LOAD_URL+"_fail_2"));
        assertThat(second.getImpressionTrackingUrls()).isEqualTo(Collections.singletonList("imptracker_url3"));
        assertThat(subject.getFailURL()).isEqualTo(FAIL_URL);
        assertFalse(subject.hasNext());
        assertFalse(subject.isWaterfallFinished());
    }

    @Test
    public void constructor_withTwoAdResponses_withEmptyFailUrl_fullTest() throws MoPubNetworkError, JSONException {
        JSONObject secondResponse = new JSONObject(singleAdResponse.toString());
        JSONObject metadata = secondResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.AD_GROUP_ID.getKey(), AD_GROUP_ID+"_2");
        metadata.put(ResponseHeader.CLICK_TRACKING_URL.getKey(), CLICKTRACKING_URL +"_2");
        metadata.put(ResponseHeader.IMPRESSION_URLS.getKey(), new JSONArray().put("imptracker_url3"));
        metadata.put(ResponseHeader.BEFORE_LOAD_URL.getKey(), BEFORE_LOAD_URL+"_2");
        metadata.put(ResponseHeader.AFTER_LOAD_URL.getKey(), new JSONArray().put(AFTER_LOAD_URL+"_2"));
        metadata.put(ResponseHeader.AFTER_LOAD_SUCCESS_URL.getKey(), new JSONArray().put(AFTER_LOAD_URL+"_success_2"));
        metadata.put(ResponseHeader.AFTER_LOAD_FAIL_URL.getKey(), new JSONArray().put(AFTER_LOAD_URL+"_fail_2"));

        JSONObject jsonObject = createJsonBody("", singleAdResponse);
        jsonObject.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).put(secondResponse);
        byte[] body = jsonObject.toString().getBytes();
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(200, body, Collections.emptyMap());

        MultiAdResponse subject = new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        assertThat(subject.hasNext()).isTrue();
        assertThat(subject.getFailURL()).isEqualTo("");
        AdResponse first = subject.next();
        assertNotNull(first);
        assertThat(first.getAdType()).isEqualTo("html");
        assertThat(first.getAdGroupId()).isEqualTo(AD_GROUP_ID);
        assertThat(first.getClickTrackingUrls().get(0)).isEqualTo(CLICKTRACKING_URL + "1");
        assertThat(first.getClickTrackingUrls().get(1)).isEqualTo(CLICKTRACKING_URL + "2");
        assertThat(first.getClickTrackingUrls().size()).isEqualTo(2);
        assertThat(first.getBeforeLoadUrls().get(0)).isEqualTo(BEFORE_LOAD_URL + "1");
        assertThat(first.getBeforeLoadUrls().get(1)).isEqualTo(BEFORE_LOAD_URL + "2");
        assertThat(first.getBeforeLoadUrls().size()).isEqualTo(2);
        assertThat(first.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(first.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(first.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(first.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertTrue(subject.hasNext());
        AdResponse second = subject.next();
        assertThat(second.getAdType()).isEqualTo("html");
        assertThat(second.getAdGroupId()).isEqualTo(AD_GROUP_ID + "_2");
        assertThat(second.getClickTrackingUrls().get(0)).isEqualTo(CLICKTRACKING_URL +"_2");
        assertThat(second.getBeforeLoadUrls().get(0)).isEqualTo(BEFORE_LOAD_URL+"_2");
        assertThat(second.getAfterLoadUrls()).isEqualTo(Collections.singletonList(AFTER_LOAD_URL+"_2"));
        assertThat(second.getAfterLoadSuccessUrls()).isEqualTo(Collections.singletonList(AFTER_LOAD_URL+"_success_2"));
        assertThat(second.getAfterLoadFailUrls()).isEqualTo(Collections.singletonList(AFTER_LOAD_URL+"_fail_2"));
        assertThat(second.getImpressionTrackingUrls()).isEqualTo(Collections.singletonList("imptracker_url3"));
        assertThat(subject.getFailURL()).isEqualTo("");
        assertFalse(subject.hasNext());
        assertTrue(subject.isWaterfallFinished());
    }

    @Test
    public void constructor_withEnableDebugLoggingTrue_shouldSetDebugLogLevel() throws Exception {
        // Set log level to none
        MoPubLog.setLogLevel(MoPubLog.LogLevel.NONE);

        JSONObject body = createJsonBody(FAIL_URL, singleAdResponse);
        body.put(ResponseHeader.ENABLE_DEBUG_LOGGING.getKey(), 1); // true
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(200, body.toString().getBytes(),
                Collections.emptyMap());
        MultiAdResponse subject = new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        // Get log level and check that it is now MoPubLog.LogLevel.DEBUG
        final MoPubLog.LogLevel afterLogLevel = MoPubLog.getLogLevel();
        assertThat(afterLogLevel).isEqualTo(MoPubLog.LogLevel.DEBUG);
    }

    @Test
    public void constructor_withEnableDebugLoggingFalse_shouldNotChangeLogLevel() throws Exception {
        // Set log level to none and get value from MoPubLog
        MoPubLog.setLogLevel(MoPubLog.LogLevel.NONE);
        final MoPubLog.LogLevel beforeLogLevel = MoPubLog.getLogLevel();

        JSONObject body = createJsonBody(FAIL_URL, singleAdResponse);
        body.put(ResponseHeader.ENABLE_DEBUG_LOGGING.getKey(), 0); // false
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(200, body.toString().getBytes(),
                Collections.emptyMap());
        MultiAdResponse subject = new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        // Get log level
        final MoPubLog.LogLevel afterLogLevel = MoPubLog.getLogLevel();
        assertThat(afterLogLevel).isEqualTo(beforeLogLevel);
    }

    @Test
    public void constructor_withoutEnableDebugLogging_shouldNotChangeLogLevel() throws Exception {
        // Set log level to none and get value from MoPubLog
        MoPubLog.setLogLevel(MoPubLog.LogLevel.NONE);
        final MoPubLog.LogLevel beforeLogLevel = MoPubLog.getLogLevel();

        JSONObject body = createJsonBody(FAIL_URL, singleAdResponse);
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(200, body.toString().getBytes(),
                Collections.emptyMap());
        MultiAdResponse subject = new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        // The response shouldn't have the key in the headers
        assertFalse(body.has(ResponseHeader.ENABLE_DEBUG_LOGGING.getKey()));

        // Get log level
        final MoPubLog.LogLevel afterLogLevel = MoPubLog.getLogLevel();
        assertThat(afterLogLevel).isEqualTo(beforeLogLevel);
    }

    @Test
    public void constructor_withNullRewardedValue_shouldSetRewardedToFalse() throws Exception {
        JSONObject body = createJsonBody(FAIL_URL, singleAdResponse);
        body.remove(ResponseHeader.REWARDED.getKey());

        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(200, body.toString().getBytes(),
                Collections.emptyMap());
        MultiAdResponse subject = new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        assertThat(subject.hasNext()).isTrue();
        AdResponse first = subject.next();
        assertNotNull(first);
        assertThat(first.isRewarded()).isEqualTo(false);
    }

    @Test
    public void parseNetworkResponse_forBanner_withoutImpTrackingHeaders_shouldSucceed() throws MoPubNetworkError, JSONException {
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200, singleAdResponse.toString().getBytes(),
                Collections.emptyMap());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                REWARDED,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        assertThat(subject.getAdType()).isEqualTo(AdType.HTML);
        assertThat(subject.getAdGroupId()).isEqualTo(AD_GROUP_ID);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getClickTrackingUrls().get(0)).isEqualTo(CLICKTRACKING_URL + "1");
        assertThat(subject.getClickTrackingUrls().get(1)).isEqualTo(CLICKTRACKING_URL + "2");
        assertThat(subject.getClickTrackingUrls().size()).isEqualTo(2);
        assertThat(subject.getImpressionMinVisibleDips()).isEmpty();
        assertThat(subject.getImpressionMinVisibleMs()).isEmpty();
        assertThat(subject.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertThat(subject.getBeforeLoadUrls().get(0)).isEqualTo(BEFORE_LOAD_URL + "1");
        assertThat(subject.getBeforeLoadUrls().get(1)).isEqualTo(BEFORE_LOAD_URL + "2");
        assertThat(subject.getBeforeLoadUrls().size()).isEqualTo(2);
        assertThat(subject.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(subject.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(subject.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
        assertThat(subject.getHeight()).isEqualTo(HEIGHT);
        assertThat(subject.getWidth()).isEqualTo(WIDTH);
        assertThat(subject.getStringBody()).isEqualTo("content_text");
        assertThat(subject.getBaseAdClassName()).isEqualTo(MoPubInline.class.getName());
        assertThat(subject.isRewarded()).isEqualTo(REWARDED);
        final Map<String, String> serverExtras = subject.getServerExtras();
        assertNotNull(serverExtras);
        assertThat(serverExtras.get(DataKeys.HTML_RESPONSE_BODY_KEY)).isEqualToIgnoringCase("content_text");
        assertThat(serverExtras.get(DataKeys.ADUNIT_FORMAT)).isEqualTo(ADUNIT_FORMAT);
    }

    @Test
    public void parseNetworkResponse_forBanner_withImpTrackingHeaders_shouldSucceed() throws MoPubNetworkError, JSONException {
        // add impression tracking values
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.BANNER_IMPRESSION_MIN_VISIBLE_DIPS.getKey(), 1);
        metadata.put(ResponseHeader.BANNER_IMPRESSION_MIN_VISIBLE_MS.getKey(), 2);
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200, singleAdResponse.toString().getBytes(),
                Collections.emptyMap());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                REWARDED,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        assertThat(subject.getAdType()).isEqualTo(AdType.HTML);
        assertThat(subject.getAdGroupId()).isEqualTo(AD_GROUP_ID);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getBeforeLoadUrls().size()).isEqualTo(2);
        assertThat(subject.getClickTrackingUrls().get(0)).isEqualTo(CLICKTRACKING_URL + "1");
        assertThat(subject.getClickTrackingUrls().get(1)).isEqualTo(CLICKTRACKING_URL + "2");
        assertThat(subject.getClickTrackingUrls().size()).isEqualTo(2);
        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo("1");
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo("2");
        assertThat(subject.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertThat(subject.getBeforeLoadUrls().get(0)).isEqualTo(BEFORE_LOAD_URL + "1");
        assertThat(subject.getBeforeLoadUrls().get(1)).isEqualTo(BEFORE_LOAD_URL + "2");
        assertThat(subject.getBeforeLoadUrls().size()).isEqualTo(2);
        assertThat(subject.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(subject.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(subject.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
        assertThat(subject.getHeight()).isEqualTo(HEIGHT);
        assertThat(subject.getWidth()).isEqualTo(WIDTH);
        assertThat(subject.getStringBody()).isEqualTo("content_text");
        assertThat(subject.getCustomEventClassName()).isEqualTo(MoPubInline.class.getName());
        assertThat(subject.isRewarded()).isEqualTo(REWARDED);
        final Map<String, String> serverExtras = subject.getServerExtras();
        assertNotNull(serverExtras);
        assertThat(serverExtras).isNotEmpty();
        assertThat(serverExtras.get(DataKeys.HTML_RESPONSE_BODY_KEY)).isEqualToIgnoringCase("content_text");
        assertThat(serverExtras.get(DataKeys.ADUNIT_FORMAT)).isEqualTo(ADUNIT_FORMAT);
        assertThat(subject.getImpressionData()).isNull();
    }

    @Test
    public void parseNetworkResponse_forBanner_withImpressionData_shouldSucceed() throws MoPubNetworkError, JSONException {
        // add impression data
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        JSONObject impJson = createImpressionData();
        metadata.put(ResponseHeader.IMPRESSION_DATA.getKey(), impJson);
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200, singleAdResponse.toString().getBytes(),
                Collections.emptyMap());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                REWARDED,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        assertThat(subject.getAdType()).isEqualTo(AdType.HTML);
        assertThat(subject.getAdGroupId()).isEqualTo(AD_GROUP_ID);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        ImpressionData impressionData = subject.getImpressionData();
        assert (impressionData != null);
        assertThat(impressionData.getImpressionId()).isEqualTo(impJson.getString("id"));
        assertThat(impressionData.getAppVersion()).isEqualTo(impJson.getString("app_version"));
        assertThat(impressionData.getAdUnitId()).isEqualTo(impJson.getString("adunit_id"));
        assertThat(impressionData.getAdUnitName()).isEqualTo(impJson.getString("adunit_name"));
        assertThat(impressionData.getAdUnitFormat()).isEqualTo(impJson.getString("adunit_format"));
        assertThat(impressionData.getAdGroupId()).isEqualTo(impJson.getString("adgroup_id"));
        assertThat(impressionData.getAdGroupName()).isEqualTo(impJson.getString("adgroup_name"));
        assertThat(impressionData.getAdGroupType()).isEqualTo(impJson.getString("adgroup_type"));
        assertThat(impressionData.getAdGroupPriority()).isEqualTo(impJson.getInt("adgroup_priority"));
        assertThat(impressionData.getCurrency()).isEqualTo(impJson.getString("currency"));
        assertThat(impressionData.getCountry()).isEqualTo(impJson.getString("country"));
        assertThat(impressionData.getNetworkName()).isEqualTo(impJson.getString("network_name"));
        assertThat(impressionData.getNetworkPlacementId()).isEqualTo(impJson.getString("network_placement_id"));
        assertThat(impressionData.getPublisherRevenue()).isEqualTo(impJson.getDouble("publisher_revenue"));
        assertThat(impressionData.getPrecision()).isEqualTo(impJson.getString("precision"));
        assertThat(impressionData.getDemandPartnerData())
                .isEqualsToByComparingFields(impJson.getJSONObject("demand_partner_data"));
    }

    @Test
    public void parseNetworkResponse_forBanner_withAdvancedBidding_shouldSucceed() throws MoPubNetworkError, JSONException {
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(DataKeys.ADM_KEY, ADM_VALUE);
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200, singleAdResponse.toString().getBytes(),
                Collections.emptyMap());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                FAIL_URL,
                REWARDED,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        assertThat(subject.getAdType()).isEqualTo(AdType.HTML);
        assertThat(subject.getAdGroupId()).isEqualTo(AD_GROUP_ID);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getClickTrackingUrls().get(0)).isEqualTo(CLICKTRACKING_URL + "1");
        assertThat(subject.getClickTrackingUrls().get(1)).isEqualTo(CLICKTRACKING_URL + "2");
        assertThat(subject.getClickTrackingUrls().size()).isEqualTo(2);
        assertThat(subject.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertThat(subject.getBeforeLoadUrls().get(0)).isEqualTo(BEFORE_LOAD_URL + "1");
        assertThat(subject.getBeforeLoadUrls().get(1)).isEqualTo(BEFORE_LOAD_URL + "2");
        assertThat(subject.getBeforeLoadUrls().size()).isEqualTo(2);
        assertThat(subject.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(subject.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(subject.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
        assertThat(subject.getHeight()).isEqualTo(HEIGHT);
        assertThat(subject.getWidth()).isEqualTo(WIDTH);
        assertThat(subject.getStringBody()).isEqualTo("content_text");
        assertThat(subject.getCustomEventClassName()).isEqualTo(MoPubInline.class.getName());
        assertThat(subject.isRewarded()).isEqualTo(REWARDED);
        final Map<String, String> serverExtras = subject.getServerExtras();
        assertNotNull(serverExtras);
        assertThat(serverExtras).isNotEmpty();
        assertThat(serverExtras.get(DataKeys.HTML_RESPONSE_BODY_KEY)).isEqualToIgnoringCase("content_text");
        assertThat(serverExtras.get(DataKeys.ADM_KEY)).isEqualTo(ADM_VALUE);
        assertThat(serverExtras.get(DataKeys.ADUNIT_FORMAT)).isEqualTo(ADUNIT_FORMAT);
    }

    @Test(expected = MoPubNetworkError.class)
    public void parseNetworkResponse_forNatvieStatic_withInvalidContent_throwsException() throws MoPubNetworkError, JSONException {
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.STATIC_NATIVE);
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(500, singleAdResponse.toString().getBytes(),
                Collections.emptyMap());

        MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                REWARDED,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);
    }

    @Test
    public void parseNetworkResponse_forNatvieStatic_shouldSucceed() throws MoPubNetworkError, JSONException {
        singleAdResponse.put(ResponseHeader.CONTENT.getKey(), new JSONObject());
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.IMPRESSION_MIN_VISIBLE_PERCENT.getKey(), 33);
        metadata.put(ResponseHeader.IMPRESSION_VISIBLE_MS.getKey(), 900);
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.STATIC_NATIVE);
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200, singleAdResponse.toString().getBytes(),
                Collections.emptyMap());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                REWARDED,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        assertThat(subject.getAdType()).isEqualTo(AdType.STATIC_NATIVE);
        assertThat(subject.getAdGroupId()).isEqualTo(AD_GROUP_ID);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getClickTrackingUrls().get(0)).isEqualTo(CLICKTRACKING_URL + "1");
        assertThat(subject.getClickTrackingUrls().get(1)).isEqualTo(CLICKTRACKING_URL + "2");
        assertThat(subject.getClickTrackingUrls().size()).isEqualTo(2);
        assertThat(subject.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertThat(subject.getBeforeLoadUrls().get(0)).isEqualTo(BEFORE_LOAD_URL + "1");
        assertThat(subject.getBeforeLoadUrls().get(1)).isEqualTo(BEFORE_LOAD_URL + "2");
        assertThat(subject.getBeforeLoadUrls().size()).isEqualTo(2);
        assertThat(subject.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(subject.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(subject.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
        assertThat(subject.getStringBody()).isEqualTo("{}");
        assertThat(subject.getCustomEventClassName()).isEqualTo(MoPubCustomEventNative.class.getName());
        assertThat(subject.isRewarded()).isEqualTo(REWARDED);
        final Map<String, String> serverExtras = subject.getServerExtras();
        assertNotNull(serverExtras);
        assertThat(serverExtras).isNotEmpty();
        assertThat(serverExtras.get(DataKeys.IMPRESSION_MIN_VISIBLE_PERCENT)).isEqualTo("33");
        assertThat(serverExtras.get(DataKeys.IMPRESSION_VISIBLE_MS)).isEqualTo("900");
    }

    @Test
    public void parseNetworkResponse_forNatvieStatic_withAdvancedBidding_shouldSucceed() throws MoPubNetworkError, JSONException {
        singleAdResponse.put(ResponseHeader.CONTENT.getKey(), new JSONObject());
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(DataKeys.ADM_KEY, ADM_VALUE);
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.STATIC_NATIVE);
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200, singleAdResponse.toString().getBytes(),
                Collections.emptyMap());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                REWARDED,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        assertThat(subject.getAdType()).isEqualTo(AdType.STATIC_NATIVE);
        assertThat(subject.getAdGroupId()).isEqualTo(AD_GROUP_ID);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getClickTrackingUrls().get(0)).isEqualTo(CLICKTRACKING_URL + "1");
        assertThat(subject.getClickTrackingUrls().get(1)).isEqualTo(CLICKTRACKING_URL + "2");
        assertThat(subject.getClickTrackingUrls().size()).isEqualTo(2);
        assertThat(subject.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertThat(subject.getBeforeLoadUrls().get(0)).isEqualTo(BEFORE_LOAD_URL + "1");
        assertThat(subject.getBeforeLoadUrls().get(1)).isEqualTo(BEFORE_LOAD_URL + "2");
        assertThat(subject.getBeforeLoadUrls().size()).isEqualTo(2);
        assertThat(subject.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(subject.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(subject.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
        assertThat(subject.getStringBody()).isEqualTo("{}");
        assertThat(subject.getCustomEventClassName()).isEqualTo(MoPubCustomEventNative.class.getName());
        assertThat(subject.isRewarded()).isEqualTo(REWARDED);
        final Map<String, String> serverExtras = subject.getServerExtras();
        assertNotNull(serverExtras);
        assertThat(serverExtras).isNotEmpty();
        assertThat(serverExtras.get(DataKeys.ADM_KEY)).isEqualTo(ADM_VALUE);
    }

    @Test
    public void parseNetworkResponse_forEmptyJsonArray_shouldReturnEmptyLists() throws MoPubNetworkError, JSONException {
        singleAdResponse.put(ResponseHeader.CONTENT.getKey(), new JSONObject());
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CLICK_TRACKING_URL.getKey(), new JSONArray());
        metadata.put(ResponseHeader.IMPRESSION_URLS.getKey(), new JSONArray());
        metadata.put(ResponseHeader.BEFORE_LOAD_URL.getKey(), new JSONArray());
        metadata.put(ResponseHeader.AFTER_LOAD_URL.getKey(), new JSONArray());
        metadata.put(ResponseHeader.AFTER_LOAD_SUCCESS_URL.getKey(), new JSONArray());
        metadata.put(ResponseHeader.AFTER_LOAD_FAIL_URL.getKey(), new JSONArray());
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200, singleAdResponse.toString().getBytes(),
                Collections.emptyMap());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                REWARDED,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getAdGroupId()).isEqualTo(AD_GROUP_ID);
        assertThat(subject.getClickTrackingUrls()).isEmpty();
        assertThat(subject.getImpressionTrackingUrls()).isEmpty();
        assertThat(subject.getBeforeLoadUrls()).isEmpty();
        assertThat(subject.getAfterLoadUrls()).isEmpty();
        assertThat(subject.getAfterLoadSuccessUrls()).isEmpty();
        assertThat(subject.getAfterLoadFailUrls()).isEmpty();
    }

    @Test
    public void parseNetworkResponse_forRewardedVideo_withAdvancedBidding_shouldSucceed() throws MoPubNetworkError, JSONException {
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.REWARDED_VIDEO);
        metadata.put(DataKeys.ADM_KEY, ADM_VALUE);
        final String rewardedCurrenciesJson = "{\"rewards\": ["
                + "{\"name\": \"Coins\", \"amount\": 8},"
                + "{\"name\": \"Diamonds\", \"amount\": 1},"
                + "{\"name\": \"Diamonds\", \"amount\": 10 },"
                + "{\"name\": \"Energy\", \"amount\": 20}"
                + "]}";
        metadata.put(ResponseHeader.REWARDED_CURRENCIES.getKey(), rewardedCurrenciesJson);

        metadata.put(ResponseHeader.REWARDED_VIDEO_COMPLETION_URL.getKey(),
                "https://completionUrl");

        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200, singleAdResponse.toString().getBytes(),
                Collections.emptyMap());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                true,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        assertThat(subject.getAdType()).isEqualTo(AdType.REWARDED_VIDEO);
        assertThat(subject.getAdGroupId()).isEqualTo(AD_GROUP_ID);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getClickTrackingUrls().get(0)).isEqualTo(CLICKTRACKING_URL + "1");
        assertThat(subject.getClickTrackingUrls().get(1)).isEqualTo(CLICKTRACKING_URL + "2");
        assertThat(subject.getClickTrackingUrls().size()).isEqualTo(2);
        assertThat(subject.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertThat(subject.getBeforeLoadUrls().get(0)).isEqualTo(BEFORE_LOAD_URL + "1");
        assertThat(subject.getBeforeLoadUrls().get(1)).isEqualTo(BEFORE_LOAD_URL + "2");
        assertThat(subject.getBeforeLoadUrls().size()).isEqualTo(2);
        assertThat(subject.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(subject.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(subject.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
        assertThat(subject.getHeight()).isEqualTo(HEIGHT);
        assertThat(subject.getWidth()).isEqualTo(WIDTH);
        assertThat(subject.getStringBody()).isEqualTo("content_text");
        assertThat(subject.getCustomEventClassName()).isEqualTo(MoPubFullscreen.class.getName());
        assertThat(subject.getRewardedCurrencies()).isEqualTo(rewardedCurrenciesJson);
        assertThat(subject.getRewardedAdCompletionUrl()).isEqualTo(
                "https://completionUrl");
        assertThat(subject.isRewarded()).isEqualTo(true);
        final Map<String, String> serverExtras = subject.getServerExtras();
        assertNotNull(serverExtras);
        assertThat(serverExtras).isNotEmpty();
        assertNull(serverExtras.get(DataKeys.HTML_RESPONSE_BODY_KEY));
        assertThat(serverExtras.get(DataKeys.ADM_KEY)).isEqualTo(ADM_VALUE);
        assertThat(serverExtras.get(DataKeys.ADUNIT_FORMAT)).isEqualTo(ADUNIT_FORMAT);
    }

    @Test
    public void parseNetworkResponse_withInAppBrowserAgent_shouldSucceed() throws MoPubNetworkError, JSONException {
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.BROWSER_AGENT.getKey(), IN_APP.ordinal());
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200, singleAdResponse.toString().getBytes(),
                Collections.emptyMap());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                REWARDED,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getBrowserAgent()).isEqualTo(IN_APP);
    }

    @Test
    public void parseNetworkResponse_withNativeBrowserAgent_shouldSucceed() throws MoPubNetworkError, JSONException {
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.BROWSER_AGENT.getKey(), NATIVE.ordinal());
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200, singleAdResponse.toString().getBytes(),
                Collections.emptyMap());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                REWARDED,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getBrowserAgent()).isEqualTo(NATIVE);
    }

    @Test
    public void parseNetworkResponse_withNullBrowserAgent_shouldDefaultToInApp() throws MoPubNetworkError, JSONException {
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.BROWSER_AGENT.getKey(), null);
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200, singleAdResponse.toString().getBytes(),
                Collections.emptyMap());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                REWARDED,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getBrowserAgent()).isEqualTo(IN_APP);
    }

    @Test
    public void parseNetworkResponse_withUndefinedBrowserAgent_shouldDefaultToInApp() throws MoPubNetworkError, JSONException {
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200, singleAdResponse.toString().getBytes(),
                Collections.emptyMap());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                REWARDED,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getBrowserAgent()).isEqualTo(IN_APP);
    }

    @Test
    public void parseNetworkResponse_withoutRefreshTime_shouldNotIncludeRefreshTime() throws MoPubNetworkError, JSONException {
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.remove(ResponseHeader.REFRESH_TIME.getKey());
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200, singleAdResponse.toString().getBytes(),
                Collections.emptyMap());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                REWARDED,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertNull(subject.getRefreshTimeMillis());
    }

    @Test
    public void parseNetworkResponse_withOnlyLegacyImpressionTracker_shouldPopulateImpressionTrackersList() throws MoPubNetworkError, JSONException {
        singleAdResponse.put(ResponseHeader.CONTENT.getKey(), new JSONObject());
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(DataKeys.ADM_KEY, ADM_VALUE);
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.STATIC_NATIVE);
        metadata.remove(ResponseHeader.IMPRESSION_URLS.getKey());
        metadata.put(ResponseHeader.IMPRESSION_URL.getKey(), IMPTRACKER_URL);
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200, singleAdResponse.toString().getBytes(),
                Collections.emptyMap());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                REWARDED,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        assertThat(subject.getAdType()).isEqualTo(AdType.STATIC_NATIVE);
        assertThat(subject.getAdGroupId()).isEqualTo(AD_GROUP_ID);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getClickTrackingUrls().get(0)).isEqualTo(CLICKTRACKING_URL + "1");
        assertThat(subject.getClickTrackingUrls().get(1)).isEqualTo(CLICKTRACKING_URL + "2");
        assertThat(subject.getClickTrackingUrls().size()).isEqualTo(2);
        assertThat(subject.getImpressionTrackingUrls()).isEqualTo(Collections.singletonList(IMPTRACKER_URL));
        assertThat(subject.getBeforeLoadUrls().get(0)).isEqualTo(BEFORE_LOAD_URL + "1");
        assertThat(subject.getBeforeLoadUrls().get(1)).isEqualTo(BEFORE_LOAD_URL + "2");
        assertThat(subject.getBeforeLoadUrls().size()).isEqualTo(2);
        assertThat(subject.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(subject.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(subject.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
        assertThat(subject.getStringBody()).isEqualTo("{}");
        assertThat(subject.getCustomEventClassName()).isEqualTo(MoPubCustomEventNative.class.getName());
        assertThat(subject.isRewarded()).isEqualTo(REWARDED);
        final Map<String, String> serverExtras = subject.getServerExtras();
        assertNotNull(serverExtras);
        assertThat(serverExtras).isNotEmpty();
        assertThat(serverExtras.get(DataKeys.ADM_KEY)).isEqualTo(ADM_VALUE);
    }

    @Test
    public void parseNetworkResponse_withSingleAfterLoadUrl_shouldPopulateAfterLoadUrlsList() throws MoPubNetworkError, JSONException {
        singleAdResponse.put(ResponseHeader.CONTENT.getKey(), new JSONObject());
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(DataKeys.ADM_KEY, ADM_VALUE);
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.STATIC_NATIVE);
        metadata.put(ResponseHeader.AFTER_LOAD_URL.getKey(), AFTER_LOAD_URL+"_1");
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200, singleAdResponse.toString().getBytes(),
                Collections.emptyMap());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                REWARDED,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        assertThat(subject.getAdType()).isEqualTo(AdType.STATIC_NATIVE);
        assertThat(subject.getAdGroupId()).isEqualTo(AD_GROUP_ID);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getClickTrackingUrls().get(0)).isEqualTo(CLICKTRACKING_URL + "1");
        assertThat(subject.getClickTrackingUrls().get(1)).isEqualTo(CLICKTRACKING_URL + "2");
        assertThat(subject.getClickTrackingUrls().size()).isEqualTo(2);
        assertThat(subject.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertThat(subject.getBeforeLoadUrls().get(0)).isEqualTo(BEFORE_LOAD_URL + "1");
        assertThat(subject.getBeforeLoadUrls().get(1)).isEqualTo(BEFORE_LOAD_URL + "2");
        assertThat(subject.getBeforeLoadUrls().size()).isEqualTo(2);
        assertThat(subject.getAfterLoadUrls()).isEqualTo(Collections.singletonList(AFTER_LOAD_URL+"_1"));
        assertThat(subject.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(subject.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
        assertThat(subject.getStringBody()).isEqualTo("{}");
        assertThat(subject.getCustomEventClassName()).isEqualTo(MoPubCustomEventNative.class.getName());
        assertThat(subject.isRewarded()).isEqualTo(REWARDED);
        final Map<String, String> serverExtras = subject.getServerExtras();
        assertNotNull(serverExtras);
        assertThat(serverExtras).isNotEmpty();
        assertThat(serverExtras.get(DataKeys.ADM_KEY)).isEqualTo(ADM_VALUE);
    }

    @Test
    public void parseNetworkResponse_withSingleAfterLoadSuccessUrl_shouldPopulateAfterLoadSuccessUrlsList() throws MoPubNetworkError, JSONException {
        singleAdResponse.put(ResponseHeader.CONTENT.getKey(), new JSONObject());
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(DataKeys.ADM_KEY, ADM_VALUE);
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.STATIC_NATIVE);
        metadata.put(ResponseHeader.AFTER_LOAD_SUCCESS_URL.getKey(), AFTER_LOAD_SUCCESS_URL+"_1");
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200, singleAdResponse.toString().getBytes(),
                Collections.emptyMap());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                REWARDED,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        assertThat(subject.getAdType()).isEqualTo(AdType.STATIC_NATIVE);
        assertThat(subject.getAdGroupId()).isEqualTo(AD_GROUP_ID);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getClickTrackingUrls().get(0)).isEqualTo(CLICKTRACKING_URL + "1");
        assertThat(subject.getClickTrackingUrls().get(1)).isEqualTo(CLICKTRACKING_URL + "2");
        assertThat(subject.getClickTrackingUrls().size()).isEqualTo(2);
        assertThat(subject.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertThat(subject.getBeforeLoadUrls().get(0)).isEqualTo(BEFORE_LOAD_URL + "1");
        assertThat(subject.getBeforeLoadUrls().get(1)).isEqualTo(BEFORE_LOAD_URL + "2");
        assertThat(subject.getBeforeLoadUrls().size()).isEqualTo(2);
        assertThat(subject.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(subject.getAfterLoadSuccessUrls()).isEqualTo(Collections.singletonList(AFTER_LOAD_SUCCESS_URL+"_1"));
        assertThat(subject.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
        assertThat(subject.getStringBody()).isEqualTo("{}");
        assertThat(subject.getCustomEventClassName()).isEqualTo(MoPubCustomEventNative.class.getName());
        assertThat(subject.isRewarded()).isEqualTo(REWARDED);
        final Map<String, String> serverExtras = subject.getServerExtras();
        assertNotNull(serverExtras);
        assertThat(serverExtras).isNotEmpty();
        assertThat(serverExtras.get(DataKeys.ADM_KEY)).isEqualTo(ADM_VALUE);
    }

    @Test
    public void parseNetworkResponse_withSingleAfterLoadFailUrl_shouldPopulateAfterLoadFailUrlsList() throws MoPubNetworkError, JSONException {
        singleAdResponse.put(ResponseHeader.CONTENT.getKey(), new JSONObject());
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(DataKeys.ADM_KEY, ADM_VALUE);
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.STATIC_NATIVE);
        metadata.put(ResponseHeader.AFTER_LOAD_FAIL_URL.getKey(), AFTER_LOAD_FAIL_URL+"_1");
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200, singleAdResponse.toString().getBytes(),
                Collections.emptyMap());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                REWARDED,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        assertThat(subject.getAdType()).isEqualTo(AdType.STATIC_NATIVE);
        assertThat(subject.getAdGroupId()).isEqualTo(AD_GROUP_ID);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getClickTrackingUrls().get(0)).isEqualTo(CLICKTRACKING_URL + "1");
        assertThat(subject.getClickTrackingUrls().get(1)).isEqualTo(CLICKTRACKING_URL + "2");
        assertThat(subject.getClickTrackingUrls().size()).isEqualTo(2);
        assertThat(subject.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertThat(subject.getBeforeLoadUrls().get(0)).isEqualTo(BEFORE_LOAD_URL + "1");
        assertThat(subject.getBeforeLoadUrls().get(1)).isEqualTo(BEFORE_LOAD_URL + "2");
        assertThat(subject.getBeforeLoadUrls().size()).isEqualTo(2);
        assertThat(subject.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(subject.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(subject.getAfterLoadFailUrls()).isEqualTo(Collections.singletonList(AFTER_LOAD_FAIL_URL+"_1"));
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
        assertThat(subject.getStringBody()).isEqualTo("{}");
        assertThat(subject.getCustomEventClassName()).isEqualTo(MoPubCustomEventNative.class.getName());
        assertThat(subject.isRewarded()).isEqualTo(REWARDED);
        final Map<String, String> serverExtras = subject.getServerExtras();
        assertNotNull(serverExtras);
        assertThat(serverExtras).isNotEmpty();
        assertThat(serverExtras.get(DataKeys.ADM_KEY)).isEqualTo(ADM_VALUE);
    }

    @Test
    public void parseNetworkResponse_withDisableVieabilitySet_shouldDisableViewability() throws MoPubNetworkError, JSONException {
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.DISABLE_VIEWABILITY.getKey(), "1");
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200, singleAdResponse.toString().getBytes(),
                Collections.emptyMap());

        ViewabilityManager.setViewabilityEnabled(true);
        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                REWARDED,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        assertFalse(ViewabilityManager.isViewabilityEnabled());
    }

    @Test
    public void parseNetworkResponse_withDisableVieabilitySetToZero_shouldNotDisableViewability() throws MoPubNetworkError, JSONException {
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.DISABLE_VIEWABILITY.getKey(), "0");
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200, singleAdResponse.toString().getBytes(),
                Collections.emptyMap());

        ViewabilityManager.setViewabilityEnabled(true);
        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                REWARDED,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        assertTrue(ViewabilityManager.isViewabilityEnabled());
    }

    @Test
    public void parseNetworkResponse_withDisableVieabilityNotSet_shouldNotChangeViewability() throws MoPubNetworkError, JSONException {
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200, singleAdResponse.toString().getBytes(),
                Collections.emptyMap());
        ViewabilityManager.setViewabilityEnabled(true);

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                REWARDED,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        assertTrue(ViewabilityManager.isViewabilityEnabled());
    }

    @Test
    public void parseNetworkResponse_withVerificationResources_shouldParseVerificationResources() throws MoPubNetworkError, JSONException {
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.VIEWABILITY_VERIFICATION.getKey(), createVieabilityVerificationJson());
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200, singleAdResponse.toString().getBytes(),
                Collections.emptyMap());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                REWARDED,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        final Set<ViewabilityVendor> set =  subject.getViewabilityVendors();
        assertNotNull(set);
        assertEquals(1, set.size());
        final ViewabilityVendor viewabilityVendor = set.iterator().next();
        assertEquals("company.com-omid", viewabilityVendor.getVendorKey());
        assertEquals("[parameters string]", viewabilityVendor.getVerificationParameters());
        assertEquals("https://abc.com/verification.js", viewabilityVendor.getJavascriptResourceUrl().toString());
    }

    @Test
    public void parseNetworkResponse_withRewardedTrue_shouldSucceed() throws MoPubNetworkError, JSONException {
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200,
                singleAdResponse.toString().getBytes(), Collections.emptyMap());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                true,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        assertTrue(subject.isRewarded());
    }

    @Test
    public void parseNetworkResponse_withRewardedFalse_shouldSucceed() throws MoPubNetworkError, JSONException {
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200,
                singleAdResponse.toString().getBytes(), Collections.emptyMap());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                false,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        assertFalse(subject.isRewarded());
    }

    @Test
    public void parseNetworkResponse_withCreativeExperienceSettings_shouldSucceed() throws MoPubNetworkError, JSONException {
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200,
                singleAdResponse.toString().getBytes(), Collections.emptyMap());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                REWARDED,
                CREATIVE_EXPERIENCE_SETTINGS_OBJECT);

        CreativeExperienceSettings expectedSettings = CreativeExperienceSettingsParser
                .parse(CREATIVE_EXPERIENCE_SETTINGS_OBJECT, REWARDED);
        assertEquals(expectedSettings, subject.getCreativeExperienceSettings());
    }

    @Test
    public void parseNetworkResponse_withNullCreativeExperienceSettings_shouldSucceed() throws MoPubNetworkError, JSONException {
        MoPubNetworkResponse networkResponse = new MoPubNetworkResponse(200,
                singleAdResponse.toString().getBytes(), Collections.emptyMap());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                ADUNIT_FORMAT,
                REQUEST_ID_VALUE,
                REWARDED,
                null);

        assertThat(subject.getCreativeExperienceSettings())
                .isEqualTo(CreativeExperienceSettings.getDefaultSettings(REWARDED));
    }

    // Utility functions
    private static byte[] createResponseBody(String failURL, JSONObject adResponse) throws JSONException {
        return createJsonBody(failURL, adResponse).toString().getBytes();
    }

    /**
     * @param failURL    test value for failURL
     * @param adResponse ad response JSON for single creative
     * @return JSONObject in the same format like it comes from the server
     * @throws JSONException unlikely to happen
     */
    private static JSONObject createJsonBody(String failURL, JSONObject adResponse) throws JSONException {
        // array of JSON objects AdResponse
        JSONArray adResponses = new JSONArray();
        if (adResponse != null)
            adResponses.put(adResponse);

        // whole response body
        JSONObject jsonBody = new JSONObject();
        jsonBody.put(ResponseHeader.FAIL_URL.getKey(), failURL);
        jsonBody.put(ResponseHeader.AD_RESPONSES.getKey(), adResponses);
        jsonBody.put(ResponseHeader.REWARDED.getKey(), "0");
        return jsonBody;
    }

    //    {
    //        "metadata": {
    //          "x-adtype": "clear",
    //          "x-backfill": "clear",
    //          "x-refreshtime": 30
    //        }
    //    }
    private static JSONObject createClearAdResponse() throws JSONException {
        JSONObject metadata = new JSONObject();
        metadata.put(ResponseHeader.AD_TYPE.getKey(), "clear");
        metadata.put(ResponseHeader.BACKFILL.getKey(), "clear");
        metadata.put(ResponseHeader.REFRESH_TIME.getKey(), REFRESH_TIME);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(ResponseHeader.METADATA.getKey(), metadata);
        return jsonObject;
    }

    //    {
    //        "metadata": {
    //          "x-adtype": "clear",
    //          "x-backfill": "clear",
    //          "x-refreshtime": 30
    //          "x-warmup": "1",
    //        }
    //    }
    private static JSONObject createWarmupAdResponse() throws JSONException {
        JSONObject metadata = new JSONObject();
        metadata.put(ResponseHeader.AD_TYPE.getKey(), "clear");
        metadata.put(ResponseHeader.BACKFILL.getKey(), "clear");
        metadata.put(ResponseHeader.REFRESH_TIME.getKey(), REFRESH_TIME);
        metadata.put(ResponseHeader.WARMUP.getKey(), 1);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(ResponseHeader.METADATA.getKey(), metadata);
        return jsonObject;
    }

    private static JSONObject createImpressionData() throws JSONException {
        String jsonString = "{\n" +
                        "          \"id\": \"impid\",\n" +
                        "          \"app_version\": \"mAppVersion\",\n" +
                        "          \"adunit_id\": \"adunitid\",\n" +
                        "          \"adunit_name\": \"adunitname\",\n" +
                        "          \"adunit_format\": \"adunitformat\",\n" +
                        "          \"adgroup_id\": \"adgroupid\",\n" +
                        "          \"adgroup_name\": \"adgroupname\",\n" +
                        "          \"adgroup_type\": \"adgrouptype\",\n" +
                        "          \"adgroup_priority\": 123,\n" +
                        "          \"currency\": \"USD\",\n" +
                        "          \"country\": \"USA\",\n" +
                        "          \"network_name\": \"networkname\",\n" +
                        "          \"network_placement_id\": \"networkplacementid\",\n" +
                        "          \"publisher_revenue\": 0.0001,\n" +
                        "          \"precision\": \"exact\",\n" +
                        "          \"demand_partner_data\": {\n" +
                        "              \"encrypted_cpm\": \"test_cpm\"\n" +
                        "          }\n" +
                        "     }";
        return new JSONObject(jsonString);
    }

    private static JSONArray createVieabilityVerificationJson() throws JSONException {
        final String jsonString = "[\n" +
                "        {\n" +
                "            \"vendorKey\": \"company.com-omid\",\n" +
                "                \"apiFramework\": \"omid\",\n" +
                "                \"javascriptResourceUrl\": \"https://abc.com/verification.js\",\n" +
                "                \"verificationParameters\": \"[parameters string]\"\n" +
                "        }\n" +
                "  ]";

        return new JSONArray(jsonString);
    }

    private static void addBackoffParameters(final JSONObject response, int time, String reason) throws JSONException {
        response.put(ResponseHeader.BACKOFF_MS.getKey(), time);
        response.put(ResponseHeader.BACKOFF_REASON.getKey(), reason);
    }
}
