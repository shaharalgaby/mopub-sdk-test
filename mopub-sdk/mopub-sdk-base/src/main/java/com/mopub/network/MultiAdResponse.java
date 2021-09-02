// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.AdFormat;
import com.mopub.common.AdType;
import com.mopub.common.BrowserAgentManager;
import com.mopub.common.BrowserAgentManager.BrowserAgent;
import com.mopub.common.Constants;
import com.mopub.common.DataKeys;
import com.mopub.common.FullAdType;
import com.mopub.common.MoPub;
import com.mopub.common.Preconditions;
import com.mopub.common.ViewabilityVendor;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Json;
import com.mopub.common.util.ResponseHeader;
import com.mopub.mobileads.AdTypeTranslator;
import com.mopub.mobileads.CreativeExperienceSettings;
import com.mopub.mobileads.CreativeExperienceSettingsParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.mopub.common.DataKeys.ADM_KEY;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.RESPONSE_RECEIVED;
import static com.mopub.network.HeaderUtils.extractBooleanHeader;
import static com.mopub.network.HeaderUtils.extractHeader;
import static com.mopub.network.HeaderUtils.extractIntegerHeader;
import static com.mopub.network.HeaderUtils.extractJsonArrayHeader;
import static com.mopub.network.HeaderUtils.extractJsonObjectHeader;
import static com.mopub.network.HeaderUtils.extractPercentHeaderString;
import static com.mopub.network.HeaderUtils.extractStringArray;

/**
 * Immutable data class to parse client side waterfall network response.
 */
public class MultiAdResponse implements Iterator<AdResponse> {

    public interface ServerOverrideListener {
        void onForceExplicitNo(@Nullable final String consentChangeReason);
        void onInvalidateConsent(@Nullable final String consentChangeReason);
        void onReacquireConsent(@Nullable final String consentChangeReason);
        void onForceGdprApplies();
        void onRequestSuccess(@Nullable final String adUnitId);
    }

    private static final String EMPTY_JSON_ARRAY = "[]";

    @NonNull
    private final Iterator<AdResponse> mResponseIterator;

    @NonNull
    private String mFailUrl;

    @NonNull
    public String getFailURL() {
        return mFailUrl;
    }

    @Nullable
    private static ServerOverrideListener sServerOverrideListener;

    /**
     *
     * @param appContext application context
     * @param networkResponse {@link MoPubNetworkResponse} to be parsed
     * @param adFormat {@link AdFormat}
     * @param adUnitId ad unit id originally sent to server
     * @throws JSONException when JSON parsing fails
     * @throws MoPubNetworkError when ad unit is warming up or no ads are found for ad unit
     */
    public MultiAdResponse(@NonNull final Context appContext,
                           @NonNull final MoPubNetworkResponse networkResponse,
                           @NonNull final AdFormat adFormat,
                           @Nullable final String adUnitId) throws JSONException, MoPubNetworkError {

        // Response Body encoding / decoding
        final String responseBody = parseStringBody(networkResponse);

        JSONObject jsonObject = new JSONObject(responseBody);
        mFailUrl = jsonObject.optString(ResponseHeader.FAIL_URL.getKey());
        final String adUnitFormat = jsonObject.optString(ResponseHeader.ADUNIT_FORMAT.getKey());
        String requestId = jsonObject.optString(ResponseHeader.REQUEST_ID.getKey());

        final Integer backoffMs = extractIntegerHeader(jsonObject, ResponseHeader.BACKOFF_MS);
        final String backoffReason = extractHeader(jsonObject, ResponseHeader.BACKOFF_REASON);
        RequestRateTracker.getInstance().registerRateLimit(adUnitId, backoffMs, backoffReason);

        final boolean invalidateConsent = extractBooleanHeader(jsonObject,
                ResponseHeader.INVALIDATE_CONSENT, false);
        final boolean forceExplicitNo = extractBooleanHeader(jsonObject,
                ResponseHeader.FORCE_EXPLICIT_NO, false);
        final boolean reacquireConsent = extractBooleanHeader(jsonObject,
                ResponseHeader.REACQUIRE_CONSENT, false);
        final String consentChangeReason = extractHeader(jsonObject,
                ResponseHeader.CONSENT_CHANGE_REASON);
        final boolean forceGdprApplies = extractBooleanHeader(jsonObject,
                ResponseHeader.FORCE_GDPR_APPLIES, false);
        if (sServerOverrideListener != null) {
            if (forceGdprApplies) {
                sServerOverrideListener.onForceGdprApplies();
            }
            if (forceExplicitNo) {
                sServerOverrideListener.onForceExplicitNo(consentChangeReason);
            } else if (invalidateConsent) {
                sServerOverrideListener.onInvalidateConsent(consentChangeReason);
            } else if (reacquireConsent) {
                sServerOverrideListener.onReacquireConsent(consentChangeReason);
            }
            sServerOverrideListener.onRequestSuccess(adUnitId);
        }

        final boolean enableDebugLogging = extractBooleanHeader(jsonObject,
                ResponseHeader.ENABLE_DEBUG_LOGGING, false);

        if (enableDebugLogging) {
            MoPubLog.setLogLevel(MoPubLog.LogLevel.DEBUG);
        }

        boolean isRewarded = extractBooleanHeader(jsonObject, ResponseHeader.REWARDED, false);

        JSONObject creativeExperienceSettings = extractJsonObjectHeader(jsonObject,
                ResponseHeader.CREATIVE_EXPERIENCE_SETTINGS);
        
        JSONArray adResponses = jsonObject.getJSONArray(ResponseHeader.AD_RESPONSES.getKey());
        int ADS_PER_RESPONSE = 3;
        List<AdResponse> list = new ArrayList<>(ADS_PER_RESPONSE);
        AdResponse adResponseClear = null;
        for (int i = 0; i < adResponses.length(); i++) {
            try {
                JSONObject item = adResponses.getJSONObject(i);
                AdResponse singleAdResponse = parseSingleAdResponse(appContext,
                        networkResponse,
                        item,
                        adUnitId,
                        adFormat,
                        adUnitFormat,
                        requestId,
                        isRewarded,
                        creativeExperienceSettings);
                if (!AdType.CLEAR.equals(singleAdResponse.getAdType())) {
                    list.add(singleAdResponse);
                    continue;
                }

                // received message 'clear'
                mFailUrl = "";
                adResponseClear = singleAdResponse;
                if (extractWarmup(item)) {
                    throw new MoPubNetworkError.Builder("Server is preparing this Ad Unit.")
                            .reason(MoPubNetworkError.Reason.WARMING_UP)
                            .refreshTimeMillis(adResponseClear.getRefreshTimeMillis())
                            .build();
                }
                break; // we don't process items beyond 'clear'

            } catch (JSONException ex) {
                // don't break everything because of single item parsing error
                MoPubLog.log(CUSTOM, "Invalid response item. Body: " + responseBody);
            } catch (MoPubNetworkError ex) {
                if (ex.getReason() == MoPubNetworkError.Reason.WARMING_UP) {
                    throw ex;
                }
                MoPubLog.log(CUSTOM, "Invalid response item. Error: " + ex.getReason());
            } catch (Exception ex) {
                MoPubLog.log(CUSTOM, "Unexpected error parsing response item. " + ex.getMessage());
            }
        }
        mResponseIterator = list.iterator();

        // validate if there is any valid ad response
        if (!mResponseIterator.hasNext()) {
            Integer refreshTimeMilliseconds = Constants.THIRTY_SECONDS_MILLIS;
            if (adResponseClear != null) {
                refreshTimeMilliseconds = adResponseClear.getRefreshTimeMillis();
            }
            throw new MoPubNetworkError.Builder("No ads found for ad unit.")
                    .reason(MoPubNetworkError.Reason.NO_FILL)
                    .refreshTimeMillis(refreshTimeMilliseconds)
                    .build();
        }
    }

    @Override
    public boolean hasNext() {
        return mResponseIterator.hasNext();
    }

    @NonNull
    @Override
    public AdResponse next() {
        return mResponseIterator.next();
    }

    boolean isWaterfallFinished() {
        return TextUtils.isEmpty(mFailUrl);
    }

    public static void setServerOverrideListener(
            @NonNull final ServerOverrideListener serverOverrideListener) {
        sServerOverrideListener = serverOverrideListener;
    }

    /**
     * Parse single object {@link AdResponse} from JSON
     *
     * @param appContext      application context
     * @param networkResponse original {@link MoPubNetworkResponse}
     * @param jsonObject      JSON object to parse
     * @param adUnitId        request ad unit id
     * @param adFormat        {@link AdFormat}
     * @param requestId       GUID assigned by server
     * @return valid {@link AdResponse} or throws exception
     * @throws JSONException     when JSON format is broken or critical field is missing
     * @throws MoPubNetworkError when high level validation failed
     */
    @NonNull
    protected static AdResponse parseSingleAdResponse(@NonNull final Context appContext,
                                                      @NonNull final MoPubNetworkResponse networkResponse,
                                                      @NonNull final JSONObject jsonObject,
                                                      @Nullable final String adUnitId,
                                                      @NonNull final AdFormat adFormat,
                                                      @NonNull final String adUnitFormat,
                                                      @Nullable final String requestId,
                                                      @NonNull final Boolean isRewarded,
                                                      @Nullable final JSONObject ceSettingsJSONObject)
            throws JSONException, MoPubNetworkError {
        Preconditions.checkNotNull(appContext);
        Preconditions.checkNotNull(networkResponse);
        Preconditions.checkNotNull(jsonObject);
        Preconditions.checkNotNull(adFormat);
        Preconditions.checkNotNull(adUnitFormat);
        Preconditions.checkNotNull(isRewarded);

        MoPubLog.log(RESPONSE_RECEIVED, jsonObject.toString());

        final AdResponse.Builder builder = new AdResponse.Builder();
        final String content = jsonObject.optString(ResponseHeader.CONTENT.getKey());
        final JSONObject jsonHeaders = jsonObject.getJSONObject(ResponseHeader.METADATA.getKey());

        builder.setAdUnitId(adUnitId);

        // Response Body encoding / decoding
        builder.setResponseBody(content);

        String adTypeString = extractHeader(jsonHeaders, ResponseHeader.AD_TYPE);
        String adGroupIdString = extractHeader(jsonHeaders, ResponseHeader.AD_GROUP_ID);
        String fullAdTypeString = extractHeader(jsonHeaders, ResponseHeader.FULL_AD_TYPE);
        builder.setAdType(adTypeString);
        builder.setAdGroupId(adGroupIdString);
        builder.setFullAdType(fullAdTypeString);

        // In the case of a CLEAR response, the REFRESH_TIME header must still be respected. Ensure
        // that it is parsed and passed along to the MoPubNetworkError.
        final Integer refreshTimeMilliseconds = extractRefreshTimeMS(jsonObject);
        builder.setRefreshTimeMilliseconds(refreshTimeMilliseconds);

        if (AdType.CLEAR.equals(adTypeString)) {
            return builder.build();
        }

        String dspCreativeId = extractHeader(jsonHeaders, ResponseHeader.DSP_CREATIVE_ID);
        builder.setDspCreativeId(dspCreativeId);

        String networkType = extractHeader(jsonHeaders, ResponseHeader.NETWORK_TYPE);
        builder.setNetworkType(networkType);

        JSONObject impressionJson = extractJsonObjectHeader(jsonHeaders, ResponseHeader.IMPRESSION_DATA);
        builder.setImpressionData(ImpressionData.create(impressionJson));

        // clicktrackers is parsed into the AdResponse as the click tracker
        // Used by AdViewController, Rewarded Video, Native Adapter, MoPubNative
        // As of 5.14, we allow for an array of click trackers.
        final List<String> clickTrackingUrls = extractStringArray(jsonHeaders,
                ResponseHeader.CLICK_TRACKING_URL);
        if (clickTrackingUrls.isEmpty()) {
            final String clickTrackingUrl = extractHeader(jsonHeaders, ResponseHeader.CLICK_TRACKING_URL);
            if (!TextUtils.isEmpty(clickTrackingUrl) && !EMPTY_JSON_ARRAY.equals(clickTrackingUrl)) {
                clickTrackingUrls.add(clickTrackingUrl);
            }
        }
        builder.setClickTrackingUrls(clickTrackingUrls);

        // As of 5.3, we moved to an array of impression urls.
        final List<String> impressionUrls = extractStringArray(jsonHeaders,
                ResponseHeader.IMPRESSION_URLS);
        if (impressionUrls.isEmpty()) {
            // During the transition period where adserver still sends back just one impression
            // url, handle this as if we get a list of one impression url.
            final String impressionUrl = extractHeader(jsonHeaders, ResponseHeader.IMPRESSION_URL);
            if (!TextUtils.isEmpty(impressionUrl) && !EMPTY_JSON_ARRAY.equals(impressionUrl)) {
                impressionUrls.add(impressionUrl);
            }
        }
        builder.setImpressionTrackingUrls(impressionUrls);

        // As of 5.14, we allow for an array of before load urls.
        final List<String> beforeLoadUrls = extractStringArray(jsonHeaders,
                ResponseHeader.BEFORE_LOAD_URL);
        if (beforeLoadUrls.isEmpty()) {
            final String beforeLoadUrl = extractHeader(jsonHeaders, ResponseHeader.BEFORE_LOAD_URL);
            if (!TextUtils.isEmpty(beforeLoadUrl) && !EMPTY_JSON_ARRAY.equals(beforeLoadUrl)) {
                beforeLoadUrls.add(beforeLoadUrl);
            }
        }
        builder.setBeforeLoadUrls(beforeLoadUrls);

        final List<String> afterLoadUrls = extractStringArray(jsonHeaders,
                ResponseHeader.AFTER_LOAD_URL);
        if (afterLoadUrls.isEmpty()) {
            final String afterLoadUrl = extractHeader(jsonHeaders, ResponseHeader.AFTER_LOAD_URL);
            if (!TextUtils.isEmpty(afterLoadUrl) && !EMPTY_JSON_ARRAY.equals(afterLoadUrl)) {
                afterLoadUrls.add(afterLoadUrl);
            }
        }
        builder.setAfterLoadUrls(afterLoadUrls);

        final List<String> afterLoadSuccessUrls = extractStringArray(jsonHeaders,
                ResponseHeader.AFTER_LOAD_SUCCESS_URL);
        if (afterLoadSuccessUrls.isEmpty()) {
            final String afterLoadSuccessUrl = extractHeader(jsonHeaders, ResponseHeader.AFTER_LOAD_SUCCESS_URL);
            if (!TextUtils.isEmpty(afterLoadSuccessUrl) && !EMPTY_JSON_ARRAY.equals(afterLoadSuccessUrl)) {
                afterLoadSuccessUrls.add(afterLoadSuccessUrl);
            }
        }
        builder.setAfterLoadSuccessUrls(afterLoadSuccessUrls);

        final List<String> afterLoadFailUrls = extractStringArray(jsonHeaders,
                ResponseHeader.AFTER_LOAD_FAIL_URL);
        if (afterLoadFailUrls.isEmpty()) {
            final String afterLoadFailUrl = extractHeader(jsonHeaders, ResponseHeader.AFTER_LOAD_FAIL_URL);
            if (!TextUtils.isEmpty(afterLoadFailUrl) && !EMPTY_JSON_ARRAY.equals(afterLoadFailUrl)) {
                afterLoadFailUrls.add(afterLoadFailUrl);
            }
        }
        builder.setAfterLoadFailUrls(afterLoadFailUrls);

        builder.setRequestId(requestId);

        Integer width = extractIntegerHeader(jsonHeaders, ResponseHeader.WIDTH);
        Integer height = extractIntegerHeader(jsonHeaders, ResponseHeader.HEIGHT);
        builder.setDimensions(width, height);

        Integer adTimeoutDelayMilliseconds = extractIntegerHeader(jsonHeaders, ResponseHeader.AD_TIMEOUT);
        builder.setAdTimeoutDelayMilliseconds(adTimeoutDelayMilliseconds);

        if (AdType.STATIC_NATIVE.equals(adTypeString)) {
            try {
                builder.setJsonBody(new JSONObject(content));
            } catch (JSONException e) {
                throw new MoPubNetworkError.Builder("Failed to decode body JSON for native ad format", e)
                        .reason(MoPubNetworkError.Reason.BAD_BODY)
                        .build();
            }
        }

        // Derive base ad fields
        String customEventClassName = AdTypeTranslator.getBaseAdClassName(adFormat, adTypeString,
                fullAdTypeString, jsonHeaders);
        builder.setBaseAdClassName(customEventClassName);

        // Default browser agent from X-Browser-Agent header
        final BrowserAgent browserAgent = BrowserAgent.fromHeader(
                extractIntegerHeader(jsonHeaders, ResponseHeader.BROWSER_AGENT));
        BrowserAgentManager.setBrowserAgentFromAdServer(browserAgent);
        builder.setBrowserAgent(browserAgent);

        // Process server extras if they are present:
        String customEventData = extractHeader(jsonHeaders, ResponseHeader.CUSTOM_EVENT_DATA);

        // Some server-supported base ads (like AdMob banners) use a different header field
        if (TextUtils.isEmpty(customEventData)) {
            customEventData = extractHeader(jsonHeaders, ResponseHeader.NATIVE_PARAMS);
        }

        final Map<String, String> serverExtras;
        try {
            serverExtras = Json.jsonStringToMap(customEventData);
        } catch (JSONException e) {
            throw new MoPubNetworkError.Builder("Failed to decode server extras for base ad data.", e)
                    .reason(MoPubNetworkError.Reason.BAD_HEADER_DATA)
                    .build();
        }

        try {
            if (!jsonHeaders.optString(ADM_KEY).isEmpty()) {
                serverExtras.put(ADM_KEY, jsonHeaders.getString(ADM_KEY));
            }
        } catch (JSONException e) {
            throw new MoPubNetworkError.Builder("Failed to parse ADM for advanced bidding", e)
                    .reason(MoPubNetworkError.Reason.BAD_BODY)
                    .build();
        }

        // Flag for immediate VAST clickability
        // Enable the experiment on a value of 1
        // Disable the experiment for everything else
        final int shouldEnableVastClickInt = extractIntegerHeader(jsonHeaders,
                ResponseHeader.VAST_CLICK_ENABLED,
                0);
        serverExtras.put(DataKeys.VAST_CLICK_EXP_ENABLED_KEY,
                Boolean.toString(shouldEnableVastClickInt == 1));

        serverExtras.put(DataKeys.ADUNIT_FORMAT, adUnitFormat);

        if (eventDataIsInResponseBody(adTypeString, fullAdTypeString)) {
            // Some MoPub-specific base ads get their serverExtras from the response itself:
            serverExtras.put(DataKeys.HTML_RESPONSE_BODY_KEY, content);
            serverExtras.put(DataKeys.CREATIVE_ORIENTATION_KEY, extractHeader(jsonHeaders, ResponseHeader.ORIENTATION));
        }

        if (AdType.STATIC_NATIVE.equals(adTypeString)) {
            final String impressionMinVisiblePercent = extractPercentHeaderString(jsonHeaders,
                    ResponseHeader.IMPRESSION_MIN_VISIBLE_PERCENT);
            final String impressionVisibleMS = extractHeader(jsonHeaders,
                    ResponseHeader.IMPRESSION_VISIBLE_MS);
            final String impressionMinVisiblePx = extractHeader(jsonHeaders,
                    ResponseHeader.IMPRESSION_MIN_VISIBLE_PX);
            if (!TextUtils.isEmpty(impressionMinVisiblePercent)) {
                serverExtras.put(DataKeys.IMPRESSION_MIN_VISIBLE_PERCENT,
                        impressionMinVisiblePercent);
            }
            if (!TextUtils.isEmpty(impressionVisibleMS)) {
                serverExtras.put(DataKeys.IMPRESSION_VISIBLE_MS, impressionVisibleMS);
            }
            if (!TextUtils.isEmpty(impressionMinVisiblePx)) {
                serverExtras.put(DataKeys.IMPRESSION_MIN_VISIBLE_PX, impressionMinVisiblePx);
            }
        }

        // Extract internal video trackers, if available
        final String videoTrackers = extractHeader(jsonHeaders, ResponseHeader.VIDEO_TRACKERS);
        if (!TextUtils.isEmpty(videoTrackers)) {
            serverExtras.put(DataKeys.VIDEO_TRACKERS_KEY, videoTrackers);
        }

        // Banner imp tracking
        if (AdFormat.BANNER.equals(adFormat)) {
            builder.setBannerImpressionMinVisibleMs(
                    extractHeader(jsonHeaders, ResponseHeader.BANNER_IMPRESSION_MIN_VISIBLE_MS));
            builder.setBannerImpressionMinVisibleDips(
                    extractHeader(jsonHeaders, ResponseHeader.BANNER_IMPRESSION_MIN_VISIBLE_DIPS));
        }

        // Disable viewability vendors, if any
        final String disabledViewabilityVendors = extractHeader(jsonHeaders,
                ResponseHeader.DISABLE_VIEWABILITY);
        if (!TextUtils.isEmpty(disabledViewabilityVendors)) {
            try {
                int disableViewabilityMask = Integer.parseInt(disabledViewabilityVendors);
                if (disableViewabilityMask > 0) {
                    MoPub.disableViewability();
                }
            } catch (Exception ex) {
                MoPubLog.log(CUSTOM, "Error: invalid response value DISABLE_VIEWABILITY");
            }
        }

        final JSONArray viewabilityVerification = extractJsonArrayHeader(jsonHeaders, ResponseHeader.VIEWABILITY_VERIFICATION);
        final Set<ViewabilityVendor> vendors = ViewabilityVendor.createFromJsonArray(viewabilityVerification);
        builder.setViewabilityVendors(vendors);

        builder.setServerExtras(serverExtras);

        final String rewardedVideoCurrencyName = extractHeader(jsonHeaders,
                ResponseHeader.REWARDED_VIDEO_CURRENCY_NAME);
        final String rewardedVideoCurrencyAmount = extractHeader(jsonHeaders,
                ResponseHeader.REWARDED_VIDEO_CURRENCY_AMOUNT);
        final String rewardedCurrencies = extractHeader(jsonHeaders,
                ResponseHeader.REWARDED_CURRENCIES);
        final String rewardedVideoCompletionUrl = extractHeader(jsonHeaders,
                ResponseHeader.REWARDED_VIDEO_COMPLETION_URL);
        builder.setRewardedAdCurrencyName(rewardedVideoCurrencyName);
        builder.setRewardedAdCurrencyAmount(rewardedVideoCurrencyAmount);
        builder.setRewardedCurrencies(rewardedCurrencies);
        builder.setRewardedAdCompletionUrl(rewardedVideoCompletionUrl);
        builder.setRewarded(isRewarded);

        final CreativeExperienceSettings ceSettings =
                CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, isRewarded);
        builder.setCreativeExperienceSettings(ceSettings);

        return builder.build();
    }

    /**
     * Extract parameter 'x-refreshtime' from ad JSON
     *
     * @param item server data in JSON format
     * @return refresh time in milliseconds
     * @throws JSONException when JSON key is not found
     */
    @Nullable
    private static Integer extractRefreshTimeMS(@NonNull final JSONObject item) throws JSONException {
        Preconditions.checkNotNull(item);

        final JSONObject jsonHeaders = item.getJSONObject(ResponseHeader.METADATA.getKey());
        final Integer refreshTimeSeconds = extractIntegerHeader(jsonHeaders, ResponseHeader.REFRESH_TIME);
        return refreshTimeSeconds == null ? null : refreshTimeSeconds * 1000;
    }

    private static boolean extractWarmup(@NonNull final JSONObject item) {
        Preconditions.checkNotNull(item);

        final JSONObject jsonHeaders = item.optJSONObject(ResponseHeader.METADATA.getKey());
        return extractBooleanHeader(jsonHeaders, ResponseHeader.WARMUP, false);
    }

    // Based on Volley's StringResponse class.
    private static String parseStringBody(@NonNull final MoPubNetworkResponse response) {
        Preconditions.checkNotNull(response);

        String parsed;
        try {
            parsed = new String(response.getData(),
                    MoPubNetworkUtils.parseCharsetFromContentType(response.getHeaders()));
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.getData());
        }
        return parsed;
    }

    private static boolean eventDataIsInResponseBody(@Nullable final String adType,
                                                     @Nullable final String fullAdType) {
        return AdType.MRAID.equals(adType) || AdType.HTML.equals(adType) ||
                (AdType.INTERSTITIAL.equals(adType) && FullAdType.VAST.equals(fullAdType)) ||
                (AdType.INTERSTITIAL.equals(adType) && FullAdType.JSON.equals(fullAdType)) ||
                (AdType.REWARDED_VIDEO.equals(adType) && FullAdType.VAST.equals(fullAdType)) ||
                AdType.REWARDED_PLAYABLE.equals(adType) ||
                AdType.FULLSCREEN.equals(adType);
    }
}
