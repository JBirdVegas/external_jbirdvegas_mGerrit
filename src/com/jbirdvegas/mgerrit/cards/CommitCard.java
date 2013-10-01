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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import android.widget.Toast;
import com.android.volley.RequestQueue;
import com.fima.cardsui.objects.Card;
import com.jbirdvegas.mgerrit.CardsFragment;
import com.jbirdvegas.mgerrit.GerritControllerActivity;
import com.jbirdvegas.mgerrit.PatchSetViewerFragment;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.listeners.TrackingClickListener;
import com.jbirdvegas.mgerrit.objects.ChangeLogRange;
import com.jbirdvegas.mgerrit.objects.ChangedFile;
import com.jbirdvegas.mgerrit.objects.CommitterObject;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

import java.util.Arrays;
import java.util.List;

public class CommitCard extends Card {
    private static final String TAG = CommitCard.class.getSimpleName();

    private final CommitterObject mCommitterObject;
    private final RequestQueue mRequestQuery;
    private final GerritControllerActivity mActivity;
    private JSONCommit mCommit;
    private TextView mProjectTextView;
    private ChangeLogRange mChangeLogRange;

    public CommitCard(JSONCommit commit,
                      CommitterObject committerObject,
                      RequestQueue requestQueue,
                      GerritControllerActivity activity) {
        this.mCommit = commit;
        this.mCommitterObject = committerObject;
        this.mRequestQuery = requestQueue;
        this.mActivity = activity;
    }

    @Override
    public View getCardContent(final Context context) {
        int mGreen = context.getResources().getColor(R.color.text_green);
        int mRed = context.getResources().getColor(R.color.text_red);

        // We are inflating a layout for each card, this is not good when we could be re-using them!
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View commitCardView = inflater.inflate(R.layout.commit_card, null);

        // I hate UI code so instead of embedding a LinearLayout for just an
        // ImageView with an associated TextView we just use the TextView's
        // built in CompoundDrawablesWithIntrinsicBounds(Drawable, Drawable, Drawable, Drawable)
        // to handle the layout work. This also has a benefit of better performance!
        TextView ownerTextView = (TextView) commitCardView.findViewById(R.id.commit_card_commit_owner);
        // set the text
        if (mCommit.getOwnerObject() != null) {
            ownerTextView.setText(mCommit.getOwnerObject().getName());
            ownerTextView.setTag(mCommit.getOwnerObject());
            TrackingClickListener trackingClickListener =
                    new TrackingClickListener(context, mCommit.getOwnerObject());
            if (CardsFragment.inProject) {
                trackingClickListener.addProjectToStalk(mCommit.getProject());
            }
            ownerTextView.setOnClickListener(trackingClickListener);
            GravatarHelper.attachGravatarToTextView(ownerTextView,
                    mCommit.getOwnerObject().getEmail(),
                    mRequestQuery);
        }
        mProjectTextView = (TextView) commitCardView.findViewById(R.id.commit_card_project_name);
        mProjectTextView.setText(mCommit.getProject());
        mProjectTextView.setTextSize(18f);
        TrackingClickListener trackingClickListener =
                new TrackingClickListener(context, mCommit.getProject(), mChangeLogRange);
        if (mCommitterObject != null) {
            trackingClickListener.addUserToStalk(mCommitterObject);
        }
        mProjectTextView.setOnClickListener(trackingClickListener);

        ((TextView) commitCardView.findViewById(R.id.commit_card_title))
                .setText(mCommit.getSubject());
        ((TextView) commitCardView.findViewById(R.id.commit_card_last_updated))
                .setText(mCommit.getLastUpdatedDate(context));
        ((TextView) commitCardView.findViewById(R.id.commit_card_commit_status))
                .setText(mCommit.getStatus().toString());
        if (mCommit.getStatus().toString() == "MERGED") {
            ((TextView) commitCardView.findViewById(R.id.commit_card_commit_status)).setTextColor(mGreen);
        } else if (mCommit.getStatus().toString() == "ABANDONED") {
            ((TextView) commitCardView.findViewById(R.id.commit_card_commit_status)).setTextColor(mRed);
        }
        TextView messageTv = (TextView)
                commitCardView.findViewById(R.id.commit_card_message);
        TextView changedFilesTv = (TextView)
                commitCardView.findViewById(R.id.commit_card_changed_files);
        ImageView browserView = (ImageView) commitCardView.findViewById(
                R.id.commit_card_view_in_browser);
        ImageView shareView = (ImageView) commitCardView.findViewById(
                R.id.commit_card_share_info);
        ImageView moarInfo = (ImageView) commitCardView.findViewById(
                R.id.commit_card_moar_info);

        View.OnClickListener cardListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, PatchSetViewerFragment.class);
                // example website
                // http://gerrit.aokp.co/changes/?q=7615&o=CURRENT_REVISION&o=CURRENT_COMMIT&o=CURRENT_FILES&o=DETAILED_LABELS
                mActivity.onChangeSelected(mCommit.getChangeId(), mCommit.getStatus().toString(), true);
            }
        };

        if (moarInfo != null) {
            moarInfo.setOnClickListener(cardListener);
        } else {
            setOnClickListener(cardListener);
        }

        shareView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                intent.putExtra(Intent.EXTRA_SUBJECT, String.format(context.getString(R.string.commit_shared_from_mgerrit), mCommit.getChangeId()));
                intent.putExtra(Intent.EXTRA_TEXT, mCommit.getWebAddress() + " #mGerrit");
                context.startActivity(intent);
            }
        });
        browserView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCommit.getWebAddress() != null) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(mCommit.getWebAddress()));
                    context.startActivity(browserIntent);
                } else {
                    Toast.makeText(view.getContext(),
                            R.string.failed_to_find_url,
                            Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
        // we only have these if we direct query the commit specifically
        if (mCommit.getCurrentRevision() != null) {
            messageTv.setText(mCommit.getMessage());
            changedFilesTv.setText(
                    buildChangedFilesString(mCommit.getChangedFiles()));
        } else {
            messageTv.setVisibility(View.GONE);
            changedFilesTv.setVisibility(View.GONE);
        }
        return commitCardView;
    }

    public CommitCard setChangeLogRange(ChangeLogRange logRange) {
        mChangeLogRange = logRange;
        return this;
    }

    public void update(JSONCommit commit) {
        this.mCommit = commit;
    }

    public JSONCommit getJsonCommit() {
        return mCommit;
    }

    /**
     * returns the ChangedFile list as a string
     *
     * @param fileList List of ChangedFiles
     * @return String representation of list
     */
    private String buildChangedFilesString(List<ChangedFile> fileList) {
        return Arrays.toString(fileList.toArray());
    }
}
