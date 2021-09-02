// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.VectorDrawable;
import android.widget.ImageView;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.network.MoPubImageLoader;
import com.mopub.network.MoPubNetworkError;
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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
public class VastVideoCloseButtonWidgetTest {
    private Context context;
    private VastVideoCloseButtonWidget subject;

    private static final String ICON_IMAGE_URL = "iconimageurl";

    @Mock
    MoPubRequestQueue mockRequestQueue;
    @Mock
    private MoPubImageLoader mockImageLoader;
    @Mock
    private MoPubImageLoader.ImageContainer mockImageContainer;
    @Mock
    private Bitmap mockBitmap;
    @Captor
    private ArgumentCaptor<MoPubImageLoader.ImageListener> imageCaptor;

    @Before
    public void setUp() throws Exception {
        Networking.setRequestQueueForTesting(mockRequestQueue);
        Networking.setImageLoaderForTesting(mockImageLoader);
        context = Robolectric.buildActivity(Activity.class).create().get();
        subject = new VastVideoCloseButtonWidget(context, null);
    }

    @Test
    public void updateCloseButtonIcon_imageListenerOnResponse_shouldUseImageBitmap() {
        final ImageView imageView = new ImageView(context, null);
        imageView.setImageDrawable(context.getDrawable(R.drawable.ic_mopub_skip_button));
        final ImageView imageViewSpy = spy(imageView);
        subject.setImageView(imageViewSpy);

        when(mockImageContainer.getBitmap()).thenReturn(mockBitmap);

        subject.updateCloseButtonIcon(ICON_IMAGE_URL, context);

        verify(mockImageLoader).fetch(eq(ICON_IMAGE_URL), imageCaptor.capture(), anyInt(), anyInt(),
                any(ImageView.ScaleType.class));
        MoPubImageLoader.ImageListener listener = imageCaptor.getValue();
        listener.onResponse(mockImageContainer, true);
        assertThat(((BitmapDrawable) subject.getImageView().getDrawable()).getBitmap()).isEqualTo(mockBitmap);
    }

    @Test
    public void updateImage_imageListenerOnResponseWhenReturnedBitMapIsNull_shouldUseDefaultCloseButtonDrawable() {
        final ImageView imageView = new ImageView(context, null);
        imageView.setImageDrawable(context.getDrawable(R.drawable.ic_mopub_skip_button));
        final ImageView imageViewSpy = spy(imageView);
        subject.setImageView(imageViewSpy);

        when(mockImageContainer.getBitmap()).thenReturn(null);

        subject.updateCloseButtonIcon(ICON_IMAGE_URL, context);

        verify(mockImageLoader).fetch(eq(ICON_IMAGE_URL), imageCaptor.capture(), anyInt(), anyInt(),
                any(ImageView.ScaleType.class));
        MoPubImageLoader.ImageListener listener = imageCaptor.getValue();
        listener.onResponse(mockImageContainer, true);
        verify(imageViewSpy, never()).setImageBitmap(any(Bitmap.class));
        assertThat(subject.getImageView().getDrawable()).isInstanceOf(VectorDrawable.class);
    }

    @Test
    public void updateImage_imageListenerOnErrorResponse_shouldUseDefaultCloseButtonDrawable() {
        final ImageView imageView = new ImageView(context, null);
        imageView.setImageDrawable(context.getDrawable(R.drawable.ic_mopub_skip_button));
        final ImageView imageViewSpy = spy(imageView);
        subject.setImageView(imageViewSpy);

        subject.updateCloseButtonIcon(ICON_IMAGE_URL, context);

        verify(mockImageLoader).fetch(eq(ICON_IMAGE_URL), imageCaptor.capture(), anyInt(), anyInt(),
                any(ImageView.ScaleType.class));
        MoPubImageLoader.ImageListener listener = imageCaptor.getValue();
        listener.onErrorResponse(new MoPubNetworkError.Builder().build());
        verify(imageViewSpy, never()).setImageBitmap(any(Bitmap.class));
        assertThat(subject.getImageView().getDrawable()).isInstanceOf(VectorDrawable.class);
    }
}
