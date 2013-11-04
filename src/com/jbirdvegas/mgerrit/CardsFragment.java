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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.haarman.listviewanimations.swinginadapters.SingleAnimationAdapter;
import com.haarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;
import com.jbirdvegas.mgerrit.adapters.ChangeListAdapter;
import com.jbirdvegas.mgerrit.cards.CommitCard;
import com.jbirdvegas.mgerrit.cards.CommitCardBinder;
import com.jbirdvegas.mgerrit.database.SyncTime;
import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.message.ChangeLoadingFinished;
import com.jbirdvegas.mgerrit.objects.ChangeLogRange;
import com.jbirdvegas.mgerrit.objects.GerritURL;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.jbirdvegas.mgerrit.tasks.GerritService;
import com.jbirdvegas.mgerrit.tasks.GerritTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public abstract class CardsFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String KEY_DEVELOPER = "committer_object";
    public static final String KEY_OWNER = "owner";
    public static final String KEY_REVIEWER = "reviewer";

    private static final boolean DEBUG = true;
    private static final boolean CHATTY = false;
    public static final String SEARCH_QUERY = "SEARCH";
    protected String TAG = "CardsFragment";

    private GerritURL mUrl;

    private RequestQueue mRequestQueue;

    private ChangeLogRange mChangelogRange;
    private GerritControllerActivity mParent;

    // Indicates that this fragment will need to be refreshed
    private boolean mIsDirty = false;

    // Broadcast receiver to receive processed search query changes
    private BroadcastReceiver mSearchQueryListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getLoaderManager().restartLoader(0, intent.getExtras(), CardsFragment.this);
        }
    };

    private ListView mListView;
    // Adapter that binds data to the listview
    private ChangeListAdapter mAdapter;
    // Wrapper for mAdapter, enabling animations
    private SingleAnimationAdapter mAnimAdapter;
    // Whether animations have been enabled
    private boolean mAnimationsEnabled;

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        init(savedInstanceState);
        setup();
    }

    private CommitCard getCommitCard(JSONObject jsonObject, Context context) {
        return new CommitCard(
                new JSONCommit(jsonObject, context),
                mRequestQueue,
                mParent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.commit_list, container, false);
    }

    private void init(Bundle savedInstanceState)
    {
        mParent = (GerritControllerActivity) this.getActivity();
        View mCurrentFragment = this.getView();
        mRequestQueue = Volley.newRequestQueue(mParent);

        // Setup the list
        int[] to = new int[] { R.id.commit_card_title, R.id.commit_card_commit_owner,
                R.id.commit_card_project_name, R.id.commit_card_last_updated,
                R.id.commit_card_commit_status };

        String[] from = new String[] { UserChanges.C_SUBJECT, UserChanges.C_NAME,
                UserChanges.C_PROJECT, UserChanges.C_UPDATED, UserChanges.C_STATUS };

        mListView = (ListView) mCurrentFragment.findViewById(R.id.commit_cards);
        mAdapter = new ChangeListAdapter(mParent, R.layout.commit_card, null, from, to, 0);
        mAdapter.setViewBinder(new CommitCardBinder(mParent, mRequestQueue));

        /* If animations have been enabled, setup and use an animation adapter, otherwise use
         *  the regular adapter. The data should always be bound to mAdapter */
        toggleAnimations(Prefs.getAnimationPreference(mParent));

        mUrl = new GerritURL();

        // Need the account id of the owner here to maintain FK db constraint
        mUrl.setRequestDetailedAccounts(true);
        mUrl.setStatus(getQuery());
    }

    private void setup()
    {
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

        sendRequest();

        // We cannot use the search query here as the SearchView may not have been initialised yet.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(mParent).registerReceiver(mSearchQueryListener,
                new IntentFilter(CardsFragment.SEARCH_QUERY));

        boolean animations = Prefs.getAnimationPreference(mParent);
        if (animations != mAnimationsEnabled) {
            toggleAnimations(animations);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(mParent).unregisterReceiver(mSearchQueryListener);
    }


    private void loadChangeLog(final ChangeLogRange logRange) {
        new GerritTask(mParent)
        {
            @Override
            public void onJSONResult(String s)
            {
                /* This is broke at the moment. This will likely be supported by
                 *  age interval searching when implemented */
                /*drawCardsFromList(
                        generateChangeLog(
                                logRange, s),
                        mCards);*/
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
            Log.d(TAG, getString(R.string.failed_to_parse_json_response) + ' ' + url + '\n' + result, e);
        }
        return commitCardList;
    }

    /**
     * Each tab provides its own query for ?p=status:[open:merged:abandoned]
     *
     * @return current tab name used for query { open, merged, abandoned }
     */
    abstract String getQuery();

    /**
     * Start the updater to check for an update if necessary
     */
    private void sendRequest() {
        Intent it = new Intent(mParent, GerritService.class);
        it.putExtra(GerritService.DATA_TYPE_KEY, GerritService.DataType.Commit);
        it.putExtra(GerritService.URL_KEY, mUrl);
        mParent.startService(it);
    }

    protected void refresh(boolean forceUpdate)
    {
        if (!mIsDirty) return;

        mIsDirty = false;
        getLoaderManager().restartLoader(0, null, this);

        if (forceUpdate) {
            SyncTime.clear(mParent);
            sendRequest();
        }
    }

    /**
     * Enables or disables listview animations. This simply toggles the
     *  adapter, initialising a new adapter if necessary.
     * @param enable Whether to enable animations on the listview
     */
    public void toggleAnimations(boolean enable) {
        if (enable) {
            if (mAnimAdapter == null) {
                mAnimAdapter = new SwingBottomInAnimationAdapter(mAdapter);
                mAnimAdapter.setAbsListView(mListView);
            }
            mListView.setAdapter(mAnimAdapter);
        } else {
            mListView.setAdapter(mAdapter);
        }
        mAnimationsEnabled = enable;
    }

    public void markDirty() { mIsDirty = true; }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (args != null) {
            String databaseQuery = args.getString("WHERE");
            if (databaseQuery != null && !databaseQuery.isEmpty()) {
                if (args.getStringArrayList("BIND_ARGS") != null) {
                    /* Create a copy as the findCommits function can modify the contents of bindArgs
                     *  and we want each receiver to use the bindArgs from the original broadcast */
                    ArrayList<String> bindArgs = new ArrayList<String>();
                    bindArgs.addAll(args.getStringArrayList("BIND_ARGS"));
                    return UserChanges.findCommits(mParent, getQuery(), databaseQuery, bindArgs);
                }
            }
        }
        return UserChanges.findCommits(mParent, getQuery(), null, null);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Naive implementation
        mAdapter.swapCursor(null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
        // Broadcast that we have finished loading changes
        new ChangeLoadingFinished(mParent, getQuery()).sendUpdateMessage();
    }
}
