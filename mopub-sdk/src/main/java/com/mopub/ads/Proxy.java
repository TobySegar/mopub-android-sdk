package com.mopub.ads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.ads.InterstitialAd;
import com.google.firebase.crash.FirebaseCrash;
import com.mojang.base.events.AppEvent;
import com.mopub.mobileads.CustomEventInterstitial;

import org.greenrobot.eventbus.EventBus;


public class Proxy extends Activity {
    private static CustomEventInterstitial customEventInterstitial;
    private final String proxy = "Proxy";
    private static InterstitialAd mGoogleInterstitialAd;

    public void startProxyActivity(Context context, CustomEventInterstitial customEventInterstitial) {
        FirebaseCrash.log("Proxy start ");
        Proxy.customEventInterstitial = customEventInterstitial;
        Intent proxyIntent = new Intent(context, Proxy.class);
        context.startActivity(proxyIntent);
    }

    public void startProxyActivity(Context context, InterstitialAd mGoogleInterstitialAd) {
        FirebaseCrash.log("Proxy start ");
        Proxy.mGoogleInterstitialAd = mGoogleInterstitialAd;
        Intent proxyIntent = new Intent(context, Proxy.class);
        context.startActivity(proxyIntent);
    }

    public void Finish() {
        Log.d(proxy, "Finish");
        FirebaseCrash.log("Proxy finish ");
        EventBus.getDefault().post(new AppEvent(this, AppEvent.on.Stop));
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(proxy, "create");
        FirebaseCrash.log("Proxy create ");

        if (Proxy.customEventInterstitial != null) {
            Proxy.customEventInterstitial.showInterstitial();
        } else if (mGoogleInterstitialAd != null) {
            mGoogleInterstitialAd.show();
        }
        Finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(proxy, "destroy");
        Proxy.customEventInterstitial = null;
        Proxy.mGoogleInterstitialAd = null;
    }

}

