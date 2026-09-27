package com.moeketsifaso.ntsamaise

import java.util.Locale
import kotlin.math.abs

internal data class FrameGeometry(val width: Int, val height: Int, val rotation: Int)
internal data class ObstacleObservation(val bounds: DetectedObject, val geometry: FrameGeometry)
internal data class DistanceCalibration(val reference: ObstacleObservation, val meters: Float)

/** Similar triangles for the SAME object, pose, camera and field of view; not depth sensing. */
internal fun estimateDistance(calibration: DistanceCalibration, current: ObstacleObservation): Float? {
    val reference = calibration.reference
    if (!calibration.meters.isFinite() || calibration.meters !in 0.3f..10f ||
        reference.geometry != current.geometry || reference.bounds.label != current.bounds.label ||
        !usableForDistance(reference) || !usableForDistance(current)) return null
    val meters = calibration.meters *
        (reference.bounds.bottom - reference.bounds.top) / (current.bounds.bottom - current.bounds.top)
    return meters.takeIf { it.isFinite() && it in 0.3f..10f }
}

internal fun usableForDistance(observation: ObstacleObservation): Boolean {
    val b = observation.bounds
    return observation.geometry.width > 0 && observation.geometry.height > 0 &&
        observation.geometry.rotation in listOf(0, 90, 180, 270) && b.label.isNotBlank() &&
        b.confidence.isFinite() && b.confidence in 0.65f..1f &&
        listOf(b.left, b.top, b.right, b.bottom).all { it.isFinite() && it in 0.02f..0.98f } &&
        b.right - b.left >= 0.05f && b.bottom - b.top >= 0.1f
}

/** Reject unstable boxes before calibration/reading. No object identity is inferred from a label. */
internal class DistanceObservationWindow {
    private val samples = ArrayDeque<Pair<Long, ObstacleObservation>>()

    fun clear() = samples.clear()

    fun add(observation: ObstacleObservation?, now: Long): ObstacleObservation? {
        if (observation == null || !usableForDistance(observation)) {
            clear()
            return null
        }
        val previous = samples.lastOrNull()
        if (previous != null && (now <= previous.first || now - previous.first > 1_000 ||
            previous.second.geometry != observation.geometry ||
            previous.second.bounds.label != observation.bounds.label ||
            abs(centerX(previous.second) - centerX(observation)) > 0.1f)) clear()
        samples.addLast(now to observation)
        while (samples.isNotEmpty() && now - samples.first().first > 1_500) samples.removeFirst()
        if (samples.size < 3 || now - samples.first().first < 600) return null
        val heights = samples.map { it.second.bounds.bottom - it.second.bounds.top }.sorted()
        val median = heights[heights.size / 2]
        if ((heights.last() - heights.first()) / median > 0.1f) return null
        // Return an actual observation nearest the median, retaining its frame geometry.
        return samples.minBy { abs((it.second.bounds.bottom - it.second.bounds.top) - median) }.second
    }

    private fun centerX(o: ObstacleObservation) = (o.bounds.left + o.bounds.right) / 2f
}

internal fun distanceText(meters: Float): String = String.format(Locale.US, "%.1f", meters)

/** Half-metre steps keep minor box jitter from continuously changing the speech key. */
internal fun distanceSpeech(direction: String, meters: Float): String {
    val rounded = (kotlin.math.round(meters * 2f) / 2f).coerceAtLeast(0.5f)
    return "$direction. Estimated distance ${distanceText(rounded)} meters"
}
