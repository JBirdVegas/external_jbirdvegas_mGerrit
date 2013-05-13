package com.jbirdvegas.mgerrit.cards;

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
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.fima.cardsui.objects.Card;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.objects.ChangedFile;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

import java.util.List;

public class PatchSetChangesCard extends Card {
    private static final String TAG = PatchSetChangesCard.class.getSimpleName();
    private static final boolean VERBOSE = false;
    private JSONCommit mCommit;
    private LayoutInflater mInflater;

    public PatchSetChangesCard(JSONCommit commit) {
        mCommit = commit;
    }

    @Override
    public View getCardContent(final Context context) {
        mInflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup rootView = (ViewGroup) mInflater.inflate(R.layout.linear_layout, null);
        List<ChangedFile> changedFileList = mCommit.getChangedFiles();
        // its possible for this to be null so watch out
        if (changedFileList == null) {
            // EEK! just show a simple not found message
            // TODO Show some error message?
        } else {
            for (ChangedFile changedFile : changedFileList) {
                // generate and add the Changed File Views
                rootView.addView(generateChangedFileView(changedFile, context));
            }
        }
        return rootView;
    }

    private View generateChangedFileView(final ChangedFile changedFile, final Context context) {
        View innerRootView = mInflater.inflate(R.layout.patchset_file_changed_list_item, null);
        TextView path = (TextView)
                innerRootView.findViewById(R.id.changed_file_path);
        TextView inserted = (TextView)
                innerRootView.findViewById(R.id.changed_file_inserted);
        TextView deleted = (TextView)
                innerRootView.findViewById(R.id.changed_file_deleted);
        TextView insText = (TextView)
                innerRootView.findViewById(R.id.inserted_text);
        TextView delText = (TextView)
                innerRootView.findViewById(R.id.deleted_text);
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
                int mGreen = context.getResources().getColor(R.color.text_green);
                inserted.setText('+' + String.valueOf(changedFile.getInserted()));
                inserted.setTextColor(mGreen);
            }
            // we may not have deleted lines so remove if unneeded
            if (changedFile.getDeleted() == Integer.MIN_VALUE) {
                deleted.setVisibility(View.GONE);
                delText.setVisibility(View.GONE);
            } else {
                int mRed = context.getResources().getColor(R.color.text_red);
                deleted.setText('-' + String.valueOf(changedFile.getDeleted()));
                deleted.setTextColor(mRed);
            }
        }
        innerRootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String base = "%s#/c/%d/%d/%s";
                Intent browserIntent = new Intent(
                        Intent.ACTION_VIEW, Uri.parse(String.format(base,
                        Prefs.getCurrentGerrit(context),
                        mCommit.getCommitNumber(),
                        mCommit.getPatchSetNumber(),
                        changedFile.getPath())));
                context.startActivity(browserIntent);
            }
        });
        return innerRootView;
    }
}