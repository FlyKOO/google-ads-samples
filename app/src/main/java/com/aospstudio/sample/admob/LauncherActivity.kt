package com.aospstudio.sample.admob

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aospstudio.sample.admob.ads.AppOpenAdManager

class LauncherActivity : AppCompatActivity() {

    private var hasNavigatedToMain = false
    private var shouldShowAdWhenResumed = false
    private var appOpenAdManager: AppOpenAdManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        appOpenAdManager = application as? AppOpenAdManager
        val application = appOpenAdManager
        if (application == null) {
            startMainActivity()
            return
        }
        application.loadAd(
            this,
            object : AppOpenAdManager.OnLoadAdCompleteListener {
                override fun onLoadAdComplete(success: Boolean) {
                    if (!success || isFinishing || isDestroyed) {
                        startMainActivity()
                        return
                    }
                    attemptToShowAd()
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (shouldShowAdWhenResumed && !hasNavigatedToMain && !isFinishing && !isDestroyed) {
            shouldShowAdWhenResumed = false
            attemptToShowAd()
        }
    }

    private fun attemptToShowAd() {
        val application = appOpenAdManager
        if (application == null) {
            startMainActivity()
            return
        }
        if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
            shouldShowAdWhenResumed = true
            return
        }
        val didShowAd = application.showAdIfAvailable(
            this,
            object : AppOpenAdManager.OnShowAdCompleteListener {
                override fun onShowAdComplete() {
                    startMainActivity()
                }
            }
        )
        if (!didShowAd) {
            startMainActivity()
        }
    }

    private fun startMainActivity() {
        if (hasNavigatedToMain || isFinishing || isDestroyed) {
            return
        }
        hasNavigatedToMain = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
