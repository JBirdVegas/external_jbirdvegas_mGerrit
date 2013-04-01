package com.aokp.gerrit;


import com.aokp.gerrit.objects.JSONCommit;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 3/31/13
 * Time: 12:52 PM
 */
public class MergedActivity extends CardsActivity {
    private static final String TAG = MergedActivity.class.getSimpleName();

    @Override
    String getQuery() {
        return JSONCommit.KEY_STATUS_MERGED;
    }
}