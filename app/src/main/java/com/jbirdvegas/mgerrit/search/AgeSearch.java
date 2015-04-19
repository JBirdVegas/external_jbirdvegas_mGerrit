package com.jbirdvegas.mgerrit.search;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2013
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.objects.ServerVersion;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.DurationFieldType;
import org.joda.time.Instant;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AgeSearch extends SearchKeyword implements Comparable<AgeSearch> {

    public static final String OP_NAME = "age";

    // Set only if a relative time period was given. Should not be set if mInstant is set
    private Period mPeriod;
    // Set only if an absolute time was given. Should not be set if mPeriod is set
    private Instant mInstant;

    /**
     * An array of supported time units and their corresponding meaning
     *  as a DurationFieldType. This is used in the parsing of the query
     *  parameter.
     */
    private static final HashMap<String, DurationFieldType> replacers;
    static {
        replacers = new HashMap<>();
        replacers.put("s", DurationFieldType.seconds());
        replacers.put("sec", DurationFieldType.seconds());
        replacers.put("secs", DurationFieldType.seconds());
        replacers.put("second", DurationFieldType.seconds());
        replacers.put("seconds", DurationFieldType.seconds());

        replacers.put("m", DurationFieldType.minutes());
        replacers.put("min", DurationFieldType.minutes());
        replacers.put("mins", DurationFieldType.minutes());
        replacers.put("minute", DurationFieldType.minutes());
        replacers.put("minutes", DurationFieldType.minutes());

        replacers.put("h", DurationFieldType.hours());
        replacers.put("hr", DurationFieldType.hours());
        replacers.put("hrs", DurationFieldType.hours());
        replacers.put("hour", DurationFieldType.hours());
        replacers.put("hours", DurationFieldType.hours());

        replacers.put("d", DurationFieldType.days());
        replacers.put("day", DurationFieldType.days());
        replacers.put("days", DurationFieldType.days());

        replacers.put("w", DurationFieldType.weeks());
        replacers.put("week", DurationFieldType.weeks());
        replacers.put("weeks", DurationFieldType.weeks());

        replacers.put("mon", DurationFieldType.months());
        replacers.put("mons", DurationFieldType.months());
        replacers.put("mth", DurationFieldType.months());
        replacers.put("mths", DurationFieldType.months());
        replacers.put("month", DurationFieldType.months());
        replacers.put("months", DurationFieldType.months());

        replacers.put("y", DurationFieldType.years());
        replacers.put("yr", DurationFieldType.years());
        replacers.put("yrs", DurationFieldType.years());
        replacers.put("year", DurationFieldType.years());
        replacers.put("years", DurationFieldType.years());
    }

    /** Used for serialising the period into a string and must be output
     *   in a format that can be re-parsed later */
    protected static PeriodFormatter periodParser = new PeriodFormatterBuilder()
            .appendYears().appendSuffix(" years ")
            .appendMonths().appendSuffix(" months ")
            .appendWeeks().appendSuffix(" weeks ")
            .appendDays().appendSuffix(" days ")
            .appendHours().appendSuffix(" hours ")
            .appendMinutes().appendSuffix(" minutes ")
            .appendSeconds().appendSuffix(" seconds")
            .toFormatter();


    protected static final DateTimeFormatter sInstantFormatter;

    static {
        registerKeyword(OP_NAME, AgeSearch.class);

        sInstantFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").withLocale(Locale.US);
    }

    public AgeSearch(String param, String operator) {
        super(OP_NAME, operator, param);
        parseDate(param);
    }

    public AgeSearch(String param) {
        // We need to extract the operator and the parameter from the string
        this(extractParameter(param), extractOperator(param));
    }

    public AgeSearch(long timestamp, String operator) {
        super(OP_NAME, operator, String.valueOf(timestamp));
        mInstant = new Instant(timestamp);
        mPeriod = null;
    }

    @Override
    public String buildSearch() {
        String operator = getOperator();
        if ("=".equals(operator)) {
            /* Note that since datetime is an SQLite function it must be included
             *  directly in the query */
            return UserChanges.C_UPDATED + " BETWEEN datetime(?) AND datetime(?)";
        } else {
            return UserChanges.C_UPDATED + " " + operator + " datetime(?)";
        }
    }

    @Override
    public String[] getEscapeArgument() {
        DateTime now = new DateTime();
        Period period = mPeriod;

        // Equals: we need to do some arithmetic to get a range from the period
        if ("=".equals(getOperator())) {
            if (period == null) {
                period = new Period(mInstant, Instant.now());
            }
            DateTime earlier = now.minus(adjust(period, +1));
            DateTime later = now.minus(period);
            return new String[] { earlier.toString(), later.toString() };
        } else {
            if (period == null) {
                return new String[] { mInstant.toString() };
            }
            return new String[] { now.minus(period).toString() };
        }
    }

    private static String extractParameter(String param) {
        return param.replaceFirst("[=<>]+", "");
    }

    private void parseDate(String param) {
        try {
            if (param.endsWith("Z")) {
                /* The string representation of an instant includes a Z at the end, but this is not
                 *  a valid format for the parser. */
                String newParam = param.substring(0, param.length() - 1);
                mInstant = Instant.parse(newParam, ISODateTimeFormat.localDateOptionalTimeParser());
            } else {
                mInstant = Instant.parse(param, ISODateTimeFormat.localDateOptionalTimeParser());
            }
            mPeriod = null;
        } catch (IllegalArgumentException ignored) {
            mPeriod = toPeriod(param);
            mInstant = null;
        }
    }

    @Override
    public String toString() {
        return toString(OP_NAME);
    }

    public String toString(String keywordName) {
        /* Use the same format as the one initially provided. I.e. if a
         *  relative time period was set initially, we want a relative period
         *  to come back out. Otherwise it is a different search */
        String operator = getOperator() ;
        if ("=".equals(operator)) operator = "";
        String string = keywordName + ":\"" + operator;
        if (mPeriod != null) {
            return string + periodParser.print(mPeriod) + '"';
        } else {
            return string + mInstant.toString() + '"';
        }
    }

    /**
     * Parses a string into a Period object according to the replacers
     *  mapping. This allows for duplicate fields (e.g. seconds being
     *  declared twice as in "2s 3 sec") with the duplicate fields being
     *  added together (the above example would be the same as "5 seconds").
     * The order of the fields is not important.
     *
     * @param dateOffset The parameter without the operator. If the operator
     *                   is passed in it will be ignored
     * @return A period corresponding to the parsed input string
     */
    private Period toPeriod(final String dateOffset) {
        String regexp = "(\\d+) *([a-zA-z]+)";
        Period period = new Period();

        if (dateOffset == null || dateOffset.isEmpty())
            return period;

        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(dateOffset);
        while (matcher.find()) {
            String svalue = matcher.toMatchResult().group(1);
            DurationFieldType fieldType = replacers.get(matcher.toMatchResult().group(2));
            if (fieldType != null) {
                // Note that both these methods do not modify their objects
                period = period.withFieldAdded(fieldType, Integer.parseInt(svalue));
            }
        }

        return period;
    }

    /**
     * Adds adjustment to the shortest set time range in period. E.g.
     *  period("5 days 3 hours", 1) -> "5 days 4 hours". This will fall
     *  back to adjusting years if no field in the period is set.
     * @param period The period to be adjusted
     * @param adjustment The adjustment. Note that positive values will result
     *                   in larger periods and an earlier time
     * @return The adjusted period
     */
    private Period adjust(final Period period, int adjustment) {
        if (adjustment == 0) return period;

        // Order is VERY important here
        LinkedHashMap<Integer, DurationFieldType> map = new LinkedHashMap<>();
        map.put(period.getSeconds(), DurationFieldType.seconds());
        map.put(period.getMinutes(), DurationFieldType.minutes());
        map.put(period.getHours(), DurationFieldType.hours());
        map.put(period.getDays(), DurationFieldType.days());
        map.put(period.getWeeks(), DurationFieldType.weeks());
        map.put(period.getMonths(), DurationFieldType.months());
        map.put(period.getYears(), DurationFieldType.years());

        for (Map.Entry<Integer, DurationFieldType> entry : map.entrySet()) {
            if (entry.getKey() > 0) {
                return period.withFieldAdded(entry.getValue(), adjustment);
            }
        }
        // Fall back to modifying years
        return period.withFieldAdded(DurationFieldType.years(), adjustment);
    }

    /**
     * Calculates the number of days spanned in a period assuming 365 days per year, 30 days per
     * month, 7 days per week, 24 hours per day, 60 minutes per hour and 60 seconds per minute.
     * @param period A period to retrieve the number of standard days for
     * @return The number of days spanned by the period.
     */
    protected static int getDaysInPeriod(final Period period) {
        int totalDays = 0;
        Period temp = new Period(period);
        if (period.getYears() > 0) {
            int years = period.getYears();
            totalDays += 365*years;
            temp = temp.minusYears(years);
        }
        if (period.getMonths() > 0) {
            int months = period.getMonths();
            totalDays += 30*period.getMonths();
            temp = temp.minusMonths(months);
        }
        return totalDays + temp.toStandardDays().getDays();
    }

    @Override
    public String getGerritQuery(ServerVersion serverVersion) {
        String operator = getOperator();

        if ("<=".equals(operator) || "<".equals(operator)) {
            return BeforeSearch._getGerritQuery(this, serverVersion);
        } else if (">=".equals(operator) || ">".equals(operator)) {
            return AfterSearch._getGerritQuery(this, serverVersion);
        }

        if (serverVersion != null &&
                serverVersion.isFeatureSupported(ServerVersion.VERSION_BEFORE_SEARCH) &&
                mInstant != null) {
            // Use a combination of before and after to get an interval
            if (mPeriod == null) {
                mPeriod = new Period(mInstant, Instant.now());
            }
            DateTime now = new DateTime();
            DateTime earlier = now.minus(adjust(mPeriod, +1));
            DateTime later = now.minus(mPeriod);

            SearchKeyword newer = new AfterSearch(earlier.toString());
            SearchKeyword older = new BeforeSearch(later.toString());
            return newer.getGerritQuery(serverVersion) + "+" + older.getGerritQuery(serverVersion);
        } else {
            // Need to leave off the operator and make sure we are using relative format
            /* Gerrit only supports specifying one time unit, so we will normalize the period
             *  into days.  */
            return OP_NAME + ":" + String.valueOf(toDays()) + "d";
        }
    }

    protected int toDays() {
        // Need to leave off the operator and make sure we are using relative format
        Period period = mPeriod;
        if (period == null) {
            period = new Period(mInstant, Instant.now());
        }
            /* Gerrit only supports specifying one time unit, so we will normalize the period
             *  into days.  */
        return getDaysInPeriod(period);
    }

    protected Period getPeriod() { return mPeriod; }
    protected Instant getInstant() { return mInstant; }

    protected static Instant getInstantFromPeriod(Period period) {
        Instant now = new Instant();
        Duration duration = period.toDurationTo(now);
        return now.minus(duration);
    }

    @Override
    public int compareTo(AgeSearch rhs) {
        if (this.equals(rhs)) return 0;
        else if (mInstant != null && rhs.mInstant != null) {
            return mInstant.compareTo(rhs.mInstant);
        } else {
            // Compare the normalised period format (i.e. the period in days)
            return toDays() - rhs.toDays();
        }
    }
}
