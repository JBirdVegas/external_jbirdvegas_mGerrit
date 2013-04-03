package com.aokp.gerrit.cards;

import android.content.Context;
import android.view.View;
import android.widget.ListView;
import com.aokp.gerrit.adapters.PatchSetLabelsAdapter;
import com.aokp.gerrit.objects.JSONCommit;
import com.aokp.gerrit.objects.Reviewer;
import com.fima.cardsui.objects.Card;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 4/3/13
 * Time: 3:50 PM
 */
public class PatchSetReviewers extends Card {
    private final String mCommit;
    private JSONCommit mJSONCommit;
    private ListView mReviewersList;

    public PatchSetReviewers(String json) {
        this.mCommit = json;
        try {
            mJSONCommit = new JSONCommit(new JSONObject(json));
        } catch (JSONException e) {
            throw new ExceptionInInitializerError("Failed to parse Reviewers labels!");
        }
    }

    @Override
    public View getCardContent(Context context) {
        mReviewersList = new ListView(context);
        mReviewersList.setAdapter(new PatchSetLabelsAdapter(context,
                (ArrayList<Reviewer>) mJSONCommit.getCodeReviewers()));
        return null;
    }
}
