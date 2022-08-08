package com.telefonica.nestedscrollwebview.helper

import android.view.View
import android.view.ViewParent
import androidx.coordinatorlayout.widget.CoordinatorLayout

class CoordinatorLayoutChildHelper {

    private var lastYPosition: Int? = null
    private var coordinatorChildView: View? = null
    private var coordinatorParentView: CoordinatorLayout? = null

    private var isBottomMatchingBehaviourEnabled = false

    fun onViewAttached(view: View) {
        lastYPosition = null
        coordinatorChildView = null
        coordinatorParentView = null

        var childView: View = view
        while (childView.parent is View && coordinatorParentView == null) {
            when(val viewParent: ViewParent = childView.parent) {
                is CoordinatorLayout -> {
                    coordinatorParentView = viewParent
                    coordinatorChildView = childView
                }
                is View ->
                    childView = viewParent
            }
        }
    }

    fun setBottomMatchingBehaviourEnabled(enabled: Boolean) {
        if (isBottomMatchingBehaviourEnabled && !enabled) {
            lastYPosition = null
            resetBottomMargin()
        }
        isBottomMatchingBehaviourEnabled = enabled
        computeBottomMarginIfNeeded()
    }

    fun computeBottomMarginIfNeeded() {
        if (coordinatorChildView == null || coordinatorParentView == null || !isBottomMatchingBehaviourEnabled) {
            return
        }

        val childBounds = IntArray(2)
        coordinatorChildView!!.getLocationOnScreen(childBounds)
        if (childBounds[1] != lastYPosition) {
            val childBottom = childBounds[1] + coordinatorChildView!!.height
            lastYPosition = childBounds[1]

            val parentBounds = IntArray(2)
            coordinatorParentView!!.getLocationOnScreen(parentBounds)
            val parentBottom = parentBounds[1] + coordinatorParentView!!.height

            val diff = childBottom - parentBottom
            if (diff != 0) {
                with(coordinatorChildView!!.layoutParams as CoordinatorLayout.LayoutParams) {
                    bottomMargin += diff
                    coordinatorChildView!!.layoutParams = this
                }
            }
        }
    }

    private fun resetBottomMargin() {
        coordinatorChildView?.let { childView ->
            with(childView.layoutParams as CoordinatorLayout.LayoutParams) {
                bottomMargin = 0
                childView.layoutParams = this
            }
        }
    }
}
