package com.mopub.ads.adapters;

import android.app.Activity;
import android.content.Context;

import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdk;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.mojang.base.Helper;
import com.mojang.base.events.AppEvent;
import com.mojang.base.json.Data;
import com.mopub.ads.Ads;
import com.mopub.ads.Interstitial;
import com.mopub.ads.Proxy;


public class FastAd {
    private final String admobId;
    private final Interstitial interstitial;
    private InterstitialAd mGoogleInterstitialAd;
    private Activity activity;
    public boolean showed;
    private boolean useApplovin;
    private AppLovinSdk sdk;
    private Runnable initMopubRunnable;
    private AppLovinAd loadedApplovinAd;


    public FastAd(String admobId, Interstitial interstitial) {
        this.admobId = admobId;
        this.interstitial = interstitial;
    }

    public void load(final Context context, final Runnable initMopubRunnable) {
        Helper.wtf("FastAd", "load: LOADING FAST AD");
        this.activity = (Activity) context;
        this.initMopubRunnable = initMopubRunnable;
        this.useApplovin = Data.Ads.Interstitial.fastAdApplovin;

        if (GooglePlayServicesInterstitial.isDisabled(activity) && !useApplovin) {
            this.initMopubRunnable.run();
            return;
        }

        if (!useApplovin) {
            loadAdmob();
        } else {
            loadApplovin();
        }

    }

    private void loadApplovin() {
        Helper.wtf("loading Applovin fastad");
        sdk = AppLovinSdk.getInstance(this.activity);
        sdk.getAdService().loadNextAd(AppLovinAdSize.INTERSTITIAL, new AppLovinAdLoadListener() {
            @Override
            public void adReceived(AppLovinAd appLovinAd) {
                loadedApplovinAd = appLovinAd;
            }

            @Override
            public void failedToReceiveAd(int i) {
                Helper.wtf("Fast Ads applovin failed init mopub");
                initMopubRunnable.run();
            }
        });
    }

    private void loadAdmob() {
        Helper.wtf("loading Admob fastad");
        mGoogleInterstitialAd = new InterstitialAd(activity);
        mGoogleInterstitialAd.setAdUnitId(admobId);
        mGoogleInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                initMopubRunnable.run();
                interstitial.callNativeBackPressed();
            }

            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                initMopubRunnable.run();
            }

            @Override
            public void onAdLeftApplication() {
                super.onAdLeftApplication();
                GooglePlayServicesInterstitial.registerAdmobClick(activity);
                Ads.getInstance().getInterstitial().disableTouch(activity,Data.Ads.Interstitial.disableTouchChance);
            }
        });

        final AdRequest adRequest = new AdRequest.Builder()
                .setRequestAgent("MoPub")
                .addTestDevice("E883C2BB7DE538BAADA96556402DA41F")
                .build();

        mGoogleInterstitialAd.loadAd(adRequest);
    }

    public boolean show() {
        Helper.wtf("FastAd", "show() called with: FastAd");
        showed = true;
        if (!AppEvent.stopped) {
            if (useApplovin) {
                if (AppLovinInterstitialAd.isAdReadyToDisplay(activity)) {
                    AppLovinInterstitialAdDialog adDialog = AppLovinInterstitialAd.create(sdk, this.activity);

                    adDialog.setAdDisplayListener(new AppLovinAdDisplayListener() {
                        @Override
                        public void adDisplayed(AppLovinAd appLovinAd) {

                        }

                        @Override
                        public void adHidden(AppLovinAd appLovinAd) {
                            Helper.wtf("Fast Ads applovin hidden init mopub");
                            initMopubRunnable.run();
                            interstitial.callNativeBackPressed();
                            interstitial.hideNavigationBar();
                        }
                    });
                    adDialog.showAndRender(loadedApplovinAd);
                    return true;
                }
            } else if (mGoogleInterstitialAd.isLoaded()) {
                Interstitial.FAST_BACK_PRESS = true;
                new Proxy().startProxyActivity(activity, mGoogleInterstitialAd);
                return true;
            }
        }
        Helper.wtf("Failed to show fastad");
        initMopubRunnable.run();
        return false;
    }
}
