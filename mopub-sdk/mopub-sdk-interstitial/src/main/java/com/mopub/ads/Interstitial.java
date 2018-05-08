package com.mopub.ads;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.mojang.base.Analytics;
import com.mojang.base.FileManager;
import com.mojang.base.Helper;
import com.mojang.base.Screen;
import com.mojang.base.json.Data;
import com.mopub.ads.adapters.FastAd;
import com.mopub.common.ClientMetadata;
import com.mopub.mobileads.AdViewController;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;
import com.unity3d.ads.UnityAds;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Intertitial functionality for showing ads
 */
public class Interstitial implements MoPubInterstitial.InterstitialAdListener {

    private static final long DISABLE_SCREEN_MILLS = 4000;
    private @Nullable MoPubInterstitial mopubInterstitial;
    public final Activity minecraftActivity;

    private final Handler mainHandler;
    private String TAG = this.getClass().getName();
    private final Runnable reloadRunnable;
    private double backOffPower = 1;
    private Runnable periodicShowRunnable;
    private Runnable showRunnable;
    private final Runnable gapUnlockRunnable;
    private double periodicMills;
    private FastAd fastAd;
    private boolean onLoadedOnce;
    private boolean periodicScheduled;
    public final Lock lock;
    private Method nativeBackPressedMethod;
    public boolean pauseScreenShowed;
    public static boolean FAST_BACK_PRESS;
    public boolean dontBackPress;
    private int curentVolume;
    public AudioManager audioManager;
    public boolean fastAdShowed;

    public Interstitial(final Activity activity) {
        this.minecraftActivity = activity;
        this.periodicMills = Helper.FasterAds() ? 25000 : Data.Ads.Interstitial.periodicShowMillsLow;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.lock = new Lock();
        this.audioManager = (AudioManager) activity.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);


        this.reloadRunnable = new Runnable() {
            @Override
            public void run() {
                //mopub can be null if we use fast ad late
                if (mopubInterstitial != null) {
                    mopubInterstitial.load();
                    return;
                }
                //this means we dont get onLoad ani onDismissed
                //so we have to try manually again
                mainHandler.postDelayed(this, 5000);
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
                Helper.wtf("PeriodicShowRunnable", "isLocked: " + "multiplayerLocalOnline [" + lock.localMultiplayer + " "+lock.onlineMultiplayer+ "]" + " " + "internet [" + lock.internet + "]" + " " + "gap [" + lock.gap + "]" + " " + "stop [" + lock.stop + "] " + "game [" + lock.game + "]");
                if (!lock.isAnyLocked()) {
                    show(true);
                }
            }
        };
        this.periodicShowRunnable = new Runnable() {
            @Override
            public void run() {
                showRunnable.run();
                mainHandler.postDelayed(periodicShowRunnable, (long) periodicMills);
            }
        };

        getNativeBackPressed();
    }

    private void getNativeBackPressed() {
        try {
            nativeBackPressedMethod = minecraftActivity.getClass().getMethod("callNativeBackPressed");
            Helper.wtf("got nativeBackPressed");
        } catch (NoSuchMethodException e) {
            Helper.wtf("----NATIVE BACK PRESS MISSING----", true);
        }
    }

    public void hideNavigationBarDelayed(final Activity activity) {
        int delay = FAST_BACK_PRESS ? 2500 : 5500;

        FAST_BACK_PRESS = false;
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean hasMenuKey = ViewConfiguration.get(activity).hasPermanentMenuKey();
                    boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
                    Helper.wtf("hasMenuKey(false) = " + hasMenuKey + " hasBackKey(false) =" + hasBackKey);
                    if (!hasMenuKey && !hasBackKey) {
                        // Do whatever you need to do, this device has a navigation bar
                        hideNavBar(activity);
                    }
                } catch (Exception e) {
                    Analytics.i().sendException(e);
                }
            }
        }, delay);
    }

    private static void hideNavBar(Activity activity) {
        if (Data.hasMinecraft) {
            View decorView = activity.getWindow().getDecorView();
            int currentVis = decorView.getSystemUiVisibility();
            int hidenVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            Helper.wtf("Curent visibility " + currentVis + " hiddenVisibility " + hidenVisibility);
            Helper.wtf("HIDING NAVBAR", true);

            decorView.setSystemUiVisibility(hidenVisibility);
        }
    }

    public void callNativeBackPressed() {
        if (pauseScreenShowed) {
            int delayMillis = FAST_BACK_PRESS ? 500 : 1555;
            FAST_BACK_PRESS = false;
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (nativeBackPressedMethod != null && !dontBackPress) {
                            nativeBackPressedMethod.invoke(minecraftActivity);
                            dontBackPress = false;
                            Helper.wtf("called -- NativeBackPressed");
                        }else{
                            Helper.wtf("nativeBackPressedMethod != null = "+ (nativeBackPressedMethod != null) + " dontBackPress = " + dontBackPress);
                        }
                    } catch (InvocationTargetException e) {
                        Helper.wtf("failed back press");
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        Helper.wtf("failed back press");
                        e.printStackTrace();
                    }
                    pauseScreenShowed = false;
                }
            }, delayMillis);
        }
    }


    @Override
    public void onInterstitialDismissed(MoPubInterstitial interstitial) {
        Helper.wtf("onInterstitialDismissed", true);
        gapLockForTime(Data.Ads.Interstitial.minimalGapMills);
        Helper.setVolume(curentVolume, audioManager);
        loadAfterDelay(3000);

        callNativeBackPressed();
        hideNavigationBarDelayed(minecraftActivity);
        nesmrtelnost(false, 15000);
    }


    @Override
    public void onInterstitialLoaded(MoPubInterstitial interstitial) {
        Helper.wtf("Interstitial: onInterstitialLoaded", true);

        if (!onLoadedOnce) {
            String country = getCountryCode();
            if (country != null && !country.isEmpty()) {
                setPeriodicMillsAndFingerChance(country);
                lockOutSE(country);
            }
            schedulePeriodicShows();
            onLoadedOnce = true;
        }
        System.gc();
    }


    /**
     * @return country code like SK,US http://www.mcc-mnc.com/
     */
    @Nullable
    private String getCountryCode() {
        try {
            String c1 = AdViewController.getCountryCodeFromMopubResponse();
            if (c1 != null) return c1.toUpperCase();
            String c2 = ClientMetadata.getInstance().getIsoCountryCode();
            if (c2 != null) return c2.toUpperCase();
            return ClientMetadata.getInstance().getSimIsoCountryCode().toUpperCase();
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode) {
        Helper.wtf(TAG, "onInterstitialFailed: " + errorCode);

        if (errorCode.equals(MoPubErrorCode.NO_FILL) || errorCode.equals(MoPubErrorCode.UNSPECIFIED)) {
            final double BACKOFF_FACTOR = 1.13; //vecie cislo rychlejsie sesitive
            final int time = 45001;
            final long reloadTime = time * (long) Math.pow(BACKOFF_FACTOR, backOffPower);
            backOffPower++;
            Helper.wtf("Loading again in " + reloadTime);
            loadAfterDelay(reloadTime);
        }
    }

    @Override
    public void onInterstitialShown(MoPubInterstitial interstitial) {
        Helper.wtf("onInterstitialShown", true);
        curentVolume = Helper.setQuietVolume(audioManager);
    }

    @Override
    public void onInterstitialClicked(MoPubInterstitial interstitial) {
        Helper.wtf("onInterstitialClicked", true);

        MoPubInterstitial.AdType adType = interstitial.getAdType();

        if (adType == MoPubInterstitial.AdType.ADMOB) {
            showBlackScreen(minecraftActivity, Data.Ads.Interstitial.disableTouchChance);
        }

    }

    public boolean show(boolean isPeriodicShow) {
        boolean showSuccesful = false;
        boolean isMopubNull = mopubInterstitial == null;
        Helper.wtf("I", "isLocked: " + "multiplayerLocalOnline [" + lock.localMultiplayer + " "+lock.onlineMultiplayer+ "]" + " " + "internet [" + lock.internet + "]" + " " + "gap [" + lock.gap + "]" + " " + "stop [" + lock.stop + "] " + "game [" + lock.game + "]");
        boolean isLocked = isPeriodicShow ? lock.isAnyLocked() : lock.isHardLocked();
        boolean isMopubReady = !isMopubNull && mopubInterstitial.isReady();
        Helper.wtf("[isMopubNull(false) = " + isMopubNull + "] " + "[isSoftLocked(false) = " + lock.isSoftLocked() + "] " +  "[isHardLocked(false) = " + lock.isHardLocked() + "] " +"[isMopubReady(true) = " + isMopubReady + "]");
        if (!fastAdShowed && fastAd != null && !isLocked) {
            nesmrtelnost(true);
            fastAd.show(mopubInterstitial);
            fastAd = null;
        } else if (!isMopubNull && !isLocked && isMopubReady) {
            Helper.wtf("Showing mopubInterstitial", true);
            nesmrtelnost(true);
            showSuccesful = mopubInterstitial.show();
        }
        return showSuccesful;
    }

    public void show(int delay, final boolean isPeriodicShow) {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                show(isPeriodicShow);
            }
        }, delay);
    }

    private void nesmrtelnost(boolean zapnut) {
        if (Data.hasMinecraft) try {
            if (zapnut) nesmrtelnostON();
            else nesmrtelnostOFF();
        } catch (UnsatisfiedLinkError ignored) {
            Helper.wtf("!Failed to zapnut nesmrtelnost");
        }
        if(Data.hasMinecraft) {
            Helper.wtf("Nesmrtelnos = " + zapnut);
        }
    }

    private void nesmrtelnost(final boolean zaplnut, int delay) {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                nesmrtelnost(zaplnut);
            }
        }, delay);
    }

    public native void nesmrtelnostON();

    public native void nesmrtelnostOFF();


    public void destroy() {
        if (mopubInterstitial != null) {
            mopubInterstitial.destroy();
        }
    }


    public void init(final boolean fromOnlineAccepted) {
        //If we played online and just accepted to play online just init slowly ads
        if (fromOnlineAccepted) {
            _initDelayed(4000);
        } else if (Data.hasMinecraft) {
            //We are using fast ad if we have minecraft game
            if (!fastAdShowed) {
                Helper.wtf(TAG, "Interstitial init and load fast ad");
                fastAd = new FastAd(Data.Ads.Interstitial.failoverId, this);
                fastAd.load(minecraftActivity, new Runnable() {
                    @Override
                    public void run() {
                        _initDelayed(4000);
                        gapLockForTime(Data.Ads.Interstitial.minimalGapMills);
                    }
                });
            } else {
                //We are initializin interstitial and we already showed fast ad this should not happen
                _initDelayed(4000);
            }
        } else {
            //We have victim app we dont use fast ad here so just normal slow init
            //Also we don use game lock
            lock.game = false;
            _initDelayed(4000);
        }
    }


    public void showUnityAdsVideo() {
        if (!lock.isOnlineMultiplayerLocked() && !lock.isHardLocked()) {
            if (!UnityAds.isReady()) {
                Helper.wtf(TAG, "showUnityAdsVideo: show false");
                show(true);
            } else {
                gapLockForTime(Data.Ads.Interstitial.minimalGapMills);
                UnityAds.show(minecraftActivity);
            }
        } else {
            Helper.wtf(TAG, "showUnityAdsVideo: show false multiplayer locked or hard locked");
        }
    }


    public void schedulePeriodicShows() {
        if (!periodicScheduled) {
            Helper.wtf("schedulePeriodicShows: Scheduled za " + String.valueOf(periodicMills));
            mainHandler.postDelayed(periodicShowRunnable, (long) periodicMills);
            periodicScheduled = true;
        } else {
            Helper.wtf("Not scheduling periodic cause already scheduled");
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


    public void _initDelayed(int delay) {
        Helper.wtf("Initing Mopub in 4 sec...");
        Helper.runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                if (mopubInterstitial == null) {
                    mopubInterstitial = new MoPubInterstitial(minecraftActivity, Data.Ads.Interstitial.id);
                    mopubInterstitial.setInterstitialAdListener(Interstitial.this);
                    mopubInterstitial.setKeywords("game,minecraft,business,twitter");
                    mopubInterstitial.load();
                } else if (!mopubInterstitial.isReady()) {
                    Helper.wtf("Mopub Forcing Refresh");
                    mopubInterstitial.forceRefresh();
                }

                if (UnityAds.isSupported() && !UnityAds.isInitialized()) {
                    Helper.wtf("Initing Unity ads");
                    //final String _69633 = Helper.convertString("4E6A6B324D7A4D3D");
                    UnityAds.initialize(minecraftActivity, null, null, Helper.USE_UNITY_TEST_ADS);
                }
            }
        }, delay);
    }

    @SuppressLint("CommitPrefEdits")
    private void lockOutSE(String countryCode) {
        if (countryCode == null) return;
        final String country = "Country";
        minecraftActivity.getSharedPreferences(country, Context.MODE_PRIVATE).edit().putString(country, countryCode).apply();
        if (!countryCode.equals("SE")) return;

        //create file
        FileManager.i().put(FileManager.SE, null);
        Helper.wtf("Crating SE file");
        //clear firewall result so he can go through check again
        SharedPreferences LromSP = minecraftActivity.getApplicationContext().getSharedPreferences("vic", Context.MODE_PRIVATE);
        LromSP.edit().clear().apply();
        //sendAnalitics
        Analytics.i().sendOther("SECreated", countryCode);
        try {
            minecraftActivity.finishAffinity();
        } catch (Exception e) {
            Analytics.i().sendException(e);
            System.exit(0);
        }
    }


    void setPeriodicMillsAndFingerChance(String interstitialCountryCode) {
        //we have to split all hightECPmCountires cause they might have chance with them SK-0.23
        for (String countyAndChance : Data.Ads.Interstitial.highEcpmCountries) {
            String codeAndChance[] = countyAndChance.split("-");
            String countryCode = codeAndChance[0];

            if (countryCode.equals(interstitialCountryCode)) {
                periodicMills = Data.Ads.Interstitial.periodicShowMillsHigh;
            }
        }
    }


    private void gapLockForTime(long minimalAdGapMills) {
        lock.gapLock();
        Helper.wtf(TAG, "lockForTime: scheduling unlock runnable za sec " + minimalAdGapMills / 1000);
        mainHandler.removeCallbacks(gapUnlockRunnable);
        mainHandler.postDelayed(gapUnlockRunnable, minimalAdGapMills);
    }

    public void showBlackScreen(Activity activity, double disableTouchChance) {
        /**
         * Note: this was casing the black view to stay on screen when applovin add
         * was pressed instantaneously . We disabled it for now will see the $$ impact
         */
        if (Helper.chance(disableTouchChance) && Data.hasMinecraft) {
            Screen.i().disableTouch(activity, DISABLE_SCREEN_MILLS);
        }
    }

    private void loadAfterDelay(long delay) {
        try {
            Helper.removeFromWorkerThread(reloadRunnable);
        } catch (Exception e) {
            Analytics.i().sendException(e);
            e.printStackTrace();
        }
        Helper.runOnWorkerThread(reloadRunnable, delay);
    }

    public class Lock {
        private boolean stop;
        private boolean onlineMultiplayer;
        private boolean localMultiplayer;
        private boolean internet;
        private boolean gap;
        private boolean game = true;


        public boolean isHardLocked() {
            //we never show in these conditions
            return gap || internet || stop || localMultiplayer;
        }
        public boolean isSoftLocked() {
            //we can show in these conditions
            return onlineMultiplayer || game;
        }

        public boolean isAnyLocked() {
            return onlineMultiplayer || game || gap || internet || stop || localMultiplayer;
        }

        public boolean isOnlineMultiplayerLocked() {
            return onlineMultiplayer;
        }

        public void unlockStop() {
            Helper.wtf("I", "unlockStop: ");
            stop = false;
        }

        public void stopLock() {
            Helper.wtf("I", "stopLock: ");
            stop = true;
        }


        public void unlockGap() {
            Helper.wtf("I", "unlockGap: ");
            gap = false;
        }

        public void gapLock() {
            Helper.wtf("I", "gapLock: ");
            gap = true;
        }

        public void lockMultiplayer() {
            Helper.wtf("I", "lockMultiplayer: ");
            onlineMultiplayer = true;
        }

        public void unlockOnlineMultiplayer() {
            Helper.wtf("I", "unlockOnlineMultiplayer: ");
            onlineMultiplayer = false;
        }

        public void gameUnlock() {
            Helper.wtf("I", "gameUnlock: ");
            game = false;
        }

        public void gameLock() {
            Helper.wtf("I", "gameLock: ");
            game = true;
        }

        public void internetLock() {
            Helper.wtf("I", "internetLock: ");
            internet = true;
        }

        public void internetUnlock() {
            Helper.wtf("I", "internetUnlock: ");
            internet = false;
        }

        public void unlockLocalMultiplayer() {
            Helper.wtf("I", "unlockLocalMultiplayer: ");
            localMultiplayer = false;
        }

        public void lockLocalMultiplayer() {
            Helper.wtf("I", "lockLocalMultiplayer: ");
            localMultiplayer = true;
        }

        public boolean isLocalMultiplayerLocked() {
            return localMultiplayer;
        }

        public boolean isGapLocked() {
            return gap;
        }
    }
}
