package com.jbirdvegas.mgerrit.cards;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.fima.cardsui.objects.Card;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.caches.BitmapLruCache;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.objects.CommitterObject;

public class ImageCard extends Card {

    private final String mTitle;
    private final RequestQueue mRequestQuery;
    private NetworkImageView mUserGravatar;
    private final CommitterObject mCommitterObject;

    public ImageCard(RequestQueue requestQueue,
                     String title,
                     CommitterObject committerObject) {
        super(title);
        mTitle = title;
        mCommitterObject = committerObject;
        mRequestQuery = requestQueue;
        // notify super we want to swipe
        // this card away
        setSwipableCard(true);
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
        return view;
    }
}