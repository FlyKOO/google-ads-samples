package com.aospstudio.sample.admob

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aospstudio.sample.admob.ads.AppOpenAdManager

class LauncherActivity : AppCompatActivity() {

    private var hasNavigatedToMain = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        val application = application as? AppOpenAdManager
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
                    val didShowAd = application.showAdIfAvailable(
                        this@LauncherActivity,
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
            }
        )
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
