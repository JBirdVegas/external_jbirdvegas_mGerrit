package com.jbirdvegas.mgerrit.message;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.jbirdvegas.mgerrit.activities.PatchSetViewerActivity;
import com.jbirdvegas.mgerrit.fragments.PatchSetViewerFragment;
import com.jbirdvegas.mgerrit.helpers.AnalyticsHelper;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

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
 *  Event: A new change was selected to view the change details for
 */
public class NewChangeSelected {

    String mChangeId;   // The currently selected change ID
    int mChangeNumber;
    String mStatus;
    boolean mInflate;   // Whether to expand the change and view the change details.
    PatchSetViewerFragment mFragment;

    private NewChangeSelected(String changeId, int changeNumber, String status) {
        this.mChangeId = changeId;
        this.mChangeNumber = changeNumber;
        this.mStatus = status;

        AnalyticsHelper.setCustomString(AnalyticsHelper.C_CHANGE_ID, changeId);
        AnalyticsHelper.setCustomInt(AnalyticsHelper.C_CHANGE_NUMBER, changeNumber);
    }

    public NewChangeSelected(String changeId, int changeNumber, String status, boolean inflate) {
        this(changeId, changeNumber, status);
        this.mInflate = inflate;
    }

    public NewChangeSelected(String changeId, int changeNumber, String status, PatchSetViewerFragment fragment) {
        this(changeId, changeNumber, status);
        this.mInflate = true;
        this.mFragment = fragment;
    }

    public String getChangeId() {
        return mChangeId;
    }

    public String getStatus() {
        return mStatus;
    }

    public boolean compareStatuses(String status) {
        JSONCommit.Status a = JSONCommit.Status.getStatusFromString(mStatus);
        JSONCommit.Status b = JSONCommit.Status.getStatusFromString(status);
        return a == b;
    }

    public void setFragment(PatchSetViewerFragment fragment) {
        this.mFragment = fragment;
    }

    public void inflate(Context context) {
        if (mInflate && mFragment != null) {
            mFragment.loadChange(mChangeId);
        } else if (mInflate) {
            Bundle arguments = new Bundle();
            arguments.putString(PatchSetViewerFragment.CHANGE_ID, mChangeId);
            arguments.putString(PatchSetViewerFragment.STATUS, mStatus);
            arguments.putInt(PatchSetViewerFragment.CHANGE_NO, mChangeNumber);
            Intent detailIntent = new Intent(context, PatchSetViewerActivity.class);
            detailIntent.putExtras(arguments);
            context.startActivity(detailIntent);
        }
    }
}
