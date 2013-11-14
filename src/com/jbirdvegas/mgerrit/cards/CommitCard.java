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
import com.jbirdvegas.mgerrit.GerritControllerActivity;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.objects.FileInfo;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

import java.util.Arrays;
import java.util.List;

public class CommitCard extends RecyclableCard {
    private static final String TAG = CommitCard.class.getSimpleName();

    private final RequestQueue mRequestQuery;
    private final GerritControllerActivity mActivity;
    private final int mGreen;
    private final int mRed;
    private final int mOrange;
    private JSONCommit mCommit;

    public CommitCard(JSONCommit commit,
                      RequestQueue requestQueue,
                      GerritControllerActivity activity) {
        this.mCommit = commit;
        this.mRequestQuery = requestQueue;
        this.mActivity = activity;

        this.mGreen = mActivity.getResources().getColor(R.color.text_green);
        this.mRed = mActivity.getResources().getColor(R.color.text_red);
        this.mOrange = mActivity.getResources().getColor(android.R.color.holo_orange_light);
    }

    @Override
    protected void applyTo(View commitCardView) {
        // I hate UI code so instead of embedding a LinearLayout for just an
        // ImageView with an associated TextView we just use the TextView's
        // built in CompoundDrawablesWithIntrinsicBounds(Drawable, Drawable, Drawable, Drawable)
        // to handle the layout work. This also has a benefit of better performance!

        ViewHolder viewHolder = (ViewHolder) commitCardView.getTag();
        if (commitCardView.getTag() == null) {
            viewHolder = new ViewHolder(commitCardView);
            commitCardView.setTag(viewHolder);
        }

        // set the text
        if (mCommit.getOwnerObject() != null) {
            viewHolder.owner.setText(mCommit.getOwnerObject().getName());
            viewHolder.owner.setTag(mCommit.getOwnerObject());
            viewHolder.owner.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Prefs.setTrackingUser(mActivity, mCommit.getOwnerObject());
                }
            });
            GravatarHelper.attachGravatarToTextView(viewHolder.owner,
                    mCommit.getOwnerObject().getEmail(),
                    mRequestQuery);
        }

        viewHolder.project.setText(mCommit.getProject());
        viewHolder.project.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setCurrentProject(mActivity, mCommit.getProject());
            }
        });

        viewHolder.cardTitle.setText(mCommit.getSubject());
        viewHolder.updated.setText(mCommit.getLastUpdatedDate(mActivity));

        String statusText = mCommit.getStatus().toString();
        switch (statusText) {
            case "MERGED":
                viewHolder.status.setBackgroundColor(mGreen);
                break;
            case "ABANDONED":
                viewHolder.status.setBackgroundColor(mRed);
                break;
            default:
                viewHolder.status.setBackgroundColor(mOrange);
                break;
        }

        View.OnClickListener cardListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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

    public void update(JSONCommit commit) {
        this.mCommit = commit;
    }

    public JSONCommit getJsonCommit() {
        return mCommit;
    }

    /**
     * returns the FileInfo list as a string
     *
     * @param fileList List of ChangedFiles
     * @return String representation of list
     */
    private String buildChangedFilesString(List<FileInfo> fileList) {
        return Arrays.toString(fileList.toArray());
    }

    private static class ViewHolder {
        TextView owner;
        TextView project;
        TextView cardTitle;
        TextView updated;
        View status;
        ImageView browserView;
        ImageView shareView;
        ImageView moarInfo;
        TextView message;
        TextView changedFiles;

        ViewHolder(View view) {
            owner = (TextView) view.findViewById(R.id.commit_card_commit_owner);
            project = (TextView) view.findViewById(R.id.commit_card_project_name);
            cardTitle = (TextView) view.findViewById(R.id.commit_card_title);
            updated = (TextView) view.findViewById(R.id.commit_card_last_updated);
            status = view.findViewById(R.id.commit_card_commit_status);
            browserView = (ImageView) view.findViewById(R.id.commit_card_view_in_browser);
            shareView = (ImageView) view.findViewById(R.id.commit_card_share_info);
            moarInfo = (ImageView) view.findViewById(R.id.commit_card_moar_info);
            message = (TextView) view.findViewById(R.id.commit_card_message);
            changedFiles = (TextView) view.findViewById(R.id.commit_card_changed_files);
        }
    }
}
