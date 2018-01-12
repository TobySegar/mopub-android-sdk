package com.mopub.ads.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyZone;
import com.mopub.common.util.Json;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;

import java.util.Arrays;
import java.util.Map;

/**
 * Please reference the Supported Mediation Partner page at http://bit.ly/2mqsuFH for the latest version and ad format certifications.
 */
public class AdColonyInterstitial extends CustomEventInterstitial {
    private static final String TAG = "AdColonyInterstitial";
    /*
     * We recommend passing the AdColony client options, app ID, all zone IDs, and current zone ID
     * in the serverExtras Map by specifying Custom Event Data in MoPub's web interface.
     *
     * Please see AdColony's documentation for more information:
     * https://github.com/AdColony/AdColony-Android-SDK-3
     */
    private static final String DEFAULT_CLIENT_OPTIONS = "version=2.0.0,store:google";
    private static final String DEFAULT_APP_ID = "appef2a58e902804bcbb9";
    //private static final String[] DEFAULT_ALL_ZONE_IDS = {"vzc83fa8b4362244f3b0"};
    private static final String DEFAULT_ZONE_ID = "vzc83fa8b4362244f3b0";

    /*
     * These keys are intended for MoPub internal use. Do not modify.
     */
    //public static final String CLIENT_OPTIONS_KEY = "clientOptions";
    public static final String APP_ID_KEY = "appId";
    public static final String ALL_ZONE_IDS_KEY = "allZoneIds";
    public static final String ZONE_ID_KEY = "zoneId";

    private CustomEventInterstitialListener mCustomEventInterstitialListener;
    private AdColonyInterstitialListener mAdColonyInterstitialListener;
    private final Handler mHandler;
    private com.adcolony.sdk.AdColonyInterstitial mAdColonyInterstitial;
    //private static String[] previousAdColonyAllZoneIds;

    public AdColonyInterstitial() {
        mHandler = new Handler();
    }

    @Override
    protected void loadInterstitial(@NonNull Context context,
            @NonNull CustomEventInterstitialListener customEventInterstitialListener,
            @Nullable Map<String, Object> localExtras,
            @NonNull Map<String, String> serverExtras) {
        if (context == null
                || !(context instanceof Activity)
                || customEventInterstitialListener == null
                || serverExtras == null) {
            customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        String clientOptions = DEFAULT_CLIENT_OPTIONS;
        String appId = DEFAULT_APP_ID;
        //String[] allZoneIds = DEFAULT_ALL_ZONE_IDS;
        String zoneId = DEFAULT_ZONE_ID;

        mCustomEventInterstitialListener = customEventInterstitialListener;

        serverExtras.clear();
        serverExtras.put(APP_ID_KEY,"appef2a58e902804bcbb9");
        serverExtras.put(ZONE_ID_KEY,"vzc83fa8b4362244f3b0");

        if (extrasAreValid(serverExtras)) {
            appId = serverExtras.get(APP_ID_KEY);
            zoneId = serverExtras.get(ZONE_ID_KEY);
        }
        //AdColonyAppOptions adColonyAppOptions = AdColonyAppOptions.getMoPubAppOptions(DEFAULT_CLIENT_OPTIONS);
        mAdColonyInterstitialListener = getAdColonyInterstitialListener();
        if (!isAdColonyConfigured()) {
            AdColony.configure((Activity) context, appId, zoneId);
        }
//        else if ((shouldReconfigure(previousAdColonyAllZoneIds, allZoneIds))) {
//            // Need to check the zone IDs sent from the MoPub portal and reconfigure if they are
//            // different than the zones we initially called AdColony.configure() with
//            AdColony.configure((Activity) context, appId, allZoneIds);
//            previousAdColonyAllZoneIds = allZoneIds;
//        }

        AdColony.requestInterstitial(zoneId, mAdColonyInterstitialListener);
    }

    @Override
    public void showInterstitial() {
        if (mAdColonyInterstitial == null || mAdColonyInterstitial.isExpired()) {
            Log.e(TAG, "AdColony interstitial ad is null or has expired");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
                }
            });
        } else {
            mAdColonyInterstitial.show();
        }
    }

    @Override
    protected boolean usesProxy() {
        return false;
    }

    @Override
    protected void onInvalidate() {
        if (mAdColonyInterstitial != null) {
            mAdColonyInterstitialListener = null;
            mAdColonyInterstitial.setListener(null);
            mAdColonyInterstitial.destroy();
            mAdColonyInterstitial = null;
        }
    }

    @Override
    protected MoPubInterstitial.AdType getAdType() {
        return MoPubInterstitial.AdType.ADCOLONY_INTERSTITIAL;
    }

    private boolean isAdColonyConfigured() {
        return !AdColony.getSDKVersion().isEmpty();
    }

    private AdColonyInterstitialListener getAdColonyInterstitialListener() {
        if (mAdColonyInterstitialListener != null) {
            return mAdColonyInterstitialListener;
        } else {
            return new AdColonyInterstitialListener() {
                @Override
                public void onRequestFilled(@NonNull com.adcolony.sdk.AdColonyInterstitial adColonyInterstitial) {
                    mAdColonyInterstitial = adColonyInterstitial;
                    Log.d(TAG, "AdColony interstitial ad has been successfully loaded.");
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCustomEventInterstitialListener.onInterstitialLoaded();
                        }
                    });
                }

                @Override
                public void onRequestNotFilled(@NonNull AdColonyZone zone) {
                    Log.d(TAG, "AdColony interstitial ad has no fill.");
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
                        }
                    });
                }

                @Override
                public void onClosed(@NonNull com.adcolony.sdk.AdColonyInterstitial ad) {
                    Log.d(TAG, "AdColony interstitial ad has been dismissed.");
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCustomEventInterstitialListener.onInterstitialDismissed();
                        }
                    });
                }

                @Override
                public void onOpened(@NonNull com.adcolony.sdk.AdColonyInterstitial ad) {
                    Log.d(TAG, "AdColony interstitial ad shown: " + ad.getZoneID());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCustomEventInterstitialListener.onInterstitialShown();
                        }
                    });
                }

                @Override
                public void onExpiring(@NonNull com.adcolony.sdk.AdColonyInterstitial ad) {
                    Log.d(TAG, "AdColony interstitial ad is expiring; requesting new ad");
                    AdColony.requestInterstitial(ad.getZoneID(), mAdColonyInterstitialListener);
                }

                @Override
                public void onClicked(@NonNull com.adcolony.sdk.AdColonyInterstitial ad) {
                    mCustomEventInterstitialListener.onInterstitialClicked();
                }
            };
        }
    }

    private boolean extrasAreValid(Map<String, String> extras) {
        return extras != null
                && extras.containsKey(APP_ID_KEY)
                && extras.containsKey(ZONE_ID_KEY);
    }

    private static boolean shouldReconfigure(String[] previousZones, String[] newZones) {
        // If AdColony is configured already, but previousZones is null, then that means AdColony
        // was configured with the AdColonyRewardedVideo adapter so attempt to configure with
        // the ids in newZones. They will be ignored within the AdColony SDK if the zones are
        // the same as the zones that the other adapter called AdColony.configure() with.
        if (previousZones == null) {
            return true;
        } else if (newZones == null) {
            return false;
        } else if (previousZones.length != newZones.length) {
            return true;
        }
        Arrays.sort(previousZones);
        Arrays.sort(newZones);
        return !Arrays.equals(previousZones, newZones);
    }

    private String[] extractAllZoneIds(Map<String, String> serverExtras) {
        String[] result = Json.jsonArrayToStringArray(serverExtras.get(ALL_ZONE_IDS_KEY));

        // AdColony requires at least one valid String in the allZoneIds array.
        if (result.length == 0) {
            result = new String[]{""};
        }

        return result;
    }
}
