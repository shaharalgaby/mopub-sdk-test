// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.framework.util;

import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.mopub.common.CloseableLayout;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class BlurLastFrameMatcher extends BaseMatcher<View> {
    @Override
    public boolean matches(Object item) {
        if (item instanceof CloseableLayout) {
            CloseableLayout container = (CloseableLayout) item;
            for (int i = 0 ; i < container.getChildCount(); i++) {
                if (container.getChildAt(i) instanceof RelativeLayout) {
                    final RelativeLayout relativeLayout = (RelativeLayout) container.getChildAt(i);

                    final View child1 = relativeLayout.getChildAt(0);
                    if (child1 instanceof ImageView) {
                        return true;
                    }

                    if (relativeLayout.getChildCount() > 1) {
                        final View child2 = relativeLayout.getChildAt(1);
                        return child2 instanceof ImageView;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Match object to the blur last video frame view structure");
    }
}
