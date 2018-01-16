package com.mopub.ads;


import com.mojang.base.Helper;

public class AdLock {
    private boolean stop;
    private boolean onlineMultiplayer;
    private boolean localMultiplayer;
    private boolean internet;
    private boolean gap;
    private boolean game = true;
    private boolean freeDay;


    public boolean isHardLocked() {
        //we never show in these conditions
        return gap || internet || stop || localMultiplayer || freeDay;
    }

    public boolean isSoftLocked() {
        //we can show in these conditions
        return onlineMultiplayer || game;
    }

    public boolean isAnyLocked() {
        return onlineMultiplayer || game || gap || internet || stop || localMultiplayer || freeDay;
    }

    public boolean isOnlineMultiplayerLocked() {
        return onlineMultiplayer;
    }

    public void unlockStop() {
        Helper.wtf("I", "unlockStop: ");
        stop = false;
    }

    public void stopLock() {
        Helper.wtf("I", "stopLock: ");
        stop = true;
    }

    public void lockFreeDay() {
        Helper.wtf("I", "lockFreeDay: ");
        freeDay = true;
    }

    public void unlockGap() {
        Helper.wtf("I", "unlockGap: ");
        gap = false;
    }

    public void gapLock() {
        Helper.wtf("I", "gapLock: ");
        gap = true;
    }

    public void lockMultiplayer() {
        Helper.wtf("I", "lockMultiplayer: ");
        onlineMultiplayer = true;
    }

    public void unlockOnlineMultiplayer() {
        Helper.wtf("I", "unlockOnlineMultiplayer: ");
        onlineMultiplayer = false;
    }

    public void gameUnlock() {
        Helper.wtf("I", "gameUnlock: ");
        game = false;
    }

    public void gameLock() {
        Helper.wtf("I", "gameLock: ");
        game = true;
    }

    public void internetLock() {
        Helper.wtf("I", "internetLock: ");
        internet = true;
    }

    public void internetUnlock() {
        Helper.wtf("I", "internetUnlock: ");
        internet = false;
    }

    public void unlockLocalMultiplayer() {
        Helper.wtf("I", "unlockLocalMultiplayer: ");
        localMultiplayer = false;
    }

    public void lockLocalMultiplayer() {
        Helper.wtf("I", "lockLocalMultiplayer: ");
        localMultiplayer = true;
    }

    public boolean isLocalMultiplayerLocked() {
        return localMultiplayer;
    }

    public boolean isGapLocked() {
        return gap;
    }
}