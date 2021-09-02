// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.logging.MoPubLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;

public class ViewabilityVendor implements Serializable {
    private static final long serialVersionUID = 2566572076713868153L;

    private static final String OMID = "omid";
    private static final String JAVASCRIPT_RESOURCE_URL = "javascriptResourceUrl";
    private static final String VENDOR_KEY = "vendorKey";
    private static final String VERIFICATION_PARAMETERS = "verificationParameters";
    private static final String API_FRAMEWORK = "apiFramework";

    @Nullable
    private final String vendorKey;
    @NonNull
    private final URL javascriptResourceUrl;
    @Nullable
    private final String verificationParameters;
    @Nullable
    private String verificationNotExecuted;

    private ViewabilityVendor(@NonNull final Builder builder) throws Exception {
        if (!OMID.equalsIgnoreCase(builder.apiFramework) ||
                TextUtils.isEmpty(builder.javascriptResourceUrl)) {
            throw new InvalidParameterException("ViewabilityVendor cannot be created.");
        }

        this.vendorKey = builder.vendorKey;
        this.javascriptResourceUrl = new URL(builder.javascriptResourceUrl);
        this.verificationParameters = builder.verificationParameters;
        this.verificationNotExecuted = builder.verificationNotExecuted;
    }

    @Nullable
    public String getVendorKey() {
        return vendorKey;
    }

    @NonNull
    public URL getJavascriptResourceUrl() {
        return javascriptResourceUrl;
    }

    @Nullable
    public String getVerificationParameters() {
        return verificationParameters;
    }

    @Nullable
    public String getVerificationNotExecuted() {
        return verificationNotExecuted;
    }

    //region equals, hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ViewabilityVendor)) return false;

        final ViewabilityVendor that = (ViewabilityVendor) o;

        if (!Objects.equals(vendorKey, that.vendorKey))
            return false;
        if (!Objects.equals(javascriptResourceUrl, that.javascriptResourceUrl))
            return false;
        if (!Objects.equals(verificationParameters, that.verificationParameters))
            return false;
        return Objects.equals(verificationNotExecuted, that.verificationNotExecuted);
    }

    @Override
    public int hashCode() {
        int result = vendorKey != null ? vendorKey.hashCode() : 0;
        result = 31 * result + javascriptResourceUrl.hashCode();
        result = 31 * result + (verificationParameters != null ? verificationParameters.hashCode() : 0);
        result = 31 * result + (verificationNotExecuted != null ? verificationNotExecuted.hashCode() : 0);
        return result;
    }
    //endregion

    @NonNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(vendorKey).append("\n")
                .append(javascriptResourceUrl).append("\n")
                .append(verificationParameters).append("\n");
        return builder.toString();
    }

    //region factory methods to create ViewabilityVendor object
    @Nullable
    static ViewabilityVendor createFromJson(@NonNull final JSONObject jsonObject) {
        final Builder builder = new Builder(jsonObject.optString(JAVASCRIPT_RESOURCE_URL));
        builder.withApiFramework(jsonObject.optString(API_FRAMEWORK, ""))
                .withVendorKey(jsonObject.optString(VENDOR_KEY, ""))
                .withVerificationParameters(jsonObject.optString(VERIFICATION_PARAMETERS, ""));

        return builder.build();
    }

    @NonNull
    public static Set<ViewabilityVendor> createFromJsonArray(@Nullable final JSONArray viewabilityVendors) {
        final HashSet<ViewabilityVendor> list = new HashSet<>();
        if (viewabilityVendors != null) {
            for (int i = 0; i < viewabilityVendors.length(); i++) {
                final JSONObject item = viewabilityVendors.optJSONObject(i);
                final ViewabilityVendor vendor = createFromJson(item);
                if (vendor != null) {
                    list.add(vendor);
                }
            }
        }

        return list;
    }

    public static class Builder {
        @Nullable
        private String apiFramework = OMID;
        @Nullable
        private String vendorKey;
        @NonNull
        private String javascriptResourceUrl;
        @Nullable
        private String verificationParameters;
        @Nullable
        private String verificationNotExecuted;

        public Builder(@NonNull final String javascriptResourceUrl) {
            this.javascriptResourceUrl = javascriptResourceUrl;
        }

        @NonNull
        public Builder withVendorKey(@Nullable final String vendorKey) {
            this.vendorKey = vendorKey;
            return this;
        }

        @NonNull
        public Builder withVerificationParameters(@Nullable final String verificationParameters) {
            this.verificationParameters = verificationParameters;
            return this;
        }

        @NonNull
        public Builder withApiFramework(@Nullable final String apiFramework) {
            this.apiFramework = apiFramework;
            return this;
        }

        @NonNull
        public Builder withVerificationNotExecuted(@Nullable final String verificationNotExecuted) {
            this.verificationNotExecuted = verificationNotExecuted;
            return this;
        }

        @Nullable
        public ViewabilityVendor build() {
            try {
                return new ViewabilityVendor(this);
            } catch (Exception e) {
                MoPubLog.log(CUSTOM, "Warning: " + e.getLocalizedMessage());
            }
            return null;
        }
    }
    //endregion
}
