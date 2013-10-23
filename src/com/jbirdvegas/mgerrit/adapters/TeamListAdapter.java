package com.jbirdvegas.mgerrit.adapters;

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

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.objects.CheckableView;
import com.jbirdvegas.mgerrit.objects.GerritDetails;

import java.util.List;

public class TeamListAdapter extends ArrayAdapter<GerritDetails> {

    private LayoutInflater mInflator;
    private ViewHolder gerritPlaceholder;
    private String gerritName, gerritUrl = "https://";

    private List<GerritDetails> data;

    public TeamListAdapter(Context context, List<GerritDetails> objects) {
        super(context, R.layout.gerrit_row, objects);
        mInflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        data = objects;
    }

    /**
     * @Override
     * Get the data item associated with the specified position in the data set.
     * @param position Position of the item whose data we want within the adapter's data set.
     * @return The data at the specified position, or null if there is no data
     *  bound to the position
     */
    public GerritDetails getItem(int position) {
        // Override this so we don't get out of range exceptions
        if (position == data.size()) {
            if (gerritName == null) {
                gerritName = "";
            }
            if (gerritUrl == null) {
                gerritUrl = "";
            }
            return new GerritDetails(gerritName, gerritUrl);
        }
        return data.get(position);
    }

    /**
     * Search for a url in the list (not the placeholder)
     * @param gerritUrl The Gerrit URL to search for
     * @return The position in the list or -1 if not found
     */
    public int findItem(String gerritUrl) {
        int pos = -1;
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).equals(gerritUrl)) {
                pos = i;
            }
        }
        return pos;
    }

    @Override
    public int getCount() {
        // We want one more view at the end of the list to add another item
        return data.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        // Either it is in the list or it isn't
        return position < data.size() ? 0 : 1;
    }

    @Override
    public int getViewTypeCount() {
        return 2; // Two different view layouts supported
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        String gerritUrl;
        GerritDetails rowData;

        // Row specific handling
        if (getItemViewType(position) == 0) {
            if (convertView == null) {
                convertView = mInflator.inflate(R.layout.gerrit_row, null);
            }

            viewHolder = (ViewHolder) convertView.getTag();
            if (viewHolder == null) {
                viewHolder = new ViewHolder((CheckableView) convertView);
                viewHolder.gerritName = (TextView) convertView.findViewById(R.id.txtGerritName);
                viewHolder.gerritUrl = (TextView) convertView.findViewById(R.id.txtGerritURL);
                convertView.setTag(viewHolder);
            }

            rowData = getItem(position);
            viewHolder.gerritName.setText(rowData.getGerritName());
            viewHolder.gerritUrl.setText(rowData.getGerritUrl());

            final ListView lv = (ListView) parent;
            viewHolder.row.setChecked(lv.isItemChecked(position));

        } else {
            if (convertView == null) {
                convertView = mInflator.inflate(R.layout.add_team_row, null);
            }

            viewHolder = (ViewHolder) convertView.getTag();
            if (viewHolder == null) {
                viewHolder = new ViewHolder((CheckableView) convertView);
                viewHolder.gerritEditName = (EditText) convertView.findViewById(R.id.add_team_name_edittext);
                viewHolder.gerritEditUrl = (EditText) convertView.findViewById(R.id.add_team_url_edittext);
                convertView.setTag(viewHolder);
            }
            gerritPlaceholder = viewHolder;

            rowData = getItem(position);

            final ListView lv = (ListView) parent;
            viewHolder.row.setChecked(lv.isItemChecked(position));

            /* Cannot set an onClick listener here, it will be ignored when the edit event
             * is handled.
             * Listview.setItemChecked requests a layout pass after setting an item's
             *  Checked state. This means that all further (unhandled) events on this
             *  item are lost. */
            View.OnTouchListener touchListener = new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (MotionEvent.ACTION_DOWN == event.getAction()) {
                        if (!lv.isItemChecked(position))
                            lv.setItemChecked(position, true);
                    }
                    return false;
                }
            };

            // If you set a listener here, it disables the EditText field
            viewHolder.gerritEditName.setOnTouchListener(touchListener);
            viewHolder.gerritEditUrl.setOnTouchListener(touchListener);

            // Bind the data (if there is any)
            viewHolder.gerritEditName.setText(rowData.getGerritName());
            viewHolder.gerritEditUrl.setText(rowData.getGerritUrl());

            // Set the text watchers so we can save the data before it gets recycled
            viewHolder.gerritEditName.addTextChangedListener(edtNameWatcher);
            viewHolder.gerritEditUrl.addTextChangedListener(edtUrlWatcher);

            viewHolder.gerritChecked.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    lv.setItemChecked(position, !lv.isItemChecked(position));
                }
            });
        }

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    TextWatcher edtNameWatcher = new SimpleTextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            gerritName = s.toString();
        }
    };

    TextWatcher edtUrlWatcher = new SimpleTextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            gerritUrl = s.toString();
        }
    };

    private static class ViewHolder {
        final CheckableView row;
        TextView gerritName;
        TextView gerritUrl;
        EditText gerritEditName;
        EditText gerritEditUrl;
        RadioButton gerritChecked;

        private ViewHolder(CheckableView view) {
            row = view;
            gerritChecked = (RadioButton) view.findViewById(R.id.chk_gerrit_selected);
        }
    }

    // Provide empty implementations of the TextWatcher callbacks not being used.
    private abstract class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Not used
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Not used
        }
    }
}
