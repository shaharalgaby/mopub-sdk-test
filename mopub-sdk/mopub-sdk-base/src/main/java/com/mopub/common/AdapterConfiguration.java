// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

public interface AdapterConfiguration {
    /**
     * Gets the adapter version.
     *
     * @return String representing the adapter version.
     */
    @NonNull
    String getAdapterVersion();

    /**
     * If this adapter has advanced bidding enabled, return an advanced bidding token. Otherwise,
     * it's okay to return null.
     *
     * @param context Context to reach Android resources.
     * @return String representing an advanced bidding token.
     */
    @Nullable
    String getBiddingToken(@NonNull final Context context);

    /**
     * The MoPub-internal name for this particular adapter.
     *
     * @return String representing the MoPub network name.
     */
    @NonNull
    String getMoPubNetworkName();

    /**
     * Get the map of options passed to our ad server. May be null.
     *
     * @return Map of options.
     */
    @Nullable
    Map<String, String> getMoPubRequestOptions();

    /**
     * Gets the version of the network.
     *
     * @return String representing the version of the network.
     */
    @NonNull
    String getNetworkSdkVersion();

    /**
     * Initializes the network. The adapter MUST call
     * {@link OnNetworkInitializationFinishedListener#onNetworkInitializationFinished} or
     * sdk initialization will not finish.
     *
     * @param context       Context to init with.
     * @param configuration Map of network initialization parameters.
     * @param listener      Callback for the SDK to continue initialization.
     */
    void initializeNetwork(@NonNull final Context context,
            @Nullable final Map<String, String> configuration,
            @NonNull final OnNetworkInitializationFinishedListener listener);

    /**
     * Save initialization parameters for future use.
     *
     * @param context       Context if needed.
     * @param configuration The map to save.
     */
    void setCachedInitializationParameters(@NonNull final Context context,
            @Nullable final Map<String, String> configuration);

    /**
     * Gets a map of network initialization parameters.
     *
     * @param context Context if needed.
     * @return Map of network initialization parameters.
     */
    @NonNull
    Map<String, String> getCachedInitializationParameters(@NonNull final Context context);

    /**
     * Sets a map of request options to send to the MoPub ad server.
     *
     * @param moPubRequestOptions Map of request options.
     */
    void setMoPubRequestOptions(@Nullable final Map<String, String> moPubRequestOptions);
}
