// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.framework.util;

import android.view.View;

import androidx.annotation.Nullable;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class ClassNameMatcher extends BaseMatcher<View> {
    private final String className;

    private ClassNameMatcher(@Nullable final String name) {
        this.className = name;
    }

    @Override
    public boolean matches(Object item) {
        if (item == null)
            return false;

        return item.getClass().getCanonicalName().equals(className);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Validate class name is = " + className);
    }

    public static ClassNameMatcher withCanonicalClassName(@Nullable final String canonicalClassName) {
        return new ClassNameMatcher(canonicalClassName);
    }
}
