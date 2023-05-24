package com.telefonica.nestedscrollwebview.helper

import android.view.MotionEvent
import kotlin.math.abs

class HorizontalScrollDetector {

    private var isHorizontalScrollMovement: Boolean? = null
    private var initialX: Float? = null
    private var initialY: Float? = null
    private var activePointerId: Int = INVALID_POINTER

    private var isEnabled = false

    fun setEnabled(enabled: Boolean) {
        if (isEnabled != enabled) {
            reset()
        }
        isEnabled = enabled
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = event.x
                initialY = event.y
                activePointerId = event.getPointerId(0)
                isHorizontalScrollMovement = null
                false
            }
            MotionEvent.ACTION_MOVE -> {
                val activePointerIndex = event.findPointerIndex(activePointerId)
                if (activePointerIndex == -1 || initialX == null || initialY == null) {
                    false
                } else {
                    val y = event.getY(activePointerIndex)
                    val x = event.getX(activePointerIndex)
                    val deltaY: Float = initialY!! - y
                    val deltaX: Float = initialX!! - x
                    val anyMovement = deltaX != 0f || deltaY != 0f
                    if (isHorizontalScrollMovement == null && anyMovement) {
                        isHorizontalScrollMovement = abs(deltaX) > abs(deltaY)
                    }
                    isHorizontalScrollMovement == true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasHorizontalScrollMovement: Boolean = isHorizontalScrollMovement == true
                reset()
                wasHorizontalScrollMovement
            }
            else -> false
        }
    }

    fun onScrollChanged(oldY: Int, newY: Int) {
        if (isEnabled && isHorizontalScrollMovement == true) {
            isHorizontalScrollMovement = oldY == newY
        }
    }

    private fun reset() {
        initialX = null
        initialY = null
        isHorizontalScrollMovement = null
    }

    private companion object {
        const val INVALID_POINTER = -1
    }
}
