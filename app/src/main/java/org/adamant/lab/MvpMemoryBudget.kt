package org.adamant.lab

/** Conservative MVP heap heuristic. It is a guard, not an OOM guarantee. */
object MvpMemoryBudget {
    const val MAX_MVP_PIXELS = 12_000_000L
    private const val ESTIMATED_BYTES_PER_PIXEL = 40L

    fun allowedPixels(maxMemoryBytes: Long): Long {
        require(maxMemoryBytes > 0L) { "maxMemoryBytes must be positive" }
        return minOf(MAX_MVP_PIXELS, (maxMemoryBytes / ESTIMATED_BYTES_PER_PIXEL).coerceAtLeast(1L))
    }

    /** BitmapFactory's inSampleSize is a power of two; return the smallest factor under the budget. */
    fun inSampleSize(width: Int, height: Int, allowedPixels: Long): Int {
        require(width > 0 && height > 0) { "Image dimensions must be positive" }
        require(allowedPixels > 0L) { "allowedPixels must be positive" }
        var sample = 1
        while (ceilDiv(width.toLong(), sample) * ceilDiv(height.toLong(), sample) > allowedPixels) {
            check(sample <= (1 shl 29)) { "Unable to find a safe sample size" }
            sample *= 2
        }
        return sample
    }

    private fun ceilDiv(value: Long, divisor: Int): Long = (value + divisor - 1L) / divisor
}
