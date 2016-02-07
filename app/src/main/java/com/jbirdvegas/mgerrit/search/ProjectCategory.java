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
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.database.ProjectsTable;
import com.jbirdvegas.mgerrit.helpers.Tools;

public class ProjectCategory extends SearchCategory<ProjectSearch> {
    private SimpleCursorAdapter mAdapter;

    @Override
    public View dialogLayout(Context context, LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.search_category_autocomplete, null);
        ProjectSearch keyword = getKeyword();
        AutoCompleteTextView textView = (AutoCompleteTextView) view.findViewById(R.id.autoComplete);
        if (keyword != null) {
            textView.setText(getKeyword().getParam());
        }

        setupAdapter(context);
        textView.setAdapter(mAdapter);
        return view;
    }

    @NonNull
    @Override
    public String name(Context context) {
        return context.getString(R.string.search_category_project);
    }

    @Override
    public void setIcon(Context context, ImageView view) {
        view.setImageResource(Tools.getResIdFromAttribute(context, R.attr.projectsIcon));
    }

    @Override
    public ProjectSearch onSave(Dialog dialog) {
        EditText text = (EditText) dialog.findViewById(R.id.autoComplete);
        String s = text.getText().toString();
        if (s.length() > 0) return new ProjectSearch(s);
        else return null;
    }

    @Override
    public Class<ProjectSearch> getClazz() {
        return ProjectSearch.class;
    }

    /**
     * Setup the autocomplete adapter
     */
    private void setupAdapter(final Context context) {
        mAdapter = new SimpleCursorAdapter(context, android.R.layout.simple_expandable_list_item_2, null,
                new String[] {ProjectsTable.C_ROOT, ProjectsTable.C_SUBPROJECT}, new int[] { android.R.id.text1, android.R.id.text2}, 0);

        mAdapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
            @Override
            public CharSequence convertToString(Cursor cursor) {
                final int index = cursor.getColumnIndexOrThrow(ProjectsTable.C_PATH);
                return cursor.getString(index);
            }
        });

        mAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence description) {
                return ProjectsTable.searchProjects(context, description.toString());
            }
        });
    }
}
