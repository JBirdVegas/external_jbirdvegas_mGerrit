package com.jbirdvegas.mgerrit.adapters;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ListView;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 4/8/13 12:10 AM
 */
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
