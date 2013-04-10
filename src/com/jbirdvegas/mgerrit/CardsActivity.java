package com.jbirdvegas.mgerrit;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
 *  Author: Jon Stanford (JBirdVegas), 2013
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import com.fima.cardsui.objects.CardStack;
import com.fima.cardsui.views.CardUI;
import com.jbirdvegas.mgerrit.cards.CommitCard;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.jbirdvegas.mgerrit.tasks.GerritTask;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.LinkedList;
import java.util.List;

public abstract class CardsActivity extends Activity {
    protected String TAG = getClass().getSimpleName();
    private String mWebsite;

    // draws a stack of cards
    // Currently not used as the number of cards tends
    // to be so large the stack is impractical to navigate
    protected void drawCardsFromListToStack(List<CommitCard> cards, final CardUI cardUI) {
        CardStack cardStack = new CardStack();
        for (CommitCard card : cards) {
            cardStack.add(card);
        }
        cardUI.addStack(cardStack);
        // once we finish adding all the cards begin building the screen
        cardUI.refresh();
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
                        new CommitCard(
                                new JSONCommit(jsonArray.getJSONObject(i),
                                        getApplicationContext())));
            }
        } catch (JSONException e) {
            Log.d(TAG, new StringBuilder(0)
                    .append("Failed to parse response from ")
                    .append(mWebsite).toString());
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
        mCards = (CardUI) findViewById(R.id.commit_cards);
        mWebsite = new StringBuilder(0)
                .append(Prefs.getCurrentGerrit(getApplicationContext()))
                .append(StaticWebAddress.getStatusQuery())
                .append(getQuery()).toString();
        loadScreen();
    }

    private void loadScreen() {
        Log.d(TAG, "Calling mgerrit: " + mWebsite);
        new GerritTask() {
            @Override
            protected void onPostExecute(String s) {
                drawCardsFromList(generateCardsList(s), mCards);
            }
        }.execute(mWebsite);
    }

    /**
     * Each tab provides its own query for ?p=status:[open:merged:abandoned]
     * @return
     */
    abstract String getQuery();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (getParent() != null) {
            return getParent().onCreateOptionsMenu(menu);
        }
        return super.onCreateOptionsMenu(menu);
    }
}