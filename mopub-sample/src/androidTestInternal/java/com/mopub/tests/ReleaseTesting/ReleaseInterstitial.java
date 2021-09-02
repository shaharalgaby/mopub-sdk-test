// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.tests.ReleaseTesting;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.mopub.framework.models.AdLabels;
import com.mopub.tests.base.MoPubBaseTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import static com.mopub.framework.base.BasePage.clickCellOnList;
import static com.mopub.framework.base.BasePage.pressBack;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ReleaseInterstitial extends MoPubBaseTestCase {

    @Test
    public void release_landscapeInterstitialHtml_shouldLoad_shouldShowMoPubBrowser() {
        clickCellOnList(InterstitialTestAdUnits.HTML_LANDSCAPE.getAdName());
        // Change orientation before loading Ad to get the right orientation
        adDetailPage.changeOrientationTo(LANDSCAPE_ORIENTATION);
        adDetailPage.pressLoadAdButton();
        adDetailPage.pressShowAdButton();// Click includes wait of 2 sec for UI update
        isShownInFullscreen();
        // uncomment when ad unit is fixed opensChromeBrowserOnClick();
    }

    @Test
    public void release_portraitInterstitialHtml_shouldLoad_shouldShowMoPubBrowser() {
        clickCellOnList(InterstitialTestAdUnits.HTML_PORTRAIT.getAdName());
        // Change orientation before loading Ad to get the right orientation
        adDetailPage.changeOrientationTo(PORTRAIT_ORIENTATION);
        adDetailPage.pressLoadAdButton();
        adDetailPage.pressShowAdButton();// Click includes wait of 2 sec for UI update
        isShownInFullscreen();
        // uncomment when ad unit is fixed opensChromeBrowserOnClick();
    }

    @Test
    public void release_portraitInterstitialImage_shouldLoad_shouldShowMoPubBrowser() {
        clickCellOnList(InterstitialTestAdUnits.IMAGE_PORTRAIT.getAdName());
        // Change orientation before loading Ad to get the right orientation
        adDetailPage.changeOrientationTo(PORTRAIT_ORIENTATION);
        adDetailPage.pressLoadAdButton();
        adDetailPage.pressShowAdButton();// Click includes wait of 2 sec for UI update
        isShownInFullscreen();
        hasClickthrough(android.R.id.content);
        pressBack();
    }

    //Trying the new thing
    @Test
    public void release_landscapeInterstitialImage_shouldLoad_shouldShowMoPubBrowser() {
        clickCellOnList(InterstitialTestAdUnits.IMAGE_LANDSCAPE.getAdName());
        // Change orientation before loading Ad to get the right orientation
        adDetailPage.changeOrientationTo(LANDSCAPE_ORIENTATION);
        adDetailPage.pressLoadAdButton();
        adDetailPage.pressShowAdButton();// Click includes wait of 2 sec for UI update
        isShownInFullscreen();
        hasClickthrough(android.R.id.content);
        pressBack();
    }

    @Test
    public void release_landscapeInterstitialVideo_shouldLoad_shouldShowMoPubBrowser() {
        clickCellOnList(InterstitialTestAdUnits.VIDEO_LANDSCAPE.getAdName());
        // Change orientation before loading Ad to get the right orientation
        adDetailPage.changeOrientationTo(LANDSCAPE_ORIENTATION);
        adDetailPage.pressLoadAdButton();
        adDetailPage.pressShowAdButton();// Click includes wait of 2 sec for UI update
        isShownInFullscreen();
        hasClickthrough(android.R.id.content);
        pressBack();
    }

    @Test
    public void release_portraitInterstitialVideo_shouldLoad_shouldShowMoPubBrowser() {
        clickCellOnList(InterstitialTestAdUnits.VIDEO_PORTRAIT.getAdName());
        // Change orientation before loading Ad to get the right orientation
        adDetailPage.changeOrientationTo(PORTRAIT_ORIENTATION);
        adDetailPage.pressLoadAdButton();
        adDetailPage.pressShowAdButton();// Click includes wait of 2 sec for UI update
        isShownInFullscreen();
        hasClickthrough(android.R.id.content);
        pressBack();
    }

    private enum InterstitialTestAdUnits {
        IMAGE_PORTRAIT(AdLabels.INTERSTITIAL_IMAGE_PORTRAIT),
        IMAGE_LANDSCAPE(AdLabels.INTERSTITIAL_IMAGE_LANDSCAPE),
        HTML_PORTRAIT(AdLabels.INTERSTITIAL_HTML_PORTRAIT),
        HTML_LANDSCAPE(AdLabels.INTERSTITIAL_HTML_LANDSCAPE),
        VIDEO_PORTRAIT(AdLabels.INTERSTITIAL_VIDEO_PORTRAIT),
        VIDEO_LANDSCAPE(AdLabels.INTERSTITIAL_VIDEO_LANDSCAPE);

        private final String label;

        InterstitialTestAdUnits(String adType) {
            this.label = adType;
        }

        public String getAdName() {
            return label;
        }
    }
}
