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
public class ReleaseMediumRectangleTest extends MoPubBaseTestCase {

    @Test
    public void release_portraitMediumRectangleHtml_shouldLoadCenteredHorizontally_shouldShowMoPubBrowser() {
        clickCellOnList(MediumRectangleTestAdUnits.HTML.getAdName());
        adDetailPage.pressLoadAdButton();
        inLineAdDidLoad();
        adDetailPage.changeOrientationTo(PORTRAIT_ORIENTATION);
        isAlignedInLine();
        hasClickthrough(R.id.banner_mopubview);
        pressBack();
    }

    @Test
    public void release_landscapeMediumRectangleHtml_shouldLoadCenteredHorizontally_shouldShowMoPubBrowser() {
        clickCellOnList(MediumRectangleTestAdUnits.HTML.getAdName());
        adDetailPage.pressLoadAdButton();
        inLineAdDidLoad();
        adDetailPage.changeOrientationTo(LANDSCAPE_ORIENTATION);
        isAlignedInLine();
        hasClickthrough(R.id.banner_mopubview);
        pressBack();
    }

    @Test
    public void release_portraitMediumRectangleImage_shouldLoadCenteredHorizontally_shouldShowMoPubBrowser() {
        clickCellOnList(MediumRectangleTestAdUnits.IMAGE.getAdName());
        adDetailPage.pressLoadAdButton();
        inLineAdDidLoad();
        adDetailPage.changeOrientationTo(PORTRAIT_ORIENTATION);
        isAlignedInLine();
        hasClickthrough(R.id.banner_mopubview);
        pressBack();
    }

    @Test
    public void release_landscapeMediumRectangleImage_shouldLoadCenteredHorizontally_shouldShowMoPubBrowser() {
        clickCellOnList(MediumRectangleTestAdUnits.IMAGE.getAdName());
        adDetailPage.pressLoadAdButton();
        inLineAdDidLoad();
        adDetailPage.changeOrientationTo(LANDSCAPE_ORIENTATION);
        isAlignedInLine();
        hasClickthrough(R.id.banner_mopubview);
        pressBack();
    }

    @Test
    public void release_portraitMediumRectangleHtmlVideo_shouldLoadCenteredHorizontally_shouldShowMoPubBrowser() {
        clickCellOnList(MediumRectangleTestAdUnits.VIDEO.getAdName());
        adDetailPage.pressLoadAdButton();
        inLineAdDidLoad();
        adDetailPage.changeOrientationTo(PORTRAIT_ORIENTATION);
        isAlignedInLine();
        hasClickthrough(R.id.banner_mopubview);
        pressBack();
    }

    @Test
    public void release_landscapeMediumRectangleHtmlVideo_shouldLoadCenteredHorizontally_shouldShowMoPubBrowser() {
        clickCellOnList(MediumRectangleTestAdUnits.VIDEO.getAdName());
        adDetailPage.pressLoadAdButton();
        inLineAdDidLoad();
        adDetailPage.changeOrientationTo(LANDSCAPE_ORIENTATION);
        isAlignedInLine();
        hasClickthrough(R.id.banner_mopubview);
        pressBack();
    }

    private enum MediumRectangleTestAdUnits {
        HTML(AdLabels.MEDIUM_RECTANGLE_HTML),
        IMAGE(AdLabels.MEDIUM_RECTANGLE_IMAGE),
        VIDEO(AdLabels.MEDIUM_RECTANGLE_HTML_VIDEO);

        private final String label;

        MediumRectangleTestAdUnits(final String adType) {
            this.label = adType;
        }

        public String getAdName() {
            return label;
        }
    }
}
