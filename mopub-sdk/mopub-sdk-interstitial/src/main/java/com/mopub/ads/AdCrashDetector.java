package com.mopub.ads;

import android.content.Context;
import android.content.SharedPreferences;

import com.mojang.base.Analytics;
import com.mojang.base.Helper;
import com.mopub.mobileads.MoPubInterstitial;

/**
 * Detects when ap crashes after and advert is shown and sends  to analytics
 */

public class AdCrashDetector {
    private final String LAST_AD_KEY = "LAST_AD_SHOWN";
    private final String IS_SHOWING_IN_PROCESS_KEY = "SHOWING_IN_PROCESS";
    private SharedPreferences mSharedPreferences;

    public AdCrashDetector(Context context) {
        String SP_KEY = "AD_CRASH_DETECTOR";
        mSharedPreferences = context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE);
        onAppStart();
    }

    public void onInterstitialShown(MoPubInterstitial interstitial) {
        if(interstitial!= null) {
            //We save the name of the interstitial class
            //so that is about to be showed so we can detect crashes afterwards
            mSharedPreferences.edit()
                    .putString(LAST_AD_KEY, interstitial.getAdType().name())
                    .apply();

            //Showing is in process so we know when it crashes
            mSharedPreferences.edit().putBoolean(IS_SHOWING_IN_PROCESS_KEY, true).apply();
        }else{
            Helper.wtf("CrashDetector:: CANT GET AD TYPE INTERSTITIAL ALREADY NULL!!");
        }
    }

    public void onInterstitialDismissed() {
        clear();
    }

    //  Destroy is not called garanteed so we might get some
    //  False reporting when user swipes away our app while watching
    //  Hopefully it doesn't happen that often.
    public void onDestroy() {
        clear();
    }

    private void clear() {
        mSharedPreferences.edit().putString(LAST_AD_KEY, "").apply();
        mSharedPreferences.edit().putBoolean(IS_SHOWING_IN_PROCESS_KEY, false).apply();
    }

    private void onAppStart() {
        //This indicates crash
        boolean wasShowingInProgress = mSharedPreferences.getBoolean(IS_SHOWING_IN_PROCESS_KEY, false);

        if (wasShowingInProgress) {
            //The app had to crash during ad showing
            Analytics.i().sendAdException("AdCrash when showing: " + mSharedPreferences.getString(LAST_AD_KEY, ""));
        }

        clear();
    }
}
