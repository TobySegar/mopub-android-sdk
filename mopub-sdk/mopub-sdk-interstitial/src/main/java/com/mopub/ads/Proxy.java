package com.mopub.ads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

//import com.google.android.gms.ads.InterstitialAd;

import com.heyzap.sdk.ads.InterstitialAd;
import com.mojang.base.Helper;
import com.mojang.base.Logger;
import com.mojang.base.events.AppEvent;
import com.mopub.mobileads.CustomEventInterstitial;
import org.greenrobot.eventbus.EventBus;
import static com.mojang.base.events.AppEvent.Stop;



public class Proxy extends Activity {
    private static CustomEventInterstitial customEventInterstitial;
    private final String proxy = "Proxy";
    public static boolean isProxyBeingUsed;
    public static Proxy instance = null;
    public static Activity activityz2 = null;
    //private static InterstitialAd mGoogleInterstitialAd;

    public void startProxyActivity(Context context) {
        Logger.Log(proxy, "::startProxyActivity - mopub");
        Proxy.customEventInterstitial = customEventInterstitial;
        isProxyBeingUsed = true;
        Intent proxyIntent = new Intent(context, Proxy.class);
        proxyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(proxyIntent);
    }

//    public void startProxyActivity(Context context, InterstitialAd mGoogleInterstitialAd) {
//        Logger.Log(proxy, "startProxyActivity - mGoogleInterstitialAd");
//        Proxy.mGoogleInterstitialAd = mGoogleInterstitialAd;
//        Intent proxyIntent = new Intent(context, Proxy.class);
//        context.startActivity(proxyIntent);
//    }

    public void Finish() {
        Logger.Log(proxy, "::Finish -- posting fake stop");
        EventBus.getDefault().post(new AppEvent(Stop));
        activityz2.finish();
        instance.finish();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.Log(proxy, "::create");
        activityz2 = this;
        instance = this;

        InterstitialAd.display(activityz2);
        //if (Proxy.customEventInterstitial != null) {
        //    Proxy.customEventInterstitial.showInterstitial();
        //}
//        else if (mGoogleInterstitialAd != null) {
//            mGoogleInterstitialAd.show();
//        }
        //Finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logger.Log(proxy, "::destroy");
        instance = null;
        activityz2 = null;
        //Proxy.customEventInterstitial = null;
        //Proxy.mGoogleInterstitialAd = null;
    }

}

