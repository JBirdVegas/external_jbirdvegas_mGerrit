package com.aokp.gerrit.objects;

import android.content.Context;
import android.view.View;
import com.fima.cardsui.objects.Card;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 3/31/13
 * Time: 4:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class CommitCard {
    public static Card generateCommitCard(Context context, JSONCommit commit) {
        // TODO!!!
        return new Card() {
            @Override
            public View getCardContent(Context context) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        };
    }
}
