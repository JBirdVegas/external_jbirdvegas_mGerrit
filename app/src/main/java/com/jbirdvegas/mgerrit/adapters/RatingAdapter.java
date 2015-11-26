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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.objects.LabelValues;

import org.jetbrains.annotations.Nullable;

public class RatingAdapter extends ArrayAdapter<LabelValues> {

    private final LayoutInflater mInflater;

    public RatingAdapter(Context context, int resource) {
        super(context, resource);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return setupView(convertView, parent, false);
    }

    @Nullable
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return setupView(convertView, parent, true);
    }

    private View setupView(View convertView, ViewGroup parent, boolean isDropdown) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_label_values, parent, false);
        }

        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        if (viewHolder == null) {
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }

        viewHolder.txtDescription.setVisibility(isDropdown ? View.VISIBLE : View.GONE);

        return convertView;
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
