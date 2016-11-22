package com.mopub.ads.adapters;

//import android.content.Context;
//import android.util.Log;
//
//import com.facebook.ads.Ad;
//import com.facebook.ads.AdError;
//import com.facebook.ads.AdSettings;
//import com.facebook.ads.InterstitialAd;
//import com.facebook.ads.InterstitialAdListener;
//import com.mojang.base.Helper;
//import com.mopub.mobileads.CustomEventInterstitial;
//import com.mopub.mobileads.MoPubErrorCode;
//
//import java.util.Map;

import android.content.Context;

import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;

import java.util.Map;

/**
 * Tested with Facebook SDK 4.8.1.
 */
public class FacebookInterstitial extends CustomEventInterstitial implements MoPubInterstitial.InterstitialAdListener {
    @Override
    protected void loadInterstitial(Context context, CustomEventInterstitialListener customEventInterstitialListener, Map<String, Object> localExtras, Map<String, String> serverExtras) {

    }

    @Override
    public void showInterstitial() {

    }

    @Override
    protected boolean usesProxy() {
        return false;
    }

    @Override
    protected void onInvalidate() {

    }

    @Override
    public void onInterstitialLoaded(MoPubInterstitial interstitial) {

    }

    @Override
    public void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode) {

    }

    @Override
    public void onInterstitialShown(MoPubInterstitial interstitial) {

    }

    @Override
    public void onInterstitialClicked(MoPubInterstitial interstitial) {

    }

    @Override
    public void onInterstitialDismissed(MoPubInterstitial interstitial) {

    }
//    public static final String PLACEMENT_ID_KEY = "placement_id";
//
//    private InterstitialAd mFacebookInterstitial;
//    private CustomEventInterstitialListener mInterstitialListener;
//
//    @Override
//    public void showInterstitial() {
//        if (mFacebookInterstitial != null && mFacebookInterstitial.isAdLoaded()) {
//            mFacebookInterstitial.show();
//        } else {
//            Helper.wtf("MoPub", "Tried to show a Facebook interstitial ad before it finished loading. Please try again.");
//            if (mInterstitialListener != null) {
//                onError(mFacebookInterstitial, AdError.INTERNAL_ERROR);
//            } else {
//                Helper.wtf("MoPub", "Interstitial listener not instantiated. Please load interstitial again.");
//            }
//        }
//    }
//
//    /**
//     * InterstitialAdListener implementation
//     */
//
//    @Override
//    public void onAdLoaded(final Ad ad) {
//        Helper.wtf("MoPub", "Facebook interstitial ad loaded successfully.");
//        mInterstitialListener.onInterstitialLoaded();
//    }
//
//    @Override
//    public void onError(final Ad ad, final AdError error) {
//        Helper.wtf("MoPub", "Facebook interstitial ad failed to load. " + error.getErrorMessage());
//        Helper.wtf("Facebook Failed");
//        if (error == AdError.NO_FILL) {
//            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
//        } else if (error == AdError.INTERNAL_ERROR) {
//            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
//        } else {
//            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
//        }
//    }
//
//    @Override
//    public void onInterstitialDisplayed(final Ad ad) {
//        Helper.wtf("MoPub", "Showing Facebook interstitial ad.");
//        mInterstitialListener.onInterstitialShown();
//    }
//
//    @Override
//    public void onAdClicked(final Ad ad) {
//        Helper.wtf("MoPub", "Facebook interstitial ad clicked.");
//        mInterstitialListener.onInterstitialClicked();
//    }
//
//    @Override
//    public void onInterstitialDismissed(final Ad ad) {
//        Helper.wtf("MoPub", "Facebook interstitial ad dismissed.");
//        mInterstitialListener.onInterstitialDismissed();
//    }
//
//    /**
//     * CustomEventInterstitial implementation
//     */
//    @Override
//    protected void loadInterstitial(final Context context,
//                                    final CustomEventInterstitialListener customEventInterstitialListener,
//                                    final Map<String, Object> localExtras,
//                                    final Map<String, String> serverExtras) {
//        mInterstitialListener = customEventInterstitialListener;
//
//        Helper.wtf("Facebook Load");
//
//        final String placementId;
//        if (extrasAreValid(serverExtras)) {
//            placementId = serverExtras.get(PLACEMENT_ID_KEY);
//        } else {
//            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
//            return;
//        }
//        if (Helper.DEBUG) {
//            AdSettings.addTestDevice("8d3cef1dfffa38d6463891bfd97b478e");
//            AdSettings.addTestDevice("21f3542e3b4f4a0c5469b674257d2933");
//        }
//        mFacebookInterstitial = new InterstitialAd(context, placementId);
//        mFacebookInterstitial.setAdListener(this);
//        mFacebookInterstitial.loadAd();
//    }
//
//    @Override
//    protected boolean usesProxy() {
//        return true;
//    }
//
//    @Override
//    protected void onInvalidate() {
//        if (mFacebookInterstitial != null) {
//            mFacebookInterstitial.destroy();
//            mFacebookInterstitial = null;
//        }
//    }
//
//    private boolean extrasAreValid(final Map<String, String> serverExtras) {
//        final String placementId = serverExtras.get(PLACEMENT_ID_KEY);
//        return (placementId != null && placementId.length() > 0);
//    }
//
//    @Deprecated
//        // for testing
//    InterstitialAd getInterstitialAd() {
//        return mFacebookInterstitial;
//    }
}