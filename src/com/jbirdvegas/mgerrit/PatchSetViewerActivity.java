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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.fima.cardsui.views.CardUI;
import com.jbirdvegas.mgerrit.cards.PatchSetChangesCard;
import com.jbirdvegas.mgerrit.cards.PatchSetCommentsCard;
import com.jbirdvegas.mgerrit.cards.PatchSetMessageCard;
import com.jbirdvegas.mgerrit.cards.PatchSetPropertiesCard;
import com.jbirdvegas.mgerrit.cards.PatchSetReviewersCard;
import com.jbirdvegas.mgerrit.objects.CommitterObject;
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
public class PatchSetViewerActivity extends Activity {
    private static final String TAG = PatchSetViewerActivity.class.getSimpleName();
    private static final String KEY_STORED_PATCHSET = "storedPatchset";
    private CardUI mCardsUI;
    private RequestQueue mRequestQueue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.commit_list);

        // Action bar Up affordance
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mRequestQueue = Volley.newRequestQueue(this);
        String query = getIntent().getStringExtra(JSONCommit.KEY_WEBSITE);
        Log.d(TAG, "Website to query: " + query);
        mCardsUI = (CardUI) findViewById(R.id.commit_cards);
        if (savedInstanceState == null) {
            savePatchSet("");
        }
        executeGerritTask(query);
    }

    private void executeGerritTask(final String query) {
        if ("".equals(getStoredPatchSet())) {
            new GerritTask(this) {
                @Override
                public void onJSONResult(String s) {
                    try {
                        savePatchSet(s);
                        addCards(mCardsUI,
                                new JSONCommit(
                                        new JSONArray(s).getJSONObject(0),
                                        getApplicationContext()));
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
                        getApplicationContext()
                ));
            } catch (JSONException e) {
                Log.d(TAG, "Stored response could not be parsed into cards :(", e);
            }
        }
    }

    private void addCards(CardUI ui, JSONCommit jsonCommit) {
        // Properties card
        Log.d(TAG, "Loading Properties Card...");
        ui.addCard(new PatchSetPropertiesCard(jsonCommit, this, mRequestQueue), true);

        // Message card
        Log.d(TAG, "Loading Message Card...");
        ui.addCard(new PatchSetMessageCard(jsonCommit), true);

        // Changed files card
        if (jsonCommit.getChangedFiles() != null
                && !jsonCommit.getChangedFiles().isEmpty()) {
            Log.d(TAG, "Loading Changes Card...");
            ui.addCard(new PatchSetChangesCard(jsonCommit, this), true);
        }

        // Code reviewers card
        if (jsonCommit.getCodeReviewers() != null
                && !jsonCommit.getCodeReviewers().isEmpty()) {
            Log.d(TAG, "Loading Reviewers Card...");
            ui.addCard(new PatchSetReviewersCard(jsonCommit, this, mRequestQueue), true);
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

    // Handles correctly setting the ListViews height based on all the children
    // from http://nex-otaku-en.blogspot.com/2010/12/android-put-listview-in-scrollview.html
    public static void setListViewHeightBasedOnChildren(ListView... listViews) {
        for (ListView listView : listViews) {
            ListAdapter listAdapter = listView.getAdapter();
            if (listAdapter == null) {
                // pre-condition
                return;
            }
            ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height = getTotalHeight(listView, listAdapter)
                    + listView.getDividerHeight() * (listAdapter.getCount() - 1);
            listView.setLayoutParams(params);
            listView.requestLayout();
        }
    }

    static int getTotalHeight(ListView listView, ListAdapter listAdapter) {
        int totalHeight = 0;
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(
                listView.getWidth(), View.MeasureSpec.AT_MOST);
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += listItem.getMeasuredHeight();
        }
        return totalHeight;
    }


    public static void setNotFoundListView(Context context, ListView listView) {
        listView.setAdapter(
                new ArrayAdapter<String>(context,
                        android.R.layout.simple_list_item_1,
                        android.R.id.text1,
                        new String[]{
                                context.getString(R.string.none_found),
                                context.getString(R.string.please_try_again)
                        }));
    }

    private void savePatchSet(String jsonResponse) {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(KEY_STORED_PATCHSET, jsonResponse)
                .commit();
    }

    private String getStoredPatchSet() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getString(KEY_STORED_PATCHSET, "");
    }

    private CommitterObject committerObject = null;

    public void registerViewForContextMenu(View view) {
        registerForContextMenu(view);
    }

    public void unregisterViewForContextMenu(View view) {
        unregisterForContextMenu(view);
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
                tab = CardsActivity.KEY_OWNER;
                break;
            case REVIEWER:
                tab = CardsActivity.KEY_REVIEWER;
        }
        committerObject.setState(tab);
        Intent intent = new Intent(this, ReviewTab.class);
        intent.putExtra(CardsActivity.KEY_DEVELOPER, committerObject);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}