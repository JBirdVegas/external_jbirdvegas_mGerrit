package com.jbirdvegas.mgerrit.adapters;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2013
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
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.jbirdvegas.mgerrit.PatchSetViewerFragment;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.cards.CommitCard;
import com.jbirdvegas.mgerrit.cards.CommitCardBinder;
import com.jbirdvegas.mgerrit.database.SelectedChange;
import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

public class ChangeListAdapter extends SimpleCursorAdapter {

    private final String mStatus;
    Context mContext;

    // Cursor indices
    private Integer changeid_index;
    private Integer changenum_index;
    private Integer status_index;

    private String selectedChangeId;
    private CommitCard selectedChangeView;

    public ChangeListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags,
                             String status) {
        super(context, layout, c, from, to, flags);
        mContext = context;

        mStatus = JSONCommit.Status.getStatusString(status);
        selectedChangeId = SelectedChange.getSelectedChange(context, mStatus);
    }

    @Override
    public void bindView(View view, Context context, final Cursor cursor) {

        setIndicies(cursor);

        ViewHolder viewHolder = (ViewHolder) view.getTag();
        if (viewHolder == null) {
            viewHolder = new ViewHolder(view);
        }

        viewHolder.changeid = cursor.getString(changeid_index);
        viewHolder.changeStatus = cursor.getString(status_index);
        viewHolder.webAddress = getWebAddress(cursor.getInt(changenum_index));
        view.setTag(viewHolder);

        if (viewHolder.changeid.equals(selectedChangeId)) {
            CommitCard commitCard = (CommitCard) view;
            commitCard.setChangeSelected(true);
            selectedChangeView = commitCard;
        } else {
            ((CommitCard) view).setChangeSelected(false);
        }

        View.OnClickListener cardListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ViewHolder vh = (ViewHolder) view.getTag();
                Intent intent = new Intent(PatchSetViewerFragment.NEW_CHANGE_SELECTED);
                intent.putExtra(PatchSetViewerFragment.CHANGE_ID, vh.changeid);
                intent.putExtra(PatchSetViewerFragment.STATUS, vh.changeStatus);
                intent.putExtra(PatchSetViewerFragment.EXPAND_TAG, true);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);

                // Set this view as selected
                setSelectedChangeId((CommitCard) view, vh.changeid);
            }
        };

        // Root view already has viewHolder tagged
        view.setOnClickListener(cardListener);

        viewHolder.shareView.setTag(viewHolder);
        viewHolder.shareView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ViewHolder vh = (ViewHolder) view.getTag();
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                intent.putExtra(Intent.EXTRA_SUBJECT,
                        String.format(mContext.getResources().getString(R.string.commit_shared_from_mgerrit),
                                vh.changeid));
                intent.putExtra(Intent.EXTRA_TEXT, vh.webAddress + " #mGerrit");
                mContext.startActivity(intent);
            }
        });

        viewHolder.browserView.setTag(viewHolder);
        viewHolder.browserView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String webAddr = ((ViewHolder) view.getTag()).webAddress;
                if (webAddr != null) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(webAddr));
                    mContext.startActivity(browserIntent);
                } else {
                    Toast.makeText(view.getContext(),
                            R.string.failed_to_find_url,
                            Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });

        super.bindView(view, context, cursor);
    }

    private String getWebAddress(int commitNumber) {
        return String.format("%s#/c/%d/", Prefs.getCurrentGerrit(mContext), commitNumber);
    }

    /**
     * Notify the adapter that a new changeid has been selected.
     *  This will refresh the adapter, forcing each view to refresh
     *  and ensuring that only the view specified has its change selected state set
     * @param change The id of the change that was selected
     */
    public void setSelectedChangeId(String change) {
        // Check if pre-condition is statisfied
        if (change == null || change.isEmpty()) return;

        // Check if there is any work to do here
        if (this.selectedChangeId != null && this.selectedChangeId.equals(change)) {
            return;
        }

        this.selectedChangeId = change;
        /* We need to refresh the view that holds the selectedChange.
         *  Since we cannot get the view and refresh it directly (it may be off-screen) we
         *  have to refresh all the views in the adapter.
         * Set the previous selected change view to unselected (even if it was recycled, we
         *  will still refresh it.
         */
        if (selectedChangeView != null) {
            selectedChangeView.setChangeSelected(false);
            selectedChangeView = null;
        }
        this.notifyDataSetChanged();
    }

    private void setSelectedChangeId(CommitCard card, String selectedChangeId) {
        //  Only invalidate the view if the changeid matches (i.e. it hasn't already been recycled)
        if (selectedChangeView != null) {
            ViewHolder viewHolder = (ViewHolder) selectedChangeView.getTag();
            if (viewHolder.changeid.equals(this.selectedChangeId)) {
                selectedChangeView.setChangeSelected(false);
            }
        }

        selectedChangeView = card;
        this.selectedChangeId = selectedChangeId;
        card.setChangeSelected(true);
    }

    private void setIndicies(Cursor cursor) {
        // These indices will not change regardless of the view
        if (changeid_index == null) {
            changeid_index = cursor.getColumnIndex(UserChanges.C_CHANGE_ID);
        }
        if (changenum_index == null) {
            changenum_index = cursor.getColumnIndex(UserChanges.C_COMMIT_NUMBER);
        }
        if (status_index == null) {
            status_index = cursor.getColumnIndex(UserChanges.C_STATUS);
        }
    }

    @Override
    public Cursor swapCursor(Cursor c) {
        CommitCardBinder binder = (CommitCardBinder) getViewBinder();
        if (binder != null ) {
            binder.onCursorChanged();
        }

        changeid_index = null;
        changenum_index = null;
        status_index = null;

        return super.swapCursor(c);
    }

    private static class ViewHolder {
        ImageView browserView;
        ImageView shareView;

        String changeid;
        String changeStatus;
        String webAddress;

        ViewHolder(View view) {
            browserView = (ImageView) view.findViewById(R.id.commit_card_view_in_browser);
            shareView = (ImageView) view.findViewById(R.id.commit_card_share_info);
        }
    }
}
