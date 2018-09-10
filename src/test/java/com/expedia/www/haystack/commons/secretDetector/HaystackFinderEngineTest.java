package com.expedia.www.haystack.commons.secretDetector;

import com.expedia.www.haystack.metrics.MetricObjects;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.RANDOM;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class HaystackFinderEngineTest {
    private static final String SUBSYSTEM = RANDOM.nextLong() + "SUBSYSTEM";
    private static final String APPLICATION = RANDOM.nextLong() + "APPLICATION";

    @Mock
    private MetricObjects mockMetricObjects;

    private HaystackFinderEngine haystackFinderEngine;

    @Before
    public void setUp() {
        haystackFinderEngine = new HaystackFinderEngine(mockMetricObjects, SUBSYSTEM, APPLICATION);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(mockMetricObjects);
    }

    @Test
    public void testGetFinders() {
        assertEquals(13, haystackFinderEngine.getFinders().size());
    }
}
