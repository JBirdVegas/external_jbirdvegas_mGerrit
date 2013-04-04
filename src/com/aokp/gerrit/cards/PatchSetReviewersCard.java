package com.aokp.gerrit.cards;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import com.aokp.gerrit.PatchSetViewerActivity;
import com.aokp.gerrit.R;
import com.aokp.gerrit.adapters.PatchSetReviewersAdapter;
import com.aokp.gerrit.objects.JSONCommit;
import com.fima.cardsui.objects.Card;

import static com.aokp.gerrit.PatchSetViewerActivity.setListViewHeightBasedOnChildren;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 4/3/13
 * Time: 3:50 PM
 */
public class PatchSetReviewersCard extends Card {
    private static final String TAG = PatchSetReviewersAdapter.class.getSimpleName();
    private JSONCommit mJSONCommit;
    private ListView mReviewersList;
    private ListView mVerifiedList;

    public PatchSetReviewersCard(JSONCommit commit) {
        mJSONCommit = commit;
    }

    @Override
    public View getCardContent(Context context) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.patchset_labels_card, null);
        mReviewersList = (ListView) view.findViewById(R.id.patchset_labels_code_reviewers);
        mVerifiedList = (ListView) view.findViewById(R.id.patchset_labels_verified_reviewers);
        // don't try to set from null values
        if (mJSONCommit.getCodeReviewers() == null) {
            PatchSetViewerActivity.setNotFoundListView(context, mReviewersList);
        } else {
            mReviewersList.setAdapter(new PatchSetReviewersAdapter(context,
                    mJSONCommit.getCodeReviewers()));
        }
        // don't try to set from null values
        if (mJSONCommit.getVerifiedReviewers() == null) {
            PatchSetViewerActivity.setNotFoundListView(context, mVerifiedList);
        } else {
            mVerifiedList.setAdapter(new PatchSetReviewersAdapter(context,
                    mJSONCommit.getVerifiedReviewers()));
        }
        // static import of PatchSetViewerActivity.setListViewHeightBasedOnChildren(ListView)
        // handles setting ListView height correctly
        setListViewHeightBasedOnChildren(mReviewersList, mVerifiedList);
        return view;
    }

}
