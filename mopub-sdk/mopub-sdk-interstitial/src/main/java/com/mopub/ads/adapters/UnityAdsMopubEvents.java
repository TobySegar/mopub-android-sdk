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


public class UnityAdsMopubEvents extends CustomEventInterstitial implements IUnityAdsListener {

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
        Helper.wtf("Loading Unity Ads");
        String gameId = serverExtras.get("gameId");
        this.activity = (Activity)context;
        UnityAds.setListener(this);

        if(!UnityAds.isInitialized()){
            Helper.wtf("Loading Initing unity ads cause not inited before");
            UnityAds.initialize(activity, gameId,this);
        }else if(UnityAds.isReady()){
            Helper.wtf("unity ads already had add so we are loaded");
            listener.onInterstitialLoaded();
        }
    }





    @Override
    public void showInterstitial() {
        Helper.wtf("UnityAds Adapter called show");
        if(UnityAds.isReady() && UnityAds.isInitialized()) {
            Helper.wtf("Should be showing Unity Ads",true);
            UnityAds.show(activity);
        } else {
            Helper.wtf("Failed to show unity ads");
            listener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
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
        Helper.wtf("onUnityAdsReady");
        listener.onInterstitialLoaded();
    }

    @Override
    public void onUnityAdsStart(String placementId) {
        Helper.wtf("onUnityAdsStart");
        listener.onInterstitialShown();
    }

    @Override
    public void onUnityAdsFinish(String placementId, UnityAds.FinishState result) {
        Helper.wtf("onUnityAdsFinish");
        listener.onInterstitialDismissed();
    }

    @Override
    public void onUnityAdsError(UnityAds.UnityAdsError error, String message) {
        Helper.wtf("onUnityAdsError + " + message);
        listener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
    }
}
