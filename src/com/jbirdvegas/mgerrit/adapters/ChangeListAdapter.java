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
import android.database.Cursor;
import android.util.Pair;
import android.view.View;
import android.widget.SimpleCursorAdapter;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.cards.CommitCard;
import com.jbirdvegas.mgerrit.cards.CommitCardBinder;
import com.jbirdvegas.mgerrit.database.SelectedChange;
import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.helpers.Tools;
import com.jbirdvegas.mgerrit.message.NewChangeSelected;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import de.greenrobot.event.EventBus;

public class ChangeListAdapter extends SimpleCursorAdapter {

    Context mContext;

    private Integer mUserId_index;
    private Integer mUserName_index;
    private Integer mProject_index;

    private String selectedChangeId;
    private CommitCard selectedChangeView;


    public ChangeListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags,
                             String status) {
        super(context, layout, c, from, to, flags);
        mContext = context;

        String statusString = JSONCommit.Status.getStatusString(status);
        Pair<String, Integer> change = SelectedChange.getSelectedChange(context, statusString);

        if (change != null) {
            // We only need the changeid
            selectedChangeId = change.first;
        }
    }

    @Override
    public void bindView(@NotNull View view, Context context, @NotNull final Cursor cursor) {

        setIndicies(cursor);

        TagHolder tagHolder = new TagHolder(context, cursor);
        view.setTag(tagHolder);

        if (tagHolder.changeid.equals(selectedChangeId)) {
            CommitCard commitCard = (CommitCard) view;
            commitCard.setChangeSelected(true);
            selectedChangeView = commitCard;
        } else {
            ((CommitCard) view).setChangeSelected(false);
        }

        view.setTag(R.id.changeID, tagHolder.changeid);
        view.setTag(R.id.changeNumber, tagHolder.changeNumber);
        view.setTag(R.id.user, cursor.getInt(mUserId_index));
        view.setTag(R.id.userName, cursor.getString(mUserName_index));
        view.setTag(R.id.project, cursor.getString(mProject_index));
        view.setTag(R.id.webAddress, tagHolder.webAddress);

        super.bindView(view, context, cursor);
    }

    @Nullable
    @Override
    public Object getItem(int position) {
        Cursor cursor = getCursor();
        if (cursor == null) return null;
        else {
            cursor.moveToPosition(position);
            return cursor;
        }
    }

    public void itemClickListener(View view) {
        TagHolder vh = (TagHolder) view.getTag();
        EventBus.getDefault().post(new NewChangeSelected(vh.changeid, vh.changeNumber, vh.changeStatus, true));

        // Set this view as selected
        setSelectedChangeId((CommitCard) view, vh.changeid);
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
            TagHolder tagHolder = (TagHolder) selectedChangeView.getTag();
            if (tagHolder.changeid.equals(this.selectedChangeId)) {
                selectedChangeView.setChangeSelected(false);
            }
        }

        selectedChangeView = card;
        this.selectedChangeId = selectedChangeId;
        card.setChangeSelected(true);
    }

    private void setIndicies(@NotNull Cursor cursor) {
        // These indices will not change regardless of the view
        if (mUserId_index == null) {
            mUserId_index = cursor.getColumnIndex(UserChanges.C_USER_ID);
        }
        if (mUserName_index == null) {
            mUserName_index = cursor.getColumnIndex(UserChanges.C_NAME);
        }
        if (mProject_index == null) {
            mProject_index = cursor.getColumnIndex(UserChanges.C_PROJECT);
        }
    }

    @Override
    public Cursor swapCursor(Cursor c) {
        CommitCardBinder binder = (CommitCardBinder) getViewBinder();
        if (binder != null ) {
            binder.onCursorChanged();
        }

        mUserId_index = null;
        mUserName_index = null;
        mProject_index = null;

        return super.swapCursor(c);
    }

    private static class TagHolder {
        String changeid;
        int changeNumber;
        String changeStatus;
        String webAddress;

        TagHolder(Context context, Cursor cursor) {
            changeid = cursor.getString(cursor.getColumnIndex(UserChanges.C_CHANGE_ID));
            changeNumber = cursor.getColumnIndex(UserChanges.C_COMMIT_NUMBER);
            changeStatus = cursor.getString(cursor.getColumnIndex(UserChanges.C_STATUS));
            webAddress = Tools.getWebAddress(context,
                    cursor.getInt(cursor.getColumnIndex(UserChanges.C_COMMIT_NUMBER)));
        }
    }
}
