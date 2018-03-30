package com.mopub.ads.adapters;


import android.app.Activity;
import android.content.Context;

import com.heyzap.sdk.ads.HeyzapAds;
import com.heyzap.sdk.ads.InterstitialAd;
import com.mojang.base.Helper;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;

import java.util.Map;

public class HeyzapInterstitial extends CustomEventInterstitial implements HeyzapAds.NetworkCallbackListener, HeyzapAds.OnStatusListener {
    private static final String KEY_ID = "key";
    private String id;
    private Activity activity;
    private CustomEventInterstitialListener mInterstitialListener;

    @Override
    protected void loadInterstitial(Context context, CustomEventInterstitialListener customEventInterstitialListener, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        mInterstitialListener = customEventInterstitialListener;

        if (context instanceof Activity) {
            activity = (Activity) context;
        } else {
            Helper.wtf("Cound not load heyzap because context is not instance of activity");
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NO_FILL);
            }
            return;
        }

        if (id == null) {
            id = extractIdFromExtras(serverExtras);
            HeyzapAds.start(id, activity,HeyzapAds.DISABLE_AUTOMATIC_FETCH,this);
        }

        InterstitialAd.fetch();
    }

    private String extractIdFromExtras(Map<String, String> serverExtras) {
        if (serverExtras.containsKey(KEY_ID)) {
            return serverExtras.get(KEY_ID);
        } else {
            Helper.wtf("Failed to extract key from heyzap server extrass");
            return null;
        }
    }

    @Override
    public void showInterstitial() {
        if (InterstitialAd.isAvailable()) {
            Helper.wtf("Showing Heyzap");
            InterstitialAd.display(activity);
        } else {
            Helper.wtf("Wanted to show heyzap but wasnt avaible");
        }
    }

    @Override
    protected boolean usesProxy() {
        return false;
    }

    @Override
    protected void onInvalidate() {

    }

    @Override
    public void onNetworkCallback(String s, String s1) {
        Helper.wtf("Heyzap network callback " + s + " : " + s1);
    }

    @Override
    public void onShow(String s) {
        if (mInterstitialListener != null) {
            mInterstitialListener.onInterstitialShown();
        }
    }

    @Override
    public void onClick(String s) {
        if (mInterstitialListener != null) {
            mInterstitialListener.onInterstitialClicked();
        }
    }

    @Override
    public void onHide(String s) {
        if (mInterstitialListener != null) {
            mInterstitialListener.onInterstitialDismissed();
        }
    }

    @Override
    public void onFailedToShow(String s) {
        if (mInterstitialListener != null) {
            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NO_FILL);
        }
    }

    @Override
    public void onAvailable(String s) {
        if (mInterstitialListener != null) {
            mInterstitialListener.onInterstitialLoaded();
        }
    }

    @Override
    public void onFailedToFetch(String s) {
        if (mInterstitialListener != null) {
            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NO_FILL);
        }
    }

    @Override
    public void onAudioStarted() {

    }

    @Override
    public void onAudioFinished() {

    }
}