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
