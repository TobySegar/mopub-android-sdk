package com.mopub.ads;


import android.app.Activity;

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
public class Interstitial implements MoPubInterstitial.InterstitialAdListener {
    private static final String DEBUG_MOPUB_INTERSTITIAL_ID = Logger.String("::c2fc437d0fd44e91982838693549cdb4");
    private MoPubInterstitial mopubInterstitial;
    private final Activity activity;
    private double periodicMills;
    private boolean periodicScheduled;
    public final Lock lock;
    boolean pauseScreenShowed;
    private Runnable loadRunnable;
    private Runnable gapLockRunnable;
    private Runnable periodicShowRunnable;
    private int timesBlockChanged;


    public Interstitial(final Activity activity) {
        this.activity = activity;
        this.periodicMills = Helper.FasterAds() ? 25000 : Data.Ads.Interstitial.periodicShowMillsLow;
        this.lock = new Lock();
        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGameEvent(GameEvent gameEvent) {
        switch (gameEvent.event) {
            case StartSleepInBed:
                show(false, 0);
                break;
            case BlockChanged:
                timesBlockChanged++;
                if (timesBlockChanged == 3) {
                    show(false, 0);
                    timesBlockChanged = 0;
                }
                break;
            case GamePlayStart:
                show(false, 5000);
                break;
            case LeaveServer:
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
        EventBus.getDefault().post(new InterstitialEvent(Dismissed));
        Logger.Log("::onInterstitialDismissed");

        gapLockForTime(Data.Ads.Interstitial.minimalGapMills);
        load();
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
        Helper.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean isMopubNull = mopubInterstitial == null;
                boolean isLocked = isPeriodicShow ? lock.isAnyLocked() : lock.isHardLocked();
                boolean isMopubReady = !isMopubNull && mopubInterstitial.isReady();
                Logger.Log("::I", "::isLocked: " + "::multiplayerLocalOnline [" + lock.localMultiplayer + ":: " + lock.onlineMultiplayer + "::]" + ":: " + "::internet [" + lock.internet + "::]" + ":: " + "::gap [" + lock.gap + "::]" + ":: " + "::stop [" + lock.stop + "::] " + "::game [" + lock.game + "::]");
                Logger.Log("::[isMopubNull(false) = " + isMopubNull + "::] " + "::[isSoftLocked(false) = " + lock.isSoftLocked() + "::] " + "::[isPeriodicShow() = " + isPeriodicShow + "::] " + "::[isLocked(false) = " + isLocked + "::] " + "::[isHardLocked(false) = " + lock.isHardLocked() + "::] " + "::[isMopubReady(true) = " + isMopubReady + "::]");
                if (!isMopubNull && !isLocked && isMopubReady) {
                    Logger.Log("::Showing mopubInterstitial");
                    mopubInterstitial.show();
                }
            }
        }, delay);

    }

    public native void nesmrtelnostON();

    public native void nesmrtelnostOFF();

    public void destroy() {
        if (mopubInterstitial != null) {
            mopubInterstitial.destroy();
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

    public void init(boolean fromOnlineAccepted, int delay) {
        Logger.Log("::Initing Mopub in " + (delay / 1000) + " sec...");
        lock.game = Data.hasMinecraft;

        Helper.runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                if (mopubInterstitial == null) {
                    String mopubId = Helper.isDebugPackage(activity) ? DEBUG_MOPUB_INTERSTITIAL_ID : Data.Ads.Interstitial.mopubId;
                    mopubInterstitial = new MoPubInterstitial(activity, mopubId);
                    mopubInterstitial.setInterstitialAdListener(Interstitial.this);
                    mopubInterstitial.setKeywords("game,minecraft,kids,casual");
                    mopubInterstitial.load();
                } else if (!mopubInterstitial.isReady()) {
                    Logger.Log("::Mopub Forcing Refresh");
                    mopubInterstitial.forceRefresh();
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

    private void load() {
        if (loadRunnable == null) {
            loadRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mopubInterstitial != null) {
                        mopubInterstitial.load();
                    }
                }
            };
        }
        Helper.removeFromWorkerThread(loadRunnable);
        Helper.runOnWorkerThread(loadRunnable, (long) 3000);
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
}
