package com.mopub.ads;

import android.content.SharedPreferences;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class AdsTest {

    private Ads subject;
    @Mock private SharedPreferences sharedPreferencesMock;
    @Mock private Interstitial interstitialMock;

    @Before
    public void setUp() throws Exception {
        subject = new Ads(interstitialMock,null, sharedPreferencesMock,null,false);
        when(sharedPreferencesMock.getBoolean(Ads.FIRST_RUN_KEY,false)).thenReturn(true);
    }

    @Test
    public void checkIfBuilding_withCloseTimingsArray_shoudReturnTrue() throws Exception{
        int numberOfElementsToSum = 3; //we check placement times of last two placed blocks
        int closePlacementDiffereence = 700; //to be considered Build
        long firstPlaceTime = 100000;
        long secondPlaceTime = 110000;
        //we simulate couple of placements with long short time distance between
        long thirdPlaceTime = secondPlaceTime + closePlacementDiffereence;
        long forthPlaceTime = secondPlaceTime + closePlacementDiffereence*2;
        long currentPlaceTime =  secondPlaceTime  + closePlacementDiffereence*3;


        long[] placementTimes = new long[]{0,0,firstPlaceTime,secondPlaceTime,thirdPlaceTime,forthPlaceTime};

        subject.checkIfBuilding(placementTimes, numberOfElementsToSum, closePlacementDiffereence, currentPlaceTime);

        assertTrue(subject.isBuilding);
    }

    @Test
    public void checkIfBuilding_withLongTimingsArray_shoudReturnTrue() throws Exception{
        int numberOfElementsToSum = 3; //we check placement times of last two placed blocks
        int closePlacementDiffereence = 700; //to be considered Build
        int longPlacementDifference = 800;
        //we simulate couple of placements with long time distance between
        long firstPlaceTime = 100000;
        long secondPlaceTime = 110000;
        long thirdPlaceTime = secondPlaceTime + longPlacementDifference;
        long forthPlaceTime = secondPlaceTime + longPlacementDifference*2;
        long currentPlaceTime =  secondPlaceTime  + longPlacementDifference*3;


        long[] placementTimes = new long[]{0,0,firstPlaceTime,secondPlaceTime,thirdPlaceTime,forthPlaceTime};

        subject.checkIfBuilding(placementTimes, numberOfElementsToSum, closePlacementDiffereence, currentPlaceTime);

        assertFalse(subject.isBuilding);
    }

    @Test
    public void showAdIfBuilding_withBuildingTrue__shouldShowInterstitial() throws Exception {
        subject.isBuilding = true;

        //noinspection ConstantConditions
        subject.showAdIfBuilding();

        verify(interstitialMock).show();
    }

    @Test
    public void showAdIfBuilding_withBuildingTrue_withReadyInterstitial_shouldShowInterstitialOnlyOnce() throws Exception {
        subject.isBuilding = true;
        when(interstitialMock.show()).thenReturn(true);

        //noinspection ConstantConditions
        subject.showAdIfBuilding();
        subject.showAdIfBuilding();

        verify(interstitialMock, times(1)).show();
    }

    @Test
    public void showAdIfBuilding_withBuildingTrue_withFailedInterstitialShow_shouldTryToShowInterstitial() throws Exception {
        subject.isBuilding = true;
        when(interstitialMock.show()).thenReturn(false);

        //noinspection ConstantConditions
        subject.showAdIfBuilding();
        subject.isBuilding = true;
        subject.showAdIfBuilding();

        verify(interstitialMock, times(2)).show();
    }

    @Test
    public void showAdIfBuilding_withBuildingFalse__shouldNotShowInterstitial() throws Exception {
        subject.isBuilding = false;

        //noinspection ConstantConditions
        subject.showAdIfBuilding();

        verify(interstitialMock, never()).show();
    }


    @After
    public void tearDown() throws Exception {

    }
}