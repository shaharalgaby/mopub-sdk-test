// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.util;

import androidx.annotation.Nullable;
import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Strings {

    public static String fromStream(InputStream inputStream) throws IOException {
        int numberBytesRead = 0;
        StringBuilder out = new StringBuilder();
        byte[] bytes = new byte[4096];

        while (numberBytesRead != -1) {
            out.append(new String(bytes, 0, numberBytesRead));
            numberBytesRead = inputStream.read(bytes);
        }

        inputStream.close();

        return out.toString();
    }

    /**
     * This helper method creates a delimited String from values in a List
     *
     * @param object The List of objects. If this is null, then an empty String is returned.
     * @param delimiter The String to be used as a delimiter. If this is null, then ", " will be used.
     *
     * @return A delimited String of all values in the list.
     */
    @SuppressWarnings("unchecked")
    public static String getDelimitedString(@Nullable final Object object, @Nullable final String delimiter) {
        if (!(object instanceof List<?>)) {
            return "";
        }
        final List<Object> list = (List<Object>) object;

        if (list.isEmpty()) {
            return "";
        }

        return TextUtils.join((delimiter != null ? delimiter : ", ") , list);
    }
}
