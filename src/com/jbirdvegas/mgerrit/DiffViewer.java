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
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.jbirdvegas.mgerrit.database.FileChanges;
import com.jbirdvegas.mgerrit.tasks.ZipRequest;
import com.jbirdvegas.mgerrit.views.DiffTextView;

import java.util.regex.Pattern;

public class DiffViewer extends FragmentActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private String mLineSplit = System.getProperty("line.separator");
    private DiffTextView mDiffTextView;
    private Spinner mSpinner;
    private SimpleCursorAdapter mAdapter;

    private String mFilePath;
    private int mChangeNumber;

    public static final String CHANGE_NUMBER_TAG = "changeNumber";
    public static final String PATCH_SET_NUMBER_TAG = "patchSetNumber";
    public static final String FILE_PATH_TAG = "file";


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

        String filePath = intent.getStringExtra(FILE_PATH_TAG);
        Integer patchSetNumber = intent.getIntExtra(PATCH_SET_NUMBER_TAG, 0);

        mFilePath = filePath;
        mDiffTextView = (DiffTextView) findViewById(R.id.diff_view_diff);
        mSpinner = (Spinner) findViewById(R.id.diff_spinner);

        mAdapter = new SimpleCursorAdapter(this, R.layout.diff_files_row, null,
                new String[] { FileChanges.C_FILE_NAME },
                new int[] { R.id.changed_file_path }, 0);
        mSpinner.setAdapter(mAdapter);

        ZipRequest request = new ZipRequest(this, mChangeNumber, patchSetNumber, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                if (s != null) setTextView(s);
                else mDiffTextView.setText("Failed to get diff!");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                mDiffTextView.setText("Failed to load diff :(");
            }
        }
        );
        Volley.newRequestQueue(this).add(request);

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

    private void setChangeTitle(Integer changeNumber) {
        String s = getResources().getString(R.string.change_detail_heading);
        setTitle(String.format(s, changeNumber));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return FileChanges.getNonBinaryChangedFiles(this, mChangeNumber);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.swapCursor(null);
    }
}
