package com.jbirdvegas.mgerrit.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.objects.ChangedFile;
import com.jbirdvegas.mgerrit.objects.Diff;
import com.jbirdvegas.mgerrit.tasks.GerritTask;

import java.util.regex.Pattern;

/**
 * Created by jbird on 5/16/13.
 */
public class DiffDialog extends AlertDialog.Builder {
    private static final String TAG = DiffDialog.class.getSimpleName();
    private static final String DIFF = "\n\nDIFF\n\n";
    private final String mUrl;
    private View mRootView;
    private final ChangedFile mChangedFile;
    private String mLineSplit = System.getProperty("line.separator");
    private LayoutInflater mInflater;

    public DiffDialog(final Context context, String website, ChangedFile changedFile) {
        super(context);
        mUrl = website;
        mChangedFile = changedFile;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRootView = mInflater.inflate(R.layout.diff_dialog, null);
        setView(mRootView);
        Log.d(TAG, "Calling url: " + mUrl);
        new GerritTask(context) {
            @Override
            public void onJSONResult(String result) {
                setTextView((TextView) mRootView.findViewById(R.id.diff_view_diff), result);
            }
        }.execute(mUrl);
    }

    private void setTextView(TextView textView, String result) {
        Pattern pattern = Pattern.compile("\\Qdiff --git \\E");
        String[] filesChanged = pattern.split(result);
        StringBuilder builder = new StringBuilder(0);
        Diff currentDiff = null;
        for (String change : filesChanged) {
            String concat;
            try {
                concat = change.substring(2, change.lastIndexOf(mChangedFile.getPath())).trim();
                concat = concat.split(" ")[0];
            } catch (StringIndexOutOfBoundsException notFound) {
                Log.d(TAG, notFound.getMessage());
                continue;
            }
            if (concat.equals(mChangedFile.getPath())) {
                builder.append(DIFF);
                change.replaceAll("\n", mLineSplit);
                currentDiff = new Diff(getContext(), change);
                builder.append(change);
            }
        }
        if (builder.length() == 0) {
            builder.append("Diff not found!");
        }
        textView.setTypeface(Typeface.MONOSPACE);
        // rebuild text; required to respect the \n
        SpannableString spannableString = currentDiff.getColorizedSpan();
        if (spannableString != null) {
            textView.setText(currentDiff.getColorizedSpan(), TextView.BufferType.SPANNABLE);
        } else {
            textView.setText("Failed to load diff :(");
        }
    }
}
