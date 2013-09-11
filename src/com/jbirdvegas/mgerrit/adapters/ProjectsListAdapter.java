package com.jbirdvegas.mgerrit.adapters;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2013
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
import android.database.Cursor;
import android.widget.SimpleCursorTreeAdapter;

import com.jbirdvegas.mgerrit.database.DatabaseFactory;
import com.jbirdvegas.mgerrit.database.ProjectsTable;

public class ProjectsListAdapter extends SimpleCursorTreeAdapter
{
    private Context mContext;
    private String mSubproject;

    // Note that the constructor does not take a Cursor. This is done to avoid
    // querying the database on the main thread.
    public ProjectsListAdapter(Context context,
                               int groupLayout,
                               String[] groupFrom,
                               int[] groupTo,
                               int childLayout,
                               String[] childFrom,
                               int[] childTo) {
        super(context, null, groupLayout, groupFrom, groupTo, childLayout,
                childFrom, childTo);
        mContext = context;
    }

    public void setSubprojectQuery(String query) {
        mSubproject = query;
    }

    @Override
    protected Cursor getChildrenCursor(Cursor groupCursor)
    {
        /* (TODO) Note: It is your responsibility to manage this Cursor through the Activity lifecycle.
         * It is a good idea to use Activity.managedQuery which will handle this for you.
         * In some situations, the adapter will deactivate the Cursor on its own, but this will
         *  not always be the case, so please ensure the Cursor is properly managed.
         */

        // TODO: The column index will always be constant here
        String root = groupCursor.getString(groupCursor.getColumnIndex(ProjectsTable.C_ROOT));
        return ProjectsTable.getSubprojects(mContext, root, mSubproject);
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true; // All children (projects) are selectable
    }

    public String getGroupName(int groupPosition) {
        Cursor c = getGroup(groupPosition);
        return c.getString(c.getColumnIndex(ProjectsTable.C_ROOT));
    }
}
