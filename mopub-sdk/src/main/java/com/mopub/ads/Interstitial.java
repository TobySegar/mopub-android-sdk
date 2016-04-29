package com.mopub.ads;


import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.mojang.base.Helper;
import com.mojang.base.Screen;
import com.mojang.base.WorkerThread;
import com.mojang.base.json.Data;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;

import java.util.Calendar;

/**
 * handles reloads errors disable screen on click
 */
public class Interstitial implements MoPubInterstitial.InterstitialAdListener {

    private static final long DISABLE_SCREEN_MILLS = 3000;
    private MoPubInterstitial mopubInterstitial;
    private final Activity activity;
    private final String interstitialId;
    private final Screen screen;
    private final Handler mainHandler;
    private String TAG = this.getClass().getName();
    private boolean isLocked;
    private boolean showOnLoad;
    private long showOnLoadCallTime;
    private boolean minimalAdGapPassed;
    private long minimalAdGapMills;
    private double disableTouchChance;
    private WorkerThread workerThread;

    public Interstitial(Activity activity, String interstitialId, Screen screen, long minimalAdGapMills, double disableTouchChance, WorkerThread workerThread) {
        this.activity = activity;
        this.interstitialId = interstitialId;
        this.screen = screen;
        this.minimalAdGapMills = minimalAdGapMills;
        this.disableTouchChance = disableTouchChance;
        this.workerThread = workerThread;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }


    public boolean show() {
        if (mopubInterstitial == null || !mopubInterstitial.isReady() || isLocked) {
            Log.e(TAG, "show Failed: null notReady or locked");
            return false;
        }

        if (!minimalAdGapPassed){
            Log.e(TAG, "showInterstitial: Minimal ad gap nepresiel!");
            return false;
        }

        return mopubInterstitial.show();
    }

    @Override
    public void onInterstitialLoaded(MoPubInterstitial interstitial) {
        //we wait 5000 mills for ad to load to show instantly for slow internet ppl
        if(showOnLoadCallTime + 5000 >= System.currentTimeMillis() && showOnLoad){
            show();
            showOnLoad = false;
        }
    }

    @Override
    public void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode) {
        if (errorCode.equals(MoPubErrorCode.NO_CONNECTION)) {
            Toast.makeText(activity, "Failed to load Advertisement make sure your internet connection is on", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onInterstitialShown(MoPubInterstitial interstitial) {
        minimalAdGapPassed = false;
        workerThread.scheduleGameTime(new Runnable() {
            @Override
            public void run() {minimalAdGapPassed = true;}
        }, minimalAdGapMills);
    }

    @Override
    public void onInterstitialClicked(MoPubInterstitial interstitial) {
        if (Helper.chance(disableTouchChance)) {screen.disableTouch(DISABLE_SCREEN_MILLS);}
    }

    @Override
    public void onInterstitialDismissed(MoPubInterstitial interstitial) {
        //we load next ad after 3 seconds so you dont feed that lag yo
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mopubInterstitial.load();
            }
        }, 3000);
    }

    public void destroy() {
        mopubInterstitial.destroy();
    }

    public void lock() {
        Log.e(TAG, "interstitial lock");
        isLocked = true;
    }

    public void unlock(){
        Log.e(TAG, "interstitial unlock");
        isLocked = false;
    }

    public void init() {
        if (mopubInterstitial == null) {
            mopubInterstitial = new MoPubInterstitial(activity, interstitialId);
            mopubInterstitial.setInterstitialAdListener(this);
            mopubInterstitial.load();
        }

        if (!mopubInterstitial.isReady()) {
            mopubInterstitial.load();
        }
    }

    public void showOnLoad(long callTimeMills) {
        showOnLoadCallTime = callTimeMills;
        showOnLoad = true;
    }
}
