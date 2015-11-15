package com.jbirdvegas.mgerrit.fragments;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2013
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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.jbirdvegas.mgerrit.activities.ChangelogActivity;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.adapters.GooFileArrayAdapter;
import com.jbirdvegas.mgerrit.helpers.AnalyticsHelper;
import com.jbirdvegas.mgerrit.objects.GooFileObject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

public class ChangelogFragment extends Fragment {

    private static final String TAG = "ChangelogFragment";
    private FragmentActivity mParent;
    private RequestQueue mRequestQueue;
    private String mQuery;

    Spinner mUpdatesList;
    private GooFileArrayAdapter gooAdapter;
    private ImageView mSaveBtn;
    private ViewSwitcher mViewSwitcher;

    public ChangelogFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.changelog_card, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        mParent = this.getActivity();
        mRequestQueue = Volley.newRequestQueue(mParent);

        mViewSwitcher = (ViewSwitcher) mParent.findViewById(R.id.vs_changelog_card);
        if (mViewSwitcher.getDisplayedChild() != 0) mViewSwitcher.showPrevious();

        mUpdatesList = (Spinner) mParent.findViewById(R.id.changelog);
        mSaveBtn = (ImageView) mParent.findViewById(R.id.goo_download_zip_button);
        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSaveClicked();
            }
        });

        mUpdatesList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                GooFileObject buildObject = (GooFileObject) parent.getItemAtPosition(position);
                GooFileObject previousBuild = null;
                if (position + 1 < parent.getCount()) {
                    previousBuild = (GooFileObject) parent.getItemAtPosition(position + 1);
                }
                ((ChangelogActivity) mParent).onBuildSelected(previousBuild, buildObject);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Not used
            }
        });

        findDates();
    }

    private void findDates() {
        // use Volley to get our packages list
        Log.d(TAG, "Deprecated ChangelogFragment.findDates called!");
    }

    private void setupList(List<GooFileObject> gooFilesList) {
        gooAdapter = new GooFileArrayAdapter(mParent,
                R.layout.goo_files_list_item,
                gooFilesList);
        mUpdatesList.setAdapter(gooAdapter);
    }

    public void setQuery(String query) {
        mQuery = query;
    }

    class gooImResponseListener implements Response.Listener<JSONObject> {
        @Override
        public void onResponse(JSONObject response) {
            JSONArray result = response.optJSONArray("list");
            if (result == null) {
                TextView text = (TextView) mParent.findViewById(R.id.changelog_searching);
                text.setText(mParent.getString(R.string.no_changelog));
                mParent.findViewById(R.id.changelog_progressBar).setVisibility(View.GONE);
                return;
            }
            mViewSwitcher.showNext();
            Toast.makeText(mParent, R.string.please_select_update_for_range,
                    Toast.LENGTH_LONG).show();

            int resultsSize = result.length();
            List<GooFileObject> filesList = new LinkedList<>();
            try {
                for (int i = 0; resultsSize > i; i++) {
                    filesList.add(GooFileObject.getInstance(result.getJSONObject(i)));
                }
            } catch (Exception e) {
                Log.e("ErrorListener", e.getLocalizedMessage());
            }
            setupList(filesList);
        }
    }

    public void onSaveClicked() {

        GooFileObject build = (GooFileObject) mUpdatesList.getSelectedItem();
        if (build != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(formUrl(build)));
            startActivity(intent);
        } else {
            // Failed to parse GooFileObject
            logFailure(null);
        }
    }

    protected String formUrl(GooFileObject build) {
        return build.getShortUrl() != null
                ? build.getShortUrl() : "http://goo.im/" + build.getPath();
    }

    private void logFailure(GooFileObject file) {
        AnalyticsHelper.sendAnalyticsEvent(getActivity(), AnalyticsHelper.GA_LOG_FAIL,
                AnalyticsHelper.ACTION_CHANGELOG_SAVE_FAIL,
                file == null ? AnalyticsHelper.EVENT_CHANGELOG_FILE_NULL
                        : AnalyticsHelper.EVENT_CHANGELOG_SHORT_URL_NULL
                , null);
    }
}
