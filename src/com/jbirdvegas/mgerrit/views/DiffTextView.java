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
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.R;

import org.jetbrains.annotations.NotNull;

public class DiffTextView extends TextView {

    private int mLineAdded_color;
    private int mLineRemoved_color;
    private final int mRangeInfo_color;
    private final int mOrigHeader_color;
    private final int mNewHeader_color;
    private final int mPathInfo_color;

    public DiffTextView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.diffStyle);
    }

    public DiffTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,R.styleable.DiffTextView, defStyle, R.style.Diff_Light);

        mLineAdded_color = a.getColor(R.styleable.DiffTextView_added, R.color.text_green);
        mLineRemoved_color = a.getColor(R.styleable.DiffTextView_removed, R.color.text_red);
        mRangeInfo_color = a.getColor(R.styleable.DiffTextView_rangeInfo, R.color.text_purple);
        mOrigHeader_color = a.getColor(R.styleable.DiffTextView_origHeader, R.color.text_brown);
        mNewHeader_color = a.getColor(R.styleable.DiffTextView_newHeader, Color.BLUE);
        mPathInfo_color = a.getColor(R.styleable.DiffTextView_pathInfo, R.color.text_orange);

        a.recycle();
    }

    /**
     * Given a line from a diff, determine which color in which to highlight it.
     * @param line A line from a diff comparison
     * @return A CharacterStyle containing a color in which to highlight the text
     */
    public CharacterStyle setColor(@NotNull String line) {
        if (line.startsWith("+++")) return new ForegroundColorSpan(mNewHeader_color);
        else if (line.startsWith("---")) return new ForegroundColorSpan(mOrigHeader_color);
        else if (line.startsWith("+") && !line.startsWith("+++")) return new ForegroundColorSpan(mLineAdded_color);
        else if (line.startsWith("-")) return new ForegroundColorSpan(mLineRemoved_color);
        else if (line.startsWith("@@")) return new ForegroundColorSpan(mRangeInfo_color);
        else if (line.startsWith("a/")) return new ForegroundColorSpan(mPathInfo_color);
        return null;
    }

    /**
     * @return The CharacterStyle containing what color to highlight trailing spaces
     */
    public CharacterStyle getTrailingSpaceColor() {
        return new BackgroundColorSpan(getResources().getColor(R.color.text_red));
    }
}
