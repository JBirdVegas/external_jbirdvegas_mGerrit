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
import android.widget.ImageView;
import android.widget.TextView;
import com.fima.cardsui.objects.Card;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

public class PatchSetPropertiesCard extends Card {
    private final JSONCommit mJSONCommit;

    public PatchSetPropertiesCard(JSONCommit commit) {
        this.mJSONCommit = commit;
    }
    @Override
    public View getCardContent(Context context) {
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootView = inflater.inflate(R.layout.properties_card, null);
        ((TextView) rootView.findViewById(R.id.prop_card_subject))
                .setText(mJSONCommit.getSubject());
        ((TextView) rootView.findViewById(R.id.prop_card_owner))
                .setText(mJSONCommit.getOwnerName());
        try {
            ((TextView) rootView.findViewById(R.id.prop_card_author))
                    .setText(mJSONCommit.getAuthorObject().getName());
            ((TextView) rootView.findViewById(R.id.prop_card_committer))
                    .setText(mJSONCommit.getCommitterObject().getName());
        } catch (NullPointerException npe) {
            rootView.findViewById(R.id.prop_card_author)
                    .setVisibility(View.GONE);
            rootView.findViewById(R.id.prop_card_committer)
                    .setVisibility(View.GONE);
        }

        // use emails to get gravatar profile images
        GravatarHelper.populateProfilePicture(
                (ImageView) rootView.findViewById(R.id.prop_card_owner_gravatar),
                mJSONCommit.getOwnerObject().getEmail());
        ImageView authorAvatar = (ImageView) rootView.findViewById(R.id.prop_card_author_gravatar);
        try {

            GravatarHelper.populateProfilePicture(
                    authorAvatar,
                    mJSONCommit.getAuthorObject().getEmail());
        } catch (NullPointerException npe) {
            // failed to get author email removing avatar
            authorAvatar.setVisibility(View.GONE);
        }

        ImageView committerGravatar = (ImageView) rootView.findViewById(R.id.prop_card_committer_gravatar);
        try {
            GravatarHelper.populateProfilePicture(
                    committerGravatar,
                    mJSONCommit.getCommitterObject().getEmail());
        } catch (NullPointerException npe) {
            committerGravatar.setVisibility(View.GONE);
        }
        return rootView;
    }
}