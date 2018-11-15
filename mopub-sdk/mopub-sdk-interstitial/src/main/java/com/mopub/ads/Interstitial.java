package com.mopub.ads;


import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import com.heyzap.sdk.ads.HeyzapAds;
import com.heyzap.sdk.ads.InterstitialAd;
import com.mojang.base.*;
import com.mojang.base.events.GameEvent;
import com.mojang.base.events.InterstitialEvent;
import com.mojang.base.json.Data;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.mojang.base.events.GameEvent.*;
import static com.mojang.base.events.InterstitialEvent.Dismissed;
import static com.mojang.base.events.InterstitialEvent.Loaded;

/**
 * Intertitial functionality for showing ads
 */
@SuppressWarnings("FieldCanBeLocal")
public class Interstitial implements MoPubInterstitial.InterstitialAdListener, HeyzapAds.OnStatusListener {
    public static final String DEBUG_MOPUB_INTERSTITIAL_ID = Logger.String("::c2fc437d0fd44e91982838693549cdb4");
    private MoPubInterstitial mopubInterstitial;
    private final Activity activity;
    private Context context;
    private double periodicMills;
    private boolean periodicScheduled;
    public final Lock lock;
    boolean pauseScreenShowed;
    private Runnable loadRunnable;
    private Runnable gapLockRunnable;
    private Runnable periodicShowRunnable;
    private Runnable proxyFinishRunnable;
    private int timesBlockChanged;
    Proxy prxy;
    Intent i;
    public Interstitial(final Activity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
        this.periodicMills = Helper.FasterAds() ? 25000 : Data.Ads.Interstitial.periodicShowMillsLow;
        this.lock = new Lock();
        EventBus.getDefault().register(this);
        prxy = new Proxy();
        i = new Intent(activity, Proxy.class);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGameEvent(GameEvent gameEvent) {
        switch (gameEvent.event) {
            case StartSleepInBed:
                Logger.String("::StartSleepInBed");
                show(false, 0);
                break;
            case BlockChanged:
                Logger.String("::BlockChanged");
                timesBlockChanged++;
                if (timesBlockChanged == 3) {
                    show(false, 0);
                    timesBlockChanged = 0;
                }
                break;
            case GamePlayStart:
                Logger.String("::GamePlayStart Interstitial");
                show(false, 5000);
                break;
            case LeaveServer:
                Logger.String("::LeaveServer");
                show(false, 5000);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onInterstitialEvent(InterstitialEvent intEvent) {
        switch (intEvent.event) {
            case Loaded:
                schedulePeriodicShows();
                break;
        }
    }

    @Override
    public void onInterstitialDismissed(MoPubInterstitial interstitial) {
        Logger.Log("::called -- onInterstitialDismissed");
        EventBus.getDefault().post(new InterstitialEvent(Dismissed));
        Logger.Log("::onInterstitialDismissed");

        gapLockForTime(Data.Ads.Interstitial.minimalGapMills);
        load(1000);
        if (Proxy.isProxyBeingUsed){

            proxyFinishRunnable = new Runnable() {
                @Override
                public void run() {
                    if (Proxy.instance!=null){
                    Proxy.instance.Finish();
                    Proxy.lock=false;
                    Analytics.lockedAnalytics  = false;
                    }
                }
            };
            Helper.runOnWorkerThread(proxyFinishRunnable, 640);
        }
    }

    @Override
    public void onInterstitialLoaded(MoPubInterstitial interstitial) {
        EventBus.getDefault().post(new InterstitialEvent(InterstitialEvent.Loaded));
        Logger.Log("::::Interstitial: onInterstitialLoaded");

        changePeriodicShowForHighEcpmCountry();
        schedulePeriodicShows();
    }

    @Override
    public void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode) {
        EventBus.getDefault().post(new InterstitialEvent(InterstitialEvent.Failed));
        Logger.Log("::onInterstitialFailed: " + errorCode);
        load(10000);
    }

    @Override
    public void onInterstitialShown(MoPubInterstitial interstitial) {
        EventBus.getDefault().post(new InterstitialEvent(InterstitialEvent.Shown));
        Logger.Log("::onInterstitialShown");
    }

    @Override
    public void onInterstitialClicked(MoPubInterstitial interstitial) {
        EventBus.getDefault().post(new InterstitialEvent(InterstitialEvent.Clicked));
        Logger.Log("::onInterstitialClicked");
    }

    private void show(final boolean isPeriodicShow, long delay) {
        Logger.Log("::Ads about to show mopubInterstitial");
        Helper.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //boolean isMopubNull = mopubInterstitial == null;

                boolean isMopubNull = false;
                boolean isLocked = isPeriodicShow ? lock.isAnyLocked() : lock.isHardLocked();
                //boolean isMopubReady = !isMopubNull && mopubInterstitial.isReady();
                boolean isMopubReady = HeyzapAds.hasStarted();
                Logger.Log("::I", "::isLocked: " + "::multiplayerLocalOnline [" + lock.localMultiplayer + ":: " + lock.onlineMultiplayer + "::]" + ":: " + "::internet [" + lock.internet + "::]" + ":: " + "::gap [" + lock.gap + "::]" + ":: " + "::stop [" + lock.stop + "::] " + "::game [" + lock.game + "::]");
                Logger.Log("::[isMopubNull(false) = " + isMopubNull + "::] " + "::[isSoftLocked(false) = " + lock.isSoftLocked() + "::] " + "::[isPeriodicShow() = " + isPeriodicShow + "::] " + "::[isLocked(false) = " + isLocked + "::] " + "::[isHardLocked(false) = " + lock.isHardLocked() + "::] " + "::[isMopubReady(true) = " + isMopubReady + "::]");
                if (!isMopubNull && !isLocked && isMopubReady) {
                    Logger.Log("::Showing mopubInterstitial");
                    //mopubInterstitial.show();
                    if (InterstitialAd.isAvailable())
                    prxy.startProxyActivity(context);
                }
            }
        }, delay);

    }

    public native void nesmrtelnostON();

    public native void nesmrtelnostOFF();

    public void destroy() {
        if (mopubInterstitial != null) {
           // mopubInterstitial.destroy();

        }
    }

    private void schedulePeriodicShows() {
        if (!periodicScheduled) {
            Logger.Log("::schedulePeriodicShows: Scheduled za " + String.valueOf(periodicMills));

            periodicShowRunnable = new Runnable() {
                @Override
                public void run() {
                    Logger.Log("::Interstitial", "::-executing periodic show");
                    show(true, 0);
                    Helper.removeFromWorkerThread(periodicShowRunnable);
                    Helper.runOnWorkerThread(periodicShowRunnable, (long) periodicMills);
                }
            };

            Helper.runOnWorkerThread(periodicShowRunnable, (long) periodicMills);

            periodicScheduled = true;
        }
    }

    public void init() {
        int delay = 4000;
        Logger.Log("::Initing Mopub in " + (delay / 1000) + " sec...");
        lock.game = Data.hasMinecraft;
        InterstitialAd.setOnStatusListener(this);
        InterstitialAd.fetch();
        Helper.runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                //if (mopubInterstitial == null || HeyzapAds.hasStarted()) {
                if ( !HeyzapAds.hasStarted()) {
                    //String mopubId = Helper.isDebugPackage(activity) ? DEBUG_MOPUB_INTERSTITIAL_ID : Data.Ads.Interstitial.mopubId;
                    //mopubInterstitial = new MoPubInterstitial(activity, mopubId);
                    //mopubInterstitial.setInterstitialAdListener(Interstitial.this);
                    //mopubInterstitial.setKeywords("game,minecraft,kids,casual");
                    //mopubInterstitial.load();
                    //InterstitialAd.
                    //HeyzapAds.setGdprConsent(true, activity.getApplicationContext());
                    //HeyzapAds.setBundleId("com.mmarcel.cnb2");

                    //HeyzapAds.setThirdPartyVerboseLogging(true);
                    InterstitialAd.fetch();
                //} else if (!mopubInterstitial.isReady()) {
                } else if (HeyzapAds.hasStarted()) {
                    Logger.Log("::Mopub Forcing Refresh");
                    InterstitialAd.fetch();
                    //mopubInterstitial.forceRefresh();

                }
            }
        }, delay);
    }

    private void gapLockForTime(long minimalAdGapMills) {
        lock.gapLock();
        Logger.Log("::lockForTime: scheduling unlock runnable za sec " + minimalAdGapMills / 1000);
        if (gapLockRunnable == null) {
            gapLockRunnable = new Runnable() {
                @Override
                public void run() {
                    lock.unlockGap();
                }
            };
        }
        Helper.removeFromWorkerThread(gapLockRunnable);
        Helper.runOnWorkerThread(gapLockRunnable, minimalAdGapMills);
    }

    private void load(long delay) {
        if (loadRunnable == null) {
            loadRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mopubInterstitial != null) {
                        //mopubInterstitial.load();
                    }
                    InterstitialAd.fetch();
                }
            };
        }
        Helper.removeFromWorkerThread(loadRunnable);
        Helper.runOnWorkerThread(loadRunnable, delay);
    }

    private void changePeriodicShowForHighEcpmCountry() {
        if (Data.country != null && Data.Ads.Interstitial.highEcpmCountries != null) {
            for (String highEcpmCountry : Data.Ads.Interstitial.highEcpmCountries) {
                if (highEcpmCountry.equals(Data.country)) {
                    periodicMills = Data.Ads.Interstitial.periodicShowMillsHigh;
                }
            }
        }
    }
//********Heyzap
    @Override
    public void onShow(String s) {
onInterstitialShown(null);
    }

    @Override
    public void onClick(String s) {
onInterstitialClicked(null);
    }

    @Override
    public void onHide(String s) {
onInterstitialDismissed(null);
    }

    @Override
    public void onFailedToShow(String s) {
onInterstitialFailed(null,MoPubErrorCode.SERVER_ERROR);
    }

    @Override
    public void onAvailable(String s) {
onInterstitialLoaded(null);
    }

    @Override
    public void onFailedToFetch(String s) {
onInterstitialFailed(null,MoPubErrorCode.NO_FILL);
    }

    @Override
    public void onAudioStarted() { }

    @Override
    public void onAudioFinished() { }
    //********Heyzap
    public class Lock {
        private boolean stop;
        private boolean onlineMultiplayer;
        private boolean localMultiplayer;
        private boolean internet;
        private boolean gap;
        private boolean game = true;


        public boolean isHardLocked() {
            //we never show in these conditions
            return gap || internet || stop || localMultiplayer;
        }

        public boolean isSoftLocked() {
            //we can show in these conditions
            return onlineMultiplayer || game;
        }

        public boolean isAnyLocked() {
            return onlineMultiplayer || game || gap || internet || stop || localMultiplayer;
        }

        public boolean isOnlineMultiplayerLocked() {
            return onlineMultiplayer;
        }

        public void unlockStop() {
            Logger.Log("::I", "::unlockStop: ");
            stop = false;
        }

        public void stopLock() {
            Logger.Log("::I", "::stopLock: ");
            stop = true;
        }


        public void unlockGap() {
            Logger.Log("::I", "::unlockGap: ");
            gap = false;
        }

        public void gapLock() {
            Logger.Log("::I", "::gapLock: ");
            if (Helper.FasterAds())
                return;
            gap = true;
        }

        public void lockMultiplayer() {
            Logger.Log("::I", "::lockMultiplayer: ");
            onlineMultiplayer = true;
        }

        public void unlockOnlineMultiplayer() {
            Logger.Log("::I", "::unlockOnlineMultiplayer: ");
            onlineMultiplayer = false;
        }

        public void gameUnlock() {
            Logger.Log("::I", "::gameUnlock: ");
            game = false;
        }

        public void gameLock() {
            Logger.Log("::I", "::gameLock: ");
            game = true;
        }

        public void internetLock() {
            Logger.Log("::I", "::internetLock: ");
            internet = true;
        }

        public void internetUnlock() {
            Logger.Log("::I", "::internetUnlock: ");
            internet = false;
        }

        public void unlockLocalMultiplayer() {
            Logger.Log("::I", "::unlockLocalMultiplayer: ");
            localMultiplayer = false;
        }

        public void lockLocalMultiplayer() {
            Logger.Log("::I", "::lockLocalMultiplayer: ");
            localMultiplayer = true;
        }

        public boolean isLocalMultiplayerLocked() {
            return localMultiplayer;
        }

        public boolean isGapLocked() {
            return gap;
        }
    }
}
