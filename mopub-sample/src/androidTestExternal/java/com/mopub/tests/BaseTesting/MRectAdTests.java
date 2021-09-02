// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.tests.BaseTesting;

import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mopub.framework.models.AdLabels;
import com.mopub.framework.pages.AdListPage;
import com.mopub.framework.pages.AdListPage.AdUnitType;
import com.mopub.simpleadsdemo.R;
import com.mopub.tests.base.MoPubBaseTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.mopub.framework.base.BasePage.pressBack;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertTrue;

import static com.mopub.framework.base.BasePage.clickCellOnList;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MRectAdTests extends MoPubBaseTestCase {

    // Test Variables
    private static final AdUnitType AD_TYPE = AdUnitType.MEDIUM_RECTANGLE;
    private static final String TITLE = AdLabels.MEDIUM_RECTANGLE;

    /*
     * Verify that the MEDIUM_RECTANGLE Ad is successfully loaded and displayed on
     * the app.
     */
    @Test
    public void adsDetailsPage_withClickOnMoPubMediumRectangleSample_shouldLoadMoPubMediumRectangle() {
        clickCellOnList(TITLE);

        final ViewInteraction bannerElement = findView(allOf(withId(R.id.banner_mopubview),
                hasChildCount(1)));

        assertTrue(adDetailPage.waitForElement(bannerElement));
    }

    /*
     * Verify that the MEDIUM_RECTANGLE Ad fails to load on the app.
     */
    @Test
    public void adsDetailsPage_withClickOnMoPubMediumRectangleSample_shouldNotLoadMoPubMediumRectangle() {
        final String fakeAdUnit = "abc";
        final String adUnitTitle = "MEDIUM_RECTANGLE Automation Test";

        final AdListPage adListPage = new AdListPage();
        adListPage.addAdUnit(AD_TYPE, fakeAdUnit, adUnitTitle);

        clickCellOnList(adUnitTitle);

        try {
            findView(withText("onBannerFailed"));
        } finally {
            // Clean Up
            adListPage.deleteAdUnit(adUnitTitle);
        }
    }

    /*
     * Verify that the user is correctly navigated to
     * MEDIUM_RECTANGLE Ad's url on click.
     */
    @Test
    public void adsDetailsPage_withClickOnMoPubMediumRectangleAd_shouldShowMoPubBrowser() {
        clickCellOnList(TITLE);

        final ViewInteraction bannerElement = findView(allOf(withId(R.id.banner_mopubview),
                hasChildCount(1)));
        adDetailPage.clickElement(bannerElement);

        assertWebViewUrl(WEB_PAGE_LINK);
        pressBack();
    }
}
