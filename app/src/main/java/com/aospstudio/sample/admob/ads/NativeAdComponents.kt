package com.aospstudio.sample.admob.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aospstudio.sample.admob.R
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    NativeAdComposable(
        adUnitId = AdUnitId.NATIVE_CARD_AD_UNIT_ID,
        layoutRes = R.layout.native_ad_card,
        modifier = modifier,
        enableVideo = true
    )
}

@Composable
fun NativeAdListItem(modifier: Modifier = Modifier) {
    NativeAdComposable(
        adUnitId = AdUnitId.NATIVE_LIST_AD_UNIT_ID,
        layoutRes = R.layout.native_ad_list_item,
        modifier = modifier
    )
}

@Composable
private fun NativeAdComposable(
    adUnitId: String,
    @LayoutRes layoutRes: Int,
    modifier: Modifier = Modifier,
    enableVideo: Boolean = false
) {
    val context = LocalContext.current
    val nativeAdState = remember { mutableStateOf<NativeAd?>(null) }
    val isLoadingState = remember { mutableStateOf(true) }

    DisposableEffect(adUnitId) {
        isLoadingState.value = true
        val builder = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                nativeAdState.value?.destroy()
                nativeAdState.value = nativeAd
                isLoadingState.value = false
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isLoadingState.value = false
                }
            })

        if (enableVideo) {
            val videoOptions = VideoOptions.Builder()
                .setStartMuted(true)
                .build()
            val adOptions = NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .build()
            builder.withNativeAdOptions(adOptions)
        }

        val adLoader = builder.build()
        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            nativeAdState.value?.destroy()
            nativeAdState.value = null
        }
    }

    val nativeAd = nativeAdState.value
    if (nativeAd == null) {
        Surface(modifier = modifier, elevation = 4.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoadingState.value) {
                    CircularProgressIndicator()
                }
            }
        }
    } else {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                inflateNativeAdLayout(ctx, layoutRes)
            },
            update = { adView ->
                populateNativeAdView(nativeAd, adView)
            }
        )
    }
}

private fun inflateNativeAdLayout(
    context: android.content.Context,
    @LayoutRes layoutRes: Int
): NativeAdView {
    val inflater = LayoutInflater.from(context)
    val view = inflater.inflate(layoutRes, null)
    return view as NativeAdView
}

private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
    val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
    val bodyView = adView.findViewById<TextView>(R.id.ad_body)
    val callToActionView = adView.findViewById<Button>(R.id.ad_call_to_action)
    val iconView = adView.findViewById<ImageView>(R.id.ad_app_icon)
    val advertiserView = adView.findViewById<TextView>(R.id.ad_advertiser)
    val priceView = adView.findViewById<TextView>(R.id.ad_price)
    val storeView = adView.findViewById<TextView>(R.id.ad_store)
    val starRatingView = adView.findViewById<RatingBar>(R.id.ad_stars)

    adView.headlineView = headlineView
    headlineView?.text = nativeAd.headline

    adView.mediaView = mediaView
    mediaView?.mediaContent = nativeAd.mediaContent
    mediaView?.setImageScaleType(ImageView.ScaleType.CENTER_CROP)

    adView.bodyView = bodyView
    setTextOrHide(bodyView, nativeAd.body)

    adView.callToActionView = callToActionView
    if (callToActionView != null) {
        setTextOrHide(callToActionView, nativeAd.callToAction)
    }

    adView.iconView = iconView
    val icon = nativeAd.icon
    if (iconView != null) {
        if (icon == null) {
            iconView.visibility = View.GONE
        } else {
            iconView.setImageDrawable(icon.drawable)
            iconView.visibility = View.VISIBLE
        }
    }

    adView.advertiserView = advertiserView
    setTextOrHide(advertiserView, nativeAd.advertiser)

    adView.priceView = priceView
    setTextOrHide(priceView, nativeAd.price)

    adView.storeView = storeView
    setTextOrHide(storeView, nativeAd.store)

    adView.starRatingView = starRatingView
    if (starRatingView != null) {
        val rating = nativeAd.starRating
        if (rating == null) {
            starRatingView.visibility = View.GONE
        } else {
            starRatingView.visibility = View.VISIBLE
            starRatingView.rating = rating.toFloat()
        }
    }

    adView.setNativeAd(nativeAd)
}

private fun setTextOrHide(view: TextView?, text: CharSequence?) {
    if (view == null) {
        return
    }
    if (text.isNullOrEmpty()) {
        view.visibility = View.GONE
    } else {
        view.text = text
        view.visibility = View.VISIBLE
    }
}
