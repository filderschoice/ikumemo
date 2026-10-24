package com.ikumemo.mobile.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoSizingTest {
    @Test
    fun targetSizeKeepsAspectRatioWithin1600() {
        assertEquals(1600 to 1200, PhotoSizing.targetSize(4000, 3000, 1600))
        assertEquals(900 to 1600, PhotoSizing.targetSize(2700, 4800, 1600))
        assertEquals(1600 to 1600, PhotoSizing.targetSize(1600, 1600, 1600))
        assertEquals(640 to 480, PhotoSizing.targetSize(640, 480, 1600))
    }

    @Test
    fun sampleSizeKeepsAtLeastTargetResolution() {
        assertEquals(1, PhotoSizing.sampleSize(1600, 1200, 1600))
        assertEquals(1, PhotoSizing.sampleSize(3199, 2000, 1600))
        assertEquals(2, PhotoSizing.sampleSize(3200, 2400, 1600))
        assertEquals(4, PhotoSizing.sampleSize(8000, 6000, 1600))
    }
}
