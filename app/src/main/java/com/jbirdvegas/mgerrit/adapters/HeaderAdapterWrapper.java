package com.jbirdvegas.mgerrit.adapters;


/*
 * Copyright (C) 2014 Android Open Kang Project (AOKP)
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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.objects.Categorizable;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * A wrapper to encapsulate the logic for adding headers to a list
 */
public class HeaderAdapterWrapper extends BaseAdapter implements StickyListHeadersAdapter {

    private LayoutInflater mInflator;

    /**
     * The child adapter that this wraps. Most of the work will be delegated to this adapter
     *  and we need to observe when its data changes.
     *
     *  The wrapped adapter must implement the Categorizable interface
     */
    private BaseAdapter wrapped;

    /**
     * An adapter that wraps this adapter so we can notify it when either the child adapter's
     *  data or this wraper's data changes.
     */
    private BaseAdapter mParentAdapter;


    public HeaderAdapterWrapper(Context context, ChangeListAdapter wrapped) {
        this.wrapped = wrapped;
        mInflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        String dateText = ((Categorizable) wrapped).categoryName(position);

        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflator.inflate(R.layout.date_card_header, parent, false);
            holder = new ViewHolder((TextView) convertView.findViewById(R.id.header));
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.text.setText(dateText);
        return convertView;
    }

    public long getHeaderId(int position) {
        return ((Categorizable) wrapped).categoryId(position);
    }

    /**
     * Returns the ListAdapter that is wrapped by this adapter
     */
    public BaseAdapter getWrappedAdapter() {
        return wrapped;
    }

    @Override
    public int getCount() {
        return wrapped.getCount();
    }

    @Override
    public Object getItem(int position) {
        return wrapped.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return wrapped.getItemId(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return wrapped.getView(position, convertView, parent);
    }

    private static class ViewHolder {
        public TextView text;

        ViewHolder(TextView text) {
            this.text = text;
        }
    }
}
