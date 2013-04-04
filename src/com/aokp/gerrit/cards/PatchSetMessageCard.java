package com.aokp.gerrit.cards;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.aokp.gerrit.R;
import com.aokp.gerrit.objects.JSONCommit;
import com.fima.cardsui.objects.Card;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 4/3/13
 * Time: 3:46 PM
 */
public class PatchSetMessageCard extends Card {
    private final JSONCommit mJSONCommit;

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
        ((TextView) rootView.findViewById(R.id.message_card_subject))
                .setText(mJSONCommit.getSubject());
        ((TextView) rootView.findViewById(R.id.message_card_last_update))
                .setText(mJSONCommit.getLastUpdatedDate());
        ((TextView) rootView.findViewById(R.id.message_card_message))
                .setText(mJSONCommit.getMessage());
        return null;
    }
}