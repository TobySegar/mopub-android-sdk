package com.mopub.ads.adapters;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.firebase.crash.FirebaseCrash;


public class FastAd {
    private final String admobId;
    private InterstitialAd mGoogleInterstitialAd;

    public FastAd(String admobId) {
        this.admobId = admobId;
    }

    public void load(Context context, final Runnable runnable) {
        Log.e("FastAd", "load: LOADING FAST AD");
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
                .build();

        mGoogleInterstitialAd.loadAd(adRequest);
    }

    public boolean show(){
        Log.w("FastAd", "show() called with: FastAd");

        if(!mGoogleInterstitialAd.isLoaded()){
            mGoogleInterstitialAd = null;
            return false;
        }else{
            mGoogleInterstitialAd.show();
            return true;
        }
    }
}
