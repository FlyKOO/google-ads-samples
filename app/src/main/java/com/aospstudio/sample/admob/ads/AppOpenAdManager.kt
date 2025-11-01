package com.aospstudio.sample.admob.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import java.util.Date

open class AppOpenAdManager : Application(), Application.ActivityLifecycleCallbacks,
    LifecycleObserver {

    private var appOpenAdManager: AppOpenAdController? = null
    private var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appOpenAdManager = AppOpenAdController()
        appOpenAdManager?.loadAd(this)
        registerActivityLifecycleCallbacks(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        currentActivity?.let {
            appOpenAdManager?.showAdIfAvailable(it)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        appOpenAdManager?.loadAd(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        if (appOpenAdManager?.isShowingAd != true) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    fun showAdIfAvailable(
        activity: Activity,
        onShowAdCompleteListener: OnShowAdCompleteListener
    ): Boolean {
        return appOpenAdManager?.showAdIfAvailable(activity, onShowAdCompleteListener) ?: false
    }

    fun loadAd(
        context: Context,
        onLoadAdCompleteListener: OnLoadAdCompleteListener? = null
    ) {
        appOpenAdManager?.loadAd(context, onLoadAdCompleteListener)
    }

    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }

    interface OnLoadAdCompleteListener {
        fun onLoadAdComplete(success: Boolean)
    }

    private inner class AppOpenAdController {

        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd = false

        private var loadTime: Long = 0
        private val pendingLoadListeners = mutableListOf<OnLoadAdCompleteListener>()

        fun loadAd(
            context: Context,
            onLoadAdCompleteListener: OnLoadAdCompleteListener? = null
        ) {
            if (isAdAvailable()) {
                onLoadAdCompleteListener?.onLoadAdComplete(true)
                return
            }

            onLoadAdCompleteListener?.let { pendingLoadListeners.add(it) }

            if (isLoadingAd) {
                return
            }

            isLoadingAd = true
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                context,
                AdUnitId.APPOPEN_AD_UNIT_ID,
                request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        isLoadingAd = false
                        loadTime = Date().time
                        notifyPendingLoadListeners(true)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isLoadingAd = false
                        notifyPendingLoadListeners(false)
                    }
                })
        }

        private fun notifyPendingLoadListeners(success: Boolean) {
            if (pendingLoadListeners.isEmpty()) {
                return
            }
            val listeners = pendingLoadListeners.toList()
            pendingLoadListeners.clear()
            listeners.forEach { it.onLoadAdComplete(success) }
        }

        private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
            val dateDifference: Long = Date().time - loadTime
            val numMilliSecondsPerHour: Long = 3600000
            return dateDifference < numMilliSecondsPerHour * numHours
        }

        private fun isAdAvailable(): Boolean {
            return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
        }

        fun showAdIfAvailable(activity: Activity): Boolean {
            return showAdIfAvailable(
                activity,
                object : OnShowAdCompleteListener {
                    override fun onShowAdComplete() {
                    }
                }
            )
        }

        fun showAdIfAvailable(
            activity: Activity,
            onShowAdCompleteListener: OnShowAdCompleteListener
        ): Boolean {
            if (isShowingAd) {
                return true
            }

            if (!isAdAvailable()) {
                onShowAdCompleteListener.onShowAdComplete()
                loadAd(activity)
                return false
            }

            appOpenAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowingAd = false

                    onShowAdCompleteListener.onShowAdComplete()
                    loadAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    isShowingAd = false

                    onShowAdCompleteListener.onShowAdComplete()
                    loadAd(activity)
                }

                override fun onAdShowedFullScreenContent() {
                }
            }
            isShowingAd = true
            appOpenAd!!.show(activity)
            return true
        }
    }
}
