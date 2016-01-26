/*
 * Copyright (C) 2016 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2016
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

package com.jbirdvegas.mgerrit.search;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.helpers.Tools;

public class StarredCategory extends SearchCategory<IsSearch> {

    @Override
    public void setIcon(Context context, ImageView view) {
        // Set the icon based on whether the keyword is set
        int resId;
        if (isKeywordSet()) {
            resId = R.attr.starredIcon;
        } else {
            resId = R.attr.unstarredIcon;
        }
        view.setImageResource(Tools.getResIdFromAttribute(context, resId));
    }

    @Override
    public View dialogLayout(Context context, LayoutInflater inflater) {
        return getTextDialogView(inflater);
    }

    @Override
    public String name(Context context) {
        return context.getString(R.string.search_category_starred);
    }

    @Override
    public IsSearch onSave(Dialog dialog) {
        SearchKeyword keyword = getKeyword();
        if (keyword != null) {
            return null;
        } else {
            return new IsSearch("starred");
        }
    }

    @Override
    public Class<IsSearch> getClazz() {
        return IsSearch.class;
    }

    @Override
    public ViewType getViewType() {
        return ViewType.Inline;
    }

    @Override
    public String getFilterDescription(Context context) {
        SearchKeyword keyword = getKeyword();
        if (keyword == null) return "starred and unstarred";
        return keyword.describe();
    }

    private boolean isKeywordSet() {
        SearchKeyword keyword = getKeyword();
        return keyword != null && IsSearch.OP_VALUE_STARRED.equals(keyword.getParam());
    }
}
