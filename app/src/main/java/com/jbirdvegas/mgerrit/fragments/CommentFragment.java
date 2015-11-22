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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.tasks.GerritService;

public class CommentFragment extends Fragment {

    public static final String CHANGE_ID = GerritService.CHANGE_ID;
    public static final String CHANGE_NO = GerritService.CHANGE_NUMBER;
    public static final String MESSAGE = GerritService.REVIEW_MESSAGE;

    private FragmentActivity mParent;
    private TextView mMessage;

    private String mChangeId;
    private int mChangeNumber;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_comment, container, false);
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
    }

    private void init() {
        View currentFragment = this.getView();
        mMessage = (TextView) currentFragment.findViewById(R.id.new_comment_message);

        Bundle args = getArguments();
        mChangeId = args.getString(CHANGE_ID);
        mChangeNumber = args.getInt(CHANGE_NO);

        String message = args.getString(MESSAGE);
        if (message != null) mMessage.setText(message);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CHANGE_ID, mChangeId);
        outState.putInt(CHANGE_NO, mChangeNumber);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mChangeId = savedInstanceState.getString(CHANGE_ID);
            mChangeNumber = savedInstanceState.getInt(CHANGE_NO);
        }
        super.onViewStateRestored(savedInstanceState);
    }

    public void addComment() {
        String message = mMessage.getText().toString();

        Bundle bundle = new Bundle();
        bundle.putString(GerritService.CHANGE_ID, mChangeId);
        bundle.putInt(GerritService.CHANGE_NUMBER, mChangeNumber);
        bundle.putString(GerritService.REVIEW_MESSAGE, message);
        GerritService.sendRequest(mParent, GerritService.DataType.Comment, bundle);
    }
}
