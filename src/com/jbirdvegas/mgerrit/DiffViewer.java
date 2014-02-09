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

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageButton;
import android.widget.Spinner;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.jbirdvegas.mgerrit.adapters.FileAdapter;
import com.jbirdvegas.mgerrit.database.FileChanges;
import com.jbirdvegas.mgerrit.tasks.ZipRequest;
import com.jbirdvegas.mgerrit.views.DiffTextView;

import java.util.regex.Pattern;

public class DiffViewer extends FragmentActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private String mLineSplit = System.getProperty("line.separator");
    private DiffTextView mDiffTextView;
    private Spinner mSpinner;
    private FileAdapter mAdapter;
    private final OnItemSelectedListener selectedListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String fileName = mAdapter.getPathAtPosition(position);
            loadDiff(fileName);

            int previousPosition = mAdapter.getPreviousPosition(position);
            mBtnPrevious.setVisibility(previousPosition >= 0 ? View.VISIBLE : View.INVISIBLE);

            int nextPosition = mAdapter.getNextPosition(position);
            mBtnNext.setVisibility(nextPosition >= 0 ? View.VISIBLE : View.INVISIBLE);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // Not used
        }
    };

    private ImageButton mBtnPrevious, mBtnNext;

    private String mFilePath;
    private int mChangeNumber;
    private int mPatchsetNumber;

    public static final String CHANGE_NUMBER_TAG = "changeNumber";
    public static final String PATCH_SET_NUMBER_TAG = "patchSetNumber";
    public static final String FILE_PATH_TAG = "file";

    private static RequestQueue requestQueue;
    private ZipRequest request;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.setTheme(Prefs.getCurrentThemeID(this));
        super.onCreate(savedInstanceState);

        setContentView(R.layout.diff_viewer);

        // Action bar Up affordance
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        mChangeNumber = getIntent().getIntExtra(CHANGE_NUMBER_TAG, 0);
        if (mChangeNumber == 0) {
            throw new IllegalArgumentException("Cannot load diff without a change number");
        }

        setChangeTitle(mChangeNumber);

        mFilePath = intent.getStringExtra(FILE_PATH_TAG);
        mPatchsetNumber = intent.getIntExtra(PATCH_SET_NUMBER_TAG, 0);

        mDiffTextView = (DiffTextView) findViewById(R.id.diff_view_diff);
        mSpinner = (Spinner) findViewById(R.id.diff_spinner);

        mBtnPrevious = (ImageButton) findViewById(R.id.diff_previous);
        mBtnNext = (ImageButton) findViewById(R.id.diff_next);

        mAdapter = new FileAdapter(this, null);
        mSpinner.setAdapter(mAdapter);
        mSpinner.setOnItemSelectedListener(selectedListener);

        loadDiff(mFilePath);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    private void setTextView(String result) {
        Pattern pattern = Pattern.compile("\\Qdiff --git \\E");
        String[] filesChanged = pattern.split(result);
        StringBuilder builder = new StringBuilder(0);
        for (String change : filesChanged) {
            String concat;
            int index = change.lastIndexOf(mFilePath);
            if (index < 0) continue;

            concat = change.substring(2, index).trim().split(" ", 2)[0];
            if (concat.equals(mFilePath)) {
                change.replaceAll("\n", mLineSplit);
                builder.append(change);
            }
        }
        if (builder.length() == 0) {
            builder.append("Diff not found!");
        } else {
            // reset text size to default
            mDiffTextView.setTextAppearance(this, android.R.style.TextAppearance_DeviceDefault_Small);
            mDiffTextView.setTypeface(Typeface.MONOSPACE);
        }
        // rebuild text; required to respect the \n
        mDiffTextView.setDiffText(builder.toString());
    }

    private void loadDiff(String fileName) {
        mFilePath = fileName;

        /* The whole diff may be too large to cache in memory or expired
         *  so we will launch another request for it, even if we have
         *  previously loaded a diff for this change
         */
        request = new ZipRequest(this, mChangeNumber,
                mPatchsetNumber, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                if (s != null) setTextView(s);
                else mDiffTextView.setText(getString(R.string.diff_load_failed));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                mDiffTextView.setText(R.string.diff_load_failed);
            }
        }
        );

        if (requestQueue == null) requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(request);
    }

    // Set the title of this activity
    private void setChangeTitle(Integer changeNumber) {
        String s = getResources().getString(R.string.change_detail_heading);
        setTitle(String.format(s, changeNumber));
    }

    // Handler for clicking on the previous file button
    public void onPreviousClick(View view) {
        int position = mAdapter.getPreviousPosition(mSpinner.getSelectedItemPosition());
        if (position >= 0) mSpinner.setSelection(position);
    }

    // Handler for clicking on the next file button
    public void onNextClick(View view) {
        int position = mAdapter.getNextPosition(mSpinner.getSelectedItemPosition());
        if (position >= 0) mSpinner.setSelection(position);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return FileChanges.getNonBinaryChangedFiles(this, mChangeNumber);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
        if (cursor != null && cursor.isAfterLast()) {
            if (request != null) request.cancel();
            mDiffTextView.setText(getString(R.string.diff_no_files));
        }

        int pos = mAdapter.getPositionOfFile(mFilePath);
        if (pos >= 0) mSpinner.setSelection(pos);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.swapCursor(null);
    }
}
