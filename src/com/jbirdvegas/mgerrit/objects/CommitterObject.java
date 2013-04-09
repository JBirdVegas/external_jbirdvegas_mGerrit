package com.jbirdvegas.mgerrit.objects;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 4/4/13 12:00 AM
 */
public class CommitterObject {
    private final String mName;
    private final String mEmail;
    private final String mDate;
    private final String mTimezone;

    private CommitterObject(String name,
                           String email,
                           String date,
                           String timezone) {
        mName = name;
        mEmail = email;
        mDate = date;
        mTimezone = timezone;
    }

    public static CommitterObject getInstance(String name,
                              String email,
                              String date,
                              String timezone) {
        return new CommitterObject(name, email, date, timezone);
    }

    public String getName() {
        return mName;
    }

    public String getEmail() {
        return mEmail;
    }

    public String getDate() {
        return mDate;
    }

    public String getTimezone() {
        return mTimezone;
    }
}
