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

import com.jbirdvegas.mgerrit.R;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.fima.cardsui.objects.Card;
import com.jbirdvegas.mgerrit.helpers.MD5Helper;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

public class PatchSetPropertiesCard extends Card {
    private static final String GRAVATAR_API = "http://www.gravatar.com/avatar/";
    private static final String DEFAULT_AVATAR_SIZE = "80";
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
                    .setText(mJSONCommit.getAuthor().getName());
            ((TextView) rootView.findViewById(R.id.prop_card_committer))
                    .setText(mJSONCommit.getCommitter().getName());
        } catch (NullPointerException npe) {
            rootView.findViewById(R.id.prop_card_author)
                    .setVisibility(View.GONE);
            rootView.findViewById(R.id.prop_card_committer)
                    .setVisibility(View.GONE);
        }

        // use emails to get gravatar profile images
        populateProfilePicture((ImageView) rootView.findViewById(R.id.prop_card_owner_gravatar),
                mJSONCommit.getOwnerObject().getEmail());

        populateProfilePicture((ImageView) rootView.findViewById(R.id.prop_card_author_gravatar),
                mJSONCommit.getAuthor().getEmail());

        populateProfilePicture((ImageView) rootView.findViewById(R.id.prop_card_committer_gravatar),
                mJSONCommit.getCommitter().getEmail());
        return rootView;
    }

    private void populateProfilePicture(ImageView imageView, String email) {
        String emailMd5 = MD5Helper.md5Hex(email.trim().toLowerCase());
        if (emailMd5 != null) {
            String url = GRAVATAR_API + emailMd5 + "?s=" + DEFAULT_AVATAR_SIZE;
            Log.d(this.getClass().getSimpleName(), "Gravatar url called: " + url);
            UrlImageViewHelper.setUrlDrawable(imageView,
                    url,
                    R.drawable.ic_action_clock,
                    UrlImageViewHelper.CACHE_DURATION_THREE_DAYS);
        } else {
            imageView.setVisibility(View.GONE);
        }
    }
}