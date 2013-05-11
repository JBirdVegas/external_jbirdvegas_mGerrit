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
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.fima.cardsui.objects.Card;
import com.jbirdvegas.mgerrit.PatchSetViewerActivity;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.StaticWebAddress;
import com.jbirdvegas.mgerrit.objects.ChangedFile;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

import java.util.Arrays;
import java.util.List;

public class CommitCard extends Card {
    private JSONCommit mCommit;

    public CommitCard(JSONCommit commit) {
        this.mCommit = commit;
    }

    @Override
    public View getCardContent(final Context context) {
        int mGreen = context.getResources().getColor(R.color.text_green);
        int mRed = context.getResources().getColor(R.color.text_red);
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View commitCardView = inflater.inflate(R.layout.commit_card, null);
        ((TextView) commitCardView.findViewById(R.id.commit_card_commit_owner))
                .setText(mCommit.getOwner());
        ((TextView) commitCardView.findViewById(R.id.commit_card_project_name))
                .setText(mCommit.getProject());
        ((TextView) commitCardView.findViewById(R.id.commit_card_title))
                .setText(mCommit.getSubject());
        ((TextView) commitCardView.findViewById(R.id.commit_card_last_updated))
                .setText(mCommit.getLastUpdatedDate());
        ((TextView) commitCardView.findViewById(R.id.commit_card_commit_status))
                .setText(mCommit.getStatus().toString());
        if(mCommit.getStatus().toString() == "MERGED") {
            ((TextView) commitCardView.findViewById(R.id.commit_card_commit_status)).setTextColor(mGreen);
        } else if(mCommit.getStatus().toString() == "ABANDONED") {
            ((TextView) commitCardView.findViewById(R.id.commit_card_commit_status)).setTextColor(mRed);
        }
        TextView messageTv = (TextView)
                commitCardView.findViewById(R.id.commit_card_message);
        TextView changedFilesTv = (TextView)
                commitCardView.findViewById(R.id.commit_card_changed_files);
        Button browserView = (Button) commitCardView.findViewById(
                R.id.commit_card_view_in_browser);
        Button moarInfo = (Button) commitCardView.findViewById(
                R.id.commit_card_moar_info);
        moarInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, PatchSetViewerActivity.class);
                // example website
                // http://gerrit.aokp.co/changes/?q=7615&o=CURRENT_REVISION&o=CURRENT_COMMIT&o=CURRENT_FILES&o=DETAILED_LABELS
                intent.putExtra(JSONCommit.KEY_WEBSITE, new StringBuilder(0)
                        .append(StaticWebAddress.getChangesQuery())
                        .append(mCommit.getCommitNumber())
                        .append(JSONCommit.CURRENT_PATCHSET_ARGS).toString());
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(intent);
            }
        });
        browserView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(mCommit.getWebAddress()));
                context.startActivity(browserIntent);
            }
        });
        // we only have these if we direct query the commit specifically
        if (mCommit.getCurrentRevision() != null) {
            messageTv.setText(mCommit.getMessage());
            changedFilesTv.setText(
                    buildChangedFilesString(mCommit.getChangedFiles()));
        } else {
            messageTv.setVisibility(View.GONE);
            changedFilesTv.setVisibility(View.GONE);
        }
        return commitCardView;
    }

    public void update(JSONCommit commit) {
        this.mCommit = commit;
    }

    /**
     * returns the ChangedFile list as a string
     * @param fileList List of ChangedFiles
     * @return String representation of list
     */
    private String buildChangedFilesString(List<ChangedFile> fileList) {
        return Arrays.toString(fileList.toArray());
    }
}
