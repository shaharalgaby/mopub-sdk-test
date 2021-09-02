// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.tests.BaseTesting;

import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.mopub.framework.models.AdLabels;
import com.mopub.simpleadsdemo.R;
import com.mopub.tests.base.MoPubBaseTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class RewardedRichMediaAdTest extends MoPubBaseTestCase {

    // Test Variables
    private static final String TITLE = AdLabels.REWARDED_RICH_MEDIA;

    /*
     * Verify that the Rewarded Rich Media Ad loads & shows on the app.
     */
    @Test
    public void adsDetailsPage_withClickOnLoadAdButtonAndThenShowAdButton_shouldLoadMoPubRewardedRichMedia() {
        onData(hasToString(startsWith(TITLE)))
                .inAdapterView(withId(android.R.id.list))
                .perform(click());

        findView(withId(R.id.load_button)).perform(click());
        adDetailPage.selectReward("10 Coins");

        ViewInteraction showButtonElement = findView(withId(R.id.show_button)); //show ad on click
        adDetailPage.clickElement(showButtonElement);

        final ViewInteraction element = findView(withId(android.R.id.content));

        assertTrue(adDetailPage.waitForElement(element));
    }

}
