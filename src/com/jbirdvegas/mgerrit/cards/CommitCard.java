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

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.fima.cardsui.objects.RecyclableCard;
import com.jbirdvegas.mgerrit.CardsFragment;
import com.jbirdvegas.mgerrit.GerritControllerActivity;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.listeners.TrackingClickListener;
import com.jbirdvegas.mgerrit.objects.ChangeLogRange;
import com.jbirdvegas.mgerrit.objects.ChangedFile;
import com.jbirdvegas.mgerrit.objects.CommitterObject;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

import java.util.Arrays;
import java.util.List;

public class CommitCard extends RecyclableCard {
    private static final String TAG = CommitCard.class.getSimpleName();

    private final CommitterObject mCommitterObject;
    private final RequestQueue mRequestQuery;
    private final GerritControllerActivity mActivity;
    private final int mGreen;
    private final int mRed;
    private JSONCommit mCommit;
    private ChangeLogRange mChangeLogRange;

    public CommitCard(JSONCommit commit,
                      CommitterObject committerObject,
                      RequestQueue requestQueue,
                      GerritControllerActivity activity) {
        this.mCommit = commit;
        this.mCommitterObject = committerObject;
        this.mRequestQuery = requestQueue;
        this.mActivity = activity;

        this.mGreen = mActivity.getResources().getColor(R.color.text_green);
        this.mRed = mActivity.getResources().getColor(R.color.text_red);
    }

    @Override
    protected void applyTo(View commitCardView) {
        // I hate UI code so instead of embedding a LinearLayout for just an
        // ImageView with an associated TextView we just use the TextView's
        // built in CompoundDrawablesWithIntrinsicBounds(Drawable, Drawable, Drawable, Drawable)
        // to handle the layout work. This also has a benefit of better performance!

        ViewHolder viewHolder = (ViewHolder) commitCardView.getTag();
        if (commitCardView.getTag() == null) {
            viewHolder = new ViewHolder();
            viewHolder.owner = (TextView) commitCardView.findViewById(R.id.commit_card_commit_owner);
            viewHolder.project = (TextView) commitCardView.findViewById(R.id.commit_card_project_name);
            viewHolder.cardTitle = (TextView) commitCardView.findViewById(R.id.commit_card_title);
            viewHolder.updated = (TextView) commitCardView.findViewById(R.id.commit_card_last_updated);
            viewHolder.status = (TextView) commitCardView.findViewById(R.id.commit_card_commit_status);
            viewHolder.browserView = (ImageView) commitCardView.findViewById(R.id.commit_card_view_in_browser);
            viewHolder.shareView = (ImageView) commitCardView.findViewById(R.id.commit_card_share_info);
            viewHolder.moarInfo = (ImageView) commitCardView.findViewById(R.id.commit_card_moar_info);
            viewHolder.message = (TextView) commitCardView.findViewById(R.id.commit_card_message);
            viewHolder.changedFiles = (TextView) commitCardView.findViewById(R.id.commit_card_changed_files);
            commitCardView.setTag(viewHolder);
        }

        // set the text
        if (mCommit.getOwnerObject() != null) {
            viewHolder.owner.setText(mCommit.getOwnerObject().getName());
            viewHolder.owner.setTag(mCommit.getOwnerObject());
            TrackingClickListener trackingClickListener =
                    new TrackingClickListener(mActivity, mCommit.getOwnerObject());
            if (CardsFragment.inProject) {
                trackingClickListener.addProjectToStalk(mCommit.getProject());
            }
            viewHolder.owner.setOnClickListener(trackingClickListener);
            GravatarHelper.attachGravatarToTextView(viewHolder.owner,
                    mCommit.getOwnerObject().getEmail(),
                    mRequestQuery);
        }

        viewHolder.project.setText(mCommit.getProject());
        viewHolder.project.setTextSize(18f);
        TrackingClickListener trackingClickListener =
                new TrackingClickListener(mActivity, mCommit.getProject(), mChangeLogRange);
        if (mCommitterObject != null) {
            trackingClickListener.addUserToStalk(mCommitterObject);
        }
        viewHolder.project.setOnClickListener(trackingClickListener);

        viewHolder.cardTitle.setText(mCommit.getSubject());
        viewHolder.updated.setText(mCommit.getLastUpdatedDate(mActivity));
        viewHolder.status.setText(mCommit.getStatus().toString());
        if (mCommit.getStatus().toString() == "MERGED") {
            viewHolder.status.setTextColor(mGreen);
        } else if (mCommit.getStatus().toString() == "ABANDONED") {
            viewHolder.status.setTextColor(mRed);
        }

        View.OnClickListener cardListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // example website
                // http://gerrit.aokp.co/changes/?q=7615&o=CURRENT_REVISION&o=CURRENT_COMMIT&o=CURRENT_FILES&o=DETAILED_LABELS
                mActivity.onChangeSelected(mCommit.getChangeId(), mCommit.getStatus().toString(), true);
            }
        };

        if (viewHolder.moarInfo != null) {
            viewHolder.moarInfo.setOnClickListener(cardListener);
        } else {
            setOnClickListener(cardListener);
        }

        viewHolder.shareView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                intent.putExtra(Intent.EXTRA_SUBJECT,
                        String.format(mActivity.getString(R.string.commit_shared_from_mgerrit),
                                mCommit.getChangeId()));
                intent.putExtra(Intent.EXTRA_TEXT, mCommit.getWebAddress() + " #mGerrit");
                mActivity.startActivity(intent);
            }
        });
        viewHolder.browserView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCommit.getWebAddress() != null) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(mCommit.getWebAddress()));
                    mActivity.startActivity(browserIntent);
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
            viewHolder.message.setText(mCommit.getMessage());
            viewHolder.changedFiles.setText(
                    buildChangedFilesString(mCommit.getChangedFiles()));
        } else {
            viewHolder.message.setVisibility(View.GONE);
            viewHolder.changedFiles.setVisibility(View.GONE);
        }
    }

    @Override
    protected int getCardLayoutId() {
        return R.layout.commit_card;
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

    private static class ViewHolder {
        TextView owner;
        TextView project;
        TextView cardTitle;
        TextView updated;
        TextView status;
        ImageView browserView;
        ImageView shareView;
        ImageView moarInfo;
        TextView message;
        TextView changedFiles;
    }
}
