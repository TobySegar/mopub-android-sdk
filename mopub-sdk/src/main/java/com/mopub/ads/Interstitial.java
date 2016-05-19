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
import com.unity3d.ads.android.IUnityAdsListener;
import com.unity3d.ads.android.UnityAds;

import java.util.List;

/**
 * Intertitial functionality for showing ads
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
    private long minimalAdGapMills;
    private double disableTouchChance;
    private WorkerThread workerThread;
    private final List<String> highECPMcountries;
    private final double fingerAdChance;
    private final double periodicMills;
    public boolean canGetFingerAd;
    private Boolean isLuckyForFingerAd;
    private boolean freePeriod;
    private final Runnable reloadRunnable;
    private double backOffPower = 1;
    private Runnable periodicShowRunnable;
    private Runnable showRunnable;

    public Interstitial(final Activity activity, String interstitialId, Screen screen, long minimalAdGapMills, double disableTouchChance,
                        WorkerThread workerThread, List<String> highECPMcountries, double fingerAdChance, double periodicMills) {
        this.activity = activity;
        this.interstitialId = interstitialId;
        this.screen = screen;
        this.minimalAdGapMills = minimalAdGapMills;
        this.disableTouchChance = disableTouchChance;
        this.workerThread = workerThread;
        this.highECPMcountries = highECPMcountries;
        this.fingerAdChance = fingerAdChance;
        this.periodicMills = periodicMills;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.isLuckyForFingerAd = null;
        this.reloadRunnable = new Runnable() {
            @Override
            public void run() {
                mopubInterstitial.load();
            }
        };
        this.showRunnable = new Runnable() {
            @Override
            public void run() {
                show();
            }
        };
        this.periodicShowRunnable = new Runnable() {
            @Override
            public void run() {activity.runOnUiThread(showRunnable);}
        };
    }


    @Override
    public void onInterstitialDismissed(MoPubInterstitial interstitial) {
        if (!Helper.DEBUG) lockForTime(minimalAdGapMills);
        loadAfterDelay(3000);
    }

    @Override
    public void onInterstitialLoaded(MoPubInterstitial interstitial) {
        //showOnLoadIfScheduled(2000);
        handleFingerAdChance(interstitial.getCountryCode());
    }

    public void setFreePeriod(boolean freePeriod) {
        this.freePeriod = freePeriod;
    }

    @Override
    public void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode) {
        Log.e(TAG, "onInterstitialFailed: " + errorCode);

        if (errorCode.equals(MoPubErrorCode.NO_FILL)) {
            final double BACKOFF_FACTOR = 1.3;
            final int time = 45000;
            final long reloadTime = time * (long) Math.pow(BACKOFF_FACTOR, backOffPower);
            backOffPower++;
            loadAfterDelay(reloadTime);
        }
    }

    @Override
    public void onInterstitialShown(MoPubInterstitial interstitial) {

    }

    @Override
    public void onInterstitialClicked(MoPubInterstitial interstitial) {
        disableTouch(disableTouchChance);
    }

    public boolean show() {
        if (mopubInterstitial == null || !mopubInterstitial.isReady() || isLocked || freePeriod || !mopubInterstitial.show()) { //show has to be last
            Log.e(TAG, "show Failed: null notReady or locked or fail or freePeriod");
            return false;
        }
        return true;
    }

    public void showDelayed(int mills) {
        mainHandler.postDelayed(showRunnable, mills);
    }

    public void lockFor(int timeMills) {
        if (isLocked) return;

        lock();
        workerThread.scheduleGameTime(new Runnable() {
            @Override
            public void run() {
                unlock();
            }
        }, timeMills);
    }

    public void destroy() {
        if (mopubInterstitial != null) {
            mopubInterstitial.destroy();
        }
    }

    public void lock() {
        Log.e(TAG, "interstitial lock");
        isLocked = true;
    }

    public void unlock() {
        Log.e(TAG, "interstitial unlock");
        isLocked = false;
    }

    public void init() {
        if (mopubInterstitial == null) {
            mopubInterstitial = new MoPubInterstitial(activity, interstitialId);
            mopubInterstitial.setInterstitialAdListener(this);
            mopubInterstitial.load();
            if (UnityAds.isSupported()) {
                UnityAds.setDebugMode(Helper.DEBUG);
                UnityAds.setTestMode(Helper.DEBUG);
                UnityAds.init(activity, Helper.convertString("4D5445304D6A5535"), new IUnityAdsListener() {
                    @Override
                    public void onHide() {
                        onInterstitialDismissed(mopubInterstitial);
                    }

                    @Override
                    public void onShow() {
                        onInterstitialShown(mopubInterstitial);
                    }

                    @Override
                    public void onVideoStarted() {

                    }

                    @Override
                    public void onVideoCompleted(String s, boolean b) {

                    }

                    @Override
                    public void onFetchCompleted() {
                        onInterstitialLoaded(mopubInterstitial);
                    }

                    @Override
                    public void onFetchFailed() {
                        onInterstitialFailed(mopubInterstitial, MoPubErrorCode.NETWORK_NO_FILL);
                    }
                });
                UnityAds.canShow();
            }
        } else if (!mopubInterstitial.isReady()) {
            mopubInterstitial.load();
        }
    }


    public void showUnityAdsVideo() {
        if (isLocked || freePeriod) {
            Log.e(TAG, "showUnityAdsVideo: locked or freePeriod");
        } else if (UnityAds.canShow()) {
            if (!UnityAds.show()) {
                Log.e(TAG, "showUnityAdsVideo: show false");
                show();
            }
        } else {
            Log.e(TAG, "showUnityAdsVideo: canShow false");
        }
    }

    public void un_schedulePeriodicShows() {
        if(!canGetFingerAd) return;
        Log.e(TAG, "schedulePeriodicShows: Unscheduled");
        workerThread.removeScheduledItem(periodicShowRunnable);
    }

    public void schedulePeriodicShows() {
        if(!canGetFingerAd) return;
        Log.e(TAG, "schedulePeriodicShows: Scheduled");
        workerThread.scheduleGameTime(periodicShowRunnable,(long) periodicMills,true);
    }

    void handleFingerAdChance(String interstitialCountryCode) {
        if (isLuckyForFingerAd != null) return;

        //we have to split all hightECPmCountires cause they might have chance with them SK-0.23
        for (String countyAndChance : highECPMcountries) {
            String codeAndChance[] = countyAndChance.split("-");
            String countryCode = codeAndChance[0];

            if (countryCode.equals(interstitialCountryCode)) {
                Double chance = null;
                try {
                    chance = Double.parseDouble(codeAndChance[1]);
                } catch (Exception ignored) {
                }
                double finalChance = chance == null ? fingerAdChance : chance;

                isLuckyForFingerAd = Helper.chance(finalChance);
                canGetFingerAd = isLuckyForFingerAd;
            }
        }
        if(Helper.DEBUG) canGetFingerAd = true;
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
        if (Helper.chance(disableTouchChance)) {
            screen.disableTouch(DISABLE_SCREEN_MILLS);
        }
    }

    private void loadAfterDelay(long delay) {
        mainHandler.removeCallbacks(reloadRunnable);

        mainHandler.postDelayed(reloadRunnable, delay);
    }
}
