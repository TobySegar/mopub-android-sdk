package com.mopub.mobileads;

import android.support.annotation.NonNull;

import com.mopub.common.Preconditions;

import java.io.Serializable;
import java.util.Locale;

/**
 * A Vast tracking URL with an "absolute" trigger threshold. The tracker should be triggered
 * after a fixed number of milliseconds have been played.
 */
public class VastAbsoluteProgressTracker extends VastTracker
        implements Comparable<VastAbsoluteProgressTracker>, Serializable {
    private static final long serialVersionUID = 0L;
    private final int mTrackingMilliseconds;

    public VastAbsoluteProgressTracker(@NonNull final MessageType messageType,
            @NonNull final String content, int trackingMilliseconds) {
        super(messageType, content);
        /**
         * Bojo-We were getting here negative @trackingMilliseconds on firebase
         * This is video time tracker that can be triggered or not base on it
         * its tracket if video was seen and you will get icon to close the vid??
         */
        //todo test this code when you can get intertitial
        if(trackingMilliseconds <= 0){
            trackingMilliseconds = 300;
        }
        Preconditions.checkArgument(trackingMilliseconds >= 0);
        mTrackingMilliseconds = trackingMilliseconds;
    }

    public VastAbsoluteProgressTracker(@NonNull final String trackingUrl,
            int trackingMilliseconds) {
        this(MessageType.TRACKING_URL, trackingUrl, trackingMilliseconds);
    }

    public int getTrackingMilliseconds() {
        return mTrackingMilliseconds;
    }

    @Override
    public int compareTo(@NonNull final VastAbsoluteProgressTracker other) {
        int you = other.getTrackingMilliseconds();
        int me = getTrackingMilliseconds();

        return me - you;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%dms: %s", mTrackingMilliseconds, getContent());
    }
}
