package com.expedia.www.haystack.commons.secretDetector;

import com.expedia.www.haystack.metrics.MetricObjects;
import com.netflix.servo.monitor.Stopwatch;
import com.netflix.servo.monitor.Timer;
import io.dataapps.chlorine.pattern.RegexFinder;
import org.junit.After;
import org.mockito.Mock;
import org.mockito.Mockito;

import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.RANDOM;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public abstract class DetectorTestBase {
    protected static final String APPLICATION = RANDOM.nextLong() + "APPLICATION";
    protected static final String SUBSYSTEM = RANDOM.nextLong() + "SUBSYSTEM";

    @Mock
    protected MetricObjects mockMetricObjects;

    @Mock
    private Timer mockTimer;

    @Mock
    private Stopwatch mockStopwatch;

    @After
    public void tearDown() {
        verifyNoMoreInteractions(mockMetricObjects);
        verifyNoMoreInteractions(mockTimer);
        verifyNoMoreInteractions(mockStopwatch);
    }

    protected void whensForFindSecrets() {
        Mockito.when(mockMetricObjects.createAndRegisterBasicTimer(any(), any(), any(), any(), any()))
                .thenReturn(mockTimer);
        Mockito.when(mockTimer.start()).thenReturn(mockStopwatch);
    }

    protected void verifiesForFindSecrets(int wantedNumberOfInvocationsStartAndStop,
                                          int wantedNumberOfInvocationsNonEmailFinders) {
        Mockito.verify(mockMetricObjects).createAndRegisterBasicTimer(SUBSYSTEM, APPLICATION,
                RegexFinder.class.getName(), "EMAIL", MILLISECONDS);
        Mockito.verify(mockMetricObjects, Mockito.times(wantedNumberOfInvocationsNonEmailFinders))
                .createAndRegisterBasicTimer(SUBSYSTEM, APPLICATION,
                        HaystackCompositeCreditCardFinder.class.getName(), "CREDIT_CARD",
                        MILLISECONDS);
        Mockito.verify(mockMetricObjects, Mockito.times(wantedNumberOfInvocationsNonEmailFinders))
                .createAndRegisterBasicTimer(SUBSYSTEM, APPLICATION,
                        NonLocalIpV4AddressFinder.class.getName(), NonLocalIpV4AddressFinder.FINDER_NAME.toUpperCase(),
                        MILLISECONDS);
        Mockito.verify(mockMetricObjects, Mockito.times(wantedNumberOfInvocationsNonEmailFinders))
                .createAndRegisterBasicTimer(SUBSYSTEM, APPLICATION,
                        RegexFinder.class.getName(), "STREET ADDRESS",
                        MILLISECONDS);
        Mockito.verify(mockMetricObjects, Mockito.times(wantedNumberOfInvocationsNonEmailFinders))
                .createAndRegisterBasicTimer(SUBSYSTEM, APPLICATION,
                        RegexFinder.class.getName(), "SSN-SPACES",
                        MILLISECONDS);
        Mockito.verify(mockMetricObjects, Mockito.times(wantedNumberOfInvocationsNonEmailFinders))
                .createAndRegisterBasicTimer(SUBSYSTEM, APPLICATION,
                        RegexFinder.class.getName(), "SSN-DASHES",
                        MILLISECONDS);
        Mockito.verify(mockMetricObjects, Mockito.times(wantedNumberOfInvocationsNonEmailFinders))
                .createAndRegisterBasicTimer(SUBSYSTEM, APPLICATION,
                        HaystackPhoneNumberFinder.class.getName(), HaystackPhoneNumberFinder.FINDER_NAME.toUpperCase(),
                        MILLISECONDS);
        Mockito.verify(mockTimer, Mockito.times(wantedNumberOfInvocationsStartAndStop)).start();
        Mockito.verify(mockStopwatch, Mockito.times(wantedNumberOfInvocationsStartAndStop)).stop();
    }
}
