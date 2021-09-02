// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.tests.ReleaseTesting;

import android.widget.ImageView;

import androidx.media2.widget.VideoView;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.mopub.common.CloseableLayout;
import com.mopub.framework.models.AdLabels;
import com.mopub.framework.util.BlurLastFrameMatcher;
import com.mopub.framework.util.Utils;
import com.mopub.mobileads.RadialCountdownWidget;
import com.mopub.mobileads.VastVideoCloseButtonWidget;
import com.mopub.mobileads.VastVideoProgressBarWidget;
import com.mopub.mobileads.VideoCtaButtonWidget;
import com.mopub.mraid.MraidBridge;
import com.mopub.tests.base.MoPubBaseTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.mopub.framework.base.BasePage.clickCellOnList;
import static com.mopub.framework.base.BasePage.pressBack;
import static com.mopub.framework.util.Actions.assertWebView;
import static com.mopub.framework.util.Actions.findViewSafe;
import static com.mopub.framework.util.ClassNameMatcher.withCanonicalClassName;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ReleaseRewardedTest extends MoPubBaseTestCase {
    private static final String CLICK_URL = "https://www.mopub.com/en";
    private static final String COMPANION_CLICK_URL = "https://www.mopub.com/en?q=companionClickThrough";
    private static final int VIDEO_LENGTH = 30000;
    private static final long SHORT_DELAY = 1000;
    private static final long LONG_DELAY = 3000;
    private static final String MRAID_CLASS_NAME = MraidBridge.MraidWebView.class.getCanonicalName();
    private static final String IMAGEVIEW_CLASS_NAME = ImageView.class.getCanonicalName();
    private static final String CTA_BUTTON_CLASS_NAME = VideoCtaButtonWidget.class.getCanonicalName();
    private static final String PROGRESS_BAR_CLASS_NAME = VastVideoProgressBarWidget.class.getCanonicalName();
    private static final String RADIAL_COUNTDOWN_CLASS_NAME = RadialCountdownWidget.class.getCanonicalName();
    private static final String VIDEO_VIEW_CLASS_NAME = VideoView.class.getCanonicalName();
    private static final String CLOSE_BUTTON_CLASS_NAME = VastVideoCloseButtonWidget.class.getCanonicalName();
    private static final String CLOSEABLE_LAYOUT_CLASS_NAME = CloseableLayout.class.getCanonicalName();

/*
    @Test
    public void mraidEndCard_shouldShowEndCard() {
        clickCellOnList(RewardedTestAdUnits.REWARDED_MRAID_END_CARD.getAdName());
        adDetailPage.pressLoadAdButton();
        adDetailPage.selectReward("1 Coins");
        adDetailPage.pressShowAdButton();

        // video playing validation
        assertTrue(findViewSafe(allOf(withCanonicalClassName(RADIAL_COUNTDOWN_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(VIDEO_VIEW_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(PROGRESS_BAR_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CTA_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CLOSE_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.GONE))));

        Utils.waitFor(VIDEO_LENGTH); // wait for video to finish

        // MRAID end card validation
        assertTrue(findViewSafe(allOf(withCanonicalClassName(MRAID_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE)), LONG_DELAY));
    }
*/
    @Test
    public void htmlEndCard_shouldShowEndCard_shouldClickthrough() {
        clickCellOnList(RewardedTestAdUnits.REWARDED_HTML_END_CARD.getAdName());
        adDetailPage.pressLoadAdButton();
        adDetailPage.pressShowAdButton();

        // video playing validation
        assertTrue(findViewSafe(allOf(withCanonicalClassName(RADIAL_COUNTDOWN_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(VIDEO_VIEW_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(PROGRESS_BAR_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CTA_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CLOSE_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.GONE))));

        Utils.waitFor(VIDEO_LENGTH);

        // HTML end card validation
        assertTrue(findViewSafe(allOf(withCanonicalClassName(MRAID_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE)), LONG_DELAY));

        adDetailPage.clickElement(onView(allOf(withId(android.R.id.content), hasChildCount(1))));
        assertWebView(WEB_PAGE_LINK);

        pressBack();// return to the ad after back button
        assertTrue(findViewSafe(allOf(withCanonicalClassName(MRAID_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE)), LONG_DELAY));
    }

/*
    @Test
    public void mraidEndCardFunction_shouldShowEndCard_shouldClickthrough() {
        clickCellOnList(RewardedTestAdUnits.REWARDED_MRAID_END_CARD_FUNCTION.getAdName());
        adDetailPage.pressLoadAdButton();
        adDetailPage.selectReward("1 Coins");
        adDetailPage.pressShowAdButton();

        // video playing validation
        assertTrue(findViewSafe(allOf(withCanonicalClassName(RADIAL_COUNTDOWN_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(VIDEO_VIEW_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(PROGRESS_BAR_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CTA_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CLOSE_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.GONE))));

        Utils.waitFor(VIDEO_LENGTH);

        // MRAID end card validation
        assertTrue(findViewSafe(allOf(withCanonicalClassName(MRAID_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE)), LONG_DELAY));

        adDetailPage.clickElement(onView(allOf(withId(android.R.id.content), hasChildCount(1))));
        assertWebView(CLICK_URL);

        pressBack();// return to the ad after back button
        assertTrue(findViewSafe(allOf(withCanonicalClassName(MRAID_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE)), LONG_DELAY));
    }
*/
    @Test
    public void noEndCard_videoDurationLess_shouldNotShowEndCard_shouldClickthrough() {
        clickCellOnList(RewardedTestAdUnits.REWARDED_NO_END_CARD_DURATION_LESS.getAdName());
        adDetailPage.pressLoadAdButton();
        adDetailPage.selectReward("1 Coins");
        adDetailPage.pressShowAdButton();

        // video playing validation
        assertTrue(findViewSafe(allOf(withCanonicalClassName(RADIAL_COUNTDOWN_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(VIDEO_VIEW_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(PROGRESS_BAR_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CTA_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CLOSE_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.GONE))));

        Utils.waitFor(25000);

        // blur last frame validation
        assertFalse(findViewSafe(withCanonicalClassName(RADIAL_COUNTDOWN_CLASS_NAME)));
        assertFalse(findViewSafe(withCanonicalClassName(VIDEO_VIEW_CLASS_NAME)));
        assertFalse(findViewSafe(withCanonicalClassName(CLOSE_BUTTON_CLASS_NAME)));
        assertFalse(findViewSafe(withCanonicalClassName(MRAID_CLASS_NAME)));
        assertTrue(findViewSafe(new BlurLastFrameMatcher(), LONG_DELAY));

        adDetailPage.clickElement(onView(withCanonicalClassName(CTA_BUTTON_CLASS_NAME)));
        assertWebView(CLICK_URL);

        pressBack();// return to the ad after back button
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CTA_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE)), SHORT_DELAY));
    }

    @Test
    public void noEndCard_videoDurationEqual_shouldNotShowEndCard_shouldShowBlurFrame_shouldClickthrough() {
        clickCellOnList(RewardedTestAdUnits.REWARDED_NO_END_CARD_DURATION_EQUAL.getAdName());
        adDetailPage.pressLoadAdButton();
        adDetailPage.pressShowAdButton();

        // video playing validation
        assertTrue(findViewSafe(allOf(withCanonicalClassName(RADIAL_COUNTDOWN_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(PROGRESS_BAR_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(VIDEO_VIEW_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CTA_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CLOSE_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.GONE))));

        Utils.waitFor(31000);

        assertTrue(findViewSafe(allOf(withCanonicalClassName(CTA_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE)), SHORT_DELAY));

        adDetailPage.clickElement(onView(withCanonicalClassName(CTA_BUTTON_CLASS_NAME)));
        assertWebView(CLICK_URL);

        pressBack();// return to the ad after back button
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CTA_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE)), SHORT_DELAY));
        assertTrue(findViewSafe(new BlurLastFrameMatcher(), LONG_DELAY));
    }

    @Test
    public void noEndCard_videoDurationMore_shouldNotShowEndCard_shouldShowBlurFrame_shouldClickthrough() {
        clickCellOnList(RewardedTestAdUnits.REWARDED_NO_END_CARD_DURATION_MORE.getAdName());
        adDetailPage.pressLoadAdButton();
        adDetailPage.selectReward("1 Coins");
        adDetailPage.pressShowAdButton();

        // video playing validation
        assertTrue(findViewSafe(allOf(withCanonicalClassName(RADIAL_COUNTDOWN_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(VIDEO_VIEW_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(PROGRESS_BAR_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CTA_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CLOSE_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.GONE))));

        Utils.waitFor(40000);

        adDetailPage.clickElement(onView(withCanonicalClassName(CTA_BUTTON_CLASS_NAME)));
        assertWebView(CLICK_URL);

        pressBack();// return to the ad after back button
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CTA_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE)), SHORT_DELAY));
        assertTrue(findViewSafe(new BlurLastFrameMatcher(), LONG_DELAY));
    }

    @Test
    public void noEndCard_videoDurationMore_shouldClickthrough_beforeVideoComplete() {
        clickCellOnList(RewardedTestAdUnits.REWARDED_NO_END_CARD_DURATION_MORE.getAdName());
        adDetailPage.pressLoadAdButton();
        adDetailPage.selectReward("1 Coins");
        adDetailPage.pressShowAdButton();

        // video playing validation
        assertTrue(findViewSafe(allOf(withCanonicalClassName(RADIAL_COUNTDOWN_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(VIDEO_VIEW_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(PROGRESS_BAR_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CTA_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CLOSE_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.GONE))));
        assertFalse(findViewSafe(withCanonicalClassName(MRAID_CLASS_NAME)));

        Utils.waitFor(10000);

        adDetailPage.clickElement(onView(withId(android.R.id.content)));
        assertWebView(CLICK_URL);
        pressBack();
    }

    @Test
    public void imageEndCard_videoDurationLess_shouldShowEndCard() {
        clickCellOnList(RewardedTestAdUnits.REWARDED_END_CARD_DURATION_LESS.getAdName());
        adDetailPage.pressLoadAdButton();
        adDetailPage.selectReward("1 Coins");
        adDetailPage.pressShowAdButton();

        // video playing validation
        assertTrue(findViewSafe(allOf(withCanonicalClassName(RADIAL_COUNTDOWN_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(VIDEO_VIEW_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(PROGRESS_BAR_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CTA_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CLOSE_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.GONE))));

        assertFalse(findViewSafe(allOf(withCanonicalClassName(IMAGEVIEW_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE)), SHORT_DELAY));

        Utils.waitFor(15000);

        assertTrue(findViewSafe(allOf(withCanonicalClassName(IMAGEVIEW_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE)), LONG_DELAY));
    }

    @Test
    public void imageEndCard_videoDurationEqual_shouldShowEndCard() {
        clickCellOnList(RewardedTestAdUnits.REWARDED_END_CARD_DURATION_EQUAL.getAdName());
        adDetailPage.pressLoadAdButton();
        adDetailPage.selectReward("1 Coins");
        adDetailPage.pressShowAdButton();

        // video playing validation
        assertTrue(findViewSafe(allOf(withCanonicalClassName(RADIAL_COUNTDOWN_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(VIDEO_VIEW_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(PROGRESS_BAR_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CTA_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CLOSE_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.GONE))));

        assertFalse(findViewSafe(allOf(withCanonicalClassName(IMAGEVIEW_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE)), SHORT_DELAY));

        Utils.waitFor(25000);
        assertTrue(findViewSafe(allOf(withCanonicalClassName(IMAGEVIEW_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE)), LONG_DELAY));
    }

    @Test
    public void imageEndCard_videoDurationMore_shouldShowEndCard_shouldClickThrough() {
        clickCellOnList(RewardedTestAdUnits.REWARDED_END_CARD_DURATION_MORE.getAdName());
        adDetailPage.pressLoadAdButton();
        adDetailPage.selectReward("1 Coins");
        adDetailPage.pressShowAdButton();

        // video playing validation
        assertTrue(findViewSafe(allOf(withCanonicalClassName(RADIAL_COUNTDOWN_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(VIDEO_VIEW_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(PROGRESS_BAR_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CTA_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CLOSE_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.GONE))));

        Utils.waitFor(28000);
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CTA_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE)), SHORT_DELAY));
        adDetailPage.clickElement(onView(withCanonicalClassName(CTA_BUTTON_CLASS_NAME)));
        assertWebView(CLICK_URL);
        pressBack();

        Utils.waitFor(10000);
        assertTrue(findViewSafe(allOf(withCanonicalClassName(IMAGEVIEW_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE)), LONG_DELAY));

        adDetailPage.clickElement(onView(allOf(withId(android.R.id.content), hasChildCount(1))));
        assertWebView(COMPANION_CLICK_URL);
        pressBack();
    }

    @Test(expected = PerformException.class)
    public void noEndCard_shouldShowBlurFrame_noClickthrough() {
        clickCellOnList(RewardedTestAdUnits.REWARDED_NO_END_CARD_NO_CLICKTHROUGH.getAdName());
        adDetailPage.pressLoadAdButton();
        adDetailPage.selectReward("1 Coins");
        adDetailPage.pressShowAdButton();

        // video playing validation
        assertTrue(findViewSafe(allOf(withCanonicalClassName(RADIAL_COUNTDOWN_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(VIDEO_VIEW_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(PROGRESS_BAR_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CTA_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.GONE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CLOSE_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.GONE))));
        assertFalse(findViewSafe(withCanonicalClassName(MRAID_CLASS_NAME)));

        Utils.waitFor(VIDEO_LENGTH);

        assertTrue(findViewSafe(new BlurLastFrameMatcher(), LONG_DELAY));

        adDetailPage.clickElement(onView(new BlurLastFrameMatcher()));
        assertWebView(CLICK_URL);
        pressBack();
    }

    @Test(expected = PerformException.class)
    public void imageEndCard_noClickthrough() {
        clickCellOnList(RewardedTestAdUnits.REWARDED_END_CARD_NO_CLICKTHROUGH.getAdName());
        adDetailPage.pressLoadAdButton();
        adDetailPage.selectReward("1 Coins");
        adDetailPage.pressShowAdButton();

        // video playing validation
        assertTrue(findViewSafe(allOf(withCanonicalClassName(RADIAL_COUNTDOWN_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(VIDEO_VIEW_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(PROGRESS_BAR_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CTA_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.GONE))));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(CLOSE_BUTTON_CLASS_NAME), withEffectiveVisibility(Visibility.GONE))));
        assertFalse(findViewSafe(withCanonicalClassName(MRAID_CLASS_NAME)));

        Utils.waitFor(VIDEO_LENGTH);

        assertTrue(findViewSafe(withCanonicalClassName(CLOSEABLE_LAYOUT_CLASS_NAME)));
        assertTrue(findViewSafe(allOf(withCanonicalClassName(IMAGEVIEW_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));

        adDetailPage.clickElement(onView(allOf(withCanonicalClassName(IMAGEVIEW_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE))));
        assertWebView(CLICK_URL);
        pressBack();
    }

    @Test
    public void htmlVideo_shouldReturnToVideoAfterClick() {
        clickCellOnList(RewardedTestAdUnits.REWARDED_HTML_VIDEO.getAdName());
        adDetailPage.pressLoadAdButton();
        adDetailPage.selectReward("1 Coins");
        adDetailPage.pressShowAdButton();

        assertTrue(findViewSafe(allOf(withCanonicalClassName(MRAID_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE)), SHORT_DELAY));

        // in the middle of the video
        Utils.waitFor(VIDEO_LENGTH / 2);
        adDetailPage.clickElement(onView(allOf(withId(android.R.id.content), hasChildCount(1))));
        assertWebView(CLICK_URL);
        pressBack();
        assertTrue(findViewSafe(allOf(withCanonicalClassName(MRAID_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE)), SHORT_DELAY));

        // after complete
        Utils.waitFor(VIDEO_LENGTH / 2);
        adDetailPage.clickElement(onView(allOf(withId(android.R.id.content), hasChildCount(1))));
        assertWebView(CLICK_URL);
        pressBack();
        assertTrue(findViewSafe(allOf(withCanonicalClassName(MRAID_CLASS_NAME), withEffectiveVisibility(Visibility.VISIBLE)), SHORT_DELAY));
    }

    private enum RewardedTestAdUnits {
        REWARDED_MRAID_END_CARD(AdLabels.REWARDED_MRAID_END_CARD),
        REWARDED_HTML_END_CARD(AdLabels.REWARDED_HTML_END_CARD),
        REWARDED_MRAID_END_CARD_FUNCTION(AdLabels.REWARDED_MRAID_END_CARD_FUNCTION),
        REWARDED_NO_END_CARD_DURATION_LESS(AdLabels.REWARDED_NO_END_CARD_DURATION_LESS),
        REWARDED_NO_END_CARD_DURATION_EQUAL(AdLabels.REWARDED_NO_END_CARD_DURATION_EQUAL),
        REWARDED_NO_END_CARD_DURATION_MORE(AdLabels.REWARDED_NO_END_CARD_DURATION_MORE),
        REWARDED_END_CARD_DURATION_LESS(AdLabels.REWARDED_END_CARD_DURATION_LESS),
        REWARDED_END_CARD_DURATION_EQUAL(AdLabels.REWARDED_END_CARD_DURATION_EQUAL),
        REWARDED_END_CARD_DURATION_MORE(AdLabels.REWARDED_END_CARD_DURATION_MORE),
        REWARDED_NO_END_CARD_NO_CLICKTHROUGH(AdLabels.REWARDED_NO_END_CARD_NO_CLICKTHROUGH),
        REWARDED_END_CARD_NO_CLICKTHROUGH(AdLabels.REWARDED_END_CARD_NO_CLICKTHROUGH),
        REWARDED_STATIC_IMAGE(AdLabels.REWARDED_STATIC_IMAGE),
        REWARDED_HTML_VIDEO(AdLabels.REWARDED_HTML_VIDEO);

        private final String label;

        RewardedTestAdUnits(final String adType) {
            this.label = adType;
        }

        public String getAdName() {
            return label;
        }
    }
}
