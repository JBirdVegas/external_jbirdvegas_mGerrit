package com.aokp.gerrit;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import com.aokp.gerrit.cards.CommitCard;
import com.aokp.gerrit.objects.JSONCommit;
import com.aokp.gerrit.tasks.GerritTask;
import com.fima.cardsui.objects.CardStack;
import com.fima.cardsui.views.CardUI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 4/1/13
 * Time: 2:07 AM
 */
public abstract class CardsActivity extends Activity {
    public static final String GERRIT = "http://gerrit.sudoservers.com/";
    public static final String GERRIT_BASE = GERRIT + "changes/?q=";
    protected String TAG = getClass().getSimpleName();
    public static String GERRIT_WEBADDRESS = GERRIT + "changes/?q=status:";

    // draws a stack of cards
    protected void drawCardsFromListToStack(List<CommitCard> cards, final CardUI cardUI) {
        CardStack cardStack = new CardStack();
        for (CommitCard card : cards) {
            cardStack.add(card);
        }
        cardUI.addStack(cardStack);
        // once we finish adding all the cards begin building the screen
        cardUI.refresh();
    }

    /**
     * TODO: remove
     * @param cardUI
     * @param card
     * @param json
     */
    @Deprecated
    private void redrawCard(CardUI cardUI, CommitCard card, String json) {
        try {
            card.update(new JSONCommit(new JSONObject(json)));
            cardUI.refresh();
        } catch (JSONException ignored) {
            // we failed to update move on
        }
    }

    // renders each card separately
    protected void drawCardsFromList(List<CommitCard> cards, CardUI cardUI) {
        for (int i = 1; cards.size() > i; i++) {
            cardUI.addCard(cards.get(i));
        }
        cardUI.refresh();
    }

    protected List<CommitCard> generateCardsList(String result) {
        List<CommitCard> commitCardList = new LinkedList<CommitCard>();
        try {
            JSONArray jsonArray = new JSONArray(result);
            int arraySize = jsonArray.length();
            for (int i = 0; arraySize > i; i++) {
                commitCardList.add(
                        new CommitCard(new JSONCommit(jsonArray.getJSONObject(i))));
            }
        } catch (JSONException e) {
            Log.d(TAG, "Failed to parse response from " + GERRIT_WEBADDRESS);
        }
        return commitCardList;
    }

    CardUI mCards;

    /**
     * This class handles the boilerplate code for those that inherit
     * @param savedInstanceState bundle containing state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.commit_list);
        Log.d(TAG, "Calling gerrit");
        mCards = (CardUI) findViewById(R.id.commit_cards);
        GerritTask gerritTask = new GerritTask() {
            @Override
            protected void onPostExecute(String s) {
                drawCardsFromList(generateCardsList(s), mCards);
            }
        };
        gerritTask.execute(GERRIT_WEBADDRESS, getQuery());
    }

    /**
     * Each tab provides its own query for ?p=status:[open:merged:abandoned]
     * @return
     */
    abstract String getQuery();
}