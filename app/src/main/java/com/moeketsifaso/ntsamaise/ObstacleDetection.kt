package com.moeketsifaso.ntsamaise

/** Bounds are fractions of the upright camera image, not the preview widget. */
internal data class DetectedObject(
    val label: String, val confidence: Float,
    val left: Float, val top: Float, val right: Float, val bottom: Float
)

internal enum class ObstacleDirection(val prompt: String) {
    LEFT("Possible obstacle on the left"),
    AHEAD("Possible obstacle ahead"),
    RIGHT("Possible obstacle on the right")
}

internal data class ObstacleCandidate(
    val label: String, val direction: ObstacleDirection, val area: Float,
    val bounds: DetectedObject
)

/**
 * Experimental image-space heuristic, NOT depth or traversability estimation.
 * All supported object classes may be obstacles. Tiny boxes are filtered; boxes
 * intersecting the middle third take priority, followed by image area.
 * Thresholds must be evaluated on representative devices and scenes.
 */
internal fun selectObstacle(objects: List<DetectedObject>): ObstacleCandidate? = objects
    .mapNotNull { obj ->
        if (obj.label.isBlank() || !obj.confidence.isFinite() || obj.confidence < 0.5f ||
            !listOf(obj.left, obj.top, obj.right, obj.bottom).all { it.isFinite() }) {
            return@mapNotNull null
        }
        val left = obj.left.coerceIn(0f, 1f)
        val right = obj.right.coerceIn(0f, 1f)
        val top = obj.top.coerceIn(0f, 1f)
        val bottom = obj.bottom.coerceIn(0f, 1f)
        val width = right - left
        val height = bottom - top
        if (width <= 0f || height <= 0f) return@mapNotNull null
        val area = width * height
        if (area < 0.04f) return@mapNotNull null
        val direction = when {
            right <= 1f / 3f -> ObstacleDirection.LEFT
            left >= 2f / 3f -> ObstacleDirection.RIGHT
            else -> ObstacleDirection.AHEAD
        }
        ObstacleCandidate(obj.label, direction, area, obj)
    }
    .sortedWith(compareByDescending<ObstacleCandidate> { it.direction == ObstacleDirection.AHEAD }
        .thenByDescending { it.area }.thenBy { it.direction.ordinal }.thenBy { it.label })
    .firstOrNull()

internal fun obstacleFeedback(
    objects: List<DetectedObject>, geometry: FrameGeometry? = null
): DetectionFeedback {
    val candidate = selectObstacle(objects)
    return if (candidate == null) {
        DetectionFeedback("No supported obstacle detected. This does not mean the path is clear.", "")
    } else {
        DetectionFeedback("${candidate.direction.prompt}: ${candidate.label}", candidate.direction.prompt,
            geometry?.let { ObstacleObservation(candidate.bounds, it) })
    }
}
