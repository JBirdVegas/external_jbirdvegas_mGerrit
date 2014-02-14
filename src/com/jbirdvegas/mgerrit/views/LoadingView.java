package com.jbirdvegas.mgerrit.views;

/*
 * Copyright (C) 2014 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2014
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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.R;

public class LoadingView extends LinearLayout {

    private TextView mMessage;
    private static String sLoadingDiffText;
    private static String sLoadingImage;

    public LoadingView(Context context) {
        super(context);
        setup(context);
    }

    public LoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    public LoadingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setup(context);
    }

    private void setup(Context context) {
        LayoutInflater.from(context).inflate(R.layout.loading_view, this);
        mMessage = (TextView) findViewById(R.id.loading_message);

        sLoadingDiffText = context.getResources().getString(R.string.diff_text_loading);
        sLoadingImage = context.getResources().getString(R.string.diff_image_loading);
    }

    public void setMessage(CharSequence text) {
        mMessage.setText(text);
    }

    public void loadingDiffText() {
        setMessage(sLoadingDiffText);
    }

    public void loadingDiffImage() {
        setMessage(sLoadingImage);
    }
}
