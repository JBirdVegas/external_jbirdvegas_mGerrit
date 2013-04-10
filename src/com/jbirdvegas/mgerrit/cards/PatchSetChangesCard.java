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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import com.jbirdvegas.mgerrit.PatchSetViewerActivity;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.adapters.PatchSetChangedFilesAdapter;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.fima.cardsui.objects.Card;

public class PatchSetChangesCard extends Card {
    private static final String TAG = PatchSetChangesCard.class.getSimpleName();
    private JSONCommit mCommit;

    public PatchSetChangesCard(JSONCommit commit) {
        mCommit = commit;
    }

    @Override
    public View getCardContent(final Context context) {
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootView = inflater.inflate(R.layout.listview_card, null);
        ListView listView = (ListView) rootView.findViewById(R.id.listView);
        // its possible for this to be null so watch out
        if (mCommit.getChangedFiles() == null) {
            // EEK! just show a simple not found message
            PatchSetViewerActivity.setNotFoundListView(context, listView);
        } else {
            try {
                listView.setAdapter(new PatchSetChangedFilesAdapter(context,
                        mCommit.getChangedFiles(),
                        mCommit));
                PatchSetViewerActivity.setListViewHeightBasedOnChildren(listView);
            } catch (NullPointerException npe) {
                Log.d(TAG, "Failed to set ListView Adapter", npe);
            }
        }
        return rootView;
    }
}
