package com.mopub.mobileads;

import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.mojang.base.Helper;
import com.mojang.base.json.Data;
import com.mopub.common.AdReport;
import com.mopub.common.AdType;
import com.mopub.common.ClientMetadata;
import com.mopub.common.Constants;
import com.mopub.common.DataKeys;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.DeviceUtils;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Utils;
import com.mopub.mraid.MraidNativeCommandHandler;
import com.mopub.network.AdRequest;
import com.mopub.network.AdResponse;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.Networking;
import com.mopub.network.TrackingRequest;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.RequestQueue;
import com.mopub.volley.VolleyError;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;

public class AdViewController {
    static final int DEFAULT_REFRESH_TIME_MILLISECONDS = 60000;  // 1 minute
    static final int MAX_REFRESH_TIME_MILLISECONDS = 600000; // 10 minutes
    static final double BACKOFF_FACTOR = 1.5;
    private static final FrameLayout.LayoutParams WRAP_AND_CENTER_LAYOUT_PARAMS =
            new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER);
    private final static WeakHashMap<View,Boolean> sViewShouldHonorServerDimensions = new WeakHashMap<View, Boolean>();

    private final long mBroadcastIdentifier;

    @Nullable private Context mContext;
    @Nullable private MoPubView mMoPubView;
    @Nullable private WebViewAdUrlGenerator mUrlGenerator;

    @Nullable private AdResponse mAdResponse;
    @Nullable private String mCustomEventClassName;
    private final Runnable mRefreshRunnable;
    @NonNull private final AdRequest.Listener mAdListener;

    private boolean mIsDestroyed;
    private Handler mHandler;
    private boolean mIsLoading;
    private String mUrl;
    private boolean mExpanded;

    // This is the power of the exponential term in the exponential backoff calculation.
    @VisibleForTesting
    int mBackoffPower = 1;

    private Map<String, Object> mLocalExtras = new HashMap<String, Object>();

    /**
     * This is the current auto refresh status. If this is true, then ads will attempt to refresh.
     * If mRefreshTimeMillis is null or not greater than 0, the auto refresh runnable will not
     * be called.
     */
    private boolean mCurrentAutoRefreshStatus = true;

    /**
     * This is the publisher-specified auto refresh flag. AdViewController will only attempt to
     * refresh ads when this is true. Setting this to false will block refreshing.
     */
    private boolean mShouldAllowAutoRefresh = true;

    private String mKeywords;
    private String mUserDataKeywords;
    private Location mLocation;
    private boolean mIsTesting;
    private boolean mAdWasLoaded;
    @Nullable private String mAdUnitId;
    private int mTimeoutMilliseconds;
    @Nullable private AdRequest mActiveRequest;
    @Nullable private Integer mRefreshTimeMillis;
    public Boolean wasFailoverApplovin;

    public static void setShouldHonorServerDimensions(View view) {
        sViewShouldHonorServerDimensions.put(view, true);
    }

    private static boolean getShouldHonorServerDimensions(View view) {
        return sViewShouldHonorServerDimensions.get(view) != null;
    }

    public AdViewController(@NonNull Context context, @NonNull MoPubView view) {
        mContext = context;
        mMoPubView = view;

        // Timeout value of less than 0 means use the ad format's default timeout
        mTimeoutMilliseconds = -1;
        mBroadcastIdentifier = Utils.generateUniqueId();

        mUrlGenerator = new WebViewAdUrlGenerator(mContext.getApplicationContext(),
                MraidNativeCommandHandler.isStorePictureSupported(mContext));

        mAdListener = new AdRequest.Listener() {
            @Override
            public void onSuccess(final AdResponse response) {
                onAdLoadSuccess(response);
            }

            @Override
            public void onErrorResponse(final VolleyError volleyError) {
                onAdLoadError(volleyError);
            }
        };

        mRefreshRunnable = new Runnable() {
            public void run() {
                internalLoadAd();
            }
        };
        mRefreshTimeMillis = DEFAULT_REFRESH_TIME_MILLISECONDS;
        mHandler = new Handler();
    }

    @VisibleForTesting
    void onAdLoadSuccess(@NonNull final AdResponse adResponse) {
        mBackoffPower = 1;
        mAdResponse = changeResponseCustomClassPath(adResponse);


        String customEventClassName = mAdResponse.getCustomEventClassName();

        if (forceAdFromMopubServer(customEventClassName)) return;

        // Do other ad loading setup. See AdFetcher & AdLoadTask.
        mTimeoutMilliseconds = mAdResponse.getAdTimeoutMillis() == null
                ? mTimeoutMilliseconds
                : mAdResponse.getAdTimeoutMillis();
        mRefreshTimeMillis = mAdResponse.getRefreshTimeMillis();

        setNotLoading();

        loadCustomEvent(mMoPubView, customEventClassName,
                adResponse.getServerExtras());

        scheduleRefreshTimerIfEnabled();
    }

    private boolean forceAdFromMopubServer(String customEventClassName) {
        if(customEventClassName != null) {
            boolean isMopubAdd = customEventClassName.equals("com.mopub.mobileads.HtmlInterstitial") || customEventClassName.equals("com.mopub.mobileads.VastVideoInterstitial") || customEventClassName.equals("com.mopub.mraid.MraidInterstitial");
            boolean isUnityAd = customEventClassName.equals("com.mopub.ads.adapters.UnityAdsMopubEvents");
            boolean isHeyzapAdd = customEventClassName.equals("com.mopub.ads.adapters.HeyzapInterstitial");
            boolean isApplovinAd = customEventClassName.equals("com.mopub.ads.adapters.ApplovinInterstitial");
            boolean isAdmobAd = customEventClassName.equals("com.mopub.ads.adapters.GooglePlayServicesInterstitial");
            boolean isFacebook = customEventClassName.equals("com.mopub.ads.adapters.FacebookInterstitial");
            boolean isFyber = customEventClassName.equals("com.mopub.ads.adapters.FyberInterstitial");

            if(Helper.FORCE_ADMOB_ADD && !isAdmobAd){
                loadFailUrl(MoPubErrorCode.NETWORK_NO_FILL);
                return true;
            }

            if(Helper.FORCE_APPLOVIN_ADD && !isApplovinAd){
                loadFailUrl(MoPubErrorCode.NETWORK_NO_FILL);
                return true;
            }

            if(Helper.FORCE_FACEBOOK && !isFacebook){
                loadFailUrl(MoPubErrorCode.NETWORK_NO_FILL);
                return true;
            }

            if(Helper.FORCE_UNITY_ADD && !isUnityAd){
                loadFailUrl(MoPubErrorCode.NETWORK_NO_FILL);
                return true;
            }

            if(Helper.FORCE_HEYZAP_ADD && !isHeyzapAdd){
                loadFailUrl(MoPubErrorCode.NETWORK_NO_FILL);
                return true;
            }

            if(Helper.FORCE_FYBER_ADD && !isFyber){
                loadFailUrl(MoPubErrorCode.NETWORK_NO_FILL);
                return true;
            }

            if((isMopubAdd && !Data.Ads.Interstitial.mopubAllowed)){
                loadFailUrl(MoPubErrorCode.NETWORK_NO_FILL);
                return true;
            }
            if(Helper.FORCE_MOPUB_ADD && !isMopubAdd){
                loadFailUrl(MoPubErrorCode.NETWORK_NO_FILL);
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    void onAdLoadError(final VolleyError error) {
        if (error instanceof MoPubNetworkError) {
            // If provided, the MoPubNetworkError's refresh time takes precedence over the
            // previously set refresh time.
            // The only types of NetworkErrors that can possibly modify
            // an ad's refresh time are CLEAR requests. For CLEAR requests that (erroneously) omit a
            // refresh time header and for all other non-CLEAR types of NetworkErrors, we simply
            // maintain the previous refresh time value.
            final MoPubNetworkError moPubNetworkError = (MoPubNetworkError) error;
            if (moPubNetworkError.getRefreshTimeMillis() != null) {
                mRefreshTimeMillis = moPubNetworkError.getRefreshTimeMillis();
            }
        }

        final MoPubErrorCode errorCode = getErrorCodeFromVolleyError(error, mContext);

        if (errorCode == MoPubErrorCode.SERVER_ERROR || errorCode == MoPubErrorCode.NO_FILL) {
            mBackoffPower++;
            onAdLoadSuccess(getFailoverResponse());
            return;
        }

        setNotLoading();
        adDidFail(errorCode);
    }

    @VisibleForTesting
    AdResponse getFailoverResponse() {
        Map<String,String> serverExtras = new HashMap<>();
        serverExtras.put("adUnitID", Data.Ads.Interstitial.failoverId);

        boolean forceApplovin = wasFailoverApplovin != null && !wasFailoverApplovin;

        AdResponse failoverResponse = new AdResponse.Builder()
                .setCustomEventClassName("com.mopub.ads.adapters.GooglePlayServicesInterstitial")
                .setServerExtras(serverExtras)
                .setAdType(AdType.CUSTOM)
                .build();
            wasFailoverApplovin = false;
        if(Data.Ads.Interstitial.failoverApplovin || forceApplovin){
            failoverResponse = new AdResponse.Builder()
                    .setCustomEventClassName("com.mopub.ads.adapters.ApplovinInterstitial")
                    .setServerExtras(serverExtras)
                    .setAdType(AdType.CUSTOM)
                    .build();
            wasFailoverApplovin = true;
        }

        return failoverResponse;
    }

    @VisibleForTesting
    void loadCustomEvent(@Nullable final MoPubView moPubView,
            @Nullable final String customEventClassName,
            @NonNull final Map<String, String> serverExtras) {
        Preconditions.checkNotNull(serverExtras);

        if (moPubView == null) {
            MoPubLog.d("Can't load an ad in this ad view because it was destroyed.");
            return;
        }

        extractCountryFromExtras(serverExtras);

        moPubView.loadCustomEvent(customEventClassName, serverExtras);
    }

    private static String mCountryCode;
    public static boolean HAS_LOCATION = true;
    private void extractCountryFromExtras(Map<String, String> serverExtras) {
        if(serverExtras.containsKey(DataKeys.CLICKTHROUGH_URL_KEY) && mCountryCode == null){
            String url = serverExtras.get(DataKeys.CLICKTHROUGH_URL_KEY);
            Pattern p = Pattern.compile("(?<=&country_code=).*?(?=&)");
            Matcher m = p.matcher(url);
            if(m.find() && mCountryCode == null){
                mCountryCode = m.group();
            }else {
                HAS_LOCATION = false;
            }
        }
    }

    public static String getCountryCodeFromMopubResponse(){
        return mCountryCode;
    }

    private AdResponse changeResponseCustomClassPath(AdResponse adResponse) {
        if(adResponse == null || adResponse.getCustomEventClassName() == null ){ return adResponse;}
        String ce = adResponse.getCustomEventClassName();
        if(ce.contains("VastVideoInterstitial") || ce.contains("MraidInterstitial") || ce.contains("HtmlInterstitial")){ return adResponse;}

        String[] customClass = adResponse.getCustomEventClassName().split("\\.");
        if(customClass[2].equals("mobileads")){
            return adResponse.toBuilder().setCustomEventClassName("com.mopub.ads.adapters." + customClass[3]).build();
        }
        return adResponse;
    }

    @VisibleForTesting
    @NonNull
    static MoPubErrorCode getErrorCodeFromVolleyError(@NonNull final VolleyError error,
            @Nullable final Context context) {
        final NetworkResponse networkResponse = error.networkResponse;

        // For MoPubNetworkErrors, networkResponse is null.
        if (error instanceof MoPubNetworkError) {
            switch (((MoPubNetworkError) error).getReason()) {
                case WARMING_UP:
                    return MoPubErrorCode.WARMUP;
                case NO_FILL:
                    return MoPubErrorCode.NO_FILL;
                default:
                    return MoPubErrorCode.UNSPECIFIED;
            }
        }

        if (networkResponse == null) {
            if (!DeviceUtils.isNetworkAvailable(context)) {
                return MoPubErrorCode.NO_CONNECTION;
            }
            return MoPubErrorCode.UNSPECIFIED;
        }

        if (error.networkResponse.statusCode >= 400) {
            return MoPubErrorCode.SERVER_ERROR;
        }

        return MoPubErrorCode.UNSPECIFIED;
    }

    @Nullable
    public MoPubView getMoPubView() {
        return mMoPubView;
    }

    public void loadAd() {
        mBackoffPower = 1;
        internalLoadAd();
    }

    private void internalLoadAd() {
        mAdWasLoaded = true;
        if (TextUtils.isEmpty(mAdUnitId)) {
            MoPubLog.d("Can't load an ad in this ad view because the ad unit ID is not set. " +
                    "Did you forget to call setAdUnitId()?");
            return;
        }

        if (!isNetworkAvailable()) {
            MoPubLog.d("Can't load an ad because there is no network connectivity.");
            scheduleRefreshTimerIfEnabled();
            return;
        }

        String adUrl = generateAdUrl();
        loadNonJavascript(adUrl);
    }

    void loadNonJavascript(@Nullable final String url) {
        if (url == null) {
            return;
        }

        if (!url.startsWith("javascript:")) {
            MoPubLog.d("Loading url: " + url);
        }

        if (mIsLoading) {
            if (!TextUtils.isEmpty(mAdUnitId)) {  // This shouldn't be able to happen?
                MoPubLog.i("Already loading an ad for " + mAdUnitId + ", wait to finish.");
            }
            return;
        }

        mUrl = url;
        mIsLoading = true;

        fetchAd(mUrl);
    }

    public void reload() {
        MoPubLog.d("Reload ad: " + mUrl);
        loadNonJavascript(mUrl);
    }

    /**
     * Returns true if continuing to load the failover url, false if the ad actually did not fill.
     */
    boolean loadFailUrl(MoPubErrorCode errorCode) {
        mIsLoading = false;

        Log.v("MoPub", "MoPubErrorCode: " + (errorCode == null ? "" : errorCode.toString()));

        final String failUrl = mAdResponse == null ? "" : mAdResponse.getFailoverUrl();
        if (!TextUtils.isEmpty(failUrl)) {
            MoPubLog.d("Loading failover url: " + failUrl);
            loadNonJavascript(failUrl);
            return true;
        } else {
            // No other URLs to try, so signal a failure.
            adDidFail(MoPubErrorCode.NO_FILL);
            return false;
        }
    }

    void setNotLoading() {
        this.mIsLoading = false;
        if (mActiveRequest != null) {
            if (!mActiveRequest.isCanceled()) {
                mActiveRequest.cancel();
            }
            mActiveRequest = null;
        }
    }

    public String getKeywords() {
        return mKeywords;
    }

    public void setKeywords(String keywords) {
        mKeywords = keywords;
    }

    public String getUserDataKeywords() {
        if (!MoPub.canCollectPersonalInformation()) {
            return null;
        }
        return mUserDataKeywords;
    }

    public void setUserDataKeywords(String userDataKeywords) {
        if (!MoPub.canCollectPersonalInformation()) {
            mUserDataKeywords = null;
            return;
        }
        mUserDataKeywords = userDataKeywords;
    }

    public Location getLocation() {
        if (!MoPub.canCollectPersonalInformation()) {
            return null;
        }
        return mLocation;
    }

    public void setLocation(Location location) {
        if (!MoPub.canCollectPersonalInformation()) {
            mLocation = null;
            return;
        }
        mLocation = location;
    }

    public String getAdUnitId() {
        return mAdUnitId;
    }

    @Nullable
    public String getCustomEventClassName() {
        return mCustomEventClassName;
    }

    public void setAdUnitId(@NonNull String adUnitId) {
        mAdUnitId = adUnitId;
    }

    public long getBroadcastIdentifier() {
        return mBroadcastIdentifier;
    }

    public int getAdWidth() {
        if (mAdResponse != null && mAdResponse.getWidth() != null) {
            return mAdResponse.getWidth();
        }

        return 0;
    }

    public int getAdHeight() {
        if (mAdResponse != null && mAdResponse.getHeight() != null) {
            return mAdResponse.getHeight();
        }

        return 0;
    }

    /**
     * This has been renamed to {@link #getCurrentAutoRefreshStatus()}.
     */
    @Deprecated
    public boolean getAutorefreshEnabled() {
        return getCurrentAutoRefreshStatus();
    }

    public boolean getCurrentAutoRefreshStatus() {
        return mCurrentAutoRefreshStatus;
    }

    void pauseRefresh() {
        setAutoRefreshStatus(false);
    }

    void resumeRefresh() {
        if (mShouldAllowAutoRefresh && !mExpanded) {
            setAutoRefreshStatus(true);
        }
    }

    void setShouldAllowAutoRefresh(final boolean shouldAllowAutoRefresh) {
        mShouldAllowAutoRefresh = shouldAllowAutoRefresh;
        setAutoRefreshStatus(shouldAllowAutoRefresh);
    }

    private void setAutoRefreshStatus(final boolean newAutoRefreshStatus) {
        final boolean autoRefreshStatusChanged = mAdWasLoaded &&
                (mCurrentAutoRefreshStatus != newAutoRefreshStatus);
        if (autoRefreshStatusChanged) {
            final String enabledString = (newAutoRefreshStatus) ? "enabled" : "disabled";
            MoPubLog.d("Refresh " + enabledString + " for ad unit (" + mAdUnitId + ").");
        }

        mCurrentAutoRefreshStatus = newAutoRefreshStatus;
        if (mAdWasLoaded && mCurrentAutoRefreshStatus) {
            scheduleRefreshTimerIfEnabled();
        } else if (!mCurrentAutoRefreshStatus) {
            cancelRefreshTimer();
        }
    }

    void expand() {
        mExpanded = true;
        pauseRefresh();
    }

    void collapse() {
        mExpanded = false;
        resumeRefresh();
    }

    @Nullable
    public AdReport getAdReport() {
        if (mAdUnitId != null && mAdResponse != null) {
            return new AdReport(mAdUnitId, ClientMetadata.getInstance(mContext), mAdResponse);
        }
        return null;
    }

    public boolean getTesting() {
        return mIsTesting;
    }

    public void setTesting(boolean enabled) {
        mIsTesting = enabled;
    }

    boolean isDestroyed() {
        return mIsDestroyed;
    }

    /*
     * Clean up the internal state of the AdViewController.
     */
    void cleanup() {
        if (mIsDestroyed) {
            return;
        }

        if (mActiveRequest != null) {
            mActiveRequest.cancel();
            mActiveRequest = null;
        }

        setAutoRefreshStatus(false);
        cancelRefreshTimer();

        // WebView subclasses are not garbage-collected in a timely fashion on Froyo and below,
        // thanks to some persistent references in WebViewCore. We manually release some resources
        // to compensate for this "leak".
        mMoPubView = null;
        mContext = null;
        mUrlGenerator = null;

        // Flag as destroyed. LoadUrlTask checks this before proceeding in its onPostExecute().
        mIsDestroyed = true;
    }

    Integer getAdTimeoutDelay() {
        return mTimeoutMilliseconds;
    }

    void trackImpression() {
        if (mAdResponse != null) {
            TrackingRequest.makeTrackingHttpRequest(mAdResponse.getImpressionTrackingUrl(),
                    mContext);
        }
    }

    void registerClick() {
        if (mAdResponse != null) {
            // Click tracker fired from Banners and Interstitials
            TrackingRequest.makeTrackingHttpRequest(mAdResponse.getClickTrackingUrl(),
                    mContext);
        }
    }

    void fetchAd(String url) {
        MoPubView moPubView = getMoPubView();
        if (moPubView == null || mContext == null) {
            MoPubLog.d("Can't load an ad in this ad view because it was destroyed.");
            setNotLoading();
            return;
        }

        AdRequest adRequest = new AdRequest(url,
                moPubView.getAdFormat(),
                mAdUnitId,
                mContext,
                mAdListener
        );
        RequestQueue requestQueue = Networking.getRequestQueue(mContext);
        requestQueue.add(adRequest);
        mActiveRequest = adRequest;
    }

    void forceRefresh() {
        setNotLoading();
        loadAd();
    }

    @Nullable
    String generateAdUrl() {
        if (mUrlGenerator == null) {
            return null;
        }

        final boolean canCollectPersonalInformation = MoPub.canCollectPersonalInformation();

        mUrlGenerator
                .withAdUnitId(mAdUnitId)
                .withKeywords(mKeywords)
                .withUserDataKeywords(canCollectPersonalInformation ? mUserDataKeywords : null)
                .withLocation(canCollectPersonalInformation ? mLocation : null);

        return mUrlGenerator.generateUrlString(Constants.HOST);
    }

    void adDidFail(MoPubErrorCode errorCode) {
        MoPubLog.i("Ad failed to load.");
        setNotLoading();

        MoPubView moPubView = getMoPubView();
        if (moPubView == null) {
            return;
        }

        scheduleRefreshTimerIfEnabled();
        moPubView.adFailed(errorCode);
    }

    void scheduleRefreshTimerIfEnabled() {
        cancelRefreshTimer();
        if (mCurrentAutoRefreshStatus && mRefreshTimeMillis != null && mRefreshTimeMillis > 0) {

            mHandler.postDelayed(mRefreshRunnable,
                    Math.min(MAX_REFRESH_TIME_MILLISECONDS,
                            mRefreshTimeMillis * (long) Math.pow(BACKOFF_FACTOR, mBackoffPower)));
        }
    }

    void setLocalExtras(Map<String, Object> localExtras) {
        mLocalExtras = (localExtras != null)
                ? new TreeMap<String,Object>(localExtras)
                : new TreeMap<String,Object>();
    }

    /**
     * Returns a copied map of localExtras
     */
    Map<String, Object> getLocalExtras() {
        return (mLocalExtras != null)
                ? new TreeMap<String,Object>(mLocalExtras)
                : new TreeMap<String,Object>();
    }

    private void cancelRefreshTimer() {
        mHandler.removeCallbacks(mRefreshRunnable);
    }

    private boolean isNetworkAvailable() {
        if (mContext == null) {
            return false;
        }
        // If we don't have network state access, just assume the network is up.
        if (!DeviceUtils.isPermissionGranted(mContext, ACCESS_NETWORK_STATE)) {
            return true;
        }

        // Otherwise, perform the connectivity check.
        ConnectivityManager cm
                = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    void setAdContentView(final View view) {
        // XXX: This method is called from the WebViewClient's callbacks, which has caused an error on a small portion of devices
        // We suspect that the code below may somehow be running on the wrong UI Thread in the rare case.
        // see: https://stackoverflow.com/questions/10426120/android-got-calledfromwrongthreadexception-in-onpostexecute-how-could-it-be
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                MoPubView moPubView = getMoPubView();
                if (moPubView == null) {
                    return;
                }
                moPubView.removeAllViews();
                moPubView.addView(view, getAdLayoutParams(view));
            }
        });
    }

    private FrameLayout.LayoutParams getAdLayoutParams(View view) {
        Integer width = null;
        Integer height = null;
        if (mAdResponse != null) {
            width = mAdResponse.getWidth();
            height = mAdResponse.getHeight();
        }

        if (width != null && height != null && getShouldHonorServerDimensions(view) && width > 0 && height > 0) {
            int scaledWidth = Dips.asIntPixels(width, mContext);
            int scaledHeight = Dips.asIntPixels(height, mContext);

            return new FrameLayout.LayoutParams(scaledWidth, scaledHeight, Gravity.CENTER);
        } else {
            return WRAP_AND_CENTER_LAYOUT_PARAMS;
        }
    }

    @Deprecated // for testing
    @VisibleForTesting
    Integer getRefreshTimeMillis() {
        return mRefreshTimeMillis;
    }

    @Deprecated // for testing
    @VisibleForTesting
    void setRefreshTimeMillis(@Nullable final Integer refreshTimeMillis) {
        mRefreshTimeMillis = refreshTimeMillis;
    }
}
