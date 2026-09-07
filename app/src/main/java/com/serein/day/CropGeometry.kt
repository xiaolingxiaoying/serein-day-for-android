package com.serein.day

/** Normalized rectangle used by the image editor's preview and crop pipeline. */
data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/** The crop frame's normalized center, kept separate from Compose UI types for testability. */
data class CropCenter(val x: Float, val y: Float)

/** Returns whether a point in normalized preview coordinates is inside the crop frame. */
fun NormalizedRect.contains(x: Float, y: Float): Boolean =
    x in left..right && y in top..bottom

/**
 * Moves a crop frame while preserving its size and preventing it from leaving the visible image.
 */
fun moveCropCenter(
    imageBounds: NormalizedRect,
    crop: NormalizedRect,
    centerX: Float,
    centerY: Float,
    deltaX: Float,
    deltaY: Float
): CropCenter = CropCenter(
    x = (centerX + deltaX).coerceIn(
        imageBounds.left + crop.width / 2f,
        imageBounds.right - crop.width / 2f
    ),
    y = (centerY + deltaY).coerceIn(
        imageBounds.top + crop.height / 2f,
        imageBounds.bottom - crop.height / 2f
    )
)

fun fittedImageBounds(
    sourceWidth: Int,
    sourceHeight: Int,
    previewWidth: Int,
    previewHeight: Int
): NormalizedRect {
    if (sourceWidth <= 0 || sourceHeight <= 0 || previewWidth <= 0 || previewHeight <= 0) {
        return NormalizedRect(0f, 0f, 1f, 1f)
    }
    val scale = minOf(previewWidth.toFloat() / sourceWidth, previewHeight.toFloat() / sourceHeight)
    val width = sourceWidth * scale / previewWidth
    val height = sourceHeight * scale / previewHeight
    return NormalizedRect(
        left = (1f - width) / 2f,
        top = (1f - height) / 2f,
        right = (1f + width) / 2f,
        bottom = (1f + height) / 2f
    )
}

fun cropFrameFor(
    imageBounds: NormalizedRect,
    targetAspectRatio: Float,
    previewAspectRatio: Float = 1f,
    zoom: Float,
    centerX: Float,
    centerY: Float
): NormalizedRect {
    val previewAspect = imageBounds.width / imageBounds.height
    // Normalized x/y units have different physical sizes when the preview is not square.
    val safeAspect = (targetAspectRatio / previewAspectRatio.coerceAtLeast(0.05f)).coerceAtLeast(0.05f)
    val safeZoom = zoom.coerceIn(1f, 4f)
    val baseWidth: Float
    val baseHeight: Float
    if (previewAspect > safeAspect) {
        baseHeight = imageBounds.height
        baseWidth = baseHeight * safeAspect
    } else {
        baseWidth = imageBounds.width
        baseHeight = baseWidth / safeAspect
    }
    val width = baseWidth / safeZoom
    val height = baseHeight / safeZoom
    val cx = centerX.coerceIn(imageBounds.left + width / 2f, imageBounds.right - width / 2f)
    val cy = centerY.coerceIn(imageBounds.top + height / 2f, imageBounds.bottom - height / 2f)
    return NormalizedRect(cx - width / 2f, cy - height / 2f, cx + width / 2f, cy + height / 2f)
}

/**
 * Converts a crop frame from preview coordinates into source-image coordinates.
 *
 * The preview uses ContentScale.Fit, so its letterboxed area must be removed before applying
 * normalized coordinates to the source bitmap.
 */
fun previewSelectionToSource(
    selection: NormalizedRect,
    sourceWidth: Int,
    sourceHeight: Int,
    previewWidth: Int,
    previewHeight: Int
): NormalizedRect {
    val image = fittedImageBounds(sourceWidth, sourceHeight, previewWidth, previewHeight)
    return NormalizedRect(
        left = ((selection.left.coerceIn(image.left, image.right) - image.left) / image.width).coerceIn(0f, 1f),
        top = ((selection.top.coerceIn(image.top, image.bottom) - image.top) / image.height).coerceIn(0f, 1f),
        right = ((selection.right.coerceIn(image.left, image.right) - image.left) / image.width).coerceIn(0f, 1f),
        bottom = ((selection.bottom.coerceIn(image.top, image.bottom) - image.top) / image.height).coerceIn(0f, 1f)
    )
}
