package com.mopub.ads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.firebase.crash.FirebaseCrash;
import com.mojang.base.Analytics;
import com.mopub.mobileads.CustomEventInterstitial;


public class Proxy extends Activity {
    private static CustomEventInterstitial customEventInterstitial;
    private final String proxy = "Proxy";

    public void startProxyActivity(Context context, CustomEventInterstitial customEventInterstitial) {
        FirebaseCrash.log("Proxy start ");
        Proxy.customEventInterstitial = customEventInterstitial;
        Intent proxyIntent = new Intent(context, Proxy.class);
        context.startActivity(proxyIntent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(proxy, "create");
        FirebaseCrash.log("Proxy create ");

        if(Proxy.customEventInterstitial != null){
            Proxy.customEventInterstitial.showInterstitial();
            Finish();
        }else{
            Finish();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(proxy, "destroy");
        Proxy.customEventInterstitial = null;
    }

    public void Finish(){
        Log.d(proxy, "Finish");
        FirebaseCrash.log("Proxy finish ");

        finish();
    }

}

