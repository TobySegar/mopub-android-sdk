package com.mopub.ads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

//import com.google.android.gms.ads.InterstitialAd;

import com.mojang.base.Analytics;
import com.mojang.base.Helper;
import com.mojang.base.Logger;
import com.mojang.base.events.AppEvent;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubInterstitial;
import org.greenrobot.eventbus.EventBus;
import static com.mojang.base.events.AppEvent.Stop;



public class Proxy extends Activity {
    private static CustomEventInterstitial customEventInterstitial;
    private final String proxy = "Proxy";
    public static boolean isProxyBeingUsed;
    public static Proxy instance = null;
    public static Activity activityz2 = null;
    public static boolean lock;
    //private static InterstitialAd mGoogleInterstitialAd;
    private MoPubInterstitial mopIntrer;
    public void startProxyActivity(Context context, MoPubInterstitial mopubInterstitial) {
        Logger.Log(proxy, "::startProxyActivity - mopub");
        mopIntrer =mopubInterstitial;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.Log(proxy, "::create");
        activityz2 = this;
        instance = this;
        if (!lock){
            Runnable proxyAdsRunnable = new Runnable() {
                @Override
                public void run() {
                    mopIntrer.show();
                }
            };
            Helper.runOnWorkerThread(proxyAdsRunnable);

        }

        //if (Proxy.customEventInterstitial != null) {
        //    Proxy.customEventInterstitial.showInterstitial();
        //}
//        else if (mGoogleInterstitialAd != null) {
//            mGoogleInterstitialAd.show();
//        }
        //Finish();
    }



    public void Finish() {
        try {
            Logger.Log(proxy, "::Finish -- posting fake stop");
            EventBus.getDefault().post(new AppEvent(Stop));
            if (activityz2!=null)
                activityz2.finish();
            if (instance!=null)
                instance.finish();
            finish();
        }
        catch (NullPointerException ignored) {
            Analytics.report("Ads","Proxy_ Finish Failed");
            Analytics.i().sendException(ignored);
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            Logger.Log(proxy, "::destroy");
            if (activityz2!=null)
                instance = null;
            if (instance!=null)
                activityz2 = null;
        }

        catch (NullPointerException ignored) {
            Analytics.report("Ads","Proxy_ onDestroy Failed");
            Analytics.i().sendException(ignored);
        }
        //Proxy.customEventInterstitial = null;
        //Proxy.mGoogleInterstitialAd = null;
    }

}

