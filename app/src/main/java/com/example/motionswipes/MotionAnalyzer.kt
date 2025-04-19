package com.example.motionswipes

import android.content.Context
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import kotlin.math.abs

class MotionAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {

    private var lastFrame: ByteArray? = null
    private var width: Int = 0
    private var height: Int = 0
    private var lastSwipeTime = 0L

    private val cooldownMillis = 1000L
    private val minMotionRatio = 10.0
    private val swipeRatio = 2.0
    private var smoothedDiff = 0.0
    private val smoothingFactor = 0.8

    override fun analyze(image: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        Log.d("MotionSwipe", "📸 Frame received")

        val byteArray = imageToByteArray(image.image) ?: run {
            image.close()
            return
        }

        if (width == 0 || height == 0) {
            width = image.width
            height = image.height
        }

        lastFrame?.let { prev ->
            val totalDiff = calculateDiff(prev, byteArray)
            val pixelCount = width * height
            val diffRatio = totalDiff.toDouble() / pixelCount

            smoothedDiff = (smoothingFactor * smoothedDiff) + ((1 - smoothingFactor) * diffRatio)

            if (smoothedDiff < minMotionRatio) {
                image.close()
                sleepBriefly()
                return
            }

            val leftDiff = calculateRegionDiff(prev, byteArray, 0, width / 2)
            val rightDiff = calculateRegionDiff(prev, byteArray, width / 2, width)

            if (currentTime - lastSwipeTime > cooldownMillis) {
                if (leftDiff > rightDiff * swipeRatio) {
                    Log.d("MotionSwipe", "👈 Swipe Detected: RIGHT")
                    triggerSwipe("right")
                    lastSwipeTime = currentTime
                } else if (rightDiff > leftDiff * swipeRatio) {
                    Log.d("MotionSwipe", "👉 Swipe Detected: LEFT")
                    triggerSwipe("left")
                    lastSwipeTime = currentTime
                } else {
                    Log.d("MotionSwipe", "🔀 Motion detected, but not a swipe")
                }
            }
        }

        lastFrame = byteArray
        image.close()
        sleepBriefly()
    }

    private fun sleepBriefly() {
        try {
            Thread.sleep(300) // Helps slow down log spam
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun triggerSwipe(direction: String) {
        val service = SwipeServiceHolder.serviceInstance
        if (service != null) {
            Log.d("MotionSwipe", "✅ Sending gesture to accessibility service: $direction")
            service.performSwipe(leftToRight = direction == "right")
        } else {
            Log.w("MotionSwipe", "❌ SwipeAccessibilityService not connected")
        }
    }

    private fun imageToByteArray(image: Image?): ByteArray? {
        if (image == null) return null
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return bytes
    }

    private fun calculateDiff(prev: ByteArray, curr: ByteArray): Long {
        return prev.zip(curr).sumOf { abs(it.first - it.second).toLong() }
    }

    private fun calculateRegionDiff(
        prev: ByteArray,
        curr: ByteArray,
        xStart: Int,
        xEnd: Int
    ): Long {
        var diff = 0L
        val rowStride = width
        for (y in 0 until height) {
            for (x in xStart until xEnd) {
                val index = y * rowStride + x
                if (index < prev.size && index < curr.size) {
                    diff += abs(prev[index] - curr[index])
                }
            }
        }
        return diff
    }
}
