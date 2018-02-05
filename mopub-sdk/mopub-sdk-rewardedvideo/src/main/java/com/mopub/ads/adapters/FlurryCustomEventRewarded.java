package com.mopub.ads.adapters;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.flurry.android.ads.FlurryAdErrorType;
import com.flurry.android.ads.FlurryAdInterstitial;
import com.flurry.android.ads.FlurryAdInterstitialListener;
import com.mojang.base.Helper;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
import com.mopub.mobileads.CustomEventRewardedVideo;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubRewardedVideoManager;

import java.util.Map;


public class FlurryCustomEventRewarded extends CustomEventRewardedVideo implements FlurryAdInterstitialListener {
    private FlurryAdInterstitial mFlurryAdInterstitial;
    private Activity mContext;

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {
        mContext = launcherActivity;

        if(!validateExtras(serverExtras)){return false;}
        String apiKey = serverExtras.get(FlurryAgentWrapper.PARAM_API_KEY);
        String mAdSpaceName = serverExtras.get(FlurryAgentWrapper.PARAM_AD_SPACE_NAME);


        if (FlurryAgentWrapper.getInstance() != null) {
            FlurryAgentWrapper.getInstance().startSession(launcherActivity, apiKey, null);
        }

        mFlurryAdInterstitial = new FlurryAdInterstitial(launcherActivity, mAdSpaceName);
        mFlurryAdInterstitial.setListener(this);


        return FlurryAgentWrapper.getInstance() != null;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {
        // allow us to get callbacks for ad events
        if(mFlurryAdInterstitial != null) {
            mFlurryAdInterstitial.setListener(this);
            mFlurryAdInterstitial.fetchAd();
        }else{
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(
                    FlurryCustomEventRewarded.class,
                    "",
                    MoPubErrorCode.UNSPECIFIED);
        }
    }

    private boolean validateExtras(final Map<String, String> serverExtras) {
        if (serverExtras == null) {
            return false;
        }

        final String flurryApiKey = serverExtras.get(FlurryAgentWrapper.PARAM_API_KEY);
        final String flurryAdSpace = serverExtras.get(FlurryAgentWrapper.PARAM_AD_SPACE_NAME);
        Helper.wtf("ServerInfo fetched from Mopub " + FlurryAgentWrapper.PARAM_API_KEY + " : "
                + flurryApiKey + " and " + FlurryAgentWrapper.PARAM_AD_SPACE_NAME + " :" +
                flurryAdSpace);

        return (!TextUtils.isEmpty(flurryApiKey) && !TextUtils.isEmpty(flurryAdSpace));
    }

    @Override
    @Nullable
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @Override
    @NonNull
    protected String getAdNetworkId() {
        return "";
    }

    @Override
    protected void onInvalidate() {
        if (mContext == null) {
            return;
        }

        if (mFlurryAdInterstitial != null) {
            mFlurryAdInterstitial.destroy();
            mFlurryAdInterstitial = null;
        }

        FlurryAgentWrapper.getInstance().endSession(mContext);

        mContext = null;
    }


    @Override
    protected boolean hasVideoAvailable() {
        return mFlurryAdInterstitial.isReady();
    }

    @Override
    protected void showVideo() {
        mFlurryAdInterstitial.displayAd();
    }

    @Override
    public void onFetched(FlurryAdInterstitial flurryAdInterstitial) {
        MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(
                FlurryCustomEventRewarded.class,
                flurryAdInterstitial.getAdSpace());
    }

    @Override
    public void onRendered(FlurryAdInterstitial flurryAdInterstitial) {

    }


    @Override
    public void onDisplay(FlurryAdInterstitial flurryAdInterstitial) {
        MoPubRewardedVideoManager.onRewardedVideoStarted(
                FlurryCustomEventRewarded.class,
                flurryAdInterstitial.getAdSpace());
    }

    @Override
    public void onClose(FlurryAdInterstitial flurryAdInterstitial) {
        MoPubRewardedVideoManager.onRewardedVideoClosed(
                FlurryCustomEventRewarded.class,
                flurryAdInterstitial.getAdSpace());
    }

    @Override
    public void onAppExit(FlurryAdInterstitial flurryAdInterstitial) {

    }

    @Override
    public void onClicked(FlurryAdInterstitial flurryAdInterstitial) {
        MoPubRewardedVideoManager.onRewardedVideoClicked(
                FlurryCustomEventRewarded.class,
                flurryAdInterstitial.getAdSpace());
    }


    @Override
    public void onVideoCompleted(FlurryAdInterstitial flurryAdInterstitial) {
        MoPubRewardedVideoManager.onRewardedVideoCompleted(
                FlurryCustomEventRewarded.class,
                flurryAdInterstitial.getAdSpace(),
                MoPubReward.success("", 1));
    }

    @Override
    public void onError(FlurryAdInterstitial flurryAdInterstitial, FlurryAdErrorType adErrorType, int errorCode) {
        Helper.wtf(String.format("onError: Flurry Rewarded ad not available. " + "Error type: %s. Error code: %s", adErrorType.toString(), errorCode));

        switch (adErrorType) {
            case FETCH:
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(
                        FlurryCustomEventRewarded.class,
                        flurryAdInterstitial.getAdSpace(),
                        MoPubErrorCode.NO_FILL);
                return;
            case RENDER:
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(
                        FlurryCustomEventRewarded.class,
                        flurryAdInterstitial.getAdSpace(),
                        MoPubErrorCode.NETWORK_INVALID_STATE);
                return;
            case CLICK:
                // Don't call onInterstitialFailed in this case.
                return;
            default:
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(
                        FlurryCustomEventRewarded.class,
                        flurryAdInterstitial.getAdSpace(),
                        MoPubErrorCode.UNSPECIFIED);
        }
    }

}
