package com.jbirdvegas.mgerrit.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.cards.CardBinder;
import com.jbirdvegas.mgerrit.cards.PatchSetChangesCard;
import com.jbirdvegas.mgerrit.cards.PatchSetCommentsCard;
import com.jbirdvegas.mgerrit.cards.PatchSetMessageCard;
import com.jbirdvegas.mgerrit.cards.PatchSetPropertiesCard;
import com.jbirdvegas.mgerrit.cards.PatchSetReviewersCard;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/*
 * Copyright (C) 2014 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2014
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
 *
 *  Base adapter for attaching data regarding change details to an
 *   expandableListView. We want to use an array rather than a cursor
 *   for the groups (headings), so we need to extend BaseExpandableListAdapter
 *   rather than CursorTreeAdapter.
 */
public class CommitDetailsAdapter extends BaseExpandableListAdapter {

    private final LayoutInflater mInflator;
    private final Context mContext;

    // Cards supported:
    public enum Cards { PROPERTIES, COMMIT_MSG, CHANGED_FILES, REVIEWERS, COMMENTS }
    private static final int _cards_count = Cards.values().length;

    // Stores the type of card at position
    private ArrayList<ChildGroupDetails> childGroupDetails = new ArrayList<>();

    // Contents for added card list headers
    private ArrayList<String> headerContents = new ArrayList<>(5);

    public CommitDetailsAdapter(Context context) {
        mContext = context;
        mInflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setup();
    }

    @Nullable
    @Override
    public Object getGroup(int groupPosition) {
        return headerContents.get(groupPosition);
    }

    @Nullable
    @Override
    public Object getChild(int groupPosition, int childPosition) {
        ChildGroupDetails details = childGroupDetails.get(groupPosition);
        details.cursor.moveToPosition(childPosition);
        return details.cursor;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return groupPosition*100 + childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Nullable
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflator.inflate(R.layout.card_header, null);
        }

        GroupViewHolder viewHolder = (GroupViewHolder) convertView.getTag();
        if (viewHolder == null) {
            viewHolder = new GroupViewHolder(convertView);
            convertView.setTag(viewHolder);
        }
        String text = headerContents.get(groupPosition);
        // Allow an empty header text
        if (text != null) {
            viewHolder.headerText.setText(text);
        }
        return convertView;
    }

    @Nullable
    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        // Get the set binder and delegate the work to the set binder
        ChildGroupDetails details = childGroupDetails.get(groupPosition);
        Cursor cursor = (Cursor) getChild(groupPosition, childPosition);
        if (cursor == null) return convertView;

        if (cursor.getPosition() < 0) cursor.moveToFirst();
        return details.binder.setViewValue(cursor, convertView, parent);
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public int getGroupCount() {
        return headerContents.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        ChildGroupDetails details = childGroupDetails.get(groupPosition);
        if (details.cursor != null) return details.cursor.getCount();
        else return  0;
    }

    @Override
    public int getChildType(int groupPosition, int childPosition) {
        return groupPosition;
    }

    @Override
    public int getChildTypeCount() {
       return _cards_count; // Number of different cards supported (view layouts)
    }

    /**
     * Card order:
     *  Properties card, Message card, Changed files card, Code reviewers card, Comments Card
     */
    public void setup() {
        headerContents.add(mContext.getString(R.string.header_properties));
        headerContents.add(mContext.getString(R.string.header_commit_message));
        headerContents.add(mContext.getString(R.string.header_changed_files));
        headerContents.add(mContext.getString(R.string.header_reviewers));
        headerContents.add(mContext.getString(R.string.header_comments));

        RequestQueue rq = Volley.newRequestQueue(mContext);

        childGroupDetails.add(new ChildGroupDetails(Cards.PROPERTIES,
                        new PatchSetPropertiesCard(mContext, rq)));
        childGroupDetails.add(new ChildGroupDetails(Cards.COMMIT_MSG,
                new PatchSetMessageCard(mContext)));
        childGroupDetails.add(new ChildGroupDetails(Cards.CHANGED_FILES,
                new PatchSetChangesCard(mContext)));
        childGroupDetails.add(new ChildGroupDetails(Cards.REVIEWERS,
                new PatchSetReviewersCard(mContext, rq)));
        childGroupDetails.add(new ChildGroupDetails(Cards.COMMENTS,
                new PatchSetCommentsCard(mContext, rq)));
    }

    public void setCursor(Cards cardType, Cursor cursor) {
        childGroupDetails.get(cardType.ordinal()).setCursor(cursor);
    }

    private static class GroupViewHolder {
        TextView headerText;

        private GroupViewHolder(View view) {
            headerText = (TextView) view.findViewById(R.id.header);
        }
    }

    private class ChildGroupDetails {
        final Cards cardType;
        Cursor cursor;
        final CardBinder binder; // handles inflating the layout and binding data from the cursor

        private ChildGroupDetails(Cards cardType, CardBinder binder) {
            this.cardType = cardType;
            this.binder = binder;
        }

        public void setCursor(Cursor cursor) {
            this.cursor = cursor;
            notifyDataSetChanged();
        }
    }
}
