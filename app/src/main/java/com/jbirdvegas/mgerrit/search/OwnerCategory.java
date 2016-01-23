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
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.adapters.UserAdapter;
import com.jbirdvegas.mgerrit.database.Users;
import com.jbirdvegas.mgerrit.helpers.Tools;
import com.jbirdvegas.mgerrit.objects.UserAccountInfo;

public class OwnerCategory extends SearchCategory<OwnerSearch> {
    private SimpleCursorAdapter mAdapter;

    @Override
    public View dialogLayout(Context context, LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.search_category_autocomplete, null);
        OwnerSearch keyword = getKeyword();
        AutoCompleteTextView textView = (AutoCompleteTextView) view.findViewById(R.id.autoComplete);
        if (keyword != null) {
            textView.setText(getKeyword().getParam());
        }

        setupAdapter(context);
        textView.setAdapter(mAdapter);
        return view;
    }

    @Override
    public String name(Context context) {
        return context.getString(R.string.search_category_owner);
    }

    @Override
    public void setIcon(Context context, ImageView view) {
        view.setImageResource(Tools.getResIdFromAttribute(context, R.attr.userIcon));
    }

    @Override
    public OwnerSearch onSave(Dialog dialog) {
        EditText text = (EditText) dialog.findViewById(R.id.autoComplete);
        String s = text.getText().toString();
        if (s.length() > 0) return new OwnerSearch(s);
        else return null;
    }

    @Override
    public Class<OwnerSearch> getClazz() {
        return OwnerSearch.class;
    }

    // Override the setKeyword method so we set it in pretty format
    @Override
    public void setKeyword(Context context, OwnerSearch keyword) {
        if (keyword == null) super.setKeyword(null);
        else if (keyword.isValid()) {
            try {
                Integer userId = Integer.parseInt(keyword.getParam());
                UserAccountInfo user = Users.getUser(context, userId);
                if (user != null) {
                    keyword = new OwnerSearch(String.format(context.getString(R.string.search_category_owner_format),
                            user.name, user.email));
                }
            } catch (NumberFormatException e) {
                // This must not be a user id, set the keyword as is
            } finally {
                super.setKeyword(keyword);
            }
        }
    }

    /**
     * Setup the autocomplete adapter
     */
    private void setupAdapter(final Context context) {
        // TODO
        mAdapter = new UserAdapter(context, R.layout.item_user, null,
                new String[] {Users.C_NAME, Users.C_EMAIL}, new int[] { R.id.txtUserName, R.id.txtUserEmail}, 0);

        mAdapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
            @Override
            public CharSequence convertToString(Cursor cursor) {
                final int emailIndex = cursor.getColumnIndexOrThrow(Users.C_EMAIL);
                final int nameIndex = cursor.getColumnIndexOrThrow(Users.C_NAME);
                return String.format(context.getString(R.string.search_category_owner_format),
                        cursor.getString(nameIndex), cursor.getString(emailIndex));
            }
        });

        mAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                return Users.searchUsers(context, constraint.toString());
            }
        });
    }

}
