package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.mopub.common.AdFormat;
import com.mopub.common.DataKeys;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.factories.CustomEventInterstitialAdapterFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_NOT_FOUND;

public class MoPubInterstitial implements CustomEventInterstitialAdapter.CustomEventInterstitialAdapterListener {

    public static boolean HAS_LOCATION = true;

    //http://www.nationsonline.org/oneworld/country_code_list.htm
    @Nullable public String getCountryCode() {
        return mCountryCode;
    }

    public String getCity() {
        return mCity;
    }

    private enum InterstitialState {
        CUSTOM_EVENT_AD_READY,
        NOT_READY;

        boolean isReady() {
            return this != InterstitialState.NOT_READY;
        }
    }

    private MoPubInterstitialView mInterstitialView;
    private CustomEventInterstitialAdapter mCustomEventInterstitialAdapter;
    private InterstitialAdListener mInterstitialAdListener;
    private Activity mActivity;
    private String mAdUnitId;
    private InterstitialState mCurrentInterstitialState;
    private boolean mIsDestroyed;
    private String mCountryCode;
    private String mCity;

    public interface InterstitialAdListener {
        public void onInterstitialLoaded(MoPubInterstitial interstitial);
        public void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode);
        public void onInterstitialShown(MoPubInterstitial interstitial);
        public void onInterstitialClicked(MoPubInterstitial interstitial);
        public void onInterstitialDismissed(MoPubInterstitial interstitial);
    }

    public MoPubInterstitial(Activity activity, String id) {
        mActivity = activity;
        mAdUnitId = id;

        mInterstitialView = new MoPubInterstitialView(mActivity);
        mInterstitialView.setAdUnitId(mAdUnitId);

        mCurrentInterstitialState = InterstitialState.NOT_READY;

    }

    public void load() {
        resetCurrentInterstitial();
        mInterstitialView.loadAd();
    }

    public void forceRefresh() {
        resetCurrentInterstitial();
        mInterstitialView.forceRefresh();
    }

    private void resetCurrentInterstitial() {
        mCurrentInterstitialState = InterstitialState.NOT_READY;

        if (mCustomEventInterstitialAdapter != null) {
            mCustomEventInterstitialAdapter.invalidate();
            mCustomEventInterstitialAdapter = null;
        }

        mIsDestroyed = false;
    }

    public boolean isReady() {
        return mCurrentInterstitialState.isReady();
    }

    boolean isDestroyed() {
        return mIsDestroyed;
    }

    public boolean show() {
        switch (mCurrentInterstitialState) {
            case CUSTOM_EVENT_AD_READY:
                showCustomEventInterstitial();
                return true;
        }
        return false;
    }

    private void showCustomEventInterstitial() {
        if (mCustomEventInterstitialAdapter != null) mCustomEventInterstitialAdapter.showInterstitial();
    }

    Integer getAdTimeoutDelay() {
        return mInterstitialView.getAdTimeoutDelay();
    }

    MoPubInterstitialView getMoPubInterstitialView() {
        return mInterstitialView;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void setKeywords(String keywords) {
        mInterstitialView.setKeywords(keywords);
    }

    public String getKeywords() {
        return mInterstitialView.getKeywords();
    }

    public Activity getActivity() {
        return mActivity;
    }

    public Location getLocation() {
        return mInterstitialView.getLocation();
    }

    public void destroy() {
        mIsDestroyed = true;

        if (mCustomEventInterstitialAdapter != null) {
            mCustomEventInterstitialAdapter.invalidate();
            mCustomEventInterstitialAdapter = null;
        }

        mInterstitialView.setBannerAdListener(null);
        mInterstitialView.destroy();
    }

    public void setInterstitialAdListener(InterstitialAdListener listener) {
        mInterstitialAdListener = listener;
    }

    public InterstitialAdListener getInterstitialAdListener() {
        return mInterstitialAdListener;
    }

    public void setTesting(boolean testing) {
        mInterstitialView.setTesting(testing);
    }

    public boolean getTesting() {
        return mInterstitialView.getTesting();
    }

    public void setLocalExtras(Map<String, Object> extras) {
        mInterstitialView.setLocalExtras(extras);
    }

    public Map<String, Object> getLocalExtras() {
        return mInterstitialView.getLocalExtras();
    }

    /*
     * Implements CustomEventInterstitialAdapter.CustomEventInterstitialListener
     */

    @Override
    public void onCustomEventInterstitialLoaded() {
        if (mIsDestroyed) return;

        mCurrentInterstitialState = InterstitialState.CUSTOM_EVENT_AD_READY;

        if (mInterstitialAdListener != null) {
            mInterstitialAdListener.onInterstitialLoaded(this);
        }
    }

    @Override
    public void onCustomEventInterstitialFailed(MoPubErrorCode errorCode) {
        if (isDestroyed()) return;

        mCurrentInterstitialState = InterstitialState.NOT_READY;
        mInterstitialView.loadFailUrl(errorCode);
    }

    @Override
    public void onCustomEventInterstitialShown() {
        if (isDestroyed()) return;

        mInterstitialView.trackImpression();

        if (mInterstitialAdListener != null) {
            mInterstitialAdListener.onInterstitialShown(this);
        }
    }

    @Override
    public void onCustomEventInterstitialClicked() {
        if (isDestroyed()) return;

        mInterstitialView.registerClick();

        if (mInterstitialAdListener != null) {
            mInterstitialAdListener.onInterstitialClicked(this);
        }
    }

    @Override
    public void onCustomEventInterstitialDismissed() {
        if (isDestroyed()) return;

        mCurrentInterstitialState = InterstitialState.NOT_READY;

        if (mInterstitialAdListener != null) {
            mInterstitialAdListener.onInterstitialDismissed(this);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public class MoPubInterstitialView extends MoPubView {

        public MoPubInterstitialView(Context context) {
            super(context);
            setAutorefreshEnabled(false);
        }

        @Override
        public AdFormat getAdFormat() {
            return AdFormat.INTERSTITIAL;
        }

        @Override
        protected void loadCustomEvent(String customEventClassName, Map<String, String> serverExtras) {
            if (mAdViewController == null) {
                return;
            }

            extractCountryFromExtras(serverExtras);

            if (TextUtils.isEmpty(customEventClassName)) {
                MoPubLog.d("Couldn't invoke custom event because the server did not specify one.");
                loadFailUrl(ADAPTER_NOT_FOUND);
                return;
            }

            if (mCustomEventInterstitialAdapter != null) {
                mCustomEventInterstitialAdapter.invalidate();
            }

            MoPubLog.d("Loading custom event interstitial adapter.");

            mCustomEventInterstitialAdapter = CustomEventInterstitialAdapterFactory.create(
                    MoPubInterstitial.this,
                    customEventClassName,
                    serverExtras,
                    mAdViewController.getBroadcastIdentifier(),
                    mAdViewController.getAdReport());
            mCustomEventInterstitialAdapter.setAdapterListener(MoPubInterstitial.this);
            mCustomEventInterstitialAdapter.loadInterstitial();
        }

        protected void trackImpression() {
            MoPubLog.d("Tracking impression for interstitial.");
            if (mAdViewController != null) mAdViewController.trackImpression();
        }

        @Override
        protected void adFailed(MoPubErrorCode errorCode) {
            if (mInterstitialAdListener != null) {
                mInterstitialAdListener.onInterstitialFailed(MoPubInterstitial.this, errorCode);
            }
        }
    }

    @VisibleForTesting
    Map<String, String> extractCountryFromExtras(Map<String, String> serverExtras) {
        Preconditions.checkNotNull(serverExtras);
        if(serverExtras.containsKey(DataKeys.CLICKTHROUGH_URL_KEY)){
            String url = serverExtras.get(DataKeys.CLICKTHROUGH_URL_KEY);
            Pattern p = Pattern.compile("(?<=&country_code=).*?(?=&)");
            Matcher m = p.matcher(url);
            if(m.find() && mCountryCode == null){
                mCountryCode = m.group();
            }else {
                HAS_LOCATION = false;
            }
            //else {
                //Pattern p2 = Pattern.compile("(?<=&cid=).*?(?=&)");
                //Matcher m2 = p2.matcher(url);
                //if(m2.find()){
                //    url = m2.replaceAll(m2.group()+"&city=SanFrancisco&ckv=2&country_code=US");
                //    serverExtras.remove(DataKeys.CLICKTHROUGH_URL_KEY);
                //    serverExtras.put(DataKeys.CLICKTHROUGH_URL_KEY, url);
                //}
            //}
        }
        return serverExtras;
    }

    @VisibleForTesting
    void extractCityFromExtras(Map<String, String> serverExtras) {
        Preconditions.checkNotNull(serverExtras);
        if(serverExtras.containsKey(DataKeys.CLICKTHROUGH_URL_KEY)){
            String url = serverExtras.get(DataKeys.CLICKTHROUGH_URL_KEY);
            Pattern p = Pattern.compile("(?<=&city=).*?(?=&)");
            Matcher m = p.matcher(url);
            if(m.find()){
                mCity = m.group();
            }
        }
    }

    @VisibleForTesting
    @Deprecated
    void setInterstitialView(MoPubInterstitialView interstitialView) {
        mInterstitialView = interstitialView;
    }
}
