package com.jbirdvegas.mgerrit.objects;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import com.jbirdvegas.mgerrit.R;

import java.util.LinkedList;

/**
 * Created by jbird on 5/18/13.
 */
public class Diff {
    private static final String TAG = Diff.class.getSimpleName();
    private final Resources mResources;
    private String mFileDiff;
    private String mPath;
    private SpannableString mColorizedSpan;
    private Context mContext;

    public Diff(Context context, String fileDiff) {
        mContext = context;
        mResources = context.getResources();
        String path = fileDiff.split(" ")[0].trim();
        if ('a' == path.charAt(0)) {
            mPath = path.substring(2, path.length());
        } else {
            mPath = path;
        }
        Log.d(TAG, "Path: " + mPath);
        try {
            // rebuild text; required to respect the \n
            mFileDiff = unescape(fileDiff.replaceAll("\\\\n", "\\\n").trim());
            mColorizedSpan = spanColoredText(mFileDiff);
        } catch (NullPointerException npe) {
            Log.e(TAG, "Diff was null!");
        }
    }

    public String getFileDiff() {
        return mFileDiff;
    }

    public String getPath() {
        return mPath;
    }

    public SpannableString getColorizedSpan() {
        return mColorizedSpan;
    }

    @Override
    public String toString() {
        return new StringBuilder(0)
                .append("Diff{ ")
                .append("mFileDiff='").append(mFileDiff).append('\'')
                .append(", mPath='").append(mPath).append('\'')
                .append(", mColorizedSpan=").append(mColorizedSpan)
                .append(" }").toString();
    }

    private SpannableString spanColoredText(String incoming) {
        if (incoming == null)
            return null;
        String[] split = incoming.split("\n");
        SpannableString spannableString = new SpannableString(incoming);
        int charCounter = 0;
        int lineTracker = 0;
        Resources resources = mContext.getResources();
        // colorize added/removed lines
        colorizeDiffs(split, spannableString, charCounter, lineTracker, resources);
        // highlight tabs in red
        highlightUnwantedChars(spannableString, resources);
        return spannableString;
    }

    private void colorizeDiffs(String[] split, SpannableString spannableString, int charCounter, int lineTracker, Resources resources) {
        int end = 0;
        for (String string : split) {
            charCounter += 1;
            lineTracker += 1;
            end = charCounter + string.length() > spannableString.length()
                    ? spannableString.length()
                    : charCounter + string.length();
            String trimmed = string.trim();
            if (trimmed.startsWith("+") && !trimmed.startsWith("+++")) {
                spannableString.setSpan(new ForegroundColorSpan(resources.getColor(R.color.text_green)),
                        charCounter - 1,
                        end,
                        0);
                // highlight removed code with red background
                // do not highlight file diffs
            } else if (trimmed.startsWith("-") && !trimmed.startsWith("---")) {
                spannableString.setSpan(new BackgroundColorSpan(resources.getColor(R.color.text_red)),
                        charCounter - 1,
                        end,
                        0);
            } else if (trimmed.startsWith("@@")) {
                spannableString.setSpan(new ForegroundColorSpan(resources.getColor(android.R.color.holo_purple)),
                        charCounter - 1,
                        end,
                        0);
            } else if (trimmed.startsWith("---")) {
                spannableString.setSpan(new ForegroundColorSpan(resources.getColor(R.color.text_brown)),
                        charCounter - 1,
                        end,
                        0);
            } else if (trimmed.startsWith("+++")) {
                spannableString.setSpan(new ForegroundColorSpan(Color.BLUE),
                        charCounter - 1,
                        end,
                        0);
            } else if (trimmed.startsWith("a/")) {
                spannableString.setSpan(new ForegroundColorSpan(resources.getColor(R.color.text_orange)),
                        charCounter - 1,
                        end,
                        0);
            }

            // highlight trailing whitespace
            if (string.endsWith(" ")) {
                // count backwards and highlight the trailing whitespace
                int lineLength = string.length();
                int startWhitespace = -1;
                for (int i = lineLength - 1; 0 <= i; i--) {
                    if (string.charAt(i) == ' ') {
                        startWhitespace = i;
                    } else {
                        break;
                    }
                }
                if (startWhitespace > 0) {
                    Log.d(TAG, String.format("Trailing whitespace at line: %d index: %d through %d in diff view of file %s",
                            lineTracker,
                            startWhitespace,
                            string.length(),
                            mPath));
                    spannableString.setSpan(new BackgroundColorSpan(resources.getColor(R.color.text_red)),
                            charCounter + startWhitespace - 1,
                            end,
                            0);
                }
// test line with trailing whitespaces ->        
// Here are 3 tabs ->	-	-	<- this line ends with four whitespaces ->    
            }
            charCounter += string.length();
        }
    }

    private void highlightUnwantedChars(SpannableString spannableString, Resources resources) {
        for (Integer ints : tabs) {
            Log.d(TAG, "Index of tab: " + ints);
            spannableString.setSpan(new BackgroundColorSpan(resources.getColor(R.color.text_red)),
                    ints - 1,
                    ints + 1,
                    Spanned.SPAN_INTERMEDIATE);
            spannableString.setSpan(new ForegroundColorSpan(Color.WHITE),
                    ints - 1,
                    ints + 1,
                    Spanned.SPAN_INTERMEDIATE);
        }
    }

    // used to track index of tab chars
    LinkedList<Integer> tabs = new LinkedList<Integer>();

    private String unescape(String s) {
        int i = 0, len = s.length(), realCounter = 0;
        char c;
        StringBuffer sb = new StringBuffer(len);
        while (i < len) {
            realCounter++;
            c = s.charAt(i++);
            if (c == '\\') {
                if (i < len) {
                    c = s.charAt(i++);
                    if (c == 'u') {
                        // TODO: check that 4 more chars exist and are all hex digits
                        c = (char) Integer.parseInt(s.substring(i, i + 4), 16);
                        i += 4;
                    } else if (c == 't') {
                        // leave \t so we can highlight
                        c = '\t';
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