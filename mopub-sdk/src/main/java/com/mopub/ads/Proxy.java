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
    private int clicks;
    private static CustomEventInterstitial customEventInterstitial;
    private static RelativeLayout relativeLayout;
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
        createClickSpace();
        if(Proxy.customEventInterstitial != null){
            Proxy.customEventInterstitial.showInterstitial();
        }else{
            Finish();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //BUG ked pozeraz vydeo a minimalizujes tak sa proxy destroyne a potom nemas nic pod tym
        Log.d(proxy, "destroy");
        Proxy.customEventInterstitial = null;
    }

    public void Finish(){
        Log.d(proxy, "Finish");
        FirebaseCrash.log("Proxy finish ");

        Activity proxy = (Activity) relativeLayout.getContext();
        proxy.finish();
        finish();
        relativeLayout = null;
    }
    private void createClickSpace() {
        // Creating a new RelativeLayout
        relativeLayout = new RelativeLayout(this);
        relativeLayout.setBackgroundColor(Color.DKGRAY);
        relativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Analytics.sendOther("Proxy","Click");
                Log.e(proxy, "Click");
                clicks++;
                if (clicks >= 2) {
                    finish();
                }
            }
        });

        this.setContentView(relativeLayout, new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT));
    }

}

