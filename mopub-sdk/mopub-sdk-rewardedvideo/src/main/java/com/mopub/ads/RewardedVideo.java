package com.mopub.ads;


import android.app.Activity;
import android.support.annotation.NonNull;

import com.mojang.base.Helper;
import com.mojang.base.json.Data;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubRewardedVideoListener;
import com.mopub.mobileads.MoPubRewardedVideos;

import java.util.Set;

public class RewardedVideo implements MoPubRewardedVideoListener {
    private final Activity mActivity;
    private static boolean mIsInitialized;
    private final MoPubRewardedVideoListener mAdsListener;


    public RewardedVideo(Activity activity, MoPubRewardedVideoListener listener) {
        mActivity = activity;
        mAdsListener = listener;
    }

    private void init() {
        if (!mIsInitialized) {
            MoPubRewardedVideos.initializeRewardedVideo(mActivity);
            MoPubRewardedVideos.setRewardedVideoListener(this);

            mIsInitialized = true;
        } else {
            Helper.wtf("Trying to init already initialized RewardedVideos");
        }
    }

    public void loadAfterDelay(int delay) {
        if (!mIsInitialized) {
            init();
        }
        if (MoPubRewardedVideos.hasRewardedVideo(Data.Ads.RewardedVideo.id)) return;

        Helper.runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                Helper.wtf("Rewarded video -> reload()");
                MoPubRewardedVideos.loadRewardedVideo(Data.Ads.RewardedVideo.id);
            }
        });
    }

    public void show() {
        Helper.wtf("Rewarded video -> show()");
        MoPubRewardedVideos.showRewardedVideo(Data.Ads.RewardedVideo.id);
    }

    @Override
    public void onRewardedVideoLoadSuccess(@NonNull String adUnitId) {
        if (!MoPubRewardedVideos.hasRewardedVideo(adUnitId)) return;
        Helper.wtf("onRewardedVideoLoadSuccess");
        mAdsListener.onRewardedVideoLoadSuccess(adUnitId);
    }

    @Override
    public void onRewardedVideoLoadFailure(@NonNull String adUnitId, @NonNull MoPubErrorCode errorCode) {
        Helper.wtf("onRewardedVideoLoadFailure " + errorCode);
    }

    @Override
    public void onRewardedVideoStarted(@NonNull String adUnitId) {
        Helper.wtf("onRewardedVideoStarted");
        mAdsListener.onRewardedVideoStarted(adUnitId);
    }

    @Override
    public void onRewardedVideoPlaybackError(@NonNull String adUnitId, @NonNull MoPubErrorCode errorCode) {
        Helper.wtf("onRewardedVideoPlaybackError");
    }

    @Override
    public void onRewardedVideoClicked(@NonNull String adUnitId) {
        Helper.wtf("onRewardedVideoClicked");
    }

    @Override
    public void onRewardedVideoClosed(@NonNull String adUnitId) {
        Helper.wtf("onRewardedVideoClosed");
        mAdsListener.onRewardedVideoClosed(adUnitId);
    }

    @Override
    public void onRewardedVideoCompleted(@NonNull Set<String> adUnitIds, @NonNull MoPubReward reward) {
        Helper.wtf("onRewardedVideoCompleted");
        mAdsListener.onRewardedVideoCompleted(adUnitIds, reward);
    }

    public Activity getActivity() {
        return mActivity;
    }

    public void onCreate() {
        MoPub.onCreate(mActivity);
    }

    public void onPause() {
        MoPub.onPause(mActivity);
    }

    public void onDestroy() {
        MoPub.onDestroy(mActivity);
    }

    public void onStop() {
        MoPub.onStop(mActivity);
    }

    public void onResume() {
        MoPub.onResume(mActivity);
    }

    public void onRestart() {
        MoPub.onRestart(mActivity);
    }

    public void onStart() {
        MoPub.onStart(mActivity);
    }

    public boolean isInitialized() {
        return mIsInitialized;
    }

    public boolean isReady() {
        return MoPubRewardedVideos.hasRewardedVideo(Data.Ads.RewardedVideo.id);
    }
}
