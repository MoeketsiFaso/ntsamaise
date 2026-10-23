package com.moeketsifaso.ntsamaise

import kotlin.math.max
import kotlin.math.min

internal enum class ObjectColour(val label: String, val sesotho: String) {
    BLACK("black", "botšo"), WHITE("white", "bosoeu"), GREY("grey", "bohlooho"),
    RED("red", "bofubelu"), ORANGE("orange", "lamunu"), YELLOW("yellow", "bosehla"),
    GREEN("green", "botala"), BLUE("blue", "bolou"), PURPLE("purple", "pherese"),
    PINK("pink", "pinki"), BROWN("brown", "sootho")
}

/** Confidence is a heuristic agreement band, not a calibrated probability. */
internal data class ColourEstimate(val colour: ObjectColour, val lowConfidence: Boolean)

/** Coarse HSV buckets, not calibrated colour measurements. No Android dependency. */
internal fun classifyColour(argb: Int): ObjectColour {
    val r = ((argb ushr 16) and 255) / 255f
    val g = ((argb ushr 8) and 255) / 255f
    val b = (argb and 255) / 255f
    val high = max(r, max(g, b))
    val low = min(r, min(g, b))
    val delta = high - low
    val saturation = if (high == 0f) 0f else delta / high
    if (high < 0.18f) return ObjectColour.BLACK
    if (saturation < 0.18f) return if (high >= 0.82f) ObjectColour.WHITE else ObjectColour.GREY
    val hue = ((when (high) {
        r -> (g - b) / delta
        g -> (b - r) / delta + 2f
        else -> (r - g) / delta + 4f
    } * 60f) + 360f) % 360f
    return when {
        hue in 15f..50f && high < 0.70f -> ObjectColour.BROWN
        (hue < 15f || hue >= 330f) && saturation < 0.65f && high >= 0.70f -> ObjectColour.PINK
        hue < 15f || hue >= 345f -> ObjectColour.RED
        hue < 45f -> ObjectColour.ORANGE
        hue < 70f -> ObjectColour.YELLOW
        hue < 170f -> ObjectColour.GREEN
        hue < 255f -> ObjectColour.BLUE
        hue < 300f -> ObjectColour.PURPLE
        else -> ObjectColour.PINK
    }
}

/**
 * Sample at most 24x24 pixels from the central 60% of the selected upright box.
 * An inset reduces background contamination but is not object segmentation.
 * Return the leading colour even for mixtures; mark weak/clipped samples low confidence.
 * Reject only unusable regions. Pixel sampling
 * stays on the camera worker and never stores an image or sends it to a service.
 */
internal fun estimateObjectColour(
    width: Int, height: Int, bounds: DetectedObject, pixelAt: (Int, Int) -> Int
): ColourEstimate? {
    if (width <= 0 || height <= 0 ||
        !listOf(bounds.left, bounds.top, bounds.right, bounds.bottom)
            .all { it.isFinite() }) return null
    val clipped = listOf(bounds.left, bounds.top, bounds.right, bounds.bottom).any { it !in 0f..1f }
    val boxWidth = (bounds.right.coerceIn(0f, 1f) - bounds.left.coerceIn(0f, 1f)) * width
    val boxHeight = (bounds.bottom.coerceIn(0f, 1f) - bounds.top.coerceIn(0f, 1f)) * height
    if (boxWidth < 4f || boxHeight < 4f) return null
    val left = bounds.left.coerceIn(0f, 1f) * width + boxWidth * 0.2f
    val top = bounds.top.coerceIn(0f, 1f) * height + boxHeight * 0.2f
    val sampleWidth = boxWidth * 0.6f
    val sampleHeight = boxHeight * 0.6f
    val columns = min(24, sampleWidth.toInt())
    val rows = min(24, sampleHeight.toInt())
    val votes = IntArray(ObjectColour.entries.size)
    var count = 0
    for (row in 0 until rows) for (column in 0 until columns) {
        val x = (left + (column + 0.5f) * sampleWidth / columns).toInt().coerceIn(0, width - 1)
        val y = (top + (row + 0.5f) * sampleHeight / rows).toInt().coerceIn(0, height - 1)
        val pixel = pixelAt(x, y)
        if ((pixel ushr 24) < 128) continue
        votes[classifyColour(pixel).ordinal]++
        count++
    }
    if (count < 4 || count < columns * rows * 0.8f) return null
    val ranked = ObjectColour.entries.sortedByDescending { votes[it.ordinal] }
    val first = votes[ranked[0].ordinal]
    val second = votes[ranked[1].ordinal]
    return ColourEstimate(ranked[0], clipped || count < 36 ||
        first < count * 0.65f || first - second < count * 0.25f)
}

/** Immediate guesses, smoothed over five recent frames; reset on selection changes. */
internal class ColourObservationWindow {
    private var previous: DetectedObject? = null
    private var geometry: FrameGeometry? = null
    private var colour: ObjectColour? = null
    private var lastTime = 0L
    private val samples = ArrayDeque<Pair<Long, ColourEstimate>>()

    fun clear() {
        previous = null
        geometry = null
        colour = null
        samples.clear()
    }

    fun add(bounds: DetectedObject, frame: FrameGeometry, candidate: ColourEstimate?, now: Long): ColourEstimate? {
        if (candidate == null) {
            clear()
            return null
        }
        val old = previous
        if (old == null || old.label != bounds.label || geometry != frame ||
            now <= lastTime || now - lastTime > 1_200 || overlap(old, bounds) < 0.5f) {
            clear()
        }
        previous = bounds
        geometry = frame
        lastTime = now
        samples.addLast(now to candidate)
        while (samples.size > 5 || now - samples.first().first > 1_200) samples.removeFirst()
        val counts = samples.groupingBy { it.second.colour }.eachCount()
        val leaders = counts.filterValues { it == counts.values.max() }.keys
        // Retain the last guess on ties to avoid alternating speech keys.
        val winner = colour?.takeIf { it in leaders } ?: leaders.first()
        colour = winner
        return ColourEstimate(winner, samples.size < 3 || counts.size > 1 ||
            samples.any { it.second.lowConfidence })
    }

    private fun overlap(a: DetectedObject, b: DetectedObject): Float {
        val intersection = max(0f, min(a.right, b.right) - max(a.left, b.left)) *
            max(0f, min(a.bottom, b.bottom) - max(a.top, b.top))
        val union = (a.right - a.left) * (a.bottom - a.top) +
            (b.right - b.left) * (b.bottom - b.top) - intersection
        return if (union > 0f) intersection / union else 0f
    }
}

internal fun colouredObjectFeedback(obj: DetectedObject, colour: ColourEstimate?): DetectionFeedback {
    val detail = colour?.let { "Possible colour: ${it.colour.label}" } ?: "Colour unclear"
    val confidence = colour?.let { if (it.lowConfidence) "low" else "moderate" }
    val display = detail + (confidence?.let { " · $it colour confidence" } ?: "")
    // Confidence bands stay on screen so small fluctuations do not restart speech.
    return DetectionFeedback("${obj.label} ${(obj.confidence * 100).toInt()}% · $display",
        "${obj.label} detected. $detail")
}
