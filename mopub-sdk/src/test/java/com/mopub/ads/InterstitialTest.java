package com.mopub.ads;

import com.mojang.base.Helper;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.mobileads.MoPubInterstitial;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class InterstitialTest {

    private Interstitial subject;
    private String interstitialId = "interstitialID";
    private long minimalAdGapMills = 15000;
    private double disableTouchChance = 0.0;
    private double fingerAdChance = 0.5;
    private List<String> highECPMcountries = new ArrayList<>();
    @Mock MoPubInterstitial moPubInterstitialMock;

    @Before
    public void setUp() throws Exception {
        subject = new Interstitial(null,interstitialId,null,minimalAdGapMills,disableTouchChance,null,highECPMcountries,fingerAdChance);
    }

    @Test
    public void onInterstitialLoaded_withInterstitialCountryCodeThat_Is_InHighEcpmCountries_shouldSetCanGetFingerAd_True() throws Exception {
        String highECPMcountry = "US";
        highECPMcountries.add(highECPMcountry);
        when(moPubInterstitialMock.getCountryCode()).thenReturn(highECPMcountry);

        subject.onInterstitialLoaded(moPubInterstitialMock);

        assertTrue(subject.canGetFingerAd);
    }
    @Test
    public void onInterstitialLoaded_withInterstitialCountryCodeThat_IsNot_InHighEcpmCountries_shouldSetCanGetFingerAd_False() throws Exception {
        String randomCountryCode = "RK";
        highECPMcountries.clear();
        when(moPubInterstitialMock.getCountryCode()).thenReturn(randomCountryCode);

        subject.onInterstitialLoaded(moPubInterstitialMock);

        assertFalse(subject.canGetFingerAd);
    }
}