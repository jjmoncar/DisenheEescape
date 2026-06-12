package com.example

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdMobManager {
    private const val TAG = "AdMobManager"

    // Test Interstitial ID provided by Google
    private const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    
    // Production Ad Unit ID provided by User
    private const val PROD_AD_UNIT_ID = "ca-app-pub-5820291022612570/1212137754"

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    /**
     * Get the appropriate Ad Unit ID depending on whether the app is in debug/testing or production.
     */
    val adUnitId: String
        get() {
            return if (BuildConfig.DEBUG) {
                Log.d(TAG, "Using AdMob TEST Interstitial unit ID: $TEST_AD_UNIT_ID")
                TEST_AD_UNIT_ID
            } else {
                Log.d(TAG, "Using AdMob PRODUCTION Interstitial unit ID: $PROD_AD_UNIT_ID")
                PROD_AD_UNIT_ID
            }
        }

    /**
     * Initialize the Mobile Ads SDK.
     */
    fun initialize(context: Context) {
        try {
            MobileAds.initialize(context) { status ->
                Log.d(TAG, "MobileAds initialization finished. Status: $status")
                // Start loading the first ad automatically
                loadAd(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds", e)
        }
    }

    /**
     * Load an interstitial ad asynchronously.
     */
    fun loadAd(context: Context) {
        if (interstitialAd != null || isLoading) {
            return
        }

        isLoading = true
        val adRequest = AdRequest.Builder().build()
        val currentId = adUnitId

        Log.d(TAG, "Loading Interstitial Ad with ID: $currentId")

        InterstitialAd.load(
            context.applicationContext,
            currentId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad successfully loaded.")
                    interstitialAd = ad
                    isLoading = false
                    
                    // Set callbacks
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Ad dismissed full screen content.")
                            interstitialAd = null
                            // Load next ad immediately to prepare for subsequent actions
                            loadAd(context)
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "Ad failed to show: ${adError.message}")
                            interstitialAd = null
                            loadAd(context)
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "Ad showed full screen content.")
                        }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Ad failed to load: ${loadAdError.message}")
                    interstitialAd = null
                    isLoading = false
                }
            }
        )
    }

    /**
     * Show the interstitial ad if ready.
     * Invokes [onAdClosed] callback when the ad is dismissed or fails to show.
     */
    fun showAdIfReady(activity: Activity, onAdClosed: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            Log.d(TAG, "Showing Interstitial Ad...")
            // Keep the callback parameter and update dismissed callback
            val originalCallback = ad.fullScreenContentCallback
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    originalCallback?.onAdDismissedFullScreenContent()
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    originalCallback?.onAdFailedToShowFullScreenContent(adError)
                    onAdClosed()
                }

                override fun onAdShowedFullScreenContent() {
                    originalCallback?.onAdShowedFullScreenContent()
                }
            }
            ad.show(activity)
        } else {
            Log.d(TAG, "Interstitial ad not ready yet. Continuing immediately.")
            // Try to trigger a load in case it hasn't succeeded
            loadAd(activity)
            onAdClosed()
        }
    }
}
