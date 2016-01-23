package com.jbirdvegas.mgerrit.search;/*
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

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.R;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

/**
 * Base class for a category in which a search can be refined by.
 * Instances of this class associate a View with one or more
 * SearchKeywords */
public abstract class SearchCategory<K extends SearchKeyword> {

    private K mKeyword;

    public K getKeyword() {
        return mKeyword;
    }
    public void clearKeyword() {
        mKeyword = null;
    }

    public void setKeyword(K keyword) {
        mKeyword = keyword;
    }
    public void setKeyword(Context context, K keyword) {
        setKeyword(keyword);
    }

    /**
     * An id of a drawable to use for this category in the listing
     * The default is a transparent icon
     */
    public void setIcon(Context context, ImageView view) {
        view.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    /**
     * Create the layout to use with this dialog
     * @param context
     * @param inflater To inflate the view
     */
    public abstract View dialogLayout(Context context, LayoutInflater inflater);

    /**
     * A friendly name for this category used in the listing
     * @param context
     */
    @NotNull
    public abstract String name(Context context);

    /**
     * Process what was entered in the view and create a search keyword for it
     * @param dialog
     */
    public abstract K onSave(Dialog dialog);

    public final void save(Dialog dialog) {
        mKeyword = onSave(dialog);
    }

    public int viewCount() {
        return 1;
    }

    public abstract Class<K> getClazz();

    /**
     * A basic dialogLayout implementation which just contains a TextView
     * @param inflater To inflate the view
     * @return
     */
    protected View getTextDialogView(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.search_category_text, null);
        K keyword = getKeyword();
        if (keyword != null) {
            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            textView.setText(getKeyword().getParam());
        }
        return view;
    }

    /**
     * Bind a list of keywords to search categories in case filters were already active.
     *  As only one keyword of each class is supported, only the first one will be used (e.g.
     *  there can only be one ProjectSearch).
     * @param categories A list of search categories for the refine search
     * @param keywords The keywords to bind to
     */
    public static void bindKeywordsToCategories(Context context, Collection<SearchCategory> categories,
                                                Collection<SearchKeyword> keywords) {
        for (SearchCategory category : categories) {
            for (SearchKeyword keyword : keywords) {
                if (keyword.getClass().equals(category.getClazz())) {
                    category.setKeyword(context, keyword);
                    break;
                }
            }
            if (category.getKeyword() != null) break;
        }
    }
}
