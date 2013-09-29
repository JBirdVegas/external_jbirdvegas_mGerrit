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
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;
import com.fima.cardsui.objects.Card;
import com.jbirdvegas.mgerrit.PatchSetViewerFragment;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.caches.BitmapLruCache;
import com.jbirdvegas.mgerrit.helpers.EmoticonSupportHelper;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.listeners.TrackingClickListener;
import com.jbirdvegas.mgerrit.objects.CommitComment;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

import java.util.LinkedList;

public class PatchSetCommentsCard extends Card {

    private JSONCommit mJsonCommit;
    private final PatchSetViewerFragment mPatchsetViewerFragment;
    private RequestQueue mRequestQuery;

    public PatchSetCommentsCard(JSONCommit jsonCommit, PatchSetViewerFragment activity, RequestQueue requestQueue) {
        mJsonCommit = jsonCommit;
        mPatchsetViewerFragment = activity;
        mRequestQuery = requestQueue;
    }

    private LayoutInflater mInflater;
    private Context mContext;
    private ViewGroup mRootView;

    @Override
    public View getCardContent(Context context) {
        mRequestQuery = Volley.newRequestQueue(context);
        mContext = context;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRootView = (ViewGroup) mInflater.inflate(R.layout.comments_card, null);
        LinkedList<CommitComment> commentsList = (LinkedList<CommitComment>) mJsonCommit.getMessagesList();
        // make and add a view for each comment
        for (CommitComment comment : commentsList) {
            mRootView.addView(getCommentView(comment));
        }
        return mRootView;
    }

    public View getCommentView(final CommitComment comment) {
        View commentView = mInflater.inflate(R.layout.commit_comment, null);
        // set author name
        TextView authorTextView = (TextView) commentView.findViewById(R.id.comment_author_name);
        authorTextView.setText(comment.getAuthorObject().getName());
        authorTextView.setOnClickListener(
                new TrackingClickListener(mContext,
                        comment.getAuthorObject()));

        authorTextView.setTag(comment.getAuthorObject());
        mPatchsetViewerFragment.registerViewForContextMenu(authorTextView);
        // setup styled comments
        TextView commentMessage = (TextView) commentView.findViewById(R.id.comment_message);
        // use Linkify to automatically linking http/email/addresses
        Linkify.addLinks(commentMessage, Linkify.ALL);
        // replace replace emoticons with drawables
        commentMessage.setText(EmoticonSupportHelper.getSmiledText(mContext, comment.getMessage()));
        // set gravatar icon for commenter
        GravatarHelper.populateProfilePicture(
                (ImageView) commentView.findViewById(R.id.comment_gravatar),
                comment.getAuthorObject().getEmail(),
                mRequestQuery);
        NetworkImageView gravatar = (NetworkImageView) commentView.findViewById(R.id.comment_gravatar);

        gravatar.setImageUrl(GravatarHelper.getGravatarUrl(comment.getAuthorObject().getEmail()),
                new ImageLoader(mRequestQuery, new BitmapLruCache(mContext)));
        return commentView;
    }
}
