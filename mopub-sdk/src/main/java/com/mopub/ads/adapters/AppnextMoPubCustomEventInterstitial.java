package com.mopub.ads.adapters;


import android.content.Context;

import com.appnext.ads.interstitial.Interstitial;
import com.appnext.ads.interstitial.InterstitialConfig;
import com.appnext.core.callbacks.OnAdClicked;
import com.appnext.core.callbacks.OnAdClosed;
import com.appnext.core.callbacks.OnAdError;
import com.appnext.core.callbacks.OnAdLoaded;
import com.mojang.base.Helper;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;

import java.util.Map;

public class AppnextMoPubCustomEventInterstitial extends CustomEventInterstitial implements OnAdClicked, OnAdClosed, OnAdError, OnAdLoaded {

    public static final String PLACEMENT_ID = "placementID";

    private Interstitial interstitial_Ad;
    private CustomEventInterstitialListener mInterstitialListener;

    @Override
    protected void loadInterstitial(Context context, CustomEventInterstitialListener customEventInterstitialListener, Map<String, Object> localExtras, Map<String, String> serverExtras) {

        String placementID = serverExtras.get(PLACEMENT_ID);
        mInterstitialListener = customEventInterstitialListener;

        if(placementID != null && !placementID.isEmpty()) {
            InterstitialConfig config = new InterstitialConfig();
            config.setBackButtonCanClose(true);
            config.setAutoPlay(true);
            config.setMute(true);
            config.setCreativeType(Interstitial.TYPE_MANAGED);

            interstitial_Ad = new Interstitial(context, placementID,config);
            interstitial_Ad.setOnAdClickedCallback(this);
            interstitial_Ad.setOnAdClosedCallback(this);
            interstitial_Ad.setOnAdErrorCallback(this);
            interstitial_Ad.setOnAdLoadedCallback(this);

            interstitial_Ad.loadAd();
        }else {
            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    public void adClicked() {
        mInterstitialListener.onInterstitialClicked();
    }

    @Override
    public void onAdClosed() {
        mInterstitialListener.onInterstitialDismissed();
    }

    @Override
    public void adError(String s) {
        Helper.wtf("Appnext Error " +s);
        mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
    }

    @Override
    public void adLoaded() {
        mInterstitialListener.onInterstitialLoaded();
    }

    @Override
    public void showInterstitial() {
        if (interstitial_Ad != null) {
            interstitial_Ad.showAd();
        } else {
            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NO_FILL);
            Helper.wtf("Wannet to show Appnext but was null ");
        }
    }

    @Override
    protected boolean usesProxy() {
        return false;
    }

    @Override
    protected void onInvalidate() {
        if (interstitial_Ad != null) {
            interstitial_Ad.setOnAdClickedCallback(null);
            interstitial_Ad.setOnAdClosedCallback(null);
            interstitial_Ad.setOnAdErrorCallback(null);
            interstitial_Ad.setOnAdLoadedCallback(null);
        }
    }
}
