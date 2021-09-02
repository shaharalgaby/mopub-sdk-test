// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.view.WindowManager

import kotlin.math.min

object ImageUtils {
    /**
     * Fast Gaussian blurring algorithm source:
     * https://github.com/patrickfav/BlurTestAndroid/blob/master/BlurBenchmark/src/main/java/at/favre/app/blurbenchmark/blur/algorithms/GaussianFastBlur.java
     *
     */
    @JvmStatic
    fun applyFastGaussianBlurToBitmap(mutableBitmap: Bitmap, radius: Int): Bitmap {
        val w = mutableBitmap.width
        val h = mutableBitmap.height
        val pixels = IntArray(w * h)
        mutableBitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        var r = radius
        while (r >= 1) {
            for (i in r until h - r) {
                for (j in r until w - r) {
                    val tl = pixels[(i - r) * w + j - r]
                    val tr = pixels[(i - r) * w + j + r]
                    val tc = pixels[(i - r) * w + j]
                    val bl = pixels[(i + r) * w + j - r]
                    val br = pixels[(i + r) * w + j + r]
                    val bc = pixels[(i + r) * w + j]
                    val cl = pixels[i * w + j - r]
                    val cr = pixels[i * w + j + r]
                    pixels[i * w + j] = -0x1000000 or (
                            (tl and 0xFF) + (tr and 0xFF) + (tc and 0xFF) + (bl and 0xFF) + (br and 0xFF) + (bc and 0xFF) + (cl and 0xFF) + (cr and 0xFF) shr 3 and 0xFF) or (
                            (tl and 0xFF00) + (tr and 0xFF00) + (tc and 0xFF00) + (bl and 0xFF00) + (br and 0xFF00) + (bc and 0xFF00) + (cl and 0xFF00) + (cr and 0xFF00) shr 3 and 0xFF00) or (
                            (tl and 0xFF0000) + (tr and 0xFF0000) + (tc and 0xFF0000) + (bl and 0xFF0000) + (br and 0xFF0000) + (bc and 0xFF0000) + (cl and 0xFF0000) + (cr and 0xFF0000) shr 3 and 0xFF0000)
                }
            }
            r /= 2
        }

        mutableBitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return mutableBitmap
    }

    @JvmStatic
    fun getMaxImageWidth(context: Context): Int {
        // Get Display Options
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getSize(size)

        // Make our images no wider than the skinny side of the display.
        return min(size.x, size.y)
    }
}
