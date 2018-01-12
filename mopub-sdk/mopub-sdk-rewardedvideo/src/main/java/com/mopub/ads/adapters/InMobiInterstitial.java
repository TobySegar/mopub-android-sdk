//package com.mopub.ads.adapters;
//
//import android.app.Activity;
//import android.content.Context;
//import android.util.Log;
//
//import com.inmobi.ads.InMobiAdRequestStatus;
//import com.inmobi.ads.InMobiAdRequestStatus.StatusCode;
//import com.inmobi.sdk.InMobiSdk;
//import com.mopub.common.MoPub;
//import com.mopub.mobileads.CustomEventInterstitial;
//import com.mopub.mobileads.MoPubErrorCode;
//import com.mopub.mobileads.MoPubInterstitial;
//
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.Map;
//
//public class InMobiInterstitial extends CustomEventInterstitial implements com.inmobi.ads.InMobiInterstitial.InterstitialAdListener2 {
//    private CustomEventInterstitialListener mInterstitialListener;
//    private String accountId = "";
//    private long placementId = -1;
//    private com.inmobi.ads.InMobiInterstitial iMInterstitial;
//    private static boolean isAppIntialize = false;
//    private static final String TAG = InMobiInterstitial.class.getSimpleName();
////App ID: 1516858738701
////Placement ID:  1514612691011 | Site ID:  a66a6ec2f64242409b4e884a237b5470 Account Id 9af2be5d22a74b35b60b4a08dfe8f706
//    @Override
//    protected void loadInterstitial(Context context, CustomEventInterstitialListener interstitialListener,
//                                    Map<String, Object> localExtras, Map<String, String> serverExtras) {
//
//        mInterstitialListener = interstitialListener;
//serverExtras.clear();
//serverExtras.put("accountid","9af2be5d22a74b35b60b4a08dfe8f706");
//        serverExtras.put("placementid","1514612691011");
//
//        Activity activity;
//        if (context != null && context instanceof Activity) {
//            activity = (Activity) context;
//        } else {
//            Log.w(TAG, "Context not an Activity. Returning error!");
//            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NO_FILL);
//            return;
//        }
//
//
//        try {
//            accountId = serverExtras.get("accountid");
//            placementId = Long.parseLong(serverExtras.get("placementid"));
//        } catch (Exception e1) {
//            e1.printStackTrace();
//        }
//
//        if (!isAppIntialize) {
//            InMobiSdk.init(activity, accountId);
//            isAppIntialize = true;
//        }
//
//		/*
//		 * You may also pass the Placement ID by
//		 * specifying Custom Event Data in MoPub's web interface.
//		 */
//
//
//        iMInterstitial = new com.inmobi.ads.InMobiInterstitial(activity, placementId, this);
//        iMInterstitial.setInterstitialAdListener(this);
//		/*
//		Sample for setting up the InMobi SDK Demographic params.
//        Publisher need to set the values of params as they want.
//
//		InMobiSdk.setAreaCode("areacode");
//		InMobiSdk.setEducation(Education.HIGH_SCHOOL_OR_LESS);
//		InMobiSdk.setGender(Gender.MALE);
//		InMobiSdk.setIncome(1000);
//		InMobiSdk.setAge(23);
//		InMobiSdk.setPostalCode("postalcode");
//		InMobiSdk.setLogLevel(LogLevel.DEBUG);
//		InMobiSdk.setLocationWithCityStateCountry("blore", "kar", "india");
//		InMobiSdk.setLanguage("ENG");
//		InMobiSdk.setInterests("dance");
//		InMobiSdk.setEthnicity(Ethnicity.ASIAN);
//		InMobiSdk.setYearOfBirth(1980);*/
//        Map<String, String> map = new HashMap<>();
//        map.put("tp", "c_mopub");
//        map.put("tp-ver", MoPub.SDK_VERSION);
//        iMInterstitial.setExtras(map);
//        iMInterstitial.load();
//    }
//
//	/*
//	 * Abstract methods from CustomEventInterstitial
//	 */
//
//    @Override
//    public void showInterstitial() {
//        if (iMInterstitial != null
//                && iMInterstitial.isReady()) {
//            iMInterstitial.show();
//        }
//    }
//
//    @Override
//    protected boolean usesProxy() {
//        return false;
//    }
//
//    @Override
//    public void onInvalidate() {
//    }
//
//    @Override
//    protected MoPubInterstitial.AdType getAdType() {
//        return MoPubInterstitial.AdType.INMOBI_INTERSTITIAL;
//    }
//
//    @Override
//    public void onAdDismissed(com.inmobi.ads.InMobiInterstitial ad) {
//        Log.d(TAG, "InMobi interstitial ad dismissed.");
//        if (mInterstitialListener != null) {
//            mInterstitialListener.onInterstitialDismissed();
//        }
//    }
//
//    @Override
//    public void onAdDisplayed(com.inmobi.ads.InMobiInterstitial ad) {
//        Log.d(TAG, "InMobi interstitial show on screen.");
//        if (mInterstitialListener != null) {
//            mInterstitialListener.onInterstitialShown();
//        }
//    }
//
//    @Override
//    public void onAdLoadFailed(com.inmobi.ads.InMobiInterstitial ad,
//                               InMobiAdRequestStatus status) {
//        Log.d(TAG, "InMobi interstitial ad failed to load.");
//        if (mInterstitialListener != null) {
//
//            if (status.getStatusCode() == StatusCode.INTERNAL_ERROR) {
//                mInterstitialListener
//                        .onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
//            } else if (status.getStatusCode() == StatusCode.REQUEST_INVALID) {
//                mInterstitialListener
//                        .onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
//            } else if (status.getStatusCode() == StatusCode.NETWORK_UNREACHABLE) {
//                mInterstitialListener
//                        .onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
//            } else if (status.getStatusCode() == StatusCode.NO_FILL) {
//                mInterstitialListener
//                        .onInterstitialFailed(MoPubErrorCode.NO_FILL);
//            } else if (status.getStatusCode() == StatusCode.REQUEST_TIMED_OUT) {
//                mInterstitialListener
//                        .onInterstitialFailed(MoPubErrorCode.NETWORK_TIMEOUT);
//            } else if (status.getStatusCode() == StatusCode.SERVER_ERROR) {
//                mInterstitialListener
//                        .onInterstitialFailed(MoPubErrorCode.SERVER_ERROR);
//            } else {
//                mInterstitialListener
//                        .onInterstitialFailed(MoPubErrorCode.UNSPECIFIED);
//            }
//        }
//
//    }
//
//    @Override
//    public void onAdReceived(com.inmobi.ads.InMobiInterstitial ad) {
//        Log.d(TAG, "InMobi Adserver responded with an Ad");
//    }
//
//    @Override
//    public void onAdLoadSucceeded(com.inmobi.ads.InMobiInterstitial ad) {
//        Log.d(TAG, "InMobi interstitial ad loaded successfully.");
//        if (mInterstitialListener != null) {
//            mInterstitialListener.onInterstitialLoaded();
//        }
//    }
//
//    @Override
//    public void onAdRewardActionCompleted(com.inmobi.ads.InMobiInterstitial ad,
//                                          Map<Object, Object> rewards) {
//        Log.d(TAG, "InMobi interstitial onRewardActionCompleted.");
//
//        if (null != rewards) {
//            Iterator<Object> iterator = rewards.keySet().iterator();
//            while (iterator.hasNext()) {
//                String key = iterator.next().toString();
//                String value = rewards.get(key).toString();
//                Log.d("Rewards: ", key + ":" + value);
//            }
//        }
//    }
//
//    @Override
//    public void onAdDisplayFailed(com.inmobi.ads.InMobiInterstitial ad) {
//        Log.d(TAG, "Interstitial ad failed to display.");
//    }
//
//    @Override
//    public void onAdWillDisplay(com.inmobi.ads.InMobiInterstitial ad) {
//        Log.d(TAG, "Interstitial ad will display.");
//    }
//
//    @Override
//    public void onUserLeftApplication(com.inmobi.ads.InMobiInterstitial ad) {
//        Log.d(TAG, "InMobi interstitial ad leaving application.");
//        mInterstitialListener.onLeaveApplication();
//    }
//
//    @Override
//    public void onAdInteraction(com.inmobi.ads.InMobiInterstitial ad,
//                                Map<Object, Object> params) {
//        Log.d(TAG, "InMobi interstitial interaction happening.");
//        if (mInterstitialListener != null) {
//            mInterstitialListener.onInterstitialClicked();
//        }
//    }
//}
