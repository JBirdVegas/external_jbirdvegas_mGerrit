package com.aokp.gerrit.cards;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import com.aokp.gerrit.PatchSetViewerActivity;
import com.aokp.gerrit.R;
import com.aokp.gerrit.adapters.PatchSetChangedFilesAdapter;
import com.aokp.gerrit.objects.JSONCommit;
import com.fima.cardsui.objects.Card;


/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 4/3/13
 * Time: 3:47 PM
 */
public class PatchSetChangesCard extends Card {
    private static final String TAG = PatchSetChangesCard.class.getSimpleName();
    private JSONCommit mCommit;

    public PatchSetChangesCard(JSONCommit commit) {
        mCommit = commit;
    }

    @Override
    public View getCardContent(final Context context) {
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootView = inflater.inflate(R.layout.listview_card, null);
        ListView listView = (ListView) rootView.findViewById(R.id.listView);
        // its possible for this to be null so watch out
        if (mCommit.getChangedFiles() == null) {
            // EEK! just show a simple not found message
            PatchSetViewerActivity.setNotFoundListView(context, listView);
        } else {
            try {
                listView.setAdapter(new PatchSetChangedFilesAdapter(context,
                        mCommit.getChangedFiles(),
                        mCommit));
                PatchSetViewerActivity.setListViewHeightBasedOnChildren(listView);
            } catch (NullPointerException npe) {
                Log.d(TAG, "Failed to set ListView Adapter", npe);
            }
        }
        return rootView;
    }
}
