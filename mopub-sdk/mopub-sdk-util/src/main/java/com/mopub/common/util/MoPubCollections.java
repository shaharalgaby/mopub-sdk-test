// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.Preconditions;

import java.util.Collection;
import java.util.Collections;

public class MoPubCollections {
    public static <T> void addAllNonNull(@NonNull final Collection<? super T> c,
            @Nullable final T... a) {
        Collections.addAll(c, a);
        c.removeAll(Collections.singleton(null));
    }

    public static <T> void addAllNonNull(@NonNull final Collection<? super T> collection,
            @NonNull final Collection<T> elementsToAdd) {
        Preconditions.checkNotNull(collection);
        Preconditions.checkNotNull(elementsToAdd);

        collection.addAll(elementsToAdd);
        collection.removeAll(Collections.singleton(null));
    }
}
