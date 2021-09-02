// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

/**
 * An annotation that indicates that the visibility of a type or member has been relaxed to make
 * the code testable. You should not use VisibleForTesting methods when integrating the SDK, as
 * they may change in future versions.
 */
public @interface VisibleForTesting {
}
