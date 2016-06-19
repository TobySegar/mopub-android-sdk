package com.mopub.ads.adapters;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.mojang.base.Helper;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;

import java.util.Map;

/*
 * Compatible with version 7.8.0 of the Google Play Services SDK.
 */

// Note: AdMob ads will now use this class as Google has deprecated the AdMob SDK.

public class GooglePlayServicesInterstitial extends CustomEventInterstitial {
    /*
     * These keys are intended for MoPub internal use. Do not modify.
     */
    public static final String AD_UNIT_ID_KEY = "adUnitID";
    public static final String LOCATION_KEY = "location";

    private CustomEventInterstitialListener mInterstitialListener;
    private InterstitialAd mGoogleInterstitialAd;
    private final String debugIntID = Helper.convertString("59324574595842774C5842315969307A4F5451774D6A55324D446B354F5451794E5451304C7A45774D7A4D784E7A4D334D54493D");

    @Override
    protected void loadInterstitial(final Context context, final CustomEventInterstitialListener customEventInterstitialListener,
                                    final Map<String, Object> localExtras, final Map<String, String> serverExtras) {
        mInterstitialListener = customEventInterstitialListener;
        final String adUnitId;

        if (extrasAreValid(serverExtras)) {
            adUnitId = Helper.DEBUG ? debugIntID : serverExtras.get(AD_UNIT_ID_KEY);
        } else {
            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        mGoogleInterstitialAd = new InterstitialAd(context);
        mGoogleInterstitialAd.setAdListener(new InterstitialAdListener());
        mGoogleInterstitialAd.setAdUnitId(adUnitId);

        final AdRequest adRequest = new AdRequest.Builder()
                .setRequestAgent("MoPub")
                .addTestDevice("E883C2BB7DE538BAADA96556402DA41F")
                .build();

        try {
            mGoogleInterstitialAd.loadAd(adRequest);
        } catch (NoClassDefFoundError e) {
            // This can be thrown by Play Services on Honeycomb.
            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    public void showInterstitial() {
        if (mGoogleInterstitialAd.isLoaded()) {
            mGoogleInterstitialAd.show();
        } else {
            Helper.wtf("MoPub", "Tried to show a Google Play Services interstitial ad before it finished loading. Please try again.");
        }
    }

    @Override
    protected boolean usesProxy() {
        return true;
    }

    @Override
    protected void onInvalidate() {
        if (mGoogleInterstitialAd != null) {
            mGoogleInterstitialAd.setAdListener(null);
        }
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras.containsKey(AD_UNIT_ID_KEY);
    }

    private class InterstitialAdListener extends AdListener {
        /*
    	 * Google Play Services AdListener implementation
    	 */
        @Override
        public void onAdClosed() {
            Helper.wtf("MoPub", "Google Play Services interstitial ad dismissed.");
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialDismissed();
            }
        }

        @Override
        public void onAdFailedToLoad(int errorCode) {
            Helper.wtf("MoPub", "Google Play Services interstitial ad failed to load.");
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
        }

        @Override
        public void onAdLeftApplication() {
            Helper.wtf("MoPub", "Google Play Services interstitial ad clicked.");
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialClicked();
            }
        }

        @Override
        public void onAdLoaded() {
            Helper.wtf("MoPub", "Google Play Services interstitial ad loaded successfully.");
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialLoaded();
            }
        }

        @Override
        public void onAdOpened() {
            Helper.wtf("MoPub", "Showing Google Play Services interstitial ad.");
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialShown();
            }
        }
    }

    @Deprecated // for testing
    InterstitialAd getGoogleInterstitialAd() {
        return mGoogleInterstitialAd;
    }
}
