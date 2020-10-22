package com.backpackingmap.backpackingmap.map

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.pow
import kotlin.math.sqrt

class OmniGestureDetector(context: Context, private val onEvent: (Event) -> Unit) {
    fun onTouchEvent(view: View, event: MotionEvent): Boolean {
        val cornerToCornerDistance =
            sqrt(view.width.toFloat().pow(2) + view.height.toFloat().pow(2))
        gestureListener.cornerToCornerDistance = cornerToCornerDistance
        scaleListener.cornerToCornerDistance = cornerToCornerDistance

        gestureDetector.onTouchEvent(event)
        scaleDetector.onTouchEvent(event)

        val scalePriority = scaleListener.lastEventMagnitude
        val scaleEvent = scaleListener.lastEvent

        val gesturePriority = gestureListener.lastEventMagnitude
        val gestureEvent = gestureListener.lastEvent

        if (gesturePriority >= scalePriority && gestureEvent != null) {
            onEvent(gestureEvent)
        } else if (scaleEvent != null) {
            onEvent(scaleEvent)
        }

        return true
    }

    private val gestureListener = ReportingGestureListener()
    private val gestureDetector = GestureDetector(context, gestureListener)
    private val scaleListener = ReportingScaleListener()
    private val scaleDetector = ScaleGestureDetector(context, scaleListener)

    sealed class Event {
        data class Down(val e: MotionEvent?) : Event()
        data class ShowPress(val e: MotionEvent?) : Event()
        data class SingleTapUp(val e: MotionEvent?) : Event()

        data class Scroll(
            val initial: MotionEvent?,
            val current: MotionEvent?,
            val distanceX: Float,
            val distanceY: Float,
        ) : Event()

        data class LongPress(val e: MotionEvent?) : Event()

        data class Fling(
            val initial: MotionEvent?,
            val current: MotionEvent?,
            val velocityX: Float?,
            val velocityY: Float?,
        ) : Event()

        data class Scale(val scaleFactor: Float?) : Event()
    }

    private class ReportingGestureListener : GestureDetector.OnGestureListener, ReportingListener {
        override var lastEvent: Event? = null
        override var lastEventMagnitude = ReportingListener.MIN_EVENT_MAGNITUDE
        override var cornerToCornerDistance: Float? = null

        var lastPointer: Int? = null

        override fun onDown(e: MotionEvent?): Boolean {
            lastEventMagnitude = ReportingListener.MAX_EVENT_MAGNITUDE
            lastEvent = Event.Down(e)
            return true
        }

        override fun onShowPress(e: MotionEvent?) {
            lastEventMagnitude = ReportingListener.MAX_EVENT_MAGNITUDE
            lastEvent = Event.ShowPress(e)
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            lastEventMagnitude = ReportingListener.MAX_EVENT_MAGNITUDE
            lastEvent = Event.SingleTapUp(e)
            return true
        }

        override fun onScroll(
            initial: MotionEvent?,
            current: MotionEvent?,
            distanceX: Float,
            distanceY: Float,
        ): Boolean {
            val lastPointerCached = lastPointer
            if (lastPointerCached != null) {
                if (current != null && current.findPointerIndex(lastPointerCached) != 0) {
                    // If the user switches pointers in the middle of a scroll we throw out the
                    // first event of the new pointer because its distanceX and distanceY are
                    // from the other pointer. If we used them the position would jump
                    lastPointer = current.getPointerId(0)

                    lastEventMagnitude = ReportingListener.MIN_EVENT_MAGNITUDE
                    lastEvent = null

                    return true
                }
            }
            lastPointer = current?.getPointerId(0)

            val cornerToCornerDistanceCached = cornerToCornerDistance
            if (cornerToCornerDistanceCached == null) {
                lastEventMagnitude = ReportingListener.MIN_EVENT_MAGNITUDE
                lastEvent = null
                return true
            }

            lastEventMagnitude =
                sqrt(distanceX.pow(2f) + distanceY.pow(2f)) / cornerToCornerDistanceCached
            lastEvent = Event.Scroll(initial, current, distanceX, distanceY)
            return true
        }

        override fun onLongPress(e: MotionEvent?) {
            lastEventMagnitude = ReportingListener.MAX_EVENT_MAGNITUDE
            lastEvent = Event.LongPress(e)
        }

        override fun onFling(
            initial: MotionEvent?,
            current: MotionEvent?,
            velocityX: Float,
            velocityY: Float,
        ): Boolean {
            lastEventMagnitude = ReportingListener.MAX_EVENT_MAGNITUDE
            lastEvent = Event.Fling(initial, current, velocityX, velocityY)
            return true
        }
    }

    private class ReportingScaleListener : ScaleGestureDetector.OnScaleGestureListener,
        ReportingListener {

        override var lastEvent: Event? = null
        override var lastEventMagnitude = ReportingListener.MIN_EVENT_MAGNITUDE
        override var cornerToCornerDistance: Float? = null

        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            lastEvent = null
            lastEventMagnitude = ReportingListener.MIN_EVENT_MAGNITUDE
            return true
        }

        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            val cornerToCornerDistanceCached = cornerToCornerDistance
            if (detector == null || cornerToCornerDistanceCached == null) {
                lastEvent = null
                lastEventMagnitude = ReportingListener.MIN_EVENT_MAGNITUDE
                return true
            }

            val distance = sqrt((detector.currentSpanX - detector.previousSpanX).pow(2) +
                    (detector.currentSpanY - detector.previousSpanY).pow(2))

            lastEventMagnitude = distance / cornerToCornerDistanceCached
            lastEvent = Event.Scale(detector.scaleFactor)
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
            lastEventMagnitude = ReportingListener.MIN_EVENT_MAGNITUDE
            lastEvent = null
        }

    }

    private interface ReportingListener {
        var lastEvent: Event?
        var lastEventMagnitude: Float
        var cornerToCornerDistance: Float?

        companion object {
            const val MIN_EVENT_MAGNITUDE = 0f
            const val MAX_EVENT_MAGNITUDE = 1f
        }
    }
}