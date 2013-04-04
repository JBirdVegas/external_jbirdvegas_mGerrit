package com.aokp.gerrit.cards;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import com.aokp.gerrit.R;
import com.aokp.gerrit.adapters.PatchSetLabelsAdapter;
import com.aokp.gerrit.objects.JSONCommit;
import com.fima.cardsui.objects.Card;

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
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootView = inflater.inflate(R.layout.listview_card, null);
        mReviewersList = (ListView) rootView.findViewById(R.id.listView);
        mReviewersList.setAdapter(new PatchSetLabelsAdapter(context,
                mJSONCommit.getCodeReviewers()));
        return mCardLayout;
    }
}
