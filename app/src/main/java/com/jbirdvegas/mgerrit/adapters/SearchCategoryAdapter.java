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
import android.widget.ImageView;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.search.SearchCategory;

import java.util.List;

public class SearchCategoryAdapter extends ArrayAdapter<SearchCategory> {

    Context mContext;
    private final LayoutInflater mInflater;

    // When a popup menu is expanded, this is set to the view where all the associated data is bound
    private View mPopupMenuTagHolder;


    public SearchCategoryAdapter(Context context, int resource, List<SearchCategory> objects) {
        super(context, resource, objects);
        this.mContext = context;
        this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_search_category, parent, false);
        }

        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        if (viewHolder == null) {
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }

        SearchCategory category = getItem(position);

        viewHolder.categoryName.setText(category.name(mContext));
        int icon = category.drawableId(mContext);
        if (icon > 0) {
            viewHolder.imgCategory.setImageResource(icon);
        }

        return convertView;
    }

    private static class ViewHolder {
        ImageView imgCategory;
        TextView categoryName;

        ViewHolder(View view) {
            imgCategory = (ImageView) view.findViewById(R.id.imgCategory);
            categoryName = (TextView) view.findViewById(R.id.txtCategoryName);
        }
    }
}
