/*
 * Copyright (C) 2015 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2015
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

package com.jbirdvegas.mgerrit.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.fragments.PrefsFragment;
import com.jbirdvegas.mgerrit.helpers.Tools;
import com.jbirdvegas.mgerrit.objects.LabelValues;

import org.jetbrains.annotations.Nullable;

public class RatingAdapter extends ArrayAdapter<LabelValues> {

    private final LayoutInflater mInflater;
    private final int mGreen;
    private final int mRed;
    private final boolean mUsingLightTheme;

    public RatingAdapter(Context context, int resource) {
        super(context, resource);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mGreen = context.getResources().getColor(R.color.text_green);
        mRed = context.getResources().getColor(R.color.text_red);

        mUsingLightTheme = (PrefsFragment.getCurrentThemeID(context) == R.style.Theme_Light);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return setupView(convertView, parent, position, false);
    }

    @Nullable
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return setupView(convertView, parent, position, true);
    }

    private View setupView(View convertView, ViewGroup parent, int postition, boolean isDropdown) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_label_values, parent, false);
        }

        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        if (viewHolder == null) {
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }

        LabelValues labelValues = getItem(postition);
        String strVal = String.valueOf(labelValues.value);
        if (labelValues.value > 0) {
            strVal = "+" + strVal;
        }
        viewHolder.txtValue.setText(strVal);
        if (labelValues.value > 0) {
            setTextColor(mGreen, viewHolder.txtValue, viewHolder.txtDescription);
        } else if (labelValues.value < 0) {
            setTextColor(mRed, viewHolder.txtValue, viewHolder.txtDescription);
        } else {
            // Need to determine from the current theme what the default color is and set it back
            if (mUsingLightTheme) {
                int light = getContext().getResources().getColor(R.color.text_light);
                setTextColor(light, viewHolder.txtValue, viewHolder.txtDescription);
            } else {
                int dark = getContext().getResources().getColor(R.color.text_dark);
                setTextColor(dark, viewHolder.txtValue, viewHolder.txtDescription);
            }
        }

        viewHolder.txtDescription.setText(labelValues.description);
        viewHolder.txtDescription.setVisibility(isDropdown ? View.VISIBLE : View.GONE);

        return convertView;
    }

    /**
     * Set the text color of multiple text views at once.
     * @param color The colour to set the text views
     * @param views A list of text vies whose color to set
     */
    private void setTextColor(int color, TextView... views) {
        for (int i = 0; i < views.length; i++) {
            views[i].setTextColor(color);
        }
    }

    class ViewHolder {
        private final TextView txtValue;
        private final TextView txtDescription;

        ViewHolder(View view) {
            txtValue = (TextView) view.findViewById(R.id.txtValue);
            txtDescription = (TextView) view.findViewById(R.id.txtDescription);
        }
    }
}
