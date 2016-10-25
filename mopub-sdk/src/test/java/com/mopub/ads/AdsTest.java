package com.mopub.ads;

import android.content.Context;
import android.content.SharedPreferences;

import com.mojang.base.events.MinecraftGameEvent;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Calendar;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class AdsTest {

    private Ads subject;
    @Mock private Interstitial interstitialMock;
    @Mock private Calendar calendarMock;

    @Before
    public void setUp() throws Exception {
        SharedPreferences sharedPreferences = RuntimeEnvironment.application.getSharedPreferences("TEST", Context.MODE_PRIVATE);
        subject = new Ads(interstitialMock,null, sharedPreferences,calendarMock,false);

        when(calendarMock.get(subject.measureUnit)).thenReturn(0);
    }


    @Test
    public void onStartSleepEventShould_ShowUnityVideo() throws Exception {
        subject.onGameEvent(new MinecraftGameEvent(null, MinecraftGameEvent.Event.StartSleepInBed));

        verify(interstitialMock, times(1)).showUnityAdsVideo();
    }


    @After
    public void tearDown() throws Exception {

    }
}