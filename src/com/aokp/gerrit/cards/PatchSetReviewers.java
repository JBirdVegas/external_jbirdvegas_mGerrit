package com.aokp.gerrit.cards;

import android.content.Context;
import android.view.View;
import android.widget.ListView;
import com.aokp.gerrit.adapters.PatchSetLabelsAdapter;
import com.aokp.gerrit.objects.JSONCommit;
import com.aokp.gerrit.objects.Reviewer;
import com.fima.cardsui.objects.Card;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 4/3/13
 * Time: 3:50 PM
 */
public class PatchSetReviewers extends Card {
    private JSONCommit mJSONCommit;
    private ListView mReviewersList;

    public PatchSetReviewers(JSONCommit commit) {
        mJSONCommit = commit;
    }

    @Override
    public View getCardContent(Context context) {
        mReviewersList = new ListView(context);
        mReviewersList.setAdapter(new PatchSetLabelsAdapter(context,
                (ArrayList<Reviewer>) mJSONCommit.getCodeReviewers()));
        return null;
    }
}
