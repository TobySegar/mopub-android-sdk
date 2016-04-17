package com.mopub.ads;

import android.app.Activity;
import android.util.Log;

import com.mopub.common.event.Event;
import com.mopub.mobileads.MoPubInterstitial;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import mojang.com.base.Game;
import mojang.com.base.Helper;
import mojang.com.base.Screen;
import mojang.com.base.WorkerThread;
import mojang.com.base.events.GuideGameEvent;
import mojang.com.base.events.MinecraftGameEvent;

/**
 * Shows interstitials to the minecraft game user based on game events
 * Shows finger ad
 */
public class CountryFingerInterstitial extends TimeLimitedInterstitial {
    private final String TAG = this.getClass().getName();
    private final List<String> fingerCountries;
    private boolean canShowFingerAd;
    private boolean fingerAdScheduled;
    private boolean schouldBeGettingFingerAds;

    private boolean fingerToAll;
    private final Game game;
    private final WorkerThread workerThread;
    private long fingerAdMills;
    private String country;


    public CountryFingerInterstitial(Activity activity, String interstitialId,Screen screen, WorkerThread workerThread,
                                     long adTimeGapMills, Game currentGame, List<String> fingerCountries) {
        super(activity, interstitialId,screen, workerThread, adTimeGapMills);
        this.game = currentGame;
        this.workerThread = workerThread;
        this.fingerCountries = fingerCountries;

        EventBus.getDefault().register(this);
}

    @Override
    public void onInterstitialLoaded(MoPubInterstitial moPubInterstitial) {
        super.onInterstitialLoaded(moPubInterstitial);

        if (country == null) {
            country = moPubInterstitial.getCountry();
        }
        //todo implement
        //scheduleFingerAd(country);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGameEvent(MinecraftGameEvent gameEvent) {
        switch (gameEvent.event) {
            case GamePlayStart:
                super.show();
                break;
            case LeaveLevel:
                super.show();
                break;
            case InBedClosed:
                super.show();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(GuideGameEvent gameEvent) {
        switch (gameEvent.event) {
            case OpenInventory:
                super.show();
                break;
        }
    }

    public void setFingerToAll(boolean fingerToAll) {
        this.fingerToAll = fingerToAll;
    }

    private void scheduleFingerAd(String country) {
        if (game == Game.Minecraft && (fingerCountries.contains(country) || fingerToAll)) {
            if (!fingerAdScheduled) {
                int RANDOMIZE_PERCENTAGE = 25;
                long randomizer = (fingerAdMills / 100) * RANDOMIZE_PERCENTAGE;
                long adTimeRandomizedMills = Helper.randLong(fingerAdMills - randomizer, fingerAdMills + randomizer);

                workerThread.scheduleGameTime(new Runnable() {
                                                  @Override
                                                  public void run() {
                                                      canShowFingerAd = true;
                                                      fingerAdScheduled = false;
                                                  }
                                              },
                        adTimeRandomizedMills);
            } else {
                Log.e(TAG, "scheduleFingerAd: finger Ad already scheduled or should not be getting finger ads");
            }

            fingerAdScheduled = true;
        }
    }
}
