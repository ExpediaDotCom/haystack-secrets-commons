package com.expedia.www.haystack.commons.secretDetector;

import com.expedia.www.haystack.commons.secretDetector.HaystackFinderProvider.Factory;
import io.dataapps.chlorine.finder.DefaultFinderProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.LocatorImpl;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.io.IOException;
import java.io.InputStream;

import static com.expedia.www.haystack.commons.secretDetector.HaystackFinderProvider.IO_EXCEPTION_PROBLEM;
import static com.expedia.www.haystack.commons.secretDetector.HaystackFinderProvider.OBJECT_CREATION_PROBLEM;
import static com.expedia.www.haystack.commons.secretDetector.HaystackFinderProvider.PARSER_CONFIGURATION_PROBLEM;
import static com.expedia.www.haystack.commons.secretDetector.HaystackFinderProvider.PROBLEM_WITH_REGION_MSG;
import static com.expedia.www.haystack.commons.secretDetector.HaystackFinderProvider.SAX_EXCEPTION_PROBLEM;
import static io.dataapps.chlorine.finder.DefaultFinderProvider.FINDERS_DEFAULT_XML;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class HaystackFinderProviderTest {
    private static final String TEST = "Test";
    @Mock
    private DefaultFinderProvider mockDefaultFinderProvider;
    @Mock
    private Logger mockLogger;
    @Mock
    private Factory mockFactory;
    @Mock
    private InputStream mockInputStream;
    @Mock
    private SAXParser mockSAXParser;

    private SAXParserFactory saxParserFactory;
    private HaystackFinderProvider haystackFinderProvider;
    private int timesConstructorCalled = 1;

    @Before
    public void setUp() throws ParserConfigurationException, SAXException {
        when(mockFactory.getFindersDotDefaultInputStream())
                .thenReturn(DefaultFinderProvider.class.getClassLoader().getResourceAsStream(FINDERS_DEFAULT_XML));
        saxParserFactory = SAXParserFactory.newInstance();
        when(mockFactory.createSaxParserFactory()).thenReturn(saxParserFactory);
        when(mockFactory.createSaxParser(any())).thenReturn(saxParserFactory.newSAXParser());

        haystackFinderProvider = new HaystackFinderProvider(mockLogger, mockFactory);
    }

    @After
    public void tearDown() throws ParserConfigurationException, SAXException {
        verify(mockFactory, times(timesConstructorCalled)).getFindersDotDefaultInputStream();
        verify(mockFactory, times(timesConstructorCalled)).createSaxParserFactory();
        verify(mockFactory, times(timesConstructorCalled)).createSaxParser(saxParserFactory);
        verify(mockLogger).error(String.format(PROBLEM_WITH_REGION_MSG, "NON_EXISTENT_REGION"));

        verifyNoMoreInteractions(mockDefaultFinderProvider);
        verifyNoMoreInteractions(mockLogger);
        verifyNoMoreInteractions(mockFactory);
        verifyNoMoreInteractions(mockInputStream);
        verifyNoMoreInteractions(mockSAXParser);
    }

    @Test
    public void testDefaultConstructor() {
        haystackFinderProvider = new HaystackFinderProvider();
        assertEquals(13, (haystackFinderProvider.getFinders().size()));
        verify(mockLogger).error(eq(OBJECT_CREATION_PROBLEM), any(ClassNotFoundException.class));
    }

    @Test
    public void testConstructor() {
        assertEquals(13, (haystackFinderProvider.getFinders().size()));
        verify(mockLogger).error(eq(OBJECT_CREATION_PROBLEM), any(ClassNotFoundException.class));
    }

    @Test
    public void testConstructorIOException() throws IOException, SAXException, ParserConfigurationException {
        when(mockFactory.createSaxParser(any())).thenReturn(mockSAXParser);
        final IOException ioException = new IOException(TEST);
        doThrow(ioException).when(mockInputStream).close();

        timesConstructorCalled = 2;
        when(mockFactory.getFindersDotDefaultInputStream()).thenReturn(mockInputStream);

        haystackFinderProvider = new HaystackFinderProvider(mockLogger, mockFactory);
        assertEquals(0, (haystackFinderProvider.getFinders().size()));

        verify(mockSAXParser).parse(eq(mockInputStream), any(DefaultHandler.class));
        verify(mockInputStream).close();

        verify(mockLogger).error(IO_EXCEPTION_PROBLEM, ioException);
        verify(mockLogger).error(eq(OBJECT_CREATION_PROBLEM), any(ClassNotFoundException.class));
    }

    @Test
    public void testConstructorSAXException() throws IOException, SAXException, ParserConfigurationException {
        final SAXParseException saxParseException = new SAXParseException(TEST, new LocatorImpl());
        when(mockFactory.createSaxParser(any())).thenThrow(saxParseException);

        timesConstructorCalled = 2;
        when(mockFactory.getFindersDotDefaultInputStream()).thenReturn(mockInputStream);

        haystackFinderProvider = new HaystackFinderProvider(mockLogger, mockFactory);
        assertEquals(0, (haystackFinderProvider.getFinders().size()));

        verify(mockInputStream).close();
        verify(mockLogger).error(SAX_EXCEPTION_PROBLEM, saxParseException);
        verify(mockLogger).error(eq(OBJECT_CREATION_PROBLEM), any(ClassNotFoundException.class));
    }

    @Test
    public void testConstructorParserConfigurationException() throws IOException, SAXException, ParserConfigurationException {
        final ParserConfigurationException parserConfigurationException = new ParserConfigurationException(TEST);
        when(mockFactory.createSaxParser(any())).thenThrow(parserConfigurationException);

        timesConstructorCalled = 2;
        when(mockFactory.getFindersDotDefaultInputStream()).thenReturn(mockInputStream);

        haystackFinderProvider = new HaystackFinderProvider(mockLogger, mockFactory);
        assertEquals(0, (haystackFinderProvider.getFinders().size()));

        verify(mockInputStream).close();
        verify(mockLogger).error(PARSER_CONFIGURATION_PROBLEM, parserConfigurationException);
        verify(mockLogger).error(eq(OBJECT_CREATION_PROBLEM), any(ClassNotFoundException.class));
    }

}
