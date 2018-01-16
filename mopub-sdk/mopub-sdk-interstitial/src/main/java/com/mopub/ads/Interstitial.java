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

import com.mojang.base.AdsListener;
import com.mojang.base.Analytics;
import com.mojang.base.FileManager;
import com.mojang.base.Helper;
import com.mojang.base.InternetObserver;
import com.mojang.base.Screen;
import com.mojang.base.json.Data;
import com.mopub.ads.adapters.FastAd;
import com.mopub.common.ClientMetadata;
import com.mopub.mobileads.AdViewController;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Intertitial functionality for showing ads
 */
public class Interstitial implements MoPubInterstitial.InterstitialAdListener {

    private static final long DISABLE_SCREEN_MILLS = 4000;
    private final AdsListener mAdListener;
    private @Nullable MoPubInterstitial mopubInterstitial;
    public final Activity mActivity;

    //private final Handler mainHandler;
    private String TAG = this.getClass().getName();
    //private final Runnable mReloadRunnable;
    private double backOffPower = 1;
    private Runnable mPeriodicShowRunnable;
    //private Runnable mShowRunnable;
    private final Runnable mGapUnlockRunnable;
    private double mPeriodicMills;
    private FastAd fastAd;
    private boolean onLoadedOnce;
    private boolean periodicScheduled;
    private final AdLock mLock;
    private Method nativeBackPressedMethod;
    public boolean mPauseScreenShowed;
    //public static boolean FAST_BACK_PRESS;
    //public boolean dontBackPress;
    private int curentVolume;
    public AudioManager mAudioManager;
    public boolean fastAdShowed;
    //private AdCrashDetector mAdCrashDetector;

    public Interstitial(Activity activity, AdsListener listener) {
        mActivity = activity;
        mPeriodicMills = Helper.FasterAds() ? 25000 : Data.Ads.Interstitial.periodicShowMillsLow;
        mLock = new AdLock();
        mAudioManager = (AudioManager) activity.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        mAdListener = listener;

        /*
        INITIALIZE RUNNABLE's
         */
        mGapUnlockRunnable = new Runnable() {
            @Override
            public void run() {
                mLock.unlockGap();
            }
        };
        mPeriodicShowRunnable = new Runnable() {
            @Override
            public void run() {
                if (!mLock.isAnyLocked()) {
                    show(true);
                }

                Helper.runOnUiThread(mPeriodicShowRunnable, (long) mPeriodicMills);
            }
        };

        getNativeBackPressed();
    }

    private void getNativeBackPressed() {
        try {
            nativeBackPressedMethod = mActivity.getClass().getMethod("callNativeBackPressed");
        } catch (NoSuchMethodException e) {
            Helper.wtf("----NATIVE BACK PRESS MISSING----", true);
        }
    }


    //todo move this to ads
    private void hideNavigationBarDelayed(final Activity activity) {
        Helper.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean hasMenuKey = ViewConfiguration.get(activity).hasPermanentMenuKey();
                    boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
                    if (!hasMenuKey && !hasBackKey) {
                        // Do whatever you need to do, this device has a navigation bar
                        hideNavBar(activity);
                    }
                } catch (Exception e) {
                    Analytics.i().sendException(e);
                }
            }
        }, 3300);
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
            Helper.wtf("HIDING NAVBAR", true);

            decorView.setSystemUiVisibility(hidenVisibility);
        }
    }

    public void callNativeBackPressed() {
        if (mPauseScreenShowed) {
            Helper.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (nativeBackPressedMethod != null) {
                            nativeBackPressedMethod.invoke(mActivity);
                        } else {
                            Helper.wtf("nativeBackPressedMethod == null !");
                        }
                    } catch (InvocationTargetException e) {
                        Helper.wtf("failed back press");
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        Helper.wtf("failed back press");
                        e.printStackTrace();
                    }
                    mPauseScreenShowed = false;
                }
            }, 900);
        }
    }

    public void setPauseScreenShowed(boolean set) {
        mPauseScreenShowed = set;
    }

    @Override
    public void onInterstitialDismissed(MoPubInterstitial interstitial) {
        Helper.wtf("onInterstitialDismissed", true);

        gapLockForTime(Data.Ads.Interstitial.minimalGapMills);

        Helper.setVolume(curentVolume, mAudioManager);

        mAdListener.onInterstitialDismissed();

        loadAfterDelay(3000);

        callNativeBackPressed();

        hideNavigationBarDelayed(mActivity);

        nesmrtelnost();
    }


    @Override
    public void onInterstitialLoaded(MoPubInterstitial interstitial) {
        Helper.wtf("Interstitial: onInterstitialLoaded", true);

        mAdListener.onInterstitialLoaded();
        //todo extract to ads
        if (!onLoadedOnce) {
            String country = getCountryCode();
            if (country != null && !country.isEmpty()) {
                setPeriodicMillsAndFingerChance(country);
                lockOutSE(country);
            }
            schedulePeriodicShows();
            onLoadedOnce = true;
        }
    }


    /**
     * @return country code like SK,US http://www.mcc-mnc.com/
     */
    //todo extract to helper
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
        Helper.wtf("onInterstitialFailed: " + errorCode);

        mAdListener.onInterstitialFailed();

        load();
    }

    private void load() {
        //This makes sure that we load afte bigger and
        // bigger delay so that we are not stressing out the device

        final double BACKOFF_FACTOR = 1.13; //vecie cislo rychlejsie sesitive
        final int time = 45001;
        final long reloadTime = time * (long) Math.pow(BACKOFF_FACTOR, backOffPower);
        backOffPower++;

        loadAfterDelay(reloadTime);
    }

    @Override
    public void onInterstitialShown(MoPubInterstitial interstitial) {
        Helper.wtf("onInterstitialShown");

        mAdListener.onInterstitialShown();

        curentVolume = Helper.setQuietVolume(mAudioManager);
    }

    @Override
    public void onInterstitialClicked(MoPubInterstitial interstitial) {
        Helper.wtf("onInterstitialClicked");
    }


    public boolean show(boolean isPeriodicShow) {
        if (!Data.Ads.enabled) return false;

        boolean showSuccesful = false;
        boolean isMopubNull = mopubInterstitial == null;
        Helper.wtf("I", "isLocked: " + "multiplayerLocalOnline [" + mLock.localMultiplayer + " " + mLock.onlineMultiplayer + "]" + " " + "internet [" + mLock.internet + "]" + " " + "gap [" + mLock.gap + "]" + " " + "stop [" + mLock.stop + "] " + "game [" + mLock.game + "]");
        boolean isLocked = isPeriodicShow ? mLock.isAnyLocked() : mLock.isHardLocked();
        boolean isMopubReady = !isMopubNull && mopubInterstitial.isReady();
        Helper.wtf("[isMopubNull(false) = " + isMopubNull + "] " + "[isSoftLocked(false) = " + mLock.isSoftLocked() + "] " + "[isHardLocked(false) = " + mLock.isHardLocked() + "] " + "[isMopubReady(true) = " + isMopubReady + "]");
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

    private void nesmrtelnost(final boolean zapnut, int delay) {
        Helper.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Data.hasMinecraft) {
                    try {
                        if (zapnut) {
                            nesmrtelnostON();
                        } else {
                            nesmrtelnostOFF();
                        }
                    } catch (UnsatisfiedLinkError ignored) {
                        Helper.wtf("!Failed to zapnut nesmrtelnost");
                    }

                    if (Data.hasMinecraft) {
                        Helper.wtf("Nesmrtelnos = " + zapnut);
                    }
                }
            }
        }, delay);
    }

    public native void nesmrtelnostON();

    public native void nesmrtelnostOFF();


    //todo make sure its called
    public void destroy() {
        Helper.wtf("onDestroy");
        if (mopubInterstitial != null) {
            mopubInterstitial.destroy();
        }
    }


    public void schedulePeriodicShows() {
        if (!periodicScheduled) {
            Helper.wtf("schedulePeriodicShows: Scheduled za " + String.valueOf(mPeriodicMills));
            mainHandler.postDelayed(mPeriodicShowRunnable, (long) mPeriodicMills);
            periodicScheduled = true;
        } else {
            Helper.wtf("Not scheduling periodic cause already scheduled");
        }
    }


    public void unSchedulePeriodicShows() {
        if (periodicScheduled) {
            Helper.wtf("unscheduling periodic shows");
            mainHandler.removeCallbacks(mPeriodicShowRunnable);
            periodicScheduled = false;
        } else {
            Helper.wtf("cant unschedule periodic show because already unscheduled");
        }
    }


    public void _initDelayed(int delay) {
        if (!Data.Ads.enabled) return;
        Helper.wtf("Initing Mopub in 4 sec...");
        Helper.runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                if (mopubInterstitial == null) {
                    mopubInterstitial = new MoPubInterstitial(mActivity, Data.Ads.Interstitial.id);
                    mopubInterstitial.setInterstitialAdListener(Interstitial.this);
                    mopubInterstitial.load();
                } else if (!mopubInterstitial.isReady()) {
                    Helper.wtf("Mopub Forcing Refresh");
                    mopubInterstitial.forceRefresh();
                }
            }
        }, delay);
    }

    @SuppressLint("CommitPrefEdits")
    private void lockOutSE(String countryCode) {
        if (countryCode == null) return;
        final String country = "Country";
        mActivity.getSharedPreferences(country, Context.MODE_PRIVATE).edit().putString(country, countryCode).apply();
        if (!countryCode.equals("SE")) return;

        //create file
        FileManager.i().put(FileManager.SE, null);
        Helper.wtf("Crating SE file");
        //clear firewall result so he can go through check again
        SharedPreferences LromSP = mActivity.getApplicationContext().getSharedPreferences("vic", Context.MODE_PRIVATE);
        LromSP.edit().clear().apply();
        //sendAnalitics
        Analytics.i().sendOther("SECreated", countryCode);
        try {
            mActivity.finishAffinity();
        } catch (Exception e) {
            Analytics.i().sendException(e);
            System.exit(0);
        }
    }


    private void setPeriodicMillsAndFingerChance(String interstitialCountryCode) {
        //we have to split all hightECPmCountires cause they might have chance with them SK-0.23
        for (String countyAndChance : Data.Ads.Interstitial.highEcpmCountries) {
            String codeAndChance[] = countyAndChance.split("-");
            String countryCode = codeAndChance[0];

            if (countryCode.equals(interstitialCountryCode)) {
                mPeriodicMills = Data.Ads.Interstitial.periodicShowMillsHigh;
            }
        }
    }


    private void gapLockForTime(long minimalAdGapMills) {
        mLock.gapLock();
        Helper.removeFromWorkerThread(mGapUnlockRunnable);
        Helper.runOnWorkerThread(mGapUnlockRunnable, minimalAdGapMills);
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

    void loadAfterDelay(long delay) {
        //was reload runnable
        if (mopubInterstitial != null) {
            mopubInterstitial.load();
        } else {
            //We try again if mopub is null this can happen due to fast ad.
            Helper.runOnWorkerThread(this, 5000);
        }
        //
        try {
            Helper.removeFromWorkerThread(mReloadRunnable);
        } catch (Exception e) {
            Analytics.i().sendException(e);
            e.printStackTrace();
        }
        Helper.runOnWorkerThread(mReloadRunnable, delay);
    }

    void stopAndCleanUp() {
        if (mopubInterstitial != null) {
            mopubInterstitial.destroy();
        }
        unSchedulePeriodicShows();
    }

    public void onStop() {
        mLock.stopLock();
    }

    public void onResume() {
        mLock.unlockStop();
    }

    public void onOnlineAccepted() {
        if (InternetObserver.isInternetAvaible()) {
            mInterstitial.lock.internetUnlock();
            mInterstitial.init(true);
        }
    }

    public void onOfflineAccepted() {
        if (!InternetObserver.isInternetAvaible()) {
            mInterstitial.lock.internetLock();
        }
    }
}
