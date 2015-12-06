/*
 * Copyright (C) 2015 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2015
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

package com.jbirdvegas.mgerrit.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.activities.ReviewActivity;
import com.jbirdvegas.mgerrit.message.CacheDataRetrieved;
import com.jbirdvegas.mgerrit.message.Finished;
import com.jbirdvegas.mgerrit.objects.CacheManager;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.jbirdvegas.mgerrit.tasks.GerritService;

import java.io.Serializable;

import de.greenrobot.event.EventBus;

public class CommentFragment extends Fragment {

    public static final String CHANGE_ID = PatchSetViewerFragment.CHANGE_ID;
    public static final String CHANGE_STATUS = PatchSetViewerFragment.STATUS;
    public static final String MESSAGE = "message";

    private FragmentActivity mParent;
    private TextView mMessage;
    private View mReviewFragment;

    private String mChangeId;
    private LabelsFragment mLabelsFragment;
    private String mStatus;

    // The key to save the comment into the cache, it must be unique to this change
    private String mCacheKey;
    private EventBus mEventBus;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_comment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        init();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mParent = this.getActivity();
    }

    private void init() {
        View currentFragment = this.getView();
        mMessage = (TextView) currentFragment.findViewById(R.id.new_comment_message);

        Bundle args = getArguments();
        mChangeId = args.getString(CHANGE_ID);
        mStatus = args.getString(CHANGE_STATUS);

        mReviewFragment = currentFragment.findViewById(R.id.review_fragment);
        if (canChangeBeReviewed()) {
            hideLabels(false);

            mLabelsFragment = new LabelsFragment();
            mLabelsFragment.setArguments(args);
            getChildFragmentManager().beginTransaction().replace(R.id.review_fragment, mLabelsFragment).commit();
        } else {
            hideLabels(true);
        }

        mCacheKey = CacheManager.getCommentKey(mChangeId);
        mEventBus = EventBus.getDefault();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CHANGE_ID, mChangeId);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mChangeId = savedInstanceState.getString(CHANGE_ID);
        }
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onPause() {
        // Save the message into the cache
        String message = mMessage.getText().toString();
        if (message.length() > 0) {
            CacheManager.put(mCacheKey, message, true);
        }
        mEventBus.unregister(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mEventBus.register(this);

        if (mChangeId != null) {
            mCacheKey = CacheManager.getCommentKey(mChangeId);
            new CacheManager<String>().get(mCacheKey, String.class, true);
        }
    }

    public void addComment() {
        String message = mMessage.getText().toString();
        Bundle review = mLabelsFragment.getReview();

        Bundle bundle = new Bundle();
        bundle.putString(GerritService.CHANGE_ID, mChangeId);
        bundle.putString(GerritService.REVIEW_MESSAGE, message);
        bundle.putBundle(GerritService.CHANGE_LABELS, review);
        GerritService.sendRequest(mParent, GerritService.DataType.Comment, bundle);
    }

    private boolean canChangeBeReviewed() {
        return mStatus == null || mStatus.equals(JSONCommit.Status.NEW.toString());
    }

    /**
     * Hide or show the labels fragment
     * @param hide true hides the labels fragment and makes the comment full height, false to
     *             show the labels fragment
     */
    private void hideLabels(boolean hide) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        if (hide) {
            mReviewFragment.setVisibility(View.GONE);
            lp.weight = 1;
            mMessage.setLayoutParams(lp);
        } else {
            mReviewFragment.setVisibility(View.VISIBLE);
            lp.weight = 0.78f;
            mMessage.setLayoutParams(lp);
        }
    }

    @Keep
    public void onEventMainThread(CacheDataRetrieved<String> ev) {
        if (ev.getKey().equals(mCacheKey) && mMessage != null) {
            if (mMessage.length() < 1) {
                mMessage.setText(ev.getData());
            }
        }
    }

    @Keep
    public void onEventMainThread(Finished ev) {
        Serializable dataType = ev.getIntent().getSerializableExtra(GerritService.DATA_TYPE_KEY);
        if (ev.getItems() < 1 && dataType == GerritService.DataType.Comment) {
            // Commented successfully, remove comment from cache and go back to the change details
            if (mParent instanceof ReviewActivity) {
                ((ReviewActivity) mParent).onCommented(mCacheKey, mChangeId);
            }
        }
    }
}
