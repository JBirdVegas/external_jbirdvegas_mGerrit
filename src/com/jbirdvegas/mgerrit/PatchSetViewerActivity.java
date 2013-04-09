package com.jbirdvegas.mgerrit;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.jbirdvegas.mgerrit.cards.PatchSetChangesCard;
import com.jbirdvegas.mgerrit.cards.PatchSetMessageCard;
import com.jbirdvegas.mgerrit.cards.PatchSetPropertiesCard;
import com.jbirdvegas.mgerrit.cards.PatchSetReviewersCard;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.jbirdvegas.mgerrit.tasks.GerritTask;
import com.fima.cardsui.views.CardUI;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 4/2/13
 * Time: 5:30 PM
 *
 * Class handles populating the screen with several
 * cards each giving more information about the patchset
 *
 * All cards are located at jbirdvegas.mgerrit.cards.*
 */
public class PatchSetViewerActivity extends Activity {
    private static final String TAG = PatchSetViewerActivity.class.getSimpleName();
    private CardUI mCardsUI;
    private ActionBar mActionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.commit_list);
        String query = getIntent().getStringExtra(JSONCommit.KEY_WEBSITE);
        Log.d(TAG,"Website to query: " + query);
        mCardsUI = (CardUI) findViewById(R.id.commit_cards);
        mActionBar = getActionBar();
        mActionBar.setHomeButtonEnabled(true);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mCardsUI.setColumnNumber(2);
        }
        mCardsUI.manuallyInitData(getApplicationContext());
        executeGerritTask(query);
    }

    private void executeGerritTask(final String query) {
        new GerritTask() {
            @Override
            protected void onPostExecute(String s) {
                try {
                    addCards(mCardsUI,
                            new JSONCommit(
                                    new JSONArray(s).getJSONObject(0),
                                    getApplicationContext()));
                } catch (JSONException e) {
                    Log.d(TAG, "Response from "
                            + query + " could not be parsed into cards :(", e);
                }
            }
        }.execute(query);
    }

    private void addCards(CardUI ui, JSONCommit jsonCommit) {
        Log.d(TAG, "Loading Properties Card");
        ui.addCard(new PatchSetPropertiesCard(jsonCommit));
        Log.d(TAG, "Loading Message Card");
        ui.addCard(new PatchSetMessageCard(jsonCommit));
        Log.d(TAG, "Loading Changes Card");
        ui.addCard(new PatchSetChangesCard(jsonCommit));
        Log.d(TAG, "Loading Reviewers Card");
        ui.addCard(new PatchSetReviewersCard(jsonCommit));
        // TODO make card!
        //ui.addCard(new PatchSetCommentCard(jsonCommit));
        ui.refresh();
    }

    /*
    Possible cards

    --Patch Set--
    Select patchset number to display in these cards
    -------------

    --Times Card--
    Original upload time
    Most recent update
    --------------

    --Inline comments Card?--
    Show all comments inlined on code view pages
    **may be kind of pointless without context of sourounding code**
    * maybe a webview for each if possible? *
    -------------------------

     */

    // Handles correctly setting the ListViews height based on all the children
    // from http://nex-otaku-en.blogspot.com/2010/12/android-put-listview-in-scrollview.html
    public static void setListViewHeightBasedOnChildren(ListView... listViews) {
        for (ListView listView : listViews) {
            ListAdapter listAdapter = listView.getAdapter();
            if (listAdapter == null) {
                // pre-condition
                return;
            }
            ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height = getTotalHeight(listView, listAdapter)
                    + listView.getDividerHeight() * (listAdapter.getCount() - 1);
            listView.setLayoutParams(params);
            listView.requestLayout();
        }
    }

    static int getTotalHeight(ListView listView, ListAdapter listAdapter) {
        int totalHeight = 0;
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += listItem.getMeasuredHeight();
        }
        return totalHeight;
    }


    public static void setNotFoundListView(Context context, ListView listView) {
        listView.setAdapter(
                new ArrayAdapter<String>(context,
                        android.R.layout.simple_list_item_1,
                        android.R.id.text1,
                        new String[]{
                                context.getString(R.string.none_found),
                                context.getString(R.string.please_try_again)
                        }));
    }
}