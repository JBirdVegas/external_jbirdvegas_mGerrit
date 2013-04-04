package com.aokp.gerrit;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import com.aokp.gerrit.cards.PatchSetChangesCard;
import com.aokp.gerrit.cards.PatchSetMessageCard;
import com.aokp.gerrit.cards.PatchSetPropertiesCard;
import com.aokp.gerrit.cards.PatchSetReviewers;
import com.aokp.gerrit.objects.JSONCommit;
import com.fima.cardsui.views.CardUI;
import org.json.JSONException;
import org.json.JSONObject;

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
    private JSONObject mChangeInfo;
    private JSONCommit mPatchsetInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.commit_list);
        mCardsUI = (CardUI) findViewById(R.id.commit_cards);
        try {
            // TODO any need for the abbreviated commit information?
            mChangeInfo = new JSONObject(savedInstanceState.getString(JSONCommit.KEY_JSON_COMMIT));
            mPatchsetInfo = new JSONCommit(new JSONObject(savedInstanceState.getString(JSONCommit.KEY_PATCHSET_IN_JSON)));
            addCards(mPatchsetInfo);
        } catch (JSONException e) {
            // should never happen
            Log.wtf(TAG, "failed to parse PatchSet details", e);
        }
    }

    private void addCards(JSONCommit jsonCommit) {
        mCardsUI.addCard(new PatchSetPropertiesCard(jsonCommit));
        mCardsUI.addCard(new PatchSetMessageCard(jsonCommit));
        mCardsUI.addCard(new PatchSetChangesCard(jsonCommit));
        mCardsUI.addCard(new PatchSetReviewers(jsonCommit));
        // TODO make card!
        //mCardsUI.addCard(new PatchSetCommentCard(jsonCommit));
    }

    /* TODO
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
