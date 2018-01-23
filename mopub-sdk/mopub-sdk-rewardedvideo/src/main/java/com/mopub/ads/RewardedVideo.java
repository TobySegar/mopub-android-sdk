package com.mopub.ads;


import android.app.Activity;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.mojang.base.Helper;
import com.mojang.base.json.Data;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubRewardedVideoListener;
import com.mopub.mobileads.MoPubRewardedVideos;

import java.util.Calendar;
import java.util.Set;

public class RewardedVideo implements MoPubRewardedVideoListener {
    private final Activity mActivity;
    private static boolean mIsInitialized;
    private final String FREE_DAY_KEY = "FREE_DAY";
    private final MoPubRewardedVideoListener mAdsListener;
    private SharedPreferences mSharedPreferences;
    private int mCurrentDateInYear;
    private final String NUM_OF_WATCHED_VID_KEY = "NUM_OF_WATCHED_VID";
    private int MAX_NUM_OF_VIDS_PER_DAY = 3;

//    public RewardedVideo(Activity activity,RewardedVideo.UI ui) {
//        mActivity = activity;
//        mSharedPreferences = activity.getSharedPreferences("REWARDED_VIDEO", Context.MODE_PRIVATE);
//        mCurrentDateInYear = setAndGetCurrentDayInYear();
//        mUI = ui;
//    }

    public RewardedVideo(Activity activity, MoPubRewardedVideoListener listener) {
        mActivity = activity;
        mAdsListener = listener;
    }

    private int setAndGetCurrentDayInYear() {
        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        final String CURRENT_DAY_KEY = "CURRENT_DAY";
        int lastCurrentDay = mSharedPreferences.getInt(CURRENT_DAY_KEY, 0);

        //Check if day changed
        if (currentDay != lastCurrentDay) {
            mSharedPreferences.edit().putInt(CURRENT_DAY_KEY, currentDay).apply();
            resetNumberOfWatchedVideosForToday();
        }
        return currentDay;
    }

    private void init() {
        if (!mIsInitialized) {
            //final List<Class<? extends CustomEventRewardedVideo>> sNetworksToInit = Arrays.asList(
            //        AdColonyRewardedVideo.class,
            //        ChartboostRewardedVideo.class,
            //        FacebookRewardedVideo.class,
            //        GooglePlayServicesRewardedVideo.class,
            //        AppLovinCustomEventRewardedVideo.class
            //        );

            //MoPubRewardedVideos.initializeRewardedVideo(mActivity,sNetworksToInit);
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

        if (getNumberOfWatchedVideosForToday() != MAX_NUM_OF_VIDS_PER_DAY) {
            loadAfterDelay(0);
        }
    }

    @Override
    public void onRewardedVideoCompleted(@NonNull Set<String> adUnitIds, @NonNull MoPubReward reward) {
        Helper.wtf("onRewardedVideoCompleted");
        mAdsListener.onRewardedVideoCompleted(adUnitIds, reward);
        incrementNumberOfWatchedVideosForToday();

        if (getNumberOfWatchedVideosForToday() == MAX_NUM_OF_VIDS_PER_DAY) {
            reward();
        }
    }

    private void reward() {
        mSharedPreferences.edit().putInt(FREE_DAY_KEY, mCurrentDateInYear).apply();
        //Ads.i().onFreeDayEarned();
    }

    public boolean hasFreeDayActive() {
        int freeDay = mSharedPreferences.getInt(FREE_DAY_KEY, 0);
        return mCurrentDateInYear == freeDay;
    }

    private int getNumberOfWatchedVideosForToday() {
        return mSharedPreferences.getInt(NUM_OF_WATCHED_VID_KEY, 0);
    }

    private void incrementNumberOfWatchedVideosForToday() {
        int currentNumOfVids = mSharedPreferences.getInt(NUM_OF_WATCHED_VID_KEY, 0);
        if (currentNumOfVids < MAX_NUM_OF_VIDS_PER_DAY) {
            mSharedPreferences.edit().putInt(NUM_OF_WATCHED_VID_KEY, (currentNumOfVids + 1)).apply();
        } else {
            Helper.wtf("Cant increment number of videos past maximum " + MAX_NUM_OF_VIDS_PER_DAY);
        }
    }

    private void resetNumberOfWatchedVideosForToday() {
        mSharedPreferences.edit().putInt(NUM_OF_WATCHED_VID_KEY, 0).apply();
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

    public void stopAndCleanUp() {
    }

    public boolean isInitialized() {
        return mIsInitialized;
    }

    public boolean isReady() {
        return MoPubRewardedVideos.hasRewardedVideo(Data.Ads.RewardedVideo.id);
    }

    public interface UI {
        void onVideoLoading();

        void showOnVidSuccess();
    }
}
