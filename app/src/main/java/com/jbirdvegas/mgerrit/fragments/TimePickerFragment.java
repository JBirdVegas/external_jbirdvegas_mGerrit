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

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.TimePicker;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import java.util.Calendar;

/**
 * Used for the date category refine search options for selecting an absolute date in more detail
 */
public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    public static final String DEFAULT_TIME = "default";
    private DialogListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        Bundle args = getArguments();
        if (args != null) {
            DateTime dateTime = (DateTime) args.getSerializable(DEFAULT_TIME);
            if (dateTime != null) {
                hour = dateTime.getHourOfDay();
                minute = dateTime.getMinuteOfHour();
            }
        }

        return new TimePickerDialog(getActivity(), this, hour, minute, true);
    }

    /**
     * Bind a listener to this dialog to listen for changes to the selected date
     * @param listener An object which implements the DialogListener to receive callbacks
     */
    public void setListener(DialogListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        mListener.onTimeChanged(new LocalTime(hourOfDay, minute));
    }

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks. */
    public interface DialogListener {
        void onTimeChanged(LocalTime time);
    }
}
