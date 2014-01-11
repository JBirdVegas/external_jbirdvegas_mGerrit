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
import android.support.v4.app.FragmentActivity;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.fima.cardsui.objects.RecyclableCard;
import com.jbirdvegas.mgerrit.PatchSetViewerFragment;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.objects.CommitterObject;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

public class PatchSetPropertiesCard extends RecyclableCard {
    private final JSONCommit mJSONCommit;
    private final PatchSetViewerFragment mPatchSetViewerFragment;
    private final RequestQueue mRequestQuery;
    private final Context mContext;
    private final FragmentActivity mActivity;

    public PatchSetPropertiesCard(JSONCommit commit,
                                  PatchSetViewerFragment fragment,
                                  RequestQueue requestQueue,
                                  Context context) {
        this.mJSONCommit = commit;
        this.mPatchSetViewerFragment = fragment;
        this.mRequestQuery = requestQueue;
        this.mContext = context;
        this.mActivity = (FragmentActivity) mContext;
    }

    @Override
    protected void applyTo(View convertView) {

        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        if (convertView.getTag() == null) {
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }

        viewHolder.subject.setText(mJSONCommit.getSubject());
        viewHolder.branch.setText(mJSONCommit.getBranch());

        setImageCaption(viewHolder.owner, R.string.commit_owner, mJSONCommit.getOwnerObject().getName());
        setImageCaption(viewHolder.committer, R.string.commit_owner, mJSONCommit.getOwnerObject().getName());

        String topic = mJSONCommit.getTopic();
        if (topic != null && !topic.isEmpty()) {
            viewHolder.topic.setText(topic);
            viewHolder.topic.setVisibility(View.VISIBLE);
        } else {
            viewHolder.topic.setVisibility(View.GONE);
        }

        // attach owner's gravatar
        GravatarHelper.attachGravatarToTextView(
                viewHolder.owner,
                mJSONCommit.getOwnerObject().getEmail(),
                mRequestQuery);
        viewHolder.owner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTrackingUser(mJSONCommit.getOwnerObject());
            }
        });
        viewHolder.owner.setTag(mJSONCommit.getOwnerObject());
        setContextMenu(viewHolder.owner);
        setClicksToActionViews(
                (ImageView) convertView.findViewById(R.id.properties_card_share_info),
                (ImageView) convertView.findViewById(R.id.properties_card_view_in_browser));
        try {
            // set text will throw NPE if we don't have author/committer objects
            setImageCaption(viewHolder.author, R.string.commit_author,
                    mJSONCommit.getAuthorObject().getName());
            viewHolder.author.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setTrackingUser(mJSONCommit.getAuthorObject());
                }
            });

            setImageCaption(viewHolder.committer, R.string.commit_committer,
                    mJSONCommit.getCommitterObject().getName());
            viewHolder.committer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setTrackingUser(mJSONCommit.getCommitterObject());
                }
            });
            viewHolder.author.setTag(mJSONCommit.getAuthorObject());
            setContextMenu(viewHolder.author);
            viewHolder.committer.setTag(mJSONCommit.getCommitterObject());
            setContextMenu(viewHolder.committer);
            // attach gravatars (if objects are not null)
            GravatarHelper.attachGravatarToTextView(
                    viewHolder.author,
                    mJSONCommit.getAuthorObject().getEmail(),
                    mRequestQuery);
            GravatarHelper.attachGravatarToTextView(
                    viewHolder.committer,
                    mJSONCommit.getCommitterObject().getEmail(),
                    mRequestQuery);
        } catch (NullPointerException npe) {
            convertView.findViewById(R.id.prop_card_author)
                    .setVisibility(View.GONE);
            convertView.findViewById(R.id.prop_card_committer)
                    .setVisibility(View.GONE);
        }
    }

    @Override
    protected int getCardLayoutId() {
        return R.layout.properties_card;
    }

    private void setClicksToActionViews(ImageView share, ImageView browser) {
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                intent.putExtra(Intent.EXTRA_SUBJECT,
                        String.format(view.getContext().getString(R.string.commit_shared_from_mgerrit),
                                mJSONCommit.getChangeId()));
                intent.putExtra(Intent.EXTRA_TEXT, mJSONCommit.getWebAddress() + " #mGerrit");
                view.getContext().startActivity(intent);
            }
        });
        browser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(mJSONCommit.getWebAddress()));
                view.getContext().startActivity(browserIntent);
            }
        });
    }

    private void setContextMenu(TextView textView) {
        mPatchSetViewerFragment.registerViewForContextMenu(textView);
    }

    private void setImageCaption(TextView textView, int resID, String authorName) {
        String title = mContext.getResources().getString(resID);
        if (title == null || authorName == null) return;

        SpannableString text = new SpannableString(title + "\n" + authorName);
        text.setSpan(new TextAppearanceSpan(mContext, R.style.CardText_CommitOwnerText),
                0, title.length(), 0);

        int end = title.length()+1;
        text.setSpan(new TextAppearanceSpan(mContext, R.style.CardText_CommitOwnerDetails),
                end, end+authorName.length(), 0);

        textView.setText(text, TextView.BufferType.SPANNABLE);
    }

    private void setTrackingUser(CommitterObject user) {
        Prefs.setTrackingUser(mContext, user);
        if (!Prefs.isTabletMode(mContext)) mActivity.finish();
    }

    private static class ViewHolder {
        TextView subject;
        TextView owner;
        TextView author;
        TextView committer;
        TextView branch;
        TextView topic;

        ViewHolder(View view) {
            subject = (TextView) view.findViewById(R.id.prop_card_subject);
            owner = (TextView) view.findViewById(R.id.prop_card_owner);
            author = (TextView) view.findViewById(R.id.prop_card_author);
            committer = (TextView) view.findViewById(R.id.prop_card_committer);
            branch = (TextView) view.findViewById(R.id.prop_card_branch);
            topic = (TextView) view.findViewById(R.id.prop_card_topic);
        }
    }
}