package com.jbirdvegas.mgerrit.cards;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.fima.cardsui.objects.Card;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 4/3/13
 * Time: 3:46 PM
 */
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
        Log.d(TAG, "JSONCommit.toString()::" + mJSONCommit.toString());
        ((TextView) rootView.findViewById(R.id.message_card_subject))
                .setText(mJSONCommit.getSubject());
        ((TextView) rootView.findViewById(R.id.message_card_last_update))
                .setText(mJSONCommit.getLastUpdatedDate());
        ((TextView) rootView.findViewById(R.id.message_card_message))
                .setText(mJSONCommit.getMessage());
        return rootView;
    }
}