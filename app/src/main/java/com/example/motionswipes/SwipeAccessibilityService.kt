package com.example.motionswipes

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class SwipeAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        SwipeServiceHolder.serviceInstance = this
        Log.d("SwipeService", "✅ Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun performSwipe(leftToRight: Boolean) {
        val path = Path()
        if (leftToRight) {
            path.moveTo(200f, 1000f)
            path.lineTo(800f, 1000f)
        } else {
            path.moveTo(800f, 1000f)
            path.lineTo(200f, 1000f)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()

        dispatchGesture(gesture, null, null)
        Log.d("SwipeService", "✅ Swipe performed: ${if (leftToRight) "RIGHT" else "LEFT"}")
    }
}
