// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.tests.BaseTesting;

import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.mopub.framework.models.AdLabels;
import com.mopub.simpleadsdemo.R;
import com.mopub.tests.base.MoPubBaseTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.mopub.framework.base.BasePage.pressBack;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class InterstitialAdTests extends MoPubBaseTestCase {

    // Test Variables
    private static final String TITLE = AdLabels.INTERSTITIAL;

    /*
     * Verify that the Interstitial Ad loads & shows on the app.
     */
    @Test
    public void adsDetailsPage_withClickOnLoadAdButtonAndThenShowAdButton_shouldLoadMoPubInterstitial() {
        onData(hasToString(startsWith(TITLE)))
                .inAdapterView(withId(android.R.id.list))
                .perform(click());

        adDetailPage.clickLoadAdButton();

        clickElement(withId(R.id.show_button));

        final ViewInteraction element = findView(withId(android.R.id.content));

        assertTrue(adDetailPage.waitForElement(element));
    }

    /*
     * Verify that the user is correctly navigated to MoPub browser.
     */
    @Test
    public void adsDetailsPage_withClickOnAd_shouldLoadMoPubBrowser() {
        onData(hasToString(startsWith(TITLE)))
                .inAdapterView(withId(android.R.id.list))
                .perform(click());

        adDetailPage.clickLoadAdButton();

        clickElement(withId(R.id.show_button));

        findView(withId(android.R.id.content)).perform(click());

        assertWebViewUrl(WEB_PAGE_LINK);
        pressBack();
    }
}
