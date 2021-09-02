// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.tests.ReleaseTesting;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.mopub.framework.models.AdLabels;
import com.mopub.simpleadsdemo.R;
import com.mopub.tests.base.MoPubBaseTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import static com.mopub.framework.base.BasePage.clickCellOnList;
import static com.mopub.framework.base.BasePage.pressBack;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ReleaseBannerTest extends MoPubBaseTestCase {

    @Test
    public void release_portraitBannerImage_shouldLoadBannerCenteredHorizontally_shouldShowMoPubBrowser() {
        clickCellOnList(BannerTestAdUnits.IMAGE.getAdName());
        adDetailPage.pressLoadAdButton();
        inLineAdDidLoad();
        adDetailPage.changeOrientationTo(PORTRAIT_ORIENTATION);
        isAlignedInLine();
        hasClickthrough(R.id.banner_mopubview);
        pressBack();
    }

    @Test
    public void release_landscapeBannerImage_shouldLoadBannerCenteredHorizontally_shouldShowMoPubBrowser() {
        clickCellOnList(BannerTestAdUnits.IMAGE.getAdName());
        adDetailPage.pressLoadAdButton();
        inLineAdDidLoad();
        adDetailPage.changeOrientationTo(LANDSCAPE_ORIENTATION);
        isAlignedInLine();
        hasClickthrough(R.id.banner_mopubview);
        pressBack();
    }

    @Test
    public void release_portraitBannerHTML_shouldLoadBannerCenteredHorizontally_shouldShowMoPubBrowser() {
        clickCellOnList(BannerTestAdUnits.HTML.getAdName());
        adDetailPage.pressLoadAdButton();
        inLineAdDidLoad();
        adDetailPage.changeOrientationTo(PORTRAIT_ORIENTATION);
        isAlignedInLine();
        hasClickthrough(R.id.banner_mopubview);
        pressBack();
    }

    @Test
    public void release_landscapeBannerHTML_shouldLoadBannerCenteredHorizontally_shouldShowMoPubBrowser() {
        clickCellOnList(BannerTestAdUnits.HTML.getAdName());
        adDetailPage.pressLoadAdButton();
        inLineAdDidLoad();
        adDetailPage.changeOrientationTo(LANDSCAPE_ORIENTATION);
        isAlignedInLine();
        hasClickthrough(R.id.banner_mopubview);
        pressBack();
    }

    @Test
    public void release_mraidDeviceOrientation_shouldExpand_shouldShowMoPubBrowser() {
        clickCellOnList(BannerTestAdUnits.MRAID_DEVICE.getAdName());
        adDetailPage.pressLoadAdButton();
        inLineAdDidLoad();
        isAlignedInLine();
        hasMraidClickthrough();
        pressBack();
        adDetailPage.changeOrientationTo(LANDSCAPE_ORIENTATION);
        checkChangeOnRotation(LANDSCAPE_ORIENTATION);
    }

    @Test
    public void release_mraidLandscapeOrientation_shouldExpand_shouldShowMoPubBrowser() {
        // Test specific setup to validate it auto rotates when Mraid expands
        adDetailPage.changeOrientationTo(PORTRAIT_ORIENTATION);
        clickCellOnList(BannerTestAdUnits.MRAID_LANDSCAPE.getAdName());
        adDetailPage.pressLoadAdButton();
        inLineAdDidLoad();
        isAlignedInLine();
        hasMraidClickthrough();
        pressBack();
        checkMraidElementChangeOrientationOnExpand(LANDSCAPE_ORIENTATION);
    }

    @Test
    public void release_mraidPortraitOrientation_shouldExpand_shouldShowMoPubBrowser() {
        // Test specific setup to validate it will auto rotates when Mraid expands
        adDetailPage.changeOrientationTo(LANDSCAPE_ORIENTATION);
        clickCellOnList(BannerTestAdUnits.MRAID_PORTRIAT.getAdName());
        adDetailPage.pressLoadAdButton();
        inLineAdDidLoad();
        isAlignedInLine();
        hasMraidClickthrough();
        pressBack();
        checkMraidElementChangeOrientationOnExpand(PORTRAIT_ORIENTATION);
    }


    private enum BannerTestAdUnits {
        HTML(AdLabels.BANNER_HTML),
        IMAGE(AdLabels.BANNER_IMAGE),
        MRAID_DEVICE(AdLabels.BANNER_MRAID_DEVICE),
        MRAID_LANDSCAPE(AdLabels.BANNER_MRAID_LANDSCAPE),
        MRAID_PORTRIAT(AdLabels.BANNER_MRAID_PORTRAIT);

        private final String label;

        BannerTestAdUnits(final String adType) {
            this.label = adType;
        }

        public String getAdName() {
            return label;
        }
    }
}
