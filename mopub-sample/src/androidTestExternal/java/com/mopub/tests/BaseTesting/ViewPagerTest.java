// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.tests.BaseTesting;

import androidx.test.espresso.DataInteraction;
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
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.mopub.framework.base.BasePage.pressBack;
import static com.mopub.framework.util.Actions.loopMainThreadUntilIdle;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ViewPagerTest extends MoPubBaseTestCase {

    // Test Variables
    private static final String TITLE = AdLabels.VIEW_PAGER;

    /*
     * Verify that the View Pager Ad loads & shows on the app.
     */
    @Test
    public void adsDetailsPage_withClickOnLoadAdButton_shouldLoadMoPubViewPager() {
        onData(hasToString(startsWith(TITLE)))
                .inAdapterView(withId(android.R.id.list))
                .perform(click());

        ViewInteraction galleryPager = findView(withId(R.id.gallery_pager));

        galleryPager.perform(swipeLeft()); //swipe left to bring ad to view
        galleryPager.perform(swipeLeft()); //swipe left to bring ad to view

        ViewInteraction element = findView(withId(R.id.native_main_image));

        assertTrue(adDetailPage.waitForElement(element));
    }

    /*
     * Verify that the user is correctly navigated to MoPub browser.
     */
    @Test
    public void adsDetailsPage_withClickOnAd_shouldLoadMoPubViewBrowser()  {
        onData(hasToString(startsWith(TITLE)))
                .inAdapterView(withId(android.R.id.list))
                .perform(click());

        ViewInteraction galleryPager = findView(withId(R.id.gallery_pager));

        galleryPager.perform(swipeLeft()); //swipe left to bring ad to view
        galleryPager.perform(swipeLeft()); //swipe left to bring ad to view

        clickElement(withId(R.id.native_main_image));

        assertWebViewUrl(WEB_PAGE_LINK);
        pressBack();
    }
}
