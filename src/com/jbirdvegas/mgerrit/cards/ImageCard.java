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
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.fima.cardsui.objects.Card;
import com.jbirdvegas.mgerrit.CardsFragment;
import com.jbirdvegas.mgerrit.GerritControllerActivity;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.caches.BitmapLruCache;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.objects.CommitterObject;

public class ImageCard extends Card {

    private GerritControllerActivity mGerritControllerActivity;
    private CardsFragment mCardsFragment;
    private final String mTitle;
    private final RequestQueue mRequestQuery;
    private NetworkImageView mUserGravatar;
    private final CommitterObject mCommitterObject;

    public ImageCard(RequestQueue requestQueue,
                     GerritControllerActivity gerritControllerActivity,
                     CardsFragment cardsFragment,
                     String title,
                     CommitterObject committerObject) {
        super(title);
        mGerritControllerActivity = gerritControllerActivity;
        mCardsFragment = cardsFragment;
        mTitle = title;
        mCommitterObject = committerObject;
        mRequestQuery = requestQueue;
    }

    @Override
    public View getCardContent(Context context) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.card_picture, null);
        ((TextView) view.findViewById(R.id.card_picture_title))
                .setText(title);
        mUserGravatar = (NetworkImageView)
                view.findViewById(R.id.card_picture_image);
        mUserGravatar.setImageUrl(
                GravatarHelper.getGravatarUrl(
                        mCommitterObject.getEmail()),
                new ImageLoader(mRequestQuery,
                        new BitmapLruCache(context)));
        ((TextView) view.findViewById(R.id.card_picture_user))
                .setText(mCommitterObject.getEmail());
        view.findViewById(R.id.remove_content).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCardsFragment.mSkipStalking = true;
                OnSwipeCard();
                mGerritControllerActivity.refreshTabs();
            }
        });
        return view;
    }
}