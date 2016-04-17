package com.mopub.ads;


import android.app.Activity;
import android.util.Log;

import com.mopub.mobileads.MoPubInterstitial;
import com.mojang.base.Screen;
import com.mojang.base.WorkerThread;

/**
 * Limits showing interstitial based on time gap
 * Makes sures interstitial are not showed too often
 */
public class TimeLimitedInterstitial extends Interstitial {
    private final String TAG = this.getClass().getName();
    private final WorkerThread workerThread;
    private final long timeGap;
    private boolean canShow;


    public TimeLimitedInterstitial(Activity activity,String interstitialId,Screen screen,WorkerThread workerThread,long adTimeGapMills) {
        super(activity,interstitialId,screen);
        this.workerThread = workerThread;
        this.timeGap = adTimeGapMills;
        this.canShow = true;
    }


    @Override
    public void onInterstitialShown(MoPubInterstitial interstitial) {
        workerThread.scheduleGameTime(new Runnable() {
            @Override
            public void run() {
                canShow = true;
            }
        }, timeGap);
    }

    public boolean canShowAd(){
        return canShow;
    }
    
    public boolean show() {
        if (!canShow){
            Log.e(TAG, "showInterstitial: Minimal ad gap nepresiel!");
            return false;
        }

        if (!super.show()){
            Log.e(TAG, "showInterstitial: Failed");
            return false;
        }

        canShow = false;
        return true;
    }
}
