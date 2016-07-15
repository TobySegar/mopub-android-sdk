package com.mopub.ads.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.firebase.crash.FirebaseCrash;
import com.mojang.base.Helper;
import com.mojang.base.events.AppEvent;
import com.mojang.base.json.Data;
import com.mopub.ads.Interstitial;
import com.mopub.ads.Proxy;


public class FastAd {
    private final String admobId;
    private InterstitialAd mGoogleInterstitialAd;
    private Context context;
    public boolean showed;

    public FastAd(String admobId) {
        this.admobId = admobId;
    }

    public void load(final Context context, final Runnable initMopubRunnable) {
        Helper.wtf("FastAd", "load: LOADING FAST AD");
        this.context = context;
        final SharedPreferences sharedPreferences = context.getSharedPreferences(GooglePlayServicesInterstitial.CLICKREDUCE, Context.MODE_PRIVATE);

        if(GooglePlayServicesInterstitial.isDisabled(sharedPreferences)){
            initMopubRunnable.run();
            return;
        }

        mGoogleInterstitialAd = new InterstitialAd(context);
        mGoogleInterstitialAd.setAdUnitId(admobId);
        mGoogleInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                initMopubRunnable.run();
            }

            @Override
            public void onAdLeftApplication() {
                super.onAdLeftApplication();
                GooglePlayServicesInterstitial.registerAdmobClick(sharedPreferences);
                Interstitial.disableTouch(Data.Ads.Interstitial.disableTouchChance);
            }
        });

        final AdRequest adRequest = new AdRequest.Builder()
                .setRequestAgent("MoPub")
                .addTestDevice("E883C2BB7DE538BAADA96556402DA41F")
                .build();

        mGoogleInterstitialAd.loadAd(adRequest);
    }

    public boolean show(){
        Helper.wtf("FastAd", "show() called with: FastAd");
        showed = true;
        if(!mGoogleInterstitialAd.isLoaded()){
            mGoogleInterstitialAd = null;
            return false;
        }else if(!AppEvent.stopped){
            new Proxy().startProxyActivity(context,mGoogleInterstitialAd);
            return true;
        }
        return false;
    }
}
