package com.mopub.ads.adapters;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.firebase.crash.FirebaseCrash;
import com.mojang.base.Helper;
import com.mojang.base.events.AppEvent;
import com.mopub.ads.Proxy;


public class FastAd {
    private final String admobId;
    private InterstitialAd mGoogleInterstitialAd;
    private Context context;
    public boolean showed;

    public FastAd(String admobId) {
        this.admobId = admobId;
    }

    public void load(Context context, final Runnable runnable) {
        Helper.wtf("FastAd", "load: LOADING FAST AD");
        this.context = context;
        mGoogleInterstitialAd = new InterstitialAd(context);
        mGoogleInterstitialAd.setAdUnitId(admobId);
        mGoogleInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                runnable.run();
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
