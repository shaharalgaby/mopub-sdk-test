// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.framework.pages;

import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.test.espresso.ViewInteraction;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.withResourceName;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import com.mopub.framework.base.BasePage;


public class AdListPage extends BasePage {

    static final String CELL_RESOURCE_NAME = "banner_description";

    public enum AdUnitType {
        BANNER("Banner"),
        MEDIUM_RECTANGLE("Medium Rectangle"),
        INTERSTITIAL("Interstitial"),
        REWARDED_VIDEO("Rewarded Video"),
        NATIVE_LIST_VIEW("Native List View"),
        NATIVE_RECYCLER_VIEW("Native Recycler View"),
        NATIVE_GALLERY("Native Gallery (Custom Stream)");

        private final String type;

        private AdUnitType(@NonNull final String type) {
            this.type = type;
        }

        public String getName() {
            return type;
        }
    }

    public AdDetailPage clickCell(@NonNull final String title) {
        final String failMessage = "This element with resource name '" + CELL_RESOURCE_NAME
                + "' and title '" + title + "' is not present";
        final ViewInteraction element = onView(allOf(withText(title),
                withResourceName(CELL_RESOURCE_NAME)));
        clickElement(element, failMessage);

        return new AdDetailPage();
    }

    public AdListPage addAdUnit(@NonNull final AdUnitType type, @NonNull final String adUnitId, @NonNull final String adUnitName) {
        final String saveAdUnitLabel = "Save ad unit";
        final String adTypeSpinnerResourceId = "add_ad_unit_type";
        final String adUnitIdTextFieldResourceId = "add_ad_unit_id";
        final String adUnitNameTextFieldResourceId = "add_ad_unit_description";

        goToHome();

        clickElementWithText(ADD_AD_UNIT_LABEL);

        final ViewInteraction adTypeSpinner = onView(withResourceName(adTypeSpinnerResourceId));
        adTypeSpinner.perform(click());

        final ViewInteraction adTypeOptionElement = onView(withText(type.getName()))
                .inRoot(isPlatformPopup());
        adTypeOptionElement.perform(click());

        final ViewInteraction adUnitIdTextField = onView(withResourceName(adUnitIdTextFieldResourceId));
        adUnitIdTextField.perform(replaceText(adUnitId), closeSoftKeyboard());

        final ViewInteraction adUnitNameTextField = onView(withResourceName(adUnitNameTextFieldResourceId));
        adUnitNameTextField.perform(replaceText(adUnitName), closeSoftKeyboard());
        SystemClock.sleep(2000); // wait for keyboard to be closed

        clickElementWithText(saveAdUnitLabel);

        return this;
    }

    public AdListPage deleteAdUnit(@NonNull final String adUnitName) {
        final String deleteButtonLabel = "Delete";

        goToHome();

        final ViewInteraction bannerDeleteElement = onView(allOf(withResourceName("banner_delete"),
                hasSibling(withText(adUnitName))));
        quickClickElement(bannerDeleteElement);

        clickElementWithText(deleteButtonLabel);

        return this;
    }
}
