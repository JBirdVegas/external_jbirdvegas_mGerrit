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
    public int inserted;
    public int deleted;

    private ChangedFile(String _path, JSONObject object) throws JSONException {
        path = _path;
        try {
            inserted = object.getInt(JSONCommit.KEY_INSERTED);
        } catch (JSONException noInserted) {
            inserted = Integer.MIN_VALUE;
        }
        try {
            deleted = object.getInt(JSONCommit.KEY_DELETED);
        } catch (JSONException noDeleted) {
            deleted = Integer.MIN_VALUE;
        }
    }

    public static ChangedFile parseFromJSONObject(String _path, JSONObject object) throws JSONException {
        return new ChangedFile(_path, object);
    }
}