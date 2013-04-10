package com.jbirdvegas.mgerrit.adapters;

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

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ListView;

public class SensitiveListView extends ListView {
    private static final int WIDE_HEIGHT = -2147483218;
    private static final String TAG = SensitiveListView.class.getSimpleName();

    public SensitiveListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SensitiveListView(Context context) {
        super(context);
    }

    public SensitiveListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onMeasure, widthMeasureSpec=" + widthMeasureSpec
                    + ", heightMeasureSpec=" + heightMeasureSpec);
            Log.d(TAG, "onMeasure, set heightMeasureSpec=" + WIDE_HEIGHT);
        }
        super.onMeasure(widthMeasureSpec, WIDE_HEIGHT);
    }
}
