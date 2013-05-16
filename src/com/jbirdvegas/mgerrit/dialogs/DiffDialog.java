package com.jbirdvegas.mgerrit.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.objects.ChangedFile;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jbird on 5/16/13.
 */
public class DiffDialog extends AlertDialog.Builder {
    private static final String TAG = DiffDialog.class.getSimpleName();
    private static final String DIFF = "\n\nDIFF\n\n";
    private final String mUrl;
    private final View mRootView;
    private final ChangedFile mChangedFile;

    public DiffDialog(Context context, String website, ChangedFile changedFile) {
        super(context);
        mUrl = website;
        mChangedFile = changedFile;
        Log.d(TAG, "Calling url: " + mUrl);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRootView = inflater.inflate(R.layout.diff_dialog, null);
        setView(mRootView);
        AsyncTask<Void, Void, String> asyncTask = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                BufferedReader reader = null;
                try {
                    URL url = new URL(mUrl);
                    URLConnection connection = url.openConnection();
                    connection.connect();
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder builder = new StringBuilder(0);
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                    return  builder.toString();
                } catch (EOFException eof) {
                    Log.e(TAG, "Url returned blank!", eof);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to download patchset diff!", e);
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            // failed to close reader
                        }
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                setTextView((TextView) mRootView.findViewById(R.id.diff_view_diff), result);
            }
        };
        asyncTask.execute();
    }

    private void setTextView(TextView textView, String result) {
        Pattern pattern = Pattern.compile("git a");
        String[] filesChanged = pattern.split(result);
        StringBuilder builder = new StringBuilder(0);
        String split = System.getProperty("line.separator");
        for (String change : filesChanged) {
            String concat;
            try {
                concat = change.substring(1, change.lastIndexOf(mChangedFile.getPath())).trim();
                concat = concat.split(" ")[0];
            } catch (StringIndexOutOfBoundsException notFound) {
                Log.d(TAG, notFound.getMessage());
                continue;
            }
            if (concat.equals(mChangedFile.getPath())) {
                builder.append(DIFF);
                change.replaceAll("\n", split);
                builder.append(change);
            }
        }
        if (builder.length() == 0) {
            builder.append("Diff not found!");
        }
        // rebuild text; required to respect the \n
        textView.setText(builder.toString().replaceAll("\\\\n", "\\\n").trim());
    }
}
