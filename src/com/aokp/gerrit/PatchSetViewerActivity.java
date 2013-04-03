package com.aokp.gerrit;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
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
            mChangeInfo = new JSONObject(savedInstanceState.getString(JSONCommit.KEY_JSON_COMMIT));
            mPatchsetInfo = new JSONCommit(new JSONObject(savedInstanceState.getString(JSONCommit.KEY_PATCHSET_IN_JSON)));
        } catch (JSONException e) {
            // should never happen
            Log.wtf(TAG, "failed to parse PatchSet details", e);
        }
    }

    private void addCards() {

    }

    // Generate cards
    /*
    Possible cards


    --Properties Card--
    Subject
    Owner
    Author
    Committer
    ------------------

    --Message Card--
    Commit subject
    Last Update timestamp
    Commit message
    ----------------

    --Changes Card--
    Files Changed (ListView?)
    File Diff?
    ----------------

    --Patch Set--
    Select patchset number to display in these cards
    -------------

    --Times Card--
    Original upload time
    Most recent update
    --------------

    --Comments Card--
    ListView with Commentor name, Timestamp and comment
    -----------------

    --Reviewers--
    ListView or TableView with all reviews and their +1;+2;-1;-2s
    -------------

    --Inline comments Card?--
    Show all comments inlined on code view pages
    **may be kind of pointless without context of sourounding code**
    * maybe a webview for each if possible? *
    -------------------------

     */
}
