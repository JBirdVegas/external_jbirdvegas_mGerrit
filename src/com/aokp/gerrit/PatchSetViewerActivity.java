package com.aokp.gerrit;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import com.aokp.gerrit.cards.PatchSetChangesCard;
import com.aokp.gerrit.cards.PatchSetMessageCard;
import com.aokp.gerrit.cards.PatchSetPropertiesCard;
import com.aokp.gerrit.cards.PatchSetReviewers;
import com.aokp.gerrit.objects.JSONCommit;
import com.aokp.gerrit.tasks.GerritTask;
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
 * All cards are located at aokp.gerrit.cards.*
 */
public class PatchSetViewerActivity extends Activity {
    private static final String TAG = PatchSetViewerActivity.class.getSimpleName();

    private CardUI mCardsUI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.commit_list);
        String query = getIntent().getStringExtra(JSONCommit.KEY_WEBSITE);
        Log.d(TAG,"Website to query: " + query);
        mCardsUI = (CardUI) findViewById(R.id.commit_cards);
        new GerritTask() {
            @Override
            protected void onPostExecute(String s) {
                try {
                    Log.d(TAG, "Query response: " + s);
                    addCards(mCardsUI, new JSONCommit(new JSONArray(s).getJSONObject(0)));
                } catch (JSONException e) {
                    Log.d(TAG, "Failed to get patchset info from JSON", e);
                }
            }
        }.execute(query);
    }

    private void addCards(CardUI ui, JSONCommit jsonCommit) {
        ui.addCard(new PatchSetPropertiesCard(jsonCommit));
        ui.addCard(new PatchSetMessageCard(jsonCommit));
        ui.addCard(new PatchSetChangesCard(jsonCommit));
        ui.addCard(new PatchSetReviewers(jsonCommit));
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
}
