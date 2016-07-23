package com.mopub.ads.adapters;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.mojang.base.Helper;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;
import com.unity3d.ads.android.IUnityAdsListener;
import com.unity3d.ads.android.UnityAds;

public class UnityAdsMopubEvents extends CustomEventInterstitial implements IUnityAdsListener {

    private CustomEventInterstitialListener listener = null;
    private String gameId = null;
    private String zoneId = null;
    private Map<String, Object> options = null;
    public static String GAME_ID_KEY= "gameId";
    private static UnityAdsMopubEvents currentShowingWrapper = null;
    private UnityAdsMopubEvents wrapperAfterShow = null;
    private Activity nextActivity = null;
    public static String GAME_ID_VALUE = Helper.convertString("4E6A6B324D7A4D3D");

    @Override
    protected void loadInterstitial(Context context,
                                    CustomEventInterstitialListener customEventInterstitialListener,
                                    Map<String, Object> localExtras, Map<String, String> serverExtras) {
        listener = customEventInterstitialListener;

        if(serverExtras.get(GAME_ID_KEY) == null || !(serverExtras.get(GAME_ID_KEY) instanceof String)) {
            listener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        gameId = serverExtras.get(GAME_ID_KEY);
        zoneId = serverExtras.get("zoneId");

        options = new HashMap<String, Object>();
        options.putAll(localExtras);
        options.putAll(serverExtras);

        if(currentShowingWrapper == null) {
            UnityAds.setDebugMode(true);

            UnityAds.init((Activity)context, gameId, this);
            UnityAds.changeActivity((Activity)context);
            UnityAds.setListener(this);

            if(UnityAds.canShow() && UnityAds.canShowAds()) {
                listener.onInterstitialLoaded();
            }
        } else {
            currentShowingWrapper.setNextWrapper(this);
            nextActivity = (Activity)context;
        }
    }

    private void setNextWrapper(UnityAdsMopubEvents nextWrapper) {
        wrapperAfterShow = nextWrapper;
    }

    private void activateNextWrapper() {
        UnityAds.changeActivity(nextActivity);
        UnityAds.setListener(this);

        if(UnityAds.canShow() && UnityAds.canShowAds()) {
            listener.onInterstitialLoaded();
        }
    }

    @Override
    public void showInterstitial() {
        if(UnityAds.canShow() && UnityAds.canShowAds()) {
            UnityAds.setZone(zoneId);
            if(UnityAds.show(options)) {
                currentShowingWrapper = this;
            } else {
                listener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
        } else {
            listener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
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
    public void onHide() {
        currentShowingWrapper = null;

        if(wrapperAfterShow != null) {
            wrapperAfterShow.activateNextWrapper();
            wrapperAfterShow = null;
        }

        listener.onInterstitialDismissed();
    }

    @Override
    public void onShow() {
        listener.onInterstitialShown();
    }

    @Override
    public void onVideoStarted() {
    }

    @Override
    public void onVideoCompleted(String rewardItemKey, boolean skipped) {
    }

    @Override
    public void onFetchCompleted() {
        listener.onInterstitialLoaded();
    }

    @Override
    public void onFetchFailed() {
        listener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
    }
}
