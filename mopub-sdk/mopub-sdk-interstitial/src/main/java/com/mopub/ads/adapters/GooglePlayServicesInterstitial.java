package com.mopub.ads.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.mojang.base.Analytics;
import com.mojang.base.Helper;
import com.mojang.base.json.Data;
import com.mopub.ads.Interstitial;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;

import java.util.Calendar;
import java.util.Map;

/*
 * Compatible with version 7.8.0 of the Google Play Services SDK.
 */

// Note: AdMob ads will now use this class as Google has deprecated the AdMob SDK.

public class GooglePlayServicesInterstitial extends CustomEventInterstitial {
    /*
     * These keys are intended for MoPub internal use. Do not modify.
     */
    public static final String AD_UNIT_ID_KEY = "adUnitID";
    public static final String LOCATION_KEY = "location";
    public static final String CLICKS_SP = "clicks";
    private static SharedPreferences sharedPreferences;

    private CustomEventInterstitialListener mInterstitialListener;
    private InterstitialAd mGoogleInterstitialAd;
    private final String debugIntID = Helper.convertString("59324574595842774C5842315969307A4F5451774D6A55324D446B354F5451794E5451304C7A45774D7A4D784E7A4D334D54493D");
    private static Integer currentDayNumber = null;
    private static final String DISABLED_DAY_KEY = "DisabledDay";
    private Context mContext;

    @Override
    protected void loadInterstitial(final Context context, final CustomEventInterstitialListener customEventInterstitialListener,
                                    final Map<String, Object> localExtras, final Map<String, String> serverExtras) {
        mInterstitialListener = customEventInterstitialListener;
        final String adUnitId;

        Helper.wtf("Admob Load");
        setSharedPreferences(context);
        mContext = context;

        if(isDisabled(context)){
            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            return;
        }

        if(true){//if (extrasAreValid(serverExtras)) {
            adUnitId = Helper.DEBUG ? debugIntID : serverExtras.get(AD_UNIT_ID_KEY);
        } else {
            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        mGoogleInterstitialAd = new InterstitialAd(context);
        mGoogleInterstitialAd.setAdListener(new InterstitialAdListener());
        mGoogleInterstitialAd.setAdUnitId(adUnitId);

        final AdRequest adRequest = new AdRequest.Builder()
                .setRequestAgent("MoPub")
                .addTestDevice("E883C2BB7DE538BAADA96556402DA41F")
                .build();

        try {
            mGoogleInterstitialAd.loadAd(adRequest);
        } catch (NoClassDefFoundError e) {
            // This can be thrown by Play Services on Honeycomb.
            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    private static void setSharedPreferences(Context context) {
        if(sharedPreferences == null){
            sharedPreferences = context.getSharedPreferences(CLICKS_SP,Context.MODE_PRIVATE);
        }
    }

    @SuppressLint("CommitPrefEdits")
    public static void registerAdmobClick(Context context) {
        setSharedPreferences(context);
        int numOfClickToday = sharedPreferences.getInt(String.valueOf(currentDayNumber),0);

        sharedPreferences.edit().putInt(String.valueOf(currentDayNumber),numOfClickToday+1).apply();

        if((numOfClickToday+1) >= Data.Ads.Interstitial.maximumClicksPerDay){
            sharedPreferences.edit().putInt(DISABLED_DAY_KEY,currentDayNumber).apply();
            Helper.wtf("DISABLING ADMOB");
            Analytics.i().sendOther("Admob","Disabled");
        }
    }

    public static boolean isDisabled(Context context) {
        setSharedPreferences(context);

        if(currentDayNumber == null){
            Calendar calendar = Calendar.getInstance();
            currentDayNumber = calendar.get(Calendar.DAY_OF_YEAR);
        }
        int disabledDay = sharedPreferences.getInt(DISABLED_DAY_KEY, -1);
        boolean isDisabled = disabledDay == currentDayNumber;
        if(isDisabled){
            Helper.wtf("ADMOB DISABLED",true);
        }
        return isDisabled;
    }

    @Override
    public void showInterstitial() {
        if (mGoogleInterstitialAd.isLoaded()) {
            Helper.wtf("Showing Admob",true);
            Interstitial.FAST_BACK_PRESS = true;
            mGoogleInterstitialAd.show();
        } else {
            Helper.wtf("MoPub", "Tried to show a Google Play Services interstitial ad before it finished loading. Please try again.");
        }
    }

    @Override
    protected boolean usesProxy() {
        return true;
    }

    @Override
    protected void onInvalidate() {
        if (mGoogleInterstitialAd != null) {
            mGoogleInterstitialAd.setAdListener(null);
        }
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras.containsKey(AD_UNIT_ID_KEY);
    }

    private class InterstitialAdListener extends AdListener {
        /*
    	 * Google Play Services AdListener implementation
    	 */
        @Override
        public void onAdClosed() {
            Helper.wtf("MoPub", "Google Play Services interstitial ad dismissed.");
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialDismissed();
            }
        }

        @Override
        public void onAdFailedToLoad(int errorCode) {
            Helper.wtf("MoPub", "Google Play Services interstitial ad failed to load.");
            Helper.wtf("Admob Failed");
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
        }

        @Override
        public void onAdLeftApplication() {
            Helper.wtf("MoPub", "Google Play Services interstitial ad clicked.");
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialClicked();
            }
            registerAdmobClick(mContext);
        }

        @Override
        public void onAdLoaded() {
            Helper.wtf("MoPub", "Google Play Services interstitial ad loaded successfully.");
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialLoaded();
            }
        }

        @Override
        public void onAdOpened() {
            Helper.wtf("MoPub", "Showing Google Play Services interstitial ad.");
            if (mInterstitialListener != null) {
                mInterstitialListener.onInterstitialShown();
            }
        }
    }



    @Deprecated // for testing
    InterstitialAd getGoogleInterstitialAd() {
        return mGoogleInterstitialAd;
    }
}
