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

package com.jbirdvegas.mgerrit.fragments;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.MutableDateTime;

import java.util.Calendar;

/**
 * Used for the date category refine search options for selecting an absolute date in more detail
 */
public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    public static final String DEFAULT_DATE = "default_date";
    public static final String MIN_DATE = "min_date";
    public static final String MAX_DATE = "max_date";
    private DialogListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        int day = c.get(Calendar.DAY_OF_MONTH);
        int month = c.get(Calendar.MONTH);
        int year = c.get(Calendar.YEAR);

        long minDate = 0, maxDate = 0;

        Bundle args = getArguments();
        if (args != null) {
            DateTime dateTime = (DateTime) args.getSerializable(DEFAULT_DATE);
            if (dateTime != null) {
                day = dateTime.getDayOfMonth();
                month = dateTime.getMonthOfYear();
                year = dateTime.getYear();
            }

            minDate = args.getLong(MIN_DATE);
            maxDate = args.getLong(MAX_DATE);
        }

        DatePickerDialog dialog = new DatePickerDialog(getActivity(), this, year, month, day);

        DatePicker picker = dialog.getDatePicker();
        if (minDate > 0) picker.setMinDate(minDate);
        if (maxDate > 0) picker.setMaxDate(maxDate);
        picker.setCalendarViewShown(true);

        return dialog;
    }

    /**
     * Bind a listener to this dialog to listen for changes to the selected date
     * @param listener An object which implements the DialogListener to receive callbacks
     */
    public void setListener(DialogListener listener) {
        this.mListener = listener;
    }


    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        DateTime dateTime = new DateTime(year, monthOfYear + 1, dayOfMonth, 0, 0);
        mListener.onDateChanged(dateTime);
    }

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks. */
    public interface DialogListener {
        void onDateChanged(DateTime dateTime);
    }

    public static DateTime onDateChanged(DateTime dateTime, DateTime selectedDateTime) {
        if (dateTime != null) {

            if (selectedDateTime == null) {
                selectedDateTime = dateTime;
            } else {
                /* Copy the date fields over to the selected datetime.
                 * Temporarily create a mutable datetime otherwise we will needlessly be creating
                 * immutable objects */
                MutableDateTime dt = new MutableDateTime(dateTime);
                dt.setDayOfMonth(dateTime.getDayOfMonth());
                dt.setMonthOfYear(dateTime.getMonthOfYear());
                dt.setYear(dateTime.getYear());
                selectedDateTime = dt.toDateTime();
            }
        }
        return selectedDateTime;
    }
}
