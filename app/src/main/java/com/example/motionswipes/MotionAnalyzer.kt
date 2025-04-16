package com.example.motionswipes

import android.media.Image
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import kotlin.math.abs

class MotionAnalyzer : ImageAnalysis.Analyzer {

    private var lastFrame: ByteArray? = null
    private var width: Int = 0
    private var height: Int = 0
    private var lastSwipeTime = 0L
    private val cooldownMillis = 1000L // 1 second between swipe logs

    override fun analyze(image: ImageProxy) {
        val currentTime = System.currentTimeMillis()

        val byteArray = imageToByteArray(image.image) ?: return

        if (width == 0 || height == 0) {
            width = image.width
            height = image.height
        }

        lastFrame?.let { prev ->
            val totalDiff = calculateDiff(prev, byteArray)

            val minMotionToCare = 300_000
            if (totalDiff < minMotionToCare) {
                // No meaningful movement — do nothing
                image.close()
                return
            }

            val leftDiff = calculateRegionDiff(prev, byteArray, 0, width / 2)
            val rightDiff = calculateRegionDiff(prev, byteArray, width / 2, width)

            if (currentTime - lastSwipeTime > cooldownMillis) {
                if (leftDiff > rightDiff * 1.5) {
                    Log.d("MotionSwipe", "👈 Swipe Detected: RIGHT")
                    lastSwipeTime = currentTime
                } else if (rightDiff > leftDiff * 1.5) {
                    Log.d("MotionSwipe", "👉 Swipe Detected: LEFT")
                    lastSwipeTime = currentTime
                } else {
                    Log.d("MotionSwipe", "🔀 Motion detected, but not a swipe")
                }
            }
        }

        lastFrame = byteArray
        image.close()
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
