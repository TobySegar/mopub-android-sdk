package com.mopub.ads.adapters;


import android.content.Context;

import com.inlocomedia.android.ads.AdError;
import com.inlocomedia.android.ads.AdRequest;
import com.inlocomedia.android.ads.InLocoMedia;
import com.inlocomedia.android.ads.InLocoMediaOptions;
import com.inlocomedia.android.ads.interstitial.InterstitialAd;
import com.inlocomedia.android.ads.interstitial.InterstitialAdListener;
import com.mojang.base.Helper;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;

import java.util.Map;

/**
 * Dostal som ad ale service dala no responding avyhodilo na display ze firewall not responding ked som to mal na pozadi\ ale asi preto ze som mal debugger
 * stop nemohol som to reproducnut
 * videl som jenen error neviem preco cekni ci to je v production AndroidProtocolHandler: Unable to open asset URL: file:///android_asset/inlocomedia/sdkResourcesValidator.js
 * disable testing ads on the web
 */
public class InLocoMediaInterstitial extends CustomEventInterstitial {
    private static final String INTERSTITIAL_ID_KEY = "interstitial_id";
    private static final String DEVELOPMENT_DEVICE_ID = "A1C75E75F99473B4278ABE39C130BF93"; //You can get it from logcat logs
    private static final String APP_ID_KEY = "app_id";

    private InterstitialAd mLocoMediaInterstitial;
    private CustomEventInterstitialListener mMopubListener;
    private InterstitialAdListener mLocoMediaInterstitialListener;

    /**
     * CustomEventInterstitial implementation
     */

    @Override
    protected void loadInterstitial(final Context context,
                                    final CustomEventInterstitialListener customEventInterstitialListener,
                                    final Map<String, Object> localExtras,
                                    Map<String, String> serverExtras) {
        serverExtras.clear();
        serverExtras.put(INTERSTITIAL_ID_KEY,"558422fd7076a9f78217d77156fc624f4ab65be74f55040b30b834c3880a81af");
        serverExtras.put(APP_ID_KEY,"21d8da4c24a98a1106e6113017fcd95ac9782028cdb9a47cb5b4012566ed3599");


        mMopubListener = customEventInterstitialListener;

        InLocoMediaInitialize(context, serverExtras);

        mLocoMediaInterstitial = new InterstitialAd(context);
        mLocoMediaInterstitial.setInterstitialAdListener(getListener());
        mLocoMediaInterstitial.loadAd(createLocoMediaAdRequest(serverExtras));
    }

    private void InLocoMediaInitialize(Context context, Map<String, String> serverExtras) {
        // In Loco Media SDK Init
        InLocoMediaOptions options = InLocoMediaOptions.getInstance(context);

        // The AppId you acquired in earlier steps
        String appId = getFromServerExtras(serverExtras, APP_ID_KEY);
        if (appId != null) {
            options.setAdsKey(appId);
        }

        // Verbose mode flag, if this is set as true InLocoMedia SDK will let you know about errors on the Logcat
        options.setLogEnabled(false);

        options.setLocationTrackingEnabled(true);

        // Development Devices set here are only going to receive test ads
        //options.setDevelopmentDevices(DEVELOPMENT_DEVICE_ID);

        InLocoMedia.init(context, options);
    }

    private String getFromServerExtras(Map<String, String> serverExtras, String KEY) {
        if (serverExtras != null) {
            if (serverExtras.containsKey(KEY)) {
                return serverExtras.get(KEY);
            }
        }
        return "";
    }

    private AdRequest createLocoMediaAdRequest(Map<String, String> serverExtras) {
        String interstitialId = getFromServerExtras(serverExtras, INTERSTITIAL_ID_KEY);

        AdRequest adRequest = new AdRequest();
        adRequest.setAdUnitId(interstitialId);

        return null;
    }

    @Override
    public void showInterstitial() {
        if (mLocoMediaInterstitial != null && mLocoMediaInterstitial.isLoaded()) {
            mLocoMediaInterstitial.show();
        } else {
            Helper.wtf("Failed to get Loco is null = " + (mLocoMediaInterstitial == null) + " isLoaded = nemozem bo moze byt null tak si domysli");
            mMopubListener.onInterstitialFailed(MoPubErrorCode.NO_FILL);
        }
    }

    @Override
    protected boolean usesProxy() {
        return false;
    }

    @Override
    protected void onInvalidate() {
        if(mLocoMediaInterstitial != null){
            mLocoMediaInterstitial = null;
        }
    }

    @Override
    protected MoPubInterstitial.AdType getAdType() {
        return MoPubInterstitial.AdType.INLOCOMEDIA_INTERSTITIAL;
    }


    /**
     * This class cannot implement the listener because its a class and not interface
     *
     * @return LocoMedia Interstitial Listener
     */
    public InterstitialAdListener getListener() {
        if (mLocoMediaInterstitialListener == null) {
            mLocoMediaInterstitialListener = new InterstitialAdListener() {
                @Override
                public void onAdReady(InterstitialAd ad) {
                    Helper.wtf("LocoMedia onAdReady");

                    mMopubListener.onInterstitialLoaded();

                    super.onAdReady(ad);
                }

                @Override
                public void onAdError(InterstitialAd ad, AdError error) {
                    Helper.wtf("LocoMedia onAdError " + error.toString());

                    mMopubListener.onInterstitialFailed(MoPubErrorCode.NO_FILL);

                    super.onAdError(ad, error);
                }

                @Override
                public void onAdOpened(InterstitialAd ad) {
                    Helper.wtf("LocoMedia onAdOpened");

                    mMopubListener.onInterstitialShown();

                    super.onAdOpened(ad);
                }

                @Override
                public void onAdClosed(InterstitialAd ad) {
                    Helper.wtf("LocoMedia onAdClosed");

                    mMopubListener.onInterstitialDismissed();

                    super.onAdClosed(ad);
                }

                @Override
                public void onAdLeftApplication(InterstitialAd ad) {
                    Helper.wtf("LocoMedia onAdLeftApplication");

                    mMopubListener.onLeaveApplication();

                    super.onAdLeftApplication(ad);
                }
            };
        }

        return mLocoMediaInterstitialListener;
    }
}
