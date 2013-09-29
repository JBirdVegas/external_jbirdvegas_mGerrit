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
import com.android.volley.RequestQueue;
import com.fima.cardsui.objects.Card;
import com.jbirdvegas.mgerrit.PatchSetViewerFragment;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.listeners.TrackingClickListener;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

public class PatchSetPropertiesCard extends Card {
    private final JSONCommit mJSONCommit;
    private final PatchSetViewerFragment mPatchSetViewerFragment;
    private final RequestQueue mRequestQuery;
    private final Context mContext;
    private TextView mSubject;
    private TextView mOwner;
    private TextView mAuthor;
    private TextView mCommitter;

    public PatchSetPropertiesCard(JSONCommit commit,
                                  PatchSetViewerFragment activity,
                                  RequestQueue requestQueue,
                                  Context context) {
        this.mJSONCommit = commit;
        this.mPatchSetViewerFragment = activity;
        this.mRequestQuery = requestQueue;
        this.mContext = context;
    }

    @Override
    public View getCardContent(Context context) {
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootView = inflater.inflate(R.layout.properties_card, null);
        mSubject = (TextView) rootView.findViewById(R.id.prop_card_subject);
        mOwner = (TextView) rootView.findViewById(R.id.prop_card_owner);
        mAuthor = (TextView) rootView.findViewById(R.id.prop_card_author);
        mCommitter = (TextView) rootView.findViewById(R.id.prop_card_committer);

        mSubject.setText(mJSONCommit.getSubject());
        mOwner.setText(mJSONCommit.getOwnerObject().getName());
        // attach owner's gravatar
        GravatarHelper.attachGravatarToTextView(
                mOwner,
                mJSONCommit.getOwnerObject().getEmail(),
                mRequestQuery);
        mOwner.setOnClickListener(new TrackingClickListener(
                mContext,
                mJSONCommit.getOwnerObject()
        ));
        mOwner.setTag(mJSONCommit.getOwnerObject());
        setContextMenu(mOwner);
        setClicksToActionViews(
                (ImageView) rootView.findViewById(R.id.properties_card_share_info),
                (ImageView) rootView.findViewById(R.id.properties_card_view_in_browser));
        try {
            // set text will throw NullPointer if
            // we don't have author/committer objects
            mAuthor.setText(mJSONCommit.getAuthorObject().getName());
            mAuthor.setOnClickListener(
                    new TrackingClickListener(
                            mContext,
                            mJSONCommit.getAuthorObject()));
            mCommitter.setText(mJSONCommit.getCommitterObject().getName());
            mCommitter.setOnClickListener(
                    new TrackingClickListener(
                            mContext,
                            mJSONCommit.getCommitterObject()));
            // setup contextmenu click actions
            mAuthor.setTag(mJSONCommit.getAuthorObject());
            setContextMenu(mAuthor);
            mCommitter.setTag(mJSONCommit.getCommitterObject());
            setContextMenu(mCommitter);
            // attach gravatars (if objects are not null)
            GravatarHelper.attachGravatarToTextView(
                    mAuthor,
                    mJSONCommit.getAuthorObject().getEmail(),
                    mRequestQuery);
            GravatarHelper.attachGravatarToTextView(
                    mCommitter,
                    mJSONCommit.getCommitterObject().getEmail(),
                    mRequestQuery);
        } catch (NullPointerException npe) {
            rootView.findViewById(R.id.prop_card_author)
                    .setVisibility(View.GONE);
            rootView.findViewById(R.id.prop_card_committer)
                    .setVisibility(View.GONE);
        }
        return rootView;
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
}