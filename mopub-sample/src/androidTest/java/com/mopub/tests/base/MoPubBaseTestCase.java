// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.tests.base;

import android.Manifest;
import android.view.View;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.espresso.web.sugar.Web;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import com.mopub.framework.pages.AdDetailPage;
import com.mopub.framework.util.Actions;
import com.mopub.framework.util.Utils;
import com.mopub.simpleadsdemo.BuildConfig;
import com.mopub.simpleadsdemo.MoPubSampleActivity;
import com.mopub.simpleadsdemo.R;
import com.mopub.test.rule.RetryRule;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static com.mopub.framework.util.Actions.assertWebView;
import static com.mopub.framework.util.Utils.waitForSdkToInitialize;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertTrue;

public class MoPubBaseTestCase {

    protected static final String WEB_PAGE_LINK = "https://www.mopub.com/en";
    private static final String RELEASE_WEB_PAGE_LINK = "https://www.mopub.com/en";
    private static final String CHROME_BROWSER_PACKAGE_NAME = "com.android.chrome";
    private static final String MRAID_CLICKTHROUGH_BUTTON = "//div[@id=\"expanded\"]/img[2]";
    private static final long CHROME_BROWSER_LAUNCH_TIMEOUT = 5000L;
    protected final int PORTRAIT_ORIENTATION = SCREEN_ORIENTATION_PORTRAIT;
    protected final int LANDSCAPE_ORIENTATION = SCREEN_ORIENTATION_LANDSCAPE;

    @Rule
    public IntentsTestRule<MoPubSampleActivity> mActivityRule =
            new IntentsTestRule<MoPubSampleActivity>(MoPubSampleActivity.class);

    @Rule
    public RetryRule mRetryRule = new RetryRule(BuildConfig.CONNECTED_TEST_RETRY_COUNT);

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
    );

    protected AdDetailPage adDetailPage;

    @Before
    public void setUp() {
        adDetailPage = new AdDetailPage();
        waitForSdkToInitialize();
    }

    @After
    public void tearDown() {
    }

    /**
     * Validates if it is horizontally centered on screen.
     */
    protected void isAlignedInLine() {
        final ViewInteraction bannerElement = onView(allOf(withId(R.id.banner_mopubview),
                hasChildCount(1)));
        bannerElement.check(matches(adDetailPage.isCenteredInParent()));
    }

    /**
     * Verify the navigation and loading of the given adUnit.
     */
    protected void isShownInFullscreen() {
        final ViewInteraction element = onView(withId(android.R.id.content));
        element.check(matches(adDetailPage.isInFullscreen()));
    }

    protected void inLineAdDidLoad() {
        final ViewInteraction bannerElement = onView(allOf(withId(R.id.banner_mopubview),
                hasChildCount(1)));
        assertTrue("Ad banner failed to load", adDetailPage.waitForElement(bannerElement));
    }

    /**
     * Asserts the webView turns to the orientation position using the MoPubSampleActivity rule
     *
     * @param orientation ActivityInfo orientation to be set
     */
    protected void checkChangeOnRotation(final int orientation) {
        final ViewInteraction mraidElement = onView(withId(android.R.id.content));
        adDetailPage.changeOrientationTo(orientation);
        mraidElement.check(matches((adDetailPage.didRotate(orientation))));
    }

    /**
     * Asserts the webView orientation matches expected when expanded
     *
     * @param orientation ActivityInfo orientation to be asserted
     */
    protected void checkMraidElementChangeOrientationOnExpand(int orientation) {
        Actions.loopMainThreadAtLeast(1000);
        findView(allOf(withId(android.R.id.content), adDetailPage.didRotate(orientation)));
    }

    /**
     * Verify clicking on the Ad shows the clickthrough url in the MoPubBrowser.
     *
     * @param id element to validate clickthrough
     */
    protected void hasClickthrough(final int id) {
        final ViewInteraction bannerElement = findView(allOf(withId(id), hasChildCount(1)));
        // Check for web page loads correctly
        adDetailPage.clickElement(bannerElement);
        assertWebViewUrl(RELEASE_WEB_PAGE_LINK);
    }

    /**
     * Verify clicking on the Ad shows the clickthrough url in the Chrome browser.
     */
    protected void opensChromeBrowserOnClick() {
        final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final ViewInteraction bannerElement = onView(allOf(withId(android.R.id.content),
                hasChildCount(1)));

        adDetailPage.clickElement(bannerElement);

        // Check that Chrome launches correctly
        final Boolean successfulBrowserLaunch = device.wait(Until.hasObject(By.pkg(CHROME_BROWSER_PACKAGE_NAME).depth(0)),
                CHROME_BROWSER_LAUNCH_TIMEOUT);

        assertTrue("Chrome browser did not launch", successfulBrowserLaunch);
    }

    /**
     * Asserts the Mraid clickthrough works and has the MoPub browser redirect
     */
    protected void hasMraidClickthrough() {
        final ViewInteraction bannerElement = findView(allOf(withId(R.id.banner_mopubview),
                hasChildCount(1)));

        adDetailPage.clickElement(bannerElement);
        Actions.loopMainThreadUntilIdle();

        Web.WebInteraction elem = onWebView()
                .withElement(findElement(Locator.XPATH, MRAID_CLICKTHROUGH_BUTTON));
        adDetailPage.clickWebElement(elem, "Could not click web element");
        elem.reset();
        Utils.waitFor(2000);

        assertWebView(RELEASE_WEB_PAGE_LINK);
    }

    protected ViewInteraction findView(Matcher<View> matcher) {
        return Actions.findView(matcher);
    }

    protected void assertWebViewUrl(final String url) {
        assertWebView(url);
    }

    protected void clickElement(Matcher<View> matcher) {
        Actions.clickElement(matcher);
    }
}
