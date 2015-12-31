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
import android.view.LayoutInflater;
import android.view.View;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Base class for a category in which a search can be refined by.
 * Instances of this class associate a View with one or more
 * SearchKeywords */
public abstract class SearchCategory<K extends SearchKeyword> {

    ArrayList<K> mKeywords;

    public ArrayList<K> getKeywords() {
        return mKeywords;
    }

    /**
     * An id of a drawable to use for this category in the listing
     */
    public abstract int drawableId(Context context);

    /**
     * Create the layout to use with this dialog
     * @param inflater
     */
    public abstract View dialogLayout(LayoutInflater inflater);

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
    public abstract SearchKeyword onSave(Dialog dialog);

    /**
     * Add a search keyword to the list of keywords.
     * The default implementation will add a keyword if there are none of that class
     * @param keyword The keyword to add
     * @return Whether this keyword could be added successfully
     */
    protected boolean addSearchKeyword(K keyword) {
        for (K token : mKeywords) {
            if (token.getClass().equals(keyword.getClass())) {
                return false;
            }
        }
        mKeywords.add(keyword);
        return true;
    }

    public int viewCount() {
        return 1;
    }
}
