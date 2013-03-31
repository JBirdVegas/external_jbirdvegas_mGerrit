package com.aokp.gerrit.objects;

import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 3/31/13
 * Time: 4:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONCommit {
    private final JSONObject mRawJSONCommit;

    public JSONCommit(JSONObject object) {
        this.mRawJSONCommit = object;
    }
    public JSONCommit() {
        this(null);
    }
}
