package com.jbirdvegas.mgerrit.helpers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import com.jbirdvegas.mgerrit.R;

/**
 * Author jbird on 6/9/13.
 */
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
