package com.aospstudio.sample.admob

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aospstudio.sample.admob.ads.AdUnitId
import com.aospstudio.sample.admob.ads.NativeAdCard
import com.aospstudio.sample.admob.ads.NativeAdListItem
import com.aospstudio.sample.admob.network.NetworkMonitorUtil
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

private const val OVER_REWARD = 1

class MainActivity : AppCompatActivity() {

    private lateinit var networkMonitor: NetworkMonitorUtil
    private var consentInformation: ConsentInformation? = null
    private var consentForm: ConsentForm? = null
    private var mAdIsLoading: Boolean = false
    private var interstitialAd: InterstitialAd? = null
    private var coinCount: Int = 0
    private var isLoadingAds = false
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private val coinCountState = mutableStateOf(0)
    private val isBannerVisible = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        networkMonitor = NetworkMonitorUtil(this)

        setContent {
            MaterialTheme {
                MainScreen(
                    earnedCoins = coinCountState.value,
                    onOpenInterstitial = { initLoadInterstitial() },
                    onOpenRewardedInterstitial = { showRewardedVideo() },
                    showBanner = isBannerVisible.value
                )
            }
        }

        networkMonitor.result = { isAvailable, _ ->
            runOnUiThread {
                if (isAvailable) {
                    MobileAds.initialize(this) { }
                    initConsentForm()
                    initStartApp()
                    if (rewardedInterstitialAd == null && !isLoadingAds) {
                        initLoadRewardedInterstitialAd()
                    }
                }
                isBannerVisible.value = isAvailable
            }
        }

        val extras = Bundle()
        extras.putString("npa", "1")

        AdRequest.Builder()
            .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
            .build()
    }

    private fun initConsentForm() {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation!!.requestConsentInfoUpdate(this, params, {
            if (consentInformation!!.isConsentFormAvailable) {
                initLoadForm()
            }
        }, {})
    }

    private fun initLoadForm() {
        UserMessagingPlatform.loadConsentForm(this, { consentForm ->
            this@MainActivity.consentForm = consentForm
            if (consentInformation!!.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                consentForm.show(
                    this@MainActivity
                ) {
                    initLoadForm()
                }
            }
        }) {}
    }

    private fun initInterstitial() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this, AdUnitId.INTERSTITIAL_AD_UNIT_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                    mAdIsLoading = false
                }

                override fun onAdLoaded(mInterstitialAd: InterstitialAd) {
                    interstitialAd = mInterstitialAd
                    mAdIsLoading = false
                }
            }
        )
    }

    private fun initLoadInterstitial() {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    initInterstitial()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                }

                override fun onAdShowedFullScreenContent() {
                }
            }
            interstitialAd?.show(this)
        } else {
            initStartApp()
        }
    }

    private fun initStartApp() {
        if (!mAdIsLoading && interstitialAd == null) {
            mAdIsLoading = true
            initInterstitial()
        }
    }

    private fun initLoadRewardedInterstitialAd() {
        if (rewardedInterstitialAd == null) {
            isLoadingAds = true
            val adRequest = AdRequest.Builder().build()

            RewardedInterstitialAd.load(
                this,
                AdUnitId.REWARD_AD_UNIT_ID,
                adRequest,
                object : RewardedInterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        super.onAdFailedToLoad(adError)
                        isLoadingAds = false
                        rewardedInterstitialAd = null
                    }

                    override fun onAdLoaded(rewardedAd: RewardedInterstitialAd) {
                        super.onAdLoaded(rewardedAd)
                        rewardedInterstitialAd = rewardedAd
                        isLoadingAds = false
                    }
                })
        }
    }

    private fun addCoins(coins: Int) {
        coinCount += coins
        coinCountState.value = coinCount
    }

    private fun showRewardedVideo() {
        if (rewardedInterstitialAd == null) {
            if (!isLoadingAds) {
                initLoadRewardedInterstitialAd()
            }
            return
        }

        rewardedInterstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedInterstitialAd = null
                initLoadRewardedInterstitialAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedInterstitialAd = null
            }

            override fun onAdShowedFullScreenContent() {
            }
        }

        rewardedInterstitialAd?.show(
            this
        ) { _ ->
            addCoins(OVER_REWARD)
        }
    }

    override fun onResume() {
        networkMonitor.register()
        super.onResume()
    }

    override fun onPause() {
        networkMonitor.unregister()
        super.onPause()
    }
}

@Composable
private fun MainScreen(
    earnedCoins: Int,
    onOpenInterstitial: () -> Unit,
    onOpenRewardedInterstitial: () -> Unit,
    showBanner: Boolean
) {
    val sampleItems = remember { List(12) { it + 1 } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            AdActionSection(
                earnedCoins = earnedCoins,
                onOpenInterstitial = onOpenInterstitial,
                onOpenRewardedInterstitial = onOpenRewardedInterstitial
            )
        }
        if (showBanner) {
            item {
                BannerAd(
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }
        item {
            SectionTitle(text = stringResource(R.string.native_ad_card_title))
        }
        item {
            NativeAdCard(
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            SectionTitle(text = stringResource(R.string.native_ad_list_title))
        }
        itemsIndexed(sampleItems) { index, item ->
            Column(modifier = Modifier.fillMaxWidth()) {
                SampleListItem(position = item)
                if ((index + 1) % 4 == 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    NativeAdListItem(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun BannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val adWidth = configuration.screenWidthDp
    val adSize = remember(configuration.screenWidthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    }
    val adRequest = remember { AdRequest.Builder().build() }

    AndroidView(
        modifier = modifier,
        factory = {
            AdView(context).apply {
                adUnitId = AdUnitId.BANNER_AD_UNIT_ID
                setAdSize(adSize)
                loadAd(adRequest)
            }
        },
        update = { view ->
            if (view.adSize != adSize) {
                view.setAdSize(adSize)
                view.loadAd(adRequest)
            }
        }
    )
}

@Composable
private fun AdActionSection(
    earnedCoins: Int,
    onOpenInterstitial: () -> Unit,
    onOpenRewardedInterstitial: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onOpenInterstitial) {
            Text(text = stringResource(R.string.open_interstitial_ad))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onOpenRewardedInterstitial) {
            Text(text = stringResource(R.string.open_rewarded_interstitial_ad))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.earned_count, earnedCoins),
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.subtitle1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    )
}

@Composable
private fun SampleListItem(position: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Text(
            text = stringResource(R.string.sample_list_title, position),
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(16.dp)
        )
    }
}
