package xyz.siwane.drawix.pro.data

object ImageDecodePolicy {
    fun sample(width: Int, height: Int, targetEdge: Int, maxPixels: Long): Int {
        require(width > 0 && height > 0 && targetEdge > 0 && maxPixels > 0)
        var sample = 1
        // Keep enough real pixels for the requested viewport; never upscale a tiny preview.
        while (maxOf(width, height).toLong() / (sample.toLong() * 2) >= targetEdge) sample *= 2
        while ((width.toLong() / sample) * (height.toLong() / sample) > maxPixels && sample < 32768) sample *= 2
        return sample
    }
    /** A cropped tile must retain sufficient pixels in BOTH dimensions. */
    fun previewSample(width: Int, height: Int, targetWidth: Int, targetHeight: Int, maxPixels: Long): Int {
        require(width > 0 && height > 0 && targetWidth > 0 && targetHeight > 0 && maxPixels > 0)
        var sample = 1
        while (width.toLong() / (sample.toLong() * 2) >= targetWidth &&
            height.toLong() / (sample.toLong() * 2) >= targetHeight) sample *= 2
        while ((width.toLong() / sample) * (height.toLong() / sample) > maxPixels && sample < 32768) sample *= 2
        return sample
    }
}
