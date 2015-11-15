package com.jbirdvegas.mgerrit.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.objects.GooFileObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
 *  Author: Jon Stanford (JBirdVegas), 2013
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
public class GooFileArrayAdapter extends BaseAdapter {
    private final Context mContext;
    private final int mLayoutResourceId;
    private final List<GooFileObject> mGooFilesList;
    private final SimpleDateFormat mDateFormat;
    private final LayoutInflater mInflater;

    public GooFileArrayAdapter(Context context, int layoutResourceId, List<GooFileObject> objects) {
        this.mContext = context;
        this.mLayoutResourceId = layoutResourceId;
        this.mGooFilesList = objects;
        this.mDateFormat = new SimpleDateFormat("MM-dd-yyyy");
        this.mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mGooFilesList.size();
    }

    @Override
    public Object getItem(int position) {
        return mGooFilesList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(mLayoutResourceId, parent, false);
        }

        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        if (viewHolder == null) {
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }

        GooFileObject file = mGooFilesList.get(position);
        viewHolder.fileName.setText(file.getFileName());
        long unixDate = file.getModified();
        viewHolder.fileUpdate.setText(mDateFormat.format(new Date(unixDate)));
        return convertView;
    }

    private static class ViewHolder {
        TextView fileName;
        TextView fileUpdate;

        ViewHolder(View view) {
            fileName = (TextView) view.findViewById(R.id.goo_file_name);
            fileUpdate = (TextView) view.findViewById(R.id.goo_file_date);
        }
    }
}