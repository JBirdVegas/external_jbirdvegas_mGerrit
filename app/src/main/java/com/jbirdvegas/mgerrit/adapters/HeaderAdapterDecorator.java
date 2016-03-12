package com.jbirdvegas.mgerrit.adapters;

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

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * A delegator class for a listview with sticky headers allowing wrapping of adapters.
 * The EndlessAdapterWrapper does not implement the StickyListHeadersAdapter interface which the listview
 *  expects on the adapter from setAdapter. We can use this to delegate called methods to the relevant
 *  adapter, possibly skipping one level.
 */
public class HeaderAdapterDecorator extends ChangeListWrappable {

    BaseAdapter mWrappedBase;
    StickyListHeadersAdapter mWrappedHeaders;

    public HeaderAdapterDecorator(BaseAdapter wrappedBase, StickyListHeadersAdapter wrappedHeaders) {
        mWrappedBase = wrappedBase;
        if (wrappedHeaders == null) {
            mWrappedHeaders = (StickyListHeadersAdapter) wrappedBase;
        } else {
            mWrappedHeaders = wrappedHeaders;
        }
    }

    @Override
    public View getHeaderView(int position, View view, ViewGroup viewGroup) {
        return mWrappedHeaders.getHeaderView(position, view, viewGroup);
    }

    @Override
    public long getHeaderId(int position) {
        return mWrappedHeaders.getHeaderId(position);
    }

    @Override
    public int getCount() {
        return mWrappedBase.getCount();
    }

    @Override
    public Object getItem(int position) {
        return mWrappedBase.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return mWrappedBase.getItemId(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return mWrappedBase.getView(position, convertView, parent);
    }

    @Override
    public int getViewTypeCount() {
        return mWrappedBase.getViewTypeCount();
    }
}
