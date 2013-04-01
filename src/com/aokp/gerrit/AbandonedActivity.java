package com.aokp.gerrit;

import com.aokp.gerrit.objects.JSONCommit;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 3/31/13
 * Time: 12:52 PM
 */
public class AbandonedActivity extends CardsActivity {
    @Override
    String getQuery() {
        return JSONCommit.KEY_STATUS_ABANDONED;
    }
}
