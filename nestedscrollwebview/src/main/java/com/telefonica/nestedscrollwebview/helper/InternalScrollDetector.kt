package com.telefonica.nestedscrollwebview.helper

import android.view.MotionEvent

class InternalScrollDetector {

    private var isScrolling: Boolean = false
    private var pageScrollChangedWhileScrolling: Boolean = false
    private var initialX: Float? = null
    private var initialY: Float? = null
    private var activePointerId: Int = INVALID_POINTER

    private var isEnabled = true

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
                reset()
                initialX = event.x
                initialY = event.y
                activePointerId = event.getPointerId(0)
                false
            }
            MotionEvent.ACTION_MOVE -> {
                val activePointerIndex = event.findPointerIndex(activePointerId)
                val initialEventY = initialY
                val initialEventX = initialX
                if (activePointerIndex == -1 || initialEventY == null || initialEventX == null) {
                    false
                } else {
                    val y = event.getY(activePointerIndex)
                    val x = event.getX(activePointerIndex)
                    val deltaY: Float = initialEventY - y
                    val deltaX: Float = initialEventX - x
                    val anyMovement = deltaX != 0f || deltaY != 0f
                    if (!isScrolling && anyMovement) {
                        isScrolling = true
                    }
                    isInternalScroll()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                (isInternalScroll()).also {
                    reset()
                }
            }
            else -> false
        }
    }

    fun onPageScrolled() {
        if (isEnabled && isScrolling) {
            pageScrollChangedWhileScrolling = true
        }
    }

    private fun reset() {
        initialX = null
        initialY = null
        activePointerId = INVALID_POINTER
        isScrolling = false
        pageScrollChangedWhileScrolling = false
    }

    private fun isInternalScroll(): Boolean =
        isScrolling && !pageScrollChangedWhileScrolling

    private companion object {
        const val INVALID_POINTER = -1
    }
}
