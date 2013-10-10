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
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.fima.cardsui.objects.RecyclableCard;
import com.jbirdvegas.mgerrit.PatchSetViewerFragment;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.caches.BitmapLruCache;
import com.jbirdvegas.mgerrit.helpers.EmoticonSupportHelper;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.listeners.TrackingClickListener;
import com.jbirdvegas.mgerrit.objects.CommitComment;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

import java.util.LinkedList;

public class PatchSetCommentsCard extends RecyclableCard {

    private JSONCommit mJsonCommit;
    private final PatchSetViewerFragment mPatchsetViewerFragment;
    private RequestQueue mRequestQuery;
    private Context mContext;

    public PatchSetCommentsCard(JSONCommit jsonCommit, PatchSetViewerFragment fragment, RequestQueue requestQueue) {
        mJsonCommit = jsonCommit;
        mPatchsetViewerFragment = fragment;
        mRequestQuery = requestQueue;

        mContext = fragment.getActivity();
    }

    @Override
    protected void applyTo(View convertView) {
        LinkedList<CommitComment> commentsList = (LinkedList<CommitComment>) mJsonCommit.getMessagesList();

        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        if (convertView.getTag() == null) {
            viewHolder = new ViewHolder();
            viewHolder.viewGroup = (ViewGroup) convertView.findViewById(R.id.comments_list);
            convertView.setTag(viewHolder);
        }

        if (commentsList != null) {
            viewHolder.viewGroup.removeAllViews();

            // make and add a view for each comment
            for (CommitComment comment : commentsList) {
                viewHolder.viewGroup.addView(getCommentView(comment));
            }
        }
    }

    private View getCommentView(final CommitComment comment) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View commentView = inflater.inflate(R.layout.commit_comment, null);
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
        NetworkImageView gravatar = (NetworkImageView) commentView.findViewById(R.id.comment_gravatar);
        gravatar.setImageUrl(GravatarHelper.getGravatarUrl(comment.getAuthorObject().getEmail()),
                new ImageLoader(mRequestQuery, new BitmapLruCache(mContext)));
        return commentView;
    }

    @Override
    protected int getCardLayoutId() {
        return R.layout.comments_card;
    }

    private static class ViewHolder {
        static ViewGroup viewGroup;
    }
}
