package com.aokp.gerrit.objects;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.aokp.gerrit.R;
import com.fima.cardsui.objects.Card;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 3/31/13
 * Time: 4:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class CommitCard {
    public static Card generateCommitCard(final JSONCommit commit) {
        return new Card() {
            @Override
            public View getCardContent(Context context) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View commitCardView = inflater.inflate(R.layout.commit_card, null);
                ((TextView) commitCardView.findViewById(R.id.commit_card_commit_owner)).setText(commit.getOwner());
                ((TextView) commitCardView.findViewById(R.id.commit_card_project_name)).setText(commit.getProject());
                ((TextView) commitCardView.findViewById(R.id.commit_card_title)).setText(commit.getSubject());
                return commitCardView;
            }
        };
    }
}
