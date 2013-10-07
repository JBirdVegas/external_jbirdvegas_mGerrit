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
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.fima.cardsui.objects.Card;
import com.fima.cardsui.objects.CardStack;
import com.fima.cardsui.views.CardUI;
import com.jbirdvegas.mgerrit.cards.CommitCard;
import com.jbirdvegas.mgerrit.cards.ImageCard;
import com.jbirdvegas.mgerrit.cards.ProjectCard;
import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.objects.ChangeLogRange;
import com.jbirdvegas.mgerrit.objects.CommitterObject;
import com.jbirdvegas.mgerrit.objects.GerritURL;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.jbirdvegas.mgerrit.tasks.GerritService;
import com.jbirdvegas.mgerrit.tasks.GerritTask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public abstract class CardsFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String KEY_STORED_CARDS = "storedCards";
    public static final String KEY_DEVELOPER = "committer_object";
    public static final String AT_SYMBOL = "@";
    public static final String KEY_OWNER = "owner";
    public static final String KEY_REVIEWER = "reviewer";

    // TODO: Could take this out and put it in GerritControllerActivity
    public static boolean mSkipStalking;
    private static final boolean DEBUG = true;
    private static final boolean CHATTY = false;
    protected String TAG = "CardsFragment";

    private GerritURL mUrl;

    private long mTimerStart;

    private RequestQueue mRequestQueue;

    public static boolean inProject;
    private ChangeLogRange mChangelogRange;
    private GerritControllerActivity mParent;
    private View mCurrentFragment;

    CardUI mCards;

    // Indicates that this fragment will need to be refreshed
    private boolean mIsDirty = false;


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
        for (Card card : cards) {
            cardUI.addCard(card);
        }

        // Check if the fragment is attached to an activity
        if (this.isAdded())
        {
            /*Toast.makeText(mParent,
                    String.format(getString(R.string.found_cards_toast,
                            count,
                            (System.currentTimeMillis() - mTimerStart) / 1000)),
                    Toast.LENGTH_LONG).show();*/
        }
        cardUI.setSwipeable(false);
        cardUI.refresh();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        init(savedInstanceState);
        setup();
    }

    protected List<CommitCard> generateCardsList(Cursor changes) {
        List<CommitCard> commitCardList = new LinkedList<CommitCard>();

        /*
         * TODO: Put this into JSONCommit:
         *     - takes a map (Cursor column name -> annotated field name
         *     - uses reflection to map the columns provided in the cursor to JSONCommit values
         */

        int changeid_index = changes.getColumnIndex(UserChanges.C_CHANGE_ID);
        int subject_index = changes.getColumnIndex(UserChanges.C_SUBJECT);
        int project_index = changes.getColumnIndex(UserChanges.C_PROJECT);
        int updated_index = changes.getColumnIndex(UserChanges.C_UPDATED);
        int status_index = changes.getColumnIndex(UserChanges.C_STATUS);
        int changenum_index = changes.getColumnIndex(UserChanges.C_COMMIT_NUMBER);

        // Committer object
        int username_index = changes.getColumnIndex(UserChanges.C_NAME);
        int useremail_index = changes.getColumnIndex(UserChanges.C_EMAIL);
        // TODO: This column is not present in the Users table yet
        int userid_index = changes.getColumnIndex(UserChanges.C_USER_ID);

        while (changes.moveToNext()) {
            CommitterObject committer = new CommitterObject(changes.getString(username_index),
                    changes.getString(useremail_index),
                    changes.getInt(userid_index));

            JSONCommit commit = new JSONCommit(mParent, changes.getString(changeid_index),
                    changes.getInt(changenum_index),
                    changes.getString(project_index),
                    changes.getString(subject_index),
                    committer,
                    changes.getString(updated_index),
                    changes.getString(status_index));

            CommitCard card = new CommitCard(commit, mParent.getCommitterObject(),
                    mRequestQueue, mParent);

            commitCardList.add(card);
        }
        // Don't close the cursor, the loader manager does that
        return commitCardList;
    }

    private CommitCard getCommitCard(JSONObject jsonObject, Context context) {
        return new CommitCard(
                new JSONCommit(jsonObject, context),
                mParent.getCommitterObject(),
                mRequestQueue,
                mParent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.commit_list, container, false);
    }

    // committerObject == mParent.getCommitterObject()
    private ImageCard stalkUser(final CommitterObject committerObject)
    {
        Toast.makeText(mParent,
                // format string with a space
                String.format("%s %s",
                        getString(R.string.stalker_mode_toast),
                        committerObject.getName()),
                Toast.LENGTH_LONG).show();

        // now add a project card if
        // we are looking at a single project
        ImageCard userImageCard = new ImageCard(mRequestQueue,
                mParent,
                this,
                committerObject.getName(),
                committerObject);
        userImageCard.setOnCardSwipedListener(new Card.OnCardSwiped() {
            @Override
            public void onCardSwiped(Card card, View layout) {
                mParent.clearCommitterObject();
                mSkipStalking = true;
                mParent.refreshTabs();
            }
        });
        return userImageCard;
    }

    private void init(Bundle savedInstanceState)
    {
        mParent = (GerritControllerActivity) this.getActivity();
        mCurrentFragment = this.getView();

        mTimerStart = System.currentTimeMillis();
        mCards = (CardUI) mCurrentFragment.findViewById(R.id.commit_cards);
        mCards.setSwipeable(true);
        mRequestQueue = Volley.newRequestQueue(mParent);
        // default to non author specific view

        mUrl = new GerritURL();

        // Need the account id of the owner here to maintain FK db constraint
        mUrl.setRequestDetailedAccounts(true);
        mUrl.setStatus(getQuery());
    }

    private void setup()
    {
        boolean followingUser = false;
        // track if we are in project
        String project = Prefs.getCurrentProject(mParent);
        inProject = (!"".equals(project));

        CommitterObject user = mParent.getCommitterObject();
        if (!mSkipStalking) {
            String userEmail = "";

            if (user == null) followingUser = false;
            else userEmail = user.getEmail();
            if (userEmail != null
                    && !userEmail.trim().isEmpty()
                    && userEmail.contains(AT_SYMBOL)) {

                mCards.addCard(stalkUser(user));
            }
        }

        if (inProject) mCards.addCard(getProjectCard());

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
            if (DEBUG) Log.w(TAG, "Not making changelog");
        }

        sendRequest(false);
        getLoaderManager().initLoader(0, null, this);
    }

    private void loadChangeLog(final ChangeLogRange logRange) {
        new GerritTask(mParent)
        {
            @Override
            public void onJSONResult(String s)
            {
                drawCardsFromList(
                        generateChangeLog(
                                logRange, s),
                        mCards);
            }
        }.execute(mUrl.toString());
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
            String url = mUrl.toString();
            Log.d(TAG, new StringBuilder(0)
                    .append(getString(R.string.failed_to_parse_json_response))
                    .append(' ')
                    .append(url)
                    .append('\n')
                    .append(result).toString(), e);
        }
        return commitCardList;
    }

    private Card getProjectCard() {
        return new ProjectCard(mParent, Prefs.getCurrentProject(mParent));
    }

    /**
     * Each tab provides its own query for ?p=status:[open:merged:abandoned]
     *
     * @return current tab name used for query { open, merged, abandoned }
     */
    abstract String getQuery();

    private void sendRequest(boolean forceUpdate) {
        Intent it = new Intent(mParent, GerritService.class);
        it.putExtra(GerritService.DATA_TYPE_KEY, GerritService.DataType.Commit);
        it.putExtra(GerritService.URL_KEY, mUrl);
        it.putExtra(GerritService.FORCE_UPDATE_KEY, forceUpdate);
        mParent.startService(it);
    }

    protected void refresh(boolean forceUpdate)
    {
        if (!mIsDirty) return;
        mCards.clearCards();

        if (inProject) mCards.addCard(getProjectCard());
        mIsDirty = false;
        getLoaderManager().restartLoader(0, null, this);

        if (forceUpdate) sendRequest(forceUpdate);
    }

    public void markDirty() { mIsDirty = true; }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String project = Prefs.getCurrentProject(mParent);
        if ("".equals(project)) project = null;

        CommitterObject user = mParent.getCommitterObject();

        return UserChanges.listCommits(mParent, getQuery(), user, project);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Naive implementation
        mCards.clearCards();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        drawCardsFromList(generateCardsList(cursor), mCards);
    }
}
