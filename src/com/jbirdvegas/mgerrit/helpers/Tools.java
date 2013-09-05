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
import com.jbirdvegas.mgerrit.R;

public class Tools {
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


}
