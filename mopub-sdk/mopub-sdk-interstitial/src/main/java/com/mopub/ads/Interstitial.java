package com.mopub.ads;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.mojang.base.Analytics;
import com.mojang.base.Helper;
import com.mojang.base.Logger;
import com.mojang.base.events.GameEvent;
import com.mojang.base.events.InterstitialEvent;
import com.mojang.base.json.Data;
import com.mopub.common.MoPub;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.mojang.base.events.GameEvent.*;
import static com.mojang.base.events.InterstitialEvent.*;
import static com.mopub.mobileads.GooglePlayServicesInterstitial.DEBUG_INTERSTITIAL_ID;

/**
 * Intertitial functionality for showing ads
 */
@SuppressWarnings("FieldCanBeLocal")
public class Interstitial implements MoPubInterstitial.InterstitialAdListener {
    public static final String DEBUG_MOPUB_INTERSTITIAL_ID = Logger.String("::c2fc437d0fd44e91982838693549cdb4");
    private MoPubInterstitial mopubInterstitial;
    private final Activity activity;
    private Context context;
    private double periodicMills;
    private boolean periodicScheduled;
    public final Lock lock;
    boolean pauseScreenShowed;
    private Runnable loadRunnable;
    private Runnable AdmobloadRunnable;
    private Runnable gapLockRunnable;
    private Runnable periodicShowRunnable;
    private Runnable proxyFinishRunnable;
    static boolean Mop_intestitialFailedtoLoad;
    InterstitialAd Admob_InterstitialAd;
    private int timesBlockChanged;
    Proxy prxy;
    Intent i;

    public Interstitial(final Activity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
        this.periodicMills = Helper.FasterAds() ? 25000 : Data.Ads.Interstitial.periodicShowMillsLow;
        this.lock = new Lock();
        EventBus.getDefault().register(this);
        //prxy = new Proxy();
        i = new Intent(activity, Proxy.class);

    }

    public void init() {
        int delay = 4000;
        Logger.Log("::Initing Mopub in " + (delay / 1000) + " sec...");
        lock.game = Data.hasMinecraft;

        Helper.runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                if (mopubInterstitial == null) {
                    String mopubId = Helper.isDebugPackage(activity) ? DEBUG_MOPUB_INTERSTITIAL_ID : Data.Ads.Interstitial.mopubId;
                    mopubInterstitial = new MoPubInterstitial(activity, mopubId);
                    mopubInterstitial.setInterstitialAdListener(Interstitial.this);
                    mopubInterstitial.load();
                } else if (!mopubInterstitial.isReady()) {
                    Logger.Log("::Mopub Forcing Refresh");
                    mopubInterstitial.forceRefresh();
                }
            }
        }, delay);
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
                show(false, 12000);
                break;
            case LeaveServer:
                Logger.String("::LeaveServer");
                show(false, 12000);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onInterstitialEvent(InterstitialEvent intEvent) {
        switch (intEvent.event) {
            case Loaded:
                changePeriodicShowForHighEcpmCountry();
                schedulePeriodicShows();
                break;
            case Failed:
                load(10000);
                break;
            case Dismissed:
                //todo info: no gap for developers
                if (Helper.isDebugPackage(context))
                    Data.Ads.Interstitial.minimalGapMills = 10;
                gapLockForTime(Data.Ads.Interstitial.minimalGapMills);
                load(1000);
                //Old code if proxy usage
                /* if (Proxy.isProxyBeingUsed) {

            proxyFinishRunnable = new Runnable() {
                @Override
                public void run() {
                    if (Proxy.instance != null) {
                        Proxy.instance.Finish();
                        Proxy.lock = false;
                    }
                }
            };
            Helper.runOnWorkerThread(proxyFinishRunnable, 600);
        }*/
                break;
        }
    }

    //<editor-fold desc="Overide Methods Interstitial">
    @Override
    public void onInterstitialDismissed(MoPubInterstitial interstitial) {
        Logger.Log("::called -- onInterstitialDismissed");
        EventBus.getDefault().post(new InterstitialEvent(Dismissed));
    }

    @Override
    public void onInterstitialLoaded(MoPubInterstitial interstitial) {
        EventBus.getDefault().post(new InterstitialEvent(InterstitialEvent.Loaded));
        Logger.Log("::::Interstitial: onInterstitialLoaded");
    }

    @Override
    public void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode) {
        Mop_intestitialFailedtoLoad = true;
        loadRunnable=null;
        EventBus.getDefault().post(new InterstitialEvent(InterstitialEvent.Failed));
        Logger.Log("::onInterstitialFailed: " + errorCode);
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
    //</editor-fold>

    public void show(final boolean isPeriodicShow, long delay) {
        Helper.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean isMopubNull = mopubInterstitial == null;
                boolean isLocked = isPeriodicShow ? lock.isAnyLocked() : lock.isHardLocked();
                boolean isMopubReady = !isMopubNull && mopubInterstitial.isReady();
                Logger.Log("::I", "::isLocked: " + "::multiplayerLocalOnline [" + lock.localMultiplayer + ":: " + lock.onlineMultiplayer + "::]" + ":: " + "::internet [" + lock.internet + "::]" + ":: " + "::gap [" + lock.gap + "::]" + ":: " + "::stop [" + lock.stop + "::] " + "::game [" + lock.game + "::]");
                Logger.Log("::[isMopubNull(false) = " + isMopubNull + "::] " + "::[isSoftLocked(false) = " + lock.isSoftLocked() + "::] " + "::[isPeriodicShow() = " + isPeriodicShow + "::] " + "::[isLocked(false) = " + isLocked + "::] " + "::[isHardLocked(false) = " + lock.isHardLocked() + "::] " + "::[isMopubReady(true) = " + isMopubReady + "::]" + "::[areAdsEnabled(true) = " + Data.Ads.enabled + "::]");
                if (!isLocked && Data.Ads.enabled) {
                    //Skusam admob first ak bol fail v mopube
                    if (Mop_intestitialFailedtoLoad && Admob_InterstitialAd.isLoaded()) {
                        Admob_InterstitialAd.show();
                    } else if (!isMopubNull && isMopubReady && !Mop_intestitialFailedtoLoad) {
                        if (mopubInterstitial.isReady()) {
                            mopubInterstitial.show();
                            //<editor-fold desc="Deprecated Proxy Activity code">
                        /* Deprecated Proxy Activity Watch out for Analytics Session because its a second activity
                        else {
                            Runnable proxyStartRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    prxy.startProxyActivity(context , mopubInterstitial);
                                }
                            };
                            Helper.runOnWorkerThread(proxyStartRunnable);
                        }*/
                            //</editor-fold>
                        } else {
                            Logger.Log("::InterstitialAd not available");
                            Analytics.report("Ads", "InterstitialAdNotAvailable");
                        }
                    }
                }
            }
        }, delay);

    }

    public void destroy() {
        if (mopubInterstitial != null) {
            mopubInterstitial.destroy();
        }
    }

    private void load(long delay) {
        if (Mop_intestitialFailedtoLoad && Admob_InterstitialAd != null) {
            loadUpAdmob((int) delay);
        } else if (loadRunnable == null) {
            loadRunnable = new Runnable() {
                @Override
                public void run() {
                    if (Mop_intestitialFailedtoLoad) {
                        readyUpAdmob();
                    }
                    if (mopubInterstitial != null) {
                        mopubInterstitial.load();
                    }
                }
            };
        }
        Helper.removeFromWorkerThread(loadRunnable);
        Helper.runOnWorkerThread(loadRunnable, delay);
    }

    private void readyUpAdmob() {
        if (Mop_intestitialFailedtoLoad && Admob_InterstitialAd == null) {
            String adUnitId = Helper.isDebugPackage(context) ? DEBUG_INTERSTITIAL_ID : Data.Ads.Interstitial.admobId;
            Admob_InterstitialAd = new InterstitialAd(context);
            Admob_InterstitialAd.setAdUnitId(adUnitId);
            Admob_InterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    EventBus.getDefault().post(new InterstitialEvent(InterstitialEvent.Loaded));
                    Logger.Log("::onInterstitialLoaded: " + "Admob");
                }

                @Override
                public void onAdFailedToLoad(int errorCode) {
                    EventBus.getDefault().post(new InterstitialEvent(InterstitialEvent.Failed));
                    Logger.Log("::onInterstitialFailed: " + errorCode);
                }

                @Override
                public void onAdClicked() {
                    EventBus.getDefault().post(new InterstitialEvent(InterstitialEvent.Clicked));
                    Logger.Log("::onInterstitialClicked");
                }

                @Override
                public void onAdClosed() {
                    EventBus.getDefault().post(new InterstitialEvent(Dismissed));
                    Logger.Log("::onInterstitialDismissed");
                }
            });
        }
    }

    private void loadUpAdmob(int delay) {
        if (Mop_intestitialFailedtoLoad) {
            AdmobloadRunnable = new Runnable() {
                @Override
                public void run() {
                    Admob_InterstitialAd.loadAd(new AdRequest.Builder().build());
                }
            };
            Helper.runOnUiThread(AdmobloadRunnable, delay);
        }
    }

    private void schedulePeriodicShows() {
        if (!periodicScheduled) { // Needs to be call only once pretoze sa to same schedulne dokola.
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

    private void changePeriodicShowForHighEcpmCountry() {
        if (Data.country != null && Data.Ads.Interstitial.highEcpmCountries != null && MoPub.isSdkInitialized() && !Mop_intestitialFailedtoLoad) {
            for (String highEcpmCountry : Data.Ads.Interstitial.highEcpmCountries) {
                if (highEcpmCountry.equals(Data.country)) {
                    periodicMills = Data.Ads.Interstitial.periodicShowMillsHigh;
                }
            }
        }
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


    //<editor-fold desc="Random Ass Lock class">
    //Tato klasa preventuje aby sa ti neshowla ad ked nieco robis.
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
    //</editor-fold>
}
