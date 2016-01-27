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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageView;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.helpers.Tools;

import org.joda.time.DateTime;
import org.joda.time.Instant;

public class BeforeCategory extends SearchCategory<BeforeSearch> {

    // The currently selected date in the CalendarView
    DateTime mSelectedDateTime = null;

    @Override
    public void setIcon(Context context, ImageView view) {
        view.setImageResource(Tools.getResIdFromAttribute(context, R.attr.dateIcon));
    }

    @Override
    public View dialogLayout(Context context, LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.search_category_date, null);
        CalendarView calendarView = (CalendarView) view.findViewById(R.id.datePicker);
        // We are not going to find any changes into the future
        long now = System.currentTimeMillis();
        calendarView.setMaxDate(now);

        // We cannot call a method to get the selected date, we need to register a listener to
        //  watch for changes to the date instead
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month,
                                            int date) {
                // month is in the range 0-11 but Jodatime expects it in 1-12
                mSelectedDateTime = new DateTime(year, month + 1, date, 0, 0);
            }
        });


        BeforeSearch keyword = getKeyword();
        if (keyword != null) {
            calendarView.setDate(keyword.getMillis());
        } else {
            calendarView.setDate(now);
        }
        return view;
    }

    @Override
    public String name(Context context) {
        return context.getString(R.string.search_category_before);
    }

    @Override
    public BeforeSearch onSave(Dialog dialog) {
        return new BeforeSearch(mSelectedDateTime.toInstant());
    }

    @Override
    public Class<BeforeSearch> getClazz() {
        return BeforeSearch.class;
    }
}
