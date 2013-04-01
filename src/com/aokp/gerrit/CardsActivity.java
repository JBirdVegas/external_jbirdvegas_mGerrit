package com.aokp.gerrit;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import com.aokp.gerrit.objects.CommitCard;
import com.aokp.gerrit.objects.JSONCommit;
import com.aokp.gerrit.tasks.GerritTask;
import com.fima.cardsui.objects.Card;
import com.fima.cardsui.objects.CardStack;
import com.fima.cardsui.views.CardUI;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 4/1/13
 * Time: 2:07 AM
 */
public abstract class CardsActivity extends Activity {
    protected String TAG = getClass().getSimpleName();
    public static String GERRIT_WEBADDRESS = "http://gerrit.sudoservers.com/changes/?q=status:";

    protected void drawCardsFromList(List<Card> cards, CardUI cardUI) {
        CardStack cardStack = new CardStack();
        for (Card card : cards) {
            cardStack.add(card);
        }
        cardUI.addStack(cardStack);
        // once we finish adding all the cards begin building the screen
        cardUI.refresh();
    }

    protected List<Card> generateCardsList(String result) {
        List<Card> commitCardList = new LinkedList<Card>();
        try {
            JSONArray jsonArray = new JSONArray(result);
            int arraySize = jsonArray.length();
            for (int i = 0; arraySize > i; i++) {
                commitCardList.add(
                        CommitCard.generateCommitCard(
                                new JSONCommit(jsonArray.getJSONObject(i))));
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
        gerritTask.execute(getQuery());
    }

    abstract String getQuery();
}
