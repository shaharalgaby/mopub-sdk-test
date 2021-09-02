// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.factories

import android.content.Context
import android.widget.RelativeLayout
import androidx.media2.widget.VideoView
import com.mopub.mobileads.base.R

open class VideoViewFactory {

    companion object {
        var instance = VideoViewFactory()

        fun create(context: Context, layout: RelativeLayout?): VideoView {
            return instance.internalCreate(context, layout)
        }
    }

    open fun internalCreate(context: Context, layout: RelativeLayout?): VideoView {
        return when {
            layout != null -> layout.findViewById<VideoView>(R.id.mopub_vast_video_view)
            else -> VideoView(context)
        }
    }
}
