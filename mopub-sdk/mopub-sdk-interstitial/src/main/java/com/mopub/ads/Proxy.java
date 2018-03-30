package com.mopub.ads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

//import com.google.android.gms.ads.InterstitialAd;
import com.mojang.base.Helper;
import com.mojang.base.events.AppEvent;
import com.mopub.mobileads.CustomEventInterstitial;

import org.greenrobot.eventbus.EventBus;

import static com.mojang.base.events.AppEvent.Stop;


public class Proxy extends Activity {
    private static CustomEventInterstitial customEventInterstitial;
    private final String proxy = "Proxy";
    //private static InterstitialAd mGoogleInterstitialAd;

    public void startProxyActivity(Context context, CustomEventInterstitial customEventInterstitial) {
        Helper.wtf(proxy, "startProxyActivity - mopub");
        Proxy.customEventInterstitial = customEventInterstitial;
        Intent proxyIntent = new Intent(context, Proxy.class);
        context.startActivity(proxyIntent);
    }

//    public void startProxyActivity(Context context, InterstitialAd mGoogleInterstitialAd) {
//        Helper.wtf(proxy, "startProxyActivity - mGoogleInterstitialAd");
//        Proxy.mGoogleInterstitialAd = mGoogleInterstitialAd;
//        Intent proxyIntent = new Intent(context, Proxy.class);
//        context.startActivity(proxyIntent);
//    }

    public void Finish() {
        Helper.wtf(proxy, "Finish -- posting fake stop");
        EventBus.getDefault().post(new AppEvent(Stop));
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Helper.wtf(proxy, "create");

        if (Proxy.customEventInterstitial != null) {
            Proxy.customEventInterstitial.showInterstitial();
        }
//        else if (mGoogleInterstitialAd != null) {
//            mGoogleInterstitialAd.show();
//        }
        Finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Helper.wtf(proxy, "destroy");
        Proxy.customEventInterstitial = null;
        //Proxy.mGoogleInterstitialAd = null;
    }

}

