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

package com.jbirdvegas.mgerrit.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.database.Users;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;

/*
 * Used for the user autocomplete in the refine search options
 */
public class UserAdapter extends SimpleCursorAdapter {

    private final RequestQueue mRequestQuery;
    private Integer index_name = null, index_email = null;

    public UserAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        this.mRequestQuery = Volley.newRequestQueue(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (index_name == null || index_email == null) {
            index_name = cursor.getColumnIndex(Users.C_NAME);
            index_email = cursor.getColumnIndex(Users.C_EMAIL);
        }

        ViewHolder viewHolder = (ViewHolder) view.getTag();
        if (viewHolder == null) {
            viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);
        }

        String email = cursor.getString(index_email);
        GravatarHelper.populateProfilePicture(viewHolder.user, email, mRequestQuery);
        viewHolder.userName.setText(cursor.getString(index_name));
        viewHolder.userEmail.setText(email);
        super.bindView(view, context, cursor);
    }

    private class ViewHolder {
        public final TextView userName;
        public final TextView userEmail;
        public final ImageView user;

        ViewHolder(View view) {
            userName = (TextView) view.findViewById(R.id.txtUserName);
            userEmail = (TextView) view.findViewById(R.id.txtUserEmail);
            user = (ImageView) view.findViewById(R.id.imgUser);
        }
    }
}
