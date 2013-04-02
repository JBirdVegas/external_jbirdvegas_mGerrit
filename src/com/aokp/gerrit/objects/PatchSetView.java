package com.aokp.gerrit.objects;

import android.app.Activity;
import android.os.Bundle;
import com.aokp.gerrit.R;
import com.fima.cardsui.views.CardUI;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 4/2/13
 * Time: 5:30 PM
 */
public class PatchSetView extends Activity {
    private CardUI mCardsUI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.commit_list);
        mCardsUI = (CardUI) findViewById(R.id.commit_cards);

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
