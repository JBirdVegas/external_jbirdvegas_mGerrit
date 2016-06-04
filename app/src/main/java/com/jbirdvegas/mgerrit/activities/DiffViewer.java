package com.jbirdvegas.mgerrit.activities;

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
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.ViewFlipper;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.adapters.FileAdapter;
import com.jbirdvegas.mgerrit.database.FileChanges;
import com.jbirdvegas.mgerrit.helpers.ThemeHelper;
import com.jbirdvegas.mgerrit.helpers.Tools;
import com.jbirdvegas.mgerrit.message.ChangeDiffLoaded;
import com.jbirdvegas.mgerrit.message.ErrorDuringConnection;
import com.jbirdvegas.mgerrit.message.ImageLoaded;
import com.jbirdvegas.mgerrit.objects.ChangedFileInfo;
import com.jbirdvegas.mgerrit.tasks.GerritService;
import com.jbirdvegas.mgerrit.views.DiffTextView;
import com.jbirdvegas.mgerrit.views.LoadingView;
import com.jbirdvegas.mgerrit.views.StripedImageView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.regex.Pattern;


public class DiffViewer extends BaseDrawerActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private String mLineSplit = System.getProperty("line.separator");
    private DiffTextView mDiffTextView;
    private Spinner mSpinner;
    private FileAdapter mAdapter;

    private final AdapterView.OnItemSelectedListener mSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mFilePath = mAdapter.getPathAtPosition(position);
            String statusString = (String) view.getTag(R.id.status);
            ChangedFileInfo.Status status = ChangedFileInfo.Status.getValue(statusString);
            switchViews(DiffType.Loading);

            if (Tools.isImage(mFilePath)) {
                mLoadingView.loadingDiffImage();
            } else {
                mLoadingView.loadingDiffText();
            }

            // Whether we are already showing this diff due to a recent configuration change
            boolean diffSelected = (mFilePath.equals(mSelectedFilePath) && mPatchsetNumber == mSelectedPatchsetNumber);
            if (Tools.isImage(mFilePath) || !diffSelected || !mDiffTextView.hasDiff()) {
                makeRequest(mFilePath, status);
            } else {
                /* Finished restoring the state, resetting the saved file path is enough to make sure
                 *  we keep making a new request until the next configuration change */
                mSelectedFilePath = null;
                switchViews(DiffType.Text);
            }

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
    private ViewFlipper mSwitcher;
    private LoadingView mLoadingView;

    private String mFilePath;
    private int mChangeNumber;
    private int mPatchsetNumber;
    // Used for restoring the Diff text - we don't need to requery if the text hasn't changed
    private String mSelectedFilePath;
    private int mSelectedPatchsetNumber;

    public static final String CHANGE_NUMBER_TAG = "changeNumber";
    public static final String PATCH_SET_NUMBER_TAG = "patchSetNumber";
    public static final String FILE_PATH_TAG = "file";

    private static final String _DIFF_TEXT_TAG = "diff_text";
    private static final String _DIFF_STATE = "diff_state";
    private EventBus mEventBus;

    private enum DiffType { Loading, Text, Image }


    public static int LOADER_DIFF = 14;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.setTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.diff_viewer);

        Intent intent = getIntent();
        mChangeNumber = getIntent().getIntExtra(CHANGE_NUMBER_TAG, 0);
        if (mChangeNumber == 0) {
            throw new IllegalArgumentException("Cannot load diff without a change number");
        }

        initNavigationDrawer(false);
        setChangeTitle(mChangeNumber);

        mFilePath = intent.getStringExtra(FILE_PATH_TAG);
        mPatchsetNumber = intent.getIntExtra(PATCH_SET_NUMBER_TAG, 0);

        mLoadingView = (LoadingView) findViewById(R.id.diff_loading);
        mDiffTextView = (DiffTextView) findViewById(R.id.diff_view_diff);
        mSpinner = (Spinner) findViewById(R.id.diff_spinner);
        mSwitcher = (ViewFlipper) findViewById(R.id.diff_switcher);
        mSwitcher.setDisplayedChild(DiffType.Loading.ordinal());

        mBtnPrevious = (ImageButton) findViewById(R.id.diff_previous);
        mBtnNext = (ImageButton) findViewById(R.id.diff_next);

        mAdapter = new FileAdapter(this, null);
        mSpinner.setAdapter(mAdapter);
        mSpinner.setOnItemSelectedListener(mSelectedListener);

        mAdapter = new FileAdapter(this, null);
        mSpinner.setAdapter(mAdapter);
        mSpinner.setOnItemSelectedListener(mSelectedListener);
        getSupportLoaderManager().initLoader(LOADER_DIFF, null, this);

        mEventBus = EventBus.getDefault();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mEventBus.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mEventBus.unregister(this);
    }

    private void makeRequest(final String filePath, final ChangedFileInfo.Status fileStatus) {
        if (filePath == null) return;
        mFilePath = filePath;

        Intent it = new Intent(this, GerritService.class);

        if (Tools.isImage(mFilePath)) {
            it.putExtra(GerritService.DATA_TYPE_KEY, GerritService.DataType.Image);
        } else {
            it.putExtra(GerritService.DATA_TYPE_KEY, GerritService.DataType.Diff);
        }

        it.putExtra(GerritService.CHANGE_NUMBER, mChangeNumber);
        it.putExtra(GerritService.PATCHSET_NUMBER, mPatchsetNumber);
        it.putExtra(GerritService.FILE_PATH, filePath);

        if (fileStatus != null) {
            it.putExtra(GerritService.FILE_STATUS, fileStatus);
        }
        startService(it);
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

    // Set the title of this activity
    private void setChangeTitle(Integer changeNumber) {
        String s = getResources().getString(R.string.change_detail_heading);
        setTitle(String.format(s, changeNumber));
    }

    private void switchViews(DiffType type) {
        mSwitcher.setDisplayedChild(type.ordinal());
    }

    private boolean isDisplayedView(DiffType type) {
        return mSwitcher.getDisplayedChild() == type.ordinal();
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
    protected void onSaveInstanceState(Bundle outState) {
        if (outState == null) outState = new Bundle();
        outState.putString(FILE_PATH_TAG, mFilePath);
        outState.putInt(PATCH_SET_NUMBER_TAG, mPatchsetNumber);
        outState.putInt(_DIFF_STATE, mSwitcher.getDisplayedChild());

        if (isDisplayedView(DiffType.Text)) {
            outState.putCharSequence(_DIFF_TEXT_TAG, mDiffTextView.getText());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mSelectedFilePath = savedInstanceState.getString(FILE_PATH_TAG);
        mSelectedPatchsetNumber = savedInstanceState.getInt(PATCH_SET_NUMBER_TAG);
        CharSequence text = savedInstanceState.getCharSequence(_DIFF_TEXT_TAG);
        if (text != null) {
            mDiffTextView.setText(text);
            switchViews(DiffType.Text);
        } else {
            mSwitcher.setDisplayedChild(savedInstanceState.getInt(_DIFF_STATE, 0));
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        if (i == LOADER_DIFF) {
            return FileChanges.getDiffableFiles(this, mChangeNumber);
        } else {
            return super.onCreateLoader(i, bundle);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursorLoader.getId() == LOADER_DIFF) {
            mAdapter.swapCursor(cursor);
            if (cursor != null && cursor.isAfterLast()) {
                mDiffTextView.setText(getString(R.string.diff_no_files));
            }

            int pos = mAdapter.getPositionOfFile(mFilePath);
            if (pos >= 0) mSpinner.setSelection(pos);
        } else {
            super.onLoadFinished(cursorLoader, cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (cursorLoader.getId() == LOADER_DIFF) {
            mAdapter.swapCursor(null);
        } else {
            super.onLoaderReset(cursorLoader);
        }
    }

    /* Setting this sticky will deliver any events that may have come through during
     *  a configuration change */
    @Keep @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onTextDiffLoaded(ChangeDiffLoaded ev) {
        // Check if this is the event we requested.
        int psNumber = ev.getPatchsetNumber();
        if (ev.getChangeNumber() == mChangeNumber && psNumber == mPatchsetNumber) {
            String diff = ev.getDiff();
            if (diff != null) setTextView(diff);
            else mDiffTextView.setText(getString(R.string.failed_to_get_diff));
            switchViews(DiffType.Text);

            // Record that we have already loaded this diff (don't need to re-query it)
            mSelectedPatchsetNumber = psNumber;
            mSelectedFilePath = mFilePath;

            // Remove the event so we don't process the response again
            mEventBus.removeStickyEvent(ev);
        }
    }

    @Keep @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onImageLoaded(ImageLoaded ev) {
        Bitmap bitmap = ev.getImage();
        String filePath = ev.getFilePath();
        ChangedFileInfo.Status fileStatus = ev.getFileStatus();

        if (bitmap == null) {
            mDiffTextView.setText(R.string.failed_to_decode_image);
            switchViews(DiffType.Text);
            return;
        }

        // Only display the image if this was the one we requested
        if (filePath.equals(mFilePath)) {
            StripedImageView imageView = (StripedImageView) findViewById(R.id.diff_image);
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(bitmap);
            imageView.setStripe(fileStatus);
            switchViews(DiffType.Image);
        }
    }

    @Keep @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnectionError(ErrorDuringConnection ev) {
        if (Tools.isImage(mFilePath)) {
            mDiffTextView.setText(R.string.failed_to_load_image);
        } else {
            mDiffTextView.setText(R.string.failed_to_get_diff);
        }
        switchViews(DiffType.Text);
    }
}
