package com.mopub.ads;


import android.app.Activity;
import android.util.Log;

import com.mojang.base.InternetObserver;
import com.mojang.base.events.AppEvent;
import com.mojang.base.events.GuideGameEvent;
import com.mojang.base.events.MinecraftGameEvent;
import com.mojang.base.events.OfflineEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Inicialization start stop, network management
 */
public class Ads {

    private final String TAG = this.getClass().getName();
    private final InternetObserver internetObserver;
    private Interstitial interstitial;
    private final FreeAdPeriod freeAdPeriod;
    private final Activity activity;
    private int numOfPlayers;
    private boolean firstGamePlayStart;
    private int timesBlockChanged;


    public Ads(Activity activity, Interstitial interstitial, InternetObserver internetObserver, FreeAdPeriod freeAdPeriod) {
        this.internetObserver = internetObserver;
        this.activity = activity;
        this.interstitial = interstitial;
        this.freeAdPeriod = freeAdPeriod;
        this.numOfPlayers = 1;
        this.firstGamePlayStart = true;

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
    public void onAppEvent(AppEvent appEvent) {
        switch (appEvent.lifeCycle) {
            case Destroy:
                interstitial.destroy();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGameEvent(MinecraftGameEvent gameEvent) {
        switch (gameEvent.event) {
            case PlayerConnected:
                numOfPlayers++;
                interstitial.lock();
                break;
            case PlayerDisconnected:
                if (numOfPlayers > 1) {
                    numOfPlayers--;
                }
                if (numOfPlayers == 1) {
                    interstitial.unlock();
                }
                break;
            case GamePlayStart:
                if (!interstitial.show() && firstGamePlayStart) {
                    interstitial.showOnLoad(System.currentTimeMillis());
                    firstGamePlayStart = false;
                }
                break;
            case LeaveLevel:
                interstitial.show();
                break;
            case StopSleepInBed:
                interstitial.show();
                break;
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void guideEvent(GuideGameEvent gameEvent) {
        switch (gameEvent.event) {
            case BlockChanged:
                timesBlockChanged++;
                if (timesBlockChanged == 3) {
                    interstitial.show();
                    timesBlockChanged = 0;
                }
                break;
        }
    }


    @Subscribe
    public void onViewEvent(OfflineEvent viewEvent) {
        if (viewEvent.playOffline_Accepted) {
            interstitial.lock();
        } else if (viewEvent.playOnline_Accepted) {
            if (numOfPlayers != 1) {
                throw new RuntimeException("numOfPlayer > 1 this should never happen");
            }
            interstitial.unlock();
            interstitial.init();
        }
    }

    public void init() {
        if (freeAdPeriod.isFree()) {
            Log.e(TAG, "start: FreePeriond");
            return;
        }

        if (internetObserver.isInternetAvaible()) {
            Log.e(TAG, "start");
            interstitial.init();
        } else {
            Log.i(TAG, "start: No Internet Avaible for ads");
        }
    }
}
