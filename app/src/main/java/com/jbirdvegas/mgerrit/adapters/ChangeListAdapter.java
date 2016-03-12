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
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SimpleCursorAdapter;

import com.jbirdvegas.mgerrit.fragments.PrefsFragment;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.cards.CommitCard;
import com.jbirdvegas.mgerrit.cards.CommitCardBinder;
import com.jbirdvegas.mgerrit.database.SelectedChange;
import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.helpers.Tools;
import com.jbirdvegas.mgerrit.message.NewChangeSelected;
import com.jbirdvegas.mgerrit.objects.Categorizable;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.jbirdvegas.mgerrit.tasks.GerritService;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.Locale;
import java.util.TimeZone;


public class ChangeListAdapter extends SimpleCursorAdapter implements Categorizable,
        PopupMenu.OnMenuItemClickListener {

    Context mContext;

    private Integer mUserId_index;
    private Integer mUserName_index;
    private Integer mProject_index;

    private String selectedChangeId;
    private View selectedChangeView;

    // When a popup menu is expanded, this is set to the view where all the associated data is bound
    private View mPopupMenuTagHolder;

    private final Locale mLocale;
    private final TimeZone mServerTimeZone, mLocalTimeZone;
    private Integer mDateColumnIndex;

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

        mServerTimeZone = PrefsFragment.getServerTimeZone(context);
        mLocalTimeZone = PrefsFragment.getLocalTimeZone(context);
        mLocale = context.getResources().getConfiguration().locale;
    }

    @Override
    public void bindView(@NotNull View view, Context context, @NotNull final Cursor cursor) {

        setIndicies(cursor);

        TagHolder tagHolder = new TagHolder(context, cursor);
        view.setTag(tagHolder);

        if (tagHolder.changeid.equals(selectedChangeId)) {
            CommitCard commitCard = (CommitCard) view.findViewById(R.id.commit_card_wrapper);
            commitCard.setChangeSelected(true);
            selectedChangeView = view;
        } else {
            CommitCard commitCard = (CommitCard) view.findViewById(R.id.commit_card_wrapper);
            commitCard.setChangeSelected(false);
        }

        ImageView settings = (ImageView) view.findViewById(R.id.commit_card_settings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopup(v);
            }
        });

        view.setTag(R.id.changeID, tagHolder.changeid);
        view.setTag(R.id.changeNumber, tagHolder.changeNumber);
        view.setTag(R.id.user, cursor.getInt(mUserId_index));
        view.setTag(R.id.userName, cursor.getString(mUserName_index));
        view.setTag(R.id.project, cursor.getString(mProject_index));
        view.setTag(R.id.webAddress, tagHolder.webAddress);
        view.setTag(R.id.starred, tagHolder.isStarred);

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
        EventBus.getDefault().postSticky(new NewChangeSelected(vh.changeid, vh.changeNumber, vh.changeStatus, true));

        // Set this view as selected
        setSelectedChangeId(view, vh.changeid);
    }

    /**
     * Notify the adapter that a new changeid has been selected.
     * This will refresh the adapter, forcing each view to refresh
     * and ensuring that only the view specified has its change selected state set
     *
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
            CommitCard commitCard = (CommitCard) selectedChangeView.findViewById(R.id.commit_card_wrapper);
            commitCard.setChangeSelected(false);
            selectedChangeView = null;
        }
        this.notifyDataSetChanged();
    }

    private void setSelectedChangeId(View card, String selectedChangeId) {
        //  Only invalidate the view if the changeid matches (i.e. it hasn't already been recycled)
        if (selectedChangeView != null) {
            TagHolder tagHolder = (TagHolder) selectedChangeView.getTag();
            if (tagHolder.changeid.equals(this.selectedChangeId)) {
                CommitCard commitCard = (CommitCard) selectedChangeView.findViewById(R.id.commit_card_wrapper);
                commitCard.setChangeSelected(false);
            }
        }

        selectedChangeView = card;
        this.selectedChangeId = selectedChangeId;
        CommitCard commitCard = (CommitCard) selectedChangeView.findViewById(R.id.commit_card_wrapper);
        commitCard.setChangeSelected(true);
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
        if (binder != null) {
            binder.onCursorChanged();
        }

        mUserId_index = null;
        mUserName_index = null;
        mProject_index = null;

        return super.swapCursor(c);
    }

    @Override
    public String categoryName(int position) {
        Cursor c = (Cursor) getItem(position);
        Integer index = getDateColumnIndex(c);
        // Convert to date
        DateTime date = Tools.parseDate(c.getString(index), mServerTimeZone, mLocalTimeZone);
        return DateTimeFormat.forPattern(mContext.getString(R.string.header_date_format)).withLocale(mLocale).print(date);
    }

    @Override
    public long categoryId(int position) {
        Cursor c = (Cursor) getItem(position);
        Integer index = getDateColumnIndex(c);
        // Convert to date
        DateTime date = Tools.parseDate(c.getString(index), mServerTimeZone, mLocalTimeZone);
        return date.getMillis();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        View targetView = mPopupMenuTagHolder;
        String webAddress = (String) mPopupMenuTagHolder.getTag(R.id.webAddress);
        String changeid = (String) mPopupMenuTagHolder.getTag(R.id.changeID);
        switch (item.getItemId()) {
            case R.id.menu_change_details:
                itemClickListener(targetView);
                return true;
            case R.id.menu_change_browser:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webAddress));
                mContext.startActivity(browserIntent);
                return true;
            case R.id.menu_change_track_user:
                int user = (int) mPopupMenuTagHolder.getTag(R.id.user);
                PrefsFragment.setTrackingUser(mContext, user);
                return true;
            case R.id.menu_change_track_project:
                String project = (String) mPopupMenuTagHolder.getTag(R.id.project);
                PrefsFragment.setCurrentProject(mContext, project);
                return true;
            case R.id.menu_change_share:
                Intent intent = Tools.createShareIntent(mContext, changeid, webAddress);
                mContext.startActivity(intent);
                return true;
            case R.id.menu_star_change:
                int changeNumber = (int) mPopupMenuTagHolder.getTag(R.id.changeNumber);
                int isStarred = (int) mPopupMenuTagHolder.getTag(R.id.starred);
                onStarChange(changeid, changeNumber, isStarred != 1);
                notifyDataSetChanged();
            default:
                return false;
        }
    }

    public void showPopup(View view) {
        PopupMenu popup = new PopupMenu(mContext, view);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.change_list_menu);

        View row = (View) view.getParent().getParent();
        mPopupMenuTagHolder = row;

        // Set the title of the user tracking menu item
        MenuItem userMenuItem = popup.getMenu().findItem(R.id.menu_change_track_user);
        String title = String.format(mContext.getResources().getString(R.string.context_menu_track_user),
                (String) row.getTag(R.id.userName));
        userMenuItem.setTitle(title);

        MenuItem starMenuItem = popup.getMenu().findItem(R.id.menu_star_change);
        int starred = (int) row.getTag(R.id.starred);
        starMenuItem.setTitle(starred == 1 ? R.string.menu_unstar_change : R.string.menu_star_change);

        popup.show();
    }

    private Integer getDateColumnIndex(Cursor cursor) {
        if (mDateColumnIndex == null)
            mDateColumnIndex = cursor.getColumnIndex(UserChanges.C_UPDATED);
        return mDateColumnIndex;
    }

    private void onStarChange(String changeId, int changeNumber, boolean starred) {
        Intent it = new Intent(mContext, GerritService.class);
        it.putExtra(GerritService.DATA_TYPE_KEY, GerritService.DataType.Star);
        it.putExtra(GerritService.CHANGE_ID, changeId);
        it.putExtra(GerritService.CHANGE_NUMBER, changeNumber);
        it.putExtra(GerritService.IS_STARRING, starred);
        mContext.startService(it);
    }

    private static class TagHolder {
        String changeid;
        int changeNumber;
        String changeStatus;
        String webAddress;
        int isStarred;

        TagHolder(Context context, Cursor cursor) {
            changeid = cursor.getString(cursor.getColumnIndex(UserChanges.C_CHANGE_ID));
            changeNumber = cursor.getInt(cursor.getColumnIndex(UserChanges.C_COMMIT_NUMBER));
            changeStatus = cursor.getString(cursor.getColumnIndex(UserChanges.C_STATUS));
            webAddress = Tools.getWebAddress(context,
                    cursor.getInt(cursor.getColumnIndex(UserChanges.C_COMMIT_NUMBER)));
            isStarred = cursor.getInt(cursor.getColumnIndex(UserChanges.C_STARRED));
        }
    }
}
