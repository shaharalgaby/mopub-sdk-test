// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.BrowserAgentManager.BrowserAgent;
import com.mopub.common.Preconditions;
import com.mopub.common.ViewabilityVendor;
import com.mopub.common.util.DateAndTime;
import com.mopub.mobileads.CreativeExperienceSettings;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class AdResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    @Nullable
    private final String mAdType;

    @Nullable
    private final String mAdGroupId;

    @Nullable
    private final String mAdUnitId;

    @Nullable
    private final String mFullAdType;
    @Nullable
    private final String mNetworkType;

    private final boolean mRewarded;
    @Nullable
    private final String mRewardedAdCurrencyName;
    @Nullable
    private final String mRewardedAdCurrencyAmount;
    @Nullable
    private final String mRewardedCurrencies;
    @Nullable
    private final String mRewardedAdCompletionUrl;

    @Nullable
    private final ImpressionData mImpressionData;
    @NonNull
    private final List<String> mClickTrackingUrls;
    @NonNull
    private final List<String> mImpressionTrackingUrls;
    @Nullable
    private final String mFailoverUrl;
    @NonNull
    private final List<String> mBeforeLoadUrls;
    @NonNull
    private final List<String> mAfterLoadUrls;
    @NonNull
    private final List<String> mAfterLoadSuccessUrls;
    @NonNull
    private final List<String> mAfterLoadFailUrls;
    @Nullable
    private final String mRequestId;

    @Nullable
    private final Integer mWidth;
    @Nullable
    private final Integer mHeight;
    @Nullable
    private final Integer mAdTimeoutDelayMillis;
    @Nullable
    private final Integer mRefreshTimeMillis;
    @Nullable
    private final String mBannerImpressionMinVisibleDips;
    @Nullable
    private final String mBannerImpressionMinVisibleMs;
    @Nullable
    private final String mDspCreativeId;

    @Nullable
    private final String mResponseBody;
    @Nullable
    private final JSONObject mJsonBody;

    @Nullable
    private final String mBaseAdClassName;
    @Nullable
    private final BrowserAgent mBrowserAgent;
    @NonNull
    private final Map<String, String> mServerExtras;

    private final long mTimestamp;

    @Nullable
    private final Set<ViewabilityVendor> mViewabilityVendors;

    @NonNull
    private final CreativeExperienceSettings mCreativeExperienceSettings;

    private AdResponse(@NonNull Builder builder) {

        mAdType = builder.adType;
        mAdGroupId = builder.adGroupId;
        mAdUnitId = builder.adUnitId;
        mFullAdType = builder.fullAdType;
        mNetworkType = builder.networkType;

        mRewarded = builder.rewarded;
        mRewardedAdCurrencyName = builder.rewardedAdCurrencyName;
        mRewardedAdCurrencyAmount = builder.rewardedAdCurrencyAmount;
        mRewardedCurrencies = builder.rewardedCurrencies;
        mRewardedAdCompletionUrl = builder.rewardedAdCompletionUrl;

        mImpressionData = builder.impressionData;
        mClickTrackingUrls = builder.clickTrackingUrls;
        mImpressionTrackingUrls = builder.impressionTrackingUrls;
        mFailoverUrl = builder.failoverUrl;
        mBeforeLoadUrls = builder.beforeLoadUrls;
        mAfterLoadUrls = builder.afterLoadUrls;
        mAfterLoadSuccessUrls = builder.afterLoadSuccessUrls;
        mAfterLoadFailUrls = builder.afterLoadFailUrls;
        mRequestId = builder.requestId;
        mWidth = builder.width;
        mHeight = builder.height;
        mAdTimeoutDelayMillis = builder.adTimeoutDelayMillis;
        mRefreshTimeMillis = builder.refreshTimeMillis;
        mBannerImpressionMinVisibleDips = builder.bannerImpressionMinVisibleDips;
        mBannerImpressionMinVisibleMs = builder.bannerImpressionMinVisibleMs;
        mDspCreativeId = builder.dspCreativeId;
        mResponseBody = builder.responseBody;
        mJsonBody = builder.jsonBody;
        mBaseAdClassName = builder.customEventClassName;
        mBrowserAgent = builder.browserAgent;
        mServerExtras = builder.serverExtras;
        mTimestamp = DateAndTime.now().getTime();
        mViewabilityVendors = builder.viewabilityVendors;
        mCreativeExperienceSettings = builder.creativeExperienceSettings;
    }

    public boolean hasJson() {
        return mJsonBody != null;
    }

    @Nullable
    public JSONObject getJsonBody() {
        return mJsonBody;
    }

    @Nullable
    public String getStringBody() {
        return mResponseBody;
    }

    @Nullable
    public String getAdType() {
        return mAdType;
    }

    @Nullable
    public String getAdGroupId() {
        return mAdGroupId;
    }

    @Nullable
    public String getFullAdType() {
        return mFullAdType;
    }

    @Nullable
    public String getAdUnitId() {
        return mAdUnitId;
    }

    @Nullable
    public String getNetworkType() {
        return mNetworkType;
    }

    public boolean isRewarded() {
        return mRewarded;
    }

    @Nullable
    public String getRewardedAdCurrencyName() {
        return mRewardedAdCurrencyName;
    }

    @Nullable
    public String getRewardedAdCurrencyAmount() {
        return mRewardedAdCurrencyAmount;
    }

    @Nullable
    public String getRewardedCurrencies() {
        return mRewardedCurrencies;
    }

    @Nullable
    public String getRewardedAdCompletionUrl() {
        return mRewardedAdCompletionUrl;
    }

    @Nullable
    public ImpressionData getImpressionData() {
        return mImpressionData;
    }

    @NonNull
    public List<String> getClickTrackingUrls() {
        return mClickTrackingUrls;
    }

    @NonNull
    public List<String> getImpressionTrackingUrls() {
        return mImpressionTrackingUrls;
    }

    @Deprecated
    @Nullable
    public String getFailoverUrl() {
        return mFailoverUrl;
    }

    @NonNull
    public List<String> getBeforeLoadUrls() {
        return mBeforeLoadUrls;
    }

    @NonNull
    public List<String> getAfterLoadUrls() {
        return mAfterLoadUrls;
    }

    @NonNull
    public List<String> getAfterLoadSuccessUrls() {
        return mAfterLoadSuccessUrls;
    }

    @NonNull
    public List<String> getAfterLoadFailUrls() {
        return mAfterLoadFailUrls;
    }

    @Nullable
    public String getRequestId() {
        return mRequestId;
    }

    @Nullable
    public Integer getWidth() {
        return mWidth;
    }

    @Nullable
    public Integer getHeight() {
        return mHeight;
    }

    @NonNull
    public Integer getAdTimeoutMillis(int defaultValue) {
        if (mAdTimeoutDelayMillis == null || mAdTimeoutDelayMillis < 1000) {
            return defaultValue;
        }
        return mAdTimeoutDelayMillis;
    }

    @Nullable
    public Integer getRefreshTimeMillis() {
        return mRefreshTimeMillis;
    }

    @Nullable
    public String getImpressionMinVisibleDips() {
        return mBannerImpressionMinVisibleDips;
    }

    @Nullable
    public String getImpressionMinVisibleMs() {
        return mBannerImpressionMinVisibleMs;
    }

    @Nullable
    public String getDspCreativeId() {
        return mDspCreativeId;
    }

    @Nullable
    @Deprecated
    public String getCustomEventClassName() {
        return getBaseAdClassName();
    }

    @Nullable
    public String getBaseAdClassName() {
        return mBaseAdClassName;
    }

    @Nullable
    public BrowserAgent getBrowserAgent() { return mBrowserAgent; }

    @NonNull
    public Map<String, String> getServerExtras() {
        // Strings are immutable, so this works as a "deep" copy.
        return new TreeMap<>(mServerExtras);
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    @Nullable
    public Set<ViewabilityVendor> getViewabilityVendors() {
        return mViewabilityVendors;
    }

    @NonNull
    public CreativeExperienceSettings getCreativeExperienceSettings() {
        return mCreativeExperienceSettings;
    }

    public Builder toBuilder() {
        return new Builder()
                .setAdType(mAdType)
                .setAdGroupId(mAdGroupId)
                .setNetworkType(mNetworkType)
                .setRewarded(mRewarded)
                .setRewardedAdCurrencyName(mRewardedAdCurrencyName)
                .setRewardedAdCurrencyAmount(mRewardedAdCurrencyAmount)
                .setRewardedCurrencies(mRewardedCurrencies)
                .setRewardedAdCompletionUrl(mRewardedAdCompletionUrl)
                .setImpressionData(mImpressionData)
                .setClickTrackingUrls(mClickTrackingUrls)
                .setImpressionTrackingUrls(mImpressionTrackingUrls)
                .setFailoverUrl(mFailoverUrl)
                .setBeforeLoadUrls(mBeforeLoadUrls)
                .setAfterLoadUrls(mAfterLoadUrls)
                .setAfterLoadSuccessUrls(mAfterLoadSuccessUrls)
                .setAfterLoadFailUrls(mAfterLoadFailUrls)
                .setDimensions(mWidth, mHeight)
                .setAdTimeoutDelayMilliseconds(mAdTimeoutDelayMillis)
                .setRefreshTimeMilliseconds(mRefreshTimeMillis)
                .setBannerImpressionMinVisibleDips(mBannerImpressionMinVisibleDips)
                .setBannerImpressionMinVisibleMs(mBannerImpressionMinVisibleMs)
                .setDspCreativeId(mDspCreativeId)
                .setResponseBody(mResponseBody)
                .setJsonBody(mJsonBody)
                .setBaseAdClassName(mBaseAdClassName)
                .setBrowserAgent(mBrowserAgent)
                .setServerExtras(mServerExtras)
                .setViewabilityVendors(mViewabilityVendors)
                .setCreativeExperienceSettings(mCreativeExperienceSettings);
    }

    public static class Builder {
        private String adType;
        private String adGroupId;
        private String adUnitId;
        private String fullAdType;
        private String networkType;

        private boolean rewarded = false;
        private String rewardedAdCurrencyName;
        private String rewardedAdCurrencyAmount;
        private String rewardedCurrencies;
        private String rewardedAdCompletionUrl;

        private ImpressionData impressionData;
        private List<String> clickTrackingUrls = new ArrayList<>();
        private List<String> impressionTrackingUrls = new ArrayList<>();
        private String failoverUrl;
        private List<String> beforeLoadUrls = new ArrayList<>();
        private List<String> afterLoadUrls = new ArrayList<>();
        private List<String> afterLoadSuccessUrls = new ArrayList<>();
        private List<String> afterLoadFailUrls = new ArrayList<>();
        private String requestId;

        private Integer width;
        private Integer height;
        private Integer adTimeoutDelayMillis;
        private Integer refreshTimeMillis;
        private String bannerImpressionMinVisibleDips;
        private String bannerImpressionMinVisibleMs;
        private String dspCreativeId;

        private String responseBody;
        private JSONObject jsonBody;

        private String customEventClassName;
        private BrowserAgent browserAgent;

        private Map<String, String> serverExtras = new TreeMap<>();

        private Set<ViewabilityVendor> viewabilityVendors = null;

        private CreativeExperienceSettings creativeExperienceSettings;

        public Builder setAdType(@Nullable final String adType) {
            this.adType = adType;
            return this;
        }

        public Builder setAdGroupId(@Nullable final String adGroupId) {
            this.adGroupId = adGroupId;
            return this;
        }

        public Builder setAdUnitId(@Nullable final String adUnitId) {
            this.adUnitId = adUnitId;
            return this;
        }

        public Builder setFullAdType(@Nullable final String fullAdType) {
            this.fullAdType = fullAdType;
            return this;
        }

        public Builder setNetworkType(@Nullable final String networkType) {
            this.networkType = networkType;
            return this;
        }

        public Builder setRewarded(boolean rewarded) {
            this.rewarded = rewarded;
            return this;
        }

        public Builder setRewardedAdCurrencyName(
                @Nullable final String rewardedAdCurrencyName) {
            this.rewardedAdCurrencyName = rewardedAdCurrencyName;
            return this;
        }

        public Builder setRewardedAdCurrencyAmount(
                @Nullable final String rewardedAdCurrencyAmount) {
            this.rewardedAdCurrencyAmount = rewardedAdCurrencyAmount;
            return this;
        }

        public Builder setRewardedCurrencies(@Nullable final String rewardedCurrencies) {
            this.rewardedCurrencies = rewardedCurrencies;
            return this;
        }

        public Builder setRewardedAdCompletionUrl(
                @Nullable final String rewardedAdCompletionUrl) {
            this.rewardedAdCompletionUrl = rewardedAdCompletionUrl;
            return this;
        }

        public Builder setImpressionData(@Nullable ImpressionData impressionData) {
            this.impressionData = impressionData;
            return this;
        }

        public Builder setClickTrackingUrls(@NonNull final List<String> clickTrackingUrls) {
            Preconditions.checkNotNull(clickTrackingUrls);

            this.clickTrackingUrls = clickTrackingUrls;
            return this;
        }

        public Builder setImpressionTrackingUrls(@NonNull final List<String> impressionTrackingUrls) {
            Preconditions.checkNotNull(impressionTrackingUrls);

            this.impressionTrackingUrls = impressionTrackingUrls;
            return this;
        }

        public Builder setFailoverUrl(@Nullable final String failoverUrl) {
            this.failoverUrl = failoverUrl;
            return this;
        }

        public Builder setBeforeLoadUrls(@NonNull final List<String> beforeLoadUrls) {
            Preconditions.checkNotNull(beforeLoadUrls);

            this.beforeLoadUrls = beforeLoadUrls;
            return this;
        }

        public Builder setAfterLoadUrls(@NonNull final List<String> afterLoadUrls) {
            Preconditions.checkNotNull(afterLoadUrls);
            this.afterLoadUrls = afterLoadUrls;
            return this;
        }

        public Builder setAfterLoadSuccessUrls(@NonNull final List<String> afterLoadSuccessUrls) {
            Preconditions.checkNotNull(afterLoadSuccessUrls);
            this.afterLoadSuccessUrls = afterLoadSuccessUrls;
            return this;
        }

        public Builder setAfterLoadFailUrls(@NonNull final List<String> afterLoadFailUrls) {
            Preconditions.checkNotNull(afterLoadFailUrls);
            this.afterLoadFailUrls = afterLoadFailUrls;
            return this;
        }

        public Builder setRequestId(@Nullable final String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder setDimensions(@Nullable final Integer width,
                @Nullable final Integer height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder setAdTimeoutDelayMilliseconds(@Nullable final Integer adTimeoutDelayMilliseconds) {
            this.adTimeoutDelayMillis = adTimeoutDelayMilliseconds;
            return this;
        }

        public Builder setRefreshTimeMilliseconds(@Nullable final Integer refreshTimeMilliseconds) {
            this.refreshTimeMillis = refreshTimeMilliseconds;
            return this;
        }

        public Builder setBannerImpressionMinVisibleDips(@Nullable final String bannerImpressionMinVisibleDips) {
            this.bannerImpressionMinVisibleDips = bannerImpressionMinVisibleDips;
            return this;
        }

        public Builder setBannerImpressionMinVisibleMs(@Nullable final String bannerImpressionMinVisibleMs) {
            this.bannerImpressionMinVisibleMs = bannerImpressionMinVisibleMs;
            return this;
        }

        public Builder setDspCreativeId(@Nullable final String dspCreativeId) {
            this.dspCreativeId = dspCreativeId;
            return this;
        }

        public Builder setResponseBody(@Nullable final String responseBody) {
            this.responseBody = responseBody;
            return this;
        }

        public Builder setJsonBody(@Nullable final JSONObject jsonBody) {
            this.jsonBody = jsonBody;
            return this;
        }

        public Builder setBaseAdClassName(@Nullable final String customEventClassName) {
            this.customEventClassName = customEventClassName;
            return this;
        }

        public Builder setBrowserAgent(@Nullable final BrowserAgent browserAgent) {
            this.browserAgent = browserAgent;
            return this;
        }

        public Builder setServerExtras(@Nullable final Map<String, String> serverExtras) {
            if (serverExtras == null) {
                this.serverExtras = new TreeMap<>();
            } else {
                this.serverExtras = new TreeMap<>(serverExtras);
            }
            return this;
        }

        public Builder setViewabilityVendors(@Nullable final Set<ViewabilityVendor> viewabilityVendors) {
            this.viewabilityVendors = viewabilityVendors;
            return this;
        }

        public Builder setCreativeExperienceSettings(
                @NonNull CreativeExperienceSettings creativeExperienceSettings) {
            Preconditions.checkNotNull(creativeExperienceSettings);

            this.creativeExperienceSettings = creativeExperienceSettings;
            return this;
        }

        public AdResponse build() {
            return new AdResponse(this);
        }
    }
}
