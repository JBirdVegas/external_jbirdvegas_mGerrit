package com.jbirdvegas.mgerrit.cards;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.fima.cardsui.objects.Card;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.objects.CommitterObject;

public class ImageCard extends Card {

    private final String mTitle;
    private ImageView UserGravatar;
    private final CommitterObject mCommitterObject;

    public ImageCard(String title, CommitterObject committerObject){
        super(title);
        mTitle = title;
        mCommitterObject = committerObject;
    }

    @Override
    public View getCardContent(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_picture, null);
        ((TextView) view.findViewById(R.id.card_picture_title)).setText(title);
        UserGravatar = (ImageView) view.findViewById(R.id.card_picture_image);
        GravatarHelper.populateProfilePicture(UserGravatar, mCommitterObject.getEmail());
        ((TextView) view.findViewById(R.id.card_picture_user)).setText(mCommitterObject.getEmail());
        return view;
    }
}