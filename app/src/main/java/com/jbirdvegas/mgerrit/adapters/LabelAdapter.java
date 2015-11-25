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
import android.widget.CursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.database.Labels;

import org.jetbrains.annotations.Nullable;

public class LabelAdapter extends CursorAdapter {

    private final Context mContext;
    private final LayoutInflater mInflater;

    private static Integer mName_index;
    private static Integer mProject_index;
    private static Integer mValue_index;
    private static Integer mDescription_index;


    public LabelAdapter(Context context, Cursor c) {
        super(context, c, 0);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContext = context;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.item_label, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        if (viewHolder == null) {
            viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);
        }
        setupIndicies(cursor);

        viewHolder.txtLabel.setText(cursor.getString(mName_index));

        // Record the adapter for the spinner against the view
        SpinnerAdapter adapter = (SpinnerAdapter) viewHolder.spnLabelRating.getTag(R.id.adapter);
        if (adapter == null) {
            adapter = new SpinnerAdapter(mContext, R.layout.item_label_values);
            viewHolder.spnLabelRating.setTag(R.id.adapter, adapter);
            viewHolder.spnLabelRating.setAdapter(adapter);
        }

        adapter.clear(); // Clear all items for now

        String currentLabel = cursor.getString(mName_index);
        String currentProject = cursor.getString(mProject_index);
        while (cursor.moveToNext() && cursor.getString(mProject_index).equals(currentProject)) {
            if (!cursor.getString(mName_index).equals(currentLabel)) break;
            String value = cursor.getString(mValue_index);
            String description = cursor.getString(mDescription_index);
            adapter.add(new LabelValues(currentLabel, value, description));
        }
        // Move back to the previous row
        cursor.moveToPrevious();
    }

    @Nullable
    @Override
    public Object getItem(int position) {
        Cursor cursor = getCursor();
        if (cursor != null) getCursor().moveToPosition(position);
        return cursor;
    }

    /**
     * Initialise the constant cursor index fields. This should always be called
     *  before trying to access these fields.
     * @param cursor A cursor from which to initialise the constant cursor indices.
     */
    private void setupIndicies(Cursor cursor) {
        if (mProject_index == null) {
            mProject_index = cursor.getColumnIndex(Labels.C_PROJECT);
        }
        if (mName_index == null) {
            mName_index = cursor.getColumnIndex(Labels.C_NAME);
        }
        if (mValue_index == null) {
            mValue_index = cursor.getColumnIndex(Labels.C_VALUE);
        }
        if (mDescription_index == null) {
            mDescription_index = cursor.getColumnIndex(Labels.C_DESCRIPTION);
        }
    }

    class ViewHolder {
        private final TextView txtLabel;
        private final Spinner spnLabelRating;

        ViewHolder(View view) {
            txtLabel = (TextView) view.findViewById(R.id.txtLabel);
            spnLabelRating = (Spinner) view.findViewById(R.id.spnLabelRating);
        }
    }

    class LabelValues {
        public String label, value, description;

        public LabelValues(String label, String value, String description) {
            this.label = label;
            this.value = value;
            this.description = description;
        }
    }

    class SpinnerAdapter extends ArrayAdapter<LabelValues> {

        public SpinnerAdapter(Context context, int resource) {
            super(context, resource);
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
}
