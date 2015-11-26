/*
 * Copyright (C) 2015 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2015
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

package com.jbirdvegas.mgerrit.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.adapters.RatingAdapter;
import com.jbirdvegas.mgerrit.database.Changes;
import com.jbirdvegas.mgerrit.database.Labels;
import com.jbirdvegas.mgerrit.objects.LabelValues;

import java.util.ArrayList;

public class ReviewFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String C_CHANGE_ID = Changes.C_CHANGE_ID;
    public static final int LOADER_LABELS = 3;

    private LayoutInflater mInflater;
    private Context mContext;
    ArrayList<View> labelViews = new ArrayList<>();
    private String mChangeId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mInflater = inflater;
        return inflateRow(inflater, container);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mContext = getContext();

        Bundle args = getArguments();
        if (args != null) {
            mChangeId = args.getString(C_CHANGE_ID);
        }

        getLoaderManager().initLoader(LOADER_LABELS, null, this);
    }

    private View inflateRow(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.item_label, container, false);
        labelViews.add(view);
        return view;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // TODO: Need to either pass the project in here or it needs to fetch the project from the
        //  the change id. The latter would be easier
        return Labels.getPermittedLabels(getContext(), mChangeId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Inflate one item for item_label for each row
        int labelRowNumber = 0;

        int labelIndex = data.getColumnIndexOrThrow(Labels.C_NAME);
        int valueIndex = data.getColumnIndexOrThrow(Labels.C_VALUE);
        int descIndex = data.getColumnIndexOrThrow(Labels.C_DESCRIPTION);

        while (data.moveToNext()) {
            if (labelRowNumber >= labelViews.size()) {
                inflateRow(mInflater, (ViewGroup) getView());
            }

            View view = labelViews.get(labelRowNumber);
            TextView txtLabel = (TextView) view.findViewById(R.id.txtLabel);
            Spinner spnRating = (Spinner) view.findViewById(R.id.spnLabelRating);

            String label = data.getString(labelIndex);
            txtLabel.setText(label);

            ArrayList<LabelValues> ratingValues = new ArrayList<>();
            while (data.getString(labelIndex).equals(label)) {
                String value = data.getString(valueIndex);
                String desc = data.getString(descIndex);
                ratingValues.add(new LabelValues(label, value, desc));
            }
            data.moveToPrevious();

            RatingAdapter adapter = new RatingAdapter(mContext, R.layout.item_label_values);
            adapter.addAll(ratingValues);
            spnRating.setAdapter(adapter);

            // We have finished with this row, inflate another one if necessary
            labelIndex++;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Do nothing (for now)
    }
}
