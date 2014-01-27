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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.database.FileChanges;
import com.jbirdvegas.mgerrit.dialogs.DiffDialog;
import com.jbirdvegas.mgerrit.helpers.Tools;
import com.jbirdvegas.mgerrit.objects.FileInfo;

public class PatchSetChangesCard implements CardBinder {
    private static final String TAG = PatchSetChangesCard.class.getSimpleName();
    private final Context mContext;
    private final LayoutInflater mInflater;
    private AlertDialog mAlertDialog;

    // Colors
    private final int mGreen;
    private final int mRed;
    // The theme we are using, so we can get the default text color
    private final boolean mUsingLightTheme;

    // Cursor indices
    private Integer mChangeId_index;
    private Integer mFileName_index;
    private Integer mStatus_index;
    private Integer mIsBinary_index;
    private Integer mOldPath_index;
    private Integer mInserted_index;
    private Integer mDeleted_index;
    private Integer mPatchSet_index;
    private Integer mCommit_index;

    public PatchSetChangesCard(Context context) {
        mContext = context;
        mGreen = context.getResources().getColor(R.color.text_green);
        mRed = context.getResources().getColor(R.color.text_red);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mUsingLightTheme = (Prefs.getCurrentThemeID(mContext) == R.style.Theme_Light);
    }

    @Override
    public View setViewValue(Cursor cursor, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.patchset_changes_card, null);
        }

        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        if (viewHolder == null) {
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }

        setupCusorIndicies(cursor);

        // we always have a path
        viewHolder.path.setText(cursor.getString(mFileName_index));

        FileInfo.Status status = FileInfo.Status.getValue(cursor.getString(mStatus_index));
        if (status == FileInfo.Status.ADDED) {
            viewHolder.path.setTextColor(mGreen);
        } else if (status == FileInfo.Status.DELETED) {
            viewHolder.path.setTextColor(mRed);
        } else {
            // Need to determine from the current theme what the default color is and set it back
            if (mUsingLightTheme) {
                viewHolder.path.setTextColor(mContext.getResources().getColor(R.color.text_light));
            } else {
                viewHolder.path.setTextColor(mContext.getResources().getColor(R.color.text_dark));
            }
        }

        String oldPath = cursor.getString(mOldPath_index);
        if (oldPath != null && !oldPath.isEmpty()) {
            viewHolder.oldPathContainer.setVisibility(View.VISIBLE);
            viewHolder.oldPath.setText(oldPath);
        } else {
            viewHolder.oldPathContainer.setVisibility(View.GONE);
        }

        /* If the file is binary don't offer to show the diff or any statistics as
         *  we cannot get detailed information on binary files */
        if (cursor.getInt(mIsBinary_index) != 0) {
            viewHolder.binaryText.setVisibility(View.VISIBLE);
            return convertView;
        } else {
            viewHolder.binaryText.setVisibility(View.GONE);
        }

        int insertedInFile = cursor.getInt(mInserted_index);
        int deletedInFile = cursor.getInt(mDeleted_index);
        // we may not have inserted lines so remove if unneeded
        if (insertedInFile < 1) {
            viewHolder.insText.setVisibility(View.GONE);
        } else {
            viewHolder.insText.setVisibility(View.VISIBLE);
            viewHolder.inserted.setText('+' + String.valueOf(insertedInFile));
        }
        // we may not have deleted lines so remove if unneeded
        if (deletedInFile < 1) {
            viewHolder.delText.setVisibility(View.GONE);
        } else {
            viewHolder.delText.setVisibility(View.VISIBLE);
            viewHolder.deleted.setText('-' + String.valueOf(deletedInFile));
        }

        // We have already set an anonymous tag so we need to use ids
        convertView.setTag(R.id.changeNumber, cursor.getInt(mCommit_index));
        convertView.setTag(R.id.filePath, cursor.getString(mFileName_index));
        convertView.setTag(R.id.patchSetNumber, cursor.getInt(mPatchSet_index));

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                AlertDialog.Builder ad = new AlertDialog.Builder(mContext)
                        .setTitle(R.string.choose_diff_view);

                final Integer changeNumber = (Integer) view.getTag(R.id.changeNumber);
                final String filePath = (String) view.getTag(R.id.filePath);
                final Integer patchset = (Integer) view.getTag(R.id.patchSetNumber);

                ad.setPositiveButton(R.string.context_menu_view_diff_dialog, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // v2.8 (returns Base64 encoded String)
                        // http://gerrit.aokp.co/changes/I554a3ab/revisions/current/files/res%2Fvalues%2Fcustom_arrays.xml/diff
                        //changes/{change-id}/revisions/current/files/{file-path}/content
                        String base64 = "%schanges/%s/revisions/current/patch";
                        String url = String.format(base64,
                                Prefs.getCurrentGerrit(mContext),
                                changeNumber);
                        launchDiffDialog(url, filePath);
                    }
                });

                ad.setNegativeButton(
                        R.string.context_menu_diff_view_in_browser, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // launch in web browser
                        String base = "%s#/c/%d/%d/%s,unified";
                        Intent browserIntent = new Intent(
                                Intent.ACTION_VIEW, Uri.parse(String.format(base,
                                Prefs.getCurrentGerrit(mContext),
                                changeNumber,
                                patchset, filePath)));
                        mContext.startActivity(browserIntent);
                    }
                });
                ad.create().show();
            }
        });
        return convertView;
    }

    // creates the Diff viewer dialog
    private void launchDiffDialog(String url, String filePath) {
        Log.d(TAG, "Attempting to contact: " + url);
        DiffDialog diffDialog = new DiffDialog(mContext, url, filePath);
        diffDialog.addExceptionCallback(new DiffDialog.DiffFailCallback() {
            @Override
            public void killDialogAndErrorOut(Exception e) {
                if (mAlertDialog != null) {
                    mAlertDialog.cancel();
                }
                Tools.showErrorDialog(mContext, e);
            }
        });
        mAlertDialog = diffDialog.create();
        mAlertDialog.show();
    }

    private void setupCusorIndicies(Cursor cursor) {
        if (cursor.getPosition() < 0) {
            cursor.moveToFirst();
        }

        if (mChangeId_index == null) {
            mChangeId_index = cursor.getColumnIndex(FileChanges.C_CHANGE_ID);
        }
        if (mFileName_index == null) {
            mFileName_index = cursor.getColumnIndex(FileChanges.C_FILE_NAME);
        }
        if (mStatus_index == null) {
            mStatus_index = cursor.getColumnIndex(FileChanges.C_STATUS);
        }
        if (mIsBinary_index == null) {
            mIsBinary_index = cursor.getColumnIndex(FileChanges.C_ISBINARY);
        }
        if (mOldPath_index == null) {
            mOldPath_index = cursor.getColumnIndex(FileChanges.C_OLDPATH);
        }
        if (mInserted_index == null) {
            mInserted_index = cursor.getColumnIndex(FileChanges.C_LINES_INSERTED);
        }
        if (mDeleted_index == null) {
            mDeleted_index = cursor.getColumnIndex(FileChanges.C_LINES_DELETED);
        }
        if (mPatchSet_index == null) {
            mPatchSet_index = cursor.getColumnIndex(FileChanges.C_PATCH_SET_NUMBER);
        }
        if (mCommit_index == null) {
            mCommit_index = cursor.getColumnIndex(FileChanges.C_COMMIT_NUMBER);
        }
    }

    class ViewHolder {

        TextView path;
        TextView inserted;
        TextView deleted;
        View binaryText;
        View insText;
        View delText;
        TextView oldPath;
        View oldPathContainer;

        ViewHolder(View view) {
            path = (TextView)view.findViewById(R.id.changed_file_path);
            inserted = (TextView)view.findViewById(R.id.changed_file_inserted);
            deleted = (TextView)view.findViewById(R.id.changed_file_deleted);
            binaryText = view.findViewById(R.id.binary_text);
            insText = view.findViewById(R.id.inserted_text);
            delText = view.findViewById(R.id.deleted_text);
            oldPath = (TextView) view.findViewById(R.id.changed_file_old_path);
            oldPathContainer = (View) view.findViewById(R.id.old_path_container);
        }
    }
}
