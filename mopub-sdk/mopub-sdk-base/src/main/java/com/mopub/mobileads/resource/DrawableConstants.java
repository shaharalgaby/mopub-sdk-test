// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.resource;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

public class DrawableConstants {

    public static final int TRANSPARENT_GRAY = 0x88000000;

    public static class ProgressBar {
        public static final int NUGGET_WIDTH_DIPS = 4;

        public static final int BACKGROUND_COLOR = Color.WHITE;
        public static final int BACKGROUND_ALPHA = 128;
        public static final Paint.Style BACKGROUND_STYLE = Paint.Style.FILL;

        public static final int PROGRESS_COLOR = Color.parseColor("#FFCC4D");
        public static final int PROGRESS_ALPHA = 255;
        public static final Paint.Style PROGRESS_STYLE = Paint.Style.FILL;
    }

    public static class RadialCountdown {
        public static final int CIRCLE_STROKE_WIDTH_DIPS = 3;
        public static final float START_ANGLE = -90f;

        public static final int BACKGROUND_COLOR = Color.BLACK;
        public static final Paint.Style BACKGROUND_STYLE = Paint.Style.FILL;

        public static final int PROGRESS_CIRCLE_COLOR = Color.WHITE;
        public static final int PROGRESS_CIRCLE_ALPHA = 128;
        public static final Paint.Style PROGRESS_CIRCLE_STYLE = Paint.Style.STROKE;

        public static final int PROGRESS_ARC_COLOR = Color.WHITE;
        public static final int PROGRESS_ARC_ALPHA = 255;
        public static final Paint.Style PROGRESS_ARC_STYLE = Paint.Style.STROKE;

        public static final int TEXT_COLOR = Color.WHITE;
        public static final Paint.Align TEXT_ALIGN = Paint.Align.CENTER;
    }

    public static class CtaButton {
        public static final int BACKGROUND_COLOR = Color.BLACK;
        public static final int BACKGROUND_ALPHA = 51;
        public static final Paint.Style BACKGROUND_STYLE = Paint.Style.FILL;

        public static final int OUTLINE_COLOR = Color.WHITE;
        public static final int OUTLINE_ALPHA = 51;
        public static final Paint.Style OUTLINE_STYLE = Paint.Style.STROKE;

        public static final String DEFAULT_CTA_TEXT = "Learn More";
        public static final Typeface TEXT_TYPEFACE = Typeface.create("Helvetica", Typeface.NORMAL);
        public static final int TEXT_COLOR = Color.WHITE;
        public static final Paint.Align TEXT_ALIGN = Paint.Align.CENTER;
    }

    public static class BlurredLastVideoFrame {
        public static final int ALPHA = 100;
    }

    public static class PrivacyInfoIcon {
        public static final int LEFT_MARGIN_DIPS = 12;
        public static final int TOP_MARGIN_DIPS = 12;
    }
}
