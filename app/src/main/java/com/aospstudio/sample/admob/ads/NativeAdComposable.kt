package com.aospstudio.sample.admob.ads

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aospstudio.sample.admob.R
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    val state = rememberNativeAdState(adUnitId = AdUnitId.NATIVE_AD_UNIT_ID)
    NativeAdContainer(modifier = modifier, state = state)
}

@Composable
fun NativeAdListItem(modifier: Modifier = Modifier) {
    val state = rememberNativeAdState(adUnitId = AdUnitId.NATIVE_AD_UNIT_ID)
    NativeAdContainer(modifier = modifier, state = state)
}

@Composable
private fun NativeAdContainer(
    modifier: Modifier = Modifier,
    state: NativeAdState
) {
    val nativeAd = state.nativeAd
    if (nativeAd != null) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                LayoutInflater.from(context).inflate(R.layout.native_ad_card, null).apply {
                    val adView: NativeAdView = findViewById(R.id.native_ad_view)
                    populateNativeAdView(adView, nativeAd)
                }
            },
            update = { view ->
                val adView: NativeAdView = view.findViewById(R.id.native_ad_view)
                populateNativeAdView(adView, nativeAd)
            }
        )
    } else {
        AdPlaceholder(modifier = modifier)
    }
}

@Composable
private fun AdPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.native_ad_loading),
                style = MaterialTheme.typography.body2
            )
        }
    }
}

@Composable
fun rememberNativeAdState(adUnitId: String): NativeAdState {
    val context = LocalContext.current
    val state = remember(adUnitId) { NativeAdState(context, adUnitId) }

    LaunchedEffect(state.isLoading, state.nativeAd) {
        state.ensureAdLoaded()
    }

    DisposableEffect(state) {
        onDispose { state.destroy() }
    }

    return state
}

class NativeAdState internal constructor(
    private val context: Context,
    private val adUnitId: String
) {
    var nativeAd: NativeAd? by mutableStateOf(null)
        private set

    var isLoading: Boolean by mutableStateOf(false)
        private set

    private var adLoader: AdLoader? = null

    fun ensureAdLoaded() {
        if (nativeAd != null || isLoading) {
            return
        }
        loadAd()
    }

    private fun loadAd() {
        isLoading = true
        adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                nativeAd?.destroy()
                nativeAd = ad
                isLoading = false
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    nativeAd?.destroy()
                    nativeAd = null
                    isLoading = false
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setVideoOptions(
                        VideoOptions.Builder()
                            .setStartMuted(true)
                            .build()
                    )
                    .build()
            )
            .build()
        adLoader?.loadAd(AdRequest.Builder().build())
    }

    fun destroy() {
        adLoader = null
        nativeAd?.destroy()
        nativeAd = null
        isLoading = false
    }
}

private fun populateNativeAdView(adView: NativeAdView, nativeAd: NativeAd) {
    adView.mediaView = adView.findViewById(R.id.ad_media)
    adView.headlineView = adView.findViewById(R.id.ad_headline)
    adView.bodyView = adView.findViewById(R.id.ad_body)
    adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
    adView.iconView = adView.findViewById(R.id.ad_app_icon)
    adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

    (adView.headlineView as? TextView)?.text = nativeAd.headline
    adView.mediaView?.setMediaContent(nativeAd.mediaContent)

    val body = nativeAd.body
    if (body.isNullOrEmpty()) {
        adView.bodyView?.visibility = View.GONE
    } else {
        adView.bodyView?.visibility = View.VISIBLE
        (adView.bodyView as? TextView)?.text = body
    }

    val callToAction = nativeAd.callToAction
    if (callToAction.isNullOrEmpty()) {
        adView.callToActionView?.visibility = View.GONE
    } else {
        adView.callToActionView?.visibility = View.VISIBLE
        (adView.callToActionView as? Button)?.text = callToAction
    }

    val icon = nativeAd.icon
    val iconView = adView.iconView as? ImageView
    if (icon == null) {
        iconView?.visibility = View.GONE
    } else {
        iconView?.setImageDrawable(icon.drawable)
        iconView?.visibility = View.VISIBLE
    }

    val advertiser = nativeAd.advertiser
    val advertiserView = adView.advertiserView as? TextView
    if (advertiser.isNullOrEmpty()) {
        advertiserView?.visibility = View.GONE
    } else {
        advertiserView?.text = advertiser
        advertiserView?.visibility = View.VISIBLE
    }

    adView.setNativeAd(nativeAd)
}
