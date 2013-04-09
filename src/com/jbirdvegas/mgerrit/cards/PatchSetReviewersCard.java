package com.jbirdvegas.mgerrit.cards;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import com.jbirdvegas.mgerrit.PatchSetViewerActivity;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.adapters.PatchSetReviewersAdapter;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.fima.cardsui.objects.Card;

import static com.jbirdvegas.mgerrit.PatchSetViewerActivity.setListViewHeightBasedOnChildren;

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
            // *hopefully* the only reason this would be null is if user is
            // viewing ABANDONED tab so just remove the card
            view.setVisibility(View.GONE);
            return view;
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
