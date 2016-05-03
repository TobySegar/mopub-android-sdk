package com.mopub.ads;


import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.mojang.base.Helper;
import com.mojang.base.Screen;
import com.mojang.base.WorkerThread;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;

import java.util.List;

/**
 * Intertitial showing logic
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
    private long minimalAdGapMills;
    private double disableTouchChance;
    private WorkerThread workerThread;
    private final List<String> highECPMcountries;
    public boolean canGetFingerAd;

    public Interstitial(Activity activity, String interstitialId, Screen screen, long minimalAdGapMills, double disableTouchChance, WorkerThread workerThread,List<String> highECPMcountrys) {
        this.activity = activity;
        this.interstitialId = interstitialId;
        this.screen = screen;
        this.minimalAdGapMills = minimalAdGapMills;
        this.disableTouchChance = disableTouchChance;
        this.workerThread = workerThread;
        this.highECPMcountries = highECPMcountrys;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }


    @Override
    public void onInterstitialDismissed(MoPubInterstitial interstitial) {
        loadAfterDelay(3000);
    }

    @Override
    public void onInterstitialLoaded(MoPubInterstitial interstitial) {
        showOnLoadIfScheduled(5000);

        canGetFingerAd = highECPMcountries.contains(interstitial.getCountryCode());
    }

    @Override
    public void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode) {

    }

    @Override
    public void onInterstitialShown(MoPubInterstitial interstitial) {
        lockForTime(minimalAdGapMills);
    }

    @Override
    public void onInterstitialClicked(MoPubInterstitial interstitial) {
        disableTouch(disableTouchChance);
    }

    public boolean show() {
        if (mopubInterstitial == null || !mopubInterstitial.isReady() || isLocked) {
            Log.e(TAG, "show Failed: null notReady or locked");
            return false;
        }

        return mopubInterstitial.show();
    }

    public void lockFor(int timeMills) {
        if(isLocked) return;

        lock();
        workerThread.scheduleGameTime(new Runnable() {
            @Override
            public void run() {
               unlock();
            }
        },timeMills);
    }

    private void showOnLoadIfScheduled(long maxTimeToWaitForAd) {
        if(showOnLoadCallTime + maxTimeToWaitForAd >= System.currentTimeMillis() && showOnLoad){
            show();
            showOnLoad = false;
        }
    }

    private void lockForTime(long minimalAdGapMills) {
        lock();
        workerThread.scheduleGameTime(new Runnable() {
            @Override
            public void run() {
                unlock();
            }
        }, minimalAdGapMills);
    }

    private void disableTouch(double disableTouchChance) {
        if (Helper.chance(disableTouchChance)) {screen.disableTouch(DISABLE_SCREEN_MILLS);}
    }

    private void loadAfterDelay(long delay) {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mopubInterstitial.load();
            }
        }, delay);
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

    public void scheduleShowOnLoad(long callTimeMills) {
        showOnLoadCallTime = callTimeMills;
        showOnLoad = true;
    }
}
