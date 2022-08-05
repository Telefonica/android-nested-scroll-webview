package com.telefonica.nestedscrollwebviewdemo

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import com.telefonica.nestedscrollwebviewdemo.databinding.ActivityScrollingBinding

class ScrollingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScrollingBinding
    private var coordinatorBottomMatchingEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityScrollingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(findViewById(R.id.toolbar))
        binding.toolbarLayout.title = title
        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        WebView.setWebContentsDebuggingEnabled(true)
        binding.webView.apply {
            with(settings) {
                javaScriptEnabled = true
                domStorageEnabled = true
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    binding.swiperefresh.isRefreshing = false
                    super.onPageFinished(view, url)
                }
            }
            loadUrl(SCROLLABLE_WEB_CONTENT_URL)
        }
        binding.swiperefresh.setOnRefreshListener {
            binding.webView.reload()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_scrolling, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_set_parent_bottom_matching_behaviour)
            ?.isVisible = !coordinatorBottomMatchingEnabled
        menu?.findItem(R.id.action_set_regular_behaviour)
            ?.isVisible = coordinatorBottomMatchingEnabled
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return when (item.itemId) {
            R.id.action_set_parent_bottom_matching_behaviour -> {
                binding.webView.apply {
                    setCoordinatorBottomMatchingBehaviourEnabled(true)
                    loadUrl(SCROLLABLE_WEB_CONTENT_WITH_FOOTER_URL)
                }
                coordinatorBottomMatchingEnabled = true
                true
            }
            R.id.action_set_regular_behaviour -> {
                binding.webView.apply {
                    setCoordinatorBottomMatchingBehaviourEnabled(false)
                    loadUrl(SCROLLABLE_WEB_CONTENT_URL)
                }
                coordinatorBottomMatchingEnabled = false
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private companion object {
        const val SCROLLABLE_WEB_CONTENT_URL =
            "file:///android_asset/scrollable_web_content.html"
        const val SCROLLABLE_WEB_CONTENT_WITH_FOOTER_URL =
            "file:///android_asset/scrollable_web_content_with_footer.html"
    }
}
