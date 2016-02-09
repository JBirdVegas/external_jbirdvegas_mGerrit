/*
 * Copyright (C) 2016 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2016
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

package com.jbirdvegas.mgerrit.search;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.fragments.DatePickerFragment;
import com.jbirdvegas.mgerrit.fragments.TimePickerFragment;
import com.jbirdvegas.mgerrit.helpers.Tools;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.joda.time.MutableDateTime;

import java.util.Locale;

public class BeforeCategory extends SearchCategory<BeforeSearch>
        implements DatePickerFragment.DialogListener, TimePickerFragment.DialogListener {

    // The currently selected date in the CalendarView
    private DateTime mSelectedDateTime = null;
    private TextView mTxtDate, mTxtTime;
    private Context mContext;

    @Override
    public void setIcon(Context context, ImageView view) {
        view.setImageResource(Tools.getResIdFromAttribute(context, R.attr.dateIcon));
    }

    @Override
    public View dialogLayout(final Context context, final LayoutInflater inflater) {
        this.mContext = context;

        View view = inflater.inflate(R.layout.search_category_date_absolute, null);

        mTxtDate = (TextView) view.findViewById(R.id.txtSearchDate);
        mTxtTime = (TextView) view.findViewById(R.id.txtSearchTime);

        BeforeSearch keyword = getKeyword();
        final DateTime dt = (keyword != null) ? keyword.getDateTime() : DateTime.now();

        mTxtDate.setText(prettyPrintDate(dt));
        mTxtTime.setText(dt.toString("kk:mm"));

        final AppCompatActivity activity = (AppCompatActivity) context;

        mTxtDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerFragment newFragment = new DatePickerFragment();
                Bundle bundle = new Bundle();
                bundle.putSerializable(DatePickerFragment.DEFAULT_DATE, dt);
                // We are not going to find any changes into the future
                bundle.putLong(DatePickerFragment.MAX_DATE, System.currentTimeMillis());
                newFragment.setArguments(bundle);

                newFragment.setListener(BeforeCategory.this);
                newFragment.show(activity.getFragmentManager(), "datePicker");
            }
        });

        mTxtTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerFragment newFragment = new TimePickerFragment();
                Bundle bundle = new Bundle();
                bundle.putSerializable(TimePickerFragment.DEFAULT_TIME, dt);
                newFragment.setArguments(bundle);

                newFragment.setListener(BeforeCategory.this);
                newFragment.show(activity.getFragmentManager(), "timePicker");
            }
        });

        return view;
    }

    @NonNull
    @Override
    public String name(Context context) {
        return context.getString(R.string.search_category_before);
    }

    @Override
    public BeforeSearch onSave(Dialog dialog) {
        if (mSelectedDateTime != null) {
            return new BeforeSearch(mSelectedDateTime);
        } else {
            // We default to now, so that is what must have been selected as the time has not changed
            return new BeforeSearch(DateTime.now());
        }
    }

    @Override
    public Class<BeforeSearch> getClazz() {
        return BeforeSearch.class;
    }

    @Override
    public void onDateChanged(DateTime dateTime) {
        if (dateTime != null && mTxtDate != null) {
            mTxtDate.setText(prettyPrintDate(dateTime));
            if (mSelectedDateTime == null) {
                mSelectedDateTime = dateTime;
            } else {
                /* Copy the date fields over to the selected datetime.
                 * Temporarily create a mutable datetime otherwise we will needlessly be creating
                 * immutable objects */
                MutableDateTime dt = new MutableDateTime(dateTime);
                dt.setDayOfMonth(dateTime.getDayOfMonth());
                dt.setMonthOfYear(dateTime.getMonthOfYear());
                dt.setYear(dateTime.getYear());
                mSelectedDateTime = dt.toDateTime();
            }
        }
    }

    @Override
    public void onTimeChanged(LocalTime time) {
        if (time != null && mTxtTime != null) {
            if (mSelectedDateTime == null) mSelectedDateTime = DateTime.now();

            mSelectedDateTime = mSelectedDateTime.withHourOfDay(time.getHourOfDay())
                .withMinuteOfHour(time.getMinuteOfHour());
            mTxtTime.setText(time.toString("kk:mm"));
        }
    }

    /**
     * Pretty print the date portion of a datetime (not hour/minute) using the user's locale
     * @param dateTime A datetime
     * @return A string
     */
    public @NonNull
    String prettyPrintDate(@NonNull DateTime dateTime) {
        String dateFormat = mContext.getResources().getString(R.string.header_date_format);
        return dateTime.toString(dateFormat, Locale.getDefault());
    }
}
