package com.mopub.ads;


import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.mojang.base.json.Data;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;

import com.mojang.base.Screen;

/**
 * handles reloads errors disable screen on click
 */
public class Interstitial implements MoPubInterstitial.InterstitialAdListener,Ad {

    private static final long DISABLE_SCREEN_MILLS = 3000;
    private MoPubInterstitial mopubInterstitial;
    private final Activity activity;
    private final String interstitialId;
    private final Screen screen;
    private String country;
    private final Handler mainHandler;
    private String TAG = this.getClass().getName();

    public Interstitial(Activity activity,String interstitialId,Screen screen) {
        this.activity = activity;
        this.interstitialId = interstitialId;
        this.screen = screen;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Nullable
    public String getCountry(){
        return country;
    }

    @Override
    public boolean show() {
        if(mopubInterstitial != null) {
            return mopubInterstitial.show();
        }else{
            Log.e(TAG, "show: Cant show because int == null forgot to start ?");
            return false;
        }
    }

    @Override
    public void onInterstitialLoaded(MoPubInterstitial interstitial) {
        country = interstitial.getCountry();
    }

    @Override
    public void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode) {
        if(errorCode.equals(MoPubErrorCode.NO_CONNECTION)){
            Toast.makeText(activity,"Failed to load Advertisement make sure your internet connection is on",Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onInterstitialShown(MoPubInterstitial interstitial) {

    }

    @Override
    public void onInterstitialClicked(MoPubInterstitial interstitial) {
        if(Data.Ads.Interstitial.disableTouch) {
            screen.disableTouch(DISABLE_SCREEN_MILLS);
        }
    }

    @Override
    public void onInterstitialDismissed(MoPubInterstitial interstitial) {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mopubInterstitial.load();
            }
        },3000);
    }

    public void destroy() {
        mopubInterstitial.destroy();
    }

    public void stop() {

    }

    public void start() {
        if (mopubInterstitial == null) {
            mopubInterstitial = new MoPubInterstitial(activity,interstitialId);
            mopubInterstitial.setInterstitialAdListener(this);
            mopubInterstitial.load();
        }
    }
}
