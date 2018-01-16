package com.mopub.ads;


import android.app.Activity;
import android.os.Handler;
import android.widget.Toast;

import com.mojang.base.AdsListener;
import com.mojang.base.Analytics;
import com.mojang.base.Helper;
import com.mojang.base.events.AppEvent;
import com.mojang.base.events.GameEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.mojang.base.events.AppEvent.Create;
import static com.mojang.base.events.AppEvent.Destroy;
import static com.mojang.base.events.AppEvent.OfflineAccepted;
import static com.mojang.base.events.AppEvent.OnlineAccepted;
import static com.mojang.base.events.AppEvent.Pause;
import static com.mojang.base.events.AppEvent.Restart;
import static com.mojang.base.events.AppEvent.Resume;
import static com.mojang.base.events.AppEvent.Start;
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
public class Ads implements AdsListener {
    private AdCrashDetector mAdCrashDetector;
    private Interstitial mInterstitial;
    private RewardedVideo mRewardedVideo;
    private int numOfPlayers;
    private int timesBlockChanged;
    private static Ads mInstance;


    public Ads(Activity activity) {
        mAdCrashDetector = new AdCrashDetector(activity.getApplicationContext());
        mInterstitial = new Interstitial(activity, this);
        mRewardedVideo = new RewardedVideo(activity, this);
        Ads.mInstance = this;

        EventBus.getDefault().register(this);
    }

    public static Ads i() {
        return mInstance;
    }


    @Subscribe(priority = 1, threadMode = ThreadMode.MAIN)
    public void onAppEvent(AppEvent appEvent) {
        switch (appEvent.event) {
            case Destroy:
                mRewardedVideo.onDestroy();
                mInterstitial.destroy();
                break;
            case Pause:
                mRewardedVideo.onPause();
                break;
            case Stop:
                mRewardedVideo.onStop();
                mInterstitial.onStop();
                break;
            case Create:
                mRewardedVideo.onCreate();
                break;
            case Resume:
                mRewardedVideo.onResume();
                mInterstitial.onResume();
                break;
            case OfflineAccepted:
                mInterstitial.onOfflineAccepted();
                break;
            case OnlineAccepted:
                mInterstitial.onOnlineAccepted();
                break;
            case Restart:
                mRewardedVideo.onRestart();
                break;
            case Start:
                mRewardedVideo.onStart();
                break;

        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGameEvent(GameEvent gameEvent) {
        switch (gameEvent.event) {
            case PlayerConnected:
                onPlayerConnected();
                break;
            case PlayerDisconnected:
                onPlayerDisconnected();
                break;
            case PlayerJoinedMultiplayer:
                interstitial.lock.lockMultiplayer();
                break;
            case GamePlayStart:
                interstitial.lock.gameUnlock();
                interstitial.show(5000, false);
                break;
            case LeaveLevel:
                numOfPlayers = 0;
                interstitial.lock.gameLock();
                interstitial.lock.unlockOnlineMultiplayer();
                interstitial.lock.unlockLocalMultiplayer();
                break;
            case StartSleepInBed:
                if (!interstitial.lock.isHardLocked()) {
                    rewardedVideo.show();
                }
                break;
            case PauseScreenPushed:
                interstitial.setPauseScreenShowed(true);
                break;
            case BlockChanged:
                timesBlockChanged++;
                if (timesBlockChanged == 3) {
                    interstitial.show(false);
                    timesBlockChanged = 0;
                }
                break;
        }
    }

    private void onPlayerDisconnected() {
        if (numOfPlayers > 0) {
            numOfPlayers--;
            Helper.wtf("Number of players in game = " + numOfPlayers);
        }
        if (numOfPlayers == 1) interstitial.lock.unlockLocalMultiplayer();
    }

    private void onPlayerConnected() {
        numOfPlayers++;

        if (numOfPlayers > 1) {
            interstitial.lock.lockLocalMultiplayer();
        }

        Helper.wtf("Number of players in game = " + numOfPlayers);
    }

    public void onAchievementsButtonClicked() {
        //Cache the ads
        rewardedVideo.initializeAndLoad();
        interstitial.loadAfterDelay(0);
    }

    public void onAheadAdsClicked() {
        boolean _isRewardedVideo = false;

        //Try to show rewarded video first
        if (rewardedVideo.isInitialized()) {
            if (rewardedVideo.hasRewardedVideoReady()) {
                _isRewardedVideo = true;
                rewardedVideo.show();
            } else {
                interstitial.show(false);
            }
        } else {
            Helper.wtf("Wanted To use rewarded videos but they weren't initialized!!!!!");
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
                            Analytics.i().sendException(e);
                            System.exit(0);
                        }
                    }
                }, 2000);
            }
        } else {
            System.exit(0);
        }
    }

    @Override
    public void onRewardedVideoCompleted() {

    }

    @Override
    public void onRewardedVideoClosed() {

    }

    @Override
    public void onRewardedVideoStarted() {

    }

    @Override
    public void onRewardedVideoLoadSuccess() {

    }

    @Override
    public void onLoadRewardedVideo() {

    }

    @Override
    public void onInterstitialDismissed() {

    }

    @Override
    public void onInterstitialLoaded() {

    }

    @Override
    public void onInterstitialFailed() {

    }

    @Override
    public void onInterstitialShown() {

    }
}
