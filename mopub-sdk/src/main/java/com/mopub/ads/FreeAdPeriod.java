package com.mopub.ads;


import android.content.SharedPreferences;
import android.util.Log;

import com.mojang.base.Helper;

import java.util.Calendar;

public class FreeAdPeriod {

    private SharedPreferences sharedPreferences;
    private Calendar calendar;
    private static final String FIRST_RUN_DAY_KEY = "FirstRunDay";
    private static final String FIRST_RUN_KEY = "FirstRun";
    private static final int NUM_FREE_DAYS = 2;
    private boolean runnedBefore;
    private String TAG = this.getClass().getName();
    private final int measureUnit = Calendar.DAY_OF_YEAR;

    public FreeAdPeriod(SharedPreferences sharedPreferences,Calendar calendar){
        this.sharedPreferences = sharedPreferences;
        this.calendar = calendar;
        this.runnedBefore = sharedPreferences.getBoolean(FIRST_RUN_KEY,false);

        if (!runnedBefore) {
            int today = calendar.get(measureUnit);
            sharedPreferences.edit().putInt(FIRST_RUN_DAY_KEY,today).apply();
            sharedPreferences.getBoolean(FIRST_RUN_KEY,true);
        }
    }

    public boolean isFree() {
        if(Helper.DEBUG){
            Log.e(TAG, "isFree: false cause debug");
            return false;
        }
        if(!runnedBefore){
            int firstRunDay = sharedPreferences.getInt(FIRST_RUN_DAY_KEY,-1);
            int today = calendar.get(measureUnit);
            int endFreeDay = firstRunDay+NUM_FREE_DAYS;
            return today >= firstRunDay && today <= endFreeDay;
        }else{
            Log.e(TAG, "isFree: forget to set free date for ads");
        }
        return false;
    }
}
