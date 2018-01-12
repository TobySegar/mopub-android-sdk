package com.mopub.ads.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;
import com.smaato.soma.CrashReportTemplate;
import com.smaato.soma.debug.DebugCategory;
import com.smaato.soma.debug.Debugger;
import com.smaato.soma.debug.LogMessage;
import com.smaato.soma.interstitial.Interstitial;
import com.smaato.soma.interstitial.InterstitialAdListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Example of MoPub Smaato Interstitial mediation adapter.
 *
 * Didnt get ad fur no fill
 * @author Chouaieb Nabil
 *         Updated to support latest MoPub SDK 4.7.0
 *         Common for all Publishers
 */
public class SomaMopubAdapterInterstitial extends CustomEventInterstitial implements InterstitialAdListener {
    private static String TAG = "###SmaatoInt";

    private Interstitial interstitial;
    private CustomEventInterstitialListener customEventInterstitialListener = null;
    private Handler mHandler;

    @Override
    protected void loadInterstitial(Context context,
                                    final CustomEventInterstitialListener customEventInterstitialListener, Map<String, Object> localExtras,
                                    Map<String, String> serverExtras) {

        serverExtras = new HashMap<>();
        serverExtras.put("publisherId","1100013377");
        serverExtras.put("adSpaceId","130054389");
        final Map<String, String> finalServerExtras = serverExtras;

        // Enable to get full logs
        Debugger.setDebugMode(Debugger.Level_1);
        Debugger.enableCrashReporting(false);

        mHandler = new Handler(Looper.getMainLooper());
        this.customEventInterstitialListener = customEventInterstitialListener;
        if (interstitial == null) {
            interstitial = new Interstitial((Activity) context);
            interstitial.setInterstitialAdListener(this);
        }


        new CrashReportTemplate<Void>() {
            @Override
            public Void process() throws Exception {
                int publisherId = Integer.parseInt((String) finalServerExtras.get("publisherId"));
                int adSpaceId = Integer.parseInt((String) finalServerExtras.get("adSpaceId"));
                interstitial.getAdSettings().setPublisherId(publisherId);
                interstitial.getAdSettings().setAdspaceId(adSpaceId);

                printDebugLogs("loading SmaatoInter",DebugCategory.ERROR);

                interstitial.asyncLoadNewBanner();

                return null;
            }
        }.execute();

    }

    @Override
    protected void onInvalidate() {
        if(interstitial != null){
            interstitial.destroy();
            interstitial = null;
        }
    }

    @Override
    protected MoPubInterstitial.AdType getAdType() {
        return MoPubInterstitial.AdType.SMATO_INTERSTITIAL;
    }

    @Override
    public void showInterstitial() {
        new CrashReportTemplate<Void>() {
            @Override
            public Void process() throws Exception {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (interstitial.isInterstitialReady()) {
                            printDebugLogs("isInterstitialReady = true",DebugCategory.ERROR);
                            interstitial.show();
                        }else{
                            customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NO_FILL);
                        }
                    }
                });

                return null;
            }
        }.execute();


    }

    @Override
    protected boolean usesProxy() {
        return false;
    }

    @Override
    public void onFailedToLoadAd() {
        new CrashReportTemplate<Void>() {
            @Override
            public Void process() throws Exception {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        printDebugLogs("onFailedToLoadAd = true",DebugCategory.ERROR);
                        customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NO_FILL);
                    }
                });

                return null;
            }
        }.execute();


    }

    @Override
    public void onReadyToShow() {
        new CrashReportTemplate<Void>() {
            @Override
            public Void process() throws Exception {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        printDebugLogs("onReadyToShow ",DebugCategory.ERROR);
                        customEventInterstitialListener.onInterstitialLoaded();
                    }
                });

                return null;
            }
        }.execute();


    }

    @Override
    public void onWillClose() {
        new CrashReportTemplate<Void>() {
            @Override
            public Void process() throws Exception {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        printDebugLogs("onWillClose ",DebugCategory.ERROR);
                        customEventInterstitialListener.onInterstitialDismissed();
                    }
                });
                return null;
            }
        }.execute();

    }

    @Override
    public void onWillOpenLandingPage() {
        new CrashReportTemplate<Void>() {
            @Override
            public Void process() throws Exception {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        printDebugLogs("onWillOpenLandingPage ",DebugCategory.ERROR);
                        customEventInterstitialListener.onInterstitialClicked();
                    }
                });

                return null;
            }
        }.execute();

    }

    @Override
    public void onWillShow() {
        new CrashReportTemplate<Void>() {
            @Override
            public Void process() throws Exception {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        printDebugLogs("onWillShow ", DebugCategory.ERROR);
                        customEventInterstitialListener.onInterstitialShown();
                    }
                });

                return null;
            }
        }.execute();

    }

    public void printDebugLogs(String str, DebugCategory debugCategory) {
        Debugger.showLog(new LogMessage(TAG,
                str,
                Debugger.Level_1,
                debugCategory));
    }

}