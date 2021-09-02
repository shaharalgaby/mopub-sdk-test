// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.DataKeys;
import com.mopub.common.ExternalViewabilitySessionManager;
import com.mopub.common.ViewabilityVendor;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.nativeads.NativeImageHelper.ImageListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.mopub.common.DataKeys.JSON_BODY_KEY;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.util.Numbers.parseDouble;
import static com.mopub.nativeads.NativeImageHelper.preCacheImages;

public class MoPubCustomEventNative extends CustomEventNative {
    public static final String ADAPTER_NAME = MoPubCustomEventNative.class.getSimpleName();
    private MoPubStaticNativeAd moPubStaticNativeAd;

    @Override
    protected void loadNativeAd(@NonNull final Context context,
                                @NonNull final CustomEventNativeListener customEventNativeListener,
                                @NonNull final Map<String, Object> localExtras,
                                @NonNull final Map<String, String> serverExtras) {
        MoPubLog.log(LOAD_ATTEMPTED, ADAPTER_NAME);

        if (moPubStaticNativeAd != null && !moPubStaticNativeAd.isInvalidated()) {
            return;
        }

        Object json = localExtras.get(JSON_BODY_KEY);
        // null or non-JSONObjects should not be passed in localExtras as JSON_BODY_KEY
        if (!(json instanceof JSONObject)) {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    NativeErrorCode.INVALID_RESPONSE.getIntCode(),
                    NativeErrorCode.INVALID_RESPONSE);
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.INVALID_RESPONSE);
            return;
        }

        moPubStaticNativeAd =
                new MoPubStaticNativeAd(context,
                        (JSONObject) json,
                        new ImpressionTracker(context),
                        new NativeClickHandler(context),
                        customEventNativeListener);

        if (serverExtras.containsKey(DataKeys.IMPRESSION_MIN_VISIBLE_PERCENT)) {
            try {
                moPubStaticNativeAd.setImpressionMinPercentageViewed(Integer.parseInt(
                        serverExtras.get(DataKeys.IMPRESSION_MIN_VISIBLE_PERCENT)));
            } catch (final NumberFormatException e) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unable to format min visible percent: " +
                        serverExtras.get(DataKeys.IMPRESSION_MIN_VISIBLE_PERCENT));
            }
        }

        if (serverExtras.containsKey(DataKeys.IMPRESSION_VISIBLE_MS)) {
            try {
                moPubStaticNativeAd.setImpressionMinTimeViewed(
                        Integer.parseInt(serverExtras.get(DataKeys.IMPRESSION_VISIBLE_MS)));
            } catch (final NumberFormatException e) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unable to format min time: " +
                        serverExtras.get(DataKeys.IMPRESSION_VISIBLE_MS));
            }
        }

        if (serverExtras.containsKey(DataKeys.IMPRESSION_MIN_VISIBLE_PX)) {
            try {
                moPubStaticNativeAd.setImpressionMinVisiblePx(Integer.parseInt(
                        serverExtras.get(DataKeys.IMPRESSION_MIN_VISIBLE_PX)));
            } catch (final NumberFormatException e) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unable to format min visible px: " +
                        serverExtras.get(DataKeys.IMPRESSION_MIN_VISIBLE_PX));
            }
        }

        try {
            final Object vendorSet = localExtras.get(DataKeys.VIEWABILITY_VENDORS_KEY);
            if (vendorSet instanceof Set) {
                moPubStaticNativeAd.setViewabilityVendors((Set<ViewabilityVendor>)vendorSet);
            }
        } catch (Exception ex) {
            MoPubLog.log(CUSTOM, "Ignore empty viewability vendors list.");
        }

        try {
            moPubStaticNativeAd.loadAd();
            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
        } catch (IllegalArgumentException e) {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    NativeErrorCode.UNSPECIFIED.getIntCode(),
                    NativeErrorCode.UNSPECIFIED);
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
        }
    }

    @Override
    protected void onInvalidate() {
        if (moPubStaticNativeAd == null) {
            return;
        }
        moPubStaticNativeAd.invalidate();
    }

    static class MoPubStaticNativeAd extends StaticNativeAd {
        enum Parameter {
            IMPRESSION_TRACKER("imptracker", true),
            CLICK_TRACKER("clktracker", true),

            TITLE("title", false),
            TEXT("text", false),
            MAIN_IMAGE("mainimage", false),
            ICON_IMAGE("iconimage", false),

            CLICK_DESTINATION("clk", false),
            FALLBACK("fallback", false),
            CALL_TO_ACTION("ctatext", false),
            STAR_RATING("starrating", false),

            PRIVACY_INFORMATION_ICON_IMAGE_URL("privacyicon", false),
            PRIVACY_INFORMATION_ICON_CLICKTHROUGH_URL("privacyclkurl", false),

            SPONSORED("sponsored", false);

            @NonNull
            final String name;
            final boolean required;

            Parameter(@NonNull final String name, final boolean required) {
                this.name = name;
                this.required = required;
            }

            @Nullable
            static Parameter from(@NonNull final String name) {
                for (final Parameter parameter : values()) {
                    if (parameter.name.equals(name)) {
                        return parameter;
                    }
                }

                return null;
            }

            @NonNull
            @VisibleForTesting
            static final Set<String> requiredKeys = new HashSet<String>();

            static {
                for (final Parameter parameter : values()) {
                    if (parameter.required) {
                        requiredKeys.add(parameter.name);
                    }
                }
            }
        }

        @VisibleForTesting
        static final String PRIVACY_INFORMATION_CLICKTHROUGH_URL = "https://www.mopub.com/optout";

        @NonNull
        private final Context mContext;
        @NonNull
        private final CustomEventNativeListener mCustomEventNativeListener;
        @NonNull
        private final JSONObject mJsonObject;
        @NonNull
        private final ImpressionTracker mImpressionTracker;
        @NonNull
        private final NativeClickHandler mNativeClickHandler;
        @Nullable
        private ExternalViewabilitySessionManager viewabilitySessionManager;
        @NonNull
        private final Set<ViewabilityVendor> viewabilityVendorsSet;


        MoPubStaticNativeAd(@NonNull final Context context,
                            @NonNull final JSONObject jsonBody,
                            @NonNull final ImpressionTracker impressionTracker,
                            @NonNull final NativeClickHandler nativeClickHandler,
                            @NonNull final CustomEventNativeListener customEventNativeListener) {
            mJsonObject = jsonBody;
            mContext = context.getApplicationContext();
            mImpressionTracker = impressionTracker;
            mNativeClickHandler = nativeClickHandler;
            mCustomEventNativeListener = customEventNativeListener;
            viewabilityVendorsSet = new HashSet<>();
        }

        void loadAd() throws IllegalArgumentException {
            if (!containsRequiredKeys(mJsonObject)) {
                throw new IllegalArgumentException("JSONObject did not contain required keys.");
            }

            final Iterator<String> keys = mJsonObject.keys();
            while (keys.hasNext()) {
                final String key = keys.next();
                final Parameter parameter = Parameter.from(key);

                if (parameter != null) {
                    try {
                        addInstanceVariable(parameter, mJsonObject.opt(key));
                    } catch (ClassCastException e) {
                        throw new IllegalArgumentException("JSONObject key (" + key + ") contained unexpected value.");
                    }
                } else {
                    addExtra(key, mJsonObject.opt(key));
                }
            }
            if (TextUtils.isEmpty(getPrivacyInformationIconClickThroughUrl())) {
                setPrivacyInformationIconClickThroughUrl(PRIVACY_INFORMATION_CLICKTHROUGH_URL);
            }

            preCacheImages(mContext, getAllImageUrls(), new ImageListener() {
                @Override
                public void onImagesCached() {
                    if (isInvalidated()) {
                        return;
                    }
                    mCustomEventNativeListener.onNativeAdLoaded(MoPubStaticNativeAd.this);
                }

                @Override
                public void onImagesFailedToCache(final NativeErrorCode errorCode) {
                    if (isInvalidated()) {
                        return;
                    }
                    mCustomEventNativeListener.onNativeAdFailed(errorCode);
                }
            });
        }

        private boolean containsRequiredKeys(@NonNull final JSONObject jsonObject) {
            final Set<String> keys = new HashSet<String>();
            final Iterator<String> jsonKeys = jsonObject.keys();
            while (jsonKeys.hasNext()) {
                keys.add(jsonKeys.next());
            }

            return keys.containsAll(Parameter.requiredKeys);
        }

        private void addInstanceVariable(@NonNull final Parameter key,
                                         @Nullable final Object value) throws ClassCastException {
            try {
                switch (key) {
                    case MAIN_IMAGE:
                        setMainImageUrl((String) value);
                        break;
                    case ICON_IMAGE:
                        setIconImageUrl((String) value);
                        break;
                    case IMPRESSION_TRACKER:
                        addImpressionTrackers(value);
                        break;
                    case CLICK_DESTINATION:
                        setClickDestinationUrl((String) value);
                        break;
                    case CLICK_TRACKER:
                        parseClickTrackers(value);
                        break;
                    case CALL_TO_ACTION:
                        setCallToAction((String) value);
                        break;
                    case TITLE:
                        setTitle((String) value);
                        break;
                    case TEXT:
                        setText((String) value);
                        break;
                    case STAR_RATING:
                        setStarRating(parseDouble(value));
                        break;
                    case PRIVACY_INFORMATION_ICON_IMAGE_URL:
                        setPrivacyInformationIconImageUrl((String) value);
                        break;
                    case PRIVACY_INFORMATION_ICON_CLICKTHROUGH_URL:
                        setPrivacyInformationIconClickThroughUrl((String) value);
                        break;
                    case SPONSORED:
                        setSponsored((String) value);
                    default:
                        MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unable to add JSON key to internal mapping: " + key.name);
                        break;
                }
            } catch (ClassCastException e) {
                if (!key.required) {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Ignoring class cast exception for optional key: " + key.name);
                } else {
                    throw e;
                }
            }
        }

        private void setViewabilityVendors(@NonNull final Set<ViewabilityVendor> viewabilityVendors) {
            viewabilityVendorsSet.addAll(viewabilityVendors);
        }

        private void parseClickTrackers(@NonNull final Object clickTrackers) {
            if (clickTrackers instanceof JSONArray) {
                addClickTrackers(clickTrackers);
            } else {
                addClickTracker((String) clickTrackers);
            }
        }

        private boolean isImageKey(@Nullable final String name) {
            return name != null && name.toLowerCase(Locale.US).endsWith("image");
        }

        @NonNull
        List<String> getExtrasImageUrls() {
            final List<String> extrasBitmapUrls = new ArrayList<String>(getExtras().size());
            for (final Map.Entry<String, Object> entry : getExtras().entrySet()) {
                if (isImageKey(entry.getKey()) && entry.getValue() instanceof String) {
                    extrasBitmapUrls.add((String) entry.getValue());
                }
            }

            return extrasBitmapUrls;
        }

        @NonNull
        List<String> getAllImageUrls() {
            final List<String> imageUrls = new ArrayList<String>();
            if (!TextUtils.isEmpty(getMainImageUrl())) {
                imageUrls.add(getMainImageUrl());
            }
            if (!TextUtils.isEmpty(getIconImageUrl())) {
                imageUrls.add(getIconImageUrl());
            }
            if (!TextUtils.isEmpty(getPrivacyInformationIconImageUrl())) {
                imageUrls.add(getPrivacyInformationIconImageUrl());
            }
            imageUrls.addAll(getExtrasImageUrls());
            return imageUrls;
        }

        // Lifecycle Handlers
        @Override
        public void prepare(@NonNull final View view) {
            mImpressionTracker.addView(view, this);
            mNativeClickHandler.setOnClickListener(view, this);
            if (viewabilitySessionManager == null) {
                viewabilitySessionManager = ExternalViewabilitySessionManager.create();
                viewabilitySessionManager.createNativeSession(view, viewabilityVendorsSet);
                viewabilitySessionManager.startSession();
            } else {
                viewabilitySessionManager.registerTrackedView(view);
            }
        }

        @Override
        public void clear(@NonNull final View view) {
            mImpressionTracker.removeView(view);
            mNativeClickHandler.clearOnClickListener(view);
        }

        @Override
        public void destroy() {
            mImpressionTracker.destroy();
            if (viewabilitySessionManager != null) {
                viewabilitySessionManager.registerTrackedView(new View(mContext));
                viewabilitySessionManager.endSession();
                viewabilitySessionManager = null;
            }
            super.destroy();
        }

        // Event Handlers
        @Override
        public void recordImpression(@NonNull final View view) {
            notifyAdImpressed();
            if (viewabilitySessionManager != null) {
                viewabilitySessionManager.trackImpression();
            }
        }

        @Override
        public void handleClick(@Nullable final View view) {
            MoPubLog.log(CLICKED, ADAPTER_NAME);
            notifyAdClicked();
            mNativeClickHandler.openClickDestinationUrl(getClickDestinationUrl(), view);
        }
    }
}
