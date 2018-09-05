package com.expedia.www.haystack.commons.secretDetector;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HaystackFinderEngineTest {
    private HaystackFinderEngine haystackFinderEngine;

    @Before
    public void setUp() {
        haystackFinderEngine = new HaystackFinderEngine();
    }

    @Test
    public void testGetFinders() {
        assertEquals(13, haystackFinderEngine.getFinders().size());
    }
}
