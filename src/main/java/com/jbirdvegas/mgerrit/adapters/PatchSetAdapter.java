package com.jbirdvegas.mgerrit.adapters;

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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Pair;

import com.jbirdvegas.mgerrit.PatchSetViewerActivity;
import com.jbirdvegas.mgerrit.PatchSetViewerFragment;

public class PatchSetAdapter extends FragmentStatePagerAdapter {
    private final PatchSetViewerActivity mParent;
    private final Bundle mBundle;

    public PatchSetAdapter(PatchSetViewerActivity parent, FragmentManager fm, Bundle bundle) {
        super(fm);
        mParent = parent;
        mBundle = bundle;
    }

    @Override
    public Fragment getItem(int position) {
        Pair<String, Integer> change = mParent.getChangeAtPosition(position);
        if (change == null) return null;

        PatchSetViewerFragment fragment = new PatchSetViewerFragment();
        Bundle b = new Bundle(mBundle);
        b.putString(PatchSetViewerFragment.CHANGE_ID, change.first);
        b.putInt(PatchSetViewerFragment.CHANGE_NO, change.second);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public int getCount() {
        return mParent.getNumberOfChanges();
    }
}
