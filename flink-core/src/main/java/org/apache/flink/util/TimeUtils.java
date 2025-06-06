/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.util;

import java.math.BigInteger;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.flink.util.Preconditions.checkArgument;
import static org.apache.flink.util.Preconditions.checkNotNull;

/** Collection of utilities about time intervals. */
public class TimeUtils {

    private static final Map<String, ChronoUnit> LABEL_TO_UNIT_MAP =
            Collections.unmodifiableMap(initMap());

    private static final BigInteger NANOS_PER_SECOND = BigInteger.valueOf(1_000_000_000L);

    /**
     * Parse the given string to a java {@link Duration}. The string is in format "{length
     * value}{time unit label}", e.g. "123ms", "321 s". If no time unit label is specified, it will
     * be considered as milliseconds. If above rules are not matched, it will fall back to parse
     * ISO-8601 duration format.
     *
     * <p>Supported time unit labels are:
     *
     * <ul>
     *   <li>DAYS： "d", "day"
     *   <li>HOURS： "h", "hour"
     *   <li>MINUTES： "m", "min", "minute"
     *   <li>SECONDS： "s", "sec", "second"
     *   <li>MILLISECONDS： "ms", "milli", "millisecond"
     *   <li>MICROSECONDS： "µs", "micro", "microsecond"
     *   <li>NANOSECONDS： "ns", "nano", "nanosecond"
     * </ul>
     *
     * @param text string to parse.
     */
    public static Duration parseDuration(String text) {
        checkNotNull(text);

        final String trimmed = text.trim();
        checkArgument(!trimmed.isEmpty(), "argument is an empty- or whitespace-only string");

        final int len = trimmed.length();
        int pos = 0;

        char current;
        while (pos < len && (current = trimmed.charAt(pos)) >= '0' && current <= '9') {
            pos++;
        }

        final String number = trimmed.substring(0, pos);
        final String unitLabel = trimmed.substring(pos).trim().toLowerCase(Locale.US);

        if (number.isEmpty()) {
            try {
                // Fall back to parse ISO-8601 duration format
                Duration parsedDuration = Duration.parse(trimmed);
                if (parsedDuration.isNegative()) {
                    // Don't support negative duration which is consistent with before format
                    throw new NumberFormatException("negative duration is not supported");
                }
                return parsedDuration;
            } catch (DateTimeParseException e) {
                throw new NumberFormatException(
                        "text does not start with a number, and is not a valid ISO-8601 duration format: "
                                + trimmed);
            }
        }

        final BigInteger value;
        try {
            value = new BigInteger(number); // this throws a NumberFormatException
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "The value '" + number + "' cannot be represented as an integer number.", e);
        }

        final ChronoUnit unit;
        if (unitLabel.isEmpty()) {
            unit = ChronoUnit.MILLIS;
        } else {
            unit = LABEL_TO_UNIT_MAP.get(unitLabel);
        }
        if (unit == null) {
            throw new IllegalArgumentException(
                    "Time interval unit label '"
                            + unitLabel
                            + "' does not match any of the recognized units: "
                            + TimeUnit.getAllUnits());
        }

        try {
            return convertBigIntToDuration(value, unit);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "The value '"
                            + number
                            + "' cannot be represented as Duration (numeric overflow).",
                    e);
        }
    }

    private static Duration convertBigIntToDuration(BigInteger value, ChronoUnit unit) {
        final BigInteger nanos = value.multiply(BigInteger.valueOf(unit.getDuration().toNanos()));

        final BigInteger[] dividedAndRemainder = nanos.divideAndRemainder(NANOS_PER_SECOND);
        return Duration.ofSeconds(dividedAndRemainder[0].longValueExact())
                .plusNanos(dividedAndRemainder[1].longValueExact());
    }

    private static Map<String, ChronoUnit> initMap() {
        Map<String, ChronoUnit> labelToUnit = new HashMap<>();
        for (TimeUnit timeUnit : TimeUnit.values()) {
            for (String label : timeUnit.getLabels()) {
                labelToUnit.put(label, timeUnit.getUnit());
            }
        }
        return labelToUnit;
    }

    /**
     * @param duration to convert to string
     * @return duration string in millis
     */
    public static String getStringInMillis(final Duration duration) {
        return duration.toMillis() + TimeUnit.MILLISECONDS.labels.get(0);
    }

    /**
     * Pretty prints the duration as a lowest granularity unit that does not lose precision.
     *
     * <p>Examples:
     *
     * <pre>{@code
     * Duration.ofMilliseconds(60000) will be printed as 1 min
     * Duration.ofHours(1).plusSeconds(1) will be printed as 3601 s
     * }</pre>
     *
     * <b>NOTE:</b> It supports only durations that fit into long.
     */
    public static String formatWithHighestUnit(Duration duration) {
        BigInteger nanos = toNanos(duration);

        TimeUnit highestIntegerUnit = getHighestIntegerUnit(nanos);
        return String.format(
                "%s %s",
                nanos.divide(highestIntegerUnit.getUnitAsNanos()),
                highestIntegerUnit.getLabels().get(0));
    }

    /**
     * Converted from {@link Duration#toNanos()}, but produces {@link BigInteger} and does not throw
     * an exception on overflow.
     */
    private static BigInteger toNanos(Duration duration) {
        long tempSeconds = duration.getSeconds();
        long tempNanos = duration.getNano();
        if (tempSeconds < 0) {
            // change the seconds and nano value to
            // handle Long.MIN_VALUE case
            tempSeconds = tempSeconds + 1;
            tempNanos = tempNanos - NANOS_PER_SECOND.longValue();
        }
        return BigInteger.valueOf(tempSeconds)
                .multiply(NANOS_PER_SECOND)
                .add(BigInteger.valueOf(tempNanos));
    }

    private static TimeUnit getHighestIntegerUnit(BigInteger nanos) {
        if (nanos.compareTo(BigInteger.ZERO) == 0) {
            return TimeUnit.MILLISECONDS;
        }

        final List<TimeUnit> orderedUnits =
                Arrays.asList(
                        TimeUnit.NANOSECONDS,
                        TimeUnit.MICROSECONDS,
                        TimeUnit.MILLISECONDS,
                        TimeUnit.SECONDS,
                        TimeUnit.MINUTES,
                        TimeUnit.HOURS,
                        TimeUnit.DAYS);

        TimeUnit highestIntegerUnit = null;
        for (TimeUnit timeUnit : orderedUnits) {
            if (nanos.remainder(timeUnit.getUnitAsNanos()).compareTo(BigInteger.ZERO) != 0) {
                break;
            }
            highestIntegerUnit = timeUnit;
        }

        return checkNotNull(highestIntegerUnit, "Should find a highestIntegerUnit.");
    }

    /** Enum which defines time unit, mostly used to parse value from configuration file. */
    private enum TimeUnit {
        DAYS(ChronoUnit.DAYS, singular("d"), plural("day")),
        HOURS(ChronoUnit.HOURS, singular("h"), plural("hour")),
        MINUTES(ChronoUnit.MINUTES, singular("min"), singular("m"), plural("minute")),
        SECONDS(ChronoUnit.SECONDS, singular("s"), plural("sec"), plural("second")),
        MILLISECONDS(ChronoUnit.MILLIS, singular("ms"), plural("milli"), plural("millisecond")),
        MICROSECONDS(ChronoUnit.MICROS, singular("µs"), plural("micro"), plural("microsecond")),
        NANOSECONDS(ChronoUnit.NANOS, singular("ns"), plural("nano"), plural("nanosecond"));

        private static final String PLURAL_SUFFIX = "s";

        private final List<String> labels;

        private final ChronoUnit unit;

        private final BigInteger unitAsNanos;

        TimeUnit(ChronoUnit unit, String[]... labels) {
            this.unit = unit;
            this.unitAsNanos = BigInteger.valueOf(unit.getDuration().toNanos());
            this.labels =
                    Arrays.stream(labels).flatMap(Arrays::stream).collect(Collectors.toList());
        }

        /**
         * @param label the original label
         * @return the singular format of the original label
         */
        private static String[] singular(String label) {
            return new String[] {label};
        }

        /**
         * @param label the original label
         * @return both the singular format and plural format of the original label
         */
        private static String[] plural(String label) {
            return new String[] {label, label + PLURAL_SUFFIX};
        }

        public List<String> getLabels() {
            return labels;
        }

        public ChronoUnit getUnit() {
            return unit;
        }

        public BigInteger getUnitAsNanos() {
            return unitAsNanos;
        }

        public static String getAllUnits() {
            return Arrays.stream(TimeUnit.values())
                    .map(TimeUnit::createTimeUnitString)
                    .collect(Collectors.joining(", "));
        }

        private static String createTimeUnitString(TimeUnit timeUnit) {
            return timeUnit.name() + ": (" + String.join(" | ", timeUnit.getLabels()) + ")";
        }
    }
}
