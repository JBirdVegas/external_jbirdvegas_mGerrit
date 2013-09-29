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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.fima.cardsui.views.CardUI;
import com.jbirdvegas.mgerrit.cards.PatchSetChangesCard;
import com.jbirdvegas.mgerrit.cards.PatchSetCommentsCard;
import com.jbirdvegas.mgerrit.cards.PatchSetMessageCard;
import com.jbirdvegas.mgerrit.cards.PatchSetPropertiesCard;
import com.jbirdvegas.mgerrit.cards.PatchSetReviewersCard;
import com.jbirdvegas.mgerrit.database.SelectedChange;
import com.jbirdvegas.mgerrit.objects.CommitterObject;
import com.jbirdvegas.mgerrit.objects.GerritURL;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.jbirdvegas.mgerrit.tasks.GerritTask;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Class handles populating the screen with several
 * cards each giving more information about the patchset
 * <p/>
 * All cards are located at jbirdvegas.mgerrit.cards.*
 */
public class PatchSetViewerFragment extends Fragment {
    private static final String TAG = PatchSetViewerFragment.class.getSimpleName();
    private static final String KEY_STORED_PATCHSET = "storedPatchset";

    private CardUI mCardsUI;
    private RequestQueue mRequestQueue;
    private Activity mParent;
    private View mCurrentFragment;
    private GerritURL mUrl;
    private String mSelectedChange;

    public static final String CHANGE_ID = "changeID";
    public static final String STATUS = "queryStatus";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.commit_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        init();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mParent = this.getActivity();

        if (getArguments() != null) {
            mSelectedChange = getArguments().getString(CHANGE_ID);
            String status = getArguments().getString(CHANGE_ID);
            SelectedChange.setSelectedChange(mParent.getApplicationContext(), mSelectedChange, status);
        }
    }

    private void init()
    {
        mCurrentFragment = this.getView();

        mCardsUI = (CardUI) mCurrentFragment.findViewById(R.id.commit_cards);
        mRequestQueue = Volley.newRequestQueue(mParent);

        mUrl = new GerritURL();
        setSelectedChange(mSelectedChange);
    }

    private void executeGerritTask(final String query) {
        if ("".equals(getStoredPatchSet())) {
            new GerritTask(mParent) {
                @Override
                public void onJSONResult(String s) {
                    try {
                        savePatchSet(s);
                        addCards(mCardsUI,
                                new JSONCommit(
                                        new JSONArray(s).getJSONObject(0),
                                        mParent.getApplicationContext()));
                    } catch (JSONException e) {
                        Log.d(TAG, "Response from "
                                + query + " could not be parsed into cards :(", e);
                    }
                }
            }.execute(query);
        } else {
            try {
                addCards(mCardsUI, new JSONCommit(
                        new JSONArray(getStoredPatchSet()).getJSONObject(0),
                        mParent.getApplicationContext()
                ));
            } catch (JSONException e) {
                Log.d(TAG, "Stored response could not be parsed into cards :(", e);
            }
        }
    }

    private void addCards(CardUI ui, JSONCommit jsonCommit) {
        // Properties card
        Log.d(TAG, "Loading Properties Card...");
        ui.addCard(new PatchSetPropertiesCard(jsonCommit, this, mRequestQueue, mParent), true);

        // Message card
        Log.d(TAG, "Loading Message Card...");
        ui.addCard(new PatchSetMessageCard(jsonCommit), true);

        // Changed files card
        if (jsonCommit.getChangedFiles() != null
                && !jsonCommit.getChangedFiles().isEmpty()) {
            Log.d(TAG, "Loading Changes Card...");
            ui.addCard(new PatchSetChangesCard(jsonCommit, mParent), true);
        }

        // Code reviewers card
        if (jsonCommit.getCodeReviewers() != null
                && !jsonCommit.getCodeReviewers().isEmpty()) {
            Log.d(TAG, "Loading Reviewers Card...");
            ui.addCard(new PatchSetReviewersCard(jsonCommit, mRequestQueue, mParent), true);
        } else {
            Log.d(TAG, "No reviewers found! Not adding reviewers card");
        }

        // Comments Card
        if (jsonCommit.getMessagesList() != null
                && !jsonCommit.getMessagesList().isEmpty()) {
            Log.d(TAG, "Loading Comments Card...");
            ui.addCard(new PatchSetCommentsCard(jsonCommit, this, mRequestQueue), true);
        } else {
            Log.d(TAG, "No commit comments found! Not adding comments card");
        }
    }

    public void setSelectedChange(String changeID) {
        this.mSelectedChange = changeID;
        if (mSelectedChange != null && mSelectedChange.length() > 0) {
            mUrl.setChangeID(mSelectedChange);
            mUrl.requestChangeDetail(true);
            executeGerritTask(mUrl.toString());
        }
    }

    /*
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
    **may be kind of pointless without context of surrounding code**
    * maybe a webview for each if possible? *
    -------------------------

     */

    private void savePatchSet(String jsonResponse) {
        // Caching disabled
    }

    private String getStoredPatchSet() {
        return ""; // Caching disabled
    }

    private CommitterObject committerObject = null;

    public void registerViewForContextMenu(View view) {
        registerForContextMenu(view);
    }

    public static final int OWNER = 0;
    public static final int REVIEWER = 1;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        committerObject = (CommitterObject) v.getTag();
        menu.setHeaderTitle(R.string.developers_role);
        menu.add(0, v.getId(), OWNER, v.getContext().getString(R.string.context_menu_owner));
        menu.add(0, v.getId(), REVIEWER, v.getContext().getString(R.string.context_menu_reviewer));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        String tab = null;
        switch (item.getOrder()) {
            case OWNER:
                tab = CardsFragment.KEY_OWNER;
                break;
            case REVIEWER:
                tab = CardsFragment.KEY_REVIEWER;
        }
        committerObject.setState(tab);
        Intent intent = new Intent(mParent, ReviewTab.class);
        intent.putExtra(CardsFragment.KEY_DEVELOPER, committerObject);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
        return true;
    }
}