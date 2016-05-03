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
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class AdsTest {

    private Ads subject;
    @Mock private SharedPreferences sharedPreferences;

    @Before
    public void setUp() throws Exception {
        subject = new Ads(null,null,sharedPreferences,null,false);
        when(sharedPreferences.getBoolean(Ads.FIRST_RUN_KEY,false)).thenReturn(true);
    }

    @Test
    public void checkIfBuilding_withCloseTimingsArray_shoudReturnTrue() throws Exception{
        int numberOfElementsToSum = 3; //we check placement times of last two placed blocks
        int closePlacementDiffereence = 700; //to be considered Build
        long firstPlaceTime = 100000;
        long secondPlaceTime = 110000;
        long thirdPlaceTime = secondPlaceTime + closePlacementDiffereence;
        long forthPlaceTime = secondPlaceTime + closePlacementDiffereence*2;
        long currentPlaceTime =  secondPlaceTime  + closePlacementDiffereence*3;


        long[] placementTimes = new long[]{0,0,firstPlaceTime,secondPlaceTime,thirdPlaceTime,forthPlaceTime};

        boolean isBuilding = subject.checkIfBuilding(placementTimes, numberOfElementsToSum, closePlacementDiffereence, currentPlaceTime);

        assertTrue(isBuilding);
    }

    @Test
    public void checkIfBuilding_withLongTimingsArray_shoudReturnTrue() throws Exception{
        int numberOfElementsToSum = 3; //we check placement times of last two placed blocks
        int closePlacementDiffereence = 700; //to be considered Build
        int longPlacementDifference = 800;
        long firstPlaceTime = 100000;
        long secondPlaceTime = 110000;
        long thirdPlaceTime = secondPlaceTime + longPlacementDifference;
        long forthPlaceTime = secondPlaceTime + longPlacementDifference*2;
        long currentPlaceTime =  secondPlaceTime  + longPlacementDifference*3;


        long[] placementTimes = new long[]{0,0,firstPlaceTime,secondPlaceTime,thirdPlaceTime,forthPlaceTime};

        boolean isBuilding = subject.checkIfBuilding(placementTimes, numberOfElementsToSum, closePlacementDiffereence, currentPlaceTime);

        assertFalse(isBuilding);
    }

    @After
    public void tearDown() throws Exception {

    }
}