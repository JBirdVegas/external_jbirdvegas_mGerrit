package com.aokp.gerrit.objects;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 4/3/13 6:49 PM
 */
public class ChangedFile {
    public String path;
    public String status;
    public int inserted;
    public int deleted;

    public ChangedFile parseFromJSONObject(String path, JSONObject object) throws JSONException {
        this.path = path;
        this.status = object.getString(JSONCommit.KEY_STATUS);
        this.inserted = object.getInt(JSONCommit.KEY_INSERTED);
        this.deleted = object.getInt(JSONCommit.KEY_DELETED);
        return this;
    }
}