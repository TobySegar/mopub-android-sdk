package com.mopub.ads.adapters;


import android.app.Activity;
import android.content.Context;
import android.util.Log;


import com.mojang.base.Helper;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;

import java.util.Map;

public class HeyzapInterstitialBROKEN{

}
//extends CustomEventInterstitial implements HeyzapAds.OnStatusListener, HeyzapAds.NetworkCallbackListener {
//
//    public static final String PLACEMENT_ID = "placementID";
//
//    private CustomEventInterstitialListener mInterstitialListener;
//    private Activity mActivity;
//
//    @Override
//    public void onNetworkCallback(String network, String event) {
//        Log.d("Heyzap", network + "   " + event);
//        if(event.equals(HeyzapAds.NetworkCallback.FETCH_FAILED)){
//            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
//        }
//    }
//
//    @Override
//    public void onShow(String tag) {
//        mInterstitialListener.onInterstitialShown();
//    }
//
//    @Override
//    public void onClick(String tag) {
//        mInterstitialListener.onInterstitialClicked();
//    }
//
//    @Override
//    public void onHide(String tag) {
//        mInterstitialListener.onInterstitialDismissed();
//    }
//
//    @Override
//    public void onFailedToShow(String tag) {
//        mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
//    }
//
//    @Override
//    public void onAvailable(String tag) {
//        mInterstitialListener.onInterstitialLoaded();
//    }
//
//    @Override
//    public void onFailedToFetch(String tag) {
//        Helper.wtf("Heyzap Failed to fetch ");
//        mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
//    }
//
//    @Override
//    public void onAudioStarted() {
//
//    }
//
//    @Override
//    public void onAudioFinished() {
//
//    }
//
//    @Override
//    public void showInterstitial() {
//        if (InterstitialAd.isAvailable()) {
//            InterstitialAd.display(mActivity);
//        } else {
//            Helper.wtf("Wannet to show Heyzap but wasnt avaible ");
//        }
//    }
//
//    @Override
//    protected void loadInterstitial(Context context, CustomEventInterstitialListener customEventInterstitialListener, Map<String, Object> localExtras, Map<String, String> serverExtras) {
//        Helper.wtf("Loading Heyzap");
//        String placementID = serverExtras.get(PLACEMENT_ID);
//        mInterstitialListener = customEventInterstitialListener;
//        mActivity = ((Activity) context);
//
//        if (placementID != null && !placementID.isEmpty()) {
//            if (!HeyzapAds.hasStarted()) {
//                HeyzapAds.start(placementID, mActivity, HeyzapAds.DISABLE_AUTOMATIC_FETCH|HeyzapAds.DISABLE_MEDIATION);
//                HeyzapAds.setNetworkCallbackListener(this);
//            }
//            InterstitialAd.setOnStatusListener(this);
//            if (!InterstitialAd.isAvailable()) {
//                InterstitialAd.fetch();
//            }
//        } else {
//            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
//        }
//    }
//
//    @Override
//    protected boolean usesProxy() {
//        return false;
//    }
//
//    @Override
//    protected void onInvalidate() {
//
//    }
//}
