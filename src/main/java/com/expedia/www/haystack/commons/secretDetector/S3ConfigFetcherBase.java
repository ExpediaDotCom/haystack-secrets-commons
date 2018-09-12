/*
 * Copyright 2018 Expedia, Inc.
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 *
 */
package com.expedia.www.haystack.commons.secretDetector;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.netflix.servo.util.VisibleForTesting;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public abstract class S3ConfigFetcherBase {
    private static final long ONE_HOUR = TimeUnit.HOURS.toMillis(1L);
    @VisibleForTesting
    public static final String SUCCESSFUL_WHITELIST_UPDATE_MSG = "Successfully updated the whitelist from S3";
    @VisibleForTesting
    public static final String ERROR_MESSAGE = "Exception getting white list items";
    public static final String INVALID_DATA_MSG = "The line [%s] does not contain at least %d semicolons to "
            + "separate the expected fields";
    private static final String SEMICOLON_FIELD_DELIMITER = ";";

    private final Prefix prefix;
    protected final Logger logger;
    protected final String bucket;
    protected final String key;
    private final AmazonS3 amazonS3;
    private final Factory factory;
    @VisibleForTesting
    private final AtomicReference<Object> whiteList = new AtomicReference<>();
    private final int itemCount;
    private final AtomicLong lastUpdateTime = new AtomicLong(0L);
    private final AtomicBoolean isUpdateInProgress = new AtomicBoolean(false);

    /**
     * This <code>Prefix</code> enum allows different types of lines to exist in the same S3 item. The most common use
     * case for this <code>S3ConfigFetcherBase</code> class and its derived classes is to identify data that produces
     * false positives when looking for secrets and ignore the false positives with a white list. There are three kinds
     * of white list items, specified in this Prefix. Each line in a white list configuration file should start with one
     * of the values from this <code>Prefix</code> enum.
     * <ul>
     *     <li>
     *         <code>SPAN</code> white list items have three fields:
     *         <ol>
     *             <li><code>Finder Name</code></li>
     *             <li><code>Service Name</code></li>
     *             <li><code>Operation Name</code></li>
     *             <li><code>Tag Name</code> (applies to both span tags and log tags)</li>
     *         </ol>
     *     </li>
     *     <li>
     *         <code>XML</code> and <code>JSON</code> white list items have two fields:
     *         <ol>
     *             <li><code>Finder Name</code></li>
     *             <li><code>Identifier</code> identifies the element in the XML or JSON</li>
     *         </ol>
     *     </li>
     * </ul>
     * Users of this class that do not need the Prefix functionality can specify a <code>null</code> for prefix.
     */
    public enum Prefix {
        SPAN,
        XML,
        JSON
    }

    @SuppressWarnings("ConstructorWithTooManyParameters")
    protected S3ConfigFetcherBase(Prefix prefix,
                                  Logger logger,
                                  String bucket,
                                  String key,
                                  AmazonS3 amazonS3,
                                  Factory factory,
                                  int itemCount) {
        this.prefix = prefix;
        this.logger = logger;
        this.bucket = bucket;
        this.key = key;
        this.amazonS3 = amazonS3;
        this.factory = factory;
        this.whiteList.set(factory.createWhiteList());
        this.itemCount = itemCount;
    }

    public void setUpdateInProgressForTest(boolean isUpdateInProgress) {
        this.isUpdateInProgress.set(isUpdateInProgress);
    }

    public boolean isUpdateInProgressForTest() {
        return isUpdateInProgress.get();
    }

    public long getLastUpdateTimeForTest() {
        return lastUpdateTime.get();
    }

    private BufferedReader getBufferedReader(S3Object s3Object) {
        final InputStream inputStream = s3Object.getObjectContent();
        final InputStreamReader inputStreamReader = factory.createInputStreamReader(inputStream);
        return factory.createBufferedReader(inputStreamReader);
    }

    /**
     * Reads a line from S3 and transforms it to an appropriate subclass of WhiteListItemBase
     *
     * @param reader the reader
     * @return a non-null WhiteListItemBase if the read was successful, else null to indicate all lines have been read
     * @throws IOException                        if a problem occurs reading from S3
     * @throws InvalidWhitelistItemInputException if an input line in the S3 file is not formatted properly
     */
    private WhiteListItemBase readSingleWhiteListItemFromS3(BufferedReader reader)
            throws IOException, InvalidWhitelistItemInputException {
        final String[] strings = readNextWhiteListItemFromS3WithMatchingPrefix(reader);
        if(strings == null) {
            return null;
        }
        if (strings.length >= itemCount) {
            return factory.createWhiteListItem(strings);
        }
        throw new InvalidWhitelistItemInputException(prefix, strings, itemCount);
    }

    private String[] readNextWhiteListItemFromS3WithMatchingPrefix(BufferedReader reader) throws IOException {
        while(true) {
            final String line = reader.readLine();
            if (line == null) {
                return null;
            }
            final String[] strings = line.split(SEMICOLON_FIELD_DELIMITER);
            if(prefix == null) {
                return strings;
            }
            else {
                final Prefix prefixAsEnum = Prefix.valueOf(strings[0]);
                if (prefix.equals(prefixAsEnum)) {
                    return Arrays.copyOfRange(strings, 1, strings.length);
                }
            }
        }
    }

    public Object getWhiteListItems() {
        final long now = factory.createCurrentTimeMillis();
        if ((now - lastUpdateTime.get()) > ONE_HOUR) {
            if (isUpdateInProgress.compareAndSet(false, true)) {
                try {
                    whiteList.set(readAllWhiteListItemsFromS3());
                    logger.info(SUCCESSFUL_WHITELIST_UPDATE_MSG);
                } catch (InvalidWhitelistItemInputException e) {
                    logger.error(e.getMessage(), e);
                } catch (Exception e) {
                    logger.error(ERROR_MESSAGE, e);
                } finally {
                    isUpdateInProgress.set(false);
                    // Set last update time for successes and failures, to avoid log spamming for a persistent error
                    lastUpdateTime.set(now);
                }
            }
        }
        return whiteList.get();
    }

    protected abstract void putItemInWhiteList(Object whiteList, WhiteListItemBase whiteListItem);

    public abstract boolean isInWhiteList(String... strings);

    private Object readAllWhiteListItemsFromS3()
            throws IOException, InvalidWhitelistItemInputException {
        try (final S3Object s3Object = amazonS3.getObject(bucket, key)) {
            final BufferedReader bufferedReader = getBufferedReader(s3Object);
            @SuppressWarnings("unchecked") final Object newWhiteListItems = factory.createWhiteList();
            WhiteListItemBase whiteListItem = readSingleWhiteListItemFromS3(bufferedReader);
            while (whiteListItem != null) {
                putItemInWhiteList(newWhiteListItems, whiteListItem);
                whiteListItem = readSingleWhiteListItemFromS3(bufferedReader);
            }
            return newWhiteListItems;
        }
    }


    @SuppressWarnings("MethodMayBeStatic")
    public abstract static class Factory<T extends WhiteListItemBase> {
        public long createCurrentTimeMillis() {
            return System.currentTimeMillis();
        }

        public InputStreamReader createInputStreamReader(InputStream inputStream) {
            //noinspection ImplicitDefaultCharsetUsage
            return new InputStreamReader(inputStream);
        }

        public BufferedReader createBufferedReader(InputStreamReader inputStreamReader) {
            return new BufferedReader(inputStreamReader);
        }

        protected abstract T createWhiteListItem(String... items);

        public abstract Object createWhiteList();
    }

    @SuppressWarnings("CheckedExceptionClass")
    public static class InvalidWhitelistItemInputException extends Exception {
        InvalidWhitelistItemInputException(Prefix prefix, String[] strings, int itemCount) {
            super(String.format(INVALID_DATA_MSG,
                    prefix == null
                            ? String.join(SEMICOLON_FIELD_DELIMITER, strings)
                            : prefix + SEMICOLON_FIELD_DELIMITER + String.join(SEMICOLON_FIELD_DELIMITER, strings),
                    prefix == null ? itemCount - 1 : itemCount));
        }
    }
}
