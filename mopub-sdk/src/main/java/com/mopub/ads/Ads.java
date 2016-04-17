package com.mopub.ads;


import android.app.Activity;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import com.mojang.base.InternetObserver;
import com.mojang.base.events.AppEvent;

/**
 * Inicialization start stop, network management
 */
public class Ads {

    private final String TAG = this.getClass().getName();
    private final InternetObserver internetObserver;
    private Interstitial interstitial;
    private final Activity activity;


    public Ads(Activity activity,Interstitial interstitial,InternetObserver internetObserver) {
        this.internetObserver = internetObserver;
        this.activity = activity;
        this.interstitial = interstitial;

        EventBus.getDefault().register(this);
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void forRewardedVideo(AppEvent appEvent) {
//        switch (appEvent.lifeCycle) {
//            case Destroy:
//                MoPub.onDestroy(activity);
//                break;
//            case Stop:
//                MoPub.onStop(activity);
//                break;
//            case Start:
//                MoPub.onStart(activity);
//                break;
//            case Resume:
//                MoPub.onResume(activity);
//                break;
//            case Pause:
//                MoPub.onPause(activity);
//                break;
//            case Create:
//                MoPub.onCreate(activity);
//                break;
//            case Restart:
//                MoPub.onRestart(activity);
//                break;
//            case BackPressed:
//                MoPub.onBackPressed(activity);
//                break;
//        }
//    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(AppEvent appEvent) {
        switch (appEvent.lifeCycle) {
            case Destroy:
                interstitial.destroy();
                break;
            case Stop:
                stop();
                break;
            case Start:
                start();
                break;
        }
    }

    public void start() {
            if (internetObserver.isInternetAvaible()) {
                Log.e(TAG, "start");
                interstitial.start();
            }else{
                Log.i(TAG, "start: No Internet Avaible for ads");
            }
    }

    public void stop() {
        Log.e(TAG, "stop");
        interstitial.stop();
    }
}
