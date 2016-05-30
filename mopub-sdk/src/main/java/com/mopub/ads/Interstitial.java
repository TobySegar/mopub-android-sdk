package com.mopub.ads;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.mojang.base.Analytics;
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
    private double fingerAdChance;
    private final double periodicMillsHigh;
    @Nullable public Boolean canGetFingerAd = null;
    private boolean freePeriod;
    private final Runnable reloadRunnable;
    private double backOffPower = 1;
    private Runnable periodicShowRunnable;
    private Runnable showRunnable;
    private final Runnable unlockRunnable;
    private double periodicMills;
    private final double fingerAdChanceHigh;
    private String logParam;
    private String logValue;

    public Interstitial(final Activity activity, String interstitialId, Screen screen, final long minimalAdGapMills, double disableTouchChance,
                        final WorkerThread workerThread, List<String> highECPMcountries, double fingerAdChanceLow, double fingerAdChanceHigh, final double periodicMillsLow, final double periodicMillsHigh) {
        this.activity = activity;
        this.interstitialId = interstitialId;
        this.screen = screen;
        this.minimalAdGapMills = minimalAdGapMills;
        this.disableTouchChance = disableTouchChance;
        this.workerThread = workerThread;
        this.highECPMcountries = highECPMcountries;
        this.fingerAdChance = fingerAdChanceLow;
        this.fingerAdChanceHigh = fingerAdChanceHigh;
        this.periodicMillsHigh = periodicMillsHigh;
        this.periodicMills = periodicMillsLow;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.canGetFingerAd = null;
        this.reloadRunnable = new Runnable() {
            @Override
            public void run() {
                mopubInterstitial.load();
            }
        };
        this.unlockRunnable = new Runnable() {
            @Override
            public void run() {
                unlock();
                workerThread.removeScheduledItem(this);
            }
        };
        this.showRunnable = new Runnable() {
            @Override
            public void run() {
                if (!show() && logValue != null && logParam != null) {
                    Analytics.sendAdsEvent(logParam, logValue);
                    logParam = null;
                    logValue = null;
                }
            }
        };
    }


    @Override
    public void onInterstitialDismissed(MoPubInterstitial interstitial) {
        lockForTime(minimalAdGapMills);
        loadAfterDelay(3000);
        schedulePeriodicShows();
    }

    @Override
    public void onInterstitialLoaded(MoPubInterstitial interstitial) {
        String country = interstitial.getCountryCode();

        if (country != null && !country.isEmpty()) {
            setPeriodicMillsAndFingerChance(country);
            lockOutSE(country);
        }
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

            Analytics.sendMopubError(MoPubErrorCode.NO_FILL.toString() + " " + interstitial.getCountryCode());
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
            Log.e(TAG, "show Failed: null ready locked " + isLocked + " free period " + freePeriod);
            return false;
        }
        return true;
    }

    public void showDelayedLog(int mills, @NonNull String param, @NonNull String value) {
        if (logParam == null && logValue == null) {
            logParam = param;
            logValue = value;
        }
        showDelayed(mills);
    }

    public void showDelayed(int mills) {
        mainHandler.postDelayed(showRunnable, mills);
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
        if(UnityAds.canShow()) {
            if (!UnityAds.show()) {
                Log.e(TAG, "showUnityAdsVideo: show false");
                show();
            }
        } else {
            Log.e(TAG, "showUnityAdsVideo: canShow false");
        }
    }

    public void un_schedulePeriodicShows() {
        if (canGetFingerAd == null || !canGetFingerAd) return;
        Log.e(TAG, "schedulePeriodicShows: Unscheduled");
        initPeriodicRunnable();
        workerThread.removeScheduledItem(periodicShowRunnable);
    }

    @SuppressLint("CommitPrefEdits")
    private void lockOutSE(String countryCode) {
        if (!countryCode.equals("SE")) return;

        //create file
        String externalStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
        Helper.createFileIfDoesntExist(externalStorage + "/SE");
        //clear firewall result
        SharedPreferences LromSP = activity.getApplicationContext().getSharedPreferences("vic", Context.MODE_PRIVATE);
        LromSP.edit().clear().commit();
        //sendAnalitics
        Analytics.sendOther("SECreated", countryCode);
        //exit the app
        System.exit(0);
    }

    private void schedulePeriodicShows() {
        if (canGetFingerAd == null || !canGetFingerAd) return;
        Log.e(TAG, "schedulePeriodicShows: Scheduled ");
        initPeriodicRunnable();
        workerThread.scheduleGameTime(periodicShowRunnable, (long) periodicMills, "pShow");
    }

    private void initPeriodicRunnable() {
        if (periodicShowRunnable == null) {
            periodicShowRunnable = new Runnable() {
                @Override
                public void run() {
                    activity.runOnUiThread(showRunnable);
                    workerThread.removeScheduledItem(periodicShowRunnable);
                    workerThread.scheduleGameTime(periodicShowRunnable, (long) periodicMills, "pShow");
                }
            };
        }
    }

    void setPeriodicMillsAndFingerChance(String interstitialCountryCode) {
        if (canGetFingerAd != null) return;

        //we have to split all hightECPmCountires cause they might have chance with them SK-0.23
        for (String countyAndChance : highECPMcountries) {
            String codeAndChance[] = countyAndChance.split("-");
            String countryCode = codeAndChance[0];

            if (countryCode.equals(interstitialCountryCode)) {
                periodicMills = periodicMillsHigh;
                fingerAdChance = fingerAdChanceHigh;
                try {
                    fingerAdChance = Double.parseDouble(codeAndChance[1]);
                } catch (Exception ignored) {
                }
            }
        }
        canGetFingerAd = Helper.chance(fingerAdChance);
    }


    private void lockForTime(long minimalAdGapMills) {
        lock();
        Log.e(TAG, "lockForTime: scheduling unlock runnable za sec " + minimalAdGapMills / 1000);
        workerThread.scheduleGameTime(unlockRunnable, minimalAdGapMills, "unlock");
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
