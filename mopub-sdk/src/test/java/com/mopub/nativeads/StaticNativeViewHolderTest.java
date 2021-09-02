// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.network.MoPubImageLoader;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
public class StaticNativeViewHolderTest {
    private Context context;
    private RelativeLayout relativeLayout;
    private ViewBinder viewBinder;
    private TextView titleView;
    private TextView textView;
    private TextView callToActionView;
    private ImageView mainImageView;
    private ImageView iconImageView;
    private TextView extrasTextView;
    private ImageView extrasImageView;
    private ImageView extrasImageView2;
    private ImageView privacyInformationIconImageView;
    private TextView sponsoredTextView;

    @Mock private MoPubRequestQueue mockRequestQueue;
    @Mock private MoPubImageLoader mockImageLoader;
    @Mock private MoPubImageLoader.ImageContainer mockImageContainer;
    @Mock private Bitmap mockBitmap;

    @Captor private ArgumentCaptor<MoPubImageLoader.ImageListener> mainImageCaptor;
    @Captor private ArgumentCaptor<MoPubImageLoader.ImageListener> iconImageCaptor;

    @Before
    public void setUp() throws Exception {

        Networking.setRequestQueueForTesting(mockRequestQueue);
        Networking.setImageLoaderForTesting(mockImageLoader);
        context = Robolectric.buildActivity(Activity.class).create().get();
        relativeLayout = new RelativeLayout(context);
        relativeLayout.setId(View.generateViewId());

        // Fields in the web ui
        titleView = new TextView(context);
        titleView.setId(View.generateViewId());
        textView = new TextView(context);
        textView.setId(View.generateViewId());
        callToActionView = new Button(context);
        callToActionView.setId(View.generateViewId());
        mainImageView = new ImageView(context);
        mainImageView.setId(View.generateViewId());
        iconImageView = new ImageView(context);
        iconImageView.setId(View.generateViewId());
        privacyInformationIconImageView = new ImageView(context);
        privacyInformationIconImageView.setId(View.generateViewId());
        sponsoredTextView = new TextView(context);
        sponsoredTextView.setId(View.generateViewId());

        // Extras
        extrasTextView = new TextView(context);
        extrasTextView.setId(View.generateViewId());
        extrasImageView = new ImageView(context);
        extrasImageView.setId(View.generateViewId());
        extrasImageView2 = new ImageView(context);
        extrasImageView2.setId(View.generateViewId());

        relativeLayout.addView(titleView);
        relativeLayout.addView(textView);
        relativeLayout.addView(callToActionView);
        relativeLayout.addView(mainImageView);
        relativeLayout.addView(iconImageView);
        relativeLayout.addView(extrasTextView);
        relativeLayout.addView(extrasImageView);
        relativeLayout.addView(extrasImageView2);
        relativeLayout.addView(privacyInformationIconImageView);
        relativeLayout.addView(sponsoredTextView);
    }

    @Test
    public void fromViewBinder_shouldPopulateClassFields() throws Exception {
        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .titleId(titleView.getId())
                .textId(textView.getId())
                .callToActionId(callToActionView.getId())
                .mainImageId(mainImageView.getId())
                .iconImageId(iconImageView.getId())
                .privacyInformationIconImageId(privacyInformationIconImageView.getId())
                .sponsoredTextId(sponsoredTextView.getId())
                .build();

        StaticNativeViewHolder staticNativeViewHolder =
                StaticNativeViewHolder.fromViewBinder(relativeLayout, viewBinder);

        assertThat(staticNativeViewHolder.titleView).isEqualTo(titleView);
        assertThat(staticNativeViewHolder.textView).isEqualTo(textView);
        assertThat(staticNativeViewHolder.callToActionView).isEqualTo(callToActionView);
        assertThat(staticNativeViewHolder.mainImageView).isEqualTo(mainImageView);
        assertThat(staticNativeViewHolder.iconImageView).isEqualTo(iconImageView);
        assertThat(staticNativeViewHolder.privacyInformationIconImageView).isEqualTo(
                privacyInformationIconImageView);
        assertThat(staticNativeViewHolder.sponsoredTextView).isEqualTo(sponsoredTextView);
    }

    @Test
    public void fromViewBinder_withSubsetOfFields_shouldLeaveOtherFieldsNull() throws Exception {
        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .titleId(titleView.getId())
                .iconImageId(iconImageView.getId())
                .build();

        StaticNativeViewHolder staticNativeViewHolder =
                StaticNativeViewHolder.fromViewBinder(relativeLayout, viewBinder);

        assertThat(staticNativeViewHolder.titleView).isEqualTo(titleView);
        assertThat(staticNativeViewHolder.textView).isNull();
        assertThat(staticNativeViewHolder.callToActionView).isNull();
        assertThat(staticNativeViewHolder.mainImageView).isNull();
        assertThat(staticNativeViewHolder.iconImageView).isEqualTo(iconImageView);
        assertThat(staticNativeViewHolder.privacyInformationIconImageView).isNull();
        assertThat(staticNativeViewHolder.sponsoredTextView).isNull();
    }

    @Test
    public void fromViewBinder_withNonExistantIds_shouldLeaveFieldsNull() throws Exception {
        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .titleId(View.generateViewId())
                .textId(View.generateViewId())
                .callToActionId(View.generateViewId())
                .mainImageId(View.generateViewId())
                .iconImageId(View.generateViewId())
                .privacyInformationIconImageId(View.generateViewId())
                .sponsoredTextId(View.generateViewId())
                .build();

        StaticNativeViewHolder staticNativeViewHolder =
                StaticNativeViewHolder.fromViewBinder(relativeLayout, viewBinder);

        assertThat(staticNativeViewHolder.titleView).isNull();
        assertThat(staticNativeViewHolder.textView).isNull();
        assertThat(staticNativeViewHolder.callToActionView).isNull();
        assertThat(staticNativeViewHolder.mainImageView).isNull();
        assertThat(staticNativeViewHolder.iconImageView).isNull();
        assertThat(staticNativeViewHolder.privacyInformationIconImageView).isNull();
        assertThat(staticNativeViewHolder.sponsoredTextView).isNull();
    }
}
