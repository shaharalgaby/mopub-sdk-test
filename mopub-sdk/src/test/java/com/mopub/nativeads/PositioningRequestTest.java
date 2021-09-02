// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.app.Activity;

import com.mopub.common.Constants;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.MoPubNetworkResponse;
import com.mopub.network.MoPubRequestUtils;
import com.mopub.network.MoPubResponse;
import com.mopub.network.MoPubUrlRewriter;
import com.mopub.network.Networking;
import com.mopub.network.PlayServicesUrlRewriter;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.mopub.network.MoPubRequest.DEFAULT_CONTENT_TYPE;
import static com.mopub.network.MoPubRequest.JSON_CONTENT_TYPE;
import static junit.framework.Assert.fail;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(SdkTestRunner.class)
public class PositioningRequestTest {
    private static final String URL = Constants.HTTPS + "://" + Constants.HOST;

    @Mock
    private MoPubResponse.Listener<MoPubNativeAdPositioning.MoPubClientPositioning> mockListener;
    private MoPubNetworkResponse mockNetworkResponse;
    private PositioningRequest subject;

    @Before
    public void setup() {
        subject = new PositioningRequest(Robolectric.buildActivity(Activity.class).get(), URL, mockListener);
    }

    @Test
    public void getParams_whenUrlRewriterIsNull_shouldReturnNull() {
        /*
        if (... || rewriter == null) {
            return null;
        }
         */
        Networking.setUrlRewriter(null);

        assertNull(subject.getParams());
    }

    @Test
    public void getParams_withNonMoPubRequest_shouldReturnNull() {
        /*
        if (!MoPubRequestUtils.isMoPubRequest(getUrl()) || ...) {
            return null;
        }
         */
        String nonMoPubUrl = "https://www.abcdefg.com/xyz";
        subject = new PositioningRequest(Robolectric.buildActivity(Activity.class).get(), nonMoPubUrl, mockListener);
        Networking.setUrlRewriter(new PlayServicesUrlRewriter());

        assertNull(subject.getParams());
    }

    @Test
    public void getParams_withMoPubRequest_whenUrlRewriterIsNotNull_shouldReturnParamMap() {
        String url = Constants.HTTPS + "://" + Constants.HOST + "/m/ad?query1=abc&query2=def&query3=ghi";
        subject = new PositioningRequest(Robolectric.buildActivity(Activity.class).get(), url, mockListener);
        Networking.setUrlRewriter(new PlayServicesUrlRewriter());

        Map<String, String> paramMap = subject.getParams();

        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("query1", "abc");
        expectedMap.put("query2", "def");
        expectedMap.put("query3", "ghi");
        assertNotNull(paramMap);
        assertEquals(expectedMap, paramMap);
    }

    @Test
    public void getBodyContentType_withMoPubRequest_shouldReturnJsonContentType() {
        assertEquals(JSON_CONTENT_TYPE, subject.getBodyContentType());
    }

    @Test
    public void getBodyContentType_withNonMoPubRequest_shouldReturnDefaultContentType() {
        String nonMoPubUrl = "https://www.abcdefg.com/xyz";
        subject = new PositioningRequest(Robolectric.buildActivity(Activity.class).get(), nonMoPubUrl, mockListener);

        assertEquals(DEFAULT_CONTENT_TYPE, subject.getBodyContentType());
    }

    @Test
    public void parseNetworkResponse_shouldReturnPositioning() {
        mockNetworkResponse = new MoPubNetworkResponse(200, "{fixed: []}".getBytes(), Collections.emptyMap());

        assertThat(subject.parseNetworkResponse(mockNetworkResponse).getMoPubResult())
                .isExactlyInstanceOf(MoPubNativeAdPositioning.MoPubClientPositioning.class);
    }
    
    @Test
    public void parseNetworkResponse_shouldReturnError() {
        mockNetworkResponse = new MoPubNetworkResponse(200, "garbage".getBytes(), Collections.emptyMap());

        assertThat(subject.parseNetworkResponse(mockNetworkResponse).getMoPubNetworkError()).isNotNull();
    }
    
    @Test
    public void parseJson_noFixedPositions_shouldReturnEmptyPositioning() throws Exception {
        MoPubNativeAdPositioning.MoPubClientPositioning positioning = subject.parseJson(
                "{fixed: []}");
        assertThat(positioning.getFixedPositions()).isEmpty();
        assertThat(positioning.getRepeatingInterval()).isEqualTo(MoPubNativeAdPositioning.MoPubClientPositioning.NO_REPEAT);
    }

    @Test
    public void parseJson_oneFixedPosition_shouldReturnValidPositioning() throws Exception {
        MoPubNativeAdPositioning.MoPubClientPositioning positioning = subject.parseJson(
                "{fixed: [{position: 2}]}");
        assertThat(positioning.getFixedPositions()).containsOnly(2);
        assertThat(positioning.getRepeatingInterval()).isEqualTo(MoPubNativeAdPositioning.MoPubClientPositioning.NO_REPEAT);
    }

    @Test
    public void parseJson_twoFixedPositions_shouldReturnValidPositioning() throws Exception {
        MoPubNativeAdPositioning.MoPubClientPositioning positioning = subject.parseJson(
                "{fixed: [{position: 1}, {position: 8}]}");
        assertThat(positioning.getFixedPositions()).containsExactly(1, 8);
        assertThat(positioning.getRepeatingInterval()).isEqualTo(MoPubNativeAdPositioning.MoPubClientPositioning.NO_REPEAT);
    }

    @Test
    public void parseJson_twoFixedPositions_shouldIgnoreNonZeroSection() throws Exception {
        MoPubNativeAdPositioning.MoPubClientPositioning positioning = subject.parseJson(
                "{fixed: [{section: 0, position: 5}, {section: 1, position: 8}]}");
        assertThat(positioning.getFixedPositions()).containsOnly(5);
        assertThat(positioning.getRepeatingInterval()).isEqualTo(MoPubNativeAdPositioning.MoPubClientPositioning.NO_REPEAT);
    }

    @Test
    public void parseJson_invalidFixedPosition_shouldThrowException() {
        // Must have either fixed or repeating positions.
        checkException("", "Empty response");
        checkException("{}", "Must contain fixed or repeating positions");
        checkException("{\"error\":\"WARMING_UP\"}", "WARMING_UP");

        // Position is required.
        checkException("{fixed: [{}]}", "JSONObject[\"position\"] not found.");
        checkException("{fixed: [{section: 0}]}", "JSONObject[\"position\"] not found.");

        // Section is optional, but if it exists must be > 0
        checkException("{fixed: [{section: -1, position: 8}]}", "Invalid section -1 in JSON response");

        // Positions must be between [0 and 2 ^ 16).
        checkException("{fixed: [{position: -1}]}", "Invalid position -1 in JSON response");
        checkException("{fixed: [{position: 1}, {position: -8}]}",
                "Invalid position -8 in JSON response");
        checkException("{fixed: [{position: 1}, {position: 66000}]}",
                "Invalid position 66000 in JSON response");
    }

    @Test
    public void parseJson_repeatingInterval_shouldReturnValidPositioning() throws Exception {
        MoPubNativeAdPositioning.MoPubClientPositioning positioning = subject.parseJson(
                "{repeating: {interval: 2}}");
        assertThat(positioning.getFixedPositions()).isEmpty();
        assertThat(positioning.getRepeatingInterval()).isEqualTo(2);
    }

    @Test
    public void parseJson_invalidRepeating_shouldThrowException() {
        checkException("{repeating: }", "Missing value at character 12");
        checkException("{repeating: {}}", "JSONObject[\"interval\"] not found.");

        // Intervals must be between [2 and 2 ^ 16).
        checkException("{repeating: {interval: -1}}", "Invalid interval -1 in JSON response");
        checkException("{repeating: {interval: 0}}", "Invalid interval 0 in JSON response");
        checkException("{repeating: {interval: 1}}", "Invalid interval 1 in JSON response");
        checkException("{repeating: {interval: 66000}}",
                "Invalid interval 66000 in JSON response");
    }

    @Test
    public void parseJson_fixedAndRepeating_shouldReturnValidPositioning() throws Exception {
        MoPubNativeAdPositioning.MoPubClientPositioning positioning = subject.parseJson(
                "{fixed: [{position: 0}, {position: 1}], repeating: {interval: 2}}");
        assertThat(positioning.getFixedPositions()).containsExactly(0, 1);
        assertThat(positioning.getRepeatingInterval()).isEqualTo(2);
    }

    private void checkException(String json, String expectedMessage) {
        try {
            subject.parseJson(json);
        } catch (JSONException | MoPubNetworkError e) {
            return;
        }
        fail("Should have received an exception");
    }
}
