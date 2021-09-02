// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.framework.pages;

import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;

import com.mopub.framework.base.BasePage;
import com.mopub.framework.util.Actions;
import com.mopub.simpleadsdemo.R;

import static androidx.test.espresso.matcher.ViewMatchers.withText;

public class AdDetailPage extends BasePage {
    public void clickLoadAdButton() {
        clickElementWithId(R.id.load_button);
    }

    public void selectReward(final String rewardText) {
        ViewInteraction selectButton = Actions.findView(withText("Select"));
        clickElementWithText(rewardText);
        clickElement(selectButton);
    }
}
