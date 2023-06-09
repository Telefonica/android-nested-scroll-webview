/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Based on the implementation of NestedScrollView from The Android Open Source Project
 */
package com.telefonica.nestedscrollwebview;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.webkit.WebView;
import android.widget.EdgeEffect;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.NestedScrollingChild3;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.ViewCompat;
import androidx.core.widget.EdgeEffectCompat;

import com.telefonica.nestedscrollwebview.helper.CoordinatorLayoutChildHelper;
import com.telefonica.nestedscrollwebview.helper.InternalScrollDetector;

public class NestedScrollWebView extends WebView implements NestedScrollingChild3 {

    private final CoordinatorLayoutChildHelper coordinatorLayoutChildHelper =
            new CoordinatorLayoutChildHelper();
    private final InternalScrollDetector internalScrollDetector =
            new InternalScrollDetector();

    public NestedScrollWebView(Context context) {
        super(context);
        init(context, null, null);
    }

    public NestedScrollWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, null);
    }

    public NestedScrollWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, @Nullable AttributeSet attrs, @Nullable Integer defStyleAttr) {
        if (attrs != null) {
            TypedArray styledAttrs = null;
            try {
                styledAttrs = context.getTheme().obtainStyledAttributes(
                        attrs,
                        R.styleable.NestedScrollWebView,
                        defStyleAttr != null ? defStyleAttr : 0,
                        0
                );
                coordinatorLayoutChildHelper.setBottomMatchingBehaviourEnabled(
                        styledAttrs.getBoolean(
                                R.styleable.NestedScrollWebView_coordinatorBottomMatchingEnabled,
                                false
                        )
                );
                internalScrollDetector.setEnabled(
                        styledAttrs.getBoolean(
                                R.styleable.NestedScrollWebView_blockNestedScrollingOnInternalContentScrolls,
                                true
                        )
                );
            } finally {
                if (styledAttrs != null) {
                    styledAttrs.recycle();
                }
            }
        }
        setOverScrollMode(OVER_SCROLL_NEVER);
        initNestedScrollView(context, attrs);
    }

    /* NestedScrollView class code block.
    Following functionality has been directly extracted from NestedScrollView
    class (Android Support Library Compat 1.8.0), keeping original code unaltered as
    much as possible, this way this part could be easily updated in the future if needed:
    * NestedScrollView.onTouchEvent(@NonNull MotionEvent ev) implementation
    * NestedScrollView.computeScroll() implementation
    * NestedScrollView NestedScrollingChild3 methods implementation
    */

    private static final String TAG = "NestedScrollView";

    private OverScroller mScroller;

    /** @hide */
    @RestrictTo(LIBRARY)
    @VisibleForTesting
    @NonNull
    public EdgeEffect mEdgeGlowTop;

    /** @hide */
    @RestrictTo(LIBRARY)
    @VisibleForTesting
    @NonNull
    public EdgeEffect mEdgeGlowBottom;

    /**
     * Position of the last motion event.
     */
    private int mLastMotionY;

    /**
     * True if the user is currently dragging this ScrollView around. This is
     * not the same as 'is being flinged', which can be checked by
     * mScroller.isFinished() (flinging begins when the user lifts their finger).
     */
    private boolean mIsBeingDragged = false;

    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;

    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;

    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private int mActivePointerId = INVALID_POINTER;

    /**
     * Used during scrolling to retrieve the new offset within the window.
     */
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private int mNestedYOffset;

    private int mLastScrollerY;


    /**
     * Sentinel value for no current active pointer.
     * Used by {@link #mActivePointerId}.
     */
    private static final int INVALID_POINTER = -1;

    private NestedScrollingChildHelper mChildHelper;

    // NestedScrollView constructor
    public void initNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs/*, int defStyleAttr*/) {
        mEdgeGlowTop = EdgeEffectCompat.create(context, attrs);
        mEdgeGlowBottom = EdgeEffectCompat.create(context, attrs);

        initScrollView();

        /*
        final TypedArray a = context.obtainStyledAttributes(
        		attrs, SCROLLVIEW_STYLEABLE, defStyleAttr, 0);

        setFillViewport(a.getBoolean(0, false));

        a.recycle();
        */

        mChildHelper = new NestedScrollingChildHelper(this);

        // ...because why else would you be using this widget?
        setNestedScrollingEnabled(true);

        // ViewCompat.setAccessibilityDelegate(getView(), ACCESSIBILITY_DELEGATE);
    }

    private void initScrollView() {
        mScroller = new OverScroller(getContext());
        setFocusable(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setWillNotDraw(false);
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    /* NestedScrollView onTouchEvent */
    public void onNestedTouchEvent(@NonNull MotionEvent ev) {
        initVelocityTrackerIfNotExists();

        final int actionMasked = ev.getActionMasked();

        if (actionMasked == MotionEvent.ACTION_DOWN) {
            mNestedYOffset = 0;
            /* Not present on NestedScrollView code. We need to reset scroll offset on touch down,
            as we found while testing that in some cases action was invoked with a non-zero scroll
            offset, breaking page scroll. */
            mScrollOffset[1] = 0;
        }

        MotionEvent vtev = MotionEvent.obtain(ev);
        vtev.offsetLocation(0, mNestedYOffset);

        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN: {
                /* Webview always returns no children
                if (getChildCount() == 0) {
                    return false;
                }*/
                if (mIsBeingDragged) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }

                /*
                 * If being flinged and user touches, stop the fling. isFinished
                 * will be false if being flinged.
                 */
                if (!mScroller.isFinished()) {
                    abortAnimatedScroll();
                }

                // Remember where the motion event started
                mLastMotionY = (int) ev.getY();
                mActivePointerId = ev.getPointerId(0);
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
                break;
            }
            case MotionEvent.ACTION_MOVE:
                final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                if (activePointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + mActivePointerId + " in onTouchEvent");
                    break;
                }

                final int y = (int) ev.getY(activePointerIndex);
                int deltaY = mLastMotionY - y;
                deltaY -= releaseVerticalGlow(deltaY, ev.getX(activePointerIndex));
                if (!mIsBeingDragged && Math.abs(deltaY) > mTouchSlop) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    mIsBeingDragged = true;
                    if (deltaY > 0) {
                        deltaY -= mTouchSlop;
                    } else {
                        deltaY += mTouchSlop;
                    }
                }
                if (mIsBeingDragged) {
                    // Start with nested pre scrolling
                    if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset,
                            ViewCompat.TYPE_TOUCH)) {
                        deltaY -= mScrollConsumed[1];
                        mNestedYOffset += mScrollOffset[1];
                    }

                    // Scroll to follow the motion event
                    mLastMotionY = y - mScrollOffset[1];

                    final int oldY = getScrollY();
                    final int range = getScrollRange();
                    final int overscrollMode = getOverScrollMode();
                    boolean canOverscroll = overscrollMode == OVER_SCROLL_ALWAYS
                            || (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0);

                    // Calling overScrollByCompat will call onOverScrolled, which
                    // calls onScrollChanged if applicable.
                    boolean clearVelocityTracker =
                            overScrollByCompat(0, deltaY, 0, getScrollY(), 0, range, 0,
                                    0, true) && !hasNestedScrollingParent(ViewCompat.TYPE_TOUCH);

                    final int scrolledDeltaY = getScrollY() - oldY;
                    final int unconsumedY = deltaY - scrolledDeltaY;

                    mScrollConsumed[1] = 0;

                    dispatchNestedScroll(0, scrolledDeltaY, 0, unconsumedY, mScrollOffset,
                            ViewCompat.TYPE_TOUCH, mScrollConsumed);

                    mLastMotionY -= mScrollOffset[1];
                    mNestedYOffset += mScrollOffset[1];

                    if (canOverscroll) {
                        deltaY -= mScrollConsumed[1];
                        final int pulledToY = oldY + deltaY;
                        if (pulledToY < 0) {
                            EdgeEffectCompat.onPullDistance(mEdgeGlowTop,
                                    (float) -deltaY / getHeight(),
                                    ev.getX(activePointerIndex) / getWidth());
                            if (!mEdgeGlowBottom.isFinished()) {
                                mEdgeGlowBottom.onRelease();
                            }
                        } else if (pulledToY > range) {
                            EdgeEffectCompat.onPullDistance(mEdgeGlowBottom,
                                    (float) deltaY / getHeight(),
                                    1.f - ev.getX(activePointerIndex) / getWidth());
                            if (!mEdgeGlowTop.isFinished()) {
                                mEdgeGlowTop.onRelease();
                            }
                        }
                        if (!mEdgeGlowTop.isFinished() || !mEdgeGlowBottom.isFinished()) {
                            ViewCompat.postInvalidateOnAnimation(this);
                            clearVelocityTracker = false;
                        }
                    }
                    if (clearVelocityTracker) {
                        // Break our velocity if we hit a scroll barrier.
                        mVelocityTracker.clear();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int initialVelocity = (int) velocityTracker.getYVelocity(mActivePointerId);
                if ((Math.abs(initialVelocity) >= mMinimumVelocity)) {
                    if (!edgeEffectFling(initialVelocity)
                            && !dispatchNestedPreFling(0, -initialVelocity)) {
                        dispatchNestedFling(0, -initialVelocity, true);
                        fling(-initialVelocity);
                    }
                } else if (mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0,
                        getScrollRange())) {
                    ViewCompat.postInvalidateOnAnimation(this);
                }
                mActivePointerId = INVALID_POINTER;
                endDrag();
                break;
            case MotionEvent.ACTION_CANCEL:
                // WebView always return no children
                if (mIsBeingDragged /* && getChildCount() > 0*/) {
                    if (mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0,
                            getScrollRange())) {
                        ViewCompat.postInvalidateOnAnimation(this);
                    }
                }
                mActivePointerId = INVALID_POINTER;
                endDrag();
                break;
            case MotionEvent.ACTION_POINTER_DOWN: {
                final int index = ev.getActionIndex();
                mLastMotionY = (int) ev.getY(index);
                mActivePointerId = ev.getPointerId(index);
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                mLastMotionY = (int) ev.getY(ev.findPointerIndex(mActivePointerId));
                break;
        }

        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(vtev);
        }
        vtev.recycle();
    }

    @Override
    public void computeScroll() {

        if (mScroller.isFinished()) {
            return;
        }

        mScroller.computeScrollOffset();
        final int y = mScroller.getCurrY();
        int unconsumed = y - mLastScrollerY;
        mLastScrollerY = y;

        // Nested Scrolling Pre Pass
        mScrollConsumed[1] = 0;
        dispatchNestedPreScroll(0, unconsumed, mScrollConsumed, null,
                ViewCompat.TYPE_NON_TOUCH);
        unconsumed -= mScrollConsumed[1];

        final int range = getScrollRange();

        if (unconsumed != 0) {
            // Internal Scroll
            final int oldScrollY = getScrollY();
            overScrollByCompat(0, unconsumed, getScrollX(), oldScrollY, 0, range, 0, 0, false);
            final int scrolledByMe = getScrollY() - oldScrollY;
            unconsumed -= scrolledByMe;

            // Nested Scrolling Post Pass
            mScrollConsumed[1] = 0;
            dispatchNestedScroll(0, scrolledByMe, 0, unconsumed, mScrollOffset,
                    ViewCompat.TYPE_NON_TOUCH, mScrollConsumed);
            unconsumed -= mScrollConsumed[1];
        }

        if (unconsumed != 0) {
            final int mode = getOverScrollMode();
            final boolean canOverscroll = mode == OVER_SCROLL_ALWAYS
                    || (mode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0);
            if (canOverscroll) {
                if (unconsumed < 0) {
                    if (mEdgeGlowTop.isFinished()) {
                        mEdgeGlowTop.onAbsorb((int) mScroller.getCurrVelocity());
                    }
                } else {
                    if (mEdgeGlowBottom.isFinished()) {
                        mEdgeGlowBottom.onAbsorb((int) mScroller.getCurrVelocity());
                    }
                }
            }
            abortAnimatedScroll();
        }

        if (!mScroller.isFinished()) {
            ViewCompat.postInvalidateOnAnimation(this);
        } else {
            stopNestedScroll(ViewCompat.TYPE_NON_TOUCH);
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void abortAnimatedScroll() {
        mScroller.abortAnimation();
        stopNestedScroll(ViewCompat.TYPE_NON_TOUCH);
    }

    /**
     * If either of the vertical edge glows are currently active, this consumes part or all of
     * deltaY on the edge glow.
     *
     * @param deltaY The pointer motion, in pixels, in the vertical direction, positive
     *                         for moving down and negative for moving up.
     * @param x The vertical position of the pointer.
     * @return The amount of <code>deltaY</code> that has been consumed by the
     * edge glow.
     */
    private int releaseVerticalGlow(int deltaY, float x) {
        // First allow releasing existing overscroll effect:
        float consumed = 0;
        float displacement = x / getWidth();
        float pullDistance = (float) deltaY / getHeight();
        if (EdgeEffectCompat.getDistance(mEdgeGlowTop) != 0) {
            consumed = -EdgeEffectCompat.onPullDistance(mEdgeGlowTop, -pullDistance, displacement);
            if (EdgeEffectCompat.getDistance(mEdgeGlowTop) == 0) {
                mEdgeGlowTop.onRelease();
            }
        } else if (EdgeEffectCompat.getDistance(mEdgeGlowBottom) != 0) {
            consumed = EdgeEffectCompat.onPullDistance(mEdgeGlowBottom, pullDistance,
                    1 - displacement);
            if (EdgeEffectCompat.getDistance(mEdgeGlowBottom) == 0) {
                mEdgeGlowBottom.onRelease();
            }
        }
        int pixelsConsumed = Math.round(consumed * getHeight());
        if (pixelsConsumed != 0) {
            invalidate();
        }
        return pixelsConsumed;
    }

    @SuppressWarnings({"SameParameterValue", "unused"})
    boolean overScrollByCompat(int deltaX, int deltaY,
                               int scrollX, int scrollY,
                               int scrollRangeX, int scrollRangeY,
                               int maxOverScrollX, int maxOverScrollY,
                               boolean isTouchEvent) {
        final int overScrollMode = getOverScrollMode();
        final boolean canScrollHorizontal =
                computeHorizontalScrollRange() > computeHorizontalScrollExtent();
        final boolean canScrollVertical =
                computeVerticalScrollRange() > computeVerticalScrollExtent();
        final boolean overScrollHorizontal = overScrollMode == OVER_SCROLL_ALWAYS
                || (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollHorizontal);
        final boolean overScrollVertical = overScrollMode == OVER_SCROLL_ALWAYS
                || (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollVertical);

        int newScrollX = scrollX + deltaX;
        if (!overScrollHorizontal) {
            maxOverScrollX = 0;
        }

        int newScrollY = scrollY + deltaY;
        if (!overScrollVertical) {
            maxOverScrollY = 0;
        }

        // Clamp values if at the limits and record
        final int left = -maxOverScrollX;
        final int right = maxOverScrollX + scrollRangeX;
        final int top = -maxOverScrollY;
        final int bottom = maxOverScrollY + scrollRangeY;

        boolean clampedX = false;
        if (newScrollX > right) {
            newScrollX = right;
            clampedX = true;
        } else if (newScrollX < left) {
            newScrollX = left;
            clampedX = true;
        }

        boolean clampedY = false;
        if (newScrollY > bottom) {
            newScrollY = bottom;
            clampedY = true;
        } else if (newScrollY < top) {
            newScrollY = top;
            clampedY = true;
        }

        if (clampedY && !hasNestedScrollingParent(ViewCompat.TYPE_NON_TOUCH)) {
            mScroller.springBack(newScrollX, newScrollY, 0, 0, 0, getScrollRange());
        }

        onOverScrolled(newScrollX, newScrollY, clampedX, clampedY);

        return clampedX || clampedY;
    }

    private boolean edgeEffectFling(int velocityY) {
        boolean consumed = true;
        if (EdgeEffectCompat.getDistance(mEdgeGlowTop) != 0) {
            mEdgeGlowTop.onAbsorb(velocityY);
        } else if (EdgeEffectCompat.getDistance(mEdgeGlowBottom) != 0) {
            mEdgeGlowBottom.onAbsorb(-velocityY);
        } else {
            consumed = false;
        }
        return consumed;
    }

    /**
     * Fling the scroll view
     *
     * @param velocityY The initial velocity in the Y direction. Positive
     *                  numbers mean that the finger/cursor is moving down the screen,
     *                  which means we want to scroll towards the top.
     */
    public void fling(int velocityY) {
        // WebView always returns no children
        //if (getChildCount() > 0) {

            mScroller.fling(getScrollX(), getScrollY(), // start
                    0, velocityY, // velocities
                    0, 0, // x
                    Integer.MIN_VALUE, Integer.MAX_VALUE, // y
                    0, 0); // overscroll
            runAnimatedScroll(true);
        //}
    }

    private void runAnimatedScroll(boolean participateInNestedScrolling) {
        if (participateInNestedScrolling) {
            startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
        } else {
            stopNestedScroll(ViewCompat.TYPE_NON_TOUCH);
        }
        mLastScrollerY = getScrollY();
        ViewCompat.postInvalidateOnAnimation(this);
    }

    private void endDrag() {
        mIsBeingDragged = false;

        recycleVelocityTracker();
        stopNestedScroll(ViewCompat.TYPE_TOUCH);

        mEdgeGlowTop.onRelease();
        mEdgeGlowBottom.onRelease();
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = ev.getActionIndex();
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            // TODO: Make this decision more intelligent.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionY = (int) ev.getY(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    // NestedScrollingChild3

    @Override
    public void dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                     int dyUnconsumed, @Nullable int[] offsetInWindow, int type, @NonNull int[] consumed) {
        mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                offsetInWindow, type, consumed);
    }

    // NestedScrollingChild2

    @Override
    public boolean startNestedScroll(int axes, int type) {
        return mChildHelper.startNestedScroll(axes, type);
    }

    @Override
    public void stopNestedScroll(int type) {
        mChildHelper.stopNestedScroll(type);
    }

    @Override
    public boolean hasNestedScrollingParent(int type) {
        return mChildHelper.hasNestedScrollingParent(type);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, @Nullable int[] offsetInWindow, int type) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                offsetInWindow, type);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed,
                                           @Nullable int[] offsetInWindow, int type) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
    }

    // NestedScrollingChild

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return startNestedScroll(axes, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void stopNestedScroll() {
        stopNestedScroll(ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return hasNestedScrollingParent(ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, @Nullable int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed,
                                           @Nullable int[] offsetInWindow) {
        return dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    /* End of NestedScrollView class code block. */

    /* Functions not present in original NestedScrollView implementation, but required for
    NestedScrollView implementation to work properly. */

    @Override
    public boolean overScrollBy(
            int deltaX,
            int deltaY,
            int scrollX,
            int scrollY,
            int scrollRangeX,
            int scrollRangeY,
            int maxOverScrollX,
            int maxOverScrollY,
            boolean isTouchEvent
    ) {
        if (!mIsBeingDragged) {
            overScrollByCompat(
                    deltaX,
                    deltaY,
                    scrollX,
                    scrollY,
                    scrollRangeX,
                    scrollRangeY,
                    maxOverScrollX,
                    maxOverScrollY,
                    isTouchEvent
            );
        }
        return true;
    }

    @Override
    public int getNestedScrollAxes() {
        return ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    private int getScrollRange() {
        return computeVerticalScrollRange();
    }

    /* Extra functionalities */

    public void setCoordinatorBottomMatchingBehaviourEnabled(boolean enabled) {
        coordinatorLayoutChildHelper.setBottomMatchingBehaviourEnabled(enabled);
    }

    public void setBlockNestedScrollingOnInternalContentScrollsEnabled(boolean enabled) {
        internalScrollDetector.setEnabled(enabled);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        coordinatorLayoutChildHelper.onViewAttached(this);
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        internalScrollDetector.onPageScrolled();
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
        post(coordinatorLayoutChildHelper::computeBottomMarginIfNeeded);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        internalScrollDetector.onPageScrolled();
        super.onScrollChanged(l, t, oldl, oldt);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!internalScrollDetector.onTouchEvent(event)) {
            onNestedTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }
}
