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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import com.fima.cardsui.objects.CardStack;
import com.fima.cardsui.views.CardUI;
import com.jbirdvegas.mgerrit.cards.CommitCard;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.jbirdvegas.mgerrit.tasks.GerritTask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

public abstract class CardsActivity extends Activity {
    private static final String KEY_STORED_CARDS = "storedCards";
    public static final String KEY_DEVELOPER = "dev_email";
    private static final String AT_SYMBOL = "@";
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
        for (int i = 0; cards.size() > i; i++) {
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
                commitCardList.add(getCommitCard(jsonArray.getJSONObject(i),
                        getApplicationContext()));
            }
        } catch (JSONException e) {
            Log.d(TAG, new StringBuilder(0)
                    .append(getString(R.string.failed_to_parse_json_response))
                    .append(' ')
                    .append(mWebsite)
                    .append('\n')
                    .append(result).toString(), e);
            showErrorDialog(e, false);
        }
        return commitCardList;
    }

    private CommitCard getCommitCard(JSONObject jsonObject, Context context) {
        return new CommitCard(new JSONCommit(jsonObject, context));
    }

    CardUI mCards;

    /**
     * This class handles the boilerplate code for those that inherit
     *
     * @param savedInstanceState bundle containing state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.commit_list);
        mCards = (CardUI) findViewById(R.id.commit_cards);
        String userEmail = getIntent().getStringExtra(KEY_DEVELOPER);
        mWebsite = new StringBuilder(0)
                .append(Prefs.getCurrentGerrit(getApplicationContext()))
                .append(StaticWebAddress.getStatusQuery())
                .append(getQuery())
                .append(JSONCommit.DETAILED_ACCOUNTS_ARG).toString();
        if (userEmail != null
                && !userEmail.trim().isEmpty()
                && userEmail.contains(AT_SYMBOL)) {
            mWebsite = new StringBuilder(0)
                .append(Prefs.getCurrentGerrit(getApplicationContext()))
                    .append(StaticWebAddress.getQuery())
                    .append("owner:")
                    .append(userEmail)
                    .toString();

        }
        if (savedInstanceState == null) {
            saveCards("");
        }
        loadScreen();
    }

    private void loadScreen() {
        Log.d(TAG, "Calling mgerrit: " + mWebsite);
        if (getStoredCards().equals(""))
            new GerritTask(this) {
                @Override
                public void onJSONResult(String s) {
                    saveCards(s);
                    drawCardsFromList(generateCardsList(s), mCards);
                }
            }.execute(mWebsite);
        else
            drawCardsFromList(generateCardsList(getStoredCards()), mCards);
    }

    /**
     * Each tab provides its own query for ?p=status:[open:merged:abandoned]
     *
     * @return current tab name used for query { open, merged, abandoned }
     */
    abstract String getQuery();

    private void showErrorDialog(final Exception exception, boolean showException) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                this);
        builder.setCancelable(true)
                .setTitle(R.string.failed_to_parse_json_response)
                .setInverseBackgroundForced(true)
                .setPositiveButton(R.string.exit,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dialog.dismiss();
                                gameOver();
                            }
                        });
        if (showException) {
            builder.setMessage(stackTraceToString(exception));
        } else {
            builder.setMessage(R.string.gerrit_call_failed)
                    .setNegativeButton(R.string.show_exception,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    showErrorDialog(exception, true);
                                }
                            });
        }
        builder.create().show();
    }

    private void gameOver() {
        finish();
    }

    public static String stackTraceToString(Throwable e) {
        StringBuilder sb = new StringBuilder(0);
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString());
            sb.append('\n');
        }
        return sb.toString();
    }

    private void saveCards(String jsonCards) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString(KEY_STORED_CARDS, jsonCards).commit();
    }

    private String getStoredCards() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(KEY_STORED_CARDS, "");
    }
}