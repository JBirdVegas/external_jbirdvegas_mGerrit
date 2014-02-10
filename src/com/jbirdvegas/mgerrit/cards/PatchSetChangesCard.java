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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.DiffViewer;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.adapters.CommitDetailsAdapter;
import com.jbirdvegas.mgerrit.database.Config;
import com.jbirdvegas.mgerrit.database.FileChanges;
import com.jbirdvegas.mgerrit.objects.FileInfo;

public class PatchSetChangesCard implements CardBinder {
    private final Context mContext;
    private final LayoutInflater mInflater;

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
    private Integer mIsImage_index;


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

        /* If the file is binary don't offer to show the diff as
         *  we cannot get detailed information on binary files */
        if (cursor.getInt(mIsBinary_index) != 0) {
            viewHolder.binaryText.setVisibility(View.VISIBLE);
            convertView.setEnabled(false);
            // If it is binary and not an image we don't need to tag diff-related data
            if (cursor.getInt(mIsImage_index) == 0) {
                return convertView;
            }
        } else {
            viewHolder.binaryText.setVisibility(View.GONE);
            convertView.setEnabled(true);
        }

        // We have already set an anonymous tag so we need to use ids
        convertView.setTag(R.id.changeNumber, cursor.getInt(mCommit_index));
        convertView.setTag(R.id.filePath, cursor.getString(mFileName_index));
        convertView.setTag(R.id.patchSetNumber, cursor.getInt(mPatchSet_index));

        return convertView;
    }

    // launches internal diff viewer
    public static void launchDiffViewer(Context context, Integer changeNumber,
                                        Integer patchSetNumber, String filePath) {
        Intent diffIntent = new Intent(context, DiffViewer.class);
        diffIntent.putExtra(DiffViewer.CHANGE_NUMBER_TAG, changeNumber);
        diffIntent.putExtra(DiffViewer.PATCH_SET_NUMBER_TAG, patchSetNumber);
        diffIntent.putExtra(DiffViewer.FILE_PATH_TAG, filePath);
        context.startActivity(diffIntent);
    }

    public static void launchDiffInBrowser(Context context, Integer changeNumber, Integer patchset,
                                           String filePath) {
        String base = "%s#/c/%d/%d/%s,unified";
        Intent browserIntent = new Intent(
                Intent.ACTION_VIEW, Uri.parse(String.format(base,
                Prefs.getCurrentGerrit(context),
                changeNumber,
                patchset, filePath)));
        context.startActivity(browserIntent);
    }

    public static void launchDiffOptionDialog(final Context context, final Integer changeNumber,
                                              final Integer patchset,
                                        final String filePath) {
        AlertDialog.Builder ad = new AlertDialog.Builder(context)
                .setTitle(R.string.choose_diff_view)
                .setPositiveButton(R.string.context_menu_view_diff_viewer, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        launchDiffViewer(context, changeNumber, patchset, filePath);
                    }
                })
                .setNegativeButton(
                        R.string.context_menu_diff_view_in_browser, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        launchDiffInBrowser(context, changeNumber, patchset, filePath);
                    }
                });
        ad.create().show();
    }

    public static boolean onViewClicked(Context context, View view) {
        if (view == null) return false;

        final Integer changeNumber = (Integer) view.getTag(R.id.changeNumber);
        final String filePath = (String) view.getTag(R.id.filePath);
        final Integer patchset = (Integer) view.getTag(R.id.patchSetNumber);

        if (changeNumber == null) return false;

        // If the server does not support diffs then do not show the dialog
        if (!Config.isDiffSupported(context)) {
            PatchSetChangesCard.launchDiffInBrowser(context, changeNumber, patchset, filePath);
            return true;
        }

        Prefs.DiffModes mode = Prefs.getDiffDefault(context);
        if (mode == Prefs.DiffModes.INTERNAL) {
            PatchSetChangesCard.launchDiffViewer(context, changeNumber, patchset, filePath);
        } else if (mode == Prefs.DiffModes.EXTERNAL) {
            PatchSetChangesCard.launchDiffInBrowser(context, changeNumber, patchset, filePath);
        } else {
            PatchSetChangesCard.launchDiffOptionDialog(context, changeNumber, patchset, filePath);
        }

        return true;
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
        if (mIsImage_index == null) {
            mIsImage_index = cursor.getColumnIndex(FileChanges.C_ISIMAGE);
        }
    }

    class ViewHolder {

        final TextView path;
        final TextView inserted;
        final TextView deleted;
        final View binaryText;
        final View insText;
        final View delText;
        final TextView oldPath;
        final View oldPathContainer;

        ViewHolder(View view) {
            path = (TextView)view.findViewById(R.id.changed_file_path);
            inserted = (TextView)view.findViewById(R.id.changed_file_inserted);
            deleted = (TextView)view.findViewById(R.id.changed_file_deleted);
            binaryText = view.findViewById(R.id.binary_text);
            insText = view.findViewById(R.id.inserted_text);
            delText = view.findViewById(R.id.deleted_text);
            oldPath = (TextView) view.findViewById(R.id.changed_file_old_path);
            oldPathContainer = view.findViewById(R.id.old_path_container);
        }
    }
}
