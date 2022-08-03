package com.telefonica.nestedscrollwebview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebView
import com.telefonica.nestedscrollwebview.helper.CoordinatorLayoutChildHelper
import com.telefonica.nestedscrollwebview.helper.NestedScrollViewChild
import com.telefonica.nestedscrollwebview.helper.NestedScrollViewHelper

class NestedScrollWebView: WebView, NestedScrollViewChild {

    private lateinit var nestedScrollingChildHelper: NestedScrollViewHelper
    private val coordinatorLayoutChildHelper: CoordinatorLayoutChildHelper =
        CoordinatorLayoutChildHelper()

    constructor(context: Context): super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet): super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int): super(context, attrs, defStyle) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet? = null) {
        overScrollMode = OVER_SCROLL_NEVER
        nestedScrollingChildHelper = NestedScrollViewHelper(this, attrs)
    }

    // WebView touch events must be also processed by nested scrolling logic.
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let { nestedScrollingChildHelper.onTouchEvent(it) }
        return super.onTouchEvent(event)
    }

    // Scroll computation must be delegated to nested scrolling logic.
    override fun computeScroll() {
        nestedScrollingChildHelper.computeScroll()
        coordinatorLayoutChildHelper.makeCoordinatorChildMatchItsBottomIfNeeded()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        coordinatorLayoutChildHelper.onViewAttached(this)
    }

    // Disabled to avoid double over-scrolling.
    override fun overScrollBy(
        deltaX: Int,
        deltaY: Int,
        scrollX: Int,
        scrollY: Int,
        scrollRangeX: Int,
        scrollRangeY: Int,
        maxOverScrollX: Int,
        maxOverScrollY: Int,
        isTouchEvent: Boolean
    ): Boolean {
        return true
    }

    override val view: ViewGroup
        get() = this

    // Scroll range is determined by webView vertical scroll range
    override fun getScrollRange(): Int =
        computeVerticalScrollRange()

    override fun computeHorizontalScrollRange(): Int =
        super.computeHorizontalScrollRange()

    override fun computeHorizontalScrollExtent(): Int =
        super.computeHorizontalScrollExtent()

    override fun computeVerticalScrollRange(): Int =
        super.computeVerticalScrollRange()

    override fun computeVerticalScrollExtent(): Int =
        super.computeVerticalScrollExtent()

    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
    }
}
