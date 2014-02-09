package com.jbirdvegas.mgerrit.helpers;

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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.haarman.listviewanimations.swinginadapters.SingleAnimationAdapter;
import com.haarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.objects.FileInfo;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class Tools {

    private static final String GERRIT_DATE_FORMAT = "yyyy-MM-dd hh:mm:ss.SSS";
    private static final String HUMAN_READABLE_DATE_FORMAT = "MMMM dd, yyyy '%s' hh:mm:ss aa";

    public static void showErrorDialog(Context context,
                                Exception exception) {
        showErrorDialog(context, exception, false);
    }

    private static void showErrorDialog(final Context context, final Exception exception, boolean showException) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                context);
        builder.setCancelable(true)
                .setTitle(exception.getLocalizedMessage())
                .setInverseBackgroundForced(true)
                .setPositiveButton(R.string.exit,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                // don't game over user may
                                // have not entered the right
                                // address just drop the dialog
                                dialog.dismiss();
                            }
                        });
        if (showException) {
            builder.setMessage(stackTraceToString(exception));
        } else {
            builder.setMessage(String.format("%s\n\n%s\n%s",
                    context.getString(R.string.gerrit_call_failed),
                    context.getString(R.string.caused_by),
                    exception.getLocalizedMessage()))
                    .setNegativeButton(R.string.show_exception,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    showErrorDialog(context, exception, true);
                                }
                            });
        }
        builder.create().show();
    }

    public static String stackTraceToString(Throwable e) {
        StringBuilder sb = new StringBuilder(0);
        sb.append(e.getLocalizedMessage()).append("\n\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("-- ")
                .append(element.toString())
                .append('\n');
        }
        return sb.toString();
    }

    /**
     * Enables or disables listview animations. This simply toggles the
     *  adapter, initialising a new adapter if necessary.
     * @param enable Whether to enable animations on the listview
     * @return enable
     */
    public static boolean toggleAnimations(boolean enable, ListView lv,
                                           SingleAnimationAdapter animAdapter,
                                           BaseAdapter defaultAdapter) {
        if (enable) {
            if (animAdapter == null) {
                animAdapter = new SwingBottomInAnimationAdapter(defaultAdapter);
                animAdapter.setAbsListView(lv);
            }
            lv.setAdapter(animAdapter);
        } else if (defaultAdapter != null) {
            lv.setAdapter(defaultAdapter);
        }
        return enable;
    }

    /**
     * Queries the active network and determine if it has Internet connectivity.
     * @param context Application or activity context
     * @return Whether we are connected to the internet, regardless of the network type
     */
    public static boolean isConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }


    /**
     * PrettyPrint the Gerrit provided timestamp
     * format into a more human readable format
     *
     * I have no clue what the ten zeros after the seconds are good for as
     * the exact same ten zeros are in all databases regardless of the stamp's
     * time or timezone... it comes from Google so we just handle oddities downstream :(
     * from "2013-06-09 19:47:40.000"
     * to Jun 09, 2013 07:47 40ms PM
     *
     * @return String representation of the date
     *         example: Jun 09, 2013 07:47 40ms PM
     */
    @SuppressWarnings("SimpleDateFormatWithoutLocale")
    public static String prettyPrintDate(Context context, String date,
                                   TimeZone serverTimeZone, TimeZone localTimeZone) {
        try {
            SimpleDateFormat currentDateFormat
                    = new SimpleDateFormat(GERRIT_DATE_FORMAT, Locale.US);
            DateFormat humanDateFormat = new SimpleDateFormat(
                    String.format(HUMAN_READABLE_DATE_FORMAT,
                            context.getString(R.string.at)),
                    Locale.getDefault());
            // location of server
            currentDateFormat.setTimeZone(serverTimeZone);
            // local location
            humanDateFormat.setTimeZone(localTimeZone);
            return humanDateFormat.format(currentDateFormat.parse(date));
        } catch (ParseException e) {
            e.printStackTrace();
            return date;
        }
    }

    public static void colorPath(Resources r, TextView view,
                                 String statusText, boolean usingLightTheme) {
        FileInfo.Status status = FileInfo.Status.getValue(statusText);
        int green = r.getColor(R.color.text_green);
        int red = r.getColor(R.color.text_red);

        if (status == FileInfo.Status.ADDED) {
            view.setTextColor(green);
        } else if (status == FileInfo.Status.DELETED) {
            view.setTextColor(red);
        } else {
            // Need to determine from the current theme what the default color is and set it back
            if (usingLightTheme) {
                view.setTextColor(r.getColor(R.color.text_light));
            } else {
                view.setTextColor(r.getColor(R.color.text_dark));
            }
        }
    }

    /**
     * @param filePath A path to a file
     * @return The short file name from a full file path
     */
    public static final String getFileName(String filePath) {
        int lastIndex = filePath.lastIndexOf(File.separatorChar);
        return filePath.substring(++lastIndex);
    }

    public static String getWebAddress(Context context, int commitNumber) {
        return String.format("%s#/c/%d/", Prefs.getCurrentGerrit(context), commitNumber);
    }
}