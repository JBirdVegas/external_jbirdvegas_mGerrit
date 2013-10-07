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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.fima.cardsui.objects.Card;
import com.fima.cardsui.objects.RecyclableCard;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.dialogs.DiffDialog;
import com.jbirdvegas.mgerrit.helpers.Tools;
import com.jbirdvegas.mgerrit.objects.ChangedFile;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

import java.util.List;

public class PatchSetChangesCard extends RecyclableCard {
    private static final String TAG = PatchSetChangesCard.class.getSimpleName();
    private static final boolean VERBOSE = false;
    private JSONCommit mCommit;
    private final Activity mActivity;
    private AlertDialog mAlertDialog;
    private final int mGreen;
    private final int mRed;

    public PatchSetChangesCard(JSONCommit commit, Activity activity) {
        mCommit = commit;
        mActivity = activity;

        mGreen = mActivity.getResources().getColor(R.color.text_green);
        mRed = mActivity.getResources().getColor(R.color.text_red);
    }

    private View generateChangedFileView(final ChangedFile changedFile, final Context context) {

        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View innerRootView = inflater.inflate(R.layout.patchset_file_changed_list_item, null);
        innerRootView.setTag(changedFile);
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
                inserted.setText('+' + String.valueOf(changedFile.getInserted()));
                inserted.setTextColor(mGreen);
            }
            // we may not have deleted lines so remove if unneeded
            if (changedFile.getDeleted() == Integer.MIN_VALUE) {
                deleted.setVisibility(View.GONE);
                delText.setVisibility(View.GONE);
            } else {
                deleted.setText('-' + String.valueOf(changedFile.getDeleted()));
                deleted.setTextColor(mRed);
            }
        }
        innerRootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                AlertDialog.Builder ad = new AlertDialog.Builder(mActivity)
                        .setTitle(R.string.choose_diff_view);
                // TODO XXX ABANDONED till APIs are stable on Google's side :(
                ad.setPositiveButton(R.string.context_menu_view_diff_dialog, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // http://gerrit.aokp.co/changes/AOKP%2Fexternal_jbirdvegas_mGerrit~master~I0d360472ee328c6cde0f8303b19a35f175869e68/revisions/current/patch
                        // or
                        // after v2.8 goes stable (returns Base64 encoded String)
                        // curl https://gerrit-review.googlesource.com/changes/gerrit~master~Idc97af3d01999889d9b1a818fbd1bbe0b274dcf3/revisions/77e974c7070e274aaca3f2413a3fb53031d0f50e/files/ReleaseNotes%2fReleaseNotes-2.5.3.txt/content
                        ///changes/{change-id}/revisions/{revision-id}/files/{file-id}/content
                        String base = "%schanges/%s/revisions/current/patch";
                        String base64 = "%schanges/%s/revisions/%s/files/%s/content";
                        String url = String.format(base,
                                Prefs.getCurrentGerrit(mActivity),
                                mCommit.getId());
                                //mCommit.getCurrentRevision(),
                                //URLEncoder.encode(((ChangedFile) view.getTag()).getPath()));
                        launchDiffDialog(url, changedFile);
                    }
                });

                ad.setNegativeButton(
                        R.string.context_menu_diff_view_in_browser, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // launch in webbrowser
                        String base = "%s#/c/%d/%d/%s,unified";
                        Intent browserIntent = new Intent(
                                Intent.ACTION_VIEW, Uri.parse(String.format(base,
                                Prefs.getCurrentGerrit(context),
                                mCommit.getCommitNumber(),
                                mCommit.getPatchSetNumber(),
                                changedFile.getPath())));
                        context.startActivity(browserIntent);
                    }
                });
                ad.create().show();
            }
        });
        return innerRootView;
    }

    @Override
    protected void applyTo(View convertView) {

        ViewGroup view = (ViewGroup) convertView;
        List<ChangedFile> changedFileList = mCommit.getChangedFiles();
        // its possible for this to be null so watch out
        if (changedFileList == null) {
            // EEK! just show a simple not found message
            // TODO Show some error message?
            Log.e(TAG, "Could not find the list of changed files for this commit.");
        } else {
            for (ChangedFile changedFile : changedFileList) {
                // generate and add the Changed File Views
                if (view != null) {
                    view.addView(generateChangedFileView(changedFile, mActivity));
                }

            }
        }
    }

    @Override
    protected int getCardLayoutId() {
        return R.layout.linear_layout;
    }

    // creates the Diff viewer dialog
    private void launchDiffDialog(String url, ChangedFile changedFile) {
        Log.d(TAG, "Attempting to contact: " + url);
        DiffDialog diffDialog = new DiffDialog(mActivity, url, changedFile);
        diffDialog.addExceptionCallback(new DiffDialog.DiffFailCallback() {
            @Override
            public void killDialogAndErrorOut(Exception e) {
                if (mAlertDialog != null) {
                    mAlertDialog.cancel();
                }
                Tools.showErrorDialog(mActivity, e);
            }
        });
        mAlertDialog = diffDialog.create();
        mAlertDialog.show();
    }
}