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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.search.SearchCategory;
import com.jbirdvegas.mgerrit.search.SearchKeyword;

import java.util.ArrayList;
import java.util.List;

public class SearchCategoryAdapter extends ArrayAdapter<SearchCategory> {

    Context mContext;
    private final LayoutInflater mInflater;
    private final List<SearchCategory> mCategories;


    public SearchCategoryAdapter(Context context, int resource, List<SearchCategory> objects) {
        super(context, resource, objects);
        this.mContext = context;
        this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mCategories = objects;
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

        SearchCategory<SearchKeyword> category = getItem(position);

        viewHolder.categoryName.setText(category.name(mContext));
        category.setIcon(mContext, viewHolder.imgCategory);

        // Need to save and restore the category on the view so we don't end up using a cached one
        convertView.setTag(R.id.searchCategory, category);
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final SearchCategory searchCategory = (SearchCategory) v.getTag(R.id.searchCategory);
                AlertDialog.Builder ad = new AlertDialog.Builder(mContext)
                        .setTitle(searchCategory.name(mContext))
                        .setView(searchCategory.dialogLayout(mContext, mInflater))
                        .setPositiveButton(R.string.search_category_option_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                               searchCategory.save((Dialog) dialog);
                                dialog.dismiss();
                                SearchCategoryAdapter.this.notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton(R.string.search_category_option_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                searchCategory.clearKeyword();
                                dialog.dismiss();
                                SearchCategoryAdapter.this.notifyDataSetChanged();
                            }
                        });
                ad.create().show();
            }
        });

        SearchKeyword keyword = category.getKeyword();
        String text;
        if (keyword != null) {
            text = keyword.describe();
            viewHolder.filters.setVisibility(View.VISIBLE);
        } else {
            text = "";
            viewHolder.filters.setVisibility(View.GONE);
        }
        viewHolder.filters.setText(text);

        return convertView;
    }

    public ArrayList<SearchKeyword> getKeywords() {
        ArrayList<SearchKeyword> keywords = new ArrayList<>();
        for (SearchCategory category : mCategories) {
            SearchKeyword keyword = category.getKeyword();
            if (keyword != null) {
                keywords.add(keyword);
            }
        }
        return keywords;
    }

    public void clear() {
        for (SearchCategory category : mCategories) {
            category.clearKeyword();
        }
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        ImageView imgCategory;
        TextView categoryName;
        TextView filters;

        ViewHolder(View view) {
            imgCategory = (ImageView) view.findViewById(R.id.imgCategory);
            categoryName = (TextView) view.findViewById(R.id.txtCategoryName);
            filters = (TextView) view.findViewById(R.id.txtFilters);
        }
    }
}
