package com.mopub.ads.adapters;

import android.app.Activity;
import android.content.Context;

import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import com.unity3d.ads.UnityAds;

import java.util.Map;

public class UnityInterstitial extends CustomEventInterstitial implements IUnityAdsExtendedListener {

    private CustomEventInterstitialListener mCustomEventInterstitialListener;
    private Context mContext;
    private String mPlacementId = "video";
    private boolean loadRequested = false;

    @Override
    protected void loadInterstitial(Context context,
                                    CustomEventInterstitialListener customEventInterstitialListener,
                                    Map<String, Object> localExtras,
                                    Map<String, String> serverExtras) {

        mPlacementId = UnityRouter.placementIdForServerExtras(serverExtras, mPlacementId);
        mCustomEventInterstitialListener = customEventInterstitialListener;
        mContext = context;
        loadRequested = true;

        try {
            UnityRouter.addListener(mPlacementId, this);
            initializeUnityAdsSdk(serverExtras);
            if (UnityAds.isReady(mPlacementId)) {
                mCustomEventInterstitialListener.onInterstitialLoaded();
                loadRequested = false;
            }
        } catch (UnityRouter.UnityAdsException e) {
            mCustomEventInterstitialListener.onInterstitialFailed(UnityRouter.UnityAdsUtils.getMoPubErrorCode(e.getErrorCode()));
        }
    }

    private void initializeUnityAdsSdk(Map<String, String> serverExtras) {
        if (!UnityAds.isInitialized()) {
            if (!(mContext instanceof Activity)) {
                MoPubLog.e("Context is null or is not an instanceof Activity.");
                return;
            }
            UnityRouter.initUnityAds(serverExtras, (Activity) mContext);
        }
    }

    @Override
    public void showInterstitial() {
        if (UnityAds.isReady(mPlacementId) && mContext != null) {
            UnityAds.show((Activity) mContext, mPlacementId);
        } else {
            MoPubLog.d("Attempted to show Unity interstitial video before it was available.");
        }
    }

    @Override
    protected boolean usesProxy() {
        return false;
    }

    @Override
    protected void onInvalidate() {
        UnityRouter.removeListener(mPlacementId);
    }

    @Override
    public void onUnityAdsReady(String placementId) {
        if (loadRequested) {
            mCustomEventInterstitialListener.onInterstitialLoaded();
            loadRequested = false;
        }
    }

    @Override
    public void onUnityAdsStart(String placementId) {
        mCustomEventInterstitialListener.onInterstitialShown();
    }

    @Override
    public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
        if (finishState == UnityAds.FinishState.ERROR) {
            MoPubLog.d("Unity interstitial video encountered a playback error for placement " + placementId);
            mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
        } else {
            MoPubLog.d("Unity interstitial video completed for placement " + placementId);
            mCustomEventInterstitialListener.onInterstitialDismissed();
        }
        UnityRouter.removeListener(placementId);
    }

    @Override
    public void onUnityAdsClick(String placementId) {
        mCustomEventInterstitialListener.onInterstitialClicked();
    }


    // @Override
    public void onUnityAdsPlacementStateChanged(String placementId, UnityAds.PlacementState oldState, UnityAds.PlacementState newState) {
    }

    @Override
    public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String message) {
        MoPubLog.d("Unity interstitial video cache failed for placement " + mPlacementId + ".");
        MoPubErrorCode errorCode = UnityRouter.UnityAdsUtils.getMoPubErrorCode(unityAdsError);
        mCustomEventInterstitialListener.onInterstitialFailed(errorCode);
    }
}