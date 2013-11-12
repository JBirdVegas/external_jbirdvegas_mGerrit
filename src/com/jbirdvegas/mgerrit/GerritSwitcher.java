package com.jbirdvegas.mgerrit;

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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.jbirdvegas.mgerrit.adapters.TeamListAdapter;
import com.jbirdvegas.mgerrit.helpers.GerritTeamsHelper;
import com.jbirdvegas.mgerrit.objects.GerritDetails;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dialog that lists the available Gerrit source options and
 *  allows the user to select one of them or add a new source.
 *  The selected Gerrit source is shown with a selected radio button
 *  (recorded in the adapter, but managed through callbacks here).
 *
 *  Note: This must use the non-support library DialogFragment
 *   (android.app.DialogFragment) to be invoked from the Preferences
 *   as there is no PreferenceFragment class in the support library.
 */
public class GerritSwitcher extends DialogFragment {

    public static final String TAG = "GerritSwitcher";

    private Context mContext;
    private List<GerritDetails> gerritData;
    private ListView mListView;
    private TeamListAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this.getActivity();
        Resources res = mContext.getResources();
        initialiseGerritList(res);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        mAdapter = new TeamListAdapter(mContext, gerritData);

        // Set the current Gerrit as selected
        String currentGerrit = Prefs.getCurrentGerrit(mContext);
        int pos = mAdapter.findItem(currentGerrit);

        builder.setSingleChoiceItems(mAdapter, pos, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Not used
            }
        })
                .setTitle(R.string.choose_gerrit_instance)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Only dismiss the dialog if a Gerrit has been selected
                        if (onCommitSelection()) {
                            dismiss();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        getDialog().cancel();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    private void initialiseGerritList(Resources res) {
        final ArrayList <String> teams = new ArrayList<>();
        Collections.addAll(teams, res.getStringArray(R.array.gerrit_names));

        final ArrayList<String> urls = new ArrayList<>();
        Collections.addAll(urls, res.getStringArray(R.array.gerrit_webaddresses));

        GerritTeamsHelper teamsHelper = new GerritTeamsHelper();
        teams.addAll(teamsHelper.getGerritNamesList());
        urls.addAll(teamsHelper.getGerritUrlsList());

        Set<GerritDetails> gerrits = new HashSet<>();

        int min = Math.min(teams.size(), urls.size());
        for (int i = 0; i < min; i++) {
            gerrits.add(new GerritDetails(teams.get(i), urls.get(i)));
        }
        gerritData = new ArrayList<>(gerrits);
        Collections.sort(gerritData);
    }

    @Override
    public void onStart() {
        super.onStart();

        // This enables the soft keyboard to show up when an EditText field is focused.
        getDialog().getWindow().
                clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        mListView = ((AlertDialog) getDialog()).getListView();
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                // on long click delete the file and refresh the list
                return removeItem(i);
            }
        });
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }

    /**
     * @return Whether the new gerrit was set
     */
    private boolean onCommitSelection() {
        GerritDetails gerrit = mAdapter.getItem(mListView.getCheckedItemPosition());
        String gerritName = gerrit.getGerritName().trim();
        String gerritUrl = gerrit.getGerritUrl().trim();

        if (gerritName == null || gerritName.length() < 1) {
            Toast.makeText(mContext, mContext.getString(R.string.invalid_gerrit_name), Toast.LENGTH_SHORT).show();
            return false;
        } else if (isUrlValid(gerritUrl)) {
            // ensure we end with /
            if ('/' != gerritUrl.charAt(gerritUrl.length() - 1)) {
                gerritUrl += "/";
            }
            Log.v(TAG, "Saving url: " + gerritUrl);
            GerritTeamsHelper.saveTeam(gerritName, gerritUrl);
            Prefs.setCurrentGerrit(mContext, gerritUrl);
            return true;
        } else {
            Toast.makeText(mContext, mContext.getString(R.string.invalid_gerrit_url), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    // Validator for URLs
    private boolean isUrlValid(String url) {
        return (url != null)
                && (URLUtil.isHttpUrl(url)
                || URLUtil.isHttpsUrl(url)
                && url.contains("."));
    }

    /**
     * Remove a Gerrit instance from the list
     * @param position
     * @return
     */
    private boolean removeItem(int position) {
        // If the placeholder is at this position, this cannot succeed.
        if (position >= gerritData.size()) {
            return false;
        }
        // Cannot remove the currently selected Gerrit
        String setGerrit = Prefs.getCurrentGerrit(mContext);
        String thisItem = mAdapter.getItem(position).getGerritUrl();
        if (setGerrit.equals(thisItem)) {
            return false;
        }

        GerritDetails team = gerritData.get(position);
        File target = new File(GerritTeamsHelper.mExternalCacheDir + "/" + team.getGerritName());

        boolean success = target.delete();
        StringBuilder builder = new StringBuilder().append("Attempt to delete: ")
                .append(target.getAbsolutePath())
                .append(" was a ");
        if (success) {
            builder.append("success.");
        } else {
            builder.append("failure.");
            Log.v(TAG, "Files present:" + Arrays.toString(GerritTeamsHelper.mExternalCacheDir.list()));
        }

        /* We don't need to worry about an item not being selected in the adapter from removing this
         *  element as it can always move down one position. We are not deleting the last element
         *  (the placeholder) */
        gerritData.remove(position);
        mAdapter.notifyDataSetChanged();
        return success;
    }
}
