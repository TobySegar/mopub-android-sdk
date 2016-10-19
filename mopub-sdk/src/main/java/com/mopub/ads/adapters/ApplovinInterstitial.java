/**
 * AppLovin Interstitial SDK Mediation for MoPub
 *
 * @author Matt Szaro
 * @version 1.2
 **/

package com.mopub.ads.adapters;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.sdk.*;
import com.mojang.base.Helper;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;

import java.util.Map;

public class ApplovinInterstitial extends CustomEventInterstitial implements AppLovinAdLoadListener
{
    private CustomEventInterstitial.CustomEventInterstitialListener mInterstitialListener;
    private Activity                                                parentActivity;
    private AppLovinSdk                                             sdk;
    private AppLovinAd                                              lastReceived;

    private static final String TAG = "AppLovinAdapter";

    /*
     * Abstract methods from CustomEventInterstitial
     */
    @Override
    public void loadInterstitial(Context context, CustomEventInterstitial.CustomEventInterstitialListener interstitialListener, Map<String, Object> localExtras, Map<String, String> serverExtras)
    {
        Helper.wtf("Applovin Load");
        mInterstitialListener = interstitialListener;

        if ( context instanceof Activity )
        {
            parentActivity = (Activity) context;
        }
        else
        {
            mInterstitialListener.onInterstitialFailed( MoPubErrorCode.INTERNAL_ERROR );
            return;
        }

        Helper.wtf("Request received for new interstitial." );

        sdk = AppLovinSdk.getInstance( context );
        sdk.getAdService().loadNextAd( AppLovinAdSize.INTERSTITIAL, this );

    }

    @Override
    public void showInterstitial()
    {
        final AppLovinAd adToRender = lastReceived;

        if ( adToRender != null )
        {
            Helper.wtf("Showing AppLovin interstitial ad..." );

            parentActivity.runOnUiThread( new Runnable() {
                public void run()
                {
                    AppLovinInterstitialAdDialog inter = AppLovinInterstitialAd.create(sdk, parentActivity);

                    inter.setAdClickListener( new AppLovinAdClickListener() {
                        @Override
                        public void adClicked(AppLovinAd appLovinAd)
                        {
                            mInterstitialListener.onLeaveApplication();
                        }
                    } );

                    inter.setAdDisplayListener( new AppLovinAdDisplayListener() {
                        @Override
                        public void adDisplayed(AppLovinAd appLovinAd)
                        {
                            mInterstitialListener.onInterstitialShown();
                        }

                        @Override
                        public void adHidden(AppLovinAd appLovinAd)
                        {
                            mInterstitialListener.onInterstitialDismissed();
                        }
                    } );
                    inter.showAndRender( adToRender );
                }
            } );
        }else{Helper.wtf("Showing AppLovin failed adToRender null" );}
    }

    @Override
    protected boolean usesProxy() {
        return false;
    }

    @Override
    public void onInvalidate()
    {

    }

    @Override
    public void adReceived(AppLovinAd ad)
    {
        Helper.wtf("AppLovin interstitial loaded successfully." );

        lastReceived = ad;

        parentActivity.runOnUiThread( new Runnable() {
            public void run()
            {
                mInterstitialListener.onInterstitialLoaded();
            }
        } );
    }

    @Override
    public void failedToReceiveAd(final int errorCode)
    {
        parentActivity.runOnUiThread( new Runnable() {
            public void run() {
                Helper.wtf("Applovin Fail");
                    mInterstitialListener.onInterstitialFailed( MoPubErrorCode.NETWORK_NO_FILL );
            }
        });
    }
}