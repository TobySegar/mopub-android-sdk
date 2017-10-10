package com.mopub.ads;


import android.app.Activity;
import android.os.Handler;
import android.widget.Toast;

import com.mojang.base.Helper;
import com.mojang.base.InternetObserver;
import com.mojang.base.events.AppEvent;
import com.mojang.base.events.GameEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.mojang.base.events.AppEvent.Destroy;
import static com.mojang.base.events.AppEvent.OfflineAccepted;
import static com.mojang.base.events.AppEvent.OnlineAccepted;
import static com.mojang.base.events.AppEvent.Pause;
import static com.mojang.base.events.AppEvent.Resume;
import static com.mojang.base.events.AppEvent.Stop;
import static com.mojang.base.events.GameEvent.BlockChanged;
import static com.mojang.base.events.GameEvent.GamePlayStart;
import static com.mojang.base.events.GameEvent.LeaveLevel;
import static com.mojang.base.events.GameEvent.PauseScreenPushed;
import static com.mojang.base.events.GameEvent.PlayerConnected;
import static com.mojang.base.events.GameEvent.PlayerDisconnected;
import static com.mojang.base.events.GameEvent.PlayerJoinedMultiplayer;
import static com.mojang.base.events.GameEvent.StartSleepInBed;

/**
 * Controlls how ads are showed
 */
public class Ads {
    private Interstitial interstitial;
    private int numOfPlayers;
    private int timesBlockChanged;
    private static Ads instance;


    public Ads(Interstitial interstitial) {
        this.interstitial = interstitial;
        this.numOfPlayers = 0;
        if (Ads.instance == null) {
            Ads.instance = this;
        }

        EventBus.getDefault().register(this);
    }

    public static Ads getInstance() {
        return instance;
    }

    public Interstitial getInterstitial() {
        return interstitial;
    }

    @Subscribe(priority = 1, threadMode = ThreadMode.MAIN)
    public void onAppEvent(AppEvent appEvent) {
        switch (appEvent.event) {
            case Destroy:
                interstitial.destroy();
                break;
            case Pause:
                break;
            case Stop:
                interstitial.lock.stopLock();
                break;
            case Resume:
                interstitial.lock.unlockStop();
                break;
            case OfflineAccepted:
                if (!InternetObserver.isInternetAvaible()) {
                    interstitial.lock.internetLock();
                }
                break;
            case OnlineAccepted:
                if (InternetObserver.isInternetAvaible()) {
                    interstitial.lock.internetUnlock();
                    interstitial.init(true);
                }
                break;

        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGameEvent(GameEvent gameEvent) {
        switch (gameEvent.event) {
            case PlayerConnected:
                numOfPlayers++;
                Helper.wtf("Number of players in game = " + numOfPlayers);
                if (numOfPlayers > 1) interstitial.lock.lockMultiplayer();
                break;
            case PlayerDisconnected:
                if (numOfPlayers > 0) {
                    numOfPlayers--;
                    Helper.wtf("Number of players in game = " + numOfPlayers);
                }
                if (numOfPlayers == 1) interstitial.lock.unlockMultiplayer();
                break;
            case PlayerJoinedMultiplayer:
                interstitial.lock.lockMultiplayer();
                interstitial.lock.forceOneShowLock();
                break;
            case GamePlayStart:
                interstitial.lock.gameUnlock();
                interstitial.show(5000);
                break;
            case LeaveLevel:
                if (numOfPlayers > 0) numOfPlayers--;
                interstitial.lock.gameLock();
                interstitial.lock.unlockMultiplayer();
                break;
            case StartSleepInBed:
                interstitial.showUnityAdsVideo();
                break;
            case PauseScreenPushed:
                interstitial.pauseScreenShowed = true;
                break;
            case BlockChanged:
                timesBlockChanged++;
                if (timesBlockChanged == 3) {
                    interstitial.show();
                    timesBlockChanged = 0;
                }
                break;
        }
    }


    public void init() {
        if (InternetObserver.isInternetAvaible()) {
            Helper.wtf("start", true);
            interstitial.init(false);
        } else {
            Helper.wtf("start: No Internet Avaible for ads", true);
        }
    }

    public static void kick(String text, final Activity activity) {
        if (activity != null) {
            EventBus.getDefault().post(new AppEvent(Stop));
            EventBus.getDefault().post(new AppEvent(Destroy));
            if (text != null) {
                Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            activity.finishAffinity();
                        } catch (Exception e) {
                            System.exit(0);
                        }
                    }
                }, 2000);
            }
        } else {
            System.exit(0);
        }
    }
}
