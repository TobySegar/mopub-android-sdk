package com.mopub.ads;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Toast;

import com.google.android.gms.ads.MobileAds;
import com.mojang.base.*;
import com.mojang.base.events.AppEvent;
import com.mojang.base.events.GameEvent;

import com.mojang.base.events.InterstitialEvent;
import com.mojang.base.json.Data;
import com.mopub.ads.adapters.GooglePlayServicesInterstitial;
import com.mopub.common.ClientMetadata;
import com.mopub.common.MoPub;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.SdkInitializationListener;
import com.mopub.common.privacy.ConsentDialogListener;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.mobileads.AdViewController;
import com.mopub.mobileads.MoPubErrorCode;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Method;

import static com.mojang.base.events.AppEvent.Destroy;
import static com.mojang.base.events.AppEvent.OfflineAccepted;
import static com.mojang.base.events.AppEvent.OnlineAccepted;
import static com.mojang.base.events.AppEvent.Pause;
import static com.mojang.base.events.AppEvent.Resume;
import static com.mojang.base.events.AppEvent.Stop;
import static com.mojang.base.events.GameEvent.*;
import static com.mojang.base.events.InterstitialEvent.*;
import static com.mopub.ads.Interstitial.DEBUG_MOPUB_INTERSTITIAL_ID;

/**
 * Controlls how ads are showed
 */


public class Ads {
    private Banner banner;
    private RewardedVideo rewardedVideo;
    private Activity activity;
    private Interstitial interstitial;
    private int numOfPlayers;
    private static Ads instance;
    private Method nativeBackPressedMethod;
    private boolean pauseScreenShowed;
    int numberOfClickedOnAd;


    public Ads(Activity activity, Interstitial interstitial, RewardedVideo rewardedVideo, Banner banner) {
        if (Ads.instance == null) {
            instance = this;

            this.interstitial = interstitial;
            this.rewardedVideo = rewardedVideo;
            this.banner = banner;
            this.activity = activity;

            EventBus.getDefault().register(this);
        }

        if (InternetObserver.isInternetAvaible()) {
            Logger.Log("::start");
            interstitial.init();
        } else {
            Logger.Log("::start: No Internet Avaible for ads");
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
                FileManager.i().unregisterTexturePackDownloadReceiver(activity);
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
                    interstitial.init();
                }
                break;

        }
    }

    @Subscribe(priority = 1,threadMode = ThreadMode.MAIN)
    public void onInterstitialEvent(InterstitialEvent event) {
        switch (event.event) {
            case Loaded:
                if (Data.country == null) {
                    Data.country = getCountryCodeFromMopub();
                    lockOutSE();
                }
                break;
            case Dismissed:
                Proxy.lock = true;
                Analytics.lockedAnalytics = true;
                Logger.Log("::called -- Dismissed event");
                Helper.setNormalVolume(activity);
                hideNavigationBar(activity);
                callNativeBackPressed(800);
                nesmrtelnost(false, 15000);
                break;
            case Shown:
                Analytics.lockedAnalytics = true;
                Helper.setQuietVolume(activity);
                nesmrtelnost(true, 0);
                break;
            case Clicked:
                Proxy.lock = true;
                numberOfClickedOnAd++;
                if (numberOfClickedOnAd >= 2) {
                    Data.Ads.enabled = false;
                    Logger.String("::Ads Disabled it was clicked on ad 2x");
                }
                break;
        }
    }


    @Subscribe(priority = 1,threadMode = ThreadMode.MAIN)
    public void onGameEvent(GameEvent gameEvent) {
        switch (gameEvent.event) {
            case PlayerConnected:
                Logger.String("::PlayerConnected");
                numOfPlayers++;
                Logger.Log("Number of players in game = " + numOfPlayers);
                if (numOfPlayers > 1) interstitial.lock.lockLocalMultiplayer();
                break;
            case PlayerDisconnected:
                Logger.String("::PlayerDisconnected");
                if (numOfPlayers > 0) {
                    numOfPlayers--;
                    Logger.Log("Number of players in game = " + numOfPlayers);
                }
                if (numOfPlayers == 1) interstitial.lock.unlockLocalMultiplayer();
                break;
            case PlayerJoinedMultiplayer:
                Logger.String("::PlayerJoinedMultiplayer");
                interstitial.lock.lockMultiplayer();
                break;
            case GamePlayStart:
                Logger.String("::GamePlayStart");
                interstitial.lock.gameUnlock();
                break;
            case LeaveLevel:
                numOfPlayers = 0;
                raiseLeaveMultiplayerServer();
                interstitial.lock.gameLock();
                interstitial.lock.unlockOnlineMultiplayer();
                interstitial.lock.unlockLocalMultiplayer();
                break;
            case PauseScreenPushed:
                Logger.String("::PauseScreenPushed");
                interstitial.pauseScreenShowed = true;
                break;
        }
    }

    private void raiseLeaveMultiplayerServer() {
        //we check if only lock locked is from multiplayer.
        boolean isOnlyMultiplayerLocked = false;
        if (interstitial.lock.isOnlineMultiplayerLocked()) {
            interstitial.lock.unlockOnlineMultiplayer();
            isOnlyMultiplayerLocked = !interstitial.lock.isAnyLocked();
            interstitial.lock.lockMultiplayer();
        }
        if (isOnlyMultiplayerLocked) {
            EventBus.getDefault().post(new GameEvent(LeaveServer));
        }
    }

    public static void earlyInitialization(Activity activity, final Runnable onInitialized) {
        Logger.Log("::Ads", "::Early Ads Initialization");
        initializeMoPub(activity, onInitialized);
        MobileAds.initialize(activity, GooglePlayServicesInterstitial.getAppId(activity));
    }

    static String getMopubId(Activity activity){
        return  Helper.isDebugPackage(activity) ? DEBUG_MOPUB_INTERSTITIAL_ID : Data.Ads.Interstitial.mopubId;
    }
    private static void initializeMoPub(Activity activity, final Runnable runAfter) {
        if (!MoPub.isSdkInitialized() && Data.Ads.enabled) {
            Logger.Log("::Ads", "::Initializing MoPub");
            MoPub.initializeSdk(
                    activity,
                    new SdkConfiguration.Builder(getMopubId(activity)).build(),
                    new SdkInitializationListener() {
                        @Override
                        public void onInitializationFinished() {
                            Ads.showMoPubConsentDialog(runAfter);
                        }
                    });
        } else {
            Logger.Log("::Ads", "::Failed MoPub Initialization because" +
                    " MoPub.isSdkInitialized() = " + MoPub.isSdkInitialized() + " Data.Ads.enabled " + Data.Ads.enabled );
            runAfter.run();
        }
    }

    private static void showMoPubConsentDialog(final Runnable doAfterDialog) {
        if (MoPub.isSdkInitialized()) {
            // CONSENT DIALOG FOR MOPUB
            final PersonalInfoManager mPersonalInfoManager = MoPub.getPersonalInformationManager();
            if (mPersonalInfoManager != null && mPersonalInfoManager.shouldShowConsentDialog()) {
                mPersonalInfoManager.loadConsentDialog(new ConsentDialogListener() {
                    @Override
                    public void onConsentDialogLoaded() {
                        Logger.Log("::Ads", "::SHOWING CONSENT DIALOG");
                        if (!mPersonalInfoManager.showConsentDialog()) {
                            doAfterDialog.run();
                        }
                    }

                    @Override
                    public void onConsentDialogLoadFailed(@NonNull MoPubErrorCode moPubErrorCode) {
                        Logger.Log("::Ads", "::Consent dialog failed to load.");
                        doAfterDialog.run();
                    }
                });
            } else {
                doAfterDialog.run();
            }
        } else {
            doAfterDialog.run();
        }
    }

    private void callNativeBackPressed(int delay) {
        if (pauseScreenShowed || true) {
            Logger.Log("::called -- TRYING BACK PRESS");
            //todo ENHANCE zisti aky dobry ma mobil a zmen delay here  nejde variable pause screen showed
            Helper.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Logger.Log("::called -- pauseScreenShowed " + pauseScreenShowed);
                    try {
                        if (nativeBackPressedMethod == null) {
                            Logger.Log("::called -- callNativeBackPressed");
                            nativeBackPressedMethod = activity.getClass().getMethod("callNativeBackPressed");
                            nativeBackPressedMethod.invoke(activity);
                            pauseScreenShowed = false;
                            Logger.Log("::called -- NativeBackPressed");
                        } else {
                            nativeBackPressedMethod.invoke(activity);
                            pauseScreenShowed = false;
                            Logger.Log("::called -- NativeBackPressed");
                        }
                    } catch (Exception e) {
                        Logger.Log("::failed -- back press");
                    }
                }
            }, delay);
        }
    }


    private void nesmrtelnost(final boolean zapnut, long delay) {
        Helper.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Data.hasMinecraft) {
                    try {
                        if (zapnut) {
                            interstitial.nesmrtelnostON();
                        } else {
                            interstitial.nesmrtelnostOFF();
                        }
                        Logger.Log("::Nesmrtelnos = " + zapnut);
                    } catch (UnsatisfiedLinkError ignored) {
                        Logger.Log("::!Failed to zapnut nesmrtelnost");
                    }
                }
            }
        },delay);
    }

    private void lockOutSE() {
        if(Data.country != null && Data.country.equals("SE")){
            Logger.Log("::Crating SE file");
            FileManager.i().put(FileManager.SE, null);

            //clear firewall result so he can go through check again
            SharedPreferences LromSP = activity.getApplicationContext().getSharedPreferences("vic", Context.MODE_PRIVATE);
            LromSP.edit().clear().apply();

            //sendAnalitics
            Analytics.i().sendOther("SECreated", Data.country);

            Ads.kick("",activity);
        }
    }


    private void hideNavigationBar(final Activity activity) {
        Helper.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean hasMenuKey = ViewConfiguration.get(activity).hasPermanentMenuKey();
                    boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
                    //Logger.Log("::hasMenuKey(false) = " + hasMenuKey + ":: hasBackKey(false) =" + hasBackKey);
                    if (!hasMenuKey && !hasBackKey && Data.hasMinecraft) {
                        // Do whatever you need to do, this device has a navigation bar
                        View decorView = activity.getWindow().getDecorView();
                        int currentVis = decorView.getSystemUiVisibility();
                        int hidenVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_LOW_PROFILE;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                            hidenVisibility = hidenVisibility | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                        }
                        //Logger.Log("::Curent visibility " + currentVis + ":: hiddenVisibility " + hidenVisibility);
                        Logger.Log("::HIDING NAVBAR");

                        decorView.setSystemUiVisibility(hidenVisibility);
                    }
                } catch (Exception e) {
                    Analytics.i().sendException(e);
                }
            }
        }, 4000);
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

    /**
     * @return country code like SK,US http://www.mcc-mnc.com/
     */
    @Nullable
    private String getCountryCodeFromMopub() {
        try {
            String c1 = AdViewController.getCountryCodeFromMopubResponse();
            if (c1 != null) return c1.toUpperCase();
            if (ClientMetadata.getInstance() != null) {
                String c2 = ClientMetadata.getInstance().getIsoCountryCode();
                if (c2 != null) return c2.toUpperCase();
                return ClientMetadata.getInstance().getSimIsoCountryCode().toUpperCase();
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
