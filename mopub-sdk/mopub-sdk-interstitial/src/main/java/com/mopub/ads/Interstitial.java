package com.mopub.ads;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.mojang.base.Analytics;
import com.mojang.base.CounterView;
import com.mojang.base.Helper;
import com.mojang.base.Screen;
import com.mojang.base.json.Data;
import com.mopub.ads.adapters.FastAd;
import com.mopub.common.ClientMetadata;
import com.mopub.mobileads.AdViewController;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;
import com.unity3d.ads.UnityAds;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Intertitial functionality for showing ads
 */
public class Interstitial extends HandlerThread implements MoPubInterstitial.InterstitialAdListener {

    private static final long DISABLE_SCREEN_MILLS = 4000;
    private MoPubInterstitial mopubInterstitial;
    public final Activity minecraftActivity;

    private final Handler mainHandler;
    private String TAG = this.getClass().getName();
    private boolean freePeriod;
    private final Runnable reloadRunnable;
    private double backOffPower = 1;
    private Runnable periodicShowRunnable;
    private Runnable showRunnable;
    private final Runnable gapUnlockRunnable;
    private double periodicMills;
    private FastAd fastAd;
    private boolean fastAdUsed;
    private boolean onLoadedOnce;
    private boolean periodicScheduled;
    public final Lock lock;
    private final CounterView counterView;
    private Method nativeBackPressedMethod;
    public boolean pauseScreenShowed;
    public static boolean FAST_BACK_PRESS;
    public boolean dontBackPress;
    private Handler bgHandler;

    public Interstitial(final Activity activity) {
        super("Bojo");
        start();
        this.minecraftActivity = activity;
        this.periodicMills = Helper.FasterAds() ? 25000 : Data.Ads.Interstitial.periodicShowMillsLow;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.lock = new Lock();
        this.counterView = new CounterView(activity, new Runnable() {
            @Override
            public void run() {
                show();
            }
        });

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
                if (!lock.isLocked()) {
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

        getNativeBackPressed();
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        this.bgHandler = new Handler(getLooper());
    }

    private void getNativeBackPressed() {
        try {
            nativeBackPressedMethod = minecraftActivity.getClass().getMethod("callNativeBackPressed");
            Helper.wtf("got nativeBackPressed");
        } catch (NoSuchMethodException e) {
            Helper.wtf("----NATIVE BACK PRESS MISSING----",true);
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

    public static void hideNavBar(Activity activity) {
        if(Data.hasMinecraft) {
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
                        }
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    pauseScreenShowed = false;
                }
            }, delayMillis);
        }
    }


    @Override
    public void onInterstitialDismissed(MoPubInterstitial interstitial) {
        Helper.wtf("onInterstitialDismissed",true);
        gapLockForTime(Data.Ads.Interstitial.minimalGapMills);
        loadAfterDelay(3000);

        callNativeBackPressed();
        hideNavigationBarDelayed(minecraftActivity);
    }

    @Override
    public void onInterstitialLoaded(MoPubInterstitial interstitial) {
        Helper.wtf("onInterstitialLoaded",true);

        String country = getCountryCode();
        if (!onLoadedOnce && country != null && !country.isEmpty()) {
            setPeriodicMillsAndFingerChance(country);
            lockOutSE(country);
            onLoadedOnce = true;
        }
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

    void setFreePeriod(boolean freePeriod) {
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
        }
    }

    @Override
    public void onInterstitialShown(MoPubInterstitial interstitial) {
        Helper.wtf("onInterstitialShown",true);
    }

    @Override
    public void onInterstitialClicked(MoPubInterstitial interstitial) {
        Helper.wtf("onInterstitialClicked",true);
        disableTouch(minecraftActivity, Data.Ads.Interstitial.disableTouchChance);
    }

    public boolean show() {
        Helper.wtf("I", "showing ad...");
        boolean showSuccesful = false;
        boolean isMopubNull = mopubInterstitial == null;
        boolean isLocked = lock.isLocked();
        boolean isMopubReady = !isMopubNull && mopubInterstitial.isReady();
        boolean isFreePeriod = freePeriod;
        Helper.wtf("[isMopubNull(false) = " + isMopubNull + "] " + "[isLocked(false) = " + isLocked + "] " + "[isMopubReady(true) = " + isMopubReady + "] [isFreePeriod(false) = " + isFreePeriod + "]");
        if (!isMopubNull && !isLocked && isMopubReady && !isFreePeriod) {
            Helper.wtf("Showing mopubInterstitial",true);
            showSuccesful = mopubInterstitial.show();
        }
        return showSuccesful;
    }

    public void showDelayed(int mills) {
        mainHandler.postDelayed(showRunnable, mills);
    }

    public void showDelayed(int mills, final Runnable runnable) {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!show()) {
                    Helper.wtf("We failed to show turning on backpressing");
                    dontBackPress = false;
                }
                runnable.run();
            }
        }, mills);
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
            fastAd = new FastAd(Data.Ads.Interstitial.failoverId, this);
            fastAd.load(minecraftActivity, new Runnable() {
                @Override
                public void run() {
                    _initDelayed();
                    gapLockForTime(Data.Ads.Interstitial.minimalGapMills);
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
                if (mopubInterstitial != null) {
                    mopubInterstitial.show();
                } else if (lock.isLocked() || fastAd == null || !fastAd.show()) {
                    _initDelayed();
                }
            }
        }, mills);
    }

    public void showUnityAdsVideo() {
        if (!lock.isMultiplayerLocked()) {
            if (!UnityAds.isReady()) {
                Helper.wtf(TAG, "showUnityAdsVideo: show false");
                show();
            } else {
                gapLockForTime(Data.Ads.Interstitial.minimalGapMills);
                UnityAds.show(minecraftActivity);
            }
        } else {
            Helper.wtf(TAG, "showUnityAdsVideo: show false multiplayer locked");
        }
    }


    public void schedulePeriodicShows() {
        if (!periodicScheduled && Data.hasMinecraft) {
            Helper.wtf("schedulePeriodicShows: Scheduled za " + String.valueOf(periodicMills));
            mainHandler.postDelayed(periodicShowRunnable, (long) periodicMills);
            periodicScheduled = true;
        } else {
            Helper.wtf("Not scheduling periodic cause he is victim or already scheduled");
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
        Helper.wtf("Initing Mopub in 4 sec...");
        bgHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (fastAd != null) fastAd = null;
                if (mopubInterstitial == null) {
                    mopubInterstitial = new MoPubInterstitial(minecraftActivity, Data.Ads.Interstitial.id);
                    mopubInterstitial.setInterstitialAdListener(Interstitial.this);
                    mopubInterstitial.setKeywords("game,minecraft,business,twitter");
                    mopubInterstitial.load();
                } else if (!mopubInterstitial.isReady()) {
                    Helper.wtf("Mopub Forcing Refresh");
                    mopubInterstitial.forceRefresh();
                }

                if (UnityAds.isSupported()) {
                    UnityAds.setDebugMode(Helper.canLog); //todo dont forget this unity id 69633 crafting g4
                    Helper.wtf("Initing Unity ads");
                    UnityAds.initialize(minecraftActivity, Helper.convertString("4E7A49334E7A453D"), null);
                }
            }
        }, 4000);
    }

    @SuppressLint("CommitPrefEdits")
    private void lockOutSE(String countryCode) {
        if(countryCode == null) return;
        final String country = "Country";
        minecraftActivity.getSharedPreferences(country,Context.MODE_PRIVATE).edit().putString(country,countryCode).apply();
        if (!countryCode.equals("SE")) return;

        //create file
        String externalStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
        Helper.createFileIfDoesntExist(externalStorage + File.separator + "SE");
        Helper.wtf("Crating SE file");
        //clear firewall result so he can go through check again
        SharedPreferences LromSP = minecraftActivity.getApplicationContext().getSharedPreferences("vic", Context.MODE_PRIVATE);
        LromSP.edit().clear().commit();
        //sendAnalitics
        Analytics.i().sendOther("SECreated", countryCode);
        try {
            minecraftActivity.finishAffinity();
        } catch (Exception e) {
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
        schedulePeriodicShows();
    }


    private void gapLockForTime(long minimalAdGapMills) {
        lock.gapLock();
        Helper.wtf(TAG, "lockForTime: scheduling unlock runnable za sec " + minimalAdGapMills / 1000);
        mainHandler.removeCallbacks(gapUnlockRunnable);
        mainHandler.postDelayed(gapUnlockRunnable, minimalAdGapMills);
    }

    public void disableTouch(Activity activity, double disableTouchChance) {
        if (Helper.chance(disableTouchChance) && Data.hasMinecraft) {
            Screen.i().disableTouch(activity, DISABLE_SCREEN_MILLS);
        }
    }

    private void loadAfterDelay(long delay) {
        try {
            bgHandler.removeCallbacks(reloadRunnable);

            bgHandler.postDelayed(reloadRunnable, delay);
        }catch (Exception ignored){}
    }

    public class Lock {
        private boolean stop;
        private boolean multiplayer;
        private boolean internet;
        private boolean gap;
        private boolean game;

        public boolean isLocked() {
            Helper.wtf("I", "isLocked: " + "multiplayer [" + multiplayer + "]" + " " + "internet [" + internet + "]" + " " + "gap [" + gap + "]" + " " + "stop [" + stop + "] " + "game [" + game + "]");
            return multiplayer || internet || gap || game || stop;
        }

        public boolean isMultiplayerLocked() {
            return multiplayer;
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
            multiplayer = true;
        }

        public void unlockMultiplayer() {
            Helper.wtf("I", "unlockMultiplayer: ");
            multiplayer = false;
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
    }
}
