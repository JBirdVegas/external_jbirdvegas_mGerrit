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

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.fima.cardsui.objects.Card;
import com.fima.cardsui.objects.CardStack;
import com.fima.cardsui.views.CardUI;
import com.jbirdvegas.mgerrit.cards.CommitCard;
import com.jbirdvegas.mgerrit.cards.ImageCard;
import com.jbirdvegas.mgerrit.objects.ChangeLogRange;
import com.jbirdvegas.mgerrit.objects.CommitterObject;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.jbirdvegas.mgerrit.tasks.GerritTask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public abstract class CardsFragment extends Fragment {
    private static final String KEY_STORED_CARDS = "storedCards";
    public static final String KEY_DEVELOPER = "committer_object";
    public static final String AT_SYMBOL = "@";
    public static final String KEY_OWNER = "owner";
    public static final String KEY_REVIEWER = "reviewer";

    // TODO: Could take this out and put it in GerritControllerActivity
    public static boolean mSkipStalking;
    private static final boolean DEBUG = true;
    private static final boolean CHATTY = false;
    protected String TAG = CardsFragment.class.getSimpleName();
    private String mUrlParams;
    private long mTimerStart;

    private RequestQueue mRequestQueue;
    public static boolean inProject;
    private ChangeLogRange mChangelogRange;
    private GerritControllerActivity mParent;
    private View mCurrentFragment;

    // draws a stack of cards
    // Currently not used as the number of cards tends
    // to be so large the stack is impractical to navigate
    protected void drawCardsFromListToStack(List<CommitCard> cards, CardUI cardUI) {
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
        int count = 0;
        for (int i = 0; cards.size() > i; i++) {
            cardUI.addCard(cards.get(i));
            count++;
        }

        Toast.makeText(mParent,
                String.format(getString(R.string.found_cards_toast,
                        count,
                        (System.currentTimeMillis() - mTimerStart) / 1000)),
                Toast.LENGTH_LONG).show();
        cardUI.refresh();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        mParent = (GerritControllerActivity) this.getActivity();
        mCurrentFragment = this.getView();

        mTimerStart = System.currentTimeMillis();
        mCards = (CardUI) mCurrentFragment.findViewById(R.id.commit_cards);
        mCards.setSwipeable(true);
        mRequestQueue = Volley.newRequestQueue(mParent);
        // default to non author specific view
        mUrlParams = new StringBuilder(0)
                .append(StaticWebAddress.getStatusQuery())
                .append(getQuery())
                .append(JSONCommit.DETAILED_ACCOUNTS_ARG).toString();

        if (savedInstanceState == null) saveCards("");
        setup();
    }

    protected List<CommitCard> generateCardsList(String result) {
        List<CommitCard> commitCardList = new LinkedList<CommitCard>();
        try {
            JSONArray jsonArray = new JSONArray(result);
            int arraySize = jsonArray.length();
            for (int i = 0; arraySize > i; i++) {
                commitCardList.add(getCommitCard(jsonArray.getJSONObject(i),
                        mParent.getApplicationContext()));
            }
        } catch (JSONException e) {
            String url = mParent.getGerritWebsite() + mUrlParams;
            Log.d(TAG, new StringBuilder(0)
                    .append(getString(R.string.failed_to_parse_json_response))
                    .append(' ')
                    .append(url)
                    .append('\n')
                    .append(result).toString(), e);
        }
        return commitCardList;
    }

    private CommitCard getCommitCard(JSONObject jsonObject, Context context) {
        return new CommitCard(
                new JSONCommit(jsonObject, context),
                mParent,
                mParent.getCommitterObject(),
                mRequestQueue,
                this);
    }

    CardUI mCards;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.commit_list, container, false);
    }

    private void setProject(String project, boolean followingUser, CommitterObject committer)
    {
        StringBuilder builder = new StringBuilder(0)
                .append("changes/?q=(")
                .append("status:")
                .append(getQuery())
                .append('+')
                .append("project:")
                .append(URLEncoder.encode(project));

        if (followingUser && !mSkipStalking) {
            builder.append("+owner:").append(committer.getEmail());
        }
        builder.append(')')
                .append(JSONCommit.DETAILED_ACCOUNTS_ARG);
        mUrlParams = builder.toString();
    }

    private ImageCard stalkUser(String project, final CommitterObject committerObject)
    {
        String userEmail = committerObject.getEmail();

        // http://gerrit.aokp.co/changes/?q=(owner:android@championswimmer.tk+status:open)&o=DETAILED_ACCOUNTS
        StringBuilder builder = new StringBuilder(0)
                .append("changes/?q=(")
                .append(committerObject.getState())
                .append(':')
                .append(userEmail)
                .append('+')
                .append("status:")
                .append(getQuery());

        if (project != null && !project.trim().isEmpty()) {
            try {
                builder.append('+')
                        .append("project:")
                        .append(URLEncoder.encode(project, "UTF-8"));
            } catch (UnsupportedEncodingException uee) {
                // Should not occur
            }
            inProject = true;
        }

        mUrlParams = builder.append(')')
                .append(JSONCommit.DETAILED_ACCOUNTS_ARG)
                .toString();
        Toast.makeText(mParent.getApplicationContext(),
                // format string with a space
                String.format("%s %s",
                        getString(R.string.stalker_mode_toast),
                        committerObject.getName()),
                Toast.LENGTH_LONG).show();

        // now add a project card if
        // we are looking at a single project
        ImageCard userImageCard = new ImageCard(mRequestQueue, committerObject.getName(),
                committerObject);
        userImageCard.setOnCardSwipedListener(new Card.OnCardSwiped() {
            @Override
            public void onCardSwiped(Card card, View layout) {
                mParent.clearCommitterObject();
                mSkipStalking = true;
                refresh();
            }
        });
        return userImageCard;
    }

    private void setup()
    {
        // track if we are in project
        String project = mParent.getProject();
        boolean followingUser = false;

        inProject = false;
        if (project != null && !project.trim().isEmpty()) inProject = true;

        CommitterObject user = mParent.getCommitterObject();
        if (!mSkipStalking) {
            String userEmail = "";

            if (user == null) followingUser = false;
            else userEmail = user.getEmail();
            if (userEmail != null
                    && !userEmail.trim().isEmpty()
                    && userEmail.contains(AT_SYMBOL)) {

                mCards.addCard(stalkUser(project, user));
            }
        }

        // handle if project was clicked
        if (inProject)
        {
            setProject(project, followingUser, user);
            mCards.addCard(getProjectCard(project), true);
        }

        try {
            mChangelogRange = mParent.getIntent()
                    .getExtras()
                    .getParcelable(AOKPChangelog.KEY_CHANGELOG);
            if (mChangelogRange != null) {
                loadChangeLog(mChangelogRange);
                return;
            }
        } catch (NullPointerException npe) {
            // not making a changelog
            if (DEBUG) Log.e(TAG, "Not making changelog");
        }

        loadScreen();
    }

    private void loadChangeLog(final ChangeLogRange logRange) {
        String url = mParent.getGerritWebsite() + mUrlParams;
        new GerritTask(mParent) {
            @Override
            public void onJSONResult(String s) {
                saveCards(s);
                drawCardsFromList(
                        generateChangeLog(
                                logRange, s),
                        mCards);
            }
        }.execute(url);
    }

    private List<CommitCard> generateChangeLog(ChangeLogRange logRange,
                                               String result) {
        List<CommitCard> commitCardList = new LinkedList<CommitCard>();
        try {
            Log.d(TAG, "makeing changelog from ChangeLogRange: "+ logRange);
            JSONArray jsonArray = new JSONArray(result);
            int arraySize = jsonArray.length();
            CommitCard commitCard = null;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            for (int i = 0; arraySize > i; i++) {
                commitCard = getCommitCard(jsonArray.getJSONObject(i),
                        mParent.getApplicationContext());
                try {
                    String formattedDate = commitCard
                            .getJsonCommit().getLastUpdatedDate(mParent.getApplicationContext());
                    String subStringDate = formattedDate
                            .substring(0, formattedDate.length() - 10);
                    Date commitDate  = sdf.parse(subStringDate);
                    if (CHATTY) {
                        Log.d(TAG, String.format("min: %s max: %s finding: %s",
                                mChangelogRange.startTime(), mChangelogRange.endTime(), commitDate));
                    }
                    if (mChangelogRange.isInRange(commitDate.getTime())) {
                        commitCard.setChangeLogRange(mChangelogRange);
                        commitCardList.add(commitCard);
                        if (CHATTY) {
                            Log.d(TAG, "Commit included in changelog! "
                                    + commitCard.getJsonCommit().getSubject());
                        }
                    } else {
                        if (CHATTY) {
                            Log.d(TAG, "Commit Excluded from changelog! "
                                    + commitCard.getJsonCommit().getSubject());
                        }
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            String url = mParent.getGerritWebsite() + mUrlParams;
            Log.d(TAG, new StringBuilder(0)
                    .append(getString(R.string.failed_to_parse_json_response))
                    .append(' ')
                    .append(url)
                    .append('\n')
                    .append(result).toString(), e);
        }
        return commitCardList;
    }

    private Card getProjectCard(final String project) {
        Card card = new Card(project) {
            @Override
            public View getCardContent(Context context) {
                TextView projectView = new TextView(context);
                projectView.setText(project);
                projectView.setPadding(5, 0, 0, 10);
                projectView.setTextAppearance(context, R.style.CardTitle);
                return projectView;
            }
        };
        card.setOnCardSwipedListener(new Card.OnCardSwiped() {
            @Override
            public void onCardSwiped(Card card, View layout) {
                mParent.clearProject();
                onCreate(null);
            }
        });
        card.setSwipableCard(true);
        return card;
    }

    private void loadScreen() {
        mTimerStart = System.currentTimeMillis();
        String url = mParent.getGerritWebsite() + mUrlParams;
        Log.d(TAG, "Calling mgerrit: " + url);
        if (getStoredCards().equals(""))
            new GerritTask(mParent) {
                @Override
                public void onJSONResult(String s) {
                    saveCards(s);
                    drawCardsFromList(generateCardsList(s), mCards);
                }
            }.execute(url);
        else
            drawCardsFromList(generateCardsList(getStoredCards()), mCards);
    }

    public void refresh()
    {
        mCards.clearCards();
        setup();
    }

    /**
     * Each tab provides its own query for ?p=status:[open:merged:abandoned]
     *
     * @return current tab name used for query { open, merged, abandoned }
     */
    abstract String getQuery();

    private void saveCards(String jsonCards) {
        PreferenceManager.getDefaultSharedPreferences(mParent)
                .edit()
                .putString(KEY_STORED_CARDS, jsonCards)
                .commit();
    }

    private String getStoredCards() {
        return PreferenceManager.getDefaultSharedPreferences(mParent)
                .getString(KEY_STORED_CARDS, "");
    }

    public void setInProject(boolean inProject) {
        this.inProject = inProject;
    }
}
