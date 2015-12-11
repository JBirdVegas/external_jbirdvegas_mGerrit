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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
        init(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mParent = this.getActivity();
    }

    private void init(Bundle savedInstanceState) {
        View currentFragment = this.getView();
        mMessage = (TextView) currentFragment.findViewById(R.id.new_comment_message);

        Bundle args = getArguments();
        mChangeId = args.getString(CHANGE_ID);

        String message;
        if (savedInstanceState == null) {
            message = args.getString(MESSAGE);
            mCacheKey = CacheManager.getCommentKey(mChangeId);
            String cachedMessage = new CacheManager<String>().get(mCacheKey, String.class, false);
            if (cachedMessage != null && cachedMessage.length() > 0) {
                launchRestoreMessageDialog(mParent, cachedMessage);
            }

        } else {
            message = savedInstanceState.getString(MESSAGE);
        }
        if (message != null && mMessage.length() < 1) mMessage.setText(message);

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
        outState.putString(MESSAGE, mMessage.getText().toString());
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
        mEventBus.unregister(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mEventBus.register(this);
    }

    /**
     * Call the service to post the current message and labels for this change
     */
    public void addComment() {
        String message = mMessage.getText().toString();
        Bundle review = mLabelsFragment.getReview();

        Bundle bundle = new Bundle();
        bundle.putString(GerritService.CHANGE_ID, mChangeId);
        bundle.putString(GerritService.REVIEW_MESSAGE, message);
        bundle.putBundle(GerritService.CHANGE_LABELS, review);
        GerritService.sendRequest(mParent, GerritService.DataType.Comment, bundle);
    }

    /**
     * Whether this change can be reviewed (i.e. labels can be applied to this change by any user)
     * Gerrit always permits adding comments to a change */
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

    /**
     * Launch a dialog for whether to save the message or discard it
     * @param context
     */
    public boolean launchSaveMessageDialog(final Context context) {
        final String message = mMessage.getText().toString();
        if (mMessage.length() < 1) return false;

        AlertDialog.Builder ad = new AlertDialog.Builder(context)
                .setMessage(R.string.review_discard_confirm)
                .setPositiveButton(R.string.review_discard_option, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        // Discard the message from the cache
                        String key = CacheManager.getCommentKey(mChangeId);
                        CacheManager.remove(key, false);
                        dialog.dismiss();
                        CommentFragment.this.mParent.supportFinishAfterTransition();
                    }
                })
                .setNeutralButton(
                        R.string.review_save_option, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Save the message into the cache
                                String key = CacheManager.getCommentKey(mChangeId);
                                CacheManager.put(key, message, false);
                                CommentFragment.this.mParent.finish();
                            }
                        })
                .setNegativeButton(
                        R.string.review_cancel_option, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }
                );
        ad.create().show();
        return true;
    }

    /**
     * Launch a dialog for whether to restore the cached message or start fresh
     * @param context
     */
    public void launchRestoreMessageDialog(final Context context, final String message) {
        AlertDialog.Builder ad = new AlertDialog.Builder(context)
            .setMessage(R.string.review_restore_confirm)
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {
                    mMessage.setText(message);
                    dialog.dismiss();
                }
            })
                .setNegativeButton(R.string.review_restore_no_option, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        String key = CacheManager.getCommentKey(mChangeId);
                        CacheManager.remove(key, false);
                        dialog.dismiss();
                }
            });
        ad.create().show();
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
