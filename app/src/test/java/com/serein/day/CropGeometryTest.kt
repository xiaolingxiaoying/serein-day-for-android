package com.serein.day

import org.junit.Assert.assertEquals
import org.junit.Test

class CropGeometryTest {
    @Test
    fun `full visible landscape image maps to the full source despite preview letterboxing`() {
        val result = previewSelectionToSource(
            selection = NormalizedRect(left = 0f, top = 0.25f, right = 1f, bottom = 0.75f),
            sourceWidth = 2000,
            sourceHeight = 1000,
            previewWidth = 1000,
            previewHeight = 1000
        )

        assertEquals(0f, result.left, 0.0001f)
        assertEquals(0f, result.top, 0.0001f)
        assertEquals(1f, result.right, 0.0001f)
        assertEquals(1f, result.bottom, 0.0001f)
    }

    @Test
    fun `portrait crop frame keeps destination aspect and stays inside the image`() {
        val image = fittedImageBounds(
            sourceWidth = 1000,
            sourceHeight = 2000,
            previewWidth = 1000,
            previewHeight = 1000
        )
        val frame = cropFrameFor(
            imageBounds = image,
            targetAspectRatio = 9f / 16f,
            zoom = 2f,
            centerX = 0f,
            centerY = 1f
        )

        assertEquals(9f / 16f, frame.width / frame.height, 0.0001f)
        assertEquals(image.left, frame.left, 0.0001f)
        assertEquals(image.bottom, frame.bottom, 0.0001f)
    }

    @Test
    fun `crop frame physical aspect is correct in a wide preview`() {
        val frame = cropFrameFor(
            imageBounds = NormalizedRect(0f, 0f, 1f, 1f),
            targetAspectRatio = 4f / 3f,
            previewAspectRatio = 16f / 9f,
            zoom = 1f,
            centerX = 0.5f,
            centerY = 0.5f
        )

        val physicalAspect = frame.width * (16f / 9f) / frame.height
        assertEquals(4f / 3f, physicalAspect, 0.0001f)
    }

    @Test
    fun `dragging the crop frame moves its center and clamps it inside the image`() {
        val image = NormalizedRect(0f, 0f, 1f, 1f)
        val crop = NormalizedRect(0.25f, 0.25f, 0.75f, 0.75f)

        val moved = moveCropCenter(
            imageBounds = image,
            crop = crop,
            centerX = 0.5f,
            centerY = 0.5f,
            deltaX = 1f,
            deltaY = -1f
        )

        assertEquals(0.75f, moved.x, 0.0001f)
        assertEquals(0.25f, moved.y, 0.0001f)
    }

    @Test
    fun `crop frame hit testing only accepts points inside the frame`() {
        val crop = NormalizedRect(0.2f, 0.3f, 0.8f, 0.7f)

        assertEquals(true, crop.contains(0.5f, 0.5f))
        assertEquals(false, crop.contains(0.1f, 0.5f))
    }
}
