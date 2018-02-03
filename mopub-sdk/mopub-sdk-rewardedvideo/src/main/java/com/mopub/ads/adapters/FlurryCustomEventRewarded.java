package com.mopub.ads.adapters;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.flurry.android.ads.FlurryAdErrorType;
import com.flurry.android.ads.FlurryAdInterstitial;
import com.flurry.android.ads.FlurryAdInterstitialListener;
import com.mojang.base.Helper;
import com.mopub.mobileads.MoPubInterstitial;

import java.util.Map;

import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;

/**
 * Created by Wao on 1/15/2018.
 */

//todo toto nieje rewarded video man
public class FlurryCustomEventRewarded extends com.mopub.mobileads.CustomEventInterstitial implements FlurryAdInterstitialListener {
    private Context mContext;
    private CustomEventInterstitialListener mListener;
    private String mAdSpaceName;

    private FlurryAdInterstitial mInterstitial;

    // CustomEventInterstitial
    @Override
    protected void loadInterstitial(Context context,
                                    CustomEventInterstitialListener listener,
                                    Map<String, Object> localExtras,
                                    Map<String, String> serverExtras) {

        mListener = listener;
        //serverExtras = new HashMap<>();
        //serverExtras.put(FlurryAgentWrapper.PARAM_AD_SPACE_NAME, "int");
        //serverExtras.put(FlurryAgentWrapper.PARAM_API_KEY, "P3382GDJ5NWFCYQN4X8B");

        if (context == null) {
            Helper.wtf("Context cannot be null.");
            listener.onInterstitialFailed(ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        if (listener == null) {
            Helper.wtf("CustomEventInterstitialListener cannot be null.");
            return;
        }

        if (!(context instanceof Activity)) {
            Helper.wtf("Ad can be rendered only in Activity context.");
            listener.onInterstitialFailed(ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        if (!validateExtras(serverExtras)) {
            Helper.wtf("Failed interstitial ad fetch: Missing required server extras" +
                    " [FLURRY_APIKEY and/or FLURRY_ADSPACE].");
            listener.onInterstitialFailed(ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        mContext = context;
        mListener = listener;

        String apiKey = serverExtras.get(FlurryAgentWrapper.PARAM_API_KEY);
        mAdSpaceName = serverExtras.get(FlurryAgentWrapper.PARAM_AD_SPACE_NAME_REWARD);

        FlurryAgentWrapper.getInstance().startSession(context, apiKey, null);

        Helper.wtf( "Fetching Flurry ad, ad unit name:" + mAdSpaceName);
        mInterstitial = new FlurryAdInterstitial(mContext, mAdSpaceName);
        mInterstitial.setListener(this);
        mInterstitial.fetchAd();
    }

    @Override
    protected void onInvalidate() {
        if (mContext == null) {
            return;
        }

        Helper.wtf("MoPub issued onInvalidate (" + mAdSpaceName + ")");

        if (mInterstitial != null) {
            mInterstitial.destroy();
            mInterstitial = null;
        }

        FlurryAgentWrapper.getInstance().endSession(mContext);

        mContext = null;
        mListener = null;
    }

    @Override
    protected MoPubInterstitial.AdType getAdType() {
        return MoPubInterstitial.AdType.FLURRY_INTERSTITIAL;
    }

    @Override
    public void showInterstitial() {
        Helper.wtf( "MoPub issued showRewarded (" + mAdSpaceName + ")");

        if (mInterstitial != null) {
            mInterstitial.displayAd();
        }
    }

    @Override
    protected boolean usesProxy() {
        return false;
    }

    private boolean validateExtras(final Map<String, String> serverExtras) {
        if (serverExtras == null) {
            return false;
        }

        final String flurryApiKey = serverExtras.get(FlurryAgentWrapper.PARAM_API_KEY);
        final String flurryAdSpace = serverExtras.get(FlurryAgentWrapper.PARAM_AD_SPACE_NAME_REWARD);
        Helper.wtf( "ServerInfo fetched from Mopub " + FlurryAgentWrapper.PARAM_API_KEY + " : "
                + flurryApiKey + " and " + FlurryAgentWrapper.PARAM_AD_SPACE_NAME_REWARD + " :" +
                flurryAdSpace);

        return (!TextUtils.isEmpty(flurryApiKey) && !TextUtils.isEmpty(flurryAdSpace));
    }

    @Override
    public void onFetched(FlurryAdInterstitial adInterstitial) {
        Helper.wtf( "onFetched: Flurry Rewarded ad fetched successfully!");

        if (mListener != null) {
            mListener.onInterstitialLoaded();
        }
    }

    @Override
    public void onRendered(FlurryAdInterstitial adInterstitial) {
        Helper.wtf( "onRendered: Flurry Rewarded ad rendered");

        if (mListener != null) {
            mListener.onInterstitialShown();
        }
    }

    @Override
    public void onDisplay(FlurryAdInterstitial adInterstitial) {
        Helper.wtf( "onDisplay: Flurry Rewarded ad displayed");

        // no-op
    }

    @Override
    public void onClose(FlurryAdInterstitial adInterstitial) {
        Helper.wtf( "onClose: Flurry Rewarded ad closed");

        if (mListener != null) {
            mListener.onInterstitialDismissed();
        }
    }

    @Override
    public void onAppExit(FlurryAdInterstitial adInterstitial) {
        Helper.wtf( "onAppExit: Flurry Rewarded ad exited app");
    }

    @Override
    public void onClicked(FlurryAdInterstitial adInterstitial) {
        Helper.wtf( "onClicked: Flurry Rewarded ad clicked");

        if (mListener != null) {
            mListener.onInterstitialClicked();
        }
    }

    @Override
    public void onVideoCompleted(FlurryAdInterstitial adInterstitial) {
        Helper.wtf( "onVideoCompleted: Flurry Rewarded ad video completed");

        // no-op
    }

    @Override
    public void onError(FlurryAdInterstitial adInterstitial, FlurryAdErrorType adErrorType,
                        int errorCode) {
        Helper.wtf( String.format("onError: Flurry Rewarded ad not available. " +
                "Error type: %s. Error code: %s", adErrorType.toString(), errorCode));

        if (mListener != null) {
            switch (adErrorType) {
                case FETCH:
                    mListener.onInterstitialFailed(NETWORK_NO_FILL);
                    return;
                case RENDER:
                    mListener.onInterstitialFailed(NETWORK_INVALID_STATE);
                    return;
                case CLICK:
                    // Don't call onInterstitialFailed in this case.
                    return;
                default:
                    mListener.onInterstitialFailed(UNSPECIFIED);
            }
        }
    }
}
