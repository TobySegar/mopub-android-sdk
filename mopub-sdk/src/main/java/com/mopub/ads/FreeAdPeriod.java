package com.mopub.ads;


import android.content.SharedPreferences;
import android.util.Log;

import com.mojang.base.Helper;
import com.mojang.base.json.Data;

import java.util.Calendar;

public class FreeAdPeriod {

    private SharedPreferences sharedPreferences;
    private Calendar calendar;
    private static final String FIRST_RUN_DAY_KEY = "FirstRunDay";
    private static final String FIRST_RUN_KEY = "FirstRun";
    private static final int NUM_FREE_DAYS = 2;
    private boolean didMarkFirstDay;
    private String TAG = this.getClass().getName();
    private final int measureUnit = Calendar.DAY_OF_YEAR;

    public FreeAdPeriod(SharedPreferences sharedPreferences,Calendar calendar){
        this.sharedPreferences = sharedPreferences;
        this.calendar = calendar;
        this.didMarkFirstDay = sharedPreferences.getBoolean(FIRST_RUN_KEY,false);

        if (!didMarkFirstDay) {
            int today = calendar.get(measureUnit);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(FIRST_RUN_DAY_KEY, today).apply();
            editor.putBoolean(FIRST_RUN_KEY, true);
        }
    }

    public boolean isFree() {
        if(Helper.DEBUG){
            Log.e(TAG, "isFree: false cause debug");
            return false;
        }
        if(Data.Ads.Interstitial.freePeriod){
            int firstRunDay = sharedPreferences.getInt(FIRST_RUN_DAY_KEY,-1);
            if(firstRunDay != -1){
                int today = calendar.get(measureUnit);
                int endFreeDay = firstRunDay+NUM_FREE_DAYS;
                return today >= firstRunDay && today <= endFreeDay;
            }
        }
        return false;
    }
}
