package com.jbirdvegas.mgerrit.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.database.FileChanges;
import com.jbirdvegas.mgerrit.helpers.Tools;

import org.jetbrains.annotations.Nullable;

/*
 * Copyright (C) 2014 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2014
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
 *
 *  Adapter for a file selector, used in DiffViewer.
 */
public class FileAdapter extends CursorAdapter {

    private final Context mContext;
    private final LayoutInflater mInflator;
    private final boolean mUsingLightTheme;

    private static Integer mStatus_index;
    private static Integer mPath_index;
    private static Integer mInserted_index;
    private static Integer mDeleted_index;


    public FileAdapter(Context context, Cursor c) {
        super(context, c, 0);
        mInflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mContext = context;
        mUsingLightTheme = (Prefs.getCurrentThemeID(context) == R.style.Theme_Light);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflator.inflate(R.layout.diff_files_row, null);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        if (viewHolder == null) {
            viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);
        }
        setupIndicies(cursor);
        colorPath(viewHolder.path, cursor);

        // Remove the path from the filename
        String filename = cursor.getString(mPath_index);
        int idx = filename.lastIndexOf("/");
        filename = idx >= 0 ? filename.substring(idx + 1) : filename;
        viewHolder.path.setText(filename);

    }

    @Nullable
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflator.inflate(R.layout.diff_files_dropdown_row, null);
        }

        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        if (viewHolder == null) {
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }

        Cursor cursor = (Cursor) getItem(position);
        if (cursor == null) return convertView;

        setupIndicies(cursor);

        colorPath(viewHolder.path, cursor);
        viewHolder.path.setText(cursor.getString(mPath_index));

        int inserted = cursor.getInt(mInserted_index);
        setTextOrHide(viewHolder.inserted, viewHolder.insertedContainer, inserted);

        int deleted = cursor.getInt(mDeleted_index);
        setTextOrHide(viewHolder.deleted, viewHolder.deletedContainer, deleted);

        return convertView;
    }

    @Nullable
    @Override
    public Object getItem(int position) {
        Cursor cursor = getCursor();
        if (cursor != null) getCursor().moveToPosition(position);
        return cursor;
    }

    public int getPreviousPosition(int currentPosition) {
        if (currentPosition < 1) return -1;
        else return currentPosition - 1;
    }

    public int getNextPosition(int currentPosition) {
        if (currentPosition + 1 >= getCount()) return -1;
        else return currentPosition + 1;
    }

    /**
     * @param position An unsigned integer
     * @return The file path of the item at that position
     *  or null if the position is invalid
     */
    public String getPathAtPosition(int position) {
        if (position >= 0 && position < getCount()) {
            Cursor cursor = (Cursor) getItem(position);
            if (cursor != null) {
                return cursor.getString(mPath_index);
            }
        }
        return null;
    }

    /**
     * @param fileName A file path
     * @return The position of the given file in the cursor, returns
     *  -1 if the file does not exist.
     */
    public int getPositionOfFile(String fileName) {
        Cursor cursor = getCursor();
        if (cursor != null) {
            setupIndicies(cursor);
            while (cursor.moveToNext()) {
                if (cursor.getString(mPath_index).equals(fileName)) {
                    return cursor.getPosition();
                }
            }
        }
        return -1;
    }

    private void colorPath(TextView view, Cursor cursor) {
        Tools.colorPath(mContext.getResources(), view,
                cursor.getString(mStatus_index), mUsingLightTheme);
    }

    private void setTextOrHide(TextView textView, View container, Integer count) {
        if (count > 0) {
            container.setVisibility(View.VISIBLE);
            textView.setText(String.valueOf(count));
        } else {
            container.setVisibility(View.GONE);
        }
    }

    /**
     * Initialise the constant cursor index fields. This should always be called
     *  before trying to access these fields.
     * @param cursor A cursor from which to initialise the constant cursor indices.
     */
    private void setupIndicies(Cursor cursor) {
        if (mStatus_index == null) {
            mStatus_index = cursor.getColumnIndex(FileChanges.C_STATUS);
        }
        if (mPath_index == null) {
            mPath_index = cursor.getColumnIndex(FileChanges.C_FILE_NAME);
        }
        if (mInserted_index == null) {
            mInserted_index = cursor.getColumnIndex(FileChanges.C_LINES_INSERTED);
        }
        if (mDeleted_index == null) {
            mDeleted_index = cursor.getColumnIndex(FileChanges.C_LINES_DELETED);
        }
    }

    class ViewHolder {
        private final TextView path;
        private final TextView inserted;
        private final TextView deleted;
        private final View insertedContainer;
        private final View deletedContainer;

        ViewHolder(View view) {
            path = (TextView) view.findViewById(R.id.changed_file_path);
            inserted = (TextView) view.findViewById(R.id.changed_file_inserted);
            deleted = (TextView) view.findViewById(R.id.changed_file_deleted);
            insertedContainer = view.findViewById(R.id.inserted_text);
            deletedContainer = view.findViewById(R.id.deleted_text);
        }
    }
}
