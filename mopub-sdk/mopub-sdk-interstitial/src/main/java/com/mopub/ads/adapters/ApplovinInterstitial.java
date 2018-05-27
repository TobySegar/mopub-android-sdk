/**
 * AppLovin Interstitial SDK Mediation for MoPub
 *
 * @author Matt Szaro
 * @version 1.2
 **/

package com.mopub.ads.adapters;

import android.app.Activity;
import android.content.Context;

import com.applovin.adview.AppLovinInterstitialActivity;
import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkSettings;
import com.mojang.base.Helper;
import com.mojang.base.Logger;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;

import java.util.Map;

public class ApplovinInterstitial extends CustomEventInterstitial implements AppLovinAdLoadListener {
    private static final String EXTRAS_KEY = "appKey";
    private CustomEventInterstitial.CustomEventInterstitialListener mInterstitialListener;
    private Activity parentActivity;
    private AppLovinSdk sdk;
    private AppLovinAd lastReceived;
    private String key;

    /*
     * Abstract methods from CustomEventInterstitial
     */
    @Override
    public void loadInterstitial(Context context, CustomEventInterstitial.CustomEventInterstitialListener interstitialListener, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        Logger.Log("::Applovin Load");

        mInterstitialListener = interstitialListener;

        if (context instanceof Activity) {
            parentActivity = (Activity) context;
        } else {
            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
            return;
        }

        Logger.Log("::Applovin: Request received for new interstitial.");

        AppLovinSdkSettings setting = new AppLovinSdkSettings();
        setting.setVerboseLogging(Helper.canLog);
        setting.setAutoPreloadSizes("NONE");
        setting.setMuted(true);

        if (key == null) {
            key = getKeyFromExtras(serverExtras);
        }

        sdk = AppLovinSdk.getInstance(key, setting, context);
        sdk.getAdService().loadNextAd(AppLovinAdSize.INTERSTITIAL, this);

    }

    private String getKeyFromExtras(Map<String, String> serverExtras) {
        if (serverExtras.containsKey(EXTRAS_KEY)) {
            serverExtras.get(EXTRAS_KEY);
        } else if (mInterstitialListener != null) {
            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NO_FILL);
        }
        return null;
    }

    @Override
    public void showInterstitial() {
        final AppLovinAd adToRender = lastReceived;

        if (adToRender != null) {
            Logger.Log("::Showing AppLovin interstitial ad...");


            parentActivity.runOnUiThread(new Runnable() {
                public void run() {
                    AppLovinInterstitialAdDialog inter = AppLovinInterstitialAd.create(sdk, parentActivity);

                    inter.setAdClickListener(new AppLovinAdClickListener() {
                        @Override
                        public void adClicked(AppLovinAd appLovinAd) {
                            mInterstitialListener.onLeaveApplication();
                        }
                    });

                    inter.setAdDisplayListener(new AppLovinAdDisplayListener() {

                        @Override
                        public void adDisplayed(AppLovinAd appLovinAd) {
                            mInterstitialListener.onInterstitialShown();
                        }

                        @Override
                        public void adHidden(AppLovinAd appLovinAd) {
                            mInterstitialListener.onInterstitialDismissed();
                        }
                    });

                    inter.showAndRender(adToRender);
                }
            });
        } else {
            Logger.Log("::Showing AppLovin failed adToRender null");
        }
    }

    @Override
    protected boolean usesProxy() {
        return false;
    }


    @Override
    public void onInvalidate() {
        parentActivity = null;
        AppLovinInterstitialActivity.lastKnownWrapper = null;
    }

    @Override
    public void adReceived(AppLovinAd ad) {
        Logger.Log("::AppLovin interstitial loaded successfully.");

        lastReceived = ad;

        parentActivity.runOnUiThread(new Runnable() {
            public void run() {
                mInterstitialListener.onInterstitialLoaded();
            }
        });
    }

    @Override
    public void failedToReceiveAd(final int errorCode) {
        parentActivity.runOnUiThread(new Runnable() {
            public void run() {
                Logger.Log("::Applovin Fail");
                mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
        });
    }
}