package com.mopub.ads.adapters;

import android.app.Activity;
import android.content.Context;

import com.mojang.base.Helper;
import com.mojang.base.Logger;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;

import java.util.Map;


public class UnityAdsMopubEvents extends CustomEventInterstitial implements IUnityAdsExtendedListener {

    private CustomEventInterstitialListener mopubListener;
    private static boolean sInitialized = false;
    private Activity mLauncherActivity;
    private boolean sAdCached;


    @Override
    protected void loadInterstitial(Context context,
                                    CustomEventInterstitialListener customEventInterstitialListener,
                                    Map<String, Object> localExtras,
                                    Map<String, String> serverExtras) {

        Logger.Log("::Loading Unity Ads");
        final String mGameId = serverExtras.get("gameId");

        if(mGameId == null || mGameId.isEmpty()){
            Logger.Log("::No game id bailing out");
            mopubListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        if (context == null || !(context instanceof Activity)) {
            mopubListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            return;
        }

        mLauncherActivity = (Activity) context;
        mopubListener = customEventInterstitialListener;

        if (!sInitialized || !UnityAds.isInitialized()) {
            Logger.Log("::Unity Debug");
            Helper.runOnWorkerThread(new Runnable() {
                @Override
                public void run() {
                    UnityAds.setDebugMode(true);
                    UnityAds.initialize(mLauncherActivity, mGameId, UnityAdsMopubEvents.this, Helper.USE_UNITY_TEST_ADS);
                }
            });
            UnityAds.setListener(this);

            sInitialized = true;
        } else {
            UnityAds.setListener(this);

            if (UnityAds.isReady()) {
                mopubListener.onInterstitialLoaded();
            } else {
                sAdCached = false;
            }
        }
    }


    @Override
    public void showInterstitial() {
        if (UnityAds.isReady() && mLauncherActivity != null) {
            Logger.Log("::Showing Unity Ads");
            UnityAds.show(mLauncherActivity);
        } else {
            Logger.Log("::Failed to show unity ads isReady = " + UnityAds.isReady() + "::activity null = " + mLauncherActivity);
            mopubListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    protected boolean usesProxy() {
        return false;
    }

    @Override
    protected void onInvalidate() {
        UnityAds.setListener(null);
    }


    @Override
    public void onUnityAdsReady(String placementId) {
        Logger.Log("::onUnityAdsReady");
        if (!sAdCached) {
            sAdCached = true;
            mopubListener.onInterstitialLoaded();
        }
    }

    @Override
    public void onUnityAdsStart(String placementId) {
        Logger.Log("::onUnityAdsStart");
        mopubListener.onInterstitialShown();
    }

    @Override
    public void onUnityAdsFinish(String placementId, UnityAds.FinishState result) {
        Logger.Log("::onUnityAdsFinish");
        mopubListener.onInterstitialDismissed();
    }

    @Override
    public void onUnityAdsError(UnityAds.UnityAdsError error, String message) {
        Logger.Log("::onUnityAdsError + "+ error +":: message:"+ message);
        mopubListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
    }

    @Override
    public void onUnityAdsClick(String placementId) {
        Logger.Log("::onUnityAdsClick + " + placementId);
        mopubListener.onInterstitialClicked();
    }

    @Override
    public void onUnityAdsPlacementStateChanged(String placementId, UnityAds.PlacementState oldState, UnityAds.PlacementState newState) {

    }

}
