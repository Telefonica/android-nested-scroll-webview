package com.telefonica.nestedscrollwebview.helper

import android.view.ViewGroup

interface NestedScrollViewChild {
    val view: ViewGroup
    fun getScrollRange(): Int

    /* Existing ViewGroup methods with protected visibility */
    fun computeHorizontalScrollRange(): Int
    fun computeHorizontalScrollExtent(): Int
    fun computeVerticalScrollRange(): Int
    fun computeVerticalScrollExtent(): Int
    fun onOverScrolled(
        scrollX: Int,
        scrollY: Int,
        clampedX: Boolean,
        clampedY: Boolean
    )
}
