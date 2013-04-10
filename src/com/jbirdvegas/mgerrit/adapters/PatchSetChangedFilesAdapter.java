package com.jbirdvegas.mgerrit.adapters;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
 *  Author: Jon Stanford (JBirdVegas), 2013
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
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.objects.ChangedFile;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

import java.util.List;

public class PatchSetChangedFilesAdapter extends ArrayAdapter<ChangedFile> {
    private static final String TAG = PatchSetChangedFilesAdapter.class.getSimpleName();
    private static final boolean VERBOSE = false;
    private final Context mContext;
    private final List<ChangedFile> mValues;
    private final JSONCommit mCommit;

    public PatchSetChangedFilesAdapter(Context context,
                                       List<ChangedFile> values,
                                       JSONCommit commit) {
        super(context,
                R.layout.patchset_file_changed_list_item,
                values);
        this.mContext = context;
        this.mValues = values;
        this.mCommit = commit;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)
                mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(
                R.layout.patchset_file_changed_list_item, null);
        final ChangedFile changedFile = mValues.get(position);
        Log.d(TAG, "Total number of changed Files: " + mValues.size());
        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String base = "%s#/c/%d/%d/%s";
                Intent browserIntent = new Intent(
                        Intent.ACTION_VIEW, Uri.parse(String.format(base,
                            Prefs.getCurrentGerrit(mContext),
                            mCommit.getCommitNumber(),
                            mCommit.getPatchSetNumber(),
                            changedFile.getPath())));
                mContext.startActivity(browserIntent);
            }
        });
        TextView path = (TextView)
                rowView.findViewById(R.id.changed_file_path);
        TextView inserted = (TextView)
                rowView.findViewById(R.id.changed_file_inserted);
        TextView deleted = (TextView)
                rowView.findViewById(R.id.changed_file_deleted);
        TextView insText = (TextView)
                rowView.findViewById(R.id.inserted_text);
        TextView delText = (TextView)
                rowView.findViewById(R.id.deleted_text);
        String changedFilePath = changedFile.getPath();
        int insertedInFile = changedFile.getInserted();
        int deletedInFile = changedFile.getDeleted();
        if (VERBOSE) {
            Log.d(TAG, "File change stats Path=" + changedFilePath
                    + " inserted=" + insertedInFile
                    + " deleted=" + deletedInFile
                    + " objectToString()=" + changedFile.toString());
        }
        // we always have a path
        if (path != null) {
            path.setText(changedFilePath);
            // we may not have inserted lines so remove if unneeded
            if (changedFile.getInserted() == Integer.MIN_VALUE) {
                inserted.setVisibility(View.GONE);
                insText.setVisibility(View.GONE);
            } else {
                inserted.setText('+' + String.valueOf(changedFile.getInserted()));
                inserted.setTextColor(Color.GREEN);
                insText.setTextColor(Color.GREEN); //remove?
            }
            // we may not have deleted lines so remove if unneeded
            if (changedFile.getDeleted() == Integer.MIN_VALUE) {
                deleted.setVisibility(View.GONE);
                deleted.setVisibility(View.GONE);
            } else {
                deleted.setText('-' + String.valueOf(changedFile.getDeleted()));
                deleted.setTextColor(Color.RED);
                delText.setTextColor(Color.RED); //remove?
            }
        }
        return rowView;
    }
}