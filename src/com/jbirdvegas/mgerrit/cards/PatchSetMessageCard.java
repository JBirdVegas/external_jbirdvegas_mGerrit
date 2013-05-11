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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.fima.cardsui.objects.Card;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

public class PatchSetMessageCard extends Card {
    private final JSONCommit mJSONCommit;
    private static final String TAG = PatchSetMessageCard.class.getSimpleName();

    public PatchSetMessageCard(JSONCommit commit) {
        this.mJSONCommit = commit;
    }
    @Override
    public View getCardContent(Context context) {
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootView = inflater.inflate(R.layout.patchset_message_card, null);
        /*
        --Message Card--
        Commit subject
        Last Update timestamp
        Commit message
        ----------------
         */
        ((TextView) rootView.findViewById(R.id.message_card_last_update))
                .setText(mJSONCommit.getLastUpdatedDate());
        ((TextView) rootView.findViewById(R.id.message_card_message))
                .setText(mJSONCommit.getMessage());
        return rootView;
    }
}