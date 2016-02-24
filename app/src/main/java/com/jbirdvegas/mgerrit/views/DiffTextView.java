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
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.R;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

public class DiffTextView extends TextView {

    private static final String TAG = "DiffTextView";
    private int mLineAdded_color;
    private int mLineRemoved_color;
    private final int mRangeInfo_color;
    private final int mOrigHeader_color;
    private final int mNewHeader_color;
    private final int mPathInfo_color;

    private SpannableString mColorizedSpan;

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

    public void setDiffText(String text) {
        mColorizedSpan = spanColoredText(unescape(text.replaceAll("\\\\n", "\\\n").trim()));
        if (mColorizedSpan != null && mColorizedSpan.length() > 0) {
            this.setText(mColorizedSpan, BufferType.SPANNABLE);
        } else {
            this.setText("Failed to load diff :(");
        }
    }

    public boolean hasDiff() {
        return this.length() > 0;
    }

    @Contract("null -> null")
    protected SpannableString spanColoredText(String incoming) {
        if (incoming == null) return null;

        String[] split = incoming.split("\n");
        SpannableString spannableString = new SpannableString(incoming);
        int charCounter = 0;
        int lineTracker = 0;
        // colorize added/removed lines
        colorizeDiffs(split, spannableString, charCounter, lineTracker);

        highlightUnwantedChars(spannableString);
        return spannableString;
    }

    private void colorizeDiffs(String[] split, SpannableString spannableString,
                               int charCounter, int lineTracker) {
        int end;
        for (String string : split) {
            charCounter += 1;
            lineTracker += 1;
            end = charCounter + string.length() > spannableString.length()
                    ? spannableString.length()
                    : charCounter + string.length();
            String trimmed = string.trim();

            CharacterStyle style = setColor(trimmed);
            if (style != null) {
                spannableString.setSpan(style, charCounter - 1, end, 0);
            }

            // highlight trailing whitespace
            int startWhitespace = findLastNonSpace(string);

            if (startWhitespace > 0) {
                Log.d(TAG, String.format("Trailing whitespace at line: %d index: %d through %d in diff view",
                        lineTracker,
                        startWhitespace,
                        string.length()));
                spannableString.setSpan(getTrailingSpaceColor(),
                        charCounter + startWhitespace - 1, end, 0);
            }
// test line with trailing whitespaces ->
// Here are 3 tabs ->	-	-	<- this line ends with four whitespaces ->    
            charCounter += string.length();
        }
    }

    @Contract("null -> fail")
    private int findLastNonSpace(String s) {
        int startWhitespace = -1;
        if (s.endsWith(" ")) {
            // count backwards and highlight the trailing whitespace
            int lineLength = s.length();
            for (int i = lineLength - 1; 0 <= i; i--) {
                if (s.charAt(i) == ' ') {
                    startWhitespace = i;
                } else {
                    break;
                }
            }
        }
        return startWhitespace;
    }

    /**
     * Highlights whitespace issues
     */
    private void highlightUnwantedChars(SpannableString spannableString) {
        for (Integer ints : tabs) {
            Log.d(TAG, "Index of tab: " + ints);
            if (ints + 1 < spannableString.length()) {
                spannableString.setSpan(getTrailingSpaceColor(),
                        ints - 1, ints + 1,
                        Spanned.SPAN_INTERMEDIATE);
                spannableString.setSpan(new ForegroundColorSpan(Color.WHITE),
                        ints - 1, ints + 1,
                        Spanned.SPAN_INTERMEDIATE);
            }
        }
    }

    // used to track index of tab chars
    private LinkedList<Integer> tabs = new LinkedList<>();

    protected String unescape(String s) {
        int i = 0, len = s.length();
        char c;
        StringBuilder sb = new StringBuilder(len);
        while (i < len) {
            c = s.charAt(i++);
            if (c == '\\') {
                if (i < len) {
                    c = s.charAt(i++);
                    if (c == 'u') {
                        // TODO: check that 4 more chars exist and are all hex digits
                        //noinspection MagicNumber
                        c = (char) Integer.parseInt(s.substring(i, i + 4), 16);
                        i += 4;
                    } else if (c == 't') {
                        // leave \t so we can highlight
                        sb.append("\\t");
                        continue;
                    }
                    // add other cases here as desired...
                }
            } // fall through: \ escapes itself, quotes any character but u
            if (c == '\t') {
                sb.append("\\t");
                tabs.add(sb.length() - 1);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
