package com.mopub.ads;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.mojang.base.Analytics;
import com.mojang.base.CounterView;
import com.mojang.base.Helper;
import com.mojang.base.Screen;
import com.mojang.base.json.Data;
import com.mopub.ads.adapters.FastAd;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;
import com.unity3d.ads.android.IUnityAdsListener;
import com.unity3d.ads.android.UnityAds;

import java.io.File;
import java.util.List;

/**
 * Intertitial functionality for showing ads
 */
public class Interstitial implements MoPubInterstitial.InterstitialAdListener {

    private static final long DISABLE_SCREEN_MILLS = 4000;
    private MoPubInterstitial mopubInterstitial;
    private final Activity activity;
    private final String interstitialId;
    private final Handler mainHandler;
    private String TAG = this.getClass().getName();
    private long minimalAdGapMills;
    private double disableTouchChance;
    private final List<String> highECPMcountries;
    private double fingerAdChance;
    private final double periodicMillsHigh;
    private boolean freePeriod;
    private final Runnable reloadRunnable;
    private double backOffPower = 1;
    private Runnable periodicShowRunnable;
    private Runnable showRunnable;
    private final Runnable gapUnlockRunnable;
    private double periodicMills;
    private final double fingerAdChanceHigh;
    private FastAd fastAd;
    private boolean fastAdUsed;
    private boolean onLoadedOnce;
    private boolean periodicScheduled;
    public final Lock lock;
    private final CounterView counterView;

    public Interstitial(final Activity activity, String interstitialId, final long minimalAdGapMills, double disableTouchChance,
                         List<String> highECPMcountries, double fingerAdChanceLow, double fingerAdChanceHigh, final double periodicMillsLow, final double periodicMillsHigh) {
        this.activity = activity;
        this.interstitialId = interstitialId;
        this.minimalAdGapMills = minimalAdGapMills;
        this.disableTouchChance = disableTouchChance;
        this.highECPMcountries = highECPMcountries;
        this.fingerAdChance = fingerAdChanceLow;
        this.fingerAdChanceHigh = fingerAdChanceHigh;
        this.periodicMillsHigh = periodicMillsHigh;
        this.periodicMills = periodicMillsLow;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.lock = new Lock();
        this.counterView = new CounterView(activity, new Runnable() {
            @Override
            public void run() {
                show();
            }
        },Screen.instance);

        this.reloadRunnable = new Runnable() {
            @Override
            public void run() {
                mopubInterstitial.load();
            }
        };
        this.gapUnlockRunnable = new Runnable() {
            @Override
            public void run() {
                lock.unlockGap();
            }
        };

        this.showRunnable = new Runnable() {
            @Override
            public void run() {
                Helper.wtf(TAG, "run: ShowRun");
                if(!lock.isLocked()) {
                    counterView.show();
                }
            }
        };
        this.periodicShowRunnable = new Runnable() {
            @Override
            public void run() {
                Helper.wtf(TAG, "run: PeriodicShowRun");
                showRunnable.run();
                mainHandler.postDelayed(periodicShowRunnable, (long) periodicMills);
            }
        };
    }


    @Override
    public void onInterstitialDismissed(MoPubInterstitial interstitial) {
        gapLockForTime(minimalAdGapMills);
        loadAfterDelay(3000);
    }

    @Override
    public void onInterstitialLoaded(MoPubInterstitial interstitial) {
        String country = interstitial.getCountryCode();

        if (!onLoadedOnce && country != null && !country.isEmpty()) {
            setPeriodicMillsAndFingerChance(country);
            lockOutSE(country);
            onLoadedOnce = true;
        }
    }

    public void setFreePeriod(boolean freePeriod) {
        this.freePeriod = freePeriod;
    }

    @Override
    public void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode) {
        Helper.wtf(TAG, "onInterstitialFailed: " + errorCode);

        if (errorCode.equals(MoPubErrorCode.NO_FILL) || errorCode.equals(MoPubErrorCode.UNSPECIFIED)) {
            final double BACKOFF_FACTOR = 1.3;
            final int time = 45001;
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
            Helper.wtf("I","show");
            if (mopubInterstitial == null || lock.isLocked() || !mopubInterstitial.isReady() || freePeriod || !mopubInterstitial.show()) { //show has to be last
                Helper.wtf(TAG, "show Failed: null ready locked ");
                return false;
            }
            return true;
    }

    public void showDelayed(int mills) {
        mainHandler.postDelayed(showRunnable, mills);
    }

    public void showDelayed(int mills, final Runnable runnable) {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                show();
                runnable.run();
            }
        },mills);
    }


    public void destroy() {
        if (mopubInterstitial != null) {
            mopubInterstitial.destroy();
        }
    }


    public void init(final boolean fromOnlineAccepted) {
        if (!fromOnlineAccepted && !fastAdUsed && Data.hasMinecraft) {
            Helper.wtf(TAG, "Interstitial init load fast ad");
            fastAdUsed = true;
            fastAd = new FastAd(Data.Ads.Interstitial.failoverId);
            fastAd.load(activity, new Runnable() {
                @Override
                public void run() {
                    _initDelayed();
                    gapLockForTime(minimalAdGapMills);
                }
            });
        } else {
            _initDelayed();
        }
    }

    public void showFastDelayed(int mills) {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mopubInterstitial != null){
                    mopubInterstitial.show();
                }else if (lock.isLocked() || fastAd == null || !fastAd.show()) {
                    _initDelayed();
                }
            }
        }, mills);
    }

    public void showUnityAdsVideo() {
        if(!lock.isMultiplayerLocked()) {
            if (!UnityAds.show()) {
                Helper.wtf(TAG, "showUnityAdsVideo: show false");
                show();
            }
        }else {
            Helper.wtf(TAG, "showUnityAdsVideo: show false multiplayer locked");
        }
    }


    public void schedulePeriodicShows() {
        if (!periodicScheduled) {
            Helper.wtf(TAG, "schedulePeriodicShows: Scheduled ");
            Helper.wtf(TAG, String.valueOf(periodicMills));
            mainHandler.postDelayed(periodicShowRunnable, (long) periodicMills);
            periodicScheduled = true;
        }
    }

    public void unschedulePeriodicShows() {
        if (periodicScheduled) {
            Helper.wtf(TAG, "unschedulePeriodicshows");
            Helper.wtf(TAG, String.valueOf(periodicMills));
            mainHandler.removeCallbacks(periodicShowRunnable);
            periodicScheduled = false;
        }
    }



    private void _initDelayed() {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (fastAd != null) fastAd = null;
                if (mopubInterstitial == null) {
                    mopubInterstitial = new MoPubInterstitial(activity, interstitialId);
                    mopubInterstitial.setInterstitialAdListener(Interstitial.this);
                    mopubInterstitial.setKeywords("game,minecraft,business,twitter");
                    mopubInterstitial.load();
                } else if (!mopubInterstitial.isReady()) {
                    mopubInterstitial.load();
                }

                if (UnityAds.isSupported()) {
                    UnityAds.setDebugMode(Helper.DEBUG);
                    UnityAds.setTestMode(Helper.DEBUG);
                    UnityAds.init(activity, Helper.convertString("4E7A49334E7A453D"), new IUnityAdsListener() {
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
            }
        }, 4000);
    }

    @SuppressLint("CommitPrefEdits")
    private void lockOutSE(String countryCode) {
        if (!countryCode.equals("SE")) return;

        //create file
        String externalStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
        Helper.createFileIfDoesntExist(externalStorage + File.separator + "SE");
        Helper.wtf("Crating SE file");
        //clear firewall result so he can go through check again
        SharedPreferences LromSP = activity.getApplicationContext().getSharedPreferences("vic", Context.MODE_PRIVATE);
        LromSP.edit().clear().commit();
        //sendAnalitics
        Analytics.sendOther("SECreated", countryCode);
        //exit the app
        System.exit(0);
    }


    void setPeriodicMillsAndFingerChance(String interstitialCountryCode) {
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
        schedulePeriodicShows();
    }


    private void gapLockForTime(long minimalAdGapMills) {
        lock.gapLock();
        Helper.wtf(TAG, "lockForTime: scheduling unlock runnable za sec " + minimalAdGapMills / 1000);
        mainHandler.postDelayed(gapUnlockRunnable, minimalAdGapMills);
    }

    public static void disableTouch(double disableTouchChance) {
        if (Helper.chance(disableTouchChance) && Data.hasMinecraft) {
            Screen.instance.disableTouch(DISABLE_SCREEN_MILLS);
        }
    }

    private void loadAfterDelay(long delay) {
        mainHandler.removeCallbacks(reloadRunnable);

        mainHandler.postDelayed(reloadRunnable, delay);
    }

    public class Lock {
        private boolean stop;
        private boolean multiplayer;
        private boolean internet;
        private boolean gap;
        private boolean game;

        public boolean isLocked() {
            Helper.wtf("I","isLocked: " + "multiplayer [" + multiplayer + "]" + " " + "internet [" + internet + "]" + " " + "gap [" + gap + "]" + " " + "stop [" + stop + "] "+ "game [" + game + "]");
            return multiplayer || internet || gap || game || stop;
        }

        public boolean isMultiplayerLocked(){
            return multiplayer;
        }

        public void unlockStop() {
            Helper.wtf("I","unlockStop: ");
            stop = false;
        }

        public void stopLock() {
            Helper.wtf("I","stopLock: ");
            stop = true;
        }


        public void unlockGap() {
            Helper.wtf("I","unlockGap: ");
            gap = false;
        }

        public void gapLock() {
            Helper.wtf("I","gapLock: ");
            gap = true;
        }

        public void lockMultiplayer() {
            Helper.wtf("I","lockMultiplayer: ");
            multiplayer = true;
        }

        public void unlockMultiplayer() {
            Helper.wtf("I","unlockMultiplayer: ");
            multiplayer = false;
        }

        public void gameUnlock() {
            Helper.wtf("I","gameUnlock: ");
            game = false;
        }

        public void gameLock() {
            Helper.wtf("I","gameLock: ");
            game = true;
        }

        public void internetLock() {
            Helper.wtf("I","internetLock: ");
            internet = true;
        }

        public void internetUnlock() {
            Helper.wtf("I","internetUnlock: ");
            internet = false;
        }
    }
}
