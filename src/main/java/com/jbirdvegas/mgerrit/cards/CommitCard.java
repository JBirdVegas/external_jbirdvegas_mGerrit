package com.jbirdvegas.mgerrit.cards;

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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.jbirdvegas.mgerrit.R;

/**
 * A layout that records whether it is selected or not through a custom state
 */
public class CommitCard extends LinearLayout {
    private boolean isSelected;
    private static final int[] SELECTED_STATE_ATTR = { R.attr.isSelected };

    public CommitCard(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CommitCard, 0, 0);
        isSelected = ta.getBoolean(R.styleable.CommitCard_isSelected, false);
        ta.recycle();
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {

        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isSelected) {
            mergeDrawableStates(drawableState, SELECTED_STATE_ATTR);
        }
        return drawableState;
    }

    public boolean isChangeSelected() { return isSelected; }

    public void setChangeSelected(boolean selected) {
        this.isSelected = selected;
        refreshDrawableState();
    }
}
