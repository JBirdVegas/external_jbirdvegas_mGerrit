package com.jbirdvegas.mgerrit.objects;

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
 */

import android.content.Context;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.database.Changes;
import com.jbirdvegas.mgerrit.helpers.Tools;

/**
 * Handler for the Changed files contextual action bar.
 */
public class FilesCAB implements ActionMode.Callback {

    // Whether to enable the internal viewer icon
    private final boolean mShowViewer;
    // Backing action mode where the data is tagged
    private ActionMode mActionMode;
    // View/activity context for both database access and dialog drawing
    private final Context mContext;
    // The title to be shown in the CAB
    private String mSelectedFile;

    public FilesCAB(Context context, boolean diffSupported) {
        mContext = context;
        mShowViewer = diffSupported;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.changed_file_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        // Set the selected file name as the title, note group headers do not have a file name
        if (mSelectedFile != null) {
            mode.setTitle(Tools.getFileName(mSelectedFile));
        }

        // Enable/Disable the diff viewer icon
        final MenuItem viewerItem = menu.findItem(R.id.menu_diff_internal);
        viewerItem.setEnabled(mShowViewer);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        TagHolder holder = (TagHolder) mode.getTag();
        holder.setChangeNumberFromID(mContext);

        switch (item.getItemId()) {
            case R.id.menu_diff_internal:
                Tools.launchDiffViewer(mContext, holder.changeNumber,
                        holder.patchset, holder.filePath);
                break;
            case R.id.menu_diff_external:
                Tools.launchDiffInBrowser(mContext, holder.changeNumber,
                        holder.patchset, holder.filePath);
                break;
            default:
                return false;
        }
        mode.finish();
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
    }

    public void setActionMode(ActionMode mActionMode) { this.mActionMode = mActionMode; }
    public ActionMode getActionMode() { return mActionMode; }

    public void setTitle(String fileName) {
        mSelectedFile = fileName;
    }

    /**
     * ActionMode only supports one default tag to be set, so this is a container
     *  class where we can set multiple data items.
     */
    public static class TagHolder {
        public Integer changeNumber;
        public final String filePath;
        public final Integer patchset;
        public final String changeID;
        public final int groupPosition;
        public final boolean isChild;

        public TagHolder(View view, Context context, int groupPos, boolean child) {
            changeNumber = (Integer) view.getTag(R.id.changeNumber);
            filePath = (String) view.getTag(R.id.filePath);
            patchset = (Integer) view.getTag(R.id.patchSetNumber);
            changeID = (String) view.getTag(R.id.changeID);
            setChangeNumberFromID(context);
            groupPosition = groupPos;
            isChild = child;
        }

        public void setChangeNumberFromID(Context context) {
            if (changeNumber == null && changeID != null) {
                changeNumber = Changes.getChangeNumberForChange(context, changeID);
            }
        }
    }
}
