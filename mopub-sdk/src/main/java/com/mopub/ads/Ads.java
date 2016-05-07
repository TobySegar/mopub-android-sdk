package com.mopub.ads;


import android.content.SharedPreferences;
import android.util.Log;

import com.mojang.base.Helper;
import com.mojang.base.InternetObserver;
import com.mojang.base.events.AppEvent;
import com.mojang.base.events.GuideGameEvent;
import com.mojang.base.events.MinecraftGameEvent;
import com.mojang.base.events.OfflineEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Calendar;

/**
 * Controlls how ads are showed
 */
public class Ads {

    private final String TAG = this.getClass().getName();
    private final InternetObserver internetObserver;
    private Interstitial interstitial;
    private int numOfPlayers;
    private boolean firstGamePlayStart;
    private int timesBlockChanged;
    private long[] blockPlaceTimes = new long[5];
    boolean isBuilding;
    private SharedPreferences sharedPreferences;
    private Calendar calendar;
    private static final String FIRST_RUN_DAY_KEY = "FirstRunDay";
    static final String FIRST_RUN_KEY = "FirstRun";
    private static final int NUM_FREE_DAYS = 2;
    private final int measureUnit = Calendar.DAY_OF_YEAR;
    private boolean fingerAdShowed;


    public Ads(Interstitial interstitial, InternetObserver internetObserver, SharedPreferences sharedPreferences, Calendar calendar, boolean freePeriodAllowed) {
        this.internetObserver = internetObserver;
        this.interstitial = interstitial;
        this.numOfPlayers = 1;
        this.firstGamePlayStart = true;
        this.sharedPreferences = sharedPreferences;
        this.calendar = calendar;

        this.interstitial.setFreePeriod(isInFreePeriod(freePeriodAllowed));

        EventBus.getDefault().register(this);
    }

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
                if (numOfPlayers > 1) numOfPlayers--;
                if (numOfPlayers == 1) interstitial.unlock();
                break;
            case GamePlayStart:
                interstitial.showDelayed(500);
                break;
            case LeaveLevel:
                interstitial.showDelayed(500);
                break;
            case StartSleepInBed:
                interstitial.showDelayed(500);
                break;
            case PlayerHurt:
                interstitial.lockFor(10000);
                break;
            case BlockPlaced:
                checkIfBuilding(blockPlaceTimes, 2, 1000, System.currentTimeMillis());
                break;
            case CameraMoveX:
                if(interstitial.canGetFingerAd) showAdIfBuilding();
                break;
            case CameraMoveY:
                if(interstitial.canGetFingerAd) showAdIfBuilding();
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
        if (internetObserver.isInternetAvaible()) {
            Log.e(TAG, "start");
            interstitial.init();
        } else {
            Log.i(TAG, "start: No Internet Avaible for ads");
        }
    }

    public boolean isInFreePeriod(boolean freePeriodAllowed) {
        if (Helper.DEBUG) {
            Log.e(TAG, "isInFreePeriod: false cause debug");
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

    /**
     *  Should be called when block is placed, checks previous block placements times
     *  and sums up difference in placements between last {@code numOfElemetsToSum} and compares
     *  it to {@code averageTimeBetweenPlacement}
     *
     * @param blockPlaceTimes System times of previous block placements
     * @param numOfElemetsToSum number times between placement to sum
     * @param averageTimeBetweenPlacement average period between block placements to be considered building
     * @return if {@code numOfElemetsToSum} <= {@code averageTimeBetweenPlacement}
     */
    void checkIfBuilding(long[] blockPlaceTimes, int numOfElemetsToSum, int averageTimeBetweenPlacement, long currentBlockPlaceTime) {
        //shift elements up && add time to last position
        final int lastIndex = blockPlaceTimes.length - 1;
        System.arraycopy(blockPlaceTimes, 1, blockPlaceTimes, 0, lastIndex);
        blockPlaceTimes[lastIndex] = currentBlockPlaceTime;

        //zrataj time differences bettwenn last numOfElemetsToSum
        long sumOfTimeDifferences = 0;
        for (int i = lastIndex; i >= 0; i--) {
            if (i >= blockPlaceTimes.length - numOfElemetsToSum) {
                long difference = blockPlaceTimes[i] - blockPlaceTimes[i-1];
                sumOfTimeDifferences += difference;
            }else{
                break;
            }
        }

        final int totalBetweenPeriod = averageTimeBetweenPlacement * numOfElemetsToSum;
        isBuilding = sumOfTimeDifferences <= totalBetweenPeriod;
        if(isBuilding) Log.e(TAG, "checkIfBuilding: TRUE" );
    }

    void showAdIfBuilding() {
        if (isBuilding && !fingerAdShowed) {
            if(interstitial.show()){
                fingerAdShowed = true;
            }
            this.isBuilding = false;
        }

    }

}
