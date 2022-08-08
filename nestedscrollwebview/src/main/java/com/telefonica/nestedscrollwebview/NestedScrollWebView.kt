package com.telefonica.nestedscrollwebview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebView
import androidx.core.view.NestedScrollingChild3
import androidx.core.view.ViewCompat
import com.telefonica.nestedscrollwebview.helper.CoordinatorLayoutChildHelper
import com.telefonica.nestedscrollwebview.helper.NestedScrollViewHelper
import com.telefonica.nestedscrollwebview.helper.NestedScrollingView

open class NestedScrollWebView : WebView, NestedScrollingChild3 {

    private lateinit var nestedScrollingChildHelper: NestedScrollViewHelper
    private val coordinatorLayoutChildHelper: CoordinatorLayoutChildHelper =
        CoordinatorLayoutChildHelper()

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet? = null, defStyle: Int? = null) {
        if (attrs != null) {
            val styledAttrs = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.NestedScrollWebView,
                defStyle ?: 0,
                0
            )
            try {
                coordinatorLayoutChildHelper.setBottomMatchingBehaviourEnabled(
                    styledAttrs.getBoolean(
                        R.styleable.NestedScrollWebView_coordinatorBottomMatchingEnabled,
                        false
                    )
                )
            } finally {
                styledAttrs.recycle()
            }
        }

        overScrollMode = OVER_SCROLL_NEVER
        val nestedScrollingView = object : NestedScrollingView {
            override val view: ViewGroup =
                this@NestedScrollWebView

            override fun getScrollRange(): Int =
                this@NestedScrollWebView.computeVerticalScrollRange()

            override fun computeHorizontalScrollRange(): Int =
                this@NestedScrollWebView.computeHorizontalScrollRange()

            override fun computeHorizontalScrollExtent(): Int =
                this@NestedScrollWebView.computeHorizontalScrollExtent()

            override fun computeVerticalScrollRange(): Int =
                this@NestedScrollWebView.computeVerticalScrollRange()

            override fun computeVerticalScrollExtent(): Int =
                this@NestedScrollWebView.computeVerticalScrollExtent()

            override fun onOverScrolled(
                scrollX: Int,
                scrollY: Int,
                clampedX: Boolean,
                clampedY: Boolean
            ) {
                this@NestedScrollWebView.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
            }
        }
        nestedScrollingChildHelper = NestedScrollViewHelper(nestedScrollingView, attrs)
    }

    fun setCoordinatorBottomMatchingBehaviourEnabled(enabled: Boolean) {
        coordinatorLayoutChildHelper.setBottomMatchingBehaviourEnabled(enabled)
    }

    // WebView touch events must be also processed by nested scrolling logic.
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let { nestedScrollingChildHelper.onTouchEvent(it) }
        return super.onTouchEvent(event)
    }

    // Scroll computation must be also processed by nested scrolling logic.
    override fun computeScroll() {
        nestedScrollingChildHelper.computeScroll()
        coordinatorLayoutChildHelper.computeBottomMarginIfNeeded()
        super.computeScroll()
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
    ): Boolean =
        true

    // Delegation of all NestedScrollingChild3 methods
    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int,
        consumed: IntArray
    ) {
        nestedScrollingChildHelper.dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            offsetInWindow,
            type,
            consumed
        )
    }

    override fun startNestedScroll(axes: Int, type: Int): Boolean {
        return nestedScrollingChildHelper.startNestedScroll(axes, type)
    }

    override fun stopNestedScroll(type: Int) {
        nestedScrollingChildHelper.stopNestedScroll(type)
    }

    override fun hasNestedScrollingParent(type: Int): Boolean {
        return nestedScrollingChildHelper.hasNestedScrollingParent(type)
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean =
        nestedScrollingChildHelper.dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            offsetInWindow,
            type
        )

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean =
        nestedScrollingChildHelper.dispatchNestedPreScroll(
            dx,
            dy,
            consumed,
            offsetInWindow,
            type
        )

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        nestedScrollingChildHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean =
        nestedScrollingChildHelper.isNestedScrollingEnabled

    override fun startNestedScroll(axes: Int): Boolean =
        nestedScrollingChildHelper.startNestedScroll(axes, ViewCompat.TYPE_TOUCH)

    override fun stopNestedScroll() {
        nestedScrollingChildHelper.stopNestedScroll(ViewCompat.TYPE_TOUCH)
    }

    override fun hasNestedScrollingParent(): Boolean =
        nestedScrollingChildHelper.hasNestedScrollingParent(ViewCompat.TYPE_TOUCH)

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?
    ): Boolean =
        nestedScrollingChildHelper.dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            offsetInWindow
        )

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?
    ): Boolean =
        nestedScrollingChildHelper.dispatchNestedPreScroll(
            dx,
            dy,
            consumed,
            offsetInWindow,
            ViewCompat.TYPE_TOUCH
        )

    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean =
        nestedScrollingChildHelper.dispatchNestedFling(
            velocityX,
            velocityY,
            consumed
        )

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean =
        nestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY)
}
