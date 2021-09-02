// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.location.Location;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.MoPub;

import java.util.EnumSet;

public class RequestParameters {

    public enum NativeAdAsset {
        TITLE("title"),
        TEXT("text"),
        ICON_IMAGE("iconimage"),
        MAIN_IMAGE("mainimage"),
        CALL_TO_ACTION_TEXT("ctatext"),
        STAR_RATING("starrating"),
        SPONSORED("sponsored");

        private final String mAssetName;

        private NativeAdAsset(@NonNull String assetName) {
            mAssetName = assetName;
        }

        @NonNull
        @Override
        public String toString() {
            return mAssetName;
        }
    }

    @Nullable private final String mKeywords;
    @Nullable private final String mUserDataKeywords;
    @Nullable private final Location mLocation;
    @Nullable private final EnumSet<NativeAdAsset> mDesiredAssets;

    public final static class Builder {
        private String keywords;
        private String userDatakeywords;
        private Location location;
        private EnumSet<NativeAdAsset> desiredAssets;

        @NonNull
        public final Builder keywords(String keywords) {
            this.keywords = keywords;
            return this;
        }

        @NonNull
        public final Builder userDataKeywords(String userDataKeywords) {
            this.userDatakeywords = MoPub.canCollectPersonalInformation() ? userDataKeywords : null;
            return this;
        }

        @NonNull
        public final Builder location(Location location) {
            this.location = MoPub.canCollectPersonalInformation() ? location : null;
            return this;
        }

        // Specify set of assets used by this ad request. If not set, this defaults to all assets
        @NonNull
        public final Builder desiredAssets(final EnumSet<NativeAdAsset> desiredAssets) {
            this.desiredAssets = EnumSet.copyOf(desiredAssets);
            return this;
        }

        @NonNull
        public final RequestParameters build() {
            return new RequestParameters(this);
        }
    }

    private RequestParameters(@NonNull Builder builder) {
        mKeywords = builder.keywords;
        mDesiredAssets = builder.desiredAssets;

        final boolean canCollectPersonalInformation = MoPub.canCollectPersonalInformation();
        mUserDataKeywords = canCollectPersonalInformation ? builder.userDatakeywords : null;
        mLocation = canCollectPersonalInformation ? builder.location : null;
    }

    @Nullable
    public final String getKeywords() {
        return mKeywords;
    }

    @Nullable
    public final String getUserDataKeywords() {
        if(!MoPub.canCollectPersonalInformation()) {
            return null;
        }
        return mUserDataKeywords;
    }

    @Nullable
    public final Location getLocation() {
        return mLocation;
    }

    public final String getDesiredAssets() {
        String result = "";

        if (mDesiredAssets != null) {
            result = TextUtils.join(",", mDesiredAssets.toArray());
        }
        return result;
    }
}
