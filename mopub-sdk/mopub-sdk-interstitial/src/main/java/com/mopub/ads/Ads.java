package com.mopub.ads;


import android.app.Activity;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.mojang.base.*;
import com.mojang.base.events.AppEvent;
import com.mojang.base.events.GameEvent;

import com.mopub.common.MoPub;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.ConsentDialogListener;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.ConsentStatusChangeListener;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.mobileads.MoPubErrorCode;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

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

        if (InternetObserver.isInternetAvaible()) {
            Logger.Log("::start");
            interstitial.init(false);
        } else {
            Logger.Log("::start: No Internet Avaible for ads");
        }

        EventBus.getDefault().register(this);

    }

    public static void showMopubConsentDialog(final Runnable doAfterDialog) {
        if (MoPub.isSdkInitialized()) {
            // CONSENT DIALOG FOR MOPUB
            final PersonalInfoManager mPersonalInfoManager = MoPub.getPersonalInformationManager();
            if (mPersonalInfoManager != null && mPersonalInfoManager.shouldShowConsentDialog()) {
                mPersonalInfoManager.loadConsentDialog(new ConsentDialogListener() {
                    @Override
                    public void onConsentDialogLoaded() {
                        Logger.Log("::Ads", "::SHOWING CONSENT DIALOG");
                        if(!mPersonalInfoManager.showConsentDialog()){
                            doAfterDialog.run();
                        }
                    }

                    @Override
                    public void onConsentDialogLoadFailed(@NonNull MoPubErrorCode moPubErrorCode) {
                        Logger.Log("::Ads", "::Consent dialog failed to load.");
                        doAfterDialog.run();
                    }
                });
            }else{
                doAfterDialog.run();
            }
        }else{
            doAfterDialog.run();
        }
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
                FileManager.i().unregisterTexturePackDownloadReceiver(interstitial.minecraftActivity);
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
                Logger.Log("Number of players in game = " + numOfPlayers);
                if (numOfPlayers > 1) interstitial.lock.lockLocalMultiplayer();
                break;
            case PlayerDisconnected:
                if (numOfPlayers > 0) {
                    numOfPlayers--;
                    Logger.Log("Number of players in game = " + numOfPlayers);
                }
                if (numOfPlayers == 1) interstitial.lock.unlockLocalMultiplayer();
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
                showAfterLeftMultiplayerServer();
                interstitial.lock.gameLock();
                interstitial.lock.unlockOnlineMultiplayer();
                interstitial.lock.unlockLocalMultiplayer();
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
                    interstitial.show(false);
                    timesBlockChanged = 0;
                }
                break;
        }
    }

    private void showAfterLeftMultiplayerServer() {
        boolean isOnlyMultiplayerLocked = false;
        if (interstitial.lock.isOnlineMultiplayerLocked()) {
            //we check if only lock locked is from multiplayer.
            interstitial.lock.unlockOnlineMultiplayer();
            isOnlyMultiplayerLocked = !interstitial.lock.isAnyLocked();
            interstitial.lock.lockMultiplayer();
        }

        if (isOnlyMultiplayerLocked) {
            if (interstitial.lock.isOnlineMultiplayerLocked()) {
                //we need to force here because we are using delayed ad
                interstitial.show(5000, false);
            }
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
}
