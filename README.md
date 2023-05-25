<p>
    <img src="https://img.shields.io/badge/Platform-Android-brightgreen" />
    <img src="https://maven-badges.herokuapp.com/maven-central/com.telefonica/nestedscrollwebview/badge.png" />
    <img src="https://img.shields.io/badge/Support-%3E%3D%20Android%205.0-brightgreen" />
</p>

# Android Nested Scroll WebView

Android WebView implementation for nested scrolling layouts.

## Introduction

In case you have a native application that uses a collapsing toolbar layout, you may need to wrap your webview into a [NestedScrollingView](https://developer.android.com/reference/androidx/core/widget/NestedScrollView) to handle correctly nested scrolling to expand/collapse the toolbar.

This is usually done like this:
```xml
<androidx.core.widget.NestedScrollView
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:layout_behavior="@string/appbar_scrolling_view_behavior">

    <WebView
        android:id="@+id/webView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</androidx.core.widget.NestedScrollView>
```

You may quickly realize that **webview viewport height takes all content size height**. So, for all effects, you are using a browser window with an infinite viewport height, which has important implications:
* If page content is long, this may suppose performance penalties as whole page must be rendered.
* In case of scrolling pages where new content is continuously loaded as user scrolls down the page, it will trigger all these loads automatically, this could end on serious performance problems. Think about a "infinite" news feed for example.
* Page content based on viewport height may be displayed wrongly. Think about on fixed footer elements for example, these will be always displayed at the end of the scrollable content.

If you are thinking on using the `android:fillViewport="false"` property for your NestedScrollView, you won't be able to perform any scroll on your webview content...

NestedScrollWebview implementation avoids this "height issue", making webview to have correct viewport size, while working with a collapsable toolbar layout.

## How to use it?

Just include directly the `NestedScrollWebView` in your layout:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.google.android.material.appbar.AppBarLayout
        android:id="@+id/app_bar"
        android:layout_width="match_parent"
        android:layout_height="@dimen/app_bar_height"
        android:theme="@style/Theme.NestedScrollWebViewDemo.AppBarOverlay">

        <com.google.android.material.appbar.CollapsingToolbarLayout
            android:id="@+id/toolbar_layout"
            style="@style/Widget.MaterialComponents.Toolbar.Primary"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            app:contentScrim="?attr/colorPrimary"
            app:layout_scrollFlags="scroll|exitUntilCollapsed"
            app:toolbarId="@+id/toolbar">

            <androidx.appcompat.widget.Toolbar
                android:id="@+id/toolbar"
                android:layout_width="match_parent"
                android:layout_height="?attr/actionBarSize"
                app:layout_collapseMode="pin"
                app:popupTheme="@style/Theme.NestedScrollWebViewDemo.PopupOverlay" />

        </com.google.android.material.appbar.CollapsingToolbarLayout>
    </com.google.android.material.appbar.AppBarLayout>

    <com.telefonica.nestedscrollwebview.NestedScrollWebView
        android:id="@+id/webView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

## Configuration

Utility methods for specific WebView content requirements.

### Adjust WebView bottom margin to match CoordinatorLayout visible space

When expanding toolbar inside a CoordinatorLayout, instead of resizing the space left by the toolbar, it pushes down the content below.

This may be an issue for example for pages where a fixed footer element is displayed. So, in order to correctly resize the webview viewport on these expands you can add this attribute to the NestedScrollWebView.

`app:coordinatorBottomMatchingEnabled={"true"|"false"}`

This is **disabled by default**, as webview resizing is expensive.

```xml
<com.telefonica.nestedscrollwebview.NestedScrollWebView
    android:id="@+id/webView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:coordinatorBottomMatchingEnabled="true"
    app:layout_behavior="@string/appbar_scrolling_view_behavior" />
```

### Block vertical nested scrolling on WebView internal elements scrolls

If your WebView content includes scrollable elements such as horizontal carousels or map views, nested scrolling should not be performed when scrolling these.

`app:blockNestedScrollingOnInternalContentScrolls={"true"|"false"}`

This is **enabled by default**, but can be disabled with this flag in case it does not work properly in any specific situation.

```xml
<com.telefonica.nestedscrollwebview.NestedScrollWebView
    android:id="@+id/webView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:blockNestedScrollingOnInternalContentScrolls="false"
    app:layout_behavior="@string/appbar_scrolling_view_behavior" />
```

## How we do it?

Implementation extends [WebView](https://developer.android.com/reference/android/webkit/WebView) applying nested scrolling code logic from androidx.core [NestedScrollView](https://developer.android.com/reference/androidx/core/widget/NestedScrollView).

## Videos
<table>
<tr>
<td>
<video src="https://user-images.githubusercontent.com/5360064/198062480-4f3d6908-fdcf-446f-bc8c-625635b308f9.mp4" />
</td>
<td>
<video src="https://user-images.githubusercontent.com/5360064/198062613-f77eaf9b-bf6b-48aa-b514-b86ccc910102.mp4" />
</td>
</tr>
</table>

# Usage

To include the library add to your app's `build.gradle`:

```gradle
implementation 'com.telefonica:nestedscrollwebview:{version}'
```
