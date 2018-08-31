package com.expedia.www.haystack.commons.secretDetector;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.netflix.servo.util.VisibleForTesting;
import io.dataapps.chlorine.finder.DefaultFinderProvider;
import io.dataapps.chlorine.finder.Finder;
import io.dataapps.chlorine.finder.FinderProvider;
import io.dataapps.chlorine.pattern.RegexFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static io.dataapps.chlorine.finder.DefaultFinderProvider.FINDERS_DEFAULT_XML;

@SuppressWarnings("WeakerAccess")
public class HaystackFinderProvider implements FinderProvider {
    private static final String REGION_ELEMENT_NAME = "REGION";
    private static final String CLASS_ELEMENT_NAME = "CLASS";
    private static final String ENABLED_ELEMENT_NAME = "ENABLED";
    static final String PROBLEM_WITH_REGION_MSG = "Problem with region [%s]";
    static final String SAX_EXCEPTION_PROBLEM = "SAX exception problem";
    static final String IO_EXCEPTION_PROBLEM = "IO exception problem";
    static final String PARSER_CONFIGURATION_PROBLEM = "Parser configuration problem";
    private final List<Finder> finders;
    private final Logger logger;
    private final Factory factory;

    public HaystackFinderProvider() {
        this(new DefaultFinderProvider(DefaultFinderProvider.class.getClassLoader()
                        .getResourceAsStream(FINDERS_DEFAULT_XML)),
                LoggerFactory.getLogger(HaystackFinderProvider.class),
                new Factory());
    }

    @VisibleForTesting
    HaystackFinderProvider(DefaultFinderProvider defaultFinderProvider,
                           Logger logger,
                           Factory factory) {
        finders = new ArrayList<>();
        this.logger = logger;
        this.factory = factory;
        finders.addAll(readNonHaystackFinders(defaultFinderProvider));
        finders.addAll(readHaystackPhoneNumberFinders());
    }

    private List<Finder> readNonHaystackFinders(
            DefaultFinderProvider defaultFinderProvider) {
        final ArrayList<Finder> finderList = new ArrayList<>(defaultFinderProvider.getFinders());
        finderList.removeIf(finder -> finder instanceof RegexFinder
                && ((RegexFinder) finder).getPattern().toString().isEmpty());
        return finderList;
    }

    private List<Finder> readHaystackPhoneNumberFinders() {
        final List<Finder> finderList = new ArrayList<>();
        try(final InputStream in = factory.getFindersDotDefaultInputStream()) {
            final SAXParserFactory saxParserFactory = factory.createSaxParserFactory();
            final SAXParser saxParser = factory.createSaxParser(saxParserFactory);
            final DefaultHandler handler = new DefaultHandler() {
                boolean isInRegionElement = false;
                boolean isInClassElement = false;
                boolean isInEnabledElement = false;
                final StringBuilder region = new StringBuilder();
                final StringBuilder className = new StringBuilder();
                final StringBuilder strEnabled = new StringBuilder();
                boolean enabled = true;

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    if (qName.equalsIgnoreCase(REGION_ELEMENT_NAME)) {
                        isInRegionElement = true;
                    } else if (qName.equalsIgnoreCase(CLASS_ELEMENT_NAME)) {
                        isInClassElement = true;
                    } else if (qName.equalsIgnoreCase(ENABLED_ELEMENT_NAME)) {
                        isInEnabledElement = true;
                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) {
                    if (qName.equalsIgnoreCase(REGION_ELEMENT_NAME)) {
                        isInRegionElement = false;
                    } else if (qName.equalsIgnoreCase(CLASS_ELEMENT_NAME)) {
                        isInClassElement = false;
                    } else if (qName.equalsIgnoreCase(ENABLED_ELEMENT_NAME)) {
                        enabled = Boolean.valueOf(strEnabled.toString().trim());
                        isInEnabledElement = false;
                    } else {
                        if (enabled) {
                            if (className.toString().trim().equals(HaystackPhoneNumberFinder.class.getName())) {
                                final String trimmedRegion = region.toString().trim();
                                try {
                                    final HaystackPhoneNumberFinder haystackPhoneNumberFinder =
                                            new HaystackPhoneNumberFinder(PhoneNumberUtil.getInstance(),
                                                    CldrRegion.valueOf(trimmedRegion));
                                    finderList.add(haystackPhoneNumberFinder);
                                } catch(IllegalArgumentException e) {
                                    logger.error(String.format(PROBLEM_WITH_REGION_MSG, trimmedRegion));
                                }
                            }
                        }
                        region.setLength(0);
                        className.setLength(0);
                        enabled = true;
                        strEnabled.setLength(0);
                    }
                }

                @Override
                public void characters(char ch[], int start, int length) {
                    if (isInRegionElement) {
                        region.append(ch, start, length);
                    } else if (isInClassElement) {
                        className.append(ch, start, length);
                    } else if (isInEnabledElement) {
                        strEnabled.append(ch, start, length);
                    }
                }
            };
            saxParser.parse(in, handler);
        } catch (IOException e) {
            logger.error(IO_EXCEPTION_PROBLEM, e);
        } catch (SAXException e) {
            logger.error(SAX_EXCEPTION_PROBLEM, e);
        } catch (ParserConfigurationException e) {
            logger.error(PARSER_CONFIGURATION_PROBLEM, e);
        }

        return finderList;
    }

    @Override
    public List<Finder> getFinders() {
        return finders;
    }

    static class Factory {
        SAXParserFactory createSaxParserFactory() {
            return SAXParserFactory.newInstance();
        }

        SAXParser createSaxParser(SAXParserFactory saxParserFactory)
                throws ParserConfigurationException, SAXException {
            return saxParserFactory.newSAXParser();
        }

        InputStream getFindersDotDefaultInputStream() {
            return DefaultFinderProvider.class.getClassLoader().getResourceAsStream(FINDERS_DEFAULT_XML);
        }
    }
}
