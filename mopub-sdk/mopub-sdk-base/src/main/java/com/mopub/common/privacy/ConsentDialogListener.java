// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import androidx.annotation.NonNull;

import com.mopub.mobileads.MoPubErrorCode;

/**
 * Use this interface to listen to a successful or failed consent dialog load request.
 */
public interface ConsentDialogListener {
    /**
     * Called when the consent dialog successfully loads.
     */
    void onConsentDialogLoaded();

    /**
     * Called when the consent dialog fails to load.
     *
     * @param moPubErrorCode The reason why the dialog failed to load.
     */
    void onConsentDialogLoadFailed(@NonNull final MoPubErrorCode moPubErrorCode);
}
