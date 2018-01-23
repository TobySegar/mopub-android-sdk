package com.mopub.ads;


import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.support.annotation.Nullable;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.mojang.base.Analytics;
import com.mojang.base.Helper;
import com.mojang.base.json.Data;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Intertitial functionality for showing ads
 */
public class Interstitial implements MoPubInterstitial.InterstitialAdListener {
    private final MoPubInterstitial.InterstitialAdListener mAdListener;
    private final Runnable mLoadRunnable;
    private @Nullable MoPubInterstitial mopubInterstitial;
    private final Activity mActivity;

    private double backOffPower = 1;
    private Method nativeBackPressedMethod;
    private boolean mPauseScreenShowed;
    private int curentVolume;
    private AudioManager mAudioManager;

    public Interstitial(Activity activity, MoPubInterstitial.InterstitialAdListener listener) {
        mActivity = activity;
        mAudioManager = (AudioManager) activity.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        mAdListener = listener;
        mLoadRunnable = new Runnable() {
            @Override
            public void run() {
                if (mopubInterstitial != null) {
                    mopubInterstitial.load();
                } else {
                    Analytics.i().sendException(new Exception("Couldnt Reload Int"));
                }
            }
        };

        getNativeBackPressed();
    }

    private void getNativeBackPressed() {
        try {
            nativeBackPressedMethod = mActivity.getClass().getMethod("callNativeBackPressed");
        } catch (NoSuchMethodException e) {
            Helper.wtf("----NATIVE BACK PRESS MISSING----");
        }
    }


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
            int hidenVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LOW_PROFILE;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                hidenVisibility = hidenVisibility | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }

            Helper.wtf("HIDING NAVBAR");

            decorView.setSystemUiVisibility(hidenVisibility);
        }
    }

    protected void callNativeBackPressed() {
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

    public void setPauseScreenShowed() {
        mPauseScreenShowed = true;
    }

    @Override
    public void onInterstitialDismissed(MoPubInterstitial interstitial) {
        Helper.wtf("onInterstitialDismissed");

        mAdListener.onInterstitialDismissed(interstitial);

        Helper.setVolume(curentVolume, mAudioManager);

        callNativeBackPressed();

        nesmrtelnost();

        hideNavigationBarDelayed(mActivity);

        loadAfterDelay(3000);
    }


    @Override
    public void onInterstitialLoaded(MoPubInterstitial interstitial) {
        Helper.wtf("Interstitial: onInterstitialLoaded");

        mAdListener.onInterstitialLoaded(interstitial);
    }


    @Override
    public void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode) {
        Helper.wtf("onInterstitialFailed: " + errorCode);

        mAdListener.onInterstitialFailed(interstitial,errorCode);

        reload();
    }

    private void reload() {
        //This makes sure that we reload afte bigger and
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

        mAdListener.onInterstitialShown(interstitial);

        curentVolume = Helper.setQuietVolume(mAudioManager);
    }

    @Override
    public void onInterstitialClicked(MoPubInterstitial interstitial) {
        Helper.wtf("onInterstitialClicked");
    }


    public void show() {
        if (!Data.Ads.enabled) return;
        if (mopubInterstitial == null) {
            Analytics.i().sendException(new Exception("Failed to show int null"));
        }

        if (mopubInterstitial.isReady()) {
             mopubInterstitial.show();
        } else {
            Helper.wtf("Mopub Interstitial Not Ready");
        }
    }

    public void show(int delay) {
        Helper.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                show();
            }
        }, delay);
    }

    private void nesmrtelnost() {
        if (Data.hasMinecraft) {
            //Turn on
            try {
                nesmrtelnostON();
            } catch (UnsatisfiedLinkError ignored) {
            }
            //Turn off after period
            Helper.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        nesmrtelnostOFF();
                    } catch (UnsatisfiedLinkError ignored) {
                    }
                }
            }, 10000);
        }
    }

    public native void nesmrtelnostON();

    public native void nesmrtelnostOFF();


    public void destroy() {
        Helper.wtf("onDestroy");
        if (mopubInterstitial != null) {
            mopubInterstitial.destroy();
        }
    }


    private void init() {
        if (!Data.Ads.enabled) return;

        if (mopubInterstitial == null) {
            Helper.wtf("Initing Mopub ");

            mopubInterstitial = new MoPubInterstitial(mActivity, Data.Ads.Interstitial.id);
            mopubInterstitial.setInterstitialAdListener(Interstitial.this);
        }
    }


    public void loadAfterDelay(long delay) {
        if(mopubInterstitial == null){
            init();
        }

        Helper.removeFromWorkerThread(mLoadRunnable);
        Helper.runOnWorkerThread(mLoadRunnable, delay);
    }

    public boolean isReady() {
        return mopubInterstitial != null && mopubInterstitial.isReady();
    }

}
