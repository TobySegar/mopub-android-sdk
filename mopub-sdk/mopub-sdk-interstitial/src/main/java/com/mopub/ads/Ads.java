package com.mopub.ads;


import android.content.SharedPreferences;
import android.util.Log;

import com.mojang.base.Helper;
import com.mojang.base.InternetObserver;
import com.mojang.base.events.AppEvent;
import com.mojang.base.events.GuideGameEvent;
import com.mojang.base.events.MinecraftGameEvent;
import com.mojang.base.events.OfflineEvent;
import com.mojang.base.json.Data;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Calendar;

/**
 * Controlls how ads are showed
 */
public class Ads {
    private final String TAG = this.getClass().getName();

    private Interstitial interstitial;
    private int numOfPlayers;
    private int timesBlockChanged;
    private SharedPreferences sharedPreferences;
    private Calendar calendar;
    private static final String FIRST_RUN_DAY_KEY = "FirstRunDay";
    static final String FIRST_RUN_KEY = "FirstRun";
    private static final int NUM_FREE_DAYS = 2;
    final int measureUnit = Calendar.DAY_OF_YEAR;
    private boolean fingerAdShowed;
    private static Ads instance;


    public Ads(Interstitial interstitial, SharedPreferences sharedPreferences, Calendar calendar) {
        this.interstitial = interstitial;
        this.numOfPlayers = 1;
        this.sharedPreferences = sharedPreferences;
        this.calendar = calendar;
        if(Ads.instance == null) {
            Ads.instance = this;
        }

        this.interstitial.setFreePeriod(isInFreePeriod( Data.Ads.Interstitial.freePeriodAllowed));

        EventBus.getDefault().register(this);
    }

    public static Ads getInstance() {
        return instance;
    }

    public Interstitial getInterstitial(){
        return interstitial;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAppEvent(AppEvent appEvent) {
        switch (appEvent.lifeCycle) {
            case Destroy:
                interstitial.destroy();
                break;
            case Stop:
                interstitial.lock.stopLock();
                break;
            case Resume:
                interstitial.lock.unlockStop();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGameEvent(MinecraftGameEvent gameEvent) {
        switch (gameEvent.event) {
            case PlayerConnected:
                numOfPlayers++;
                interstitial.lock.lockMultiplayer();
                break;
            case PlayerDisconnected:
                if (numOfPlayers > 1) numOfPlayers--;
                if (numOfPlayers == 1) interstitial.lock.unlockMultiplayer();
                break;
            case PlayerJoinedMultiplayer:
                interstitial.lock.lockMultiplayer();
                break;
            case GamePlayStart:
                interstitial.lock.gameUnlock();
                interstitial.showFastDelayed(2000);
                interstitial.schedulePeriodicShows();
                break;
            case LeaveLevel:
                interstitial.dontBackPress = true;
                interstitial.showDelayed(2000, new Runnable() {
                    @Override
                    public void run() {
                        interstitial.lock.gameLock();
                        interstitial.lock.unlockMultiplayer();
                    }
                });
                break;
            case StartSleepInBed:
                interstitial.showUnityAdsVideo();
                break;
            case PauseScreenPushed:
                Helper.wtf("Setting pausescreen SHowed to true");
                interstitial.pauseScreenShowed = true;
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
        if (viewEvent.getPlayOffline_Accepted() && !InternetObserver.isInternetAvaible()) {
            interstitial.lock.internetLock();
        } else if (viewEvent.getPlayOnline_Accepted() && InternetObserver.isInternetAvaible()) {
            if (numOfPlayers != 1) {
                throw new RuntimeException("numOfPlayer > 1 this should never happen");
            }
            interstitial.lock.internetUnlock();
            interstitial.init(true);
        }
    }

    public void init() {
        if (InternetObserver.isInternetAvaible()) {
            Helper.wtf("start",true);
            interstitial.init(false);
        } else {
            Helper.wtf("start: No Internet Avaible for ads",true);
        }
    }

    public boolean isInFreePeriod(boolean freePeriodAllowed) {
        if (Helper.DEBUG) {
            Helper.wtf(TAG, "isInFreePeriod: false cause debug");
            return false;
        }
        //mark first run
        final boolean runnedBefore = sharedPreferences.getBoolean(FIRST_RUN_KEY, false);
        if (!runnedBefore) {
            int today = calendar.get(measureUnit);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(FIRST_RUN_DAY_KEY, today);
            editor.putBoolean(FIRST_RUN_KEY, true);
            editor.commit();
        }

        if (freePeriodAllowed) {
            int firstRunDay = sharedPreferences.getInt(FIRST_RUN_DAY_KEY, -1);
            if (firstRunDay != -1) {
                int today = calendar.get(measureUnit);
                int endFreeDay = firstRunDay + NUM_FREE_DAYS;
                return today >= firstRunDay && today <= endFreeDay;
            }
        }
        return false;
    }

}
