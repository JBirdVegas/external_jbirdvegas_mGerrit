package com.jbirdvegas.mgerrit;


import com.jbirdvegas.mgerrit.objects.JSONCommit;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 3/31/13
 * Time: 12:52 PM
 */
public class MergedTab extends CardsActivity {
    private static final String TAG = MergedTab.class.getSimpleName();

    @Override
    String getQuery() {
        return JSONCommit.KEY_STATUS_MERGED;
    }
}