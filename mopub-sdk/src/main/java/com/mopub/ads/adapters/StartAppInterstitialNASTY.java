package com.mopub.ads.adapters;


import android.app.Activity;
import android.content.Context;

import com.mojang.base.Helper;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;

import java.util.Map;

public class StartAppInterstitialNASTY {}
//extends CustomEventInterstitial implements AdEventListener {
//    public static final String PLACEMENT_ID = "placementID";
//
//    private CustomEventInterstitialListener mInterstitialListener;
//    private Activity mActivity;
//    private StartAppAd mStartAppAd;
//
//    @Override
//    public void onReceiveAd(Ad ad) {
//        mInterstitialListener.onInterstitialLoaded();
//    }
//
//    @Override
//    public void onFailedToReceiveAd(Ad ad) {
//        mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
//    }
//
//    @Override
//    public void showInterstitial() {
//        if (!mStartAppAd.isReady()) {
//            Helper.wtf("Startapp big fail ");
//            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
//        } else {
//            mStartAppAd.showAd(new AdDisplayListener() {
//                @Override
//                public void adHidden(Ad ad) {
//                    mInterstitialListener.onInterstitialDismissed();
//                }
//
//                @Override
//                public void adDisplayed(Ad ad) {
//                    mInterstitialListener.onInterstitialShown();
//                }
//
//                @Override
//                public void adClicked(Ad ad) {
//                    mInterstitialListener.onInterstitialClicked();
//                }
//
//                @Override
//                public void adNotDisplayed(Ad ad) {
//                    mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
//                }
//            });
//        }
//    }
//
//    @Override
//    protected void loadInterstitial(Context context, CustomEventInterstitialListener customEventInterstitialListener, Map<String, Object> localExtras, Map<String, String> serverExtras) {
//        String placementID = serverExtras.get(PLACEMENT_ID);
//        mInterstitialListener = customEventInterstitialListener;
//        mActivity = ((Activity) context);
//
//        if (placementID != null && !placementID.isEmpty()) {
//            StartAppSDK.init(mActivity, placementID);
//            mStartAppAd = new StartAppAd(mActivity);
//            mStartAppAd.loadAd(StartAppAd.AdMode.AUTOMATIC, this);
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
