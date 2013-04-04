package com.aokp.gerrit.cards;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import com.aokp.gerrit.R;
import com.aokp.gerrit.adapters.PatchSetLabelsAdapter;
import com.aokp.gerrit.objects.JSONCommit;
import com.fima.cardsui.objects.Card;

import java.util.Arrays;

import static com.aokp.gerrit.PatchSetViewerActivity.setListViewHeightBasedOnChildren;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 4/3/13
 * Time: 3:50 PM
 */
public class PatchSetReviewersCard extends Card {
    private static final String TAG = PatchSetLabelsAdapter.class.getSimpleName();
    private JSONCommit mJSONCommit;
    private ListView mReviewersList;

    public PatchSetReviewersCard(JSONCommit commit) {
        mJSONCommit = commit;
    }

    //TODO FIX ME!!!
    @Override
    public View getCardContent(Context context) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.patchset_labels_card, null);
        /*
        for (int i = 0; mJSONCommit.getCodeReviewers().size() > i; i++) {
            View v = layoutInflater.inflate(R.layout.patchset_labels_list_item, null);
            ((TextView) v.findViewById(R.id.labels_card_approval)).setText(mJSONCommit.getCodeReviewers().get(i).getValue());
            ((TextView) v.findViewById(R.id.labels_card_reviewer_name)).setText(mJSONCommit.getCodeReviewers().get(i).getName());

        }
        */
        mReviewersList = (ListView) view.findViewById(R.id.patchset_labels_listview);
        mReviewersList.setAdapter(new PatchSetLabelsAdapter(context,
                mJSONCommit.getCodeReviewers()));
        // static import of PatchSetViewerActivity.setListViewHeightBasedOnChildren(ListView)
        // handles setting listview height correctly
        setListViewHeightBasedOnChildren(mReviewersList);
        Log.d(TAG, "all code reviewers::" + Arrays.toString(mJSONCommit.getCodeReviewers().toArray()));
        return view;
    }
}
