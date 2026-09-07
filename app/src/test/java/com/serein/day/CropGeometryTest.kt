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
}
