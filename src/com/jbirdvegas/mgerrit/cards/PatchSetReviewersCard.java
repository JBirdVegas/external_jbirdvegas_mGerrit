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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import com.jbirdvegas.mgerrit.PatchSetViewerActivity;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.adapters.PatchSetReviewersAdapter;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.fima.cardsui.objects.Card;

import static com.jbirdvegas.mgerrit.PatchSetViewerActivity.setListViewHeightBasedOnChildren;

public class PatchSetReviewersCard extends Card {
    private static final String TAG = PatchSetReviewersAdapter.class.getSimpleName();
    private JSONCommit mJSONCommit;
    private ListView mReviewersList;
    private ListView mVerifiedList;

    public PatchSetReviewersCard(JSONCommit commit) {
        mJSONCommit = commit;
    }

    @Override
    public View getCardContent(Context context) {
        LayoutInflater layoutInflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.patchset_labels_card, null);
        mReviewersList = (ListView)
                view.findViewById(R.id.patchset_labels_code_reviewers);
        mVerifiedList = (ListView)
                view.findViewById(R.id.patchset_labels_verified_reviewers);
        // don't try to set from null values
        if (mJSONCommit.getCodeReviewers() == null) {
            // *hopefully* the only reason this would be null is if user is
            // viewing ABANDONED tab so just remove the card
            view.setVisibility(View.GONE);
            return view;
        } else {
            mReviewersList.setAdapter(new PatchSetReviewersAdapter(context,
                    mJSONCommit.getCodeReviewers()));
        }
        // don't try to set from null values
        if (mJSONCommit.getVerifiedReviewers() == null) {
            PatchSetViewerActivity.setNotFoundListView(context, mVerifiedList);
        } else {
            mVerifiedList.setAdapter(new PatchSetReviewersAdapter(context,
                    mJSONCommit.getVerifiedReviewers()));
        }
        // static import of
        // PatchSetViewerActivity.setListViewHeightBasedOnChildren(ListView)
        // handles setting ListView height correctly
        setListViewHeightBasedOnChildren(mReviewersList, mVerifiedList);
        return view;
    }

}
