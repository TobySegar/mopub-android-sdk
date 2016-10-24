package com.mopub.ads.adapters;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;

import com.mojang.base.Helper;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;
import com.unity3d.ads.IUnityAdsListener;
import com.unity3d.ads.UnityAds;


public class UnityAdsMopub extends CustomEventInterstitial implements IUnityAdsListener {

    private CustomEventInterstitialListener listener = null;
    private Activity activity;

    @Override
    protected void loadInterstitial(Context context,
                                    CustomEventInterstitialListener customEventInterstitialListener,
                                    Map<String, Object> localExtras, Map<String, String> serverExtras) {
        listener = customEventInterstitialListener;

        if(serverExtras.get("gameId") == null || serverExtras.get("gameId") == null) {
            listener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        String gameId = serverExtras.get("gameId");
        this.activity = (Activity)context;
        UnityAds.setListener(this);

        if(!UnityAds.isInitialized()){
            UnityAds.initialize(activity, gameId,this);
        }else if(UnityAds.isReady()){
            listener.onInterstitialLoaded();
        }

    }





    @Override
    public void showInterstitial() {
        if(UnityAds.isReady() && UnityAds.isInitialized()) {
            UnityAds.show(activity);
        } else {
            listener.onInterstitialFailed(MoPubErrorCode.NO_FILL);
        }
    }

    @Override
    protected boolean usesProxy() {
        return false;
    }

    @Override
    protected void onInvalidate() {
        activity = null;
    }



    @Override
    public void onUnityAdsReady(String placementId) {
        listener.onInterstitialLoaded();
    }

    @Override
    public void onUnityAdsStart(String placementId) {
        listener.onInterstitialShown();
    }

    @Override
    public void onUnityAdsFinish(String placementId, UnityAds.FinishState result) {
        listener.onInterstitialDismissed();
    }

    @Override
    public void onUnityAdsError(UnityAds.UnityAdsError error, String message) {
        Helper.wtf(message);
        listener.onInterstitialFailed(MoPubErrorCode.NO_FILL);
    }
}
