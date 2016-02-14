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

package com.jbirdvegas.mgerrit.search.categories;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.fragments.DatePickerFragment;
import com.jbirdvegas.mgerrit.fragments.TimePickerFragment;
import com.jbirdvegas.mgerrit.helpers.Tools;
import com.jbirdvegas.mgerrit.search.AfterSearch;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

public class AfterCategory extends SearchCategory<AfterSearch>
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
        AfterSearch keyword = getKeyword();
        /* We need to default to a time which is close, so we will pick midnight as the default
         * so the default setting will show some recent changes */
        DateTime dt = keyword != null ? keyword.getDateTime() : new DateTime().withTimeAtStartOfDay();

        View view = getDatetimeDialogView(context, inflater, this, this, dt);

        this.mContext = context;
        mTxtDate = (TextView) view.findViewById(R.id.txtSearchDate);
        mTxtTime = (TextView) view.findViewById(R.id.txtSearchTime);


        mTxtDate.setText(Tools.prettyPrintDate(mContext, dt));
        mTxtTime.setText(dt.toString("HH:mm"));

        return view;
    }

    @NonNull
    @Override
    public String name(Context context) {
        return context.getString(R.string.search_category_after);
    }

    @Override
    public AfterSearch onSave(Dialog dialog) {
        if (mSelectedDateTime == null) {
            mSelectedDateTime = new DateTime().withTimeAtStartOfDay();
        }
        return new AfterSearch(mSelectedDateTime);
    }

    @Override
    public Class<AfterSearch> getClazz() {
        return AfterSearch.class;
    }

    @Override
    public void onDateChanged(DateTime dateTime) {
        if (dateTime != null && mTxtDate != null) {
            mTxtDate.setText(Tools.prettyPrintDate(mContext, dateTime));
        }
        mSelectedDateTime = DatePickerFragment.onDateChanged(dateTime, mSelectedDateTime);
    }

    @Override
    public void onTimeChanged(LocalTime time) {
        if (time != null && mTxtTime != null) {
            if (mSelectedDateTime == null) {
                /* Needs to be set to what we defaulted it to earlier, except we don't need to
                 * set the time as it will be set below  */
                mSelectedDateTime = new DateTime();
            }
            mSelectedDateTime = mSelectedDateTime.withHourOfDay(time.getHourOfDay())
                .withMinuteOfHour(time.getMinuteOfHour());
            mTxtTime.setText(time.toString("HH:mm"));
        }
    }
}
